package net.joshuad.musicplayer;

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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

public class Library {
	
	public static final boolean SHOW_SCAN_NOTES = false; //TODO: use logging, put this at the appropriate level
	
	private static WatchService watcher;
    private static final HashMap<WatchKey,Path> keys = new HashMap <WatchKey,Path> ();
    
    private static MusicFileVisitor fileWalker = null; //YOU MUST SET THIS TO NULL AFTER IT WALKS
    
	private static Thread loaderThread;
	private final static ModifiedFileUpdaterThread modifiedFileDelayedUpdater = new ModifiedFileUpdaterThread();
	
	// These are all three representations of the same data. Add stuff to the
	// Observable List, the other two can't accept add.
	final static ObservableList <Album> albums = FXCollections.observableArrayList( new ArrayList <Album>() );
	final static FilteredList <Album> albumsFiltered = new FilteredList <Album>( albums, p -> true );
	final static SortedList <Album> albumsSorted = new SortedList <Album>( albumsFiltered );

	final static ObservableList <Track> tracks = FXCollections.observableArrayList( new ArrayList <Track>() );
	final static FilteredList <Track> tracksFiltered = new FilteredList <Track>( tracks, p -> true );
	final static SortedList <Track> tracksSorted = new SortedList <Track>( tracksFiltered );

	final static ObservableList <Playlist> playlists = FXCollections.observableArrayList( new ArrayList <Playlist>() );
	final static FilteredList <Playlist> playlistsFiltered = new FilteredList <Playlist>( playlists, p -> true );
	final static SortedList <Playlist> playlistsSorted = new SortedList <Playlist>( playlistsFiltered );

	final static ObservableList <Path> musicSourcePaths = FXCollections.observableArrayList();
	
	static Vector <Album> albumsToAdd = new Vector <Album>();
	static Vector <Album> albumsToRemove = new Vector <Album>();
	static Vector <Album> albumsToUpdate = new Vector <Album>();

	static Vector <Track> tracksToAdd = new Vector <Track>();
	static Vector <Track> tracksToRemove = new Vector <Track>();
	static Vector <Track> tracksToUpdate = new Vector <Track>();

	static Vector <Playlist> playlistsToAdd = new Vector <Playlist>();
	static Vector <Playlist> playlistsToRemove = new Vector <Playlist>();
	static Vector <Playlist> playlistsToUpdate = new Vector <Playlist>();

	private static Vector <Path> sourceToAdd = new Vector <Path>();
	private static Vector <Path> sourceToRemove = new Vector <Path>();
	private static Vector <Path> sourceToUpdate = new Vector <Path>();
	
	private static boolean purgeOrphansAndMissing = true;
	
