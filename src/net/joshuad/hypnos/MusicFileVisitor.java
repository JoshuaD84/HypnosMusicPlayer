package net.joshuad.hypnos;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Logger;

/* Note: this class is not designed for repeat uses. */

public class MusicFileVisitor extends SimpleFileVisitor <Path> {

	private static transient final Logger LOGGER = Logger.getLogger( MusicFileVisitor.class.getName() );
	
	private boolean walkInterrupted = false;
	
	Library library;
	
	private static long sleepTime = 2;
	
	public MusicFileVisitor ( Library library ) {
		this.library = library;
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
					LOGGER.info( e.getClass().getCanonicalName() + ": Unable to load album track. " + file.toAbsolutePath().toString() );
					return FileVisitResult.CONTINUE;
				}
				
			} else {
				Track track = new Track( file );

				if ( !library.containsTrack( track ) ) {
					library.addTrack( track );
				}

				return FileVisitResult.CONTINUE;
			}
	        
		} else {
			return FileVisitResult.CONTINUE;
		}
	}
	
	@Override
	public FileVisitResult postVisitDirectory( Path dir, IOException exc ) {
		return FileVisitResult.CONTINUE;
	}
	
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

