import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Client {

    private static DatagramSocket clientSocket;
    private static int clientPort;
    private static final PacketHandler packetHandler = new PacketHandler();

    public static void main(String[] args) {

        // Initialise client port
        clientPort = ThreadLocalRandom.current().nextInt(0, 32767);
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

        // Start listener and wait for response from server
        startListener();

    }

    private static void startListener() {
        new Thread(() -> {

            // Read datagram packets
            while (true) {
                Packet receivedPacket = packetHandler.receivePacket(clientSocket);
                // If server responds with ack = true, ack_num = 1, other flags = 0, ackNumb = receivedPacket seqNum + 1
                if (receivedPacket.isAckBit() && receivedPacket.isSynBit() && !receivedPacket.isFinBit() && receivedPacket.getData() == null) {
                    System.out.println("Handshake 2/3 completed");
                    Packet handshake3 = new Packet();
                    handshake3.setAckBit(true);
                    handshake3.setSequenceNum(receivedPacket.getAckNumb());
                    handshake3.setAckNumb(receivedPacket.getSequenceNum() + 1);

                    PacketHandler handler = new PacketHandler();
                    handshake3.setSourcePort((short) clientPort);
                    handshake3.setDestinationPort((short) handler.getServerPort());
                    handler.sendPacket(handshake3, clientSocket);
                }

            }

        }).start();
    }
}
