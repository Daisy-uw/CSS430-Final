import java.util.Vector;
public class FileTable {
	// File Structure Table

    private Vector<FileTableEntry> table;	// the entity of File Structure Table
    private Directory dir;         		// the root directory
    
    public FileTable ( Directory directory ) {	// a default constructor
		table = new Vector<FileTableEntry>( );	// instantiate a file table
		dir = directory;                     	// instantiate the root directory
    }

	// you implement
	public synchronized FileTableEntry falloc( String fname, String mode ) {
		int inumber = dir.namei(fname);
		if(inumber == -1){
			inumber = dir.ialloc(fname);
			if(inumber == -1){
				SysLib.cout("Cannot allocate " + fname + " with mode = " + mode );
				throw new RuntimeException();
			}
		}else{
			SysLib.cout("Found a record in directory with file name = " + fname);
			for(int i = 0; i< table.size(); i++){
				FileTableEntry entry = table.get(i);
				if(entry.iNumber == inumber && entry.mode.equals(mode)){
					return entry;
				}
			}
			SysLib.cout("Cannot find an entry with file name = " + fname + " and mode = " + mode);
			throw new RuntimeException();
		}
		Inode inode = new Inode((short)inumber);
		FileTableEntry entry = new FileTableEntry(inode, (short) inumber, mode);
		table.add(entry);
		return entry;
	}

    public synchronized boolean ffree( FileTableEntry e ) {
	// receive a file table entry
	// free the file table entry corresponding to this index
	if ( table.removeElement( e ) == true ) { // find this file table entry
	    e.inode.count--;       // this entry no longer points to this inode
	    switch( e.inode.flag ) {
	    case 1: e.inode.flag = 0; break;
	    case 2: e.inode.flag = 0; break;
	    case 4: e.inode.flag = 3; break;
	    case 5: e.inode.flag = 3; break;
	    }
	    e.inode.toDisk( e.iNumber );     // reflect this inode to disk
	    e = null;                        // this file table entry is erased.
	    notify( );
	    return true;
	} else
	    return false;
    }

    public synchronized boolean fempty( ) {
		return table.isEmpty( );             // return if table is empty
    }                                        // called before a format
    
    public Vector<FileTableEntry> getEntries(){
    	return this.table;
    }
}
