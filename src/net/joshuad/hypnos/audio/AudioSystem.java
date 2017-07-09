package net.joshuad.hypnos.audio;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.joshuad.hypnos.Album;
import net.joshuad.hypnos.CurrentListTrack;
import net.joshuad.hypnos.History;
import net.joshuad.hypnos.Persister;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Queue;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Persister.Setting;

/* TODO: Consider, is this class doing too much? 
 * 1. Sends requests to AudioPlayer
 * 2. Manages next/previous stack
 * 3. manages current list
 */

public class AudioSystem {
	
	private static final Logger LOGGER = Logger.getLogger( AudioSystem.class.getName() );
	private static final int MAX_PREVIOUS_NEXT_STACK_SIZE = 10000;

	public enum ShuffleMode {
		SEQUENTIAL ( "⇉" ), SHUFFLE ( "🔀" );
		String symbol;
		ShuffleMode ( String symbol ) { this.symbol = symbol; }
		public String getSymbol () { return symbol; }
	}

	public enum RepeatMode {
		PLAY_ONCE ( "⇥" ), REPEAT ( "🔁" ), REPEAT_ONE_TRACK ( "🔂" );
		String symbol;
		RepeatMode ( String symbol ) { this.symbol = symbol; }
		public String getSymbol () { return symbol; }
	}

	private ShuffleMode shuffleMode = ShuffleMode.SEQUENTIAL;
	private RepeatMode repeatMode = RepeatMode.PLAY_ONCE;
	
	private Vector<PlayerListener> playerListeners = new Vector<PlayerListener> ();
	private Vector<CurrentListListener> currentListListeners = new Vector<CurrentListListener> ();

	private final ObservableList <CurrentListTrack> currentList = FXCollections.observableArrayList(); 

	private AudioPlayer player;

	private final ArrayList <Track> previousNextStack = new ArrayList <Track>( MAX_PREVIOUS_NEXT_STACK_SIZE );
	
	private Random randomGenerator = new Random();

	static Playlist currentPlaylist = null;
	
	private int shuffleTracksPlayedCounter = 0;
	
	private Queue queue;
	private History history; 
	
	public AudioSystem () {
		player = new AudioPlayer ( this );
		queue = new Queue();
		history = new History();
	}
	
	public void unpause() {
		player.requestUnpause();
	}
	
	public void pause() {
		player.requestPause();
	}
	
	public void togglePause() {
		player.requestTogglePause();
	}
	
	public void play() {
		stop( true );
		next();
		unpause();
	}
	
	public void stop ( boolean userRequested ) {
		if ( player != null ) {
			Track track = player.getTrack();
			if ( track instanceof CurrentListTrack ) ((CurrentListTrack)track).setIsCurrentTrack( false );
			player.requestStop();

			notifyListenersStopped( player.getTrack(), userRequested ); 
		}
		
		shuffleTracksPlayedCounter = 0;
	}
	
	public void previous ( ) {
		boolean startPaused = player.isPaused() || player.isStopped();
		previous ( startPaused );
	}
	
	public void previous( boolean startPaused ) {
		
		Track previousTrack = null;

		synchronized ( previousNextStack ) {
			while ( !previousNextStack.isEmpty() && previousTrack == null ) {
				Track candidate = previousNextStack.remove( 0 );
				if ( shuffleTracksPlayedCounter > 0 ) shuffleTracksPlayedCounter--;
				
				if ( candidate.equals( player.getTrack() ) ) {
					if ( !previousNextStack.isEmpty() ) {
						candidate = previousNextStack.remove( 0 );
						if ( shuffleTracksPlayedCounter > 0 ) shuffleTracksPlayedCounter--;
					} else {
						candidate = null;
					}
				}
				
				if ( currentList.contains( candidate ) ) {
					previousTrack = candidate;
				}
			}
		}

		if ( previousTrack != null ) {
			playTrack ( previousTrack, startPaused, false );
			
		} else if ( repeatMode == RepeatMode.PLAY_ONCE ) {
			shuffleTracksPlayedCounter = 1;
			Track previousTrackInList = null;
			for ( CurrentListTrack track : currentList ) {
				if ( track.getIsCurrentTrack() ) {
					if ( previousTrackInList != null ) {
						playTrack( previousTrackInList, startPaused, false );
					} else {
						playTrack( track, startPaused, false );
					}
					break;
				} else {
					previousTrackInList = track;
				}
			}
		} else if ( repeatMode == RepeatMode.REPEAT ) {
			Track previousTrackInList = null;
			for ( CurrentListTrack track : currentList ) {
				if ( track.getIsCurrentTrack() ) {
					if ( previousTrackInList != null ) {
						playTrack( previousTrackInList, startPaused, false );
					} else {
						playTrack( currentList.get( currentList.size() - 1 ), startPaused, false );
					}
					break;
				} else {
					previousTrackInList = track;
				}
			}
		}
	}
	
