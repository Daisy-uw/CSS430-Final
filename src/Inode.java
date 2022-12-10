public class Inode {
	public final static int iNodeSize = 32;  // fixed to 32 bytes
    public final static int directSize = 11; // # direct pointers

    public final static int NoError              = 0;
    public final static int ErrorBlockRegistered = -1;
    public final static int ErrorPrecBlockUnused = -2;
    public final static int ErrorIndirectNull    = -3;

    public int length;                 // file size in bytes
    public short count;                // # file-table entries pointing to this
    public short flag;       // 0 = unused, 1 = used(r), 2 = used(!r), 
                             // 3=unused(wreg), 4=used(r,wreq), 5= used(!r,wreg)
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // an indirect pointer

    Inode ( ) {                        // a default constructor
	length = 0;
	count = 0;
	flag = 1;
	for ( int i = 0; i < directSize; i++ )
	    direct[i] = -1;
	indirect = -1;
    }

	// making inode from disk
	Inode ( short iNumber ) {                  
		int blkNumber = 1 + iNumber / 16;          // inodes start from block#1
		byte[] data = new byte[Disk.blockSize]; 
		SysLib.rawread( blkNumber, data );         // get the inode block
		int offset = ( iNumber % 16 ) * iNodeSize; // locate the inode top

		length = SysLib.bytes2int( data, offset ); // retrieve all data members
		offset += 4;                               // from data
		count = SysLib.bytes2short( data, offset );
		offset += 2;
		flag = SysLib.bytes2short( data, offset );
		offset += 2;
		for ( int i = 0; i < directSize; i++ ) {
			direct[i] = SysLib.bytes2short( data, offset );
			offset += 2;
		}
		indirect = SysLib.bytes2short( data, offset );
		offset += 2;

		/*
		System.out.println( "Inode[" + iNumber + "]: retrieved " +
					" length = " + length +
					" count = " + count +
					" flag = " + flag +
					" direct[0] = " + direct[0] +
					" indirect = " + indirect );
		*/
    }

 	
	// saving this inode to disk
	void toDisk( short iNumber ) {
		// you implement
		int blkNumber = 1 + iNumber / 16;
		byte[] data = new byte[Disk.blockSize];
		int offset = ( iNumber % 16 ) * iNodeSize;
		SysLib.int2bytes(length, data, offset); //save the length in data
		offset += 4;
		SysLib.short2bytes(count, data, offset); //save the count in data
		offset += 2;
		SysLib.short2bytes(flag, data, offset); // save the flag in data
		offset += 2;
		for(int i = 0; i < directSize; i++){
			SysLib.short2bytes(direct[i], data, offset); //save the direct element in data
			offset += 2;
		}
		SysLib.short2bytes(indirect, data, offset); // save the indirect in data
		SysLib.rawwrite(blkNumber, data); // raw write data to the disk

	}
}
