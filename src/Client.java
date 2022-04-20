import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Client {

    private static DatagramSocket clientSocket;
    private static final PacketHandler packetHandler = new PacketHandler();

    public static void main(String[] args) {

        // Initialise client port
        int clientPort = ThreadLocalRandom.current().nextInt(0, 32767);
        System.out.println("Client port: " + clientPort);

        // Initialise client socket
        try {
            clientSocket = new DatagramSocket(clientPort);
            clientSocket.setBroadcast(true);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Initialise 3-way handshake
        Packet handshake1 = new Packet();
        handshake1.setSourcePort((short) clientPort);
        handshake1.setDestinationPort((short) packetHandler.getServerPort());
        handshake1.setSynBit(true);
        handshake1.setSequenceNum(ThreadLocalRandom.current().nextInt(0,2147483647));
        packetHandler.sendPacket(handshake1, clientSocket);
        System.out.println("Handshake 1/3");

        // Start listener and wait for response from server
        startListener();

    }

    private static void startListener() {
        new Thread(() -> {

            // Read datagram packets
            while (true) {
                Packet receivedPacket = packetHandler.receivePacket(clientSocket);
            }

        }).start();
    }
}
