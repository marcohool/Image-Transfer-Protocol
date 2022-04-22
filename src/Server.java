import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class Server {

    private static final int port = 8080;
    private static final PacketHandler packetHandler = new PacketHandler();
    private static Scanner scanner = new Scanner(System.in);
    private static HashMap<Short, ServerThread> openConnections = new HashMap<>();

    public static void main(String[] args) {

        // Initialise server datagram socket
        DatagramSocket serverSocket;
        try {
            serverSocket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        // Listen for incoming packets
        while (true) {
            Packet receivedPacket = packetHandler.receivePacket(serverSocket);

            // Check if thread is already assigned to client
            if (openConnections.containsKey(receivedPacket.getSourcePort())) {
                // Get thread to deal with packet
                openConnections.get(receivedPacket.getSourcePort()).handlePacket(receivedPacket);
            }

            // If is a new connection assign thread to client
            else {
                ServerThread serverThread = new ServerThread(serverSocket, port);
                openConnections.put(receivedPacket.getSourcePort(),serverThread);
                // Handle packet
                serverThread.handlePacket(receivedPacket);
            }
        }
    }
}
