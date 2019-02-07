import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This corresponds to an entire DNS Message. It should contain:
 * + the DNS Header
 * + an array of questions
 * + an array of answers
 * + an array of "authority records" which we'll ignore
 * + an array of "additional records" which we'll almost ignore
 *
 * You should also store the byte array containing the complete message in this class. You'll need it to handle the
 * compression technique described above
 */

public class DNSMessage {

    private byte[] completeMessage;

    private ByteArrayInputStream messageStream;

    private DNSRecord[] answers;

    private DNSHeader header;

    private DNSQuestion[] questions;

    private byte[] additionalRecord;

    private HashMap<String, Integer> domainNameLocation;


    /**
     * Use this for the request/response messages
     *
     * Reads the entirety of the response data into the completeMessage member var.
     * @param bytes - bytes from the datagram packet response from google's dns
     * @return - this DNSMessage object containing the given message
     */
    static DNSMessage decodeMessage(byte[] bytes) throws IOException {
        DNSMessage msg = new DNSMessage();
        msg.domainNameLocation = new HashMap<>();
        msg.completeMessage = new byte[bytes.length];

        // Store the passed byte[] data in this msg object
        for (int i = 0; i < bytes.length; ++i) {
            msg.completeMessage[i] = bytes[i];
        }

        msg.messageStream = new ByteArrayInputStream(msg.completeMessage); // initialize bytestream from message data for reading
        msg.header = DNSHeader.decodeHeader(msg.messageStream); // Generate the header from the stream
        msg.questions = new DNSQuestion[msg.header.getQuestionCount()]; // Initialize the array of questions based on the amount in the header

        // Generate the question objects from the stream
        for (int x = 0; x < msg.header.getQuestionCount(); ++x) {
            msg.questions[x] = DNSQuestion.decodeQuestion(msg.messageStream, msg);
        }

        msg.answers = new DNSRecord[msg.header.getAnswerCount()]; // initialize the array of answers based on the value in the header

        for (int x = 0; x < msg.header.getAnswerCount(); ++x) {
            msg.answers[x] = DNSRecord.decodeRecord(msg.messageStream, msg);
            if (DNSServer.serverCache.cacheQuery(msg.questions[0]) == false && msg.answers[0].getIpBytes().length != 0) {
                DNSServer.serverCache.addRecord(msg.questions[0], msg.answers[0]);
            }
        }
        msg.additionalRecord = new byte[11];
        msg.messageStream.read(msg.additionalRecord, 0, 11);

        return msg;
    }

    /**
     * read the pieces of a domain name starting from the current position of the input stream
     * @param stream - byte stream passed by DNSRecord
     * @return String array containing the sections of the domain name
     */
    String[] readDomainName(InputStream stream) throws IOException {
        ArrayList<String> sections = new ArrayList<>();
        byte questionLen = (byte) stream.read();
        if ((questionLen & 0xc0) == 0xc0) { // if qlength has: (1 1) . . . . . . | . . . . . . . .
            int nameLoc = questionLen & 0x3f;
            nameLoc <<= 8;
            nameLoc |= stream.read();
            return readDomainName(nameLoc);
        }

        while (questionLen != 0x00) {
            byte[] question = new byte[questionLen];
            for (int i = 0; i < questionLen; i++) {
                byte test = (byte) stream.read();
                question[i] = test;
            }

            if (questionLen == 0x00) {  // This ensures the while look does not execute one last time when byte is 0x00
                break;
            } else {
                String newSection = "";
                for (byte b : question) {
                    newSection += (char) (b & 0xff);
                }
                sections.add(newSection);
                questionLen = (byte) stream.read();
            }
        }
        String[] domain = new String[sections.size()];
        for (int x = 0; x < domain.length; ++x) {
            domain[x] = sections.get(x);
        }
        return domain;
    }

    /**
     * same, but used when there's compression and we need to find the domain from earlier in the message.
     * This method should make a ByteArrayInputStream that starts at the specified byte and call the
     * other version of this method
     * @param firstByte - location of the first byte of the domain name in the byte array[]
     * @return String array containing the sections of the domain name
     */
    String[] readDomainName(int firstByte) throws IOException {
        return readDomainName(new ByteArrayInputStream(this.completeMessage, firstByte, this.completeMessage.length));
    }

