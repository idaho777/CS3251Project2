import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ClientApplication {

	private static final String NETEMUIP = "127.0.0.1";
	private static final int NETEMUPORT = 8000;
	private static final int CLIENTPORT = 3250;
	private static String netEmuIpAddress="0.0.0.0";
	private static int netEmuPort, clientPort;
	private static RTPClient client;
	private static final String IPADDRESS_PATTERN = 
			"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	public static void main(String[] args)
	{
		//Initialization handshake

		Scanner scan = new Scanner(System.in);
		Pattern ipPattern  = Pattern.compile(IPADDRESS_PATTERN);

		if(args.length > 0 && args[0].equalsIgnoreCase("fta-client")){
			if (args.length > 3){
				try{
					//Format of command fta-client X A P
					//X is the port the client will bind to
					clientPort = Integer.parseInt(args[1]);

					//A is the IP address of NetEMU
					Matcher matcher = ipPattern.matcher(args[2]);
					if(matcher.matches()){
						netEmuIpAddress = args[2];
					}else{
						throw new IllegalArgumentException();
					}

					//P is the UDP port of NetEMU
					netEmuPort = Integer.parseInt(args[3]);

					System.out.println("Initializing RTPClient...");

					client = new RTPClient(clientPort, netEmuIpAddress, netEmuPort);
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
			System.err.println("fta-client must be run as first command in the format of fta-client X A P");
			System.exit(1);
		}

		while(true){
			String cmd = scan.nextLine().toLowerCase();
			String [] split = cmd.split("\\s+");
			byte [] fileData2 = null;
			if(split.length>0 && !cmd.equals("disconnect")){
				System.out.println(split[0]);
				switch(split[0]){
				case "connect":{ 
					if(client.setup()){
						System.out.println("Client has successfully connected to the server");
					}else{
						System.out.println("Connection failed");
					}
					break;
				}
				case "post":{
					if(split.length>1){
						String pathName = split[1];
						//						client.startUpload(getFileBytes("C:\\Users\\Eileen\\Test\\DHCPMsgExplanation.txt"));
						long start = System.nanoTime(); 

						//client.startUpload(getFileBytes(pathName));
						client.startUpload(getFileBytes("/home/joonho/Desktop/hello.txt")); //TODO change back to taking in pathname, not default file
						long elapsedTime = System.nanoTime() - start;

						/*if(startUpload){
							System.out.println("Successfully uploaded in " + elapsedTime + " seconds");
						}else{
							System.out.println("Upload failed");
						}*/

					}else{
						System.err.println("You need another argument after get");
					}
					break;
				}
				case "get":{
					if(split.length>1){
						String pathName = split[1];
						long start = System.nanoTime(); 
						//client.startDownload(getFileBytes(pathName)); //TODO implement this once this is finished
						//download file from server
						long elapsedTime = System.nanoTime() - start;
						/*if(startUpload){
						System.out.println("Successfully uploaded in " + elapsedTime + " seconds");
						}else{
							System.out.println("Upload failed");
						}*/

					}else{
						System.err.println("You need another argument after post");
					}
					break;
				}
				case "window":{
					if(split.length>1){
						try{
							int size = Integer.parseInt(split[1]);
							client.setWindowSize(size);  //change client's max window size
						}catch(NumberFormatException e){
							System.err.println("Invalid window size.");
						}
					}else{
						System.err.println("You need another argument after window");
					}
					break;
				}
				default:{
					System.err.println("Invalid command.");
					break;
				}
				}
			}else if(cmd.equalsIgnoreCase("disconnect")){
				System.out.println("Disconnecting...");
				/*if(client.teardown()){
					System.out.println("Client has successfully disconnected from the server");
				}else{
					System.out.println("Unable to disconnect"); //TODO fix and change to this code
				}*/ 
				client.teardown();
				scan.close();
				while(client.getClientState()!=ClientState.CLOSED){
				}
				break;
			}else{
				System.err.println("Invalid command.");
				System.exit(1);
				break;
			}

		}

		//System.out.println("Shutdown successful");
		scan.close();
		System.exit(0);

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
