import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class DNSHeader {

    private byte[] fullHeader;

    private int requestID;

    private int questionCount;

    private int answerCount;


    /**
     * read the header from an input stream (we'll use a ByteArrayInputStream but we will only use the basic read
     * methods of input stream to read 1 byte, or to fill in a byte array, so we'll be generic).
     * @param stream - InputStream the header will be read from
     * @return decoded header
     */
    public static DNSHeader decodeHeader(InputStream stream) throws IOException {
        DNSHeader header = new DNSHeader();
        header.fullHeader = new byte[12];

        for (int i = 0; i < 12; ++i) {
            header.fullHeader[i] = (byte) stream.read();
        }

        header.questionCount = header.fullHeader[4] << 8;
        header.questionCount |= header.fullHeader[5];

        header.answerCount = header.fullHeader[6] << 8;
        header.answerCount |= header.fullHeader[7];

        header.requestID = header.fullHeader[0] << 8;
        header.requestID |= header.fullHeader[1];

        return header;
    }

    /**
     * This will create the header for the response. It will copy some fields from the request
     * @param request - request message to parse
     * @param response - response message that will use this header
     * @return header with response message
     */
    public static DNSHeader buildResponseHeader(DNSMessage request, DNSMessage response) throws IOException {
        DNSHeader resHeader = new DNSHeader();
        resHeader.fullHeader = new byte[12];

        // pull entire header from the request (REDUNDANT)
        byte[] reqBytes = request.getHeader().fullHeader;
        for (int i = 0; i < resHeader.fullHeader.length; ++i) {
            resHeader.fullHeader[i] = reqBytes[i];
        }

        resHeader.questionCount = resHeader.fullHeader[4] << 8;
        resHeader.questionCount |= resHeader.fullHeader[5];

        resHeader.answerCount = resHeader.fullHeader[6] << 8;
        resHeader.answerCount |= resHeader.fullHeader[7];

        resHeader.requestID = resHeader.fullHeader[0] << 8;
        resHeader.requestID |= resHeader.fullHeader[1];

        resHeader.answerCount = response.getAnswers().length;

       // Masking off of the messageCount from the response to add to the header
        byte ans2 = (byte) (resHeader.answerCount & 0xff);
        byte ans1 = (byte) ((resHeader.answerCount >> 8) & 0xff);
        resHeader.fullHeader[6] =  ans1;
        resHeader.fullHeader[7] =  ans2;

        resHeader.fullHeader[2] = (byte) 0x81; // Flip the QR bit so it denotes this as a response message
        resHeader.fullHeader[3] = (byte) 0x80;

        return resHeader;
    }

    /**
     * encode the header to bytes to be sent back to the client. The OutputStream interface has methods to write a
     * single byte or an array of bytes.
     * @param stream - stream to send response to client
     */
    public void writeBytes(OutputStream stream) throws IOException {
        stream.write(fullHeader);
    }

    /**
     * Return a human readable string version of a header object.
     * @return - String representation of the header
     */
    @Override
    public String toString() {
        return "DNSHeader{" +
                "fullHeader=" + Arrays.toString(fullHeader) +
                ", requestID=" + requestID +
                ", questionCount=" + questionCount +
                ", answerCount=" + answerCount +
                '}';
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public int getAnswerCount() {
        return answerCount;
    }


}
