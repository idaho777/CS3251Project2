
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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
	private DatagramSocket serverSocket;
	private DatagramPacket sendPacket, receivePacket;
	
	private ServerState state;
	
	public RTPServer()
	{
		serverPort = 3252;
		try {
			serverIpAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		state = ServerState.CLOSED;
		System.out.println("Server IP: " + serverIpAddress);
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
		System.out.println("Server IP: " + serverIpAddress);
	}
	
	public RTPServer(short sourcePort, String ipAddress, short destPort){

	}
	
	
	public void openSession()
	{
		try
		{
			serverSocket = new DatagramSocket(serverPort, serverIpAddress);
			serverSocket.setSoTimeout(1000);
		}
		catch (SocketException e)
		{
			e.printStackTrace();
		}
		byte[] arr = new byte[1024];
		receivePacket = new DatagramPacket(arr, arr.length);
		
		state = ServerState.LISTEN;
		while (true)
		{
			try
			{
				serverSocket.receive(packet);
			}
			catch (SocketTimeoutException s)
			{
				System.out.println("Check for terminate");
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			
			// State is in Listen
			if (state == ServerState.LISTEN)
			{
				
			}
			else if (state == ServerState.LIVE_RCVD)
			{
				System.out.println("Live_Received State");
				RTPPacketHeader header = new RTPPacketHeader(Arrays.copyOfRange(packet.getData(), 0, 20));

				// Check if correct packet
				// Checksum
				if (header.getDestination() == serverPort)
				{
					state = ServerState.ESTABLISHED;
				}
			}
			
			
			
			
		}
	}
	
	
	private void listenState()
	{
		System.out.println("Listen State, Recieved Packet");
		clientIpAddress = packet.getAddress();
		clientPort = (short) packet.getPort();
		
		// Live Ack Header
		RTPPacketHeader header = new RTPPacketHeader(Arrays.copyOfRange(packet.getData(), 0, 20));
		RTPPacketHeader liveAckHeader = new RTPPacketHeader();
		liveAckHeader.setSource(header.getDestination());
		liveAckHeader.setDestination(header.getSource());
		liveAckHeader.setSeqNum(0);
		liveAckHeader.setAckNum(0);
		liveAckHeader.setFlags(true, false, true, false);
		liveAckHeader.setChecksum(1000);
		byte[] liveAckHeaderBytes = liveAckHeader.getHeaderBytes();
		
		//Sending Syn Ack package
		DatagramPacket sendPacket = new DatagramPacket(liveAckHeaderBytes, liveAckHeaderBytes.length, clientIpAddress, liveAckHeader.getDestination());
		
		serverSocket.send(sendPacket);
		state = ServerState.LIVE_RCVD;
	}
	
//	public void openSession1212()
//	{
//		// Setup Sockets and Receiving Packet
//		DatagramPacket packet = null;
//		DatagramSocket socket = null;
//		try {
//			socket = new DatagramSocket(serverPort, serverIpAddress);
//		} catch (SocketException e) {
//			e.printStackTrace();
//		}
//		
//		packet = new DatagramPacket(arr, arr.length);
//
//		
//		// Server Listening for Packets
//		System.out.println("Server Waiting");
//		state = ServerState.LISTEN;
//		while (state != ServerState.ESTABLISHED)
//		{
//			try
//			{
//				socket.receive(packet);
//			} 
//			catch (IOException e)
//			{
//				e.printStackTrace();
//			}	
//			
//			
//	
//		}
//		System.out.println("Exit openSession()");
//	}
	
	public void close()
	{
		
	}
	
	
	private boolean validatePacketHeader(RTPPacketHeader header)
	{
		//checksum
		
		//port
		//
		
		return false;
	}
}
