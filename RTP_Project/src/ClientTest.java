import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class ClientTest
{
	public static void main(String[] args)
	{
		RTPPacketHeader ackHeader = new RTPPacketHeader();
		ackHeader.setSource(11234);
		ackHeader.setDestination(456);
		ackHeader.setChecksum(90);
		ackHeader.setSeqNum(134123413);
		ackHeader.setAckNum(0);
		byte[] data = ackHeader.getHeaderBytes();
		
		byte[] bytes = new byte[50];
		System.arraycopy(data, 0, bytes, 0, 20);
		bytes[0] = 1;
		DatagramPacket packet = new DatagramPacket(bytes, 50);
		DatagramPacket packet2 = new DatagramPacket(bytes, 50);
		int hash = CheckSum.getHashCode(new DatagramPacket(bytes, 50));
		int hash2 = CheckSum.getHashCode(new DatagramPacket(bytes, 50));
		System.out.println(CheckSum.getChecksum(bytes));
		System.out.println(bytes.hashCode());
		
//		System.out.println("===Initialize client");
//		RTPClient client = new RTPClient();
////		System.out.println("===Setup Client");
//		client.setup();
////		client.teardown();
		
//		byte [] data = getFileBytes("C:\\Users\\Eileen\\DHCPMsgExplanation.txt");
//		getFileFromBytes("C:\\Users\\Eileen\\Test\\DHCPMsgExplanation.txt", data);
		
		// I RUN LINUX
//		byte [] data = getFileBytes("/home/joonho/Desktop/hello.txt");
//		getFileFromBytes("/home/joonho/Desktop/goodbye.txt", data);
		
		
		
		//DatagramPacket packet = client.createPacket(0, data);
		
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
