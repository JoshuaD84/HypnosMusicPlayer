package net.joshuad.hypnos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.AudioSystem.RepeatMode;
import net.joshuad.hypnos.audio.AudioSystem.ShuffleMode;
import net.joshuad.hypnos.fxui.ThrottledTrackFilter;

public class CurrentList {

	private static final Logger LOGGER = Logger.getLogger( CurrentList.class.getName() );
	
	public enum Mode {
		ALBUM,
		ALBUM_REORDERED,
		PLAYLIST,
		PLAYLIST_UNSAVED,
		EMPTY;
	}
	
	public enum DefaultShuffleMode {
		SEQUENTIAL, SHUFFLE, NO_CHANGE
	}
	
	public enum DefaultRepeatMode {
		PLAY_ONCE, REPEAT, NO_CHANGE
	}
		
	Mode mode = Mode.EMPTY;
	final List <Album> currentAlbums = new ArrayList <Album> ();
	Playlist currentPlaylist;
	
	private final ObservableList <CurrentListTrack> items = FXCollections.observableArrayList(); 
	private final FilteredList <CurrentListTrack> currentListFiltered = new FilteredList <CurrentListTrack>( items, p -> true );
	private final SortedList <CurrentListTrack> currentListSorted = new SortedList <CurrentListTrack>( currentListFiltered );
	private SortedList <CurrentListTrack> currentListSortedNoFilter;
	
	private final List <CurrentListListener> listeners = new ArrayList<CurrentListListener> ();

	ThrottledTrackFilter currentListTableFilter;
	
	private AudioSystem audioSystem;
	private Queue queue;

	private DefaultShuffleMode albumShuffleMode = DefaultShuffleMode.SEQUENTIAL;
	private DefaultShuffleMode trackShuffleMode = DefaultShuffleMode.NO_CHANGE;
	private DefaultShuffleMode playlistShuffleMode = DefaultShuffleMode.SHUFFLE;

	private DefaultRepeatMode albumRepeatMode = DefaultRepeatMode.PLAY_ONCE;
	private DefaultRepeatMode trackRepeatMode = DefaultRepeatMode.NO_CHANGE;
	private DefaultRepeatMode playlistRepeatMode = DefaultRepeatMode.REPEAT;
	
	List <Thread> noLoadThreads = new ArrayList <Thread> ();

	transient private boolean hasUnsavedData = true;
	
	private boolean allowAlbumReload = true; //Allow the album to be reloaded if the disk is changed
		//usually enabled, but disabled for the current list after restarting hypnos
	
	public CurrentList ( AudioSystem audioSystem, Queue queue ) {
		this.queue = queue;
		this.audioSystem = audioSystem;

		currentListTableFilter = new ThrottledTrackFilter ( currentListFiltered );
		
		startListWatcher();
		
		items.addListener( (ListChangeListener.Change<? extends Track> change) -> {
			hasUnsavedData = true;			
		});
	}
	
	public boolean hasUnsavedData() {
		return hasUnsavedData;
	}
	
	public void setHasUnsavedData( boolean b ) {
		hasUnsavedData = b;
	}
	
	public boolean allowAlbumReload () {
		return allowAlbumReload;
	}
	
	public void addNoLoadThread ( Thread t ) {
		noLoadThreads.add ( t );
	}
	
	private boolean onBadThread() {
		for ( Thread t : noLoadThreads ) {
			if ( t == Thread.currentThread() ) {
				return true;
			}
		}
		return false;
	}
	
	public void doThreadAware( Runnable runMe ) {
		if ( onBadThread() ) {
			Thread loaderThread = new Thread ( runMe );
			loaderThread.setName( "Short Off-FX Thread" );
			loaderThread.setDaemon( true );
			loaderThread.start();
			
		} else {
			runMe.run();
			
		}
	}
	
