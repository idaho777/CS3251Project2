
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
		this(new byte[20]);
	}

	public RTPPacketHeader(byte[] headerArray)
	{
		header = headerArray;
	}
	
	public void setSource(short portNumber)
	{
		header[SRC] = (byte) ((portNumber & 0xF0) >> 4);
		header[SRC + 1] = (byte) (portNumber & 0x0F); 
	}
	
	public short getSource()
	{
		return (short) (header[SRC] << 4 | header[SRC + 1]);
	}
	
	public void setDestination(short portNumber)
	{
		header[DST] = (byte) ((portNumber & 0xF0) >> 4);
		header[DST + 1] = (byte) (portNumber & 0x0F);
	}

	public short getDestination()
	{
		return (short) (header[DST] << 4 | header[DST + 1]);
	}
	
	public void setSeqNum(int sequenceNumber)
	{
		header[SEQ]		= (byte) ((sequenceNumber & 0xF000) >> 12);
		header[SEQ + 1] = (byte) ((sequenceNumber & 0x0F00) >> 8);
		header[SEQ + 2]	= (byte) ((sequenceNumber & 0x00F0) >> 4);
		header[SEQ + 3]	= (byte) (sequenceNumber & 0x000F);
	}

	public int getSeqNum()
	{
		return (int) (header[SEQ] << 12 |
				header[SEQ + 1] << 8 |
				header[SEQ + 2] << 4 |
				header[SEQ + 3]);
	}
	
	public void setAckNum(int ackNumber)
	{
		header[ACK]		= (byte) ((ackNumber & 0xF000) >> 12);
		header[ACK + 1] = (byte) ((ackNumber & 0x0F00) >> 8);
		header[ACK + 2]	= (byte) ((ackNumber & 0x00F0) >> 4);
		header[ACK + 3]	= (byte) (ackNumber & 0x000F);
	}
	
	public int getAckNum()
	{
		return (int) (header[ACK] << 12 |
				header[ACK + 1] << 8 |
				header[ACK + 2] << 4 |
				header[ACK + 3]);
	}

	public void setWindow(int windowSize)
	{
		header[WIN] = (byte) ((windowSize & 0xF0) >> 4);
		header[WIN + 1] = (byte) (windowSize & 0x0F);		
	}

	public short getWindow()
	{
		return (short) (header[WIN] << 4 | header[WIN + 1]);
	}
	
	public void setFlags(boolean live, boolean die, boolean ack, boolean last)
	{
		byte flag = 0;
		if (live) flag |= (byte) (1 << 7);
		if (die)  flag |= (byte) (1 << 6);
		if (ack)  flag |= (byte) (1 << 5);
		if (last) flag |= (byte) (1 << 4);
		
		header[FLAG] = flag;
	}

	public boolean isLive()
	{
		return ((header[FLAG] & 0b10000000) != 0);
	}
	
	public boolean isDie()
	{
		return ((header[FLAG] & 0b01000000) != 0);
	}
	
	public boolean isAck()
	{
		return ((header[FLAG] & 0b00100000) != 0);
	}
	
	public boolean isLast()
	{
		return ((header[FLAG] & 0b00010000) != 0);
	}
	
	public void setChecksum(int checksum)
	{
		header[CHKSUM]		= (byte) ((checksum & 0xF000) >> 12);
		header[CHKSUM + 1]  = (byte) ((checksum & 0x0F00) >> 8);
		header[CHKSUM + 2]	= (byte) ((checksum & 0x00F0) >> 4);
		header[CHKSUM + 3]	= (byte) (checksum & 0x000F);
	}
	
	public int getChecksum(int checksum)
	{
		return (int) (header[CHKSUM] << 12 |
				header[CHKSUM + 1] << 8 |
				header[CHKSUM + 2] << 4 |
				header[CHKSUM + 3]);
	}
	
	public byte[] getHeaderBytes()
	{
		return header;
	}
}
