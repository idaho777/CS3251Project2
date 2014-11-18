import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

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
	public void setup() throws IOException{
		// Setup Initializing Header
		RTPPacketHeader header = new RTPPacketHeader();
		header.setSource(clientPort);
		header.setDestination(serverPort);
		header.setSeqNum(0);
		header.setAckNum(0);
		header.setFlags(true, false, false, false); //setting LIVE flag on
		header.setChecksum(CHECKSUM);
		byte [] headerBytes = header.getHeaderBytes();
		
		// setup socket
		clientSocket = new DatagramSocket(clientPort, clientIpAddress);
			
		// LIVE Packet
		DatagramPacket setupPacket = new DatagramPacket(headerBytes, headerBytes.length, serverIpAddress, header.getDestination());
		
		// Send Packet
		System.out.println("Sending LIVE Packet");
		clientSocket.send(setupPacket);
		
		byte[] receiveMessage = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);
		
		clientSocket.receive(receivePacket);
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