	private void startListWatcher() {
		Thread watcher = new Thread( () -> {
			while ( true ) {
				try {
					for ( CurrentListTrack track : items ) {
						boolean isMissing = !Utils.isMusicFile( track.getPath() );
						
						if ( !isMissing && track.isMissingFile() ) {
							track.setIsMissingFile ( false );
							
						} else if ( isMissing && !track.isMissingFile() ) {
							track.setIsMissingFile ( true );
						}
						
						if ( track.needsUpdateFromDisk() ) {
							try {
								track.update();
								//Thread.sleep( 10 );
							} catch ( Exception e ) {
								//No need to log anything, UI should show it
							}
						}
					}

				} catch ( ConcurrentModificationException e ) {
					//If the list is edited, stop what we're doing and start up again later. 
				}
				
				try {
					Thread.sleep ( 250 );
				} catch ( InterruptedException e ) {
					LOGGER.fine ( "Interrupted while sleeping in current list watcher." );
				}
			}
		});
		
		watcher.setName( "Current List Watcher" );
		watcher.setDaemon( true );
		watcher.start();
	}
	
	public void addListener ( CurrentListListener listener ) {
		if ( listener == null ) {
			LOGGER.fine( "Null player listener was attempted to be added, ignoring." );
			
		} else if ( listeners.contains( listener ) ) {
			LOGGER.fine( "Null player listener was attempted to be added, ignoring." );
			
		} else {			
			listeners.add( listener );
		}
	}
	
	public ObservableList<CurrentListTrack> getItems() {
		return items;
	}
	
	
	public void setMode ( Mode mode ) {
		this.mode = mode;
		notifyListenersStateChanged();
	}
		
	public CurrentListState getState () {
		return new CurrentListState ( new ArrayList<CurrentListTrack>( items ), currentAlbums, currentPlaylist, mode );
	}
	
	public void setDefaultAlbumShuffleMode ( DefaultShuffleMode mode ) {
		this.albumShuffleMode = mode;
	}
	
	public void setDefaultTrackShuffleMode ( DefaultShuffleMode mode ) {
		this.trackShuffleMode = mode;
	}
	
	public void setDefaultPlaylistShuffleMode ( DefaultShuffleMode mode ) {
		this.playlistShuffleMode = mode;
	}
	
	public void setDefaultAlbumRepeatMode ( DefaultRepeatMode mode ) {
		this.albumRepeatMode = mode;
	}
	
	public void setDefaultTrackRepeatMode ( DefaultRepeatMode mode ) {
		this.trackRepeatMode = mode;
	}
	
	public void setDefaultPlaylistRepeatMode ( DefaultRepeatMode mode ) {
		this.playlistRepeatMode = mode;
	}
	
	public DefaultShuffleMode getDefaultTrackShuffleMode () {
		return trackShuffleMode;
	}
	
	public DefaultShuffleMode getDefaultAlbumShuffleMode () {
		return albumShuffleMode;
	}
	
	public DefaultShuffleMode getDefaultPlaylistShuffleMode () {
		return playlistShuffleMode;
	}
	
	public DefaultRepeatMode getDefaultTrackRepeatMode () {
		return trackRepeatMode;
	}
	
	public DefaultRepeatMode getDefaultAlbumRepeatMode () {
		return albumRepeatMode;
	}
	
	public DefaultRepeatMode getDefaultPlaylistRepeatMode () {
		return playlistRepeatMode;
	}
	
	public void setState( CurrentListState state ) {
		items.clear();
		items.addAll( state.getItems() );
		
		currentAlbums.clear();
		currentAlbums.addAll( state.getAlbums() );
		
		currentPlaylist = state.getPlaylist();
		
		mode = state.getMode();
		
		notifyListenersStateChanged();

		allowAlbumReload = false;
		
	}
	
	public Playlist getCurrentPlaylist () {
		return currentPlaylist;
	}
	
	public void shuffleList () {
		Collections.shuffle( items );
		listReordered();
	}
	
	public void shuffleItems ( List<Integer> input ) {
		List<CurrentListTrack> shuffleMe = new ArrayList<CurrentListTrack> ();
		List<Integer> itemIndices = new ArrayList<Integer> ( input );
		
		for ( int index : itemIndices ) {
			shuffleMe.add( items.get( index ) );
		}
		
		Collections.shuffle( shuffleMe );
		
		for ( int index : itemIndices ) {
			items.set( index, shuffleMe.remove( 0 ) );
		}

		listReordered();
	}
		
	public void clearList() {
		if ( items.size() > 0 ) {
			listCleared();
		}
		items.clear();
	}
	
