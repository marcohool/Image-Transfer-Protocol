import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class Peer {

    public static void main(String[] args) {

        startListener();
        startSender();

    }

    private static void startSender() {
        new Thread(() -> {

            // Initialise datagram socket
            DatagramSocket socket;
            try {
                socket = new DatagramSocket();
                socket.setBroadcast(true);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            // Initialise datagram packet
            while (true) {
                Scanner scanner = new Scanner(System.in);
                String message = scanner.next();

                byte[] data = message.getBytes();
                try {
                    DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 9999);
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }

    private static void startListener() {
        new Thread(() -> {

            // Initialise datagram socket
            DatagramSocket socket;
            try {
                socket = new DatagramSocket(9999);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            // Read datagram packets
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            while (true) {
                try {
                    socket.receive(packet);
                    System.out.println("-- Received from " + packet.getAddress() + " " + packet.getSocketAddress() + " --\n" + new String(packet.getData()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
