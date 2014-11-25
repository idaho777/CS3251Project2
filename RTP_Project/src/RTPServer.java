
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


	/**
	 * A method to close the server connection, 4-way handshake
	 */
	public void close()
	{		
		state = ServerState.CLOSE_WAIT1;
		byte[] arr = new byte[1024];
		receivePacket = new DatagramPacket(arr, arr.length);

		while (state != ServerState.CLOSE_WAIT2)
		{
			try
			{
				serverSocket.receive(receivePacket);
				RTPPacketHeader receiveHeader = getHeader(receivePacket);
				if (isValidPacketHeader(receiveHeader) && receiveHeader.isDie()){
					sendAckCloseState(receivePacket);
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

		//ACK has been sent, now to send a DIE
		while (state != ServerState.CLOSED)
		{
			try
			{
				sendDieCloseState();
				serverSocket.receive(receivePacket);
				RTPPacketHeader receiveHeader = getHeader(receivePacket);

				if(receiveHeader.isAck() && receiveHeader.isLast() && receiveHeader.isDie()){
					state = ServerState.CLOSED;
					serverSocket.close();
					System.out.println("Server has been shut down successfully");
				}

				//send FIN from server to client
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

	private void sendAckCloseState(DatagramPacket receivePacket) throws IOException
	{

		// Receive Packet
		RTPPacketHeader receiveHeader = getHeader(receivePacket);
		clientIpAddress = receivePacket.getAddress();

		// DIE Ack Header
		RTPPacketHeader dieAckHeader = new RTPPacketHeader();
		dieAckHeader.setSource(receiveHeader.getDestination());
		dieAckHeader.setDestination(receiveHeader.getSource());
		dieAckHeader.setChecksum(PRECHECKSUM);

		// Checksummed and DIE
		ackNum = receiveHeader.getSeqNum();
		seqNum++;
		dieAckHeader.setFlags(false, true, true, true); //ack, last flags on
		state = ServerState.CLOSE_WAIT2;

		/*else	// Resend, confused about this part ??????
		{
			dieAckHeader.setSeqNum(seqNum);
			dieAckHeader.setAckNum(0);
			dieAckHeader.setFlags(false, true, false, false);	// live, !die, !ack, !last
		}*/

		byte[] dieAckHeaderBytes = dieAckHeader.getHeaderBytes();

		DatagramPacket sendPacket = new DatagramPacket(dieAckHeaderBytes, dieAckHeaderBytes.length, clientIpAddress, clientPort);

		serverSocket.send(sendPacket);

	}

	private void sendDieCloseState() throws IOException{
		// Setup header for the DIE packet
		RTPPacketHeader dieHeader = new RTPPacketHeader();
		dieHeader.setSource(clientPort);
		dieHeader.setDestination(serverPort);
		dieHeader.setSeqNum(0); //should have last seq num 
		dieHeader.setAckNum(0); //?????? What should these be after the ACK
		dieHeader.setFlags(false, true, false, true); //setting DIE flag on
		dieHeader.setChecksum(PRECHECKSUM);
		byte [] headerBytes = dieHeader.getHeaderBytes();

		DatagramPacket diePacket = new DatagramPacket(headerBytes, headerBytes.length, clientIpAddress, clientPort);
		state = ServerState.LAST_ACK;

		serverSocket.send(diePacket);
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
