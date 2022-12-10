
public class Superblock {
    private final int defaultInodeBlocks = 64;
    public int totalBlocks;
    public int inodeBlocks;
    public int freeList;
	
    // you implement
	public Superblock (int diskSize ) {
		// read the superblock from disk
		byte [] data = new byte[Disk.blockSize];
		SysLib.rawread(0, data);
		totalBlocks = SysLib.bytes2int(data, 0);
		inodeBlocks = SysLib.bytes2int(data, 4);
		freeList = SysLib.bytes2int(data, 8);

		if(totalBlocks == diskSize && inodeBlocks >0 && freeList >=2){
			return; // this is a valid super block
		}else{
			totalBlocks = diskSize;
			format(defaultInodeBlocks);
		}
	}
	
	//  helper function
	void sync( ) {
		byte[] superBlock = new byte[Disk.blockSize];
		SysLib.int2bytes( totalBlocks, superBlock, 0 );
		SysLib.int2bytes( inodeBlocks, superBlock, 4 );
		SysLib.int2bytes( freeList, superBlock, 8 );
		SysLib.rawwrite( 0, superBlock );
		SysLib.cerr( "Superblock synchronized\n" );
    	}

    	void format( ) {
		// default format with 64 inodes
		format( defaultInodeBlocks );
    	}
	
	// you implement
	 void format( int files ) {
		// initialize the superblock
		 // need to double check later
		 inodeBlocks = files;
		 freeList = 2;
	 }
	
	// you implement
	public int getFreeBlock( ) {
		// get a new free block from the freelist
		return this.freeList;
	}
	
	// you implement
	// WHAT IS THIS METHOD?? READ DOCUMENTATION TO UNDERSTAND OR ASK CONNOR/PARSONS
	public boolean returnBlock( int oldBlockNumber ) {
	// return this former block
		return true;
	}
	
}
