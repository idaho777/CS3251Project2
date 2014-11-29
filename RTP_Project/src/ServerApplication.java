import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ServerApplication {

	private static final String NETEMUIP = "127.0.0.1";
	private static final int NETEMUPORT = 8000;
	private static final int SERVERPORT = 3251;
	private static int serverPort, netEmuPort;
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
					serverPort = Integer.parseInt(args[1]);

					//A is the IP address of NetEMU
					Matcher matcher = ipPattern.matcher(args[2]);
					if(matcher.matches()){
						netEmuIpAddress = args[2];
					}else{
						throw new IllegalArgumentException();
					}

					//P is the UDP port of NetEMU
					netEmuPort = Integer.parseInt(args[3]);

					System.out.println("Initializing RTP Server...");
					server = new RTPServer(serverPort, netEmuIpAddress, netEmuPort);
					server.connect();
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
			System.err.println("fta-server must be run as first command in the format of fta-server X A P");
			System.exit(1);
		}
		long end=System.currentTimeMillis();
		InputStreamReader fileInputStream=new InputStreamReader(System.in);
		BufferedReader bufferedReader=new BufferedReader(fileInputStream);
		try
		{
			String s = new String("");

			while((System.currentTimeMillis()>=end))
			{

				s = "";
				server.openSession();
				if (bufferedReader.ready()){
					s += bufferedReader.readLine();
					System.out.println("here");

					System.out.println(s);
					if(s.equalsIgnoreCase("terminate")){
						if(server.terminate()){
							System.out.println("Server termination successful");
							System.exit(0);
						}else{
							System.out.println("Server termination failed");
						}
					}else{
						System.err.println("Invalid command");
					}
				}
			}

			bufferedReader.close();
		}
		catch(java.io.IOException e)
		{
			System.err.println("Server could not be shut down");
			e.printStackTrace();
		}
		//		input
		//		String input;
		//		while(true){
		//			//window w 
		//			//terminate
		//			input = server.openSession();
		//			System.out.println("Timeout");
		//			
		////			Scanner scan = new Scanner(System.in);
		//
		//			String cmd = null;
		////				 cmd = scan.nextLine().toLowerCase();
		//			
		//			if (cmd == null)
		//			{
		//				continue;
		//			}
		//			String [] split = cmd.split("\\s+");
		//			if(split.length>0 && !cmd.equals("terminate")){
		//				if(split.length>1 && split[0].equalsIgnoreCase("window")){
		//					try{
		//						int windowSize = Integer.parseInt(split[1]);
		//						server.setWindowSize(windowSize);
		//					}catch(NumberFormatException e){
		//						System.err.println("Enter a valid window size.");
		//					}
		//				}
		//			}else if(cmd.equalsIgnoreCase("terminate")){
		//				System.out.println("Terminating...");
		////				scan.close();
		//				break;
		//			}else{
		//				System.err.println("Invalid command.");
		//				System.exit(1);
		//			}
		//		}
		//		System.out.println("Shutdown successful");
		//		System.exit(0);

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