	public void next() {
		boolean startPaused = player.isPaused() || player.isStopped();
		next ( startPaused );
	}
	
	public void next ( boolean startPaused ) {

		if ( queue.hasNext() ) {
			playTrack( queue.getNextTrack(), startPaused );
			
		} else if ( shuffleMode == ShuffleMode.SEQUENTIAL ) {
			ListIterator <CurrentListTrack> iterator = currentList.listIterator();
			boolean didSomething = false;
			
			while ( iterator.hasNext() ) {
				if ( iterator.next().getIsCurrentTrack() ) {
					if ( iterator.hasNext() ) {
						playTrack( iterator.next(), startPaused );
						didSomething = true;
					} else if ( repeatMode == RepeatMode.PLAY_ONCE ) {
						shuffleTracksPlayedCounter = 1;
						stop( false );
						didSomething = true;
					} else if ( repeatMode == RepeatMode.REPEAT && currentList.size() > 0 ) {
						playTrack( currentList.get( 0 ), startPaused );
						didSomething = true;
					} else {
						stop( false );
						didSomething = true;
					} 
					break;
				}
			}
			if ( !didSomething ) {
				if ( currentList.size() > 0 ) {
					playTrack ( currentList.get( 0 ), startPaused );
				}
			}
			
		} else if ( shuffleMode == ShuffleMode.SHUFFLE ) {
			if ( repeatMode == RepeatMode.REPEAT ) {
				
				shuffleTracksPlayedCounter = 1;
				// TODO: I think there may be issues with multithreading here.
				// TODO: Ban the most recent X tracks from playing
				int currentListSize = currentList.size();
				int collisionWindowSize = currentListSize / 3; // TODO: Fine tune this amount
				int permittedRetries = 3; // TODO: fine tune this number
	
				boolean foundMatch = false;
				int retryCount = 0;
				Track playMe;
	
				List <Track> collisionWindow;
	
				if ( previousNextStack.size() >= collisionWindowSize ) {
					collisionWindow = previousNextStack.subList( 0, collisionWindowSize );
				} else {
					collisionWindow = previousNextStack;
				}
	
				do {
					playMe = currentList.get( randomGenerator.nextInt( currentList.size() ) );
					if ( !collisionWindow.contains( playMe ) ) {
						foundMatch = true;
					} else {
						++retryCount;
					}
				} while ( !foundMatch && retryCount < permittedRetries );
	
				playTrack( playMe, startPaused );
				
			} else {
				if ( shuffleTracksPlayedCounter < currentList.size() ) {
					List <Track> alreadyPlayed = previousNextStack.subList( 0, shuffleTracksPlayedCounter );
					ArrayList <Track> viableTracks = new ArrayList <Track>( currentList );
					viableTracks.removeAll( alreadyPlayed );
					Track playMe = viableTracks.get( randomGenerator.nextInt( viableTracks.size() ) );
					playTrack( playMe, startPaused );
					++shuffleTracksPlayedCounter;
				} else {
					stop( false );
				}
			} 
		}
	}
	
	public int getCurrentTrackIndex() {
		for ( int k = 0 ; k < currentList.size(); k++ ) {
			if ( currentList.get( k ).getIsCurrentTrack() ) {
				return k;
			}
		}
		
		return -1;
	}
	
