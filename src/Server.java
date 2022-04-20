import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Server {

    private static final int port = 8080;
    private static DatagramSocket serverSocket;
    private static final PacketHandler packetHandler = new PacketHandler();

    public static void main(String[] args) {

        // Initialise server datagram socket
        try {
            serverSocket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        // Start listening for packets
        startListener();

    }

    private static void startListener() {
        new Thread(() -> {

            // Read datagram packets
            while (true) {
                Packet receivedPacket = packetHandler.receivePacket(serverSocket);

                // If packet is initiating 3-way handshake (syn bit = true, all other flags are false & data = null)
                if (receivedPacket.isSynBit() && !receivedPacket.isAckBit() && !receivedPacket.isFinBit() && receivedPacket.getData() == null) {
                    System.out.println("Handshake 1/3 completed");
                    // Send ack = true, syn = true, random synNum and ackNum = seqNum + 1
                    Packet handshake2 = new Packet();
                    handshake2.setSourcePort((short) port);
                    handshake2.setDestinationPort(receivedPacket.getSourcePort());
                    handshake2.setAckBit(true);
                    handshake2.setSynBit(true);
                    handshake2.setSequenceNum(ThreadLocalRandom.current().nextInt(0,2147483647));
                    handshake2.setAckNumb(receivedPacket.getSequenceNum()+1);
                    // Send packet
                    PacketHandler packetHandler = new PacketHandler();
                    packetHandler.sendPacket(handshake2, serverSocket);
                }

            }

        }).start();
    }

}
