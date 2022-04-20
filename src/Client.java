import java.io.IOException;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class Client {

    private static int serverPort = 8080;
    private static int clientPort;
    private static final String serverAddress = "localhost";
    private static DatagramSocket clientSocket;
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {

        // Initialise client socket
        try {
            clientSocket = new DatagramSocket();
            clientSocket.setBroadcast(true);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Initialise client port
        clientPort = ThreadLocalRandom.current().nextInt(0,32767);
        System.out.println("Client port: " + clientPort);


        // Initialise 3-way handshake
        Packet handshake1 = new Packet();
        handshake1.setSourcePort((short) clientPort);
        handshake1.setDestinationPort((short) serverPort);
        handshake1.setSynBit(true);
        handshake1.setSequenceNum(ThreadLocalRandom.current().nextInt(0,2147483647));
        sendPacket(handshake1);
        System.out.println("Handshake 1/3");

    }

    private static void sendPacket(Packet packet) {
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
            clientSocket.send(datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private static void startSender(int port) {
//        new Thread(() -> {
//
//            // Initialise datagram socket
//            try {
//                clientSocket = new DatagramSocket();
//                clientSocket.setBroadcast(true);
//            } catch (IOException e) {
//                e.printStackTrace();
//                return;
//            }
//
//            // Initialise datagram packet
//            while (true) {
//                String message = scanner.next();
//
//                byte[] data = message.getBytes();
//                try {
//                    DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), port);
//                    clientSocket.send(packet);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//        }).start();
//    }

//    private static void startListener(int port) {
//        new Thread(() -> {
//
//            // Initialise datagram socket
//            DatagramSocket socket;
//            try {
//                socket = new DatagramSocket(port);
//            } catch (IOException e) {
//                e.printStackTrace();
//                return;
//            }
//
//            // Read datagram packets
//            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
//            while (true) {
//                try {
//                    socket.receive(packet);
//                    System.out.println("-- Received from " + packet.getAddress() + " " + packet.getSocketAddress() + " --\n" + new String(packet.getData()));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }
}