	public void removeTracksAtIndices ( List <Integer> indices ) {

		Runnable runMe = new Runnable() {
			public void run() {
				
				List <Integer> indicesToBeRemoved = new ArrayList <>();
				
				for ( int index : indices ) { 
					indicesToBeRemoved.add( currentListFiltered.getSourceIndex( 
						currentListSorted.getSourceIndex( index ) ) );
				}
				
				indicesToBeRemoved.sort( Comparator.reverseOrder() );
				
				boolean changed = false;
				for ( Integer index : indicesToBeRemoved ) {
					CurrentListTrack removed = items.remove( index.intValue() );
					
					if ( !changed && removed != null ) changed = true;
				}
								
				if ( changed ) {
					if ( items.size() == 0 ) {
						listCleared();
						currentPlaylist = null;
					} else {
						tracksRemoved();
					}
				}
			}
		};
		
		doThreadAware ( runMe );
	}
	
	public void moveTracks ( List<Integer> fromLocations, int toLocation ) {
		if ( fromLocations == null ) {
			LOGGER.fine( "Recieved a null list, ignoring request." );
			return;
		}
		
		if ( fromLocations.size() == 0 ) {
			LOGGER.fine( "Recieved an empty list, ignoring request." );
			return;
		}
				
		ArrayList <CurrentListTrack> tracksToMove = new ArrayList <CurrentListTrack> ( fromLocations.size() );
		
		for ( int index : fromLocations ) {
			if ( index >= 0 && index < items.size() ) {
				tracksToMove.add( items.get( index ) );
			}
		}
		
		for ( int k = fromLocations.size() - 1; k >= 0; k-- ) {
			int index = fromLocations.get( k ).intValue();
			if ( index >= 0 && index < items.size() ) {
				items.remove ( index );
			}
		}

		if ( tracksToMove.size() > 0 ) {
			if ( toLocation > items.size() ) toLocation = items.size();
			items.addAll( toLocation, tracksToMove );
			listReordered();
		}
	}
	
	public void setTrack ( String location ) {
		setTracksPathList ( Arrays.asList( Paths.get( location ) ) );
	}
	
	public void setTrack ( Path path ) {
		setTracksPathList ( Arrays.asList( path ) );
	}

	public void setTrack ( Track track ) {
		setTracksPathList ( Arrays.asList( track.getPath() ) );
	}

	public void setTracks ( List <? extends Track> tracks ) {
		setTracks ( tracks, true );
	}
		
	public void setTracks ( List <? extends Track> tracks, boolean clearQueue ) {
		clearList();
		if ( clearQueue ) queue.clear();
		this.currentListTableFilter.setFilter( "", false );
		audioSystem.getUI().setCurrentListFilterText( "" );
		appendTracks ( tracks );
	}
		
	public void setTracksPathList ( List <Path> paths ) {
		setTracksPathList ( paths, null );
	}
	
	public void setTracksPathList ( List <Path> paths, Runnable afterLoad ) {
		clearList();
		queue.clear();
		appendTracksPathList ( paths, afterLoad );
	}
	
	public void appendTrack ( String location ) {
		appendTracksPathList ( Arrays.asList( Paths.get( location ) ) );
	}
	
	public void appendTrack ( Path path ) {
		appendTracksPathList ( Arrays.asList( path ) );
	}

	public void appendTrack ( Track track ) {
		insertTracks ( items.size(), Arrays.asList( track ) );
	}
	
	public void appendTracks ( List <? extends Track> tracks ) {
		insertTracks ( items.size(), tracks );
	}
		
	public void appendTracksPathList ( List <Path> paths ) {
		insertTrackPathList ( items.size(), paths, null );
	}
	