	public void setShuffleMode ( ShuffleMode newMode ) {
		this.shuffleMode = newMode;
		notifyListenersShuffleModeChanged ( shuffleMode );
	}

	public ShuffleMode getShuffleMode() {
		return shuffleMode;
	}

	public void toggleShuffleMode() {
		if ( shuffleMode == ShuffleMode.SEQUENTIAL ) {
			shuffleMode = ShuffleMode.SHUFFLE;
		} else {
			shuffleMode = ShuffleMode.SEQUENTIAL;
		}
		
		notifyListenersShuffleModeChanged ( shuffleMode );
	}
	
	
	
	public void setRepeatMode ( RepeatMode newMode ) {
		this.repeatMode = newMode;
		notifyListenersRepeatModeChanged ( newMode );
	}
	
	public RepeatMode getRepeatMode() {
		return repeatMode;
	}
	
	public void toggleRepeatMode() {
		if ( repeatMode == RepeatMode.PLAY_ONCE ) {
			repeatMode = RepeatMode.REPEAT;
		} else if ( repeatMode == RepeatMode.REPEAT ) {
			repeatMode = RepeatMode.REPEAT_ONE_TRACK;
		} else {
			repeatMode = RepeatMode.PLAY_ONCE;
		}
		
		notifyListenersRepeatModeChanged ( repeatMode );
	}
	
	

	public History getHistory () {
		return history;
	}
	
	public Queue getQueue() {
		return queue;
	}
	
	public Track getCurrentTrack() {
		return player.getTrack();
	}

	public ObservableList <CurrentListTrack> getCurrentList () {
		return currentList;
	}
	
	public Playlist getCurrentPlaylist () {
		return currentPlaylist;
	}
	
	public void shuffleList() {
		Collections.shuffle( currentList );
	}

	public void seekPercent ( double percent ) {
		player.requestSeekPercent( percent );
	}
	
	public void seekMS ( long ms ) {
		player.requestSeekMS( ms );
	}

	public void setVolumePercent ( double percent ) {
		player.requestVolumePercent( percent );
	}

	public void decrementVolume () {
		double target = player.getVolumePercent() - .05;
		if ( target < 0 ) target = 0;
		player.requestVolumePercent( target );
	}
	
	public void incrementVolume () {
		double target = player.getVolumePercent() + .05;
		if ( target > 1 ) target = 1;
		player.requestVolumePercent( target );
	}
	
	public boolean isPlaying () {
		return player.isPlaying();
	}
	
	public boolean isPaused() {
		return player.isPaused();
	}

	public boolean isStopped () {
		return player.isStopped();
	}
	
	
	
	public EnumMap <Persister.Setting, ? extends Object> getSettings () {
		EnumMap <Persister.Setting, Object> retMe = new EnumMap <Persister.Setting, Object> ( Persister.Setting.class );
		
		if ( !player.isStopped() ) {
			retMe.put ( Setting.TRACK, player.getTrack().getPath().toString() );
			retMe.put ( Setting.TRACK_POSITION, player.getPositionMS() );
			retMe.put ( Setting.TRACK_NUMBER, getCurrentTrackIndex() );
		}

		retMe.put ( Setting.SHUFFLE, getShuffleMode().toString() );
		retMe.put ( Setting.REPEAT, getRepeatMode() );
		retMe.put ( Setting.VOLUME, player.getVolumePercent() );
		
		return retMe;
	}
	
	
	
	
	
//Manage Listeners

	public void addPlayerListener ( PlayerListener listener ) {
		if ( listener != null ) {
			playerListeners.add( listener );
		} else {
			LOGGER.info( "Null player listener was attempted to be added, ignoring." );
		}
	}
	
	public void addCurrentListListener ( CurrentListListener listener ) {		
		if ( listener != null ) {
			currentListListeners.add( listener );
		} else {
			LOGGER.info( "Null player listener was attempted to be added, ignoring." );
		}
		
	}
	
	private void notifyListenersPositionChanged ( int positionMS, int lengthMS ) {
		for ( PlayerListener listener : playerListeners ) {
			listener.playerPositionChanged( positionMS, lengthMS );
		}
	}
	
