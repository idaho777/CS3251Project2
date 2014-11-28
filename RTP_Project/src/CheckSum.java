import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.zip.Adler32;



public class CheckSum {
	/**
	 * Normal Algorithm
	 * Calculates checksum based on byte [] data using Adler32
	 * 
	 * @param data
	 * @return checksum value
	 */
	public static long getChecksum(byte [] data){
		Adler32 checksum = new Adler32();
		checksum.update(data, 0, data.length);
		long checksumVal = checksum.getValue();
		return checksumVal;
		
		//note have a final number on each side, for example
		//3251 on client 
		//3251's checksum on server side, for example 9000
		//so then put the 3251 in the client header and run checksum
		//on the data when it gets through, make sure it matches da 9000
	
	}
	
	public static int getChecksum(DatagramPacket packet){
		return packet.hashCode();
	}
	
	/**
	 * Modified algorithm for our purposes
	 * 
	 * @param num
	 * @return
	 */
	public static int getChecksum(int num)
	{
		byte[] bytes = ByteBuffer.allocate(4).putInt(num).array();
		return (int) getChecksum(bytes);
		
	}
	
	public static boolean isChecksumValid(DatagramPacket packet){
		RTPPacketHeader header = RTPTools.getHeader(packet);
		int actualChecksum = header.getChecksum();
		header.setChecksum(0);
		packet = RTPTools.setHeader(packet, header);
		return (getChecksum(packet)==actualChecksum);		
	}
}