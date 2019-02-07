import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class is the local cache. It should basically just have a HashMap<DNSQuestion, DNSRecord> in it.
 * You can just store the first answer for any question in the cache (a response for google.com might return 10 IP
 * addresses, just store the first one). This class should have methods for querying and inserting records into
 * the cache. When you look up an entry, if it is too old (its TTL has expired), remove it and return "not found."
 */

public class DNSCache {

    private HashMap<DNSQuestion, DNSRecord> domainCache;

    /**
     * Cache constructor. Allocate memory for the cache HashMap.
     */
    public DNSCache() {
        domainCache = new HashMap<>();
    }

    /**
     * Add a new record to the domain cache
     * @param domain - Name of domain to be added to the cache
     * @param ip - IPv4 address of the given domain record
     */
    public void addRecord(DNSQuestion domain, DNSRecord ip) {
       domainCache.put(domain, ip);
    }

    /**
     * Checks cache for designated key (domain)
     * FALSE: No matching entry in the cache
     * TRUE: Matching domain key found in the cache and not past its retirement value
     * @param domain - Domain name of the entry being queried
     * @return - Matching key was / was not found
     */
    public boolean cacheQuery(DNSQuestion domain) {
        if (!domainCache.containsKey(domain)) {
            return false;
        }
        if (domainCache.get(domain).timestampValid()) {
            return true;
        } else {
            System.out.println("Like tears...in the rain (Record RETIRED)");
            expungeRecord(domain);
            return false;
        }
    }

    /**
     * Returns the corresponding record of the passed DNSQuestion object
     * @param domain - DNSQuestion object containing the domain name
     * @return - record corresponding to the given DNSQuestion object
     */
    public DNSRecord[] pullRecord(DNSQuestion domain) {
        DNSRecord[] records = new DNSRecord[1];
        assert(domainCache.containsKey(domain)); // program shouldn't be trying to pull records it already knows aren't there
        records[0] = domainCache.get(domain);
        return records;
    }

    /**
     * Removes overdue record from the hashmap
     * @param domain - DNSQuestion key for the record entry to be deleted
     */
    private void expungeRecord(DNSQuestion domain) {
        domainCache.remove(domain); // RETIRED
    }
}
