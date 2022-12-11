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

    FileTableEntry open( String filename, String mode ) {
        // filetable entry is allocated
        //check if Directory contains filename
        int inumber = directory.namei(filename);
        if(inumber == -1){
            return null;
        }
        Vector<FileTableEntry> entries = filetable.getEntries();
        for(int i = 0; i< entries.size(); i++){
            FileTableEntry entry = entries.get(i);
            if(inumber == entry.iNumber && mode == entry.mode){
                return entry;
            }
        }
        return null;
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
        if ( ftEnt.mode == "w" || ftEnt.mode == "a" )
            return -1;
    
        int offset   = 0;              // buffer offset
        int left     = buffer.length;  // the remaining data of this buffer
    
        synchronized ( ftEnt ) {
			// repeat reading until no more data  or reaching EOF


        }
        //just for compile problem, need change
        return 0;
    }

    int write( FileTableEntry ftEnt, byte[] buffer ) {
        // at this point, ftEnt is only the one to modify the inode
        if ( ftEnt.mode == "r" )
            return -1;
    
        synchronized ( ftEnt ) {
            int offset   = 0;              // buffer offset
            int left     = buffer.length;  // the remaining data of this buffer
    

        }
        //just for compile problem, need change
        return 0;
    }

    private boolean deallocAllBlocks( FileTableEntry ftEnt ) {

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
                ftEnt.seekPtr = SEEK_SET;
            }
            // set seekptr to end of file
            if (whence == SEEK_END) {
                ftEnt.seekPtr = ftEnt.inode.length;
            }
            // apply offset to seekptr
            ftEnt.seekPtr = ftEnt.seekPtr + offset;
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
