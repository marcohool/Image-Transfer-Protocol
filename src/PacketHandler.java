import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class PacketHandler {

    private static final String serverAddress = "localhost";
    private static final int serverPort = 8080;
    static Scanner scanner = new Scanner(System.in);

    public PacketHandler() {
    }

    public void sendPacket(Packet packet, DatagramSocket socket) {
        byte[] buffer = packet.toByteArray();
        DatagramPacket datagramPacket;

        // Initialise datagram packet to be sent to server
        try {
            datagramPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(serverAddress), serverPort);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        // Send the packet to the server
        try {
            socket.send(datagramPacket);
            String test = scanner.next();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getServerPort() {
        return serverPort;
    }
}