	private void notifyListenersStopped ( Track track, boolean userRequested ) {
		for ( PlayerListener listener : playerListeners ) {
			listener.playerStopped( track, userRequested );
		}
	}
	
	private void notifyListenersStarted ( Track track ) {
		for ( PlayerListener listener : playerListeners ) {
			listener.playerStarted( track );
		}
	}
	
	private void notifyListenersPaused () {
		for ( PlayerListener listener : playerListeners ) {
			listener.playerPaused();
		}
	}
	
	private void notifyListenersUnpaused () {
		for ( PlayerListener listener : playerListeners ) {
			listener.playerUnpaused( );
		}
	}
	
	private void notifyListenersVolumeChanged ( double newVolumePercent ) {
		for ( PlayerListener listener : playerListeners ) {
			listener.playerVolumeChanged( newVolumePercent );
		}
	}
	
	private void notifyListenersShuffleModeChanged ( ShuffleMode newMode ) {
		for ( PlayerListener listener : playerListeners ) {
			listener.playerShuffleModeChanged( newMode );
		}
	}
	
	private void notifyListenersRepeatModeChanged ( RepeatMode newMode ) {
		for ( PlayerListener listener : playerListeners ) {
			listener.playerRepeatModeChanged( newMode );
		}
	}
	
	
	
	private void notifyListenersAlbumsSet ( List<Album> albums ) {
		for ( CurrentListListener listener : currentListListeners ) {
			listener.albumsSet( albums );
		}
	}
	
	private void notifyListenersPlaylistsSet ( List<Playlist> playlists ) {
		for ( CurrentListListener listener : currentListListeners ) {
			listener.playlistsSet( playlists );
		}
	}
	
	private void notifyListenersTracksSet () {
		for ( CurrentListListener listener : currentListListeners ) {
			listener.tracksSet();
		}
	}
	
	private void notifyListenersTracksAdded () {
		for ( CurrentListListener listener : currentListListeners ) {
			listener.tracksAdded();
		}
	}
	
	private void notifyListenersTracksRemoved () {
		for ( CurrentListListener listener : currentListListeners ) {
			listener.tracksRemoved();
		}
	}
	
	private void notifyListenersListCleared () {
		for ( CurrentListListener listener : currentListListeners ) {
			listener.listCleared();
		}
	}
	
	private void notifyListenersListReordered() {
		for ( CurrentListListener listener : currentListListeners ) {
			listener.listReordered();
		}
	}
	
	
	
	
//TODO: Make these a listener interface, and add this object as a listener to player? 	
	
	void playerStopped ( boolean userRequested ) {
		
		if ( !userRequested ) {
			if ( repeatMode == RepeatMode.REPEAT_ONE_TRACK ) {
				playTrack ( history.getLastTrack() );
			} else {
				next ( false );
			}
		}
	}

	void playerPaused () {
		notifyListenersPaused();
	}

	void playerUnpaused () {
		notifyListenersUnpaused();
	}

	void volumeChanged ( double volumePercentRequested ) {
		notifyListenersVolumeChanged ( volumePercentRequested );
	}

	void playerStarted ( Track track ) {
		notifyListenersStarted( track );
	}

	void playerTrackPositionChanged ( int positionMS, int lengthMS ) {
		notifyListenersPositionChanged ( positionMS, lengthMS );
	}
	
	

	public void playTrack ( Track track ) {
		playTrack ( track, false );
	}
	
	public void playTrack ( Track track, boolean startPaused ) {
		playTrack ( track, startPaused, true );
	}
	                                                                 
	public void playTrack ( Track track, boolean startPaused, boolean addToPreviousNextStack ) {
		
		player.requestPlayTrack( track, startPaused );
		
		for ( CurrentListTrack listTrack : currentList ) {
			listTrack.setIsCurrentTrack( false );
		}
		
		if ( track instanceof CurrentListTrack ) {
			((CurrentListTrack)track).setIsCurrentTrack( true );
		}
		
		if ( addToPreviousNextStack ) {
			while ( previousNextStack.size() >= MAX_PREVIOUS_NEXT_STACK_SIZE ) {
				previousNextStack.remove( previousNextStack.size() - 1 );
			}
			
			previousNextStack.add( 0, track );
		}
		
		history.trackPlayed( track );
	}
		
	
	
// Add and remove tracks from the current list
	
