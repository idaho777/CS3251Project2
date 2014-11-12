
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 
 * @author Eileen Wang, Joonho Kim
 *
 */
public class RTPServer {

	private short srcPort, destPort;
	private InetAddress srcIpAddress, destIpAddress;
	private int windowSize;
	private DatagramSocket socket;
	
	public RTPServer()
	{
		
	}
	
	public RTPServer(short sourcePort, short destinationPort, String destinationIpAddress)
	{
		srcPort = sourcePort;
		destPort = destinationPort;
		try {
			destIpAddress = InetAddress.getByName(destinationIpAddress);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void openSession()
	{
		
	}
	
	public void close()
	{
		
	}
	
	public static void main(String[] args)
	{
		System.out.println("asdf");
	}
}
