package net.joshuad.hypnos;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.nio.file.WatchKey;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import net.joshuad.hypnos.fxui.FXUI;

public class Library {

	private static final Logger LOGGER = Logger.getLogger( Library.class.getName() );
	
	public static final int SAVE_ALL_INTERVAL = 10000;
		
	private WatchService watcher;
    private final HashMap<WatchKey,Path> keys = new HashMap <WatchKey,Path> ();
    
    private MusicFileVisitor fileWalker = null; //YOU MUST SET THIS TO NULL AFTER IT WALKS
    
	private Thread loaderThread;
	private final ModifiedFileUpdaterThread modifiedFileDelayedUpdater;
	
	// These are all three representations of the same data. Add stuff to the
	// Observable List, the other two can't accept add.
	final ObservableList <Album> albums = FXCollections.observableArrayList( new ArrayList <Album>() );
	final FilteredList <Album> albumsFiltered = new FilteredList <Album>( albums, p -> true );
	final SortedList <Album> albumsSorted = new SortedList <Album>( albumsFiltered );

	final ObservableList <Track> tracks = FXCollections.observableArrayList( new ArrayList <Track>() );
	final FilteredList <Track> tracksFiltered = new FilteredList <Track>( tracks, p -> true );
	final SortedList <Track> tracksSorted = new SortedList <Track>( tracksFiltered );

	final ObservableList <Playlist> playlists = FXCollections.observableArrayList( new ArrayList <Playlist>() );
	final FilteredList <Playlist> playlistsFiltered = new FilteredList <Playlist>( playlists, p -> true );
	final SortedList <Playlist> playlistsSorted = new SortedList <Playlist>( playlistsFiltered );
	
	final ObservableList <TagError> tagErrors = FXCollections.observableArrayList( new ArrayList <TagError>() );
	final FilteredList <TagError> tagErrorsFiltered = new FilteredList <TagError>( tagErrors, p -> true );
	final SortedList <TagError> tagErrorsSorted = new SortedList <TagError>( tagErrorsFiltered );
	

	final ObservableList <Path> musicSourcePaths = FXCollections.observableArrayList();
	
	Vector <Album> albumsToAdd = new Vector <Album>();
	Vector <Album> albumsToRemove = new Vector <Album>();
	Vector <Album> albumsToUpdate = new Vector <Album>();

	Vector <Track> tracksToAdd = new Vector <Track>();
	Vector <Track> tracksToRemove = new Vector <Track>();
	Vector <Track> tracksToUpdate = new Vector <Track>();

	Vector <Playlist> playlistsToAdd = new Vector <Playlist>();
	Vector <Playlist> playlistsToRemove = new Vector <Playlist>();
	Vector <Playlist> playlistsToUpdate = new Vector <Playlist>();

	private Vector <Path> sourceToAdd = new Vector <Path>();
	private Vector <Path> sourceToRemove = new Vector <Path>();
	private Vector <Path> sourceToUpdate = new Vector <Path>();
	
	//Vector <TagError> tagErrorsToRemove = new Vector <TagError> ();
	
	private boolean purgeOrphansAndMissing = true;
	
	private int loaderSleepTimeMS = 50;
	
	public Library() {
		modifiedFileDelayedUpdater = new ModifiedFileUpdaterThread( this );
		if ( watcher == null ) {
			try {
				watcher = FileSystems.getDefault().newWatchService();
				modifiedFileDelayedUpdater.setDaemon( true );
				modifiedFileDelayedUpdater.start();
			} catch ( IOException e ) {
				String message = "Unable to initialize file watcher, changes to file system while running won't be detected";
				LOGGER.log( Level.WARNING, message );
				FXUI.notifyUserError( message );
			}
		}
	}