	public void clearList() {
		if ( currentList.size() > 0 ) {
			notifyListenersListCleared();
		}
		currentList.clear();
	}
	
	public void clearQueue() {
		queue.clear();
	}
	
	public void removeTracksAtIndices ( List <Integer> indicies ) {
		int tracksRemoved = 0;
		for ( int k = indicies.size() - 1; k >= 0; k-- ) {
			if ( indicies.get( k ) >= 0 && indicies.get ( k ) < currentList.size() ) {
				currentList.remove ( indicies.get( k ).intValue() );
				tracksRemoved++;
			}
		}
		
		if ( tracksRemoved > 0 ) {
			if ( currentList.size() == 0 ) {
				notifyListenersListCleared();
				currentPlaylist = null;
			} else {
				notifyListenersTracksRemoved();
			}
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
		clearQueue();
		appendTracks ( tracks );
	}
		
	public void setTracksPathList ( List <Path> paths ) {
		clearList();
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
		insertTracks ( currentList.size() - 1, tracks );
	}
		
	public void appendTracksPathList ( List <Path> paths ) {
		insertTrackPathList ( currentList.size() - 1, paths );
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
		
		boolean startedEmpty = currentList.isEmpty();
		
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
		
		synchronized ( currentList ) {
			if ( index < 0 ) {
				LOGGER.fine( "Asked to insert tracks at: " + index + ", inserting at 0 instead." );
				index = 0;
			} else if ( index > currentList.size() ) {
				LOGGER.fine( "Asked to insert tracks past the end of current list. Inserting at end instead." );
				index = currentList.size();
			}
	
			currentList.addAll( index, tracks );
		}
		
		if ( tracksAdded > 0 ) {
			if ( startedEmpty ) {
				notifyListenersTracksSet();
			} else {
				notifyListenersTracksAdded();
			}
		}
	}
	
	public void appendAlbum ( Album album ) {
		
		boolean startedEmpty = false;
		if ( currentList.size() == 0 ) startedEmpty = true;
		
		appendTracks ( album.getTracks() );
		
		if ( startedEmpty ) {
			this.notifyListenersAlbumsSet( Arrays.asList( album ) );
		}
	}
	
	public void appendAlbums ( List<Album> albums ) {

		boolean startedEmpty = false;
		if ( currentList.size() == 0 ) startedEmpty = true;
		
		int albumsAdded = 0;
		Album lastAlbum = null;
		for ( Album album : albums ) {
			if ( album != null ) {
				appendTracks ( album.getTracks() );
				lastAlbum = album;
				albumsAdded++;
			}
		}
		
		if ( startedEmpty ) {
			notifyListenersAlbumsSet ( albums );
		}
	}
	
	public void setAlbum ( Album album ) {
		setTracks ( album.getTracks() );
		notifyListenersAlbumsSet ( Arrays.asList( album ) );
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
		
		notifyListenersAlbumsSet ( albums );
	}
	
	
	public void appendPlaylist ( Playlist playlist ) {
		boolean setAsPlaylist = false;
		if ( currentList.size() == 0 ) setAsPlaylist = true;
		
		appendTracks ( playlist.getTracks() );
		
		if ( setAsPlaylist ) {
			currentPlaylist = playlist;
			notifyListenersPlaylistsSet ( Arrays.asList( playlist ) );
		}
	}
	
	public void appendPlaylists ( List<Playlist> playlists ) {

		boolean startedEmpty = false;
		if ( currentList.size() == 0 ) startedEmpty = true;
		
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
			notifyListenersPlaylistsSet ( playlists );
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
		
		notifyListenersPlaylistsSet ( playlists );
	}

	public void playItems ( List <Track> items ) {
		if ( items.size() == 1 ) {
			playTrack ( items.get( 0 ) );
		} else if ( items.size() > 1 ) {
			setTracks( items );
			play();
		}
	}
}







