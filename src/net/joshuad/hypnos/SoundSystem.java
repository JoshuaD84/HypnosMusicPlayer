package net.joshuad.hypnos;

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
import net.joshuad.hypnos.Persister.Setting;
import net.joshuad.hypnos.audio.AudioPlayer;
import net.joshuad.hypnos.audio.PlayerListener;

/* TODO: Consider, is this class doing too much? 
 * 1. Sends requests to AudioPlayer
 * 2. Manages next/previous stack
 * 3. manages current list
 */

public class SoundSystem {
	
	private static final Logger LOGGER = Logger.getLogger( SoundSystem.class.getName() );

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
	
	private Vector<PlayerListener> listeners = new Vector<PlayerListener>();

	private ShuffleMode shuffleMode = ShuffleMode.SEQUENTIAL;
	private RepeatMode repeatMode = RepeatMode.PLAY_ONCE;
	
	private static final int MAX_PREVIOUS_NEXT_STACK_SIZE = 10000;

	private final ObservableList <CurrentListTrack> currentList = FXCollections.observableArrayList(); 

	private final ArrayList <Track> previousNextStack = new ArrayList <Track>( MAX_PREVIOUS_NEXT_STACK_SIZE );

	private AudioPlayer player;
	
	private Random randomGenerator = new Random();

	static Playlist currentPlaylist = null;
	
	private int playOnceShuffleTracksPlayedCounter = 1;
	
	private Queue queue;
	private History history; 
	
	public SoundSystem () {
		player = new AudioPlayer ( this );
		queue = new Queue();
		history = new History();
	}
	
	public Queue getQueue() {
		return queue;
	}
	
	public void unpause() {
		player.requestUnpause();
	}
	
	public void pause() {
		player.requestPause();
	}
	
