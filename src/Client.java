import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Client {

  private static DatagramSocket clientSocket;
  private static int clientPort;
  private static final PacketHandler packetHandler = new PacketHandler();
  static ArrayList<Byte> imageBytes = new ArrayList<>();

  public static void main(String[] args) {

    // Initialise client port
    clientPort = ThreadLocalRandom.current().nextInt(0, 32767);

    // Initialise client socket
    try {
      clientSocket = new DatagramSocket(clientPort);
      clientSocket.setBroadcast(true);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    // Initialise 3-way handshake
    Packet handshake1 =
        new Packet((short)clientPort, (short)packetHandler.getServerPort(),
                   ThreadLocalRandom.current().nextInt(0, 2147483647), 0, false,
                   true, false, new byte[0]);
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

        // If checksum is incorrect (lost data in transmission)
        if (!receivedPacket.isCheckSumCorrect()) {
          System.out.println(
              "Incorrect checksum, waiting for re-transmission of packet");
          continue;
        }

        // If server responds with ack = true, ack_num = 1, other flags = 0,
        // ackNumb = receivedPacket seqNum + 1
        if (receivedPacket.isAckBit() && receivedPacket.isSynBit() &&
            !receivedPacket.isFinBit() && receivedPacket.getData() == null) {
          System.out.println("Handshake 2/3 completed");
          Packet handshake3 = new Packet(
              (short)clientPort, (short)packetHandler.getServerPort(),
              receivedPacket.getAckNumb(), receivedPacket.getSequenceNum() + 1,
              true, false, false, new byte[0]);
          packetHandler.sendPacket(handshake3, clientSocket);
        }
        // If is data packet
        if (receivedPacket.isAckBit() && !receivedPacket.isFinBit() &&
            !receivedPacket.isSynBit() && receivedPacket.getData() != null) {
          // Get data
          for (Byte b : receivedPacket.getData()) {
            imageBytes.add(b);
          }
          // Reply with ACK
          Packet ack = new Packet(
              (short)clientPort, (short)packetHandler.getServerPort(),
              receivedPacket.getAckNumb(),
              receivedPacket.getSequenceNum() + receivedPacket.getData().length,
              true, false, false, new byte[0]);
          packetHandler.sendPacket(ack, clientSocket);
        }

        // If server is closing connection
        if (receivedPacket.isFinBit() && !receivedPacket.isSynBit() &&
            receivedPacket.isAckBit() && receivedPacket.getData() == null) {
          // Image is fully sent
          imageBytesArray = new byte[imageBytes.size()];
          for (int i = 0; i < imageBytes.size(); i++) {
            imageBytesArray[i] = imageBytes.get(i);
          }

          // Send finACK
          Packet finAck = new Packet(
              (short)clientPort, (short)packetHandler.getServerPort(),
              receivedPacket.getAckNumb(), receivedPacket.getSequenceNum(),
              true, false, false, new byte[0]);
          packetHandler.sendPacket(finAck, clientSocket);

          // Send fin bit
          Packet fin = new Packet(
              (short)clientPort, (short)packetHandler.getServerPort(),
              receivedPacket.getAckNumb(), receivedPacket.getSequenceNum() + 1,
              true, false, true, new byte[0]);
          packetHandler.sendPacket(fin, clientSocket);

          // Close connection
          clientSocket.close();
          System.out.println("Connection closed");
          break;
        }
      }
      displayImage(imageBytesArray);
    })
        .start();
  }

  private static void displayImage(byte[] image) {

    InputStream inputStream = new ByteArrayInputStream(image);

    BufferedImage img;
    try {

      img = ImageIO.read(inputStream);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    ImageIcon icon = new ImageIcon(img);
    JFrame frame = new JFrame();
    frame.setLayout(new FlowLayout());
    frame.setSize(500, 500);
    JLabel lbl = new JLabel();
    lbl.setIcon(icon);
    frame.add(lbl);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }
}