    /**
     * Makes the response from the request message which will contain the question header
     * build a response based on the request and the answers you intend to send back.
     * @param request - The original request the header and questions will be populated from
     * @param answers - The answers from the cache or the google response
     * @return - Initialized response message
     */
    static DNSMessage buildResponse(DNSMessage request, DNSRecord[] answers) throws IOException {
        DNSMessage response = new DNSMessage();
        response.domainNameLocation = new HashMap<>();
        response.completeMessage = request.toBytes();
        response.questions = request.questions;
        response.answers = answers;
        response.header = DNSHeader.buildResponseHeader(request, response);
        response.additionalRecord = request.additionalRecord;
        return response;
    }

    /**
     * get the bytes to put in a packet and send back
     * @return - byte array containing this message's data
     */
    byte[] toBytes() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        // Write all of the bytes to the stream
        header.writeBytes(stream); // HEADER

        for (DNSQuestion q : questions) { // QUESTIONS
            q.writeBytes(stream, domainNameLocation); // the question
        }

        for (DNSRecord r : answers) { // ANSWERS
            r.writeBytes(stream, domainNameLocation);
        }

        for (byte b : additionalRecord) { // Additional Records
            stream.write(b);
        }

        // convert bytes in stream to array and return
        byte[] bytes = stream.toByteArray();

        return bytes;
    }

    /**
     * If this is the first time we've seen this domain name in the packet, write it using the DNS encoding
     * (each segment of the domain prefixed with its length, 0 at the end), and add it to the hash map. Otherwise,
     * write a back pointer to where the domain has been seen previously.
     * @param byteStream - stream the domain name information will be written to for use in toBytes
     * @param domainLocations - domain name String / stream location pairs for this messages stream
     * @param domainPieces - String sections of the domain name to check and write
     */
    static void writeDomainName(ByteArrayOutputStream byteStream, HashMap<String,Integer> domainLocations, String[] domainPieces) {
        if (domainPieces.length > 0) {
            String assembledDomainName = octetsToString(domainPieces);
            int location = byteStream.size();  //bytestream get length
            if (!domainLocations.containsKey(assembledDomainName)) { // have not seen this domain before
                for (String section : domainPieces) {
                    char[] sectionChars = section.toCharArray();
                    byteStream.write(sectionChars.length); // prefixing the section length
                    for (char c : sectionChars) {
                        byteStream.write((byte) c);
                    }
                }
                byteStream.write(0x00); // terminator
                domainLocations.put(assembledDomainName, location);
            } else {
                int domainLoc = domainLocations.get(assembledDomainName);
                byte leadingSix = (byte) ((domainLoc >> 8) & 0x3f); // prefixed byte
                byte followingEight = (byte) (domainLoc & 0xff);

                byteStream.write(0xc0 | leadingSix);
                byteStream.write(followingEight);
            }
        }
    }

    /**
     * join the pieces of a domain name with dots ([ "utah", "edu"] -> "utah.edu" )
     * @param octets - the domain String sections to be joined together
     * @return - One string comprised of the passed domain name sections
     */
    static String octetsToString(String[] octets) {
        String joinedSections = "";
        for (int x = 0; x < octets.length-1; ++x) {
            joinedSections += (octets[x] + '.');
        }
        joinedSections += octets[octets.length-1];
        return joinedSections;
    }

    @Override
    public String toString() {
        return "DNSMessage{" +
                "completeMessage=" + Arrays.toString(completeMessage) +
                ", messageStream=" + messageStream +
                ", answers=" + Arrays.toString(answers) +
                ", header=" + header +
                ", question=" + Arrays.toString(questions) +
                '}';
    }

    public DNSRecord[] getAnswers() {
        return answers;
    }

    public DNSHeader getHeader() {
        return header;
    }

    public DNSQuestion[] getQuestions() {
        return questions;
    }
}
