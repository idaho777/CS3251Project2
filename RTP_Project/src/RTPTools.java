import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;


public class RTPTools {
	private static final int CHECKSUM 		= 13566144;
	private static final int PRECHECKSUM 	= 3251;
	private static final int PACKET_SIZE	= 1024;
	private static final int DATA_SIZE		= 1004;
	private static final int HEADER_SIZE 	= 20;
	private static final int MAX_SEQ_NUM 	= (int) 0xFFFF;


	
	private boolean isValidPacketHeader(DatagramPacket packet)
	{
		int headerChecksum = CheckSum.getChecksum(getHeader(packet).getChecksum());
		CheckSum.isHashcodeValid(packet);

		return (headerChecksum == CHECKSUM) && (CheckSum.isHashcodeValid(packet)) ;
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
	

	public static RTPPacketHeader getHeader(DatagramPacket receivePacket)
	{
		return new RTPPacketHeader(Arrays.copyOfRange(receivePacket.getData(), 0, 20));
	}
	
	public static DatagramPacket setHeader(DatagramPacket packet, RTPPacketHeader header)
	{
		byte [] packetData = packet.getData();
		System.arraycopy(header, 0, packetData , 0, HEADER_SIZE);
		packet.setData(packetData);
		return packet;
	}
	
//	public DatagramPacket createPacket(byte [] headerBytes, byte [] dataBytes){
//		byte [] data = new byte [DATA_SIZE];
//		byte [] packetBytes = new byte [PACKET_SIZE];
//		//bytesRemaining should be updated when we successfully get ACK back for successfully transfered packet
//		System.arraycopy(headerBytes, 0, packetBytes, 0, HEADER_SIZE);		// copying header
//		System.arraycopy(fileData, startByteIndex * DATA_SIZE, data, 0, data_length);
//		System.arraycopy(data, 0, packetBytes, HEADER_SIZE, data_length);
//		
//		DatagramPacket dataPacket = new DatagramPacket(packetBytes, packetBytes.length, serverIpAddress, serverPort);
//		return dataPacket;
//	}
}