	public void startLoader( Persister persister ) {
				
		loaderThread = new Thread ( new Runnable() {

			long lastSaveTime = System.currentTimeMillis();
			boolean albumTrackDataChangedSinceLastSave = false;
			
			@Override
			public void run() {
				int purgeCounter = 0;
				while ( true ) {
					if ( !sourceToRemove.isEmpty() ) {
						removeOneSource();
						albumTrackDataChangedSinceLastSave = true;
						
					} else if ( !sourceToAdd.isEmpty() ) {
						loadOneSource();
						albumTrackDataChangedSinceLastSave = true;
						
					} else if ( !sourceToUpdate.isEmpty() ) {
						updateOneSource();
						albumTrackDataChangedSinceLastSave = true;
						
					} else if ( purgeOrphansAndMissing && albumsToRemove.isEmpty() && tracksToRemove.isEmpty() ) {
						boolean missingFiles = purgeMissingFiles();
						boolean orphans = purgeOrphans();
						purgeOrphansAndMissing = false;
						
						if ( missingFiles || orphans ) {
							albumTrackDataChangedSinceLastSave = true;
						}
					
					} else {
						processWatcherEvents(); //Note: this blocks for 250ms, see function
					}

					if ( System.currentTimeMillis() - lastSaveTime > SAVE_ALL_INTERVAL ) {
						if ( albumTrackDataChangedSinceLastSave ) {
							persister.saveAlbumsAndTracks();
							albumTrackDataChangedSinceLastSave = false;
						}
						
						persister.saveSources();
						persister.saveCurrentList();
						persister.saveQueue();
						persister.saveHistory();
						persister.saveLibraryPlaylists();
						persister.saveSettings();
						persister.saveHotkeys();
						
						lastSaveTime = System.currentTimeMillis();
					}
					
					try {
						Thread.sleep( loaderSleepTimeMS );
						purgeCounter++;
						
						if ( purgeCounter > 20 ) {
							//TODO: This is a hack because things aren't setup right. Get all these threads and arraylists coordinating properly. 
							purgeOrphansAndMissing = true;
							purgeCounter = 0;
						}
						
					} catch ( InterruptedException e ) {
						LOGGER.fine ( "Sleep interupted during wait period." );
					}
				}
			}
		});
		
		loaderThread.setDaemon( true );
		loaderThread.start();
	}
	
	public void setLoaderSleepTimeMS ( int timeMS ) {
		this.loaderSleepTimeMS = timeMS;
	}
	
	public void requestUpdateSources ( List<Path> paths ) {
		sourceToUpdate.addAll( paths );
		for ( Path path : paths ) {
			musicSourcePaths.add( path );
		}
	}
	
	public void requestAddSources ( List<Path> paths ) {
		for ( Path path : paths ) {
			if ( path != null ) {
				path = path.toAbsolutePath();
				
				if ( path.toFile().exists() && path.toFile().isDirectory() ) {

					boolean addSelectedPathToList = true;
					for ( Path alreadyAddedPath : musicSourcePaths ) {
						try {
							if ( Files.isSameFile( path, alreadyAddedPath ) ) {
								addSelectedPathToList = false;
							}
						} catch ( IOException e1 ) {} // Do nothing, assume they don't match.
					}
					
					if ( addSelectedPathToList ) {
						sourceToAdd.add ( path );
						musicSourcePaths.add( path );
						if ( fileWalker != null ) {
							fileWalker.interrupt();
						}
					}
				}
			}
		}
	}	
	
	public void requestRemoveSources ( List<Path> paths ) {
		sourceToRemove.addAll ( paths );
		
		if ( fileWalker != null ) {
			fileWalker.interrupt();
		}
		
		for ( Path path : paths ) {
			musicSourcePaths.remove( path );
		}
	}
	
	public void requestUpdate ( Path path ) {
		sourceToUpdate.add( path );
	}
	
	public void requestUpdateSource ( Path path ) {
		requestUpdateSources( Arrays.asList( path ) );
	}

	public void requestAddSource ( Path path ) {
		requestAddSources( Arrays.asList( path ) );
	}
	
	public void requestRemoveSource ( Path path ) {
		requestRemoveSources ( Arrays.asList( path ) );
	}
	
	public boolean containsAlbum ( Album album ) {
		if ( albumsToRemove.contains ( album ) ) return false;
		else if ( albums.contains( album ) ) return true;
		else if ( albumsToAdd.contains( album ) ) return true;
		else return false;
	}
	
	public void addAlbums ( ArrayList<Album> albums ) {
		for ( Album album : albums ) {
			addAlbum ( album );
		}
	}
	
