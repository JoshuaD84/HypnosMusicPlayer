package net.joshuad.hypnos;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

/* Note: this class is not designed for repeat uses. */

public class InitialScanFileVisitor extends SimpleFileVisitor <Path> {

	private static transient final Logger LOGGER = Logger.getLogger( InitialScanFileVisitor.class.getName() );
	
	private boolean walkInterrupted = false;
	
	Library library;
	
	private static long sleepTime = 2;
	
	private long directoryTotalCount = -1;
	private long directoriesVisited = 0;
	String message = "";
	
	public InitialScanFileVisitor ( Library library, String message, long directoryTotalCount ) {
		this.library = library;
		this.message = message;
		this.directoryTotalCount = directoryTotalCount;
	}
	
	public static void setSleepTimeBetweenVisits ( long timeMS ) {
		sleepTime = timeMS;
	}
	
	@Override
	public FileVisitResult visitFile ( Path file, BasicFileAttributes attr ) {
		
		if ( walkInterrupted ) {
			return FileVisitResult.TERMINATE;
		}

		try {
			Thread.sleep( sleepTime );
		} catch ( InterruptedException e ) {
			LOGGER.fine ( "Sleep interrupted during walk break." );
		}
		
		if ( Utils.isMusicFile ( file ) ) {
				
			if ( Utils.isAlbumDirectory( file.getParent() ) ) {
				try {
					Album album = new Album ( file.getParent() );
				
					library.addAlbum( album );
					library.addTracks( album.getTracks() );
					return FileVisitResult.SKIP_SIBLINGS;
					
				} catch ( Exception e ) {
					LOGGER.info( "Unable to load album track. " + file.toAbsolutePath().toString() );
					return FileVisitResult.CONTINUE;
				}
				
			} else {
				Track track = new Track( file );
				
				try {
					if ( !library.containsTrack( track ) ) {
						library.addTrack( track );
					}
				} catch ( Exception e ) {
					LOGGER.log( Level.INFO, "Error reading track from file, skipping. " + file.toString(), e );
				}

				return FileVisitResult.CONTINUE;
			}
	        
		} else {
			return FileVisitResult.CONTINUE;
		}
	}
	
	@Override
	public FileVisitResult postVisitDirectory( Path dir, IOException exc ) {
		directoriesVisited++;
		Hypnos.getUI().setLibraryLoaderStatus ( message, directoriesVisited / (double)directoryTotalCount );
		return FileVisitResult.CONTINUE;
	}
	
	@Override	
	public FileVisitResult visitFileFailed( Path file, IOException exc ) {
		return FileVisitResult.CONTINUE;
	}
	
	public void interrupt() {
		walkInterrupted = true;
	}
		
	public boolean getWalkInterrupted() {
		return walkInterrupted;
	}
}

