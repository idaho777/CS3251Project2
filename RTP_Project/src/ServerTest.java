
public class ServerTest
{
	public static void main(String[] args)
	{
		System.out.println("====Initialize Server");
		RTPServer server = new RTPServer();
		System.out.println("====Open Session Server");
		server.openSession();
	}
}
