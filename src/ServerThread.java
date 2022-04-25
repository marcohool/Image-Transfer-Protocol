import java.io.*;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class ServerThread{

    private ThreadState state;
    private final DatagramSocket serverSocket;
    private final int serverPort;

    private Packet lastPacketSent;
    private int packetsSent = 0;
    private byte[][] fullImageByteArray;
    private final int packetDataSize = 536;

    public ServerThread(DatagramSocket serverSocket, int serverPort) {
        this.state = ThreadState.LISTEN;
        this.serverSocket = serverSocket;
        this.serverPort = serverPort;
    }

    public void handlePacket(Packet packet) {
        new Thread(() -> {

            // If thread is available and packet is initiating 3-way handshake (syn bit = true, all other flags are false & data = null)
            if (state == ThreadState.LISTEN) {
                if (packet.isSynBit() && !packet.isAckBit() && !packet.isFinBit() && packet.getData() == null) {
                    System.out.println("Handshake 1/3 completed");
                    // Send ack = true, syn = true, random synNum and ackNum = seqNum + 1
                    Packet handshake2 = new Packet();
                    handshake2.setSourcePort((short) serverPort);
                    handshake2.setDestinationPort(packet.getSourcePort());
                    handshake2.setAckBit(true);
                    handshake2.setSynBit(true);
                    handshake2.setSequenceNum(ThreadLocalRandom.current().nextInt(0,2147483647));
                    handshake2.setAckNumb(packet.getSequenceNum()+1);
                    // Send packet
                    PacketHandler packetHandler = new PacketHandler();
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
                if (packet.isAckBit() && !packet.isFinBit() && !packet.isSynBit()
                        && packet.getSequenceNum() == lastPacketSent.getAckNumb() && packet.getAckNumb() == lastPacketSent.getSequenceNum() + 1) {
                    this.state = ThreadState.ESTABLISHED;
                    System.out.println("HANDSHAKE 3/3");

                    // Decide on data segment size (TO-DO)

                    // Ready to start sending data
                    // Get image and split it into byte arrays
                    fullImageByteArray = getImageByteArray(packetDataSize);

                    // Send first byte of image
                    sendImagePacket(packet);

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
                    if (packet.getAckNumb() == lastPacketSent.getSequenceNum() + lastPacketSent.getData().length + phantomByte) {
                        // Send next packet
                        // If not all packets have been sent
                        if (!(packetsSent >= fullImageByteArray.length)) {
                            sendImagePacket(packet);
                        } else {
                            // All image data packets have been sent
                            System.out.println("All packets sent");
                            // Send FIN bit
                            this.state = ThreadState.FIN_WAIT_1;
                            Packet finPacket = new Packet();
                            finPacket.setSourcePort((short) serverPort);
                            finPacket.setDestinationPort(lastPacketSent.getDestinationPort());
                            finPacket.setSequenceNum(lastPacketSent.getAckNumb());
                            finPacket.setAckNumb(lastPacketSent.getSequenceNum()+1);
                            finPacket.setFinBit(true);
                            finPacket.setAckBit(true);
                            PacketHandler packetHandler = new PacketHandler();
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
                    Packet finAck = new Packet();
                    PacketHandler packetHandler = new PacketHandler();
                    finAck.setSourcePort((short) serverPort);
                    finAck.setDestinationPort(lastPacketSent.getDestinationPort());
                    finAck.setAckBit(true);
                    finAck.setFinBit(true);
                    finAck.setSequenceNum(packet.getAckNumb());
                    finAck.setAckNumb(packet.getSequenceNum() + 1);
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

    private void sendImagePacket(Packet previousPacket) {
        Packet imagePacket = new Packet();
        imagePacket.setAckBit(true);
        imagePacket.setSourcePort((short) serverPort);
        imagePacket.setDestinationPort(previousPacket.getSourcePort());
        imagePacket.setSequenceNum(previousPacket.getAckNumb());
        imagePacket.setAckNumb(previousPacket.getSequenceNum());
        imagePacket.setDataSegmentSize(packetDataSize);
        imagePacket.setData(fullImageByteArray[packetsSent]);
        lastPacketSent = imagePacket;
        PacketHandler packetHandler = new PacketHandler();
        packetHandler.sendPacket(imagePacket, serverSocket);
        packetsSent++;

    }

    private byte[][] getImageByteArray(int bytesPerPacket) {
        FileInputStream fileInputStream;
        File image = new File("assets/image.png");
        byte[] wholeImage = new byte[(int) image.length()];
        int totPacketsToBeSent = (int) Math.ceil((double) wholeImage.length/bytesPerPacket);
        byte[][] splitImageArray = new byte[totPacketsToBeSent][bytesPerPacket];

        try {
            fileInputStream  = new FileInputStream(image);
            fileInputStream.read(wholeImage);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        for (int i = 0; i < totPacketsToBeSent; i++) {
            splitImageArray[i] = Arrays.copyOfRange(wholeImage, i*bytesPerPacket,i*bytesPerPacket+bytesPerPacket);
        }
        return splitImageArray;
    }

}