	public void appendTracksPathList ( List <Path> paths, Runnable afterLoad ) {
		insertTrackPathList ( items.size(), paths, afterLoad );
	}
		
	
	public void insertTracks ( int index, List<? extends Track> tracks ) {
		if ( tracks == null || tracks.size() <= 0 ) {
			LOGGER.fine( "Recieved a null or empty track list. No tracks loaded." );
			return;
		}
		
		boolean startedEmpty = items.isEmpty();
		
		List <CurrentListTrack> addMe = new ArrayList <CurrentListTrack> ( tracks.size() );
		
		for ( Track track : tracks ) {
			addMe.add( new CurrentListTrack ( track ) );
		}
		
		int targetIndex = index;
		synchronized ( items ) {
			if ( index < 0 ) {
				LOGGER.fine( "Asked to insert tracks at: " + index + ", inserting at 0 instead." );
				targetIndex = 0;
			} else if ( index > items.size() ) {
				LOGGER.fine( "Asked to insert tracks past the end of current list. Inserting at end instead." );
				targetIndex = items.size();
			}
		}
		
		int tracksAdded = 0;
		for ( CurrentListTrack track : addMe ) {
			addItem ( targetIndex, track );
			targetIndex++;
			tracksAdded++;
		}
		
		if ( tracksAdded > 0 ) {
			if ( startedEmpty ) {
				tracksSet();
			} else {
				tracksAdded();
			}
		}
	}
	
	public void insertTrackPathList ( int index, List<Path> paths ) {
		insertTrackPathList ( index, paths, null );
	}
	
	public void insertTrackPathList ( int index, List <Path> paths, Runnable doAfterLoad ) {
		
		Runnable runMe = new Runnable() {
			public void run() {

				boolean startedEmpty = items.isEmpty();
				
				if ( paths == null || paths.size() <= 0 ) {
					LOGGER.fine( "Recieved a null or empty track list. No tracks loaded." );
					return;
				}
				
				int targetIndex = index;
				synchronized ( items ) {
					if ( index < 0 ) {
						LOGGER.info( "Asked to insert tracks at: " + index + ", inserting at 0 instead." );
						targetIndex = 0;
					} else if ( index > items.size() ) {
						LOGGER.info( "Asked to insert tracks past the end of current list. Inserting at end instead." );
						targetIndex = items.size() - 1;
					}
				}
				
				int tracksAdded = 0;
				for ( Path path : paths ) {
					try {
						addItem ( targetIndex, new CurrentListTrack ( path ) );
						targetIndex++;
						tracksAdded++;
					} catch ( Exception e ) {
						LOGGER.info( " -- Couldn't load track: " + path.toString() + ". Skipping." );
					}
				}
				
				if ( tracksAdded > 0 ) {
					if ( startedEmpty ) {
						tracksSet();
					} else {
						tracksAdded();
					}
				}
				//TODO: This is really bad practice, but it works for now. Refactor
				//This fixes two problems -- red rows in current list not being white after deleting and D&D
				//and the table not refreshing after drag & drop of folder
				Hypnos.getUI().refreshCurrentList();
				
				if ( doAfterLoad != null ) {
					doAfterLoad.run();
				}
			
			}
			
		};
		
		doThreadAware ( runMe );
	}
	
	private void addItem ( int index, CurrentListTrack track ) {
		if ( track == null ) return;
		
		synchronized ( items ) {
			int targetIndex = index;
			if ( index < 0 ) {
				LOGGER.info( "Asked to insert tracks at: " + index + ", inserting at 0 instead." );
				targetIndex = 0;
			} else if ( index > items.size() ) {
				LOGGER.info( "Asked to insert tracks past the end of current list. Inserting at end instead." );
				targetIndex = items.size() - 1;
			}
			items.add( targetIndex, track );
		}
	}
	
	public void appendAlbum ( Album album ) {
		appendAlbums ( Arrays.asList( album ) );
	}
	
	public void appendAlbums ( List<Album> albums ) {
		insertAlbums ( items.size(), albums );
	} 
	
	public void insertAlbums ( final int index, List<Album> albums ) {
		Runnable runMe = new Runnable() {
			public void run() {
				int targetIndex = index > items.size() ? items.size() : index;
				
				boolean insertedAtEnd = ( targetIndex == items.size() );
		
				boolean startedEmpty = false;
				Mode startMode = mode;
				
				if ( items.size() == 0 ) startedEmpty = true;
				
				List<Track> addMe = new ArrayList<Track> ();
				for ( Album album : albums ) {
					if ( album != null ) {
						addMe.addAll( album.getTracks() );
					}
				}
				
				insertTracks ( targetIndex, addMe );
				
				if ( startedEmpty ) {
					albumsSet ( albums );
					
				} else if ( startMode == Mode.ALBUM || startMode == Mode.ALBUM_REORDERED ) {
					boolean startedReordered = ( startMode == Mode.ALBUM_REORDERED );
					
					List<Album> fullAlbumList = new ArrayList<Album> ( currentAlbums );
					fullAlbumList.addAll ( albums );
					albumsSet ( fullAlbumList );
					
					if ( startedReordered && mode == Mode.ALBUM ) {
						mode = Mode.ALBUM_REORDERED;
					}
					
					if ( targetIndex != 0 && !insertedAtEnd ) {
						listReordered();
					}
				}
			}
		};
		
		doThreadAware ( runMe );
	}
	
