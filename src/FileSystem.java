import java.util.Vector;

public class FileSystem {
    private Superblock superblock;
    private Directory directory;
    private FileTable filetable;

    public FileSystem( int diskBlocks ) {
        // create superblock, and format disk with 64 inodes in default
        superblock = new Superblock( diskBlocks );
    
        // create directory, and register "/" in directory entry 0
        directory = new Directory( superblock.inodeBlocks );
    
        // file table is created, and store directory in the file table
        filetable = new FileTable( directory );
    
        // directory reconstruction
        FileTableEntry dirEnt = open( "/", "r" );
        int dirSize = fsize( dirEnt );
        if ( dirSize > 0 ) {
            byte[] dirData = new byte[dirSize];
            read( dirEnt, dirData );
            directory.bytes2directory( dirData );
        }
        if(dirEnt == null)
            SysLib.cout("DirEnt is null");
        close( dirEnt );
    }

    void sync( ) {
        // directory synchronizatioin
        FileTableEntry dirEnt = open( "/", "w" );
        byte[] dirData = directory.directory2bytes( );
        write( dirEnt, dirData );
        close( dirEnt );

        // superblock synchronization
        superblock.sync( );
    }

    boolean format( int files ) {
        // wait until all filetable entries are destructed

        while ( filetable.fempty( ) == false );

        // format superblock, initialize inodes, and create a free list
        superblock.format( files );

        // create directory, and register "/" in directory entry 0
        directory = new Directory( superblock.inodeBlocks );
    
        // file table is created, and store directory in the file table
        filetable = new FileTable( directory );
    
        return true;
    }
    // you implement
    FileTableEntry open( String filename, String mode ) {
        // filetable entry is allocated
        //check if Directory contains filename
        FileTableEntry entry = filetable.falloc(filename, mode);
        if(mode.equals("w")){
            if(deallocAllBlocks(entry) == false){
                return null;
            }
        }
        return entry;
    }

    boolean close( FileTableEntry ftEnt ) {
        // filetable entry is freed
        synchronized ( ftEnt ) {
            // need to decrement count; also: changing > 1 to > 0 below
            ftEnt.count--;
            if ( ftEnt.count > 0 ) // my children or parent are(is) using it
                return true;
        }
        return filetable.ffree( ftEnt );
    }
    int fsize( FileTableEntry ftEnt ) {
        if (ftEnt == null || ftEnt.inode == null) {
            return -1;
        }
        int size = ftEnt.inode.length;
        return size;
    }


    int read( FileTableEntry ftEnt, byte[] buffer ) {
        if (ftEnt == null) {
            return -1;
        }
        if ( ftEnt.mode == "w" || ftEnt.mode == "a" )
            return -1;
    
        int offset   = 0;              // buffer offset
        int left     = buffer.length;  // the remaining data of this buffer
    
        synchronized ( ftEnt ) {
			// repeat reading until no more data  or reaching EOF
            while(ftEnt.seekPtr < ftEnt.inode.length){
                int blockNumber = ftEnt.inode.findTargetBlock(superblock, ftEnt.seekPtr);
                byte[] data = new byte[Disk.blockSize];
                SysLib.rawread(blockNumber, data);
                int index = ftEnt.seekPtr % Disk.blockSize;
                for(; index < data.length; index++){
                    buffer[offset] = data[index];
                    offset++;
                    ftEnt.seekPtr++;
                    if(ftEnt.seekPtr == ftEnt.inode.length || offset == left){
                        return offset;
                    }
                }
            }
            return offset;
        }
    }

