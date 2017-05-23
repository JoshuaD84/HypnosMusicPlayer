package org.joshuad.musicplayer;

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
	
	public static final boolean SHOW_SCAN_NOTES = false;
	
	private static Vector <Path> loadMe = new Vector <Path> ();
	private static Vector <Path> updateMe = new Vector <Path> ();
	private static Vector <Path> removeMe = new Vector <Path> ();
	
	private static Thread loaderThread;
	private static WatchService watcher;
    private static final HashMap<WatchKey,Path> keys = new HashMap <WatchKey,Path> ();
    
    private static MusicFileVisitor fileWalker = null;

	final static ObservableList <Path> musicSourcePaths = FXCollections.observableArrayList();
	
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
	
	final static UpdaterThread modifiedFileDelayedUpdater = new UpdaterThread();
	
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
			boolean purged = false;
			public void run() {
				while ( true ) {
					
					if ( !removeMe.isEmpty() ) {
						removeOneSource();
						
					} else if ( !loadMe.isEmpty() ) {
						loadOneSource();
						
					} else if ( !purged ) {
						purgeOrphans();
						purgeMissingFiles();
						purged = true;
						
					} else if ( !updateMe.isEmpty() ) {
						updateOneSource();
					
					} else {
						processWatcherEvents();
						
					}
					try {
						Thread.sleep( 50 );
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
		updateMe.addAll( paths );
		for ( Path path : paths ) {
			Library.musicSourcePaths.add( path );
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
						loadMe.add ( path );
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
		removeMe.addAll ( paths );
		for ( Path path : paths ) {
			Library.musicSourcePaths.remove( path );
			updateMe.remove( path );
			loadMe.remove( path );
		}
		if ( fileWalker != null ) {
			fileWalker.interrupt();
		}
	}
	
	public static void requestUpdate ( Path path ) {
		updateMe.add( path );
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
		return albums.contains( album );
	}
	
	public static void addAlbums ( ArrayList<Album> albums ) {
		for ( Album album : albums ) {
			addAlbum ( album );
		}
	}
	
	static void addAlbum ( Album album ) {
		int currentIndex = albums.indexOf( album );
		
		if ( currentIndex != -1 ) {
			albums.set( currentIndex, album );
		} else {
			albums.add ( album );
		}
		addTracks( album.getTracks() );
	}
	
	static void removeAlbums ( ArrayList<Album> albums ) {
		for ( Album album : albums ) {
			removeAlbum ( album );
		}
	}
	
	static void removeAlbum ( Album album ) {
		albums.remove( album );
		removeTracks ( album.tracks );
	}
	
	public static boolean containsTrack ( Track track ) {
		return tracks.contains( track );
	}
	
	static void addTracks ( ArrayList<Track> tracks ) {
		for ( Track track : tracks ) {
			addTrack ( track );
		}
	}
	
	static void addTrack ( Track track ) {
		int currentIndex = tracks.indexOf( track );
		
		if ( currentIndex != -1 ) {
			tracks.set( currentIndex, track );
		} else {
			tracks.add ( track );
		}
	}
	
	static void removeTracks ( ArrayList<Track> tracks ) {
		for ( Track track : tracks ) {
			removeTrack ( track );
		}
	}
	
	static void removeTrack ( Track track ) {
		tracks.remove( track );
	}
	
	public static void addPlaylists ( ArrayList<Playlist> playlists ) {
		for ( Playlist playlist : playlists ) {
			removePlaylist ( playlist );
		}
	}
	
	public static void addPlaylist ( Playlist playlist ) {
		playlists.add( playlist );
	}
	
	public static void removePlaylist ( Playlist playlist ) {
		playlists.remove( playlist );
	}
	
	private static void removeOneSource() {
		Path sourcePath = removeMe.remove( 0 ).toAbsolutePath();
		
		if ( Files.isDirectory( sourcePath ) ) {
			ArrayList <Album> albumsCopy = new ArrayList <Album> ( albums );
			for ( Album album : albumsCopy ) {
				if ( album.getPath().toAbsolutePath().startsWith( sourcePath ) ) {
					removeAlbum ( album );
					ArrayList <Track> tracks = album.getTracks();
					if ( tracks != null ) {
						tracks.removeAll( tracks );
					}
				}
			}
			
			ArrayList <Track> tracksCopy = new ArrayList <Track> ( tracks );
			for ( Track track : tracksCopy ) {
				if ( track.getPath().toAbsolutePath().startsWith( sourcePath ) ) {
					removeTrack ( track );
				}
			}
		}
	}
	
	private static void loadOneSource() {
		Path selectedPath = loadMe.get( 0 );
		fileWalker = new MusicFileVisitor( );
		try {

			Files.walkFileTree ( 
				selectedPath, 
				EnumSet.of( FileVisitOption.FOLLOW_LINKS ), 
				Integer.MAX_VALUE,
				fileWalker
			);
			
			if ( !fileWalker.getWalkInterrupted() ) {
				loadMe.remove( selectedPath );
				watcherRegisterAll ( selectedPath );
			}
			
		} catch ( IOException e ) {
			System.out.println ("Unable to load some files in path: " + selectedPath.toString() );
			e.printStackTrace();
		}
	}
	
	private static void updateOneSource() {
		Path selectedPath = updateMe.get( 0 );
		fileWalker = new MusicFileVisitor( );
		try {
			Files.walkFileTree ( 
				selectedPath, 
				EnumSet.of( FileVisitOption.FOLLOW_LINKS ), 
				Integer.MAX_VALUE,
				fileWalker
			);
			
			if ( !fileWalker.getWalkInterrupted() ) {
				updateMe.remove( selectedPath );
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
				Library.albums.remove ( album );
				ArrayList <Track> tracks = album.getTracks();
				if ( tracks != null ) {
					tracks.removeAll( tracks );
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
				tracks.remove ( track );
			}
		}
	}
	
	private static void watcherRegisterAll ( final Path start ) {
		try {
			Files.walkFileTree( start, new SimpleFileVisitor <Path>() {
				@Override
				public FileVisitResult preVisitDirectory ( Path dir, BasicFileAttributes attrs ) throws IOException {
					WatchKey key = dir.register( watcher, 
							StandardWatchEventKinds.ENTRY_CREATE, 
							StandardWatchEventKinds.ENTRY_DELETE, 
							StandardWatchEventKinds.ENTRY_MODIFY );
					
					keys.put( key, dir );
					return FileVisitResult.CONTINUE;
				}
			});
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static boolean processWatcherEvents () {
		WatchKey key;
		try {
			key = watcher.poll( 100, TimeUnit.MILLISECONDS );
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
					loadMe.add( child );
				} else {
					loadMe.add( child.getParent() );
				}
				
			} else if ( eventKind == StandardWatchEventKinds.ENTRY_DELETE ) {
				//Handled by removeMissingFiles(), can ignore. 
								
			} else if ( eventKind == StandardWatchEventKinds.ENTRY_MODIFY ) {
				if ( Files.isDirectory( child ) ) {
					modifiedFileDelayedUpdater.addUpdateItem( child );
				} else {
					modifiedFileDelayedUpdater.addUpdateItem( child );
				}
			
			} else if ( eventKind == StandardWatchEventKinds.OVERFLOW ) {
				for ( Path path : musicSourcePaths ) {
					updateMe.add( path );
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

class UpdaterThread extends Thread {
	public static final int DELAY_LENGTH_MS = 500; 
	public int counter = DELAY_LENGTH_MS;
	
	Vector <Path> updateItems = new Vector <Path> ();
	
	public void run() {
		while ( true ) {
			long startSleepTime = System.currentTimeMillis();
			try { 
				Thread.sleep ( 20 ); 
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
