package net.joshuad.hypnos.audio;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import net.joshuad.hypnos.CurrentList;
import net.joshuad.hypnos.CurrentListTrack;
import net.joshuad.hypnos.History;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Persister;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.PreviousStack;
import net.joshuad.hypnos.Queue;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.fxui.FXUI;
import net.joshuad.hypnos.lastfm.LastFM;
import net.joshuad.hypnos.Persister.Setting;

public class AudioSystem {
	
	private static final Logger LOGGER = Logger.getLogger( AudioSystem.class.getName() );
	
	public enum StopReason {
		TRACK_FINISHED,
		USER_REQUESTED,
		END_OF_CURRENT_LIST,
		EMPTY_LIST,
		WRITING_TO_TAG,
		UNABLE_TO_START_TRACK,
		ERROR
	}

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
	
	private Random randomGenerator = new Random();
	
	private int shuffleTracksPlayedCounter = 0;
	
	private final VLCAudioPlayer player;
	private final Queue queue;
	private final History history; 
	private final PreviousStack previousStack;
	private final CurrentList currentList;
	private final LastFM lastFM;
	
	private Double unmutedVolume = null;
	
	private final BooleanProperty lastFMDoScrobble = new SimpleBooleanProperty ( false );
	private boolean scrobbledThisTrack = false;
	private final DoubleProperty lastFMScrobbleTime = new SimpleDoubleProperty ( 0 );
	
	private FXUI ui;
	
	public AudioSystem () {
		player = new VLCAudioPlayer ( this );
		queue = new Queue();
		history = new History();
		previousStack = new PreviousStack();
		currentList = new CurrentList( this, queue );
		lastFM = new LastFM();
	}
	
	public void setUI ( FXUI ui ) {
		this.ui = ui;
	}
	
	public void start() { //TODO: DD
	//	player.start();
	}
	
	public void unpause () {
		player.requestUnpause();
	}
	
	public void pause () {
		player.requestPause();
	}
	
	public void togglePause () {
		player.requestTogglePause();
	}
	
	public void play () {
		switch ( player.getState() ) {
			case PAUSED:
				player.requestUnpause();
				break;
			case PLAYING:
				player.requestPlayTrack( player.getTrack(), false );
				break;
			case STOPPED:
				next( false );
				break;
		}
	}
	
	public void stop ( StopReason reason ) {
		Track stoppedTrack = player.getTrack();

		player.requestStop();
		
		for ( CurrentListTrack track : currentList.getItems() ) {
			track.setIsCurrentTrack( false );
		}

		notifyListenersStopped( stoppedTrack, reason ); 
		
		shuffleTracksPlayedCounter = 0;
	}
	
	public void releaseResources() {
		player.releaseResources();
	}
	
	public void previous ( ) {
		boolean startPaused = player.isPaused() || player.isStopped();
		previous ( startPaused );
	}
	
