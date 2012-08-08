import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;


public class Protocol {
	final static int MAX_SEGMENT_SIZE = 65451;
	final static int SEGMENT_INDEX_SIZE = 2;
	final static int SEGMENT_LENGTH = 2;
	final static int FLAG_SIZE = 1;
	final static int HEADER_SIZE = SEGMENT_INDEX_SIZE + SEGMENT_LENGTH + FLAG_SIZE;
	final static int DATA_SEGMENT_SIZE = MAX_SEGMENT_SIZE - HEADER_SIZE;
	final static int TIME_OUT = 5000;
	final static byte SYN = (byte) 1;
	final static byte FIN = (byte) 2;
	final static byte ACK = (byte) 3;
	static int PORT = 9000;
    boolean notReceived;
	DatagramSocket socket;

	public static  DatagramPacket flagPacket(byte... flags) {
		// Make a packet from supplied flag
		byte flag = flags[0];
		if (flag == ACK) {
			if (flags.length == 3) {
				return new DatagramPacket(new byte[]{flag, flags[1], flags[2]}, FLAG_SIZE + SEGMENT_INDEX_SIZE);
			}
			return new DatagramPacket(new byte[]{flag, 0, 0}, FLAG_SIZE + SEGMENT_INDEX_SIZE);
		} else {
			return new DatagramPacket(new byte[]{flag}, FLAG_SIZE);
		}
	}

	public static boolean isFlag(DatagramPacket packet, byte flag) {
		// Check if the packet is of the type of flag
		return (packet != null && packet.getData()[0] == flag);
	}

	
	// Thread which continuously sends the packets
	class SendThread implements Runnable {
		DatagramPacket sendPacket;
		public SendThread(DatagramPacket sendPacket) {
			this.sendPacket = sendPacket;
		}
		@Override
		public void run() {
			while (notReceived) {
				try {
					Thread.sleep(TIME_OUT);
				} catch (InterruptedException e) {
					break;
				}
				sendPacket(sendPacket);
			}
		}
	}

	// Generates an ack packet with the supplied number
	public DatagramPacket getAcknowledgementPacket(int receivePacket, DatagramPacket receivedPacket) {

		DatagramPacket ackPacket = flagPacket(new byte[]{ACK, intToByteArray(receivePacket, 2)[0],
				intToByteArray(receivePacket, 2)[1]});
		ackPacket.setAddress(receivedPacket.getAddress());
		ackPacket.setPort(receivedPacket.getPort());

		return ackPacket;
	}

	public int getSequenceIndex(DatagramPacket packet) {
		return getSequenceIndex(packet.getData());
	}

	public int getSequenceIndex(byte[] data) {
		return byteToInt(Arrays.copyOfRange(data, 1, 3));
	}

	// Checks if the two packets have the same ACK number
	public boolean equalAcknowledgement(DatagramPacket sendPacket, DatagramPacket receivePacket) {
		if (sendPacket == null || receivePacket == null) {
			return false;
		}
		int sendSegment = getSequenceIndex(sendPacket);
		int receiveSegment = getSequenceIndex(receivePacket);
		return (receiveSegment == sendSegment);
	}

	// Checks for a valid acknowledgement
	public boolean validAcknowledgement(DatagramPacket sendPacket, DatagramPacket receivePacket) {
		if (sendPacket == null || receivePacket == null) {
			return false;
		}
		int sendSegment = getSequenceIndex(sendPacket);
		int receiveSegment = getSequenceIndex(receivePacket);
		return (receiveSegment > sendSegment);
	}

	// Sends a packet until the valid packet is received
	public void sendPacketUntilReceived(DatagramPacket sendPacket, DatagramPacket receivePacket) {
		boolean validPacket;

		do {
			validPacket = true;
			notReceived = true;
			sendPacket(sendPacket);
			Thread sendThread = new Thread(new SendThread(sendPacket));
			sendThread.start();
			try {
				socket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			sendThread.interrupt();
			notReceived = false;
			
			if (isFlag(receivePacket, ACK) && !validAcknowledgement(sendPacket, receivePacket)) {
				validPacket = false;
			}
		} while (!validPacket);
	}

	public void sendPacket(DatagramPacket sendPacket) {
		try {
			socket.send(sendPacket);
		} catch (IOException e) {
			System.err.println("Network unreachable");
			e.printStackTrace();
		}
	}

	// Converts int value to a byte array
	public final byte[] intToByteArray(int value, int size) {
		if (size == 0) {
			return null;
		}
		byte[] byteRep = new byte[] {
				(byte)(value >>> 24),
				(byte)(value >>> 16),
				(byte)(value >>> 8),
				(byte)(value)};
		return Arrays.copyOfRange(byteRep, byteRep.length - size, byteRep.length);
	}
	
	// Generates the header for the data segment
	public byte[] segmentWithHeader(byte flags, int index, int length, byte[] dataSegment) {
		byte[] segment = new byte[HEADER_SIZE + length];
		System.arraycopy(new byte[]{flags}, 0, segment, 0, 1);
		System.arraycopy(intToByteArray(index, 2), 0, segment, 1, 2);
		System.arraycopy(intToByteArray(length, 2), 0, segment, 3, 2);
		System.arraycopy(dataSegment, 0, segment, 5, length);
		return segment;
	}

	// Converts a 2 byte array into int
	public int byteToInt(byte[] num) {
		int number = (num[1]&127+(num[1]&128)) + ((num[0]&127+(num[0]&128))<<8);
		return number;
	}

	// Removes header from segment and retains only data
	public byte[] segmentWithoutHeader(byte[] dataSegment) {
		int length = byteToInt(Arrays.copyOfRange(dataSegment, 3, 5));
		int length2 = dataSegment.length - HEADER_SIZE;
		return Arrays.copyOfRange(dataSegment, HEADER_SIZE, HEADER_SIZE + length);
	}
}
