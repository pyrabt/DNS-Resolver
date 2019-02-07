import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;

public class DNSQuestion {

    private String[] domain;

    private byte[] qType;

    private byte[] qClass;

    /**
     * Read a question from the input stream. Due to compression, the parent may actually
     * be needed to read some of the fields.
     * @param stream - Stream of bytes containing the request question
     * @param message - message object to read the question section into
     * @return DNSQuestion object containing decoded question from the given request
     */
    static DNSQuestion decodeQuestion(InputStream stream, DNSMessage message) throws IOException {
        DNSQuestion newQ = new DNSQuestion();
        newQ.domain = message.readDomainName(stream);
        newQ.decodeQTypeClass(stream);
        return newQ;
    }

    /**
     * Reads the two unsigned 16 bit Ints denoting the type of record and the class of the question being asked
     * @param stream - packet byte stream being read from
     * @throws IOException
     */
    private void decodeQTypeClass(InputStream stream) throws IOException {
        this.qType = new byte[2];
        this.qClass = new byte[2];
        stream.read(this.qType, 0, 2);
        stream.read(this.qClass, 0, 2);
    }

    /**
     * Write the question bytes which will be sent to the client. The hash map is used
     * for us to compress the message.
     * @param arrayOutputStream - byte stream to client
     * @param domainNameLocations - names/ip to be sent to client
     */
    void writeBytes(ByteArrayOutputStream arrayOutputStream, HashMap<String, Integer> domainNameLocations) {
        DNSMessage.writeDomainName(arrayOutputStream, domainNameLocations, this.domain);
        for (byte b : this.qType) { // question type bytes
            arrayOutputStream.write(b);
        }
        for (byte b : this.qClass) { // question class bytes
            arrayOutputStream.write(b);
        }
    }

    @Override
    public String toString() {
        return "DNSQuestion{" +
                "domain=" + Arrays.toString(domain) +
                ", qType=" + Arrays.toString(qType) +
                ", qClass=" + Arrays.toString(qClass) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DNSQuestion that = (DNSQuestion) o;
        return Arrays.equals(domain, that.domain) &&
                Arrays.equals(qType, that.qType) &&
                Arrays.equals(qClass, that.qClass);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(domain);
        result = 31 * result + Arrays.hashCode(qType);
        result = 31 * result + Arrays.hashCode(qClass);
        return result;
    }
}
