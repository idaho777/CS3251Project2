
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

	private static final int CHECKSUM = 13566144;
	private static final int PRECHECKSUM = 3251;
	
	private short serverPort, clientPort;
	private InetAddress serverIpAddress, clientIpAddress;
	
	private int windowSize;
	private DatagramSocket serverSocket;
	private DatagramPacket sendPacket, receivePacket;
	
	private ServerState state;
	private int seqNum, ackNum;
	
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
			serverSocket.setSoTimeout(10000);
		}
		catch (SocketException e)
		{
			e.printStackTrace();
		}
		byte[] arr = new byte[1024];
		receivePacket = new DatagramPacket(arr, arr.length);
		
		state = ServerState.LISTEN;
		while (state != ServerState.ESTABLISHED)
		{
			try
			{
				serverSocket.receive(receivePacket);
				
				if (state == ServerState.LISTEN)
				{
					System.out.println(state);
					listenState(receivePacket);
				}
				else if (state == ServerState.LIVE_RCVD)
				{
					System.out.println(state);
					liveReceivedState(receivePacket);
				}
			}
			catch (SocketTimeoutException s)
			{
				System.out.println("Check for terminate");
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	
	private void listenState(DatagramPacket receivePacket) throws IOException
	{
		
		// Receive Packet
		RTPPacketHeader receiveHeader = getHeader(receivePacket);
		clientIpAddress = receivePacket.getAddress();
		
		// Live Ack Header
		RTPPacketHeader liveAckHeader = new RTPPacketHeader();
		liveAckHeader.setSource(receiveHeader.getDestination());
		liveAckHeader.setDestination(receiveHeader.getSource());
		liveAckHeader.setChecksum(PRECHECKSUM);
		// Checksummed and LIVE
		if (isValidPacketHeader(receiveHeader) && receiveHeader.isLive())
		{
			liveAckHeader.setSeqNum(0);
			liveAckHeader.setAckNum(0);
			liveAckHeader.setFlags(true, false, true, false);	// live, !die, !ack, !last

			clientPort = liveAckHeader.getDestination();
			state = ServerState.LIVE_RCVD;
		}
		else	// Resend
		{
			liveAckHeader.setSeqNum(0);
			liveAckHeader.setAckNum(0);
			liveAckHeader.setFlags(true, false, false, false);	// live, !die, !ack, !last
		}

		byte[] liveAckHeaderBytes = liveAckHeader.getHeaderBytes();
		
		DatagramPacket sendPacket = new DatagramPacket(liveAckHeaderBytes, liveAckHeaderBytes.length, clientIpAddress, clientPort);
		serverSocket.send(sendPacket);
	}
	
	
	public void liveReceivedState(DatagramPacket receivePacket) throws IOException
	{
		RTPPacketHeader receiveHeader = getHeader(receivePacket);
		clientIpAddress = receivePacket.getAddress();
		
		int receiveSeqNum = receiveHeader.getSeqNum();
		
		// Live Ack Header
		RTPPacketHeader liveAckHeader = new RTPPacketHeader();
		liveAckHeader.setSource(receiveHeader.getDestination());
		liveAckHeader.setDestination(receiveHeader.getSource());
		liveAckHeader.setChecksum(PRECHECKSUM);
		
		// Checksummed and LIVE and LAST
		if (isValidPacketHeader(receiveHeader) && receiveHeader.isLive() && receiveHeader.isLast())
		{
			ackNum = receiveHeader.getSeqNum();
			liveAckHeader.setSeqNum(seqNum);
			liveAckHeader.setAckNum(ackNum + 1);
			liveAckHeader.setFlags(true, false, true, true);	// live, !die, ack, last
			
			state = ServerState.ESTABLISHED;
		}
		else	// Resend
		{
			liveAckHeader.setSeqNum(0);
			liveAckHeader.setAckNum(0);
			liveAckHeader.setFlags(true, false, false, true);	// live, !die, !ack, last
		}

		byte[] liveAckHeaderBytes = liveAckHeader.getHeaderBytes();
		
		DatagramPacket sendPacket = new DatagramPacket(liveAckHeaderBytes, liveAckHeaderBytes.length, clientIpAddress, liveAckHeader.getDestination());
		serverSocket.send(sendPacket);
	}

	
	public void close()
	{
		
	}
	
	
	private boolean isValidPacketHeader(RTPPacketHeader header)
	{
		int headerChecksumed = CheckSum.getChecksum(header.getChecksum());
		
		return headerChecksumed == CHECKSUM;
	}
	
	private RTPPacketHeader getHeader(DatagramPacket receivePacket)
	{
		return new RTPPacketHeader(Arrays.copyOfRange(receivePacket.getData(), 0, 20));
	}
	
/*
	public void openSession1212()
	{
		// Setup Sockets and Receiving Packet
		DatagramPacket packet = null;
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(serverPort, serverIpAddress);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		packet = new DatagramPacket(arr, arr.length);

		
		// Server Listening for Packets
		System.out.println("Server Waiting");
		state = ServerState.LISTEN;
		while (state != ServerState.ESTABLISHED)
		{
			try
			{
				socket.receive(packet);
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}	
			
			
	
		}
		System.out.println("Exit openSession()");
	}
*/
}
