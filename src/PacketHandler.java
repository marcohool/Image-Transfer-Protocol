import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

public class PacketHandler {

    private static final String address = "localhost";
    private static final int serverPort = 8080;
    static Scanner scanner = new Scanner(System.in);

    public PacketHandler() {
    }

    public void sendPacket(Packet packet, DatagramSocket socket) {
        byte[] buffer = packet.toByteArray();
        DatagramPacket datagramPacket;

        // Initialise datagram packet to be sent to server
        try {
            System.out.println("Sending packet from " + packet.getSourcePort() + " to " + packet.getDestinationPort());
            datagramPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(address), packet.getDestinationPort());
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        // Send the packet
        try {
            socket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Packet receivePacket(DatagramSocket socket) {
        byte[] buffer = new byte[512];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        Packet receivedPacket = new Packet();

        try {
            socket.receive(packet);
            System.out.println("\n -- RECEIVED PACKET --");
            receivedPacket = new Packet(buffer);
            System.out.println("Source port: " + receivedPacket.getSourcePort());
            System.out.println("Dest port: " + receivedPacket.getDestinationPort());
            System.out.println("Sequence numb: " + receivedPacket.getSequenceNum());
            System.out.println("Ack numb: " + receivedPacket.getAckNumb());
            System.out.println("Ack bit: " + receivedPacket.isAckBit());
            System.out.println("Syn bit: " + receivedPacket.isSynBit());
            System.out.println("Fin bit: " + receivedPacket.isFinBit());
            System.out.println("Data : " + Arrays.toString(receivedPacket.getData()));
            System.out.println("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return receivedPacket;
    }

    public int getServerPort() {
        return serverPort;
    }
}
