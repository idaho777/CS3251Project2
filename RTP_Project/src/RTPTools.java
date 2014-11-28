import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;


public class RTPTools {
	private static final int CHECKSUM 		= 13566144;
	private static final int PRECHECKSUM 	= 3251;
	private static final int PACKET_SIZE	= 1024;
	private static final int DATA_SIZE		= 1004;
	private static final int HEADER_SIZE 	= 20;
	private static final int MAX_SEQ_NUM 	= (int) 0xFFFF;


	public static byte [] combineHeaderData(byte [] headerBytes, byte [] data){

		byte [] packetBytes = new byte [PACKET_SIZE];
		//bytesRemaining should be updated when we successfully get ACK back for successfully transfered packet
		System.arraycopy(headerBytes, 0, packetBytes, 0, HEADER_SIZE);		// copying header
		System.arraycopy(data, 0, packetBytes, HEADER_SIZE, data.length);

		return packetBytes;
	}
	
	public static byte[] extractData(DatagramPacket receivePacket)
	{
		RTPPacketHeader receiveHeader = getHeader(receivePacket);
		int data_length = receiveHeader.getWindow();
		
		byte[] extractedData = new byte[data_length];
		byte[] packet = receivePacket.getData();
		
		System.arraycopy(packet, HEADER_SIZE, extractedData, 0, data_length);
		
		return extractedData;
	}
	
	

	public static boolean isValidPacketHeader(DatagramPacket packet)
	{
		int headerChecksum = CheckSum.getChecksum(getHeader(packet).getChecksum());

		return (headerChecksum == CHECKSUM) && (CheckSum.isHashcodeValid(packet)) ;
	}

	public static boolean isValidPacketHeader(RTPPacketHeader header)
	{
		int headerChecksum = CheckSum.getChecksum(header.getChecksum());

		return (headerChecksum == CHECKSUM) ;
	}
	

	public static RTPPacketHeader getHeader(DatagramPacket receivePacket)
	{
		return new RTPPacketHeader(Arrays.copyOfRange(receivePacket.getData(), 0, 20));
	}
	
	
	
	public static DatagramPacket setHeader(DatagramPacket packet, RTPPacketHeader header)
	{
		byte [] headerBytes = header.getHeaderBytes();
		byte [] packetData = packet.getData();
		System.arraycopy(headerBytes, 0, packetData , 0, HEADER_SIZE);
		packet.setData(packetData);
		
		return packet;
	}	

	public static byte [] getFileBytes(String pathName){
		Path path = Paths.get(pathName);
		byte[] data=null;
		try {
			data = Files.readAllBytes(path);
		} catch (IOException e) {
			System.err.println("File could not be read");
			e.printStackTrace();
		}
		return data;
	}

	public static File getFileFromBytes(String pathname, byte [] data){
		File file = new File(pathname);
		try (FileOutputStream fop = new FileOutputStream(file)) {
			// if file doesn't exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
			fop.write(data);
			fop.flush();
			fop.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file;
	}
}
