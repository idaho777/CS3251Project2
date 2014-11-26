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
		System.out.println("===Initialize client");
		RTPClient client = new RTPClient();
//		System.out.println("===Setup Client");
		client.setup();
//		client.teardown();
		
		byte [] data = getFileBytes("C:\\Users\\Eileen\\DHCPMsgExplanation.txt");
		getFileFromBytes("C:\\Users\\Eileen\\Test\\DHCPMsgExplanation.txt", data);
		
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
