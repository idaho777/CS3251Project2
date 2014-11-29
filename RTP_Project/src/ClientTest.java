import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class ClientTest
{
	public static void main(String[] args)
	{
		   long end=System.currentTimeMillis();
		    InputStreamReader fileInputStream=new InputStreamReader(System.in);
		    BufferedReader bufferedReader=new BufferedReader(fileInputStream);
		    try
		    {
		    	String s = null;

		    	while((System.currentTimeMillis()>=end))
		    	{
		    	    if (bufferedReader.ready())
		    	    	
		    	        s += bufferedReader.readLine();
		    	    //System.out.println(s);
		    	}

		    	bufferedReader.close();
		    }
		    catch(java.io.IOException e)
		    {
		        e.printStackTrace();
		    }
		

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