	void addAlbum ( Album album ) {
		if ( containsAlbum( album ) ) {
			albumsToUpdate.add ( album ); 
		} else {
			albumsToAdd.add ( album );
		}
	
		addTracks( album.getTracks() );
	}
	
	void removeAlbums ( ArrayList<Album> albums ) {
		for ( Album album : albums ) {
			removeAlbum ( album );
		}
	}
	
	void removeAlbum ( Album album ) {
		if ( !albumsToRemove.contains( album ) ) {
			albumsToRemove.add ( album );
			removeTracks ( album.tracks );
		}
	}
	
	
	public boolean containsTrack ( Track track ) {
		if ( tracksToRemove.contains ( track ) ) return false;
		else if ( tracks.contains( track ) ) return true;
		else if ( tracksToAdd.contains( track ) ) return true;
		else return false;
	}
	
	void addTracks ( ArrayList<Track> tracks ) {
		for ( Track track : tracks ) {
			addTrack ( track );
		}
	}
	
	public void addTrack ( Track track ) {
		if ( containsTrack( track ) ) {
			tracksToUpdate.add ( track );
		} else {
			tracksToAdd.add ( track );
		}
	}
	
	void removeTracks ( ArrayList<Track> tracks ) {
		for ( Track track : tracks ) {
			removeTrack ( track );
		}
	}
	
	void removeTrack ( Track track ) {
		if ( !tracksToRemove.contains( track ) ) {
			tracksToRemove.add( track );
		}
	}
	
	public void addPlaylists ( ArrayList<Playlist> playlists ) {
		for ( Playlist playlist : playlists ) {
			addPlaylist ( playlist );
		}
	}
	
	public void addPlaylist ( Playlist playlist ) {
		playlistsToAdd.add( playlist );
	}
	
	public void removePlaylist ( Playlist playlist ) {
		playlistsToRemove.add( playlist );
	}
	
	public void removePlaylists ( List <Playlist> playlists ) {
		for ( Playlist playlist : playlists ) {
			removePlaylist ( playlist );
		}
	}
	
	private void removeOneSource() {

		while ( fileWalker != null ) {
			try {
				Thread.sleep( 20 );
			} catch ( InterruptedException e ) {
				LOGGER.fine ( "Interrupted while waiting for filewalker to terminate." );
			}
		}
			
		Path removeMeSource = sourceToRemove.remove( 0 );

		sourceToUpdate.remove( removeMeSource );
		sourceToAdd.remove( removeMeSource );
		musicSourcePaths.remove( removeMeSource );
		
		if ( musicSourcePaths.size() == 0 ) {

			Platform.runLater( () -> {
				synchronized ( albumsToAdd ) {
					albumsToAdd.clear();
				}
				
				synchronized ( albumsToRemove ) {
					albumsToRemove.clear();
				}
				
				synchronized ( albumsToUpdate ) {
					albumsToRemove.clear();
				}
				
				synchronized ( albums ) {
					albums.clear();
				}
				
				synchronized ( tracksToAdd ) {
					tracksToAdd.clear();
				}
				
				synchronized ( tracksToRemove ) {
					tracksToRemove.clear();
				}
				
				synchronized ( tracksToUpdate ) {
					tracksToUpdate.clear();
				}
				
				synchronized ( tracks ) {
					tracks.clear();
				}
				
				synchronized ( tagErrors ) {
					tagErrors.clear();
				}
			});
			
		} else {
		
			ArrayList <Album> albumsCopy = new ArrayList <Album> ( albums );
			for ( Album album : albumsCopy ) {
				if ( album.getPath().toAbsolutePath().startsWith( removeMeSource ) ) {
					removeAlbum ( album );
				}
			}
			
			ArrayList <Track> tracksCopy = new ArrayList <Track> ( tracks );
			for ( Track track : tracksCopy ) {
				if ( track.getPath().toAbsolutePath().startsWith( removeMeSource ) ) {
					removeTrack( track );
				}
			}
		}
	}
	
