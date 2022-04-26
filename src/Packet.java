import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Packet {

    // 24 bytes + data
    private final short sourcePort; // 2 bytes
    private final short destinationPort; // 2 bytes
    private final int sequenceNum; // 4 bytes (max value 2147483647)
    private final int ackNumb; // 4 bytes
    private final boolean ackBit; // 4 bytes (converted to integer 1 or 0)
    private final boolean synBit; // 4 bytes (converted to integer 1 or 0)
    private final boolean finBit; // 4 bytes (converted to integer 1 or 0)
    private byte[] data; // Variable size

    public Packet(short sourcePort, short destinationPort, int sequenceNum, int ackNumb, boolean ackBit, boolean synBit, boolean finBit, byte[] data) {
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.sequenceNum = sequenceNum;
        this.ackNumb = ackNumb;
        this.ackBit = ackBit;
        this.synBit = synBit;
        this.finBit = finBit;
        this.data = data;
    }

    // Create empty packet
    public Packet() {
        this( (short) 0, (short) 0, 0, 0, false, false, false, new byte[0]);
    }

    // Create populated packet from byteArray (used when packet is received from input stream)
    public Packet(byte[] byteArray) {
        byte[] trimmedByteArray = trimByteArray(byteArray);
        this.sourcePort =  ByteBuffer.wrap(Arrays.copyOfRange(trimmedByteArray, 0, 2)).getShort();
        this.destinationPort = ByteBuffer.wrap(Arrays.copyOfRange(trimmedByteArray, 2, 4)).getShort();
        this.sequenceNum = ByteBuffer.wrap(Arrays.copyOfRange(trimmedByteArray, 4, 8)).getInt();
        this.ackNumb = ByteBuffer.wrap(Arrays.copyOfRange(trimmedByteArray, 8, 12)).getInt();
        this.ackBit = (ByteBuffer.wrap(Arrays.copyOfRange(trimmedByteArray, 12, 16)).getInt() == 1);
        this.synBit = (ByteBuffer.wrap(Arrays.copyOfRange(trimmedByteArray, 16, 20)).getInt() == 1);
        this.finBit = (ByteBuffer.wrap(Arrays.copyOfRange(trimmedByteArray, 20, 24)).getInt() == 1);
        if (trimmedByteArray.length > 24) {
            this.data = (Arrays.copyOfRange(byteArray, 24, byteArray.length));
        }
    }


    // Convert Packet to byteArray in order to be sent to output stream
    public byte[] toByteArray() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // 2 bytes to "source port"
        ByteBuffer sourcePortBuffer = ByteBuffer.allocate(2);
        sourcePortBuffer.putShort(this.sourcePort);

        // 2 bytes to "destination port"
        ByteBuffer destPortBuffer = ByteBuffer.allocate(2);
        destPortBuffer.putShort(this.destinationPort);

        // 4 bytes to "sequence number"
        ByteBuffer seqNumBuffer = ByteBuffer.allocate(4);
        seqNumBuffer.putInt(this.sequenceNum);

        // 4 bytes to "ack number"
        ByteBuffer ackNumBuffer = ByteBuffer.allocate(4);
        ackNumBuffer.putInt(this.ackNumb);

        // 4 bytes to "ack Bit"
        ByteBuffer ackBitBuffer = ByteBuffer.allocate(4);
        if (this.ackBit) {
            ackBitBuffer.putInt(1);
        } else {
            ackBitBuffer.putInt(0);
        }

        // 4 bytes to "syn Bit"
        ByteBuffer synBitBuffer = ByteBuffer.allocate(4);
        if (this.synBit) {
            synBitBuffer.putInt(1);
        } else {
            synBitBuffer.putInt(0);
        }

        // 4 bytes to "fin Bit"
        ByteBuffer finBitBuffer = ByteBuffer.allocate(4);
        if (this.finBit) {
            finBitBuffer.putInt(1);
        } else {
            finBitBuffer.putInt(0);
        }

        try {
            byteArrayOutputStream.write(sourcePortBuffer.array());
            byteArrayOutputStream.write(destPortBuffer.array());
            byteArrayOutputStream.write(seqNumBuffer.array());
            byteArrayOutputStream.write(ackNumBuffer.array());
            byteArrayOutputStream.write(ackBitBuffer.array());
            byteArrayOutputStream.write(synBitBuffer.array());
            byteArrayOutputStream.write(finBitBuffer.array());
            byteArrayOutputStream.write(this.data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArrayOutputStream.toByteArray();
    }

    private byte[] trimByteArray(byte[] array) {
        int i = array.length - 1;
        while (i >= 24 && array[i] == 0)
        {
            --i;
        }

        return Arrays.copyOf(array, i + 1);
    }

    public short getSourcePort() {
        return sourcePort;
    }

    public short getDestinationPort() {
        return destinationPort;
    }

    public int getSequenceNum() {
        return sequenceNum;
    }

    public int getAckNumb() {
        return ackNumb;
    }

    public boolean isAckBit() {
        return ackBit;
    }

    public boolean isSynBit() {
        return synBit;
    }

    public boolean isFinBit() {
        return finBit;
    }

    public byte[] getData() {
        return data;
    }
}
