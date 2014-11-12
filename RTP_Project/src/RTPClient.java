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
	
	private short srcPort, destPort;
	private InetAddress destIpAddress, srcIpAddress;
	private int windowSize;
	private DatagramSocket socket;
	
	public RTPClient(){
		
		
	}
	
	public RTPClient(short srcPort, short destPort, String destIpAddress){
		this.srcPort=srcPort;
		this.destPort=destPort;
		try {
			this.srcIpAddress = InetAddress.getLocalHost();
			this.destIpAddress = InetAddress.getByName(destIpAddress);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * performs handshake
	 */
	public void setup(){
		RTPPacketHeader header = new RTPPacketHeader();
		header.setFlags(true, false, false, false); //setting LIVE flag on
		byte [] headerBytes = header.getHeaderBytes();
		//DatagramPacket setupPacket = new DatagramPacket();
		
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