	public void previousTrack() {
		
		boolean isPaused = player.isPaused();
		
		Track previousTrack = null;

		synchronized ( previousNextStack ) {
			while ( !previousNextStack.isEmpty() && previousTrack == null ) {
				Track candidate = previousNextStack.remove( 0 );
				if ( playOnceShuffleTracksPlayedCounter > 0 ) playOnceShuffleTracksPlayedCounter--;
				
				if ( candidate.equals( player.getTrack() ) ) {
					if ( !previousNextStack.isEmpty() ) {
						candidate = previousNextStack.remove( 0 );
						if ( playOnceShuffleTracksPlayedCounter > 0 ) playOnceShuffleTracksPlayedCounter--;
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
			playTrack ( previousTrack, isPaused, false );
			
		} else if ( repeatMode == RepeatMode.PLAY_ONCE ) {
			playOnceShuffleTracksPlayedCounter = 1;
			Track previousTrackInList = null;
			for ( CurrentListTrack track : currentList ) {
				if ( track.getIsCurrentTrack() ) {
					if ( previousTrackInList != null ) {
						playTrack( previousTrackInList, isPaused, false );
					} else {
						playTrack( track, isPaused, false );
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
						playTrack( previousTrackInList, isPaused, false );
					} else {
						playTrack( currentList.get( currentList.size() - 1 ), isPaused, false );
					}
					break;
				} else {
					previousTrackInList = track;
				}
			}
		}
	}
	
	public void togglePause() {
		if ( player != null && !player.isPaused() ) {
			pause();

		} else {
			unpause();
		}
	}
	
	public void nextTrack () {
		//TODO: Handle what we do when isStopped()
		if ( queue.hasNext() ) {
			playTrack( queue.getNextTrack() );
			
		} else if ( shuffleMode == ShuffleMode.SEQUENTIAL ) {
			ListIterator <CurrentListTrack> iterator = currentList.listIterator();
			boolean didSomething = false;
			boolean currentlyPaused = player.isPaused();
			while ( iterator.hasNext() ) {
				if ( iterator.next().getIsCurrentTrack() ) {
					if ( iterator.hasNext() ) {
						playTrack( iterator.next(), currentlyPaused );
						didSomething = true;
					} else if ( repeatMode == RepeatMode.PLAY_ONCE ) {
						playOnceShuffleTracksPlayedCounter = 1;
						stopTrack();
						didSomething = true;
					} else if ( repeatMode == RepeatMode.REPEAT && currentList.size() > 0 ) {
						playTrack( currentList.get( 0 ), currentlyPaused );
						didSomething = true;
					} else {
						stopTrack();
						didSomething = true;
					} 
					break;
				}
			}
			if ( !didSomething ) {
				if ( currentList.size() > 0 ) {
					playTrack ( currentList.get( 0 ), currentlyPaused );
				}
			}
			
		} else if ( shuffleMode == ShuffleMode.SHUFFLE && repeatMode == RepeatMode.PLAY_ONCE ) {

			if ( playOnceShuffleTracksPlayedCounter < currentList.size() ) {
				List <Track> alreadyPlayed = previousNextStack.subList( 0, playOnceShuffleTracksPlayedCounter );
				ArrayList <Track> viableTracks = new ArrayList <Track>( currentList );
				viableTracks.removeAll( alreadyPlayed );
				Track playMe = viableTracks.get( randomGenerator.nextInt( viableTracks.size() ) );
				playTrack( playMe );
				++playOnceShuffleTracksPlayedCounter;
			} else {
				stopTrack();
			}

		} else if ( shuffleMode == ShuffleMode.SHUFFLE && repeatMode == RepeatMode.REPEAT ) {

			playOnceShuffleTracksPlayedCounter = 1;
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
				collisionWindow = previousNextStack.subList( 0,
						collisionWindowSize );
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

			playTrack( playMe );
		}
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

	public void playAlbum ( Album album ) {
		currentPlaylist = null;
		currentList.clear();
		currentList.addAll( Utils.convertTrackList( album.getTracks() ) );
		Track firstTrack = currentList.get( 0 );
		if ( firstTrack != null ) {
			playTrack( firstTrack );
		}
	}
	
	public void playAlbums ( List<Album> albums ) {
		currentPlaylist = null;
		currentList.clear();
		for ( Album album : albums ) {
			currentList.addAll( Utils.convertTrackList( album.getTracks() ) );
		}
		Track firstTrack = currentList.get( 0 );
		if ( firstTrack != null ) {
			playTrack( firstTrack );
		}
	}
	
	public void loadTrack ( Path path ) {
		try {
			loadTrack ( new CurrentListTrack ( path ) );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			// I think we should probably throw this up for the source to handle. 
			e.printStackTrace();
		}
	}
	
	public void loadTrack ( Track track ) {
		loadTrack ( track, false );
	}
	
	public void loadTrack ( Track track, boolean startPaused ) {
		ArrayList<CurrentListTrack> loadMe = new ArrayList <CurrentListTrack> ( 1 );
		try {
			loadMe.add ( new CurrentListTrack ( track ) );
			loadTracks ( loadMe, startPaused );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void loadTrack ( String trackFile ) {
		loadTrack ( trackFile, false );
	}
	
	public void loadTrack ( String trackFile, boolean startPaused ) {
		try {
			ArrayList<CurrentListTrack> loadMe = new ArrayList <CurrentListTrack> ( 1 );
			loadMe.add ( new CurrentListTrack ( Paths.get( trackFile ) ) );
			loadTracks ( loadMe, startPaused );
		} catch ( IOException e ) {
			System.out.println( "Unable to load track: " + trackFile + ", continuing." );
			System.out.println( e.getMessage() );
		}
	}
	
	public void loadTracks ( List <? extends Track> tracks ) {
		loadTracks ( tracks, false );
	}

	public void loadTracks ( List <? extends Track> tracks, boolean startPaused ) {
		currentList.clear();
		addTracks ( tracks );
	}

	public void playPlaylist ( Playlist playlist ) {
		playPlaylists ( Arrays.asList( playlist ) );
		currentPlaylist = playlist;
	}
	
	public void playPlaylists ( List<Playlist> playlists ) {

		stopTrack();
		currentList.clear();
		for ( Playlist playlist : playlists ) {
			currentList.addAll( Utils.convertTrackList( playlist.getTracks() ) );
		}
		
		if ( !currentList.isEmpty() ) {
			Track firstTrack = currentList.get( 0 );
			if ( firstTrack != null ) {
				playTrack( firstTrack );
			}

		}
	}

	public void appendAlbum ( Album album ) {
		currentList.addAll( Utils.convertTrackList( album.getTracks() ) );
	}

	public void stopTrack () {
		if ( player != null ) {
			Track track = player.getTrack();
			if ( track instanceof CurrentListTrack ) ((CurrentListTrack)track).setIsCurrentTrack( false );
			player.requestStop();

			notifyStopped( player.getTrack(), false ); //TODO: this shouldn't auto-be false.
		}
		
		playOnceShuffleTracksPlayedCounter = 0;
	}
	
	public int getCurrentTrackNumber() {
		for ( int k = 0 ; k < currentList.size(); k++ ) {
			if ( currentList.get( k ).getIsCurrentTrack() ) {
				return k;
			}
		}
		
		return -1;
	}

	public void setShuffleMode ( ShuffleMode newMode ) {
		this.shuffleMode = newMode;
	}
	
	public void setRepeatMode ( RepeatMode newMode ) {
		this.repeatMode = newMode;
	}
	
	public void toggleShuffleMode() {
		if ( shuffleMode == ShuffleMode.SEQUENTIAL ) {
			shuffleMode = ShuffleMode.SHUFFLE;
		} else {
			shuffleMode = ShuffleMode.SEQUENTIAL;
		}
	}
	
	public ShuffleMode getShuffleMode() {
		return shuffleMode;
	}
	
	public void toggleRepeatMode() {
		if ( repeatMode == RepeatMode.PLAY_ONCE ) {
			repeatMode = RepeatMode.REPEAT;
		} else if ( repeatMode == RepeatMode.REPEAT ) {
			repeatMode = RepeatMode.REPEAT_ONE_TRACK;
		} else {
			repeatMode = RepeatMode.PLAY_ONCE;
		}
	}
	
	public RepeatMode getRepeatMode() {
		return repeatMode;
	}

	public History getHistory () {
		return history;
	}
	
	@SuppressWarnings("unchecked")
	public void addTracks ( List <? extends Track> tracks ) {
		if ( tracks == null || tracks.size() <= 0 ) {
			LOGGER.info( "Empty list of tracks added, ignored." );
			return;
		}
		
		if ( tracks.get( 0 ) instanceof CurrentListTrack ) {
			currentList.addAll( (ArrayList<CurrentListTrack>) tracks );
		} else {
			currentList.addAll( Utils.convertTrackList( (ArrayList<Track>) tracks ) );
		}
	}
	
	public void addTrack ( Path path ) throws IOException {
		CurrentListTrack track = new CurrentListTrack ( path );
	}

	public void addTracks ( int index, ArrayList <CurrentListTrack> tracks ) { 
		int boundedIndex = Math.min( index, currentList.size() );
		currentList.addAll( boundedIndex, tracks );
	}

	public boolean isStopped () {
		return player == null;
	}

	public void addAll ( ArrayList <CurrentListTrack> convertTrackList ) {
		currentList.addAll( convertTrackList );
	}
	
	public void clearList() {
		currentList.clear();
	}

	public ObservableList <CurrentListTrack> getCurrentList () {
		return currentList;
	}
	
	public void shuffleList() {
		Collections.shuffle( currentList );
	}

	public void removeTracksAtIndices ( List <Integer> indicies ) {
		for ( int k = indicies.size() - 1; k >= 0; k-- ) {
			currentList.remove ( indicies.get( k ).intValue() );
		}
		
	}

	public void seekRequested ( double percent ) {
		player.requestSeekPercent( percent );
	}

	public void setVolumePercent ( double percent ) {
		player.requestVolumePercent( percent );
	}
	
	public boolean isPaused() {
		if ( player == null ) return true;
		else return player.isPaused();
	}

	public Playlist getCurrentPlaylist () {
		return currentPlaylist;
	}

	public Track getCurrentTrack () {
		if ( player != null ) {
			return player.getTrack();
		} else {
			return null;
		}
	}

	public void seekMS ( long ms ) {	
		//TODO: 
	}
	
	public long getPositionMS() {
		return player.getPositionMS();
	}
	
	public EnumMap <Persister.Setting, ? extends Object> getSettings () {
		EnumMap <Persister.Setting, Object> retMe = new EnumMap <Persister.Setting, Object> ( Persister.Setting.class );
		
		if ( !player.isStopped() ) {
			retMe.put ( Setting.TRACK, getCurrentTrack().getPath().toString() );
			retMe.put ( Setting.TRACK_POSITION, getPositionMS() );
			retMe.put ( Setting.TRACK_NUMBER, getCurrentTrackNumber() );
		}

		retMe.put ( Setting.SHUFFLE, getShuffleMode().toString() );
		retMe.put ( Setting.REPEAT, getRepeatMode() );
		
		return retMe;
	}

	public void addListener ( PlayerListener listener ) {
		if ( listener != null ) {
			listeners.add( listener );
		} else {
			LOGGER.info( "Null player listener was attempted to be added, ignoring." );
		}
	}
	
	public void notifyPositionChanged ( int positionMS, int lengthMS ) {
		for ( PlayerListener listener : listeners ) {
			listener.playerPositionChanged( positionMS, lengthMS );
		}
	}
	
	public void notifyStopped ( Track track, boolean userRequested ) {
		for ( PlayerListener listener : listeners ) {
			listener.playerStopped( track, userRequested );
		}
	}
	public void notifyStarted ( Track track ) {
		for ( PlayerListener listener : listeners ) {
			listener.playerStarted( track );
		}
	}
	
	public void notifyPaused () {
		for ( PlayerListener listener : listeners ) {
			listener.playerPaused();
		}
	}
	
	public void notifyUnpaused () {
		for ( PlayerListener listener : listeners ) {
			listener.playerUnpaused( );
		}
	}
	
	public void notifyVolumeChanged ( double newVolumePercent ) {
		for ( PlayerListener listener : listeners ) {
			listener.playerVolumeChanged( newVolumePercent );
		}
	}
	
	public void notifyShuffleModeChanged ( ShuffleMode newMode ) {
		for ( PlayerListener listener : listeners ) {
			listener.playerShuffleModeChanged( newMode );
		}
	}
	
	public void notifyRepeatModeChanged ( RepeatMode newMode ) {
		for ( PlayerListener listener : listeners ) {
			listener.playerRepeatModeChanged( newMode );
		}
	}

	public void playerStopped ( boolean userRequested ) {
		if ( !userRequested ) {
			if ( repeatMode == RepeatMode.REPEAT_ONE_TRACK && player != null ) {
				playTrack ( player.getTrack() );
			} else {
				nextTrack();
			}
		}
	}

	public void playerPaused () {
		notifyPaused();
	}

	public void playerUnpaused () {
		notifyUnpaused();
	}

	public void volumeChanged ( double volumePercentRequested ) {
		notifyVolumeChanged ( volumePercentRequested );
	}

	public void playerStarted ( Track track ) {
		notifyStarted( track );
	}

	public void playerTrackPositionChanged ( int positionMS, int lengthMS ) {
		notifyPositionChanged ( positionMS, lengthMS );
	}
}






