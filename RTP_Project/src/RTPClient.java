import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * 
 * @author Eileen Wang, Joonho Kim
 *
 */
public class RTPClient {
	
	private static final int CHECKSUM = 13566144;
	private static final int PRECHECKSUM = 3251;
	
	private ClientState state;
	
	private short clientPort, serverPort;
	private InetAddress clientIpAddress, serverIpAddress;
	private DatagramSocket clientSocket;
	
	private int timeout = 10000;	// milliseconds
		
	private byte[] window = new byte[0xFFFF];
	private int seqNum, ackNum;
	private int windowSize;
	
	public RTPClient() {
		this.clientPort=3251;
		this.serverPort=3252;
		try {
			this.clientIpAddress = InetAddress.getLocalHost();
			this.serverIpAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		state = ClientState.CLOSED;
	}
	
	public RTPClient(short clientPort, String serverIpAddress, short serverPort){
		this.clientPort=clientPort;
		this.serverPort=serverPort;
		try {
			this.clientIpAddress = InetAddress.getLocalHost();
			this.serverIpAddress = InetAddress.getByName(serverIpAddress);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		state = ClientState.CLOSED;
	}
	
	/**
	 * performs handshake
	 * @throws IOException 
	 */
	public void setup()
	{
		// setup socket
		try {
			clientSocket = new DatagramSocket(clientPort, clientIpAddress);
		} catch (SocketException e) {
			e.printStackTrace();
		}

		byte[] receiveMessage = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);
		
		// Setup Initializing Header
		RTPPacketHeader liveHeader = new RTPPacketHeader();
		liveHeader.setSource(clientPort);
		liveHeader.setDestination(serverPort);
		liveHeader.setSeqNum(0);
		liveHeader.setAckNum(0);
		liveHeader.setFlags(true, false, false, false); //setting LIVE flag on
		liveHeader.setChecksum(PRECHECKSUM);
		byte [] headerBytes = liveHeader.getHeaderBytes();
		
		DatagramPacket setupPacket = new DatagramPacket(headerBytes, headerBytes.length, serverIpAddress, serverPort);
		
		// Sending LIVE packet and receiving ACK
		
		try {
			clientSocket.setSoTimeout(timeout);
		} catch (SocketException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


		int tries = 0;
		state = ClientState.LIVE_SENT;
		while (tries < 5 && state != ClientState.SERVER_ACK_SENT)
		{
			try
			{
				clientSocket.send(setupPacket);
				clientSocket.receive(receivePacket);

				if (!receivePacket.getAddress().equals(serverIpAddress)){
					continue;
				}

				setupPacket = liveSentState(receivePacket);
			}
			catch (SocketTimeoutException s)
			{
				System.out.println("Timeout, resend");
				tries++;
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (state != ClientState.SERVER_ACK_SENT)
		{
			System.out.println("Unsuccessful Connection");
			return;
		}
		
		
		tries = 0;
		while (tries < 5 && state != ClientState.ESTABLISHED)
		{
			try
			{
				clientSocket.send(setupPacket);
				clientSocket.receive(receivePacket);
				
				if (!receivePacket.getAddress().equals(serverIpAddress)){
					continue;
				}

				serverAckSentState(receivePacket);
			}
			catch (SocketTimeoutException s)
			{
				System.out.println("Timeout, resend");
				tries++;
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		
		if (state != ClientState.ESTABLISHED)
		{
			System.out.println("Unsuccessful Connection");
			return;
		}
			
		System.out.println("exit setup()");
	}
	
	private DatagramPacket liveSentState(DatagramPacket receivePacket) throws IOException
	{
		RTPPacketHeader receiveHeader = getHeader(receivePacket);

		RTPPacketHeader ackHeader = new RTPPacketHeader();
		ackHeader.setSource(clientPort);
		ackHeader.setDestination(serverPort);
		ackHeader.setChecksum(PRECHECKSUM);
		if (isValidPacketHeader(receiveHeader) && receiveHeader.isLive() && receiveHeader.isAck())
		{
			ackHeader.setSeqNum(seqNum);
	        ackHeader.setAckNum(0);
	        ackHeader.setFlags(true, false, false, true);
	        
			state = ClientState.SERVER_ACK_SENT;
		}
		else
		{
			ackHeader.setSeqNum(0);
	        ackHeader.setAckNum(0);
	        ackHeader.setFlags(true, false, false, false);
		}
		byte[] ackHeaderBytes = ackHeader.getHeaderBytes();
		
		DatagramPacket ackPacket = new DatagramPacket(ackHeaderBytes, ackHeaderBytes.length, serverIpAddress, serverPort);	
		clientSocket.send(ackPacket);
		return ackPacket;
	}
	
	
	private void serverAckSentState(DatagramPacket receivePacket) throws IOException
	{	
		// Wring server IP address
		if (!receivePacket.getAddress().equals(serverIpAddress)){
			return;
		}

		RTPPacketHeader receiveHeader = getHeader(receivePacket);

		RTPPacketHeader ackHeader = new RTPPacketHeader();
		ackHeader.setSource(clientPort);
		ackHeader.setDestination(serverPort);
		ackHeader.setChecksum(PRECHECKSUM);
		if (isValidPacketHeader(receiveHeader) && receiveHeader.isLive() && receiveHeader.isAck() && receiveHeader.isLast())
		{
			ackNum = receiveHeader.getSeqNum();
			seqNum++;
			state = ClientState.ESTABLISHED;
			return;
		}
		else
		{
			ackHeader.setSeqNum(seqNum);
	        ackHeader.setAckNum(0);
	        ackHeader.setFlags(true, false, false, true);
		}
		byte[] ackHeaderBytes = ackHeader.getHeaderBytes();
		
		DatagramPacket ackPacket = new DatagramPacket(ackHeaderBytes, ackHeaderBytes.length, serverIpAddress, serverPort);	
		clientSocket.send(ackPacket);
	}
	
	
	
	
	
	
	
	/**
	 * Starts sending data transfer
	 */
	public void startTransfer(){
		
	}
	
	/**
	 * Stops the data transfer
	 */
	public void stopTransfer(){
		
	}
	
	/**
	 * Once data transfer stops, performs connection teardown
	 */
	public void teardown(){
		
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
}