	public void setAlbum ( Album album, boolean clearQueue ) {
		setAlbums ( Arrays.asList( album ), clearQueue );
	}
	
	public void setAlbum ( Album album ) {
		setAlbums ( Arrays.asList( album ) );
	}
	
	public void setAlbums ( List<Album> albums ) {
		setAlbums ( albums, true );
	}

	public void setAlbums ( List<Album> albums, boolean clearQueue ) {
		List <Track> addMe = new ArrayList <Track> ();
		
		int albumsAdded = 0;
		
		List <Album> missing = new ArrayList <Album> ();
		
		for ( Album album : albums ) {
			if ( album == null ) {
				continue;
				
			} else if ( !Files.isDirectory ( album.getPath() ) ) {
				missing.add( album );
				
			} else {
				addMe.addAll ( album.getTracks() );
				albumsAdded++;
			}
		}
		
		setTracks ( addMe, clearQueue );
		albumsSet ( albums );
		
		if ( missing.size() > 0 ) {
			Hypnos.warnUserAlbumsMissing ( missing );
		}
	}
	
	
	public void appendPlaylist ( Playlist playlist ) {
		boolean setAsPlaylist = false;
		if ( items.size() == 0 ) setAsPlaylist = true;
		
		appendTracks ( playlist.getTracks() );
		
		if ( setAsPlaylist ) {
			currentPlaylist = playlist;
			playlistsSet ( Arrays.asList( playlist ) );
		}
	}
	
	public void appendPlaylists ( List<Playlist> playlists ) {
		
		
		boolean startedEmpty = false;
		if ( items.size() == 0 ) startedEmpty = true;
		
		int playlistsAdded = 0;
		Playlist lastPlaylist = null;
		for ( Playlist playlist : playlists ) {
			if ( playlist != null ) {
				appendTracks ( playlist.getTracks() );
				lastPlaylist = playlist;
				playlistsAdded++;
			}
		}
		
		if ( startedEmpty && playlistsAdded == 1 ) {
			currentPlaylist = lastPlaylist;
		}
		
		if ( startedEmpty ) {
			playlistsSet ( playlists );
		}
	}
	
	public void insertPlaylists ( int index, List<Playlist> playlists ) {
		Runnable runMe = new Runnable() {
			public void run() {
				int targetIndex = index > items.size() ? items.size() : index;
				
				boolean startedEmpty = false;
				if ( items.size() == 0 ) startedEmpty = true;
				
				List<Track> addMe = new ArrayList<Track> ();
				for ( Playlist playlist : playlists ) {
					if ( playlist != null ) {
						addMe.addAll( playlist.getTracks() );
					}
				}
		
				insertTracks ( targetIndex, addMe );
				
				if ( startedEmpty ) {
					playlistsSet ( playlists );
				} else {
					tracksAdded();
				}
			}
		};
		doThreadAware ( runMe );
	}
	
	public void setPlaylist ( Playlist playlist ) {
		setPlaylists ( Arrays.asList( playlist ) );
	}
	
	public void setPlaylists ( List<Playlist> playlists ) {
		List <Track> addMe = new ArrayList <Track> ();
		
		int playlistsAdded = 0;
		Playlist playlistAdded = null;
		
		for ( Playlist playlist : playlists ) {
			if ( playlist != null ) {
				addMe.addAll ( playlist.getTracks() );
				playlistsAdded++;
				playlistAdded = playlist;
			}
		}
		
		setTracks ( addMe );
		
		if ( playlistsAdded == 1 ) {
			currentPlaylist = playlistAdded;
		}
		
		playlistsSet ( playlists );
	}
	
	public void notifyListenersStateChanged() {

		CurrentListState state = getState();
		for ( CurrentListListener listener : listeners ) {
			listener.stateChanged ( state );
		}

		allowAlbumReload = true;
	}
	
