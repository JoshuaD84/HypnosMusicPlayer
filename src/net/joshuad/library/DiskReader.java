package net.joshuad.library;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.fxui.FXUI;

class DiskReader implements FileVisitor <Path> {

	private static final Logger LOGGER = Logger.getLogger( DiskReader.class.getName() );

	private FileTreeNode currentDirectoryNode = null;
	
	private boolean interrupted = false;
	private boolean interruptRequested = false;
	private long directoriesVisited = 0;
	private long directoriesToScan = 1;
	
	private Path currentRootPath = null;
	
	private Library library;
	private FXUI ui;
	
	DiskReader( Library library ) {
		this.library = library;
	}
	
	void setUI( FXUI ui ) {
		this.ui = ui;
	}
	
	void interrupt() {
		interruptRequested = true;
	}
	
	private void resetState() {
		currentDirectoryNode = null;
		interrupted = false;
		interruptRequested = false;
		directoriesVisited = 0;
		directoriesToScan = 1;
		currentRootPath = null;
	}
	
	public void scanMusicRoot( MusicRoot musicRoot ) {
		library.getLog().println( "[MusicRootLoader] Loading root: " + musicRoot.getPath().toString() );
		resetState();
		
		directoriesToScan = LibraryLoader.getDirectoryCount ( musicRoot.getPath() );
		
		currentRootPath = musicRoot.getPath();
		
		library.diskWatcher.watchAll( musicRoot.getPath() );
		
		try {
			Files.walkFileTree( musicRoot.getPath(), EnumSet.of( FileVisitOption.FOLLOW_LINKS ), Integer.MAX_VALUE, this );
			musicRoot.setNeedsRescan( interrupted );
			
		}  catch ( Exception e ) {
			LOGGER.log( Level.INFO, "Scan failed or incomplete for path, giving up: " + musicRoot.getPath(), e );
			//TODO: Make the UI show this status somehow. 
			musicRoot.setNeedsRescan( false );
			musicRoot.setFailedScan( true );
		}
	
		if ( ui != null ) {
			ui.setLibraryLoaderStatusToStandby();
		}
	}
	
	public void updatePath( Path path ) {
		resetState();
		directoriesToScan = LibraryLoader.getDirectoryCount ( path );
	
		currentRootPath = path;
		library.diskWatcher.watchAll( path );
		
		try {
			Files.walkFileTree( path, EnumSet.of( FileVisitOption.FOLLOW_LINKS ), Integer.MAX_VALUE, this );
			
		}  catch ( Exception e ) {
			//TODO: Decide what to do here
			LOGGER.log( Level.INFO, "Scan failed or incomplete for " + path, e );
		}
	
		if ( ui != null ) {
			ui.setLibraryLoaderStatusToStandby();
		}
	}
	
	@Override
	public FileVisitResult preVisitDirectory ( Path dir, BasicFileAttributes attrs ) throws IOException {

		if ( interruptRequested ) {
			interrupted = true;
			return FileVisitResult.TERMINATE;
		}
				
		FileTreeNode directoryNode = new FileTreeNode ( dir, currentDirectoryNode );
		if ( currentDirectoryNode != null ) {
			currentDirectoryNode.addChild ( directoryNode );
		}
		currentDirectoryNode = directoryNode;

		if ( ui != null ) {
			ui.setLibraryLoaderStatus( "Scanning " + currentRootPath.toString() + "...", directoriesVisited / (double)directoriesToScan );
		}
		
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFile ( Path filePath, BasicFileAttributes attr ) {
		if ( Utils.isMusicFile ( filePath ) ) {
			Track track = new Track ( filePath );
			currentDirectoryNode.addChild ( new FileTreeNode ( filePath, currentDirectoryNode, track ) );
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory ( Path dir, IOException exception ) throws IOException {
		
		directoriesVisited++;
		
		if ( LibraryLoader.isAlbum ( currentDirectoryNode, library.getLog() ) ) {	
			List<Track> tracks = new ArrayList<> ();
			for ( FileTreeNode child : currentDirectoryNode.getChildren() ) {
				if ( child.getTrack() != null ) {
					tracks.add( child.getTrack() );
				}
			}
		
			Album album = new Album( currentDirectoryNode.getPath(), tracks );
			
			
			library.getLog().println( "[MusicRootLoader] Loading album: " + album.getAlbumArtist() + " - " + album.getAlbumTitle() );
			
			currentDirectoryNode.setAlbum( album );
			
			for ( Track track : tracks ) {
				library.merger.addOrUpdateTrack( track );
			}
			library.merger.addOrUpdateAlbum( album );
			
		} else {
			for ( FileTreeNode child : currentDirectoryNode.getChildren() ) {
				if ( child.getTrack() != null ) {
					library.merger.addOrUpdateTrack( child.getTrack() );
				}
			}
			library.merger.notAnAlbum( currentDirectoryNode.getPath() );
		}
			
		
		if ( currentDirectoryNode.getParent() != null ) {
			currentDirectoryNode = currentDirectoryNode.getParent();
		}
		
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed ( Path file, IOException exception ) throws IOException {
		LOGGER.log( Level.INFO, "Unable to scan" + file, exception );
		return FileVisitResult.CONTINUE;
	}
}