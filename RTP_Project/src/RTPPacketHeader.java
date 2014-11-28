
public class RTPPacketHeader {
	static final int HEADERLENGTH = 20;
	
	/*
	 * Header Field offsets
	 */
	static final int SRC 	= 0;
	static final int DST	= 2;
	static final int SEQ 	= 4;
	static final int ACK 	= 6;
	static final int WIN 	= 8;
	static final int FLAG 	= 10;
	static final int CHKSUM = 12;
	static final int HASHCODE = 16;
	
	private byte[] header;
	
	public RTPPacketHeader()
	{
		this(new byte[20]);
	}

	public RTPPacketHeader(byte[] headerArray)
	{
		if (headerArray.length != 20)
		{
			header = new byte[20];
		}
		else
		{
			header = headerArray;
		}
	}
	
	public void setSource(int portNumber)
	{
		header[SRC] = (byte) ((portNumber & 0xFF00) >> 8);
		header[SRC + 1] = (byte) (portNumber & 0x00FF); 
	}
	
	public int getSource()
	{
		return (int) (header[SRC] << 8 & 0xFF00 | header[SRC + 1] & 0x00FF);
	}
	
	public void setDestination(int portNumber)
	{
		header[DST] 	= (byte) ((portNumber & 0xFF00) >> 8);
		header[DST + 1] = (byte) (portNumber & 0x00FF);
	}

	public int getDestination()
	{
		return (int) (header[DST] << 8 & 0xFF00 | header[DST + 1] & 0x00FF);
	}
	
	public void setSeqNum(int sequenceNumber)
	{
		header[SEQ]		= (byte) ((sequenceNumber & 0xFF00) >> 8);
		header[SEQ + 1]	= (byte)  (sequenceNumber & 0x00FF);
	}

	public int getSeqNum()
	{
		return (int) (header[SEQ] << 8 & 0xFF00|
				  		header[SEQ + 1] & 0x00FF);
	}
	
	public void setAckNum(int ackNumber)
	{
		header[ACK]		= (byte) ((ackNumber & 0x0000FF00) >> 8);
		header[ACK + 1]	= (byte)  (ackNumber & 0x000000FF);
	}
	
	public int getAckNum()
	{
		return (int) (header[ACK] << 8 & 0xFF00|
				  		header[ACK + 1] & 0x00FF);
	}

	public void setWindow(int windowSize)
	{
		header[WIN] = (byte) ((windowSize & 0xFF00) >> 8);
		header[WIN + 1] = (byte) (windowSize & 0x00FF);		
	}

	public int getWindow()
	{
		return (int) (header[WIN] << 8 & 0xFF00 | header[WIN + 1] & 0x00FF);
	}
	
	public void setFlags(boolean live, boolean die, boolean ack, boolean fst, boolean last)
	{
		byte flag = 0;
		if (live) flag |= (byte) (1 << 7);
		if (die)  flag |= (byte) (1 << 6);
		if (ack)  flag |= (byte) (1 << 5);
		if (fst)  flag |= (byte) (1 << 4);
		if (last) flag |= (byte) (1 << 3);
		
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
	
	public boolean isFirst()
	{
		return ((header[FLAG] & 0b00010000) != 0);
	}
	
	public boolean isLast()
	{
		return ((header[FLAG] & 0b00001000) != 0);
	}
	
	
	
	public void setChecksum(int checksum)
	{
		header[CHKSUM]		= (byte) ((checksum & 0xFF000000) >> 24);
		header[CHKSUM + 1]  = (byte) ((checksum & 0x00FF0000) >> 16);
		header[CHKSUM + 2]	= (byte) ((checksum & 0x0000FF00) >> 8);
		header[CHKSUM + 3]	= (byte) (checksum  & 0x000000FF);
	}
	
	public int getChecksum()
	{
		return (int) (header[CHKSUM] << 24 & 0xFF000000|
				  header[CHKSUM + 1] << 16 & 0x00FF0000|
				  header[CHKSUM + 2] << 8  & 0x0000FF00|
				  header[CHKSUM + 3] 	   & 0x000000FF);
	}
	
	public void setHashCode(int checksum)
	{
		header[HASHCODE]		= (byte) ((checksum & 0xFF000000) >> 24);
		header[HASHCODE + 1]    = (byte) ((checksum & 0x00FF0000) >> 16);
		header[HASHCODE + 2]	= (byte) ((checksum & 0x0000FF00) >> 8);
		header[HASHCODE + 3]	= (byte) (checksum  & 0x000000FF);
	}
	
	public int getHashCode()
	{
		return (int) (header[HASHCODE] << 24 & 0xFF000000|
				  header[HASHCODE + 1] << 16 & 0x00FF0000|
				  header[HASHCODE + 2] << 8  & 0x0000FF00|
				  header[HASHCODE + 3] 	     & 0x000000FF);
	}
	
	public byte[] getHeaderBytes()
	{
		return header;
	}
}