    int write( FileTableEntry ftEnt, byte[] buffer ) {
        if (ftEnt == null) {
            // make new file table entry
            SysLib.cout("Empty file table entry");
            return -1;
        }
        // at this point, ftEnt is only the one to modify the inode
        if ( ftEnt.mode == "r" )
            return -1;
    
        synchronized ( ftEnt ) {
            int offset   = 0;              // buffer offset
            int left     = buffer.length;  // the remaining data of this buffer

            if (ftEnt.mode == "a") {
                // append buffer
                return append(ftEnt, 0, buffer);
            }
            else {
                // write to file
                // we have to deallocate the existing data

                while(ftEnt.seekPtr < ftEnt.inode.length && offset < buffer.length){
                    int currentBlock = ftEnt.seekPtr/Disk.blockSize;
                    int currentIndex = ftEnt.seekPtr % Disk.blockSize;
                    byte[] data = new byte[Disk.blockSize];
                    SysLib.rawread(ftEnt.inode.getBlockID(superblock, currentBlock), data);
                    while(ftEnt.seekPtr < ftEnt.inode.length &&
                            currentIndex < Disk.blockSize &&
                            offset < buffer.length){
                        data[currentIndex] = buffer[offset];
                        currentIndex++;
                        offset++;
                        ftEnt.seekPtr++;
                    }
                    SysLib.rawwrite(ftEnt.inode.getBlockID(superblock, currentBlock), data);
                    if(offset == buffer.length){
                        return offset;
                    }
                    if(ftEnt.seekPtr == ftEnt.inode.length){
                        break;
                    }
                }
                if(offset < buffer.length){
                    return append(ftEnt, offset, buffer);
                }
                return offset;
            }
        }
    }
    private int append(FileTableEntry ftEnt, int offset, byte[] buffer){
        int usedBlock = ftEnt.inode.length/Disk.blockSize;
        int leftLength = ftEnt.inode.length % Disk.blockSize;
        ftEnt.seekPtr = ftEnt.inode.length;
        if(leftLength != 0){
            int blockNumber = ftEnt.inode.getBlockID(superblock, usedBlock);
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread(blockNumber, data);
            while(leftLength < Disk.blockSize && offset < buffer.length){
                data[leftLength] = buffer[offset];
                leftLength++;
                offset++;
                ftEnt.seekPtr++;
                ftEnt.inode.length++;
            }
            SysLib.rawwrite(blockNumber, data);
            usedBlock++;
        }
        while (offset < buffer.length){
            byte[] data = new byte[Disk.blockSize];
            int index = 0;
            while(offset < buffer.length && index < data.length){
                data[index] = buffer[offset];
                index++;
                offset ++;
                ftEnt.inode.length++;
                ftEnt.seekPtr++;
            }
            int freeBlock = superblock.getFreeBlock();
            SysLib.rawwrite(freeBlock, data);
            ftEnt.inode.setBlockID(superblock, usedBlock, (short)freeBlock);
            usedBlock++;

        }
        return offset;
    }

    private boolean deallocAllBlocks( FileTableEntry ftEnt ) {
        ftEnt.inode.length = 0;
        ftEnt.seekPtr = 0;
        return true;
    }
    boolean delete( String filename ) {
        FileTableEntry ftEnt = open( filename, "w" );
        short iNumber = ftEnt.iNumber;
        return close( ftEnt ) && directory.ifree( iNumber );
    }

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    int seek( FileTableEntry ftEnt, int offset, int whence ) {
        synchronized ( ftEnt ) {
            /*
            System.out.println( "seek: offset=" + offset +
                    " fsize=" + fsize( ftEnt ) +
                    " seekptr=" + ftEnt.seekPtr +
                    " whence=" + whence );
            */
            // set seekptr to beginning of file
            if (whence == SEEK_SET) {
                ftEnt.seekPtr = offset;
            }
            // set seekptr to end of file
            if (whence == SEEK_END) {
                ftEnt.seekPtr = ftEnt.inode.length + offset;
            }
            if(whence == SEEK_CUR){
                ftEnt.seekPtr = ftEnt.seekPtr + offset;
            }

            // check if seekptr is out of range, set to end point
            if (ftEnt.seekPtr < 0) {
                ftEnt.seekPtr = 0;
            }
            if (ftEnt.seekPtr > ftEnt.inode.length) {
                ftEnt.seekPtr = ftEnt.inode.length;
            }
		}
        return ftEnt.seekPtr;
    }

}
