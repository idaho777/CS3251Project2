
import java.io.File;
import java.io.FileOutputStream;
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

/**
 * 
 * @author Eileen Wang, Joonho Kim
 *
 */
public class RTPServer {

	private static final int CHECKSUM 		= 13566144;
	private static final int PRECHECKSUM 	= 3251;
	private static final int PACKET_SIZE	= 1024;
	private static final int DATA_SIZE		= 1004;
	private static final int HEADER_SIZE 	= 20;
	private static final int MAX_SEQ_NUM 	= (int) 0xFFFF;
	
	private int serverPort, clientPort;
	private InetAddress serverIpAddress, clientIpAddress;

	private int windowSize;
	private DatagramSocket serverSocket;
	private DatagramPacket sendPacket, receivePacket;
	private Random rand;
	
	private ServerState state;
	private int seqNum, ackNum;
	private String pathName="";
	private ArrayList<byte[]> bytesReceived;
	private byte[] fileData;

	public RTPServer()
	{
		bytesReceived = new ArrayList<byte []> ();
		serverPort = 3252;
		try {
			serverIpAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		state = ServerState.CLOSED;
		System.out.println("Server IP: " + serverIpAddress);
	}

	public RTPServer(int sourcePort)
	{	
		bytesReceived = new ArrayList<byte []> ();
		serverPort = sourcePort;
		try {
			serverIpAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		state = ServerState.CLOSED;
		System.out.println("Server IP: " + serverIpAddress);
	}

	public RTPServer(int serverPort, String clientIpAddress, int clientPort){

		bytesReceived = new ArrayList<byte []> ();
		this.serverPort = serverPort;
		this.clientPort = clientPort;
		try {
			this.serverIpAddress = InetAddress.getLocalHost();
			this.clientIpAddress = InetAddress.getByName(clientIpAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		state = ServerState.CLOSED;
		rand = new Random();
		seqNum = rand.nextInt(MAX_SEQ_NUM);
//		seqNum = 20;
	}


	public void openSession()
	{
		try
		{
			serverSocket = new DatagramSocket(serverPort, serverIpAddress);
			serverSocket.setSoTimeout(10000);
		}
		catch (SocketException e)
		{
			e.printStackTrace();
		}
		byte[] arr = new byte[PACKET_SIZE];
		receivePacket = new DatagramPacket(arr, PACKET_SIZE);

		state = ServerState.LISTEN;
		// handshake
		System.out.println(serverPort + " " + serverIpAddress);
		System.out.println(clientPort + " " + clientIpAddress);
		while (state != ServerState.CLOSED)
		{
			try
			{
				// Receive Packet
				serverSocket.receive(receivePacket);
				// Get Header of Packet and 
				RTPPacketHeader receiveHeader = getHeader(receivePacket);

				System.out.println("received    " + receiveHeader.getSeqNum() + "   " + receiveHeader.getAckNum());
				// Checksum validation
				if (!isValidPacketHeader(receiveHeader))
				{
					resendPacket(receivePacket);
					System.out.println("resending packet");
					continue;
				}
				
				
				// ==== Packet is valid
				if (!receiveHeader.isLive() && !receiveHeader.isDie() && !receiveHeader.isAck())
				{
					if (receiveHeader.getAckNum() != (seqNum + 1) % MAX_SEQ_NUM)
					{
						System.out.println("resending valid packet " + seqNum + " Ack Num: " + ackNum);
						resendPacket(receivePacket);
					}
					else
					{
						receiveDataPacket(receivePacket);
						if (receiveHeader.isLast())
						{
							System.out.println("assembling file");
							assembleFile();
						}	
					}
				}
				else if (receiveHeader.isLive() && !receiveHeader.isDie() && !receiveHeader.isAck() && !receiveHeader.isLast())
				{
					System.out.println("HAND SHAKE ONE");
					handShakeOne(receivePacket);
				}
				else if (receiveHeader.isLive() && !receiveHeader.isDie() && !receiveHeader.isAck() && receiveHeader.isLast())
				{
					System.out.println("HAND SHAKE TWO");
					handShakeTwo(receivePacket);
				}
				else if (!receiveHeader.isLive() && receiveHeader.isDie() && !receiveHeader.isAck() && !receiveHeader.isLast())
				{
					close();
				}
				
				
			}
			catch (SocketTimeoutException s)
			{
				System.out.println("Check for terminate");
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void resendPacket(DatagramPacket receivePacket) throws IOException
	{
		RTPPacketHeader receiveHeader = getHeader(receivePacket);
		receiveHeader.isAck();
		receiveHeader.setFlags
			(
				receiveHeader.isLive(),
				receiveHeader.isDie(),
				false,
				receiveHeader.isLast()
			);
		
		byte[] resendHeaderBytes = receiveHeader.getHeaderBytes();
	
		DatagramPacket sendPacket = new DatagramPacket
				(
					resendHeaderBytes,
					resendHeaderBytes.length,
					clientIpAddress,
					clientPort
				);
		
		serverSocket.send(receivePacket);
	}
	
	private void handShakeOne(DatagramPacket receivePacket) throws IOException
	{

		// Receive Packet
		RTPPacketHeader receiveHeader = getHeader(receivePacket);
		
		// Live Ack Header
		RTPPacketHeader liveAckHeader = new RTPPacketHeader();
		liveAckHeader.setSource(serverPort);
		liveAckHeader.setDestination(clientPort);
		liveAckHeader.setChecksum(PRECHECKSUM);
		liveAckHeader.setSeqNum(seqNum);
		liveAckHeader.setAckNum(ackNum);
		liveAckHeader.setFlags(true, false, true, false);	// live, !die, !ack, !last

		state = ServerState.LIVE_RCVD;

		byte[] liveAckHeaderBytes = liveAckHeader.getHeaderBytes();

		DatagramPacket sendPacket = new DatagramPacket
				(
					liveAckHeaderBytes,
					liveAckHeaderBytes.length,
					clientIpAddress,
					clientPort
				);
		System.out.println("Sending to " + clientIpAddress + " with port: " + clientPort);
		serverSocket.send(sendPacket);
	}

	public void handShakeTwo(DatagramPacket receivePacket) throws IOException
	{
		RTPPacketHeader receiveHeader = getHeader(receivePacket);
		int receiveSeqNum = receiveHeader.getSeqNum();

		// Live Ack Header
		RTPPacketHeader liveAckHeader = new RTPPacketHeader();
		liveAckHeader.setSource(serverPort);
		liveAckHeader.setDestination(clientPort);
		liveAckHeader.setChecksum(PRECHECKSUM);

		ackNum = receiveHeader.getSeqNum();
		liveAckHeader.setSeqNum(seqNum);
		liveAckHeader.setAckNum((ackNum + 1) % MAX_SEQ_NUM);
		liveAckHeader.setFlags(true, false, true, true);	// live, !die, ack, last

		state = ServerState.ESTABLISHED;

		byte[] liveAckHeaderBytes = liveAckHeader.getHeaderBytes();

		DatagramPacket sendPacket = new DatagramPacket
				(
					liveAckHeaderBytes,
					liveAckHeaderBytes.length,
					clientIpAddress,
					clientPort
				);
		serverSocket.send(sendPacket);
	}

	
	private void receiveDataPacket(DatagramPacket receivePacket) throws IOException
	{
		RTPPacketHeader receiveHeader = getHeader(receivePacket);
		
		// extracts and adds data to ArrayList of byte[]s
		byte[] data = extractData(receivePacket);

		bytesReceived.add(data);
		
		RTPPacketHeader dataAckHeader = new RTPPacketHeader();
		dataAckHeader.setSource(serverPort);
		dataAckHeader.setDestination(clientPort);
		dataAckHeader.setChecksum(PRECHECKSUM);

		ackNum = receiveHeader.getSeqNum();
		seqNum = (seqNum + 1) % MAX_SEQ_NUM;
		dataAckHeader.setSeqNum(seqNum);
		dataAckHeader.setAckNum((ackNum + 1) % MAX_SEQ_NUM);
		dataAckHeader.setFlags(false, false, true, false);	// ACK
		
		if (receiveHeader.isLast())
		{
			dataAckHeader.setFlags(false, false, true, true); // ACK LAST
		}
		
		byte[] liveAckHeaderBytes = dataAckHeader.getHeaderBytes();

		DatagramPacket sendPacket = new DatagramPacket
				(
					liveAckHeaderBytes,
					liveAckHeaderBytes.length,
					clientIpAddress,
					clientPort
				);
		serverSocket.send(sendPacket);
	}

	private byte[] extractData(DatagramPacket receivePacket)
	{
		RTPPacketHeader receiveHeader = getHeader(receivePacket);
		int data_length = receiveHeader.getWindow();
		
		byte[] extractedData = new byte[data_length];
		byte[] packet = receivePacket.getData();
		
		System.arraycopy(packet, HEADER_SIZE, extractedData, 0, data_length);
		
		return extractedData;
	}
	
	private void assembleFile()
	{
		int bufferLength = bytesReceived.size();
		int lastByteArrayLength = bytesReceived.get(bufferLength - 1).length;	// Length of last data
		int fileSize = (bufferLength - 1) * DATA_SIZE + lastByteArrayLength;	// number of bytes in file
		
		fileData = new byte[fileSize];
		for (int i = 0; i < bufferLength - 1; i++)
		{
			System.arraycopy(bytesReceived.get(i), 0, fileData, i * DATA_SIZE, DATA_SIZE);
		}
		
		// Copy last data
		System.arraycopy(bytesReceived.get(bufferLength - 1), 0, fileData, (bufferLength - 1) * DATA_SIZE, lastByteArrayLength);

		// I RUN LINUX
		getFileFromBytes("/home/joonho/Desktop/goodbye.txt", fileData);
//		getFileFromBytes("C:\\Users\\Eileen\\Test\\DHCPMsgExplanation.txt", fileData);
		
		//clearing out byte received buffer and file data for next uploads
		fileData = null;
		bytesReceived = new ArrayList<byte []> ();
	}
	
	
	/**
	 * A method to close the server connection, 4-way handshake
	 */
	public void close()
	{		
		state = ServerState.CLOSE_WAIT1;
		byte[] arr = new byte[PACKET_SIZE];
		receivePacket = new DatagramPacket(arr, arr.length);
		
		int tries = 0;
		while (state != ServerState.CLOSED)
		{
			try
			{
				sendAckCloseState();
				sendDieCloseState();
				serverSocket.receive(receivePacket);
				
				RTPPacketHeader receiveHeader = getHeader(receivePacket);
				
				// Checksum validation for data received from client
				if (!isValidPacketHeader(receiveHeader))
				{
					// is not Valid Packet, send back
					// set same flags but ack is false
					// send same packet as received
					// so client will resend with same everything
					continue;
				}
				
				if (!receiveHeader.isLive() && receiveHeader.isDie() && !receiveHeader.isAck() && !receiveHeader.isLast())
				{
					System.out.println("I am the cause of repeat");
					continue;
				}
				
				if (!receiveHeader.isLive() && receiveHeader.isDie() && receiveHeader.isAck() && receiveHeader.isLast())
				{
					state = ServerState.CLOSED;
					serverSocket.close();
				}
				
			}
			catch (SocketTimeoutException s)
			{
				System.out.println("Have not received DIE from Client for termination");
				if(tries++ >= 5){
					System.out.println("Teardown unstable connection");
					return;
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendAckCloseState() throws IOException
	{

		// DIE Ack Header
		RTPPacketHeader dieAckHeader = new RTPPacketHeader();
		dieAckHeader.setSource(clientPort);
		dieAckHeader.setDestination(serverPort);
		dieAckHeader.setChecksum(PRECHECKSUM);
		dieAckHeader.setSeqNum(0);
		dieAckHeader.setAckNum(0);
		dieAckHeader.setFlags(false, true, true, false); //ack, die flags on
		
		state = ServerState.CLOSE_WAIT2;

		byte[] dieAckHeaderBytes = dieAckHeader.getHeaderBytes();

		DatagramPacket sendPacket = new DatagramPacket
				(
					dieAckHeaderBytes,
					dieAckHeaderBytes.length,
					clientIpAddress,
					clientPort
				);

		serverSocket.send(sendPacket);

	}

	private void sendDieCloseState() throws IOException{
		// Setup header for the DIE packet
		RTPPacketHeader dieHeader = new RTPPacketHeader();
		dieHeader.setSource(clientPort);
		dieHeader.setDestination(serverPort);
		dieHeader.setSeqNum(10); //should have last seq num 
		dieHeader.setAckNum(0); //?????? What should these be after the ACK
		dieHeader.setFlags(false, true, false, true); //setting DIE flag on
		dieHeader.setChecksum(PRECHECKSUM);
		byte [] headerBytes = dieHeader.getHeaderBytes();

		DatagramPacket diePacket = new DatagramPacket
				(
					headerBytes,
					headerBytes.length,
					clientIpAddress,
					clientPort
				);
		
		state = ServerState.LAST_ACK;

		serverSocket.send(diePacket);
		System.out.println("Server DIE has been sent");
	}

	private boolean isValidPacketHeader(RTPPacketHeader header)
	{
		int headerChecksumed = CheckSum.getChecksum(header.getChecksum());

		return headerChecksumed == CHECKSUM;
	}

	private RTPPacketHeader getHeader(DatagramPacket receivePacket)
	{
		return new RTPPacketHeader(Arrays.copyOfRange(receivePacket.getData(), 0, 20));
	}

	
	public ServerState getServerState(){
		return state;
	}
	
	public int getWindowSize(){
		return windowSize;
	}
	
	public void setWindowSize(int window){
		this.windowSize=window;
	}
	
	public byte[] getFile()
	{
		return fileData;
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
