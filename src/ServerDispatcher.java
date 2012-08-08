import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ServerDispatcher extends Protocol
{
	public static void main(String args[]) {
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(PORT);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		int connectionNumber = 0;
		while (true) {
				System.out.println("Server waiting for connection " + ++connectionNumber);
				//Initiate transfer connection
				DatagramPacket requestToAccept = flagPacket(SYN);
				try {
					socket.receive(requestToAccept);
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (!isFlag(requestToAccept, SYN)) {
					continue;
				}
				new Thread(new Server(requestToAccept)).start();
		}
	}
}
