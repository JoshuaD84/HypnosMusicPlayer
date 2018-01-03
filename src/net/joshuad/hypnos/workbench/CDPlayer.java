package net.joshuad.hypnos.workbench;

import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

public class CDPlayer {
	
	public static void main ( String[] args ) {
		FileSystem fs = FileSystems.getDefault();
		
		for ( FileStore store : fs.getFileStores() ) {
			System.out.println ( store.name() );
		}
	}

}
