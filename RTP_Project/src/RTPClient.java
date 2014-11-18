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
	
	private static final int CHECKSUM = 1000;
	
	private ClientState state;
	private int windowSize;
	private short clientPort, serverPort;
	private InetAddress clientIpAddress, serverIpAddress;
	
	private DatagramSocket clientSocket;
	
	private int timeout = 10000;	// milliseconds
	
	public RTPClient(){
		
		
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
		// Setup Initializing Header
		RTPPacketHeader liveHeader = new RTPPacketHeader();
		liveHeader.setSource(clientPort);
		liveHeader.setDestination(serverPort);
		liveHeader.setSeqNum(0);
		liveHeader.setAckNum(0);
		liveHeader.setFlags(true, false, false, false); //setting LIVE flag on
		liveHeader.setChecksum(CHECKSUM);
		byte [] headerBytes = liveHeader.getHeaderBytes();
		
		// setup socket
		SocketAddress clientSocketAddress = new InetSocketAddress(clientIpAddress, clientPort);
		try {
			clientSocket = new DatagramSocket(clientSocketAddress);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		// LIVE Packet
		DatagramPacket setupPacket = new DatagramPacket(headerBytes, headerBytes.length, serverIpAddress, liveHeader.getDestination());
		byte[] receiveMessage = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);
		
		// Sending LIVE packet and receiving ACK
		
		int retries = 0;
		try {
			clientSocket.setSoTimeout(timeout);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		
		boolean receivedValidAck = false;
		while (retries < 3 && !receivedValidAck)
		{
			try
			{
				clientSocket.send(setupPacket);
				clientSocket.receive(receivePacket);
				System.out.println("received");
				if (receivePacket.getAddress().equals(serverIpAddress))
				{
					System.out.println("SameSocketAdd");
					RTPPacketHeader receiveHeader = new RTPPacketHeader(Arrays.copyOfRange(receivePacket.getData(), 0, 20));
					if (receiveHeader.getChecksum() == CHECKSUM && receiveHeader.isLive() && receiveHeader.isAck())
					{
						receivedValidAck = true;	
					}
					retries++;
				}
			}
			catch (SocketTimeoutException e0)
			{
				retries++;
				System.out.println("retry #" + retries);
			}
			catch (IOException e1) 
			{
				e1.printStackTrace();
			}	
		}
		
		if (!receivedValidAck)
		{
			System.out.println("Unsuccessful Connection");
			return;
		}
		RTPPacketHeader ackHeader = new RTPPacketHeader();
        ackHeader.setSource(clientPort);
        ackHeader.setDestination(serverPort);
        ackHeader.setSeqNum(0);
        ackHeader.setAckNum(0);
        ackHeader.setFlags(false, false, true, false); //setting ACK flag on
        ackHeader.setChecksum(CHECKSUM);
		byte[] ackHeaderBytes = ackHeader.getHeaderBytes();
        
		DatagramPacket ackPacket = new DatagramPacket(ackHeaderBytes, ackHeaderBytes.length, serverIpAddress, ackHeader.getDestination());
		System.out.println("Received packet");
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
	
}
