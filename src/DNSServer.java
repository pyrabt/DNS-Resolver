import java.io.IOException;
import java.net.*;

public class DNSServer {

    private DatagramSocket reqSock; // socket to read client requests from

    static DNSCache serverCache;

    private DatagramSocket googleDnsSock; // socket to forward requests to google from

    /**
     * Constructs DNSServer.
     * Initializes the socket port number
     * @throws SocketException
     */
    public DNSServer() throws SocketException {
       reqSock = new DatagramSocket(8053);
       googleDnsSock = new DatagramSocket(8054);
       serverCache = new DNSCache();
    }

    /**
     * Drives the server. Will perpetually listen for UDP DNS requests and then sends a response
     * after querying the cache.
     * @throws IOException
     */
    private void runService() throws IOException {
        while (true) {
            DatagramPacket request = new DatagramPacket(new byte[1000], 1000);
            reqSock.receive(request);
            DNSMessage reqMsg = DNSMessage.decodeMessage(request.getData());

            if (!serverCache.cacheQuery(reqMsg.getQuestions()[0])) {
                InetAddress googleIP = InetAddress.getByName("8.8.8.8");
                DatagramPacket forwardedRequest = new DatagramPacket(request.getData(), request.getLength(), googleIP, 53);
                googleDnsSock.send(forwardedRequest);
                DatagramPacket googleResponse = new DatagramPacket(new byte[1000], 1000);
                googleDnsSock.receive(googleResponse);
                DNSMessage googleMsg = DNSMessage.decodeMessage(googleResponse.getData());
                DNSMessage responseToClient = DNSMessage.buildResponse(reqMsg, googleMsg.getAnswers());
                byte[] responseData = responseToClient.toBytes();
                DatagramPacket resToClPacket = new DatagramPacket(responseData, responseData.length, request.getAddress(), request.getPort());
                reqSock.send(resToClPacket);
            } else {
                DNSMessage responseToClient = DNSMessage.buildResponse(reqMsg, serverCache.pullRecord(reqMsg.getQuestions()[0]));
                byte[] responseData = responseToClient.toBytes();
                DatagramPacket resToClPacket = new DatagramPacket(responseData, responseData.length, request.getAddress(), request.getPort());
                reqSock.send(resToClPacket);
            }
        }
    }


    public static void main(String [] args) {
        try {
           DNSServer server = new DNSServer();
            server.runService();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