	private void loadOneSource() {
		Path selectedPath = sourceToAdd.get( 0 );
		fileWalker = new MusicFileVisitor( this );
		try {

			Files.walkFileTree ( 
				selectedPath, 
				EnumSet.of( FileVisitOption.FOLLOW_LINKS ), 
				Integer.MAX_VALUE,
				fileWalker
			);
			
			if ( !fileWalker.getWalkInterrupted() ) {
				sourceToAdd.remove( selectedPath );
				watcherRegisterAll ( selectedPath );
			}
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load some files in path: " + selectedPath.toString(), e );
		}
		fileWalker = null;
	}
	
	private void updateOneSource() {
		Path selectedPath = sourceToUpdate.get( 0 );
		fileWalker = new MusicFileVisitor( this );
		try {
			Files.walkFileTree ( 
				selectedPath, 
				EnumSet.of( FileVisitOption.FOLLOW_LINKS ), 
				Integer.MAX_VALUE,
				fileWalker
			);
			
			if ( !fileWalker.getWalkInterrupted() ) {
				sourceToUpdate.remove( selectedPath );
			}
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load some files in path: " + selectedPath.toString(), e );
		}
		
		fileWalker = null;
		
		watcherRegisterAll ( selectedPath );
	}
	
	private boolean purgeMissingFiles() {
		boolean changed = false;
		ArrayList <Album> albumsCopy = new ArrayList <Album> ( albums );
		for ( Album album : albumsCopy ) {
			if ( !Files.exists( album.getPath() ) || !Files.isDirectory( album.getPath() ) ) {
				removeAlbum ( album );
				changed = true;
			}
		}
		
		ArrayList <Track> tracksCopy = new ArrayList <Track> ( tracks );
		for ( Track track : tracksCopy ) {
			if ( !Files.exists( track.getPath() ) || !Files.isRegularFile( track.getPath() ) ) {
				removeTrack ( track );
				changed = true;
			}
		}
		return changed;
	}
	
	private boolean purgeOrphans () {
		boolean changed = false;
		ArrayList <Album> albumsCopy = new ArrayList <Album> ( albums );
		for ( Album album : albumsCopy ) {
			boolean hasParent = false;
			for ( Path sourcePath : musicSourcePaths ) {
				if ( album.getPath().toAbsolutePath().startsWith( sourcePath ) ) {
					hasParent = true;
				}
			}
			
			if ( !hasParent ) {
				removeAlbum ( album );
				ArrayList <Track> albumTracks = album.getTracks();
				if ( albumTracks != null ) {
					tracksToRemove.addAll( albumTracks );
					changed = true;
				}
			}
		}
		
		ArrayList <Track> tracksCopy = new ArrayList <Track> ( tracks );
		for ( Track track : tracksCopy ) {
			boolean hasParent = false;
			for ( Path sourcePath : musicSourcePaths ) {
				if ( track.getPath().toAbsolutePath().startsWith( sourcePath ) ) {
					hasParent = true;
				}
			}
			
			if ( !hasParent ) {
				tracksToRemove.add( track );
				changed = true;
			}
		}
		return changed;
	}
	
