import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Client extends Protocol
{
	private static final String LOCALHOST = "localhost";
	String fileName = "";
	String filePath = "";
	String serverName;
	boolean transmissionDone = false;
	InetAddress IPAddress;
	int serverPort;

	public Client(String file) {
		this(file, LOCALHOST);
	}

	public Client(String file, String hostname) {
		this(file, hostname, PORT);
	}

	public Client(String file, String hostname, int port) {
		PORT = port;
		serverName = hostname;
		filePath = file;
		fileName = file.split(File.separator)[file.split(File.separator).length - 1];
		startClient();		
	}

	public void sendDataPacket(int segmentIndex, int segmentLength, byte[] dataSegment) {
		// Send a send a data packet with the supplied index, length and dataSegment
		byte[] segment = segmentWithHeader((byte)0, segmentIndex, segmentLength, dataSegment);

		DatagramPacket segmentPacket = new DatagramPacket(segment, segment.length, IPAddress, serverPort);
		DatagramPacket ackThatAccepted = flagPacket(ACK);
		sendPacketUntilReceived(segmentPacket, ackThatAccepted);
	}

	public void transmitData(FileInputStream in) throws IOException {
		// Begins transmitting data to the server
		int totalBytes = in.available();
		System.out.println("Total bytes = " + totalBytes);
		int totalSegments = totalBytes/DATA_SEGMENT_SIZE + 1;

		System.out.println("Total segments " + totalSegments);

		int segmentIndex = 0;

		//Send filename
		byte[] filenameBytes = fileName.getBytes();
		if (filenameBytes.length > MAX_SEGMENT_SIZE) {
			filenameBytes = Arrays.copyOfRange(filenameBytes, 0, MAX_SEGMENT_SIZE);
		}

		sendDataPacket(segmentIndex, filenameBytes.length, filenameBytes);

		segmentIndex++;
		//Transmit data
		while (segmentIndex <= totalSegments) {
			//Read segment from index
			byte[] dataSegment = new byte[DATA_SEGMENT_SIZE];
			int i;
			for (i = 0; i < DATA_SEGMENT_SIZE; i++) {
				int c;
				if ((c = in.read()) != -1) {
					dataSegment[i] = (byte)c;
				} else {
					break;
				}
			}
			sendDataPacket(segmentIndex, i, dataSegment);
			segmentIndex++;
		}
	}

	public void terminateConnection() {
		// Send FIN to terminate connection
		DatagramPacket endPacket = flagPacket(FIN);
		endPacket.setAddress(IPAddress);
		endPacket.setPort(serverPort);

		DatagramPacket ackThatAccepted = flagPacket(FIN);
		sendPacketUntilReceived(endPacket, ackThatAccepted);
	}

	public static void main(String args[]) {
		// Enabling execution from command-line
		run(args[0]);
	}

	public static void run(String filepath) {
		new Client(filepath);
	}

	public static void run(String filepath, String hostname) {
		new Client(filepath, hostname);
	}
	public static void run(String filepath, String hostname, int port) {
		new Client(filepath, hostname, port);
	}

	public void startClient() {
		FileInputStream in = null;
		try {
			in = new FileInputStream(filePath);

			socket = new DatagramSocket();
			IPAddress = InetAddress.getByName(serverName);

			//Initiate transfer connection

			DatagramPacket requestToSend = flagPacket(SYN);
			requestToSend.setAddress(IPAddress);
			requestToSend.setPort(PORT);
			DatagramPacket responseToAccept = flagPacket(SYN);//new DatagramPacket(responseData, responseData.length);
			sendPacketUntilReceived(requestToSend, responseToAccept);
			serverPort = responseToAccept.getPort();

			//Transmit Data
			transmitData(in);

			System.out.println("Terminating connection");
			terminateConnection();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
					socket.close();
				} catch (IOException ignored) {}
			}
		}
	}
}




