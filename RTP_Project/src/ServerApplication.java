import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ServerApplication {

	private static final String NETEMUIP = "127.0.0.1";
	private static final short NETEMUPORT = 8000;
	private static final short SERVERPORT = 3252;
	private static short serverPort, netEmuPort;
	private static String netEmuIpAddress;
	private static RTPServer server; 
	private static final String IPADDRESS_PATTERN = 
			"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	public static void main(String[] args)
	{

		Pattern ipPattern  = Pattern.compile(IPADDRESS_PATTERN);
		if(args.length > 0 && args[0].equalsIgnoreCase("fta-server")){
			if (args.length > 3){
				try{
					//Format of command fta-client X A P
					//X is the port the client will bind to
					serverPort = Short.parseShort(args[1]);

					//A is the IP address of NetEMU
					Matcher matcher = ipPattern.matcher(args[2]);
					if(matcher.matches()){
						netEmuIpAddress = args[2];
					}else{
						throw new IllegalArgumentException();
					}

					//P is the UDP port of NetEMU
					netEmuPort = Short.parseShort(args[3]);

					System.out.println("Initializing RTP Server...");
					server = new RTPServer(serverPort, netEmuIpAddress, netEmuPort);

					System.out.println("Initialization Complete");		
				}catch(NumberFormatException e){
					System.err.println("The port argument must be a valid port number.");
					System.exit(1);
				}catch(IllegalArgumentException e){
					System.err.println("The second argument must be a valid IP address.");
					System.exit(1);
				}

			}else{
				System.err.println("Not enough arguments.");
				System.exit(1);
			}
		}else{
			System.err.println("fta-server must be run as first command in the format of fta-client X A P");
			System.exit(1);
		}

		while(true){
			//window w 
			//terminate
			Scanner scan = new Scanner(System.in);
			String cmd = scan.nextLine().toLowerCase();
			String [] split = cmd.split("\\s+");
			if(split.length>0 && !cmd.equals("terminate")){
				if(split.length>1 && split[0].equalsIgnoreCase("window")){
					try{
						int windowSize = Integer.parseInt(split[1]);
						System.out.println("I am windowing " + windowSize);
					}catch(NumberFormatException e){
						//
						System.err.println("Enter a valid window size.");
					}
				}
			}else if(cmd.equalsIgnoreCase("terminate")){
				System.out.println("Terminating...");
				scan.close();
				break;
			}else{
				System.err.println("Invalid command.");
				System.exit(1);
			}

		}
		System.out.println("Shutdown successful");
		System.exit(0);
		
	}
	
	public byte[] getFile(String path){
		byte [] data= null;
		File file = new File(path);
		try{
			FileInputStream fin = new FileInputStream(file);
			data= new byte[(int)file.length()];
			fin.read(data);
		}catch(FileNotFoundException e){
			System.err.println("File not found");
		}catch (IOException e){
			System.err.println("File could not be read");
		}
		
		return data;
	}
	
	public void dataToTextFile(byte [] data, String path){
		try {
			FileOutputStream stream= new FileOutputStream(path);
			stream.write(data);
			stream.close();
		} catch (FileNotFoundException e) {
			System.err.println("File not found");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("File could not be written");
		}
	}
}
