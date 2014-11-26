
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 
 * @author Eileen Wang, Joonho Kim
 *
 */
public class RTPServer {

	private static final int CHECKSUM 		= 13566144;
	private static final int PRECHECKSUM 	= 3251;
	private static final int PACKET_SIZE	= 1024;
	private static final int DATA_SIZE		= 1004;
	private static final int HEADER_SIZE 	= 20;

	private short serverPort, clientPort;
	private InetAddress serverIpAddress, clientIpAddress;

	private int windowSize;
	private DatagramSocket serverSocket;
	private DatagramPacket sendPacket, receivePacket;

	private ServerState state;
	private int seqNum, ackNum;
	private String pathName="";
	private ArrayList<byte[]> bytesReceived;

	public RTPServer()
	{
		bytesReceived = new ArrayList<byte []> ();
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
		bytesReceived = new ArrayList<byte []> ();
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

		bytesReceived = new ArrayList<byte []> ();
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
		// handshake
		while (state != ServerState.CLOSED)
		{
			try
			{
				// Receive Packet
				serverSocket.receive(receivePacket);
				
				// Get Header of Packet and 
				RTPPacketHeader receiveHeader = getHeader(receivePacket);
				if (clientIpAddress == null) {
					clientIpAddress = receivePacket.getAddress();
				} else {
					// incorrect Client IP Address
					if (!receivePacket.getAddress().equals(clientIpAddress)) {
						continue;
					}
				}
				
				// Checksum validation
				if (!isValidPacketHeader(receiveHeader))
				{
					// is not Valid Packet, send back
					// set same flags but ack is false
					// send same packet as received
					// so client will resend with same everything
					continue;
				}
				
				
				// ==== Packet is valid
				if (!receiveHeader.isLive() && !receiveHeader.isDie() && !receiveHeader.isAck())
				{
					System.out.println("I have received a data packet");
					receiveDataPacket(receivePacket);
				}
				else if (receiveHeader.isLive() && !receiveHeader.isDie() && !receiveHeader.isAck() && !receiveHeader.isLast())
				{
					handShakeOne(receivePacket);
					System.out.println("Exit");
				}
				else if (receiveHeader.isLive() && !receiveHeader.isDie() && !receiveHeader.isAck() && receiveHeader.isLast())
				{
					handShakeTwo(receivePacket);
				}
				else if (!receiveHeader.isLive() && receiveHeader.isDie() && !receiveHeader.isAck() && !receiveHeader.isLast())
				{
					close();
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


	private void handShakeOne(DatagramPacket receivePacket) throws IOException
	{

		// Receive Packet
		RTPPacketHeader receiveHeader = getHeader(receivePacket);
		clientIpAddress = receivePacket.getAddress();
		clientPort = receiveHeader.getSource();
		
		// Live Ack Header
		RTPPacketHeader liveAckHeader = new RTPPacketHeader();
		liveAckHeader.setSource(serverPort);
		liveAckHeader.setDestination(clientPort);
		liveAckHeader.setChecksum(PRECHECKSUM);
		liveAckHeader.setSeqNum(0);
		liveAckHeader.setAckNum(0);
		liveAckHeader.setFlags(true, false, true, false);	// live, !die, !ack, !last

		state = ServerState.LIVE_RCVD;

		byte[] liveAckHeaderBytes = liveAckHeader.getHeaderBytes();

		DatagramPacket sendPacket = new DatagramPacket
				(
					liveAckHeaderBytes,
					liveAckHeaderBytes.length,
					clientIpAddress,
					clientPort
				);
		serverSocket.send(sendPacket);
	}

	public void handShakeTwo(DatagramPacket receivePacket) throws IOException
	{
//		int receiveSeqNum = receiveHeader.getSeqNum();

		// Live Ack Header
		RTPPacketHeader liveAckHeader = new RTPPacketHeader();
		liveAckHeader.setSource(serverPort);
		liveAckHeader.setDestination(clientPort);
		liveAckHeader.setChecksum(PRECHECKSUM);

//		ackNum = receiveHeader.getSeqNum();
//		liveAckHeader.setSeqNum(seqNum);
//		liveAckHeader.setAckNum(ackNum + 1);
		liveAckHeader.setSeqNum(0);
		liveAckHeader.setAckNum(0);
		
		liveAckHeader.setFlags(true, false, true, true);	// live, !die, ack, last

		state = ServerState.ESTABLISHED;

		byte[] liveAckHeaderBytes = liveAckHeader.getHeaderBytes();

		DatagramPacket sendPacket = new DatagramPacket
				(
					liveAckHeaderBytes,
					liveAckHeaderBytes.length,
					clientIpAddress,
					clientPort
				);
		serverSocket.send(sendPacket);
	}

	
	private void receiveDataPacket(DatagramPacket receivePacket) throws IOException
	{
		System.out.println("I am receiving the data.... wheeeee");
		RTPPacketHeader receiveHeader = getHeader(receivePacket);
		
		// extracts and adds data to ArrayList of byte[]s
		byte[] data = extractData(receivePacket);
		
		if(bytesReceived==null){
			System.out.println("YOU FOUND MEEE IM NULL");
		}
		bytesReceived.add(data);
		
		RTPPacketHeader dataAckHeader = new RTPPacketHeader();
		dataAckHeader.setSource(serverPort);
		dataAckHeader.setDestination(clientPort);
		dataAckHeader.setChecksum(PRECHECKSUM);
		dataAckHeader.setSeqNum(0);
		dataAckHeader.setAckNum(0);
		dataAckHeader.setFlags(false, false, true, false);
		
		if (receiveHeader.isLast())
		{
			dataAckHeader.setFlags(false, false, true, false);
		}
		
		byte[] liveAckHeaderBytes = dataAckHeader.getHeaderBytes();

		DatagramPacket sendPacket = new DatagramPacket
				(
					liveAckHeaderBytes,
					liveAckHeaderBytes.length,
					clientIpAddress,
					clientPort
				);
		serverSocket.send(sendPacket);
	}

	private byte[] extractData(DatagramPacket receivePacket)
	{
		//Check if packet is last
		RTPPacketHeader receiveHeader = getHeader(receivePacket);
		int data_length = receiveHeader.getWindow();
		
		byte[] extractedData = new byte[data_length];
		byte[] packet = receivePacket.getData();
		System.arraycopy(packet, HEADER_SIZE, extractedData, 0, data_length);
		
		System.out.println("Extracted data length: " + extractedData.length);
		return extractedData;
	}
	/**
	 * A method to close the server connection, 4-way handshake
	 */
	public void close()
	{		
		state = ServerState.CLOSE_WAIT1;
		byte[] arr = new byte[1024];
		receivePacket = new DatagramPacket(arr, arr.length);
		
		int tries = 0;
		while (state != ServerState.CLOSED)
		{
			try
			{
				sendAckCloseState();
				sendDieCloseState();
				serverSocket.receive(receivePacket);
				
				RTPPacketHeader receiveHeader = getHeader(receivePacket);
				
				// Checksum validation for data received from client
				if (!isValidPacketHeader(receiveHeader))
				{
					// is not Valid Packet, send back
					// set same flags but ack is false
					// send same packet as received
					// so client will resend with same everything
					continue;
				}
				
				if (!receiveHeader.isLive() && receiveHeader.isDie() && !receiveHeader.isAck() && !receiveHeader.isLast())
				{
					System.out.println("I am the cause of repeat");
					continue;
				}
				
				if (!receiveHeader.isLive() && receiveHeader.isDie() && receiveHeader.isAck() && receiveHeader.isLast())
				{
					state = ServerState.CLOSED;
					serverSocket.close();
				}
				
			}
			catch (SocketTimeoutException s)
			{
				System.out.println("Have not received DIE from Client for termination");
				if(tries++ >= 5){
					System.out.println("Teardown unstable connection");
					return;
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendAckCloseState() throws IOException
	{

		// DIE Ack Header
		RTPPacketHeader dieAckHeader = new RTPPacketHeader();
		dieAckHeader.setSource(clientPort);
		dieAckHeader.setDestination(serverPort);
		dieAckHeader.setChecksum(PRECHECKSUM);
		dieAckHeader.setSeqNum(0);
		dieAckHeader.setAckNum(0);
		dieAckHeader.setFlags(false, true, true, false); //ack, die flags on
		
		state = ServerState.CLOSE_WAIT2;

		byte[] dieAckHeaderBytes = dieAckHeader.getHeaderBytes();

		DatagramPacket sendPacket = new DatagramPacket
				(
					dieAckHeaderBytes,
					dieAckHeaderBytes.length,
					clientIpAddress,
					clientPort
				);

		serverSocket.send(sendPacket);

	}

	private void sendDieCloseState() throws IOException{
		// Setup header for the DIE packet
		RTPPacketHeader dieHeader = new RTPPacketHeader();
		dieHeader.setSource(clientPort);
		dieHeader.setDestination(serverPort);
		dieHeader.setSeqNum(10); //should have last seq num 
		dieHeader.setAckNum(0); //?????? What should these be after the ACK
		dieHeader.setFlags(false, true, false, true); //setting DIE flag on
		dieHeader.setChecksum(PRECHECKSUM);
		byte [] headerBytes = dieHeader.getHeaderBytes();

		DatagramPacket diePacket = new DatagramPacket
				(
					headerBytes,
					headerBytes.length,
					clientIpAddress,
					clientPort
				);
		
		state = ServerState.LAST_ACK;

		serverSocket.send(diePacket);
		System.out.println("Server DIE has been sent");
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

	
	public ServerState getServerState(){
		return state;
	}
	
	public int getWindowSize(){
		return windowSize;
	}
	
	public void setWindowSize(int window){
		this.windowSize=window;
	}
}
