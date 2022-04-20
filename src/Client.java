import java.io.IOException;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class Client {

    private static int clientPort;
    private static DatagramSocket clientSocket;

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
        PacketHandler packetHandler = new PacketHandler();
        handshake1.setSourcePort((short) clientPort);
        handshake1.setDestinationPort((short) packetHandler.getServerPort());
        handshake1.setSynBit(true);
        handshake1.setSequenceNum(ThreadLocalRandom.current().nextInt(0,2147483647));
        packetHandler.sendPacket(handshake1, clientSocket);
        System.out.println("Handshake 1/3");

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
