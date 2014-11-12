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
	
	private short portNumber, bindPort;
	private InetAddress ipAddress;
	private int windowSize;
	private DatagramSocket socket;
	
	public RTPClient(){
		
		
	}
	
	public RTPClient(short portNumber, short bindPort, String ipAddress){
		this.portNumber=portNumber;
		this.bindPort=bindPort;
		try {
			this.ipAddress = InetAddress.getByName(ipAddress);
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
