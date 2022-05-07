import java.io.*;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class ServerThread{

    public static int packetSize = 1024;
    public static int headerSize = 28;

    private ThreadState state;
    private final DatagramSocket serverSocket;
    private final int serverPort;

    private Packet lastPacketSent;
    private int packetsSent = 0;
    private byte[][] fullImageByteArray;

    public ServerThread(DatagramSocket serverSocket, int serverPort) {
        this.state = ThreadState.LISTEN;
        this.serverSocket = serverSocket;
        this.serverPort = serverPort;
    }

    public void handlePacket(Packet packet) {
        new Thread(() -> {
            PacketHandler packetHandler = new PacketHandler();

            // If checksum is incorrect (lost data in transmission)
            if (!packet.isCheckSumCorrect()) {
                System.out.println("Incorrect checksum, waiting for re-transmission of packet");
                return;
            }

            // If thread is available and packet is initiating 3-way handshake (syn bit = true, all other flags are false & data = null)
            if (state == ThreadState.LISTEN) {
                if (packet.isSynBit() && !packet.isAckBit() && !packet.isFinBit() && packet.getData() == null) {
                    System.out.println("Handshake 1/3 completed");
                    // Send ack = true, syn = true, random synNum and ackNum = seqNum + 1
                    Packet handshake2 = new Packet((short) serverPort, packet.getSourcePort(), ThreadLocalRandom.current().nextInt(0,2147483647), packet.getSequenceNum()+1, true, true, false, new byte[0]);
                    packetHandler.sendPacket(handshake2, serverSocket);
                    lastPacketSent = handshake2;
                    // Update thread state
                    state = ThreadState.SYN_SENT;
                    return;
                } else {
                    System.out.println("Unexpected packet - expected packet to initiate handshake");
                }
            }

            if (this.state == ThreadState.SYN_SENT ) {
                if (packet.isAckBit() && !packet.isFinBit() && !packet.isSynBit() && packet.getSequenceNum() == lastPacketSent.getAckNumb() && packet.getAckNumb() == lastPacketSent.getSequenceNum() + 1) {
                    this.state = ThreadState.ESTABLISHED;
                    System.out.println("HANDSHAKE 3/3");

                    // Decide on data segment size (TO-DO)

                    // Ready to start sending data
                    // Get image and split it into byte arrays
                    fullImageByteArray = getImageByteArray(packetSize-headerSize);

                    // Send first byte of image
                    sendImagePacket(packet, packetHandler);

                    return;
                } else {
                    System.out.println("Unexpected paket - expected packet to finalise handshake connection");
                }
            }

            if (this.state == ThreadState.ESTABLISHED) {
                // If incoming packet is an ACK
                if (packet.isAckBit() && !packet.isFinBit() && !packet.isSynBit()) {
                    // If ack number is correct
                    int phantomByte = 0;
                    if (lastPacketSent.getData().length == 0) {
                        phantomByte = 1;
                    }
                    int exp = lastPacketSent.getSequenceNum() + lastPacketSent.getData().length + phantomByte;
                    System.out.println("expected ack numb = " + exp);
                    if (packet.getAckNumb() == lastPacketSent.getSequenceNum() + lastPacketSent.getData().length + phantomByte) {
                        // Send next packet
                        // If not all packets have been sent
                        if (!(packetsSent >= fullImageByteArray.length)) {
                            sendImagePacket(packet, packetHandler);
                        } else {
                            // All image data packets have been sent
                            // Send FIN bit
                            this.state = ThreadState.FIN_WAIT_1;
                            Packet finPacket = new Packet((short) serverPort, lastPacketSent.getDestinationPort(), lastPacketSent.getAckNumb(), lastPacketSent.getSequenceNum()+1, true, false, true, new byte[0]);
                            packetHandler.sendPacket(finPacket, serverSocket);
                            lastPacketSent = finPacket;
                        }
                    } else {
                        System.out.println("Invalid packet - wrong ack number");
                    }
                } else {
                    System.out.println("Expected an ACK packet");
                }
                return;
            }

            // Waiting for FIN ACK
            if (this.state == ThreadState.FIN_WAIT_1) {
                if (!packet.isFinBit() && packet.isAckBit() && !packet.isSynBit() && packet.getData() == null) {
                    this.state = ThreadState.FIN_WAIT_2;
                } else {
                    System.out.println("Expected a FIN ACK packet");
                }
                return;
            }

            // Waiting for FIN
            if (this.state == ThreadState.FIN_WAIT_2) {
                if (packet.isFinBit() && !packet.isSynBit() && packet.isAckBit() && packet.getData() == null) {
                    Packet finAck = new Packet((short) serverPort, lastPacketSent.getDestinationPort(), packet.getAckNumb(), packet.getSequenceNum() + 1, true, false, true, new byte[0]);
                    packetHandler.sendPacket(finAck, serverSocket);
                    this.state = ThreadState.CLOSED;
                } else {
                    System.out.println("Expected FIN packet");
                }
            }

            // If connection is closed
            if (this.state == ThreadState.CLOSED) {
                System.out.println("Closed");
                Server.removeConnection(this);
            }

        }).start();
    }

    private void sendImagePacket(Packet previousPacket, PacketHandler handler) {
        Packet imagePacket = new Packet((short) serverPort, previousPacket.getSourcePort(), previousPacket.getAckNumb(), previousPacket.getSequenceNum(), true, false, false, fullImageByteArray[packetsSent]);
        lastPacketSent = imagePacket;
        handler.sendPacket(imagePacket, serverSocket);
        packetsSent++;
    }

    private byte[][] getImageByteArray(int bytesPerDataSegment) {
        FileInputStream fileInputStream;
        File image = new File("assets/lpfcawayoflife.jpg");
        byte[] wholeImage = new byte[(int) image.length()];
        int totPacketsToBeSent = (int) Math.ceil((double) wholeImage.length/bytesPerDataSegment);
        byte[][] splitImageArray = new byte[totPacketsToBeSent][bytesPerDataSegment];

        try {
            fileInputStream  = new FileInputStream(image);
            fileInputStream.read(wholeImage);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        for (int i = 0; i < totPacketsToBeSent; i++) {
            splitImageArray[i] = Arrays.copyOfRange(wholeImage, i*bytesPerDataSegment,i*bytesPerDataSegment+bytesPerDataSegment);
        }
        return splitImageArray;
    }

}
