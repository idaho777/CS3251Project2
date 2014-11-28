import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 
 * @author Eileen Wang, Joonho Kim
 *
 */
public class RTPClient {

	private static final int CHECKSUM 		= 13566144;
	private static final int PRECHECKSUM 	= 3251;
	private static final int PACKET_SIZE	= 1024;
	private static final int DATA_SIZE		= 1004;
	private static final int HEADER_SIZE 	= 20;
	private static final int MAX_SEQ_NUM 	= (int) 0xFFFF;

	private ClientState state;

	private int clientPort, serverPort;
	private InetAddress clientIpAddress, serverIpAddress;
	private DatagramSocket clientSocket;
	private Random rand;
	
	private int timeout = 10000;	// milliseconds

	private byte[] window = new byte[MAX_SEQ_NUM];
	private int seqNum, ackNum, windowSize, bytesRemaining;
	private String pathName="";
	private byte [] fileData;
	private boolean timedTaskRun= false;

	public RTPClient() {
		this.clientPort=3251;
		this.serverPort=3252;
		try {
			this.clientIpAddress = InetAddress.getLocalHost();
			this.serverIpAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		state = ClientState.CLOSED;
	}

	public RTPClient(int clientPort, String serverIpAddress, int serverPort){
		this.clientPort=clientPort;
		this.serverPort=serverPort;
		try {
			this.clientIpAddress = InetAddress.getLocalHost();
			this.serverIpAddress = InetAddress.getByName(serverIpAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		rand = new Random();
		seqNum = rand.nextInt(MAX_SEQ_NUM);
//		seqNum = 10;
		state = ClientState.CLOSED;
	}

	/**
	 * performs handshake
	 * @throws IOException 
	 */
	public boolean setup()
	{
		// setup socket
		System.out.println(clientPort + " " + clientIpAddress);
		System.out.println(serverPort + " " + serverIpAddress);
		try {
			clientSocket = new DatagramSocket(clientPort, clientIpAddress);
		} catch (SocketException e) {
			e.printStackTrace();
		}

		byte[] receiveMessage = new byte[PACKET_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);

		// Setup Initializing Header
		RTPPacketHeader liveHeader = new RTPPacketHeader();
		liveHeader.setSource(clientPort);
		liveHeader.setDestination(serverPort);
		liveHeader.setSeqNum(seqNum);
		liveHeader.setAckNum(ackNum);
		liveHeader.setFlags(true, false, false, false); //setting LIVE flag on
		liveHeader.setChecksum(PRECHECKSUM);
		byte [] headerBytes = liveHeader.getHeaderBytes();

		DatagramPacket setupPacket = new DatagramPacket(headerBytes, HEADER_SIZE, serverIpAddress, serverPort);

		// Sending LIVE packet and receiving ACK

		try {
			clientSocket.setSoTimeout(timeout);
		} catch (SocketException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


		int tries = 0;
		state = ClientState.LIVE_SENT;
		while (state != ClientState.ESTABLISHED)
		{
			try
			{
				System.out.println("Going to send packet");
				clientSocket.send(setupPacket);
				System.out.println("Send Packet");
				clientSocket.receive(receivePacket);
				System.out.println("Received Packet");

				RTPPacketHeader receiveHeader = getHeader(receivePacket);
				if (!isValidPacketHeader(receivePacket))
				{
					System.out.println("CURROPTED in " + state);
					continue;
				}

				// Assuming valid and Acknowledged
				if (receiveHeader.isLive() && receiveHeader.isAck() && !receiveHeader.isLast())
				{
					System.out.println("GET LIVEE");
					setupPacket = handShakeLiveAck(receivePacket);
				}
				
				if (receiveHeader.isLive() && receiveHeader.isAck() && receiveHeader.isLast())
				{
					System.out.println("GET LIVE ACK");
					handShakeLiveLast(receivePacket);
				}
				System.out.println("END OF LOOP");
			}
			catch (SocketTimeoutException s)
			{
				System.out.println("Timeout, resend");
				if (tries++ >= 5)
				{
					System.out.println("Unsuccessful Connection");
					return false;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			
		}
		System.out.println("exit setup()");
		return true;
	}

	private DatagramPacket handShakeLiveAck(DatagramPacket receivePacket) throws IOException
	{
		RTPPacketHeader ackHeader = new RTPPacketHeader();
		ackHeader.setSource(clientPort);
		ackHeader.setDestination(serverPort);
		ackHeader.setChecksum(PRECHECKSUM);
		ackHeader.setSeqNum(seqNum);
		ackHeader.setAckNum(0);
		ackHeader.setFlags(true, false, false, true); // Live and last

		state = ClientState.SERVER_ACK_SENT;

		byte[] ackHeaderBytes = ackHeader.getHeaderBytes();
		DatagramPacket ackPacket = new DatagramPacket
				(
					ackHeaderBytes,
					HEADER_SIZE,
					serverIpAddress,
					serverPort
				);
		System.out.println("SENDING SHAKE 2");
		return ackPacket;
	}


	private void handShakeLiveLast(DatagramPacket receivePacket) throws IOException
	{
		RTPPacketHeader receiveHeader = getHeader(receivePacket);
		System.out.println(ackNum + " " + seqNum + " " + receiveHeader.getAckNum());
		if (receiveHeader.getAckNum() == (seqNum + 1) % MAX_SEQ_NUM)
		{
			ackNum = receiveHeader.getSeqNum();
			seqNum = (seqNum + 1) % MAX_SEQ_NUM;
			state = ClientState.ESTABLISHED;
		}
	}

	/**
	 * Starts sending data transfer
	 */
	public void startUpload(byte [] file){
		fileData = file;
		bytesRemaining = fileData.length;
		int packetNumber = (fileData.length / DATA_SIZE) + ((fileData.length % DATA_SIZE > 0) ? 1 : 0);
		int currPacket = 0;
		DatagramPacket sendingPacket;
		DatagramPacket receivePacket = new DatagramPacket(new byte [PACKET_SIZE], PACKET_SIZE);
		
		while (currPacket < packetNumber)
		{
			sendingPacket = createPacket(currPacket);
			try {
				System.out.println(getHeader(sendingPacket).getSeqNum() + " ack num : " + getHeader(sendingPacket).getAckNum());
				clientSocket.send(sendingPacket);
				clientSocket.receive(receivePacket);
				RTPPacketHeader receiveHeader = getHeader(receivePacket);
				
				if (!isValidPacketHeader(receivePacket))
				{
					continue;
				}
<<<<<<< HEAD
				seqNum = (seqNum + 1) % MAX_SEQ_NUM;
				ackNum = receiveHeader.getSeqNum();
				if (!receiveHeader.isLive() && receiveHeader.isAck() && !receiveHeader.isDie() && !receiveHeader.isLast())
				{
=======
				
				if (!receiveHeader.isLive() && receiveHeader.isAck() && !receiveHeader.isDie() && !receiveHeader.isLast())
				{
					System.out.println("I have not received the last ack");
>>>>>>> 3dbea55163fc677062df139f13ddd0b14b3f4177
					sendingPacket = createPacket(++currPacket);
				}
				
				if (!receiveHeader.isLive() && receiveHeader.isAck() && !receiveHeader.isDie() && receiveHeader.isLast())
				{
					currPacket++;
					System.out.println("I have received the last ack!");
				}
			} catch (SocketTimeoutException s) {
				System.out.println("Timeout, resend");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public DatagramPacket createPacket(int startByteIndex){
		// Setup header for the data packet
		int bytesRemaining = fileData.length - startByteIndex * DATA_SIZE;
		int data_length = (bytesRemaining <= DATA_SIZE) ? bytesRemaining : DATA_SIZE;
		RTPPacketHeader header = new RTPPacketHeader();
		header.setSource(clientPort);
		header.setDestination(serverPort);
<<<<<<< HEAD
		header.setSeqNum(seqNum); //should have last seq num
		header.setAckNum((ackNum + 1) % MAX_SEQ_NUM); //???
=======
		header.setSeqNum(seqNum++); //should have last seq num
		header.setAckNum(seqNum); //???
>>>>>>> 3dbea55163fc677062df139f13ddd0b14b3f4177
		header.setWindow(data_length);
		header.setFlags(false, false, false, false); 
		header.setChecksum(PRECHECKSUM);
		
		header.setWindow(data_length);
		if (bytesRemaining <= DATA_SIZE) {
			header.setFlags(false, false, false, true);
		}
		
		byte [] headerBytes = header.getHeaderBytes();
		byte [] data = new byte [DATA_SIZE];
		byte [] packetBytes = new byte [PACKET_SIZE];
		//bytesRemaining should be updated when we successfully get ACK back for successfully transfered packet
		System.arraycopy(headerBytes, 0, packetBytes, 0, HEADER_SIZE);		// copying header
		System.arraycopy(fileData, startByteIndex * DATA_SIZE, data, 0, data_length);
		System.arraycopy(data, 0, packetBytes, HEADER_SIZE, data_length);
		
		header.setHashCode(CheckSum.getHashCode(data));
		
		DatagramPacket dataPacket = new DatagramPacket(packetBytes, packetBytes.length, serverIpAddress, serverPort);
		return dataPacket;
	}


	/**
	 * Stops the data transfer
	 */
	public void stopUpload(){

	}


	public void startDownload(){

	}

	public void stopDownload(){

	}


	/**
	 * Once data transfer stops, performs connection teardown
	 */
	public void teardown(){
		byte[] receiveMessage = new byte[PACKET_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);

		// Setup header for the DIE packet
		RTPPacketHeader dieHeader = new RTPPacketHeader();
		dieHeader.setSource(clientPort);
		dieHeader.setDestination(serverPort);
		dieHeader.setSeqNum(0); //should have last seq num
		dieHeader.setAckNum(0);
		dieHeader.setFlags(false, true, false, false); //setting DIE flag on
		dieHeader.setChecksum(PRECHECKSUM);
		byte [] headerBytes = dieHeader.getHeaderBytes();

		DatagramPacket teardownPacket = new DatagramPacket(headerBytes, HEADER_SIZE, serverIpAddress, serverPort);

		// Sending DIE packet and receiving ACK

		int tries = 0;
		state = ClientState.DIE_WAIT_1;
		while (state != ClientState.SERVER_ACK_SENT){
			try
			{
				System.out.println("tries:" + tries);
				clientSocket.send(teardownPacket);
				clientSocket.receive(receivePacket);

				RTPPacketHeader receiveHeader = getHeader(receivePacket);

				System.out.println(receiveHeader.getSeqNum());
				if (!isValidPacketHeader(receivePacket))
				{
					continue;
				}
				System.out.println(receiveHeader.isLive() + " " + receiveHeader.isDie() + " " + receiveHeader.isAck() + " " + receiveHeader.isLast());
				if (receiveHeader.isDie() && receiveHeader.isAck() && !receiveHeader.isLast()){
					System.out.println("ACK from server has been sent. State is now: SERVER_ACK_SENT");
					state=ClientState.SERVER_ACK_SENT;
				}
			}
			catch (SocketTimeoutException s){
				System.out.println("Timeout, resend");
				if(tries++>=5){
					System.out.println("Unsuccessful Connection");
					return;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		//entering the state where it waits for server to send DIE
		state = ClientState.DIE_WAIT_2;
		System.out.println("State: DIE_WAIT_2");
		tries = 0;
		Timer timer = null;
		while (state != ClientState.TIME_WAIT || timedTaskRun){
			try{
				clientSocket.receive(receivePacket);
				RTPPacketHeader receiveHeader = getHeader(receivePacket);
				if (!isValidPacketHeader(receivePacket))
				{
					continue;
				}
				if (receiveHeader.isDie() && receiveHeader.isLast())
				{
					sendCloseAckState(); //sends the final ACK	
					if(timer==null){
						timer = new Timer();
						timer.schedule(new timedWaitTeardown(), 5*100); //timedwaitTeardown changes state and closes socket
					}
				}
			}
			catch (SocketTimeoutException s){
				System.out.println("Timeout, resend");
				if(tries++>=5){
					System.out.println("Unsuccessful Connection");
					return;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("exit teardown");
	}

	/**
	 * This method takes a packet and creates it for the last 
	 * ACK packet sent in the 4-way handshake in the connection teardown
	 * @param receivePacket
	 * @return
	 */
	private void sendCloseAckState() throws IOException
	{	

		//RTPPacketHeader receiveHeader = getHeader(receivePacket);

		//makes new ACK header 
		RTPPacketHeader ackHeader = new RTPPacketHeader();
		ackHeader.setSource(clientPort);
		ackHeader.setDestination(serverPort);
		ackHeader.setChecksum(PRECHECKSUM);
		ackHeader.setSeqNum(0);
		ackHeader.setAckNum(0);

		System.out.println("Last DIE from Server has been received STATE: TIME_WAIT");
		state = ClientState.TIME_WAIT;
		ackHeader.setFlags(false, true, true, true); //die, ack, last flags on

		byte[] ackHeaderBytes = ackHeader.getHeaderBytes();

		DatagramPacket ackPacket = new DatagramPacket(ackHeaderBytes, HEADER_SIZE, serverIpAddress, serverPort);	
		clientSocket.send(ackPacket);
		System.out.println("Client last ACK has been sent");
	}


	private boolean isValidPacketHeader(DatagramPacket packet)
	{
		int headerChecksum = CheckSum.getChecksum(getHeader(packet).getChecksum());
		CheckSum.isHashcodeValid(packet);

		return (headerChecksum == CHECKSUM) && (CheckSum.isHashcodeValid(packet)) ;
	}



	private RTPPacketHeader getHeader(DatagramPacket receivePacket)
	{
		return new RTPPacketHeader(Arrays.copyOfRange(receivePacket.getData(), 0, 20));
	}

	public ClientState getClientState(){
		return state;
	}

	public int getWindowSize(){
		return windowSize;
	}

	public void setWindowSize(int window){
		this.windowSize=window;
	}

	/**
	 * A private class that consists of the task to close down the
	 * client socket after the timed wait
	 * 
	 * This only ocurs after the client has received the last DIE from the server
	 * @author Eileen
	 *
	 */
	private class timedWaitTeardown extends TimerTask {
		public void run() {
			state=ClientState.CLOSED;
			clientSocket.close();
			System.out.println("Task has been run");
			timedTaskRun = true;
		}
	}
}
