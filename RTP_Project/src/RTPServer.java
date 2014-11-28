
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

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
	private Scanner scan;
	
	private ServerState state;
	private int seqNum, ackNum;
	private String pathName="";
	private ArrayList<byte[]> bytesReceived;
	private byte[] fileData;
	private boolean timedTaskRun= false;

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
	}

	public void connect()
	{
		try
		{
			serverSocket = new DatagramSocket(serverPort, serverIpAddress);
			serverSocket.setSoTimeout(2000);
		}
		catch (SocketException e)
		{
			e.printStackTrace();
		}
	}
	
	public String openSession()
	{

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
				System.out.println("receiving");
				serverSocket.receive(receivePacket);
				System.out.println("received");
				// Get Header of Packet and 
				RTPPacketHeader receiveHeader = RTPTools.getHeader(receivePacket);
				// Checksum validation
				if (!RTPTools.isValidPacketHeader(receivePacket))
				{
					System.out.println("here");
					resendPacket(receivePacket, false);
					continue;
				}

				// ==== Packet is valid
				if (!receiveHeader.isLive() && !receiveHeader.isDie() && !receiveHeader.isAck() && !receiveHeader.isFirst())
				{
					if (receiveHeader.getSeqNum() == ackNum)
					{
						resendPacket(receivePacket, true);
					}
					else if (receiveHeader.getSeqNum() == ackNum + 1)
					{
						receiveDataPacket(receivePacket);
						System.out.println("resending response packet");
						if (receiveHeader.isLast())
						{
							System.out.println("assembling file");
							System.out.println(this.bytesReceived.size());
							assembleFile();
						}	
					}
				}
				// SENDING FILE NAME FOR UPLOAD = FIRST
				else if ( receiveHeader.isFirst() && !receiveHeader.isLive() && !receiveHeader.isDie() && !receiveHeader.isAck() && !receiveHeader.isLast())
				{
					receiveName(receivePacket);
					System.out.println("Read Name");
				}
				// ASK FOR DOWNLOAD = LIVE FIRST DIE
				else if (receiveHeader.isLive() && receiveHeader.isFirst() && receiveHeader.isDie() && !receiveHeader.isAck() && !receiveHeader.isLast())
				{
					System.out.println("Checking if can download");
					sendDownloadAck(receivePacket);
				}
				// Uploading to client = LIVE FIRST
				else if (receiveHeader.isFirst() && receiveHeader.isLive() && !receiveHeader.isDie() && !receiveHeader.isAck() && !receiveHeader.isLast())
				{
					System.out.println("UPLOAD?");
					sendDownload(receivePacket);
				}
				// HAND SHAKE ONE = LIVE
				else if (receiveHeader.isLive() && !receiveHeader.isDie() && !receiveHeader.isAck() && !receiveHeader.isFirst() && !receiveHeader.isLast())
				{
					System.out.println("HAND SHAKE ONE");
					handShakeOne(receivePacket);
				}
				// HAND SHAKE TWO = LIVE LAST
				else if (receiveHeader.isLive() && receiveHeader.isLast() && !receiveHeader.isDie() && !receiveHeader.isAck() && !receiveHeader.isFirst())
				{
					System.out.println("HAND SHAKE TWO");
					handShakeTwo(receivePacket);
					System.out.println(seqNum + " ack num : " + ackNum);
				}
				// close = DIE
				else if (receiveHeader.isDie() && !receiveHeader.isLive() && !receiveHeader.isAck() && !receiveHeader.isFirst() && !receiveHeader.isLast())
				{
					close();
				}	
			}
			catch (SocketTimeoutException s)
			{
				return "hack";
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return pathName;
	}

	
		
	private void resendPacket(DatagramPacket receivePacket, boolean wasAcked) throws IOException
	{
		RTPPacketHeader receiveHeader = RTPTools.getHeader(receivePacket);
		RTPPacketHeader resendHeader = new RTPPacketHeader();
		resendHeader.setSource(serverPort);
		resendHeader.setDestination(clientPort);
		resendHeader.setChecksum(PRECHECKSUM);
		resendHeader.setWindow(DATA_SIZE);
		if (wasAcked)
		{
			resendHeader.setFlags
			(
				false,
				false,
				true,
				false,
				receiveHeader.isLast()
			);
			resendHeader.setSeqNum(seqNum);
			resendHeader.setAckNum(ackNum + 1);
		}
		else
		{
			resendHeader.setFlags
			(
				receiveHeader.isLive(),
				receiveHeader.isDie(),
				false,
				receiveHeader.isFirst(),
				receiveHeader.isLast()
			);
		}

		byte[] data = new byte[DATA_SIZE];
		resendHeader.setHashCode(CheckSum.getHashCode(data));
		byte[] resendHeaderBytes = resendHeader.getHeaderBytes();
		byte[] packet = RTPTools.combineHeaderData(resendHeaderBytes, data);
	
		DatagramPacket sendPacket = new DatagramPacket
				(
					packet,
					PACKET_SIZE,
					clientIpAddress,
					clientPort
				);
		serverSocket.send(sendPacket);
	}
	
	private void receiveName(DatagramPacket receivePacket) throws IOException
	{
		bytesReceived = new ArrayList<byte []>();
		// extracts and adds data to ArrayList of byte[]s
		byte[] data = RTPTools.extractData(receivePacket);

		bytesReceived.add(data);
		
		RTPPacketHeader dataAckHeader = new RTPPacketHeader();
		dataAckHeader.setSource(serverPort);
		dataAckHeader.setDestination(clientPort);
		dataAckHeader.setChecksum(PRECHECKSUM);
		dataAckHeader.setSeqNum(0);
		dataAckHeader.setAckNum(0);
		dataAckHeader.setFlags(false, false, true, true, false);	// ACK FIRST
		dataAckHeader.setWindow(DATA_SIZE);
		byte[] sendData = new byte[DATA_SIZE];
		dataAckHeader.setHashCode(CheckSum.getHashCode(sendData));
		byte[] liveAckHeaderBytes = dataAckHeader.getHeaderBytes();
		byte[] packet = RTPTools.combineHeaderData(liveAckHeaderBytes, sendData);

		DatagramPacket sendPacket = new DatagramPacket
				(
					packet,
					PACKET_SIZE,
					clientIpAddress,
					clientPort
				);
		serverSocket.send(sendPacket);
	}
	
	private void handShakeOne(DatagramPacket receivePacket) throws IOException
	{
		// Receive Packet
		
		// Live Ack Header
		RTPPacketHeader liveAckHeader = new RTPPacketHeader();
		liveAckHeader.setSource(serverPort);
		liveAckHeader.setDestination(clientPort);
		liveAckHeader.setChecksum(PRECHECKSUM);
		liveAckHeader.setSeqNum(0);
		liveAckHeader.setAckNum(0);
		liveAckHeader.setFlags(true, false, true, false, false);	// live, !die, !ack, !last
		liveAckHeader.setWindow(DATA_SIZE);
		byte[] sendData = new byte[DATA_SIZE];
		liveAckHeader.setHashCode(CheckSum.getHashCode(sendData));
		byte[] liveAckHeaderBytes = liveAckHeader.getHeaderBytes();
		byte[] packet = RTPTools.combineHeaderData(liveAckHeaderBytes, sendData);

		state = ServerState.LIVE_RCVD;

		DatagramPacket sendPacket = new DatagramPacket
				(
					packet,
					PACKET_SIZE,
					clientIpAddress,
					clientPort
				);
		serverSocket.send(sendPacket);
	}

	private void handShakeTwo(DatagramPacket receivePacket) throws IOException
	{
		RTPPacketHeader receiveHeader = RTPTools.getHeader(receivePacket);
		int receiveSeqNum = receiveHeader.getSeqNum();

		// Live Ack Header
		RTPPacketHeader liveAckHeader = new RTPPacketHeader();
		liveAckHeader.setSource(serverPort);
		liveAckHeader.setDestination(clientPort);
		liveAckHeader.setChecksum(PRECHECKSUM);

		ackNum = receiveHeader.getSeqNum();
		liveAckHeader.setSeqNum(seqNum);
		liveAckHeader.setAckNum((ackNum + 1) % MAX_SEQ_NUM);
		liveAckHeader.setFlags(true, false, true, false, true);	// live, !die, ack, last
		liveAckHeader.setWindow(DATA_SIZE);
		byte[] sendData = new byte[DATA_SIZE];
		liveAckHeader.setHashCode(CheckSum.getHashCode(sendData));
		byte[] liveAckHeaderBytes = liveAckHeader.getHeaderBytes();
		byte[] packet = RTPTools.combineHeaderData(liveAckHeaderBytes, sendData);

		state = ServerState.ESTABLISHED;

		DatagramPacket sendPacket = new DatagramPacket
				(
					packet,
					PACKET_SIZE,
					clientIpAddress,
					clientPort
				);
		serverSocket.send(sendPacket);
	}

	private void sendDownloadAck(DatagramPacket receivePacket) throws IOException
	{
		RTPPacketHeader receiveHeader = RTPTools.getHeader(receivePacket);
		
		byte[] dataBytes = RTPTools.extractData(receivePacket);
		String fileName = new String(dataBytes); 
		String filePath = System.getProperty("user.dir") + "/" + fileName;
		
		fileData = RTPTools.getFileBytes(filePath);
		
		RTPPacketHeader downloadHeader = new RTPPacketHeader();
		downloadHeader.setSource(serverPort);
		downloadHeader.setDestination(clientPort);
		downloadHeader.setChecksum(PRECHECKSUM);
		ackNum = receiveHeader.getSeqNum();
		seqNum = (seqNum + 1) % MAX_SEQ_NUM;
		downloadHeader.setSeqNum(seqNum);
		downloadHeader.setAckNum((ackNum + 1) % MAX_SEQ_NUM);
		downloadHeader.setWindow(DATA_SIZE);
		if (fileData == null)
		{
			downloadHeader.setFlags(false, true, true, true, false); // DIE TRUE FIRST
		}
		else
		{
			// File is found
			downloadHeader.setFlags(true, false, true, true, false);
			System.out.println("File Found");
		}
		byte[] data = new byte[DATA_SIZE];
		downloadHeader.setHashCode(CheckSum.getHashCode(data));
		
		byte[] liveAckHeaderBytes = downloadHeader.getHeaderBytes();
		byte[] packet = RTPTools.combineHeaderData(liveAckHeaderBytes, data);
		DatagramPacket sendPacket = new DatagramPacket
				(
					packet,
					PACKET_SIZE,
					clientIpAddress,
					clientPort
				);
		serverSocket.send(sendPacket);
	}
	
	private void sendDownload(DatagramPacket receivePacket) throws IOException
	{
		byte[] receiveData = RTPTools.extractData(receivePacket);
		ByteBuffer wrapped = ByteBuffer.wrap(receiveData); // big-endian by default
		int currPacket = wrapped.getInt();
		System.out.println("CurrPacket " + currPacket);
		
		int bytesRemaining = fileData.length - currPacket * DATA_SIZE;
		int data_length = (bytesRemaining <= DATA_SIZE) ? bytesRemaining : DATA_SIZE;
		
		RTPPacketHeader header = new RTPPacketHeader();
		header.setSource(clientPort);
		header.setDestination(serverPort);
		header.setSeqNum(seqNum);
		header.setAckNum((ackNum + 1) % MAX_SEQ_NUM);
		header.setWindow(data_length);
		header.setFlags(false, false, true, true, false); 
		header.setChecksum(PRECHECKSUM);
		header.setWindow(data_length);
		
		if (bytesRemaining <= DATA_SIZE) { // last packet
			header.setFlags(false, false, true, true, true);
		}
		byte [] data = new byte [data_length];
		System.arraycopy(fileData, currPacket * DATA_SIZE, data, 0, data_length);
		header.setHashCode(CheckSum.getHashCode(data));

		byte [] headerBytes = header.getHeaderBytes();
		byte [] packet = RTPTools.combineHeaderData(headerBytes, data);


		DatagramPacket dataPacket = new DatagramPacket
				(
					packet,
					PACKET_SIZE,
					clientIpAddress,
					clientPort
				);
		serverSocket.send(dataPacket);
	}
	
	public DatagramPacket createPacket(int startByteIndex){
		// Setup header for the data packet
		int bytesRemaining = fileData.length - startByteIndex * DATA_SIZE;
		int data_length = (bytesRemaining <= DATA_SIZE) ? bytesRemaining : DATA_SIZE;
		
		RTPPacketHeader header = new RTPPacketHeader();
		header.setSource(clientPort);
		header.setDestination(serverPort);
		header.setSeqNum(seqNum);
		header.setAckNum((ackNum + 1) % MAX_SEQ_NUM);
		header.setWindow(data_length);
		header.setFlags(false, false, false, false, false); 
		header.setChecksum(PRECHECKSUM);
		header.setWindow(data_length);
		
		if (bytesRemaining <= DATA_SIZE) { // last packet
			header.setFlags(false, false, false, false, true);
		}

		byte [] headerBytes = header.getHeaderBytes();
		byte [] data = new byte [DATA_SIZE];
		byte [] packetBytes = new byte [PACKET_SIZE];
		//bytesRemaining should be updated when we successfully get ACK back for successfully transfered packet
		System.arraycopy(headerBytes, 0, packetBytes, 0, HEADER_SIZE);		// copying header
		System.arraycopy(fileData, startByteIndex * DATA_SIZE, packetBytes, HEADER_SIZE, data_length);

		header.setHashCode(CheckSum.getHashCode(data));

		DatagramPacket dataPacket = new DatagramPacket(packetBytes, packetBytes.length, serverIpAddress, serverPort);
		return dataPacket;
	}

	private void receiveDataPacket(DatagramPacket receivePacket) throws IOException
	{
		RTPPacketHeader receiveHeader = RTPTools.getHeader(receivePacket);
		
		// extracts and adds data to ArrayList of byte[]s
		byte[] data = RTPTools.extractData(receivePacket);

		bytesReceived.add(data);
		
		RTPPacketHeader dataAckHeader = new RTPPacketHeader();
		dataAckHeader.setSource(serverPort);
		dataAckHeader.setDestination(clientPort);
		dataAckHeader.setChecksum(PRECHECKSUM);

		ackNum = receiveHeader.getSeqNum();
		seqNum = (seqNum + 1) % MAX_SEQ_NUM;
		dataAckHeader.setSeqNum(seqNum);
		dataAckHeader.setAckNum((ackNum + 1) % MAX_SEQ_NUM);
		dataAckHeader.setFlags(false, false, true, false, false);	// ACK
		dataAckHeader.setWindow(DATA_SIZE);
		if (receiveHeader.isLast())
		{
			dataAckHeader.setFlags(false, false, true, false, true); // ACK LAST
		}
		byte[] dataArr = new byte[DATA_SIZE];
		dataAckHeader.setHashCode(CheckSum.getHashCode(dataArr));
		
		byte[] liveAckHeaderBytes = dataAckHeader.getHeaderBytes();
		byte[] packet = RTPTools.combineHeaderData(liveAckHeaderBytes, dataArr);
		
		DatagramPacket sendPacket = new DatagramPacket
				(
					packet,
					PACKET_SIZE,
					clientIpAddress,
					clientPort
				);
		serverSocket.send(sendPacket);
	}
	
	private void assembleFile()
	{
		String fileName = new String(bytesReceived.remove(0));
		int bufferLength = bytesReceived.size();
		System.out.println(bufferLength);
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
		String fileDir = System.getProperty("user.dir") + "/" + fileName;
		RTPTools.getFileFromBytes(fileDir, fileData);
//		getFileFromBytes("C:\\Users\\Eileen\\Test\\DHCPMsgExplanation.txt", fileData);
		
		//clearing out byte received buffer and file data for next uploads
		fileData = null;
		bytesReceived = new ArrayList<byte []> ();
		System.out.println("EXIT ASSEMBLE");
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
				
				RTPPacketHeader receiveHeader = RTPTools.getHeader(receivePacket);
				
				// Checksum validation for data received from client
				if (!RTPTools.isValidPacketHeader(receiveHeader))
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

	public boolean terminate(){
		byte[] receiveMessage = new byte[PACKET_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);

		// Setup header for the DIE packet
		RTPPacketHeader dieHeader = new RTPPacketHeader();
		dieHeader.setSource(clientPort);
		dieHeader.setDestination(serverPort);
		dieHeader.setSeqNum(0); //should have last seq num
		dieHeader.setAckNum(0);
		dieHeader.setFlags(false, true, false, false, false); //setting DIE flag on
		dieHeader.setChecksum(PRECHECKSUM);
		byte [] headerBytes = dieHeader.getHeaderBytes();

		DatagramPacket terminatePacket = new DatagramPacket(headerBytes, HEADER_SIZE, clientIpAddress, clientPort);
		
		int tries = 0;
		state = ServerState.CLOSE_WAIT1;
		while (state != ServerState.CLIENT_ACK_SENT){
			try
			{
				System.out.println("tries:" + tries);
				serverSocket.send(terminatePacket);
				serverSocket.receive(receivePacket);

				RTPPacketHeader receiveHeader = RTPTools.getHeader(receivePacket);

				System.out.println(receiveHeader.getSeqNum());
				if (!RTPTools.isValidPacketHeader(receiveHeader))
				{
					continue;
				}
				System.out.println(receiveHeader.isLive() + " " + receiveHeader.isDie() + " " + receiveHeader.isAck() + " " + receiveHeader.isLast());
				if (receiveHeader.isDie() && receiveHeader.isAck() && !receiveHeader.isLast()){
					System.out.println("ACK from server has been sent. State is now: SERVER_ACK_SENT");
					state=ServerState.CLIENT_ACK_SENT;
				}
			}
			catch (SocketTimeoutException s){
				System.out.println("Timeout, resend");
				if(tries++>=5){
					System.out.println("Unsuccessful Connection");
					return false;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		//entering the state where it waits for server to send DIE
		state = ServerState.CLOSE_WAIT2;
		System.out.println("State: CLOSE_WAIT_2");
		tries = 0;
		Timer timer = null;
		while (state != ServerState.TIMED_WAIT || timedTaskRun){
			try{
				serverSocket.receive(receivePacket);
				RTPPacketHeader receiveHeader = RTPTools.getHeader(receivePacket);
				if (!RTPTools.isValidPacketHeader(receiveHeader))
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
					return false;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		System.out.println("exit termination");
		
		return true;
		
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
		dieAckHeader.setFlags(false, true, true, false, false); //ack, die flags on
		
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
	
	private void sendCloseAckState() throws IOException
	{	
		//makes new ACK header 
		RTPPacketHeader ackHeader = new RTPPacketHeader();
		ackHeader.setSource(clientPort);
		ackHeader.setDestination(serverPort);
		ackHeader.setChecksum(PRECHECKSUM);
		ackHeader.setSeqNum(0);
		ackHeader.setAckNum(0);

		state = ServerState.TIMED_WAIT;
		ackHeader.setFlags(false, true, true, false, true); //die, ack, last flags on

		byte[] ackHeaderBytes = ackHeader.getHeaderBytes();

		DatagramPacket ackPacket = new DatagramPacket
				(
					ackHeaderBytes,
					HEADER_SIZE,
					serverIpAddress,
					serverPort
				);	
		serverSocket.send(ackPacket);
	}

	private void sendDieCloseState() throws IOException{
		// Setup header for the DIE packet
		RTPPacketHeader dieHeader = new RTPPacketHeader();
		dieHeader.setSource(clientPort);
		dieHeader.setDestination(serverPort);
		dieHeader.setSeqNum(10); //should have last seq num 
		dieHeader.setAckNum(0); //?????? What should these be after the ACK
		dieHeader.setFlags(false, true, false, false, true); //setting DIE flag on
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
			state=ServerState.CLOSED;
			serverSocket.close();
			System.out.println("Task has been run");
			timedTaskRun = true;
		}
	}
}

