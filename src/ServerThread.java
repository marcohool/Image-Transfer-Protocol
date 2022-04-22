import java.net.DatagramSocket;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class ServerThread{

    private ThreadState state;
    private final DatagramSocket serverSocket;
    private final int serverPort;
    private Packet lastPacketSent;
    private Scanner scanner = new Scanner(System.in);

    public ServerThread(DatagramSocket serverSocket, int serverPort) {
        this.state = ThreadState.AVAILABLE;
        this.serverSocket = serverSocket;
        this.serverPort = serverPort;
    }

    public void handlePacket(Packet packet) {
        new Thread(() -> {

            // If thread is available and packet is initiating 3-way handshake (syn bit = true, all other flags are false & data = null)
            if (state == ThreadState.AVAILABLE && packet.isSynBit() && !packet.isAckBit() && !packet.isFinBit() && packet.getData() == null) {
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
                state = ThreadState.HANDSHAKE_1_SENT;
            }

            if (state == ThreadState.HANDSHAKE_1_SENT && packet.isAckBit() && !packet.isFinBit() && !packet.isSynBit()
                    && packet.getSequenceNum() == lastPacketSent.getAckNumb() && packet.getAckNumb() == lastPacketSent.getSequenceNum() + 1) {
                state = ThreadState.CONNECTION_ESTABLISHED;
                System.out.println("HANDSHAKE 3/3");
                System.out.println("Sending data simulation ....");
                String f = scanner.next();
            }
        }).start();
    }

}