	public static void init() {
		
		if ( watcher == null ) {
			try {
				watcher = FileSystems.getDefault().newWatchService();
				modifiedFileDelayedUpdater.setDaemon( true );
				modifiedFileDelayedUpdater.start();
			} catch ( IOException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void startLoader() {
		
		
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
						purgeMissingFiles();
						purgeOrphans();
						purgeOrphansAndMissing = false;
					
					} else {
						processWatcherEvents();
					}

					if ( System.currentTimeMillis() - lastSaveTime > 10000 ) {
						if ( albumTrackDataChangedSinceLastSave ) {
							Persister.saveAlbumsAndTracks();
							albumTrackDataChangedSinceLastSave = false;
							System.out.println ( "Saving Album data" ); //TODO: DD
						}
						
						Persister.saveSources();
						Persister.saveCurrentList();
						Persister.saveQueue();
						Persister.saveHistory();
						Persister.savePlaylists();
						Persister.saveSettings();
						
						System.out.println ( "Saving settings" ); //TODO: DD
						lastSaveTime = System.currentTimeMillis();
					}
										
					try {
						Thread.sleep( 50 );
						purgeCounter++;
						
						if ( purgeCounter > 20 ) {
							//TODO: This is a hack because things aren't setup right. Get all these threads and arraylists coordinating properly. 
							purgeOrphansAndMissing = true;
							purgeCounter = 0;
						}
						
					} catch ( InterruptedException e ) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				
				}
			}
		});
		
		loaderThread.setDaemon( true );
		loaderThread.start();
	}
	
	public static void requestUpdateSources ( List<Path> paths ) {
		sourceToUpdate.addAll( paths );
		for ( Path path : paths ) {
			musicSourcePaths.add( path );
		}
	}
	
	public static void requestAddSources ( List<Path> paths ) {
		for ( Path path : paths ) {
			if ( path != null ) {
				path = path.toAbsolutePath();
				
				if ( path.toFile().exists() && path.toFile().isDirectory() ) {

					boolean addSelectedPathToList = true;
					for ( Path alreadyAddedPath : Library.musicSourcePaths ) {
						try {
							if ( Files.isSameFile( path, alreadyAddedPath ) ) {
								addSelectedPathToList = false;
							}
						} catch ( IOException e1 ) {} // Do nothing, assume they don't match.
					}
					
					if ( addSelectedPathToList ) {
						sourceToAdd.add ( path );
						Library.musicSourcePaths.add( path );
						if ( fileWalker != null ) {
							fileWalker.interrupt();
						}
					}
				}
			}
		}
	}	
	
	public static void requestRemoveSources ( List<Path> paths ) {
		sourceToRemove.addAll ( paths );
		
		if ( fileWalker != null ) {
			fileWalker.interrupt();
		}
		
		for ( Path path : paths ) {
			musicSourcePaths.remove( path );
		}
	}
	
	public static void requestUpdate ( Path path ) {
		sourceToUpdate.add( path );
	}
	
	public static void requestUpdateSource ( Path path ) {
		requestUpdateSources( Arrays.asList( path ) );
	}

	public static void requestAddSource ( Path path ) {
		requestAddSources( Arrays.asList( path ) );
	}
	
	public static void requestRemoveSource ( Path path ) {
		requestRemoveSources ( Arrays.asList( path ) );
	}
	
	public static boolean containsAlbum ( Album album ) {
		if ( albumsToRemove.contains ( album ) ) return false;
		else if ( albums.contains( album ) ) return true;
		else if ( albumsToAdd.contains( album ) ) return true;
		else return false;
	}
	
	public static void addAlbums ( ArrayList<Album> albums ) {
		for ( Album album : albums ) {
			addAlbum ( album );
		}
	}
	
	static void addAlbum ( Album album ) {
		if ( containsAlbum( album ) ) {
			albumsToUpdate.add ( album ); 
		} else {
			albumsToAdd.add ( album );
		}
	
		addTracks( album.getTracks() );
	}
	
	static void removeAlbums ( ArrayList<Album> albums ) {
		for ( Album album : albums ) {
			removeAlbum ( album );
		}
	}
	
	static void removeAlbum ( Album album ) {
		if ( !albumsToRemove.contains( album ) ) {
			albumsToRemove.add ( album );
			removeTracks ( album.tracks );
		}
	}
	
	
	public static boolean containsTrack ( Track track ) {
		if ( tracksToRemove.contains ( track ) ) return false;
		else if ( tracks.contains( track ) ) return true;
		else if ( tracksToAdd.contains( track ) ) return true;
		else return false;
	}
	
	static void addTracks ( ArrayList<Track> tracks ) {
		for ( Track track : tracks ) {
			addTrack ( track );
		}
	}
	
	static void addTrack ( Track track ) {
		if ( containsTrack( track ) ) {
			tracksToUpdate.add ( track );
		} else {
			tracksToAdd.add ( track );
		}
	}
	
	static void removeTracks ( ArrayList<Track> tracks ) {
		for ( Track track : tracks ) {
			removeTrack ( track );
		}
	}
	
	static void removeTrack ( Track track ) {
		if ( !tracksToRemove.contains( track ) ) {
			tracksToRemove.add( track );
		}
	}
	
	public static void addPlaylists ( ArrayList<Playlist> playlists ) {
		for ( Playlist playlist : playlists ) {
			addPlaylist ( playlist );
		}
	}
	
	public static void addPlaylist ( Playlist playlist ) {
		//TODO: name checking? 
		playlistsToAdd.add( playlist );
	}
	
	public static void removePlaylist ( Playlist playlist ) {
		playlistsToRemove.add( playlist );
	}
	
	private static void removeOneSource() {

		while ( fileWalker != null ) {
			try {
				Thread.sleep( 20 );
			} catch ( InterruptedException e ) {}
		}
			
		Path removeMeSource = sourceToRemove.remove( 0 );

		sourceToUpdate.remove( removeMeSource );
		sourceToAdd.remove( removeMeSource );
		musicSourcePaths.remove( removeMeSource );
		
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
	
	private static void loadOneSource() {
		Path selectedPath = sourceToAdd.get( 0 );
		fileWalker = new MusicFileVisitor( );
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
			
		} catch ( IOException e ) {
			System.out.println ("Unable to load some files in path: " + selectedPath.toString() );
			e.printStackTrace();
		}
		fileWalker = null;
	}
	
	private static void updateOneSource() {
		Path selectedPath = sourceToUpdate.get( 0 );
		fileWalker = new MusicFileVisitor( );
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
			
			
		} catch ( IOException e ) {
			System.out.println ("Unable to load some files in path: " + selectedPath.toString() );
			e.printStackTrace();
		}
		
		fileWalker = null;
		
		watcherRegisterAll ( selectedPath );
	}
	
	private static void purgeMissingFiles() {
		ArrayList <Album> albumsCopy = new ArrayList <Album> ( albums );
		for ( Album album : albumsCopy ) {
			if ( !Files.exists( album.getPath() ) || !Files.isDirectory( album.getPath() ) ) {
				removeAlbum ( album );
			}
		}
		
		ArrayList <Track> tracksCopy = new ArrayList <Track> ( tracks );
		for ( Track track : tracksCopy ) {
			if ( !Files.exists( track.getPath() ) || !Files.isRegularFile( track.getPath() ) ) {
				removeTrack ( track);
			}
		}
	}
	
	private static void purgeOrphans () {
		ArrayList <Album> albumsCopy = new ArrayList <Album> ( albums );
		for ( Album album : albumsCopy ) {
			boolean hasParent = false;
			for ( Path sourcePath : Library.musicSourcePaths ) {
				if ( album.getPath().toAbsolutePath().startsWith( sourcePath ) ) {
					hasParent = true;
				}
			}
			
			if ( !hasParent ) {
				removeAlbum ( album );
				ArrayList <Track> albumTracks = album.getTracks();
				if ( albumTracks != null ) {
					tracksToRemove.addAll( albumTracks );
				}
			}
		}
		
		ArrayList <Track> tracksCopy = new ArrayList <Track> ( tracks );
		for ( Track track : tracksCopy ) {
			boolean hasParent = false;
			for ( Path sourcePath : Library.musicSourcePaths ) {
				if ( track.getPath().toAbsolutePath().startsWith( sourcePath ) ) {
					hasParent = true;
				}
			}
			
			if ( !hasParent ) {
				tracksToRemove.add( track );
			}
		}
	}
	
	private static void watcherRegisterAll ( final Path start ) {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static boolean processWatcherEvents () {
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
					sourceToAdd.add( child ); //TODO: is this the right function call? 
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
}

class ModifiedFileUpdaterThread extends Thread {
	public static final int DELAY_LENGTH_MS = 1000; 
	public int counter = DELAY_LENGTH_MS;
	
	Vector <Path> updateItems = new Vector <Path> ();
	
	public void run() {
		while ( true ) {
			long startSleepTime = System.currentTimeMillis();
			try { 
				Thread.sleep ( 50 ); 
			} catch ( InterruptedException e ) {} //TODO: Is this OK to do? Feels dangerous.

			long sleepTime = System.currentTimeMillis() - startSleepTime;
			
			if ( counter > 0 ) {
				counter -= sleepTime; 
			} else {
				Vector <Path> copyUpdateItems = new Vector<Path> ( updateItems );
				for ( Path path : copyUpdateItems ) {
					Library.requestUpdate ( path );
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
