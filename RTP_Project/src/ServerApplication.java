
public class ServerApplication {

	private static final String NETEMUIP = "127.0.0.1";
	private static final short NETEMUPORT = 8000;
	
	private static final short SERVERPORT = 3252;
	
	public static void main(String[] args)
	{
		System.out.println("Initializing RTPClient");
		RTPServer server = new RTPServer(SERVERPORT);
		System.out.println("Initialization Complete");
		
		System.out.println("Initialization Handshake");
	}
}