	public void previous ( boolean startPaused ) {
		
		if ( player.isPlaying() || player.isPaused() ) {
			
			if ( player.getPositionMS() >= 5000 ) {
				playTrack ( player.getTrack(), player.isPaused(), false );
				return;
			}
		}		
		
		Track previousTrack = null;

		int previousStackSize = previousStack.size();
		while ( !previousStack.isEmpty() && previousTrack == null ) {
			Track candidate = previousStack.removePreviousTrack( player.getTrack() );
								
			if ( currentList.getItems().contains( candidate ) ) {
				previousTrack = candidate;
			}
		}
		
		int previousStackSizeDifference = previousStackSize - previousStack.size();
		shuffleTracksPlayedCounter -= previousStackSizeDifference;
		
		if ( previousTrack != null ) {
			playTrack ( previousTrack, startPaused, true );
			
		} else if ( repeatMode == RepeatMode.PLAY_ONCE || repeatMode == RepeatMode.REPEAT_ONE_TRACK ) {
			shuffleTracksPlayedCounter = 1;
			Track previousTrackInList = null;
			for ( CurrentListTrack track : currentList.getItems() ) {
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
			for ( CurrentListTrack track : currentList.getItems() ) {
				if ( track.getIsCurrentTrack() ) {
					if ( previousTrackInList != null ) {
						playTrack( previousTrackInList, startPaused, false );
					} else {
						playTrack( currentList.getItems().get( currentList.getItems().size() - 1 ), startPaused, false );
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
		
		List<CurrentListTrack> items = currentList.getSortedItemsNoFilter();

		if ( queue.hasNext() ) {
			playTrack( queue.getNextTrack(), startPaused );
			
		} else if ( items.size() == 0 ) {
			stop ( StopReason.EMPTY_LIST );
			return;
			
		} else if ( repeatMode == RepeatMode.REPEAT_ONE_TRACK ) {
			playTrack ( history.getLastTrack() );

		} else if ( shuffleMode == ShuffleMode.SEQUENTIAL ) {
			ListIterator <CurrentListTrack> currentListIterator = items.listIterator();
			boolean didSomething = false;
			
			while ( currentListIterator.hasNext() ) {
				if ( currentListIterator.next().isLastCurrentListTrack() ) {
					if ( currentListIterator.hasNext() ) {
						playTrack( currentListIterator.next(), startPaused );
						didSomething = true;
						
					} else if ( repeatMode == RepeatMode.PLAY_ONCE ) {
						shuffleTracksPlayedCounter = 1;
						stop( StopReason.END_OF_CURRENT_LIST );
						didSomething = true;
					
					} else if ( items.size() <= 0 ) {
						stop( StopReason.EMPTY_LIST );
						didSomething = true;

					} else if ( repeatMode == RepeatMode.REPEAT && items.size() > 0 ) {
						playTrack( items.get( 0 ), startPaused );
						didSomething = true;
					}
					
					break;
				}
			}
			if ( !didSomething ) {
				if ( items.size() > 0 ) {
					playTrack ( items.get( 0 ), startPaused );
				}
			}
			
		} else if ( shuffleMode == ShuffleMode.SHUFFLE ) {
			if ( repeatMode == RepeatMode.REPEAT ) {
				
				shuffleTracksPlayedCounter = 1;
				// TODO: Ban the most recent X tracks from playing
				int currentListSize = items.size();
				int collisionWindowSize = currentListSize / 3; // TODO: Fine tune this amount
				int permittedRetries = 3; // TODO: fine tune this number
	
				boolean foundMatch = false;
				int retryCount = 0;
				Track playMe;
	
				List <Track> collisionWindow;
	
				if ( previousStack.size() >= collisionWindowSize ) {
					collisionWindow = previousStack.subList( 0, collisionWindowSize );
				} else {
					collisionWindow = previousStack.getData();
				}
	
				do {
					playMe = items.get( randomGenerator.nextInt( items.size() ) );
					if ( !collisionWindow.contains( playMe ) ) {
						foundMatch = true;
					} else {
						++retryCount;
					}
				} while ( !foundMatch && retryCount < permittedRetries );
	
				playTrack( playMe, startPaused );
				
			} else {
				if ( shuffleTracksPlayedCounter < items.size() ) {
					List <Track> alreadyPlayed = previousStack.subList( 0, shuffleTracksPlayedCounter );
					ArrayList <Track> viableTracks = new ArrayList <Track>( items );
					viableTracks.removeAll( alreadyPlayed );
					Track playMe = viableTracks.get( randomGenerator.nextInt( viableTracks.size() ) );
					playTrack( playMe, startPaused );
					++shuffleTracksPlayedCounter;
				} else {
					stop( StopReason.END_OF_CURRENT_LIST );
				}
			} 
		}
	}
		
	public int getCurrentTrackIndex() {
		for ( int k = 0 ; k < currentList.getItems().size(); k++ ) {
			if ( currentList.getItems().get( k ).getIsCurrentTrack() ) {
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
	
	public CurrentList getCurrentList() {
		return currentList;
	}
	
	public Track getCurrentTrack() {
		return player.getTrack();
	}

	public Playlist getCurrentPlaylist () {
		return currentList.getCurrentPlaylist();
	}
	
	public void shuffleList() {
		currentList.shuffleList( );
	}

	public void seekPercent ( double percent ) {
		player.requestSeekPercent( percent );
	}
	
	public void skipMS ( int diffMS ) {
		player.requestIncrementMS ( diffMS );
	}

	public void seekMS ( long ms ) {
		player.requestSeekMS( ms );
	}
	
	public long getPositionMS() {
		return player.getPositionMS();
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
	
	public boolean volumeChangeSupported() {
		return player.volumeChangeSupported();
	}
	
	
	public EnumMap <Persister.Setting, String> getSettings () {
		EnumMap <Persister.Setting, String> retMe = new EnumMap <Persister.Setting, String> ( Persister.Setting.class );
		
		if ( !player.isStopped() ) {
			if ( player.getTrack() != null ) {
				retMe.put ( Setting.TRACK, player.getTrack().getPath().toString() );
			}
			retMe.put ( Setting.TRACK_POSITION, Long.toString( player.getPositionMS() ) );
			retMe.put ( Setting.TRACK_NUMBER, Integer.toString ( getCurrentTrackIndex() ) );
		}

		retMe.put ( Setting.SHUFFLE, getShuffleMode().toString() );
		retMe.put ( Setting.REPEAT, getRepeatMode().toString() );
		retMe.put ( Setting.VOLUME, Double.toString( player.getVolumePercent() ) );
		
		retMe.put ( Setting.DEFAULT_SHUFFLE_TRACKS, currentList.getDefaultTrackShuffleMode().toString() );
		retMe.put ( Setting.DEFAULT_SHUFFLE_ALBUMS, currentList.getDefaultAlbumShuffleMode().toString() );
		retMe.put ( Setting.DEFAULT_SHUFFLE_PLAYLISTS, currentList.getDefaultPlaylistShuffleMode().toString() );

		retMe.put ( Setting.DEFAULT_REPEAT_TRACKS, currentList.getDefaultTrackRepeatMode().toString() );
		retMe.put ( Setting.DEFAULT_REPEAT_ALBUMS, currentList.getDefaultAlbumRepeatMode().toString() );
		retMe.put ( Setting.DEFAULT_REPEAT_PLAYLISTS, currentList.getDefaultPlaylistRepeatMode().toString() );
		
		retMe.put( Setting.LASTFM_USERNAME, lastFM.getUsername() );
		
		retMe.put( Setting.LASTFM_PASSWORD_MD5, lastFM.getPasswordMD5() );
		
		retMe.put( Setting.LASTFM_SCROBBLE_ON_PLAY, lastFMDoScrobble.getValue().toString() );
		
		retMe.put( Setting.LASTFM_SCROBBLE_TIME, this.lastFMScrobbleTime.getValue().toString() );
		
		
		return retMe;
	}
	
	@SuppressWarnings("incomplete-switch")
	public void applySettings ( EnumMap <Persister.Setting, String> settings ) {
		
		settings.forEach( ( setting, value )-> {
			try {
				switch ( setting ) {
				case TRACK:
					Path trackPath = Paths.get( value );
					Path albumPath = null;
					if ( Utils.isAlbumDirectory( trackPath.toAbsolutePath().getParent() ) ) {
						albumPath = trackPath.toAbsolutePath().getParent();
					}
					Track track = new Track ( trackPath, albumPath );
					ui.artSplitPane.setImages( track ); 
					playTrack( track, true );
					settings.remove ( setting );
					break;
					
				case TRACK_POSITION:
					seekMS( Long.parseLong( value ) );
					settings.remove ( setting );
					playerTrackPositionChanged ( player.getTrack(), (int)Long.parseLong( value ), player.getTrack().getLengthS() * 1000 );
					break;
					
				case SHUFFLE:
					setShuffleMode ( AudioSystem.ShuffleMode.valueOf( value ) );
					settings.remove ( setting );
					break;
					
				case REPEAT:
					setRepeatMode ( AudioSystem.RepeatMode.valueOf( value ) );
					settings.remove ( setting );
					break;
					
				case VOLUME:
					setVolumePercent( Double.valueOf ( value ) );
					Hypnos.getUI().playerVolumeChanged ( Double.valueOf ( value ) ); //TODO: this is kind of a hack. 
					settings.remove ( setting );
					break;
					
				case TRACK_NUMBER:
					try {
						int tracklistNumber = Integer.parseInt( value );
						if ( tracklistNumber != -1 ) {
							getCurrentList().getItems().get( tracklistNumber ).setIsCurrentTrack( true );
						}
					} catch ( Exception e ) {
						LOGGER.info( "Error loading current list track number: " + e.getMessage() );
					}
		
					settings.remove ( setting );
					break;
					
				case DEFAULT_REPEAT_ALBUMS:
					getCurrentList().setDefaultAlbumRepeatMode( CurrentList.DefaultRepeatMode.valueOf( value ) );
					settings.remove ( setting );
					break;
					
				case DEFAULT_REPEAT_PLAYLISTS:
					getCurrentList().setDefaultPlaylistRepeatMode( CurrentList.DefaultRepeatMode.valueOf( value ) );
					settings.remove ( setting );
					break;
					
				case DEFAULT_REPEAT_TRACKS:
					getCurrentList().setDefaultTrackRepeatMode( CurrentList.DefaultRepeatMode.valueOf( value ) );
					settings.remove ( setting );
					break;
					
				case DEFAULT_SHUFFLE_ALBUMS:
					getCurrentList().setDefaultAlbumShuffleMode( CurrentList.DefaultShuffleMode.valueOf( value ) );
					settings.remove ( setting );
					break;
					
				case DEFAULT_SHUFFLE_PLAYLISTS:
					getCurrentList().setDefaultPlaylistShuffleMode( CurrentList.DefaultShuffleMode.valueOf( value ) );
					settings.remove ( setting );
					break;
					
				case DEFAULT_SHUFFLE_TRACKS:
					getCurrentList().setDefaultTrackShuffleMode( CurrentList.DefaultShuffleMode.valueOf( value ) );
					settings.remove ( setting );
					break;
					
				case LASTFM_USERNAME:
					lastFM.setUsername ( value );
					settings.remove ( setting );
					break;
					
				case LASTFM_PASSWORD_MD5:
					lastFM.setPassword ( value );
					settings.remove ( setting );
					break;
					
				case LASTFM_SCROBBLE_ON_PLAY:
					this.lastFMDoScrobble.setValue( Boolean.valueOf( value ) );
					settings.remove ( setting );
					break;
					
				case LASTFM_SCROBBLE_TIME:
					this.lastFMScrobbleTime.setValue( Double.valueOf( value ) );
					settings.remove( setting );
					break;
				}
			} catch ( Exception e ) {
				LOGGER.log( Level.INFO, "Unable to apply setting: " + setting + " to UI.", e );
			}
		});
	}
	
	
//Manage Listeners

	public void addPlayerListener ( PlayerListener listener ) {
		if ( listener != null ) {
			playerListeners.add( listener );
		} else {
			LOGGER.info( "Null player listener was attempted to be added, ignoring." );
		}
	}
	
	private void notifyListenersPositionChanged ( Track track, int positionMS, int lengthMS ) {
		for ( PlayerListener listener : playerListeners ) {
			listener.playerPositionChanged( positionMS, lengthMS );
		}
		
		if ( !scrobbledThisTrack && lastFMDoScrobble.getValue() ) {
			if ( positionMS / (double)lengthMS >= this.lastFMScrobbleTime.get() ) {
				lastFM.scrobbleTrack( track );
				scrobbledThisTrack = true;
			}
		}
	}
	
	private void notifyListenersStopped ( Track track, StopReason reason ) {
		for ( PlayerListener listener : playerListeners ) {
			listener.playerStopped( track, reason );
		}
		
		if ( !scrobbledThisTrack && lastFMDoScrobble.getValue() && reason == StopReason.TRACK_FINISHED && track != null ) {
			lastFM.scrobbleTrack( track );
			scrobbledThisTrack = true;
		}
	}
	
	private void notifyListenersStarted ( Track track ) {
		scrobbledThisTrack = false;
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
	
	//REFACTOR: Make these a listener interface, and add this object as a listener to player? 	
	
	private int consecutiveFailedToStartCount = 0;
	void playerStopped ( Track track, StopReason reason ) { 
		if ( reason == StopReason.TRACK_FINISHED ) {
			next ( false );
			consecutiveFailedToStartCount = 0;
			
		} else if ( reason == StopReason.UNABLE_TO_START_TRACK ) {

			consecutiveFailedToStartCount++;
			
			if ( consecutiveFailedToStartCount <= currentList.getSortedItems().size() ) {
				next ( false );
			} 
		}
		
		notifyListenersStopped ( track, reason );
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

	void playerTrackPositionChanged ( Track track, int positionMS, int lengthMS ) {
		notifyListenersPositionChanged ( track, positionMS, lengthMS );
	}
	
	public void playTrack ( Track track ) {
		playTrack ( track, false );
	}
	
	public void playTrack ( Track track, boolean startPaused ) {
		playTrack ( track, startPaused, true );
	}
	                                                                 
	public void playTrack ( Track track, boolean startPaused, boolean addToPreviousNextStack ) {
				
		player.requestPlayTrack( track, startPaused );
		for ( CurrentListTrack listTrack : currentList.getItems() ) {
			listTrack.setIsCurrentTrack( false );
		}
		
		if ( track instanceof CurrentListTrack ) {
			for ( CurrentListTrack listTrack : currentList.getItems() ) {
				listTrack.setIsLastCurrentListTrack( false );
			}
			
			((CurrentListTrack)track).setIsCurrentTrack( true );
			((CurrentListTrack)track).setIsLastCurrentListTrack( true );
		}
		
		if ( addToPreviousNextStack ) {
			previousStack.addToStack ( track );
		}
		
		history.trackPlayed( track );
	}
	

	public void playItems ( List <Track> items ) {
		//REFACTOR: maybe break this into two separate functions and have the UI determine whether to set tracks or just play
		if ( items.size() == 1 ) {
			playTrack ( items.get( 0 ) );
		} else if ( items.size() > 1 ) {
			currentList.setTracks( items );
			next ( false );
		}
	}

	//Used after program loads to get everything linked back up properly. 
	public void linkQueueToCurrentList () {
		for ( CurrentListTrack track : currentList.getItems() ) {
			for ( int index : track.getQueueIndices() ) {
				if ( index < queue.size() ) {
					queue.getData().set( index - 1, track );
				} else {
					LOGGER.fine( "Current list had a queue index beyond the length of the queue. Removing." );
					track.getQueueIndices().remove( new Integer ( index ) );
				}
			}
		}
	}

	public void toggleMute () {
		if ( player.getVolumePercent() == 0 ) {
			if ( unmutedVolume != null ) {
				player.requestVolumePercent( unmutedVolume );
				unmutedVolume = null;
			} else { 
				player.requestVolumePercent( 1 );
			}
		} else {
			unmutedVolume = player.getVolumePercent();
			player.requestVolumePercent( 0 );
		}
	}

	public LastFM getLastFM () {
		return lastFM;
	}
	
	public BooleanProperty doLastFMScrobbleProperty() {
		return lastFMDoScrobble;
	}

	public boolean doLastFMScrobble() {
		return lastFMDoScrobble.getValue();
	}
	
	public void setScrobbleTime( double value ) {
		lastFMScrobbleTime.set( value );
	}

	public DoubleProperty scrobbleTimeProperty () {
		return lastFMScrobbleTime;
	}
}







