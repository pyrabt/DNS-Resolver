/*
 * Everything after the header and question parts of the DNS message are stored as records.
 * This should have all the fields listed in the spec as well as a Date object storing when
 * this record was created by your program.
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;


public class DNSRecord {

    private LocalDateTime retirement;

    private String[] domainNames;

    private byte[] type;

    private byte[] mClass;

    private byte[] ttl;

    private byte[] RDlen;

    private byte[] ipBytes;

    /**
     * Constructor for new record from a DNS request or response message
     * @param iStream - byte stream the record information will be read from
     * @param dnsMessage - object that will be used as reference to read the domain name
     * @return - A fully initialized DNSRecord object
     */
    static DNSRecord decodeRecord(InputStream iStream, DNSMessage dnsMessage) throws IOException {
        DNSRecord newRecord = new DNSRecord();

        newRecord.domainNames = dnsMessage.readDomainName(iStream);
        newRecord.type = new byte[2];
        newRecord.mClass = new byte[2];
        newRecord.ttl = new byte[4];
        newRecord.RDlen = new byte[2];

        //get type of record
        iStream.read(newRecord.type, 0, 2);

        //get record class
        iStream.read(newRecord.mClass, 0, 2);

        // Pull the TLL
        iStream.read(newRecord.ttl, 0, 4);

        // RDLENGTH = ipv4/ipv6
        iStream.read(newRecord.RDlen, 0, 2);
        int RDval = newRecord.RDlen[0];
        RDval = (RDval << 8) | newRecord.RDlen[1];

        // IP Address
        newRecord.ipBytes = new byte[RDval];
        iStream.read(newRecord.ipBytes,0,newRecord.ipBytes.length);

        // planned obsolescence
        long lifeSpan = 0;
        lifeSpan |= newRecord.ttl[0] & 0xff;
        for (int t = 1; t < newRecord.ttl.length; ++t) {
            lifeSpan <<= 8;
            lifeSpan |= newRecord.ttl[t] & 0xff;
        }
        System.out.println("Lifespan: " + lifeSpan);
        newRecord.retirement = LocalDateTime.now().plusSeconds(lifeSpan);

        return newRecord;
    }

    /**
     * Writes all of the record information to a byte stream to be utilized when building a response message
     * @param outputStream - Byte stream the data of this record will be written to and used by DNSMessage toBytes
     * @param cacheRecord - A hashmap of String domain name / stream location pairs
     */
    void writeBytes(ByteArrayOutputStream outputStream, HashMap<String, Integer> cacheRecord) {
        DNSMessage.writeDomainName(outputStream, cacheRecord, domainNames);

        for (byte b : type) {
            outputStream.write(b);
        }

        for (byte b : mClass) {
            outputStream.write(b);
        }

        for (byte b : ttl) {
            outputStream.write(b);
        }

        for (byte b : RDlen) {
            outputStream.write(b);
        }

        for (byte b : ipBytes) {
            outputStream.write(b);
        }

    }

    public byte[] getIpBytes() {
        return ipBytes;
    }

    /**
     * @return - string representation of this object
     */
    @Override
    public String toString() {
        return "DNSRecord{" +
                "retirement=" + retirement +
                ", domainNames=" + Arrays.toString(domainNames) +
                ", type=" + Arrays.toString(type) +
                ", mClass=" + Arrays.toString(mClass) +
                ", ttl=" + Arrays.toString(ttl) +
                ", RDlen=" + Arrays.toString(RDlen) +
                ", ipBytes=" + Arrays.toString(ipBytes) +
                '}';
    }

    /**
     * return whether the creation date + the time to live is after the current time. The Date
     * and Calendar classes will be useful for this.
     * @return - whether record has been retired or not
     */
    boolean timestampValid() {
        return retirement.isAfter(LocalDateTime.now());
    }
}
