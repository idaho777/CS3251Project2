
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

	private static final int CHECKSUM = 1000;
	
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
	
	public void openSession()
	{
		DatagramPacket packet = null;
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(serverPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		byte[] arr = new byte[1024];
		
		System.out.println("Server Waiting");
		packet = new DatagramPacket(arr, arr.length);
		
		try {
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Received Packet");
		clientIpAddress = packet.getAddress();
		clientPort = (short) packet.getPort();
		
		RTPPacketHeader header = new RTPPacketHeader(Arrays.copyOfRange(packet.getData(), 0, 20));
		
		RTPPacketHeader liveAckHeader = new RTPPacketHeader();
		liveAckHeader.setSource(header.getDestination());
		liveAckHeader.setDestination(header.getSource());
		liveAckHeader.setSeqNum(0);
		liveAckHeader.setAckNum(0);
		liveAckHeader.setFlags(true, false, true, false);
		liveAckHeader.setChecksum(1000);
		
		byte[] liveAckHeaderBytes = liveAckHeader.getHeaderBytes();
		
		DatagramPacket sendPacket = new DatagramPacket(liveAckHeaderBytes, liveAckHeaderBytes.length, clientIpAddress, liveAckHeader.getDestination());
		System.out.println(sendPacket.getSocketAddress());
		try {
			socket.send(sendPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
//		try {
//			Thread.sleep(30000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	public void close()
	{
		
	}
}
