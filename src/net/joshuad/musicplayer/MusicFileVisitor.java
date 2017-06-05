package net.joshuad.musicplayer;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

/* Note: this class is not designed for repeat uses. */

public class MusicFileVisitor extends SimpleFileVisitor <Path> {

	private static transient final Logger LOGGER = Logger.getLogger( MusicFileVisitor.class.getName() );
	
	private boolean walkInterrupted = false;
	
	public MusicFileVisitor ( ) {}
	
	@Override
	public FileVisitResult visitFile ( Path file, BasicFileAttributes attr ) {
		
		if ( walkInterrupted ) {
			return FileVisitResult.TERMINATE;
		}

		try {
			Thread.sleep( 10 );
		} catch ( InterruptedException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if ( Utils.isMusicFile ( file ) ) {
				
			if ( Utils.isAlbumDirectory( file.getParent() ) ) {
				try {
					Track track = new Track ( file, true ); //This track is discarded. 
					Album album = new Album ( file.getParent() );
				
					Library.addAlbum( album );
					Library.addTracks( album.getTracks() );
					
					return FileVisitResult.SKIP_SIBLINGS;
				} catch ( IOException e ) {
					LOGGER.log( Level.INFO, "Unable to load album track", e );
					return FileVisitResult.CONTINUE;
				}
				
			} else {
				try {
					Track track = new Track( file, false );
	
					if ( !Library.containsTrack( track ) ) {
						Library.addTrack( track );
					}
				} catch ( IOException e ) { 
					LOGGER.log( Level.INFO, "Unable to load track", e );
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

