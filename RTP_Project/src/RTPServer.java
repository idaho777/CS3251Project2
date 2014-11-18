
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * 
 * @author Eileen Wang, Joonho Kim
 *
 */
public class RTPServer {

	private short serverPort, clientPort;
	private InetAddress serverIpAddress, clientIpAddress;
	
	private int windowSize;
	private DatagramSocket socket;
	
	private ServerState state;
	
	public RTPServer()
	{
		
	}
	
	public RTPServer(short sourcePort)
	{
		serverPort = sourcePort;
		try {
			serverIpAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		state = ServerState.CLOSED;
	}
	
	public void openSession() throws IOException
	{
		DatagramPacket packet = null;
		DatagramSocket socket = new DatagramSocket(serverPort);
		byte[] arr = new byte[2000];
		
		System.out.println("Server Waiting");
		packet = new DatagramPacket(arr, arr.length);
		socket.receive(packet);
		
		clientIpAddress = packet.getAddress();
		clientPort = (short) packet.getPort();
		
		RTPPacketHeader header = new RTPPacketHeader(Arrays.copyOfRange(packet.getData(), 0, 20));
		
		String receivedMsg = new String(packet.getData());
		System.out.println(receivedMsg);
		
		DatagramPacket sendPacket = new DatagramPacket(arr, windowSize, clientIpAddress, windowSize);
		
//		socket.send(p);
	}
	
	public void close()
	{
		
	}
}