	public void albumsSet ( List <Album> input ) {
		
		if ( input == null ) {
			mode = Mode.EMPTY;
			currentAlbums.clear();
			currentPlaylist = null;
			LOGGER.log( Level.FINE, "Recieved an null album list." );
			notifyListenersStateChanged();
			return;
		}

		ArrayList<Album> albums = new ArrayList<Album> ( input );
		
		albums.removeIf( Objects::isNull );
		
		if ( albums.size() == 0 ) {
			mode = Mode.EMPTY;
			currentAlbums.clear();
			currentPlaylist = null;
			notifyListenersStateChanged();
			return;
		
		} else if ( albums.size() == 1 ) {
			mode = Mode.ALBUM;
			updateDefaultModes();
			currentAlbums.clear();
			currentAlbums.addAll( albums );
			notifyListenersStateChanged();
			return;

		} else {
			boolean initialized = false;
			String simpleTitle = "", year = "", artist = "";
			boolean differentBaseAlbums = false;
			
			for ( int k = 0; k < albums.size(); k++ ) {
				Album album = albums.get( k );
						
				if ( !initialized ) {
					simpleTitle = album.getAlbumTitle();
					year = album.getYear();
					artist = album.getAlbumArtist();
					initialized = true;
					
				} else {
					if ( !album.getAlbumTitle().equals( simpleTitle ) 
					||   !album.getAlbumArtist().equals( artist )
					||   !album.getYear().equals( year )
					){
						differentBaseAlbums = true;
					}
				}
			}
			
			if ( !initialized ) {
				mode = Mode.EMPTY;
				currentAlbums.clear();
				currentPlaylist = null;
				notifyListenersStateChanged();
				return;
				
			} else if ( differentBaseAlbums ) {
				mode = Mode.PLAYLIST_UNSAVED;
				updateDefaultModes();
				currentAlbums.clear();
				currentPlaylist = null;
				notifyListenersStateChanged();
				return;
				
			} else {
				mode = Mode.ALBUM;
				updateDefaultModes();
				currentAlbums.clear();
				currentAlbums.addAll( albums );
				notifyListenersStateChanged();
				return;
			}
		}
	}
	
	//TODO: these nested case statements are ugly
	private void updateDefaultModes() {
		
		DefaultShuffleMode shuffleTarget;
		DefaultRepeatMode repeatTarget;
		
		switch ( mode ) {
			case ALBUM:
			case ALBUM_REORDERED:
				shuffleTarget = albumShuffleMode;
				repeatTarget = albumRepeatMode;
				break;
				
			case PLAYLIST:
				switch ( getCurrentPlaylist().getShuffleMode() ) {
					case SEQUENTIAL: shuffleTarget = DefaultShuffleMode.SEQUENTIAL; break;
					case SHUFFLE: shuffleTarget = DefaultShuffleMode.SHUFFLE; break;
					case USE_DEFAULT: default: shuffleTarget = playlistShuffleMode; break;
				}
				
				switch ( getCurrentPlaylist().getRepeatMode() ) {
					case PLAY_ONCE: repeatTarget = DefaultRepeatMode.PLAY_ONCE; break;
					case REPEAT: repeatTarget = DefaultRepeatMode.REPEAT; break;
					case USE_DEFAULT: default: repeatTarget = playlistRepeatMode; break;
				}
				break;
				
			case PLAYLIST_UNSAVED:
			case EMPTY:
			default:
				shuffleTarget = trackShuffleMode;
				repeatTarget = trackRepeatMode;
				break;
		}
		
		switch ( shuffleTarget ) {
			case NO_CHANGE:
				//Do nothing
				break;
			case SEQUENTIAL:
				audioSystem.setShuffleMode( ShuffleMode.SEQUENTIAL );
				break;
			case SHUFFLE:
				audioSystem.setShuffleMode( ShuffleMode.SHUFFLE );
				break;
		}
		
		switch ( repeatTarget ) {
			case NO_CHANGE:
				//Do nothing
				break;
			case PLAY_ONCE:
				audioSystem.setRepeatMode( RepeatMode.PLAY_ONCE );
				break;
			case REPEAT:
				audioSystem.setRepeatMode( RepeatMode.REPEAT );
				break;
		}
	}
	
