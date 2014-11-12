
public class RTPPacketHeader {
	static final int HEADERLENGTH = 20;
	
	/*
	 * Header Field offsets
	 */
	static final int SRC 	= 0;
	static final int DST	= 2;
	static final int SEQ 	= 4;
	static final int ACK 	= 8;
	static final int WIN 	= 12;
	static final int FLAG 	= 14;
	static final int CHKSUM = 16;
	
	private byte[] header;
	
	public RTPPacketHeader()
	{
		header = new byte[20];
	}

	public void setSource(short portNumber)
	{
		header[SRC] = (byte) ((portNumber & 0xF0) >> 4);
		header[SRC + 1] = (byte) (portNumber & 0x0F); 
	}
	
	public void setDestination(short portNumber)
	{
		header[DST] = (byte) ((portNumber & 0xF0) >> 4);
		header[DST + 1] = (byte) (portNumber & 0x0F);
	}

	public void setSeqNum(int sequenceNumber)
	{
		header[SEQ]		= (byte) ((sequenceNumber & 0xF000) >> 12);
		header[SEQ + 1] = (byte) ((sequenceNumber & 0x0F00) >> 8);
		header[SEQ + 2]	= (byte) ((sequenceNumber & 0x00F0) >> 4);
		header[SEQ + 3]	= (byte) (sequenceNumber & 0x000F);
	}

	public void setAckNum(int ackNumber)
	{
		header[ACK]		= (byte) ((ackNumber & 0xF000) >> 12);
		header[ACK + 1] = (byte) ((ackNumber & 0x0F00) >> 8);
		header[ACK + 2]	= (byte) ((ackNumber & 0x00F0) >> 4);
		header[ACK + 3]	= (byte) (ackNumber & 0x000F);
	}

	public void setWindow(int windowSize)
	{
		header[WIN] = (byte) ((windowSize & 0xF0) >> 4);
		header[WIN + 1] = (byte) (windowSize & 0x0F);		
	}

	public void setFlags(boolean live, boolean die, boolean ack, boolean last)
	{
		byte flag = 0;
		if (live) flag |= (byte) (1 << 3);
		if (die)  flag |= (byte) (1 << 2);
		if (ack)  flag |= (byte) (1 << 1);
		if (last) flag |= (byte) (1);
		
		header[FLAG] = flag;
	}

	public void setChecksum(int checksum)
	{
		header[CHKSUM]		= (byte) ((checksum & 0xF000) >> 12);
		header[CHKSUM + 1]  = (byte) ((checksum & 0x0F00) >> 8);
		header[CHKSUM + 2]	= (byte) ((checksum & 0x00F0) >> 4);
		header[CHKSUM + 3]	= (byte) (checksum & 0x000F);
	}
	
	public byte[] getHeaderBytes()
	{
		return header;
	}
}