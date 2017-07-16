package net.joshuad.hypnos;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CurrentList {

	private static final Logger LOGGER = Logger.getLogger( CurrentList.class.getName() );
	
	enum Mode {
		ALBUM,
		ALBUM_REORDERED,
		PLAYLIST,
		PLAYLIST_UNSAVED,
		EMPTY;
	}
		
	Mode mode = Mode.EMPTY;
	final List <Album> currentAlbums = new ArrayList <Album> ();
	Playlist currentPlaylist;
	
	private final ObservableList <CurrentListTrack> items = FXCollections.observableArrayList(); 
	
	private final List <CurrentListListener> listeners = new ArrayList<CurrentListListener> ();
	
	Queue queue;
	
	
	public CurrentList ( Queue queue ) {
		this.queue = queue;
		
		startListWatcher();
	}
	
	private void startListWatcher() {
		Thread watcher = new Thread( () -> {
			while ( true ) {
				synchronized ( items ) {
					for ( CurrentListTrack track : items ) {
						boolean isMissing = !Utils.isMusicFile( track.getPath() );
						if ( !isMissing && track.isMissingFile() ) {
							track.setIsMissingFile ( false );
							
						} else if ( isMissing && !track.isMissingFile() ) {
							track.setIsMissingFile ( true );
						}
					}
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
	
	public CurrentListState getState () {
		return new CurrentListState ( items, currentAlbums, currentPlaylist, mode );
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
		int tracksRemoved = 0;
		for ( int k = indicies.size() - 1; k >= 0; k-- ) {
			if ( indicies.get( k ) >= 0 && indicies.get ( k ) < items.size() ) {
				items.remove ( indicies.get( k ).intValue() );
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
		appendTracksPathList ( Arrays.asList( track.getPath() ) );
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
		
		ArrayList <Path> paths = new ArrayList <Path> ( tracks.size() );
		
		for ( Track track : tracks ) {
			if ( track == null ) {
				LOGGER.fine( "Recieved a null track. Skipping." );
			} else {
				paths.add ( track.getPath() );
			}
		}
		
		insertTrackPathList ( index, paths );
	}
	
	public void insertTrackPathList ( int index, List <Path> paths ) {
		
		boolean startedEmpty = items.isEmpty();
		
		if ( paths == null || paths.size() <= 0 ) {
			LOGGER.fine( "Recieved a null or empty track list. No tracks loaded." );
			return;
		}
		
		ArrayList <CurrentListTrack> tracks = new ArrayList <CurrentListTrack> ( paths.size() );
		
		int tracksAdded = 0;
		for ( Path path : paths ) {
			try {
				tracks.add ( new CurrentListTrack ( path ) );
				tracksAdded++;
			} catch ( IOException | NullPointerException e ) {
				LOGGER.fine( "Recieved a null or empty track. Skipping." );
			}
		}
		
		synchronized ( items ) {
			if ( index < 0 ) {
				LOGGER.fine( "Asked to insert tracks at: " + index + ", inserting at 0 instead." );
				index = 0;
			} else if ( index > items.size() ) {
				LOGGER.fine( "Asked to insert tracks past the end of current list. Inserting at end instead." );
				index = items.size();
			}
	
			items.addAll( index, tracks );
		}
		
		if ( tracksAdded > 0 ) {
			if ( startedEmpty ) {
				tracksSet();
			} else {
				tracksAdded();
			}
		}
	}
	
	public void appendAlbum ( Album album ) {
		appendAlbums ( Arrays.asList( album ) );
	}
	
	public void appendAlbums ( List<Album> albums ) {
		insertAlbums ( items.size(), albums );
	} 
	
	public void insertAlbums ( int index, List<Album> albums ) {
		if ( index > items.size() ) index = items.size();
		
		boolean insertedAtEnd = ( index == items.size() );

		boolean startedEmpty = false;
		Mode startMode = mode;
		
		if ( items.size() == 0 ) startedEmpty = true;
		
		List<Track> addMe = new ArrayList<Track> ();
		for ( Album album : albums ) {
			if ( album != null ) {
				addMe.addAll( album.getTracks() );
			}
		}
		
		insertTracks ( index, addMe );
		
		if ( startedEmpty ) {
			albumsSet ( albums );
			
		} else if ( startMode == Mode.ALBUM || startMode == Mode.ALBUM_REORDERED ) {
			List<Album> fullAlbumList = new ArrayList<Album> ( currentAlbums );
			fullAlbumList.addAll ( albums );
			albumsSet ( fullAlbumList );
			
			if ( index != 0 && !insertedAtEnd ) {
				listReordered();
			}
		}
	}
	
	public void setAlbum ( Album album ) {
		setTracks ( album.getTracks() );
		albumsSet ( Arrays.asList( album ) );
	}
	
	public void setAlbums ( List<Album> albums ) {
		List <Track> addMe = new ArrayList <Track> ();
		
		int albumsAdded = 0;
		Album albumAdded = null;
		
		for ( Album album : albums ) {
			if ( album != null ) {
				addMe.addAll ( album.getTracks() );
				albumsAdded++;
				albumAdded = album;
			}
		}
		
		setTracks ( addMe );
		albumsSet ( albums );
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
				currentAlbums.clear();
				currentPlaylist = null;
				notifyListenersStateChanged();
				return;
				
			} else {
				mode = Mode.ALBUM;
				currentAlbums.clear();
				currentAlbums.addAll( albums );
				notifyListenersStateChanged();
				return;
			}
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
			currentAlbums.clear();
			currentPlaylist = playlists.get( 0 );
			notifyListenersStateChanged();
			return;

		} else {
			mode = Mode.PLAYLIST;
			currentAlbums.clear();
			currentPlaylist = null;
			notifyListenersStateChanged();
			return;
		}
	}
	
	public void tracksSet () {
		mode = Mode.PLAYLIST_UNSAVED;
		currentAlbums.clear();
		currentPlaylist = null;
		notifyListenersStateChanged();
	}
	
	public void tracksAdded () {
		if ( mode == Mode.PLAYLIST ) {
			
			if ( currentPlaylist != null && items.equals( currentPlaylist.getTracks() ) ) {
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