	public void playlistsSet ( List <Playlist> playlists ) {
		
		if ( playlists == null ) {
			mode = Mode.EMPTY;
			currentAlbums.clear();
			currentPlaylist = null;
			LOGGER.log( Level.FINE, "Recieved an null playlist list." );
			notifyListenersStateChanged();
			return;
		}
		
		playlists.removeIf( Objects::isNull );
		
		if ( playlists.size() == 0 ) {
			mode = Mode.EMPTY;
			currentAlbums.clear();
			currentPlaylist = null;
			notifyListenersStateChanged();
			return;
		
		} else if ( playlists.size() >= 1 ) {
			mode = Mode.PLAYLIST;
			currentPlaylist = playlists.get( 0 );
			updateDefaultModes();
			currentAlbums.clear();
			notifyListenersStateChanged();
			return;
		}
	}
	
	public void tracksSet () {
		mode = Mode.PLAYLIST_UNSAVED;
		updateDefaultModes();
		currentAlbums.clear();
		currentPlaylist = null;
		notifyListenersStateChanged();
	}
	
	public void tracksAdded () {
		
		if ( mode == Mode.PLAYLIST ) {
			
			if ( currentPlaylist.getTracks().equals( items ) ) {
				mode = Mode.PLAYLIST;			
				
			} else {
				mode = Mode.PLAYLIST_UNSAVED;
			}
			
		} else if ( mode == Mode.ALBUM || mode == Mode.ALBUM_REORDERED ) {
			mode = Mode.PLAYLIST_UNSAVED;
		}
		
		notifyListenersStateChanged();
		
	}
	
	public void tracksRemoved () {
		if ( mode == Mode.PLAYLIST ) {
			mode = Mode.PLAYLIST_UNSAVED;
			
		} else if ( mode == Mode.ALBUM ) {
			mode = Mode.PLAYLIST_UNSAVED;
			this.currentAlbums.clear();

		}
		
		notifyListenersStateChanged();
		
	}
	
	public void listCleared () {
		currentAlbums.clear();
		mode = Mode.EMPTY;
		notifyListenersStateChanged();
		
	}

	public void listReordered () {
		if ( mode == Mode.ALBUM ) {
			mode = Mode.ALBUM_REORDERED;
			
		} else if ( mode == Mode.PLAYLIST ) {
			if ( currentPlaylist != null && items.equals( currentPlaylist.getTracks() ) ) {
				mode = Mode.PLAYLIST;			
				
			} else {
				mode = Mode.PLAYLIST_UNSAVED;
			}
		}

		notifyListenersStateChanged();
	}
	
	public void setAndPlayAlbum ( Album album ) {
		setAndPlayAlbums( Arrays.asList( album ) );
	}
	
	public void setAndPlayAlbums ( List <Album> albums ) {
		setAlbums( albums );
		audioSystem.next( false );
		Hypnos.getLibrary().albumsToUpdate.addAll( albums );  //TODO: pass library in don't call Hypnos.get()
	}
	
	public void setAndPlayPlaylist ( Playlist playlist ) {
		setAndPlayPlaylists( Arrays.asList( playlist ) );
	}

	public void setAndPlayPlaylists ( List <Playlist> playlists ) {
		setPlaylists( playlists );
		audioSystem.next( false );
	}

	public FilteredList <CurrentListTrack> getFilteredItems () {
		return currentListFiltered;
	}
	
	public SortedList <CurrentListTrack> getSortedItems () {
		return currentListSorted;
	}

	public List <CurrentListTrack> getSortedItemsNoFilter () {
		if ( currentListSortedNoFilter == null ) {
			//REFACTOR: This is the right way to do this, but it's really bad in terms of module independence
			//Fix it at some point. 
			currentListSortedNoFilter = new SortedList <CurrentListTrack> ( items );
			currentListSortedNoFilter.comparatorProperty().bind( Hypnos.getUI().getCurrentListPane().currentListTable.comparatorProperty() );
		}
		
		return currentListSortedNoFilter;
	}
	
	public void setItemsToSortedOrder() {
		items.setAll( new ArrayList<CurrentListTrack> ( currentListSorted ) );
	}

	public void setFilter ( String newValue, boolean b ) {
		currentListTableFilter.setFilter( newValue, false );
	}
}