	private void watcherRegisterAll ( final Path start ) {
		try {
			Files.walkFileTree( 
				start, 
				EnumSet.of( FileVisitOption.FOLLOW_LINKS ), 
				Integer.MAX_VALUE,
				new SimpleFileVisitor <Path>() {
					@Override
					public FileVisitResult preVisitDirectory ( Path dir, BasicFileAttributes attrs ) throws IOException {
						WatchKey key = dir.register( 
							watcher, 
							StandardWatchEventKinds.ENTRY_CREATE, 
							StandardWatchEventKinds.ENTRY_DELETE, 
							StandardWatchEventKinds.ENTRY_MODIFY 
						);
						
						keys.put( key, dir );
						return FileVisitResult.CONTINUE;
					}
				}
			);
		
		} catch ( IOException e ) {
			LOGGER.log( Level.INFO, "Unable to watch directory for changes: " + start.toString() + "\n" + e.getMessage() );
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean processWatcherEvents () {
		WatchKey key;
		try {
			key = watcher.poll( 250, TimeUnit.MILLISECONDS );
		} catch ( InterruptedException e ) {
			return false;
		}
		
		Path directory = keys.get( key );
		if ( directory == null ) {
			return false;
		}

		for ( WatchEvent <?> event : key.pollEvents() ) {
			WatchEvent.Kind eventKind = event.kind();

			WatchEvent <Path> watchEvent = (WatchEvent<Path>)event;
			Path child = directory.resolve( watchEvent.context() );

			if ( eventKind == StandardWatchEventKinds.ENTRY_CREATE ) {
				if ( Files.isDirectory( child ) ) {
					sourceToAdd.add( child );  
				} else {
					sourceToAdd.add( child.getParent() );
				}
				
			} else if ( eventKind == StandardWatchEventKinds.ENTRY_DELETE ) {
				purgeOrphans();
				purgeMissingFiles();
				
			} else if ( eventKind == StandardWatchEventKinds.ENTRY_MODIFY ) {
				if ( Files.isDirectory( child ) ) {
					modifiedFileDelayedUpdater.addUpdateItem( child );
				} else {
					modifiedFileDelayedUpdater.addUpdateItem( child );
				}
			
			} else if ( eventKind == StandardWatchEventKinds.OVERFLOW ) {
				for ( Path path : musicSourcePaths ) {
					sourceToUpdate.add( path );
				}
			}

			boolean valid = key.reset();
			if ( !valid ) {
				keys.remove( key );
			}
		}
		
		return true;
	}
	
	public String getUniquePlaylistName() {
		return getUniquePlaylistName ( "New Playlist" );
	}
	
	public String getUniquePlaylistName( String base ) {
		String name = base;
		
		int number = 0;
		
		while ( true ) {
			
			boolean foundMatch = false;
			for ( Playlist playlist : playlists ) {
				if ( playlist.getName().toLowerCase().equals( name.toLowerCase() ) ) {
					foundMatch = true;
				}
			}
			
			if ( foundMatch ) {
				number++;
				name = base + " " + number;
			} else {
				break;
			}
		}
		
		return name;
	}

	public SortedList <Playlist> getPlaylistSorted () {
		return playlistsSorted;
	}

	public ObservableList <Playlist> getPlaylists () {
		return playlists;
	}

	public ObservableList <Path> getMusicSourcePaths () {
		return musicSourcePaths;
	}

	public FilteredList <Playlist> getPlaylistsFiltered () {
		return playlistsFiltered;
	}

	public FilteredList <Track> getTracksFiltered () {
		return tracksFiltered;
	}
	
	public FilteredList <Album> getAlbumsFiltered() {
		return albumsFiltered;
	}
	
	public SortedList <Album> getAlbumsSorted() {
		return albumsSorted;
	}
	
	public ObservableList <Album> getAlbums () {
		return albums;
	}
	
	public ObservableList <Track> getTracks () {
		return tracks;
	}

	public SortedList <Track> getTracksSorted () {
		return tracksSorted;
	}
	
	public SortedList <TagError> getTagErrorsSorted () {
		return tagErrorsSorted;
	}
}

class ModifiedFileUpdaterThread extends Thread {
	private final Logger LOGGER = Logger.getLogger( ModifiedFileUpdaterThread.class.getName() );
	public final int DELAY_LENGTH_MS = 1000; 
	public int counter = DELAY_LENGTH_MS;
	
	Vector <Path> updateItems = new Vector <Path> ();
	Library library;
	
	public ModifiedFileUpdaterThread ( Library library ) {
		this.library = library;
	}
	
	public void run() {
		while ( true ) {
			long startSleepTime = System.currentTimeMillis();
			try { 
				Thread.sleep ( 50 ); 
			} catch ( InterruptedException e ) {
				LOGGER.log ( Level.FINE, "Sleep interupted during wait period." );
			}

			long sleepTime = System.currentTimeMillis() - startSleepTime;
			
			if ( counter > 0 ) {
				counter -= sleepTime; 
			} else {
				Vector <Path> copyUpdateItems = new Vector<Path> ( updateItems );
				for ( Path path : copyUpdateItems ) {
					library.requestUpdate ( path );
					updateItems.remove( path );
				}
			}
		}
	}
	
	public void addUpdateItem ( Path path ) {
		counter = DELAY_LENGTH_MS;
		if ( !updateItems.contains( path ) ) {
			updateItems.add ( path );
		}
	}
};
