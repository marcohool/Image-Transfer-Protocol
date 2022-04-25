import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class Client {

    private static DatagramSocket clientSocket;
    private static int clientPort;
    private static final PacketHandler packetHandler = new PacketHandler();
    static ArrayList<Byte> imageBytes = new ArrayList<>();
    private static int sequenceNumber;

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
            byte[] imageBytesArray;
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

                    handshake3.setSourcePort((short) clientPort);
                    handshake3.setDestinationPort((short) packetHandler.getServerPort());
                    packetHandler.sendPacket(handshake3, clientSocket);

                }
                // If is data packet
                if (receivedPacket.isAckBit() && !receivedPacket.isFinBit() && !receivedPacket.isSynBit() && receivedPacket.getData() != null) {
                    // Get data
                    for (Byte b : receivedPacket.getData()) {
                        imageBytes.add(b);
                    }
                    // Reply with ACK
                    Packet ack = new Packet();
                    ack.setSourcePort((short) clientPort);
                    ack.setDestinationPort((short) packetHandler.getServerPort());
                    ack.setAckBit(true);
                    sequenceNumber = receivedPacket.getAckNumb();
                    ack.setSequenceNum(sequenceNumber);
                    ack.setAckNumb(receivedPacket.getSequenceNum() + receivedPacket.getData().length);
                    packetHandler.sendPacket(ack, clientSocket);
                }

                // If server is closing connection
                if (receivedPacket.isFinBit() && !receivedPacket.isSynBit() && receivedPacket.isAckBit() && receivedPacket.getData() == null) {
                    // Image is fully sent
                    imageBytesArray = new byte[imageBytes.size()];
                    for (int i = 0; i < imageBytes.size(); i++) {
                        imageBytesArray[i] = imageBytes.get(i);
                    }
                    // Send finACK
                    PacketHandler packetHandler = new PacketHandler();
                    Packet finAck = new Packet();
                    finAck.setSourcePort((short) clientPort);
                    finAck.setDestinationPort((short) packetHandler.getServerPort());
                    finAck.setAckBit(true);
                    finAck.setSequenceNum(receivedPacket.getAckNumb());
                    finAck.setAckNumb(receivedPacket.getSequenceNum());
                    packetHandler.sendPacket(finAck, clientSocket);
                    // Send fin bit
                    Packet fin = new Packet();
                    fin.setSourcePort((short) clientPort);
                    fin.setDestinationPort((short) packetHandler.getServerPort());
                    fin.setFinBit(true);
                    fin.setAckBit(true);
                    fin.setSequenceNum(receivedPacket.getSequenceNum());
                    fin.setAckNumb(receivedPacket.getSequenceNum() + 1);
                    packetHandler.sendPacket(fin, clientSocket);
                    // Close connection
                    clientSocket.close();
                    break;
                }

            }
            displayImage(imageBytesArray);

        }).start();
    }

        private static void displayImage(byte[] image) {

        InputStream inputStream = new ByteArrayInputStream(image);

        BufferedImage img= null;
        try {
            img = ImageIO.read(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ImageIcon icon=new ImageIcon(img);
        JFrame frame=new JFrame();
        frame.setLayout(new FlowLayout());
        frame.setSize(500,500);
        JLabel lbl=new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
