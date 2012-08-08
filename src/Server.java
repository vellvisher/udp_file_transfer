import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Server extends Protocol implements Runnable
{
	FileOutputStream out;
	String fileName = "";
	String SERVER_NAME = "localhost";
	final String UPLOAD_DIRECTORY = System.getProperty("user.dir") + File.separator
			+ "uploads";
	boolean notTerminated = true;
	DatagramPacket synPacket;
	int currentSegmentIndex;
	public Server(DatagramPacket requestToAccept) {
		synPacket = requestToAccept;
	}

	public void processPacket(DatagramPacket packet) throws IOException {
		// Function which determines whether the packet is data or the filename and processes accordingly
		byte[] packetData = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());
		if (packetData[2] == 0 && packetData[1] == 0) {
			fileName = new String(segmentWithoutHeader(packetData));
			try {
				if (!new File(UPLOAD_DIRECTORY).exists()) {
					new File(UPLOAD_DIRECTORY).mkdir();
				}
				out = new FileOutputStream(UPLOAD_DIRECTORY + File.separator + fileName);
			} catch (FileNotFoundException ignored) {
				ignored.printStackTrace();
			}
			System.out.println("Saving to file " + fileName);
		} else {
			processData(segmentWithoutHeader(packetData));
		}
	}

	public void processData(byte[] data) throws IOException {
		// Processes data by writing it to the file
		out.write(data);
	}

	public void run() {
		// Main point of execution for the thread
		FileOutputStream out = null;

		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}

		try {
			notTerminated = true;

			DatagramPacket synAckPacket = flagPacket(SYN);
			synAckPacket.setAddress(synPacket.getAddress());
			synAckPacket.setPort(synPacket.getPort());

			sendPacket(synAckPacket);

			byte[] dataToAccept = new byte[MAX_SEGMENT_SIZE];
			DatagramPacket dataPacket = new DatagramPacket(dataToAccept, dataToAccept.length);
			currentSegmentIndex = 0;
			while(notTerminated) {
				dataPacket = new DatagramPacket(dataToAccept, dataToAccept.length);
				DatagramPacket ackPacket = getAcknowledgementPacket(currentSegmentIndex, synPacket);
				socket.receive(dataPacket);

				if (isFlag(dataPacket, FIN)) {
					System.out.println("Terminating connection");
					notTerminated = false;
					DatagramPacket finAckPacket = flagPacket(FIN);
					finAckPacket.setAddress(synPacket.getAddress());
					finAckPacket.setPort(synPacket.getPort());
					sendPacket(finAckPacket);
				} else if (isFlag(dataPacket, SYN)) {
					//System.out.println("SYN Flag again!");
					DatagramPacket reSynPacket = getAcknowledgementPacket(0, synPacket);
					sendPacket(reSynPacket);
				} else if (equalAcknowledgement(dataPacket, ackPacket)) {
					//System.out.println("Equal ack. Processing data");
					processPacket(dataPacket);
					currentSegmentIndex++;
					ackPacket = getAcknowledgementPacket(currentSegmentIndex, synPacket);
					sendPacket(ackPacket);
				} else {
					//System.out.println("Sending dup ack with seg" + currentSegmentIndex);
					DatagramPacket dupAckPacket = getAcknowledgementPacket(currentSegmentIndex, synPacket);
					sendPacket(dupAckPacket);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.close();
					socket.close();
				} catch (IOException ignored) {}
			}
		}
	}
}