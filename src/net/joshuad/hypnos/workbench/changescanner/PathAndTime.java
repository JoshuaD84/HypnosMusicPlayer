package net.joshuad.hypnos.workbench.changescanner;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

class PathAndTime implements Serializable {
	private static final long serialVersionUID = 1L;
	
	File file;
	long timeMS;
	
	public PathAndTime ( Path path, FileTime time ) {
		this.file = path.toFile();
		this.timeMS = time.toMillis();
	}
	
	public File getFile() {
		return file;
	}
	
	public long getTimeMS() {
		return timeMS;
	}
	
	public boolean equals( Object object ) {
		if ( object == null ) return false;
		if  ( !(object instanceof PathAndTime) ) return false;
		
		File inputPath = ((PathAndTime)object).getFile();
				
		if ( file == null || inputPath == null ) return false;
		
		else return file.equals( inputPath );
	}
}
