package net.joshuad.hypnos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.AudioSystem.RepeatMode;
import net.joshuad.hypnos.audio.AudioSystem.ShuffleMode;

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
	
	private final List <CurrentListListener> listeners = new ArrayList<CurrentListListener> ();
	
	private AudioSystem player;
	private Queue queue;

	private DefaultShuffleMode albumShuffleMode = DefaultShuffleMode.SEQUENTIAL;
	private DefaultShuffleMode trackShuffleMode = DefaultShuffleMode.NO_CHANGE;
	private DefaultShuffleMode playlistShuffleMode = DefaultShuffleMode.SHUFFLE;

	private DefaultRepeatMode albumRepeatMode = DefaultRepeatMode.PLAY_ONCE;
	private DefaultRepeatMode trackRepeatMode = DefaultRepeatMode.NO_CHANGE;
	private DefaultRepeatMode playlistRepeatMode = DefaultRepeatMode.REPEAT;
	
	List <Thread> noLoadThreads = new ArrayList <Thread> ();
	
	public CurrentList ( AudioSystem player, Queue queue ) {
		this.queue = queue;
		this.player = player;
		
		startListWatcher();
		
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
						
						if ( track.needsUpdate() ) {
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
		return new CurrentListState ( items, currentAlbums, currentPlaylist, mode );
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
	}
		
	public void clearList() {
		if ( items.size() > 0 ) {
			listCleared();
		}
		items.clear();
	}
	
	public void removeTracksAtIndices ( List <Integer> indicies ) {

		Runnable runMe = new Runnable() {
			public void run() {
				int tracksRemoved = 0;
				for ( int k = indicies.size() - 1; k >= 0; k-- ) {
					if ( indicies.get( k ) >= 0 && indicies.get ( k ) < items.size() ) {
						CurrentListTrack itemRemoved = items.remove ( indicies.get( k ).intValue() );
						
						if ( indicies.get( k ) > 0 && itemRemoved.isLastCurrentListTrack() ) {
							items.get( indicies.get( k ) - 1 ).setIsLastCurrentListTrack( true );
						}
							
						tracksRemoved++;
					}
				}
				
				if ( tracksRemoved > 0 ) {
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
		clearList();
		queue.clear();
		appendTracks ( tracks );
	}
		
	public void setTracksPathList ( List <Path> paths ) {
		clearList();
		queue.clear();
		appendTracksPathList ( paths );
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
		insertTrackPathList ( items.size() - 1, paths );
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
	
	public void insertTrackPathList ( int index, List <Path> paths ) {
		
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
						LOGGER.fine( "Asked to insert tracks at: " + index + ", inserting at 0 instead." );
						targetIndex = 0;
					} else if ( index > items.size() ) {
						LOGGER.fine( "Asked to insert tracks past the end of current list. Inserting at end instead." );
						targetIndex = items.size();
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
			}
		};
		
		doThreadAware ( runMe );
	}
	
	private void addItem ( int index, CurrentListTrack track ) {
		if ( track == null ) return;
		
		items.add( index, track );
		
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
	
	public void setAlbum ( Album album ) {
		setAlbums ( Arrays.asList( album ) );
	}
	
	public void setAlbums ( List<Album> albums ) {
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
		
		setTracks ( addMe );
		albumsSet ( albums );
		
		Hypnos.getLibrary().albumsToUpdate.addAll( albums ); //REFACTOR: pass library in at constructor rather than calling Hypnos.get
		
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
			updateShuffleAndRepeatMode();
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
					simpleTitle = album.getSimpleTitle();
					year = album.getYear();
					artist = album.getAlbumArtist();
					initialized = true;
					
				} else {
					if ( !album.getSimpleTitle().equals( simpleTitle ) 
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
				updateShuffleAndRepeatMode();
				currentAlbums.clear();
				currentPlaylist = null;
				notifyListenersStateChanged();
				return;
				
			} else {
				mode = Mode.ALBUM;
				updateShuffleAndRepeatMode();
				currentAlbums.clear();
				currentAlbums.addAll( albums );
				notifyListenersStateChanged();
				return;
			}
		}
	}
	
	private void updateShuffleAndRepeatMode() {
		
		DefaultShuffleMode shuffleTarget;
		DefaultRepeatMode repeatTarget;
		
		switch ( mode ) {
			case ALBUM:
			case ALBUM_REORDERED:
				shuffleTarget = albumShuffleMode;
				repeatTarget = albumRepeatMode;
				break;
				
			case PLAYLIST:
				shuffleTarget = playlistShuffleMode;
				repeatTarget = playlistRepeatMode;
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
				player.setShuffleMode( ShuffleMode.SEQUENTIAL );
				break;
			case SHUFFLE:
				player.setShuffleMode( ShuffleMode.SHUFFLE );
				break;
			
		}
		
		switch ( repeatTarget ) {
			case NO_CHANGE:
				//Do nothing
				break;
			case PLAY_ONCE:
				player.setRepeatMode( RepeatMode.PLAY_ONCE );
				break;
			case REPEAT:
				player.setRepeatMode( RepeatMode.REPEAT );
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
		
		} else if ( playlists.size() == 1 ) {
			mode = Mode.PLAYLIST;
			updateShuffleAndRepeatMode();
			currentAlbums.clear();
			currentPlaylist = playlists.get( 0 );
			notifyListenersStateChanged();
			return;

		} else {
			mode = Mode.PLAYLIST;
			updateShuffleAndRepeatMode();
			currentAlbums.clear();
			currentPlaylist = null;
			notifyListenersStateChanged();
			return;
		}
	}
	
	public void tracksSet () {
		mode = Mode.PLAYLIST_UNSAVED;
		updateShuffleAndRepeatMode();
		currentAlbums.clear();
		currentPlaylist = null;
		notifyListenersStateChanged();
	}
	
	public void tracksAdded () {
		if ( mode == Mode.PLAYLIST ) {
			boolean listsMatch = true;
			
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
	
}
