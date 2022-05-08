import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;

public class Server {

    private static final int port = 8080;
    private static final PacketHandler packetHandler = new PacketHandler();
    private static final HashMap<Short, ServerThread> openConnections = new HashMap<>(); // Key = source port, value = server thread

    public static void main(String[] args) throws SocketException {

        // Initialise server datagram socket
        DatagramSocket serverSocket;
        try {
            serverSocket = new DatagramSocket(port);
            serverSocket.setSoTimeout(2000);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        // Listen for incoming packets
        while (true) {
            Packet receivedPacket;
            try {
                receivedPacket = packetHandler.receivePacket(serverSocket);

                // Check if thread is already assigned to client
                if (openConnections.containsKey(receivedPacket.getSourcePort())) {
                    // Get thread to deal with packet
                    openConnections.get(receivedPacket.getSourcePort()).handlePacket(receivedPacket);
                }

                // If is a new connection assign thread to client
                else {
                    ServerThread serverThread = new ServerThread(serverSocket, port);
                    openConnections.put(receivedPacket.getSourcePort(), serverThread);
                    // Handle packet
                    serverThread.handlePacket(receivedPacket);
                }
            } catch (SocketTimeoutException e) {
                // If timeout has occurred - if any open serverthreads are expecting packets, resend the last sent packet in case it was lost in transit
                System.out.println("No packet received within " + serverSocket.getSoTimeout() + "ms - Resending any packets in transmit");
                for (ServerThread serverThread : openConnections.values()) {
                    // If the serverThread has sent a packet and is waiting for response
                    if (serverThread.getState() != ThreadState.LISTEN && serverThread.getState() != ThreadState.CLOSED) {
                        // Resend packets in transmit
                        System.out.println("Resending last packet");
                        serverThread.resendLastPacket();
                    }
                }
            }
        }
    }

    public static void removeConnection(ServerThread thread) {
        openConnections.values().remove(thread);
    }
}
