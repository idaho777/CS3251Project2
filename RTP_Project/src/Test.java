import java.util.zip.Adler32;



public class Test {

	public static void main(String[] args) {
		

	}
	
	
	/**
	 * Calculates checksum based on byte [] data using Adler32
	 * @param data
	 * @return checksum value
	 */
	public long getChecksum(byte [] data){
		Adler32 checksum = new Adler32();
		checksum.update(data, 0, data.length);
		long checksumVal = checksum.getValue();
		return checksumVal;
		
		//note have a final number on each side, for example
		//3251 on client 
		//3251's checksum on server side, for example 9000
		//so then put the 3251 in the client header and run checksum
		//on the data when it gets through, make sure it matches da 9000
		
		/**
		 * Validate packet
		 * -check IP Address, 
		 * -checksum
		 * -check flags
		 */
	}
	
	
}
