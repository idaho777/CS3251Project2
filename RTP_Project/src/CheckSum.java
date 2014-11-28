import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.zip.Adler32;



public class CheckSum {
	/**
	 * Normal Algorithm
	 * Calculates checksum based on byte [] data using Adler32
	 * outdated.. lol
	 * @param data
	 * @return checksum value
	 */
	public static long getChecksum(byte [] data){
		Adler32 checksum = new Adler32();
		checksum.update(data, 0, data.length);
		long checksumVal = checksum.getValue();
		return checksumVal;	
	}
	
	public static int getChecksumInt(byte [] data){
		Adler32 checksum = new Adler32();
		checksum.update(data, 0, data.length);
		long checksumVal = checksum.getValue();
		return (int)checksumVal;	
	}

	
	public static int getHashCode(byte [] data){
		return (int) getChecksum(data);
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
	
	public static boolean isHashcodeValid(DatagramPacket packet){
		RTPPacketHeader header = RTPTools.getHeader(packet);
		int actualHash = header.getHashCode();
		header.setHashCode(0);
		packet = RTPTools.setHeader(packet, header);
		return (actualHash == getHashCode(packet.getData()));
	}
}