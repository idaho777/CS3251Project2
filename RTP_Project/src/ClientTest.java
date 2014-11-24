
public class ClientTest
{
	public static void main(String[] args)
	{
		System.out.println("===Initialize client");
		RTPClient client = new RTPClient();
		System.out.println("===Setup Client");
		client.setup();
	}
}
