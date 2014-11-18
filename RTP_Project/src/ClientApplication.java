
public class ClientApplication {

	private static final String NETEMUIP = "127.0.0.1";
	private static final short NETEMUPORT = 8000;
	
	private static final short CLIENTPORT = 3251;
	
	public static void main(String[] args)
	{
		System.out.println("Initializing RTPClient");
		RTPClient client = new RTPClient(CLIENTPORT, NETEMUIP, NETEMUPORT);
		System.out.println("Initialization Complete");
		
		System.out.println("Initialization Handshake");
	}
}
