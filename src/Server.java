import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Server {

    private static final int port = 8080;
    private static DatagramSocket serverSocket;
    private static final int bufferSize = 512;

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


            // Buffer holds byteArray of content of packet
            byte[] buffer = new byte[bufferSize];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // Read datagram packets
            while (true) {
                try {
                    serverSocket.receive(packet);
                    System.out.println("-- RECEIVED PACKET --");
                    Packet receivedPacket = new Packet(buffer);
                    System.out.println("Source port: " + receivedPacket.getSourcePort());
                    System.out.println("Dest port: " + receivedPacket.getDestinationPort());
                    System.out.println("Sequence numb: " + receivedPacket.getSequenceNum());
                    System.out.println("Ack numb: " + receivedPacket.getAckNumb());
                    System.out.println("Ack bit: " + receivedPacket.isAckBit());
                    System.out.println("Syn bit: " + receivedPacket.isSynBit());
                    System.out.println("Fin bit: " + receivedPacket.isFinBit());
                    System.out.println("Data : " + Arrays.toString(receivedPacket.getData()));
                    System.out.println("\n");

                    // If packet is initiating 3-way handshake (syn bit = true & data = null)
                    if (receivedPacket.isSynBit() && receivedPacket.getData() == null) {
                        System.out.println("1/3");
                        // Send ack = true, syn = true, random synNum and ackNum = seqNum + 1
                        Packet handshake2 = new Packet();
                        handshake2.setAckBit(true);
                        handshake2.setSynBit(true);
                        handshake2.setSequenceNum(ThreadLocalRandom.current().nextInt(0,2147483647));
                        handshake2.setAckNumb(receivedPacket.getSequenceNum()+1);
                        // Send packet
                        PacketHandler packetHandler = new PacketHandler();
                        packetHandler.sendPacket(handshake2, serverSocket);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
