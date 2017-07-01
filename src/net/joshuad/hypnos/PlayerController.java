package net.joshuad.hypnos;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.joshuad.hypnos.audio.AbstractPlayer;

public class PlayerController {

	public enum ShuffleMode {
		SEQUENTIAL ( "⇉" ), SHUFFLE ( "🔀" );

		String symbol;

		ShuffleMode ( String symbol ) {
			this.symbol = symbol;
		}

		public String getSymbol () {
			return symbol;
		}
	}

	public enum RepeatMode {
		PLAY_ONCE ( "⇥" ), REPEAT ( "🔁" ), REPEAT_ONE_TRACK ( "🔂" );

		String symbol;

		RepeatMode ( String symbol ) {
			this.symbol = symbol;
		}

		public String getSymbol () {
			return symbol;
		}
	}


	private ShuffleMode shuffleMode = ShuffleMode.SEQUENTIAL;
	private RepeatMode repeatMode = RepeatMode.PLAY_ONCE;
	
	private static final int MAX_PREVIOUS_NEXT_STACK_SIZE = 10000;
	private static final int MAX_HISTORY_SIZE = 100;

	private final ObservableList <CurrentListTrack> currentList = FXCollections.observableArrayList(); 

	private final ArrayList <Track> previousNextStack = new ArrayList <Track>(MAX_PREVIOUS_NEXT_STACK_SIZE);
	private final ObservableList <Track> history = FXCollections.observableArrayList( new ArrayList <Track>(MAX_HISTORY_SIZE) );

	private AbstractPlayer currentPlayer;
	
	private Random randomGenerator = new Random();

	static Playlist currentPlaylist = null;
	
	private int playOnceShuffleTracksPlayedCounter = 1;

	// This is called by the various players
	public void songFinishedPlaying ( boolean userRequested ) {
		Platform.runLater( new Runnable() {
			public void run () {
				if ( !userRequested ) {
					if ( repeatMode == RepeatMode.REPEAT_ONE_TRACK && currentPlayer != null ) {
						playTrack ( currentPlayer.getTrack() );
					} else {
						nextTrack();
					}
				}
			}
		} );
	}
	
	public void previousTrack() {
		
		boolean isPaused = currentPlayer.isPaused();
		
		Track previousTrack = null;

		synchronized ( previousNextStack ) {
			while ( !previousNextStack.isEmpty() && previousTrack == null ) {
				Track candidate = previousNextStack.remove( 0 );
				if ( playOnceShuffleTracksPlayedCounter > 0 ) playOnceShuffleTracksPlayedCounter--;
				
				if ( currentPlayer != null && candidate.equals( currentPlayer.getTrack() ) ) {
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
		if ( currentPlayer != null && !currentPlayer.isPaused() ) {
			pause();

		} else {
			play();
		}
	}
	
	
	public void play() {
		if ( currentPlayer != null && currentPlayer.isPaused() ) {
			currentPlayer.play();
			
		} else if ( Hypnos.queue.hasNext() ) {
			playTrack ( Hypnos.queue.getNextTrack() );
		
		} else {
			Track selectedTrack = Hypnos.ui.getSelectedTrack();

			if ( selectedTrack != null ) {
				playTrack( selectedTrack );

			} else if ( !currentList.isEmpty() ) {
				selectedTrack = currentList.get( 0 );
				playTrack( selectedTrack );
			}
		}
	}
	
	public void pause() {
		if ( currentPlayer != null ) {
			currentPlayer.pause();
		}
	}
		
	public void nextTrack () {
		//TODO: Handle what we do when isStopped()
		if ( Hypnos.queue.hasNext() ) {
			playTrack( Hypnos.queue.getNextTrack() );
			
		} else if ( shuffleMode == ShuffleMode.SEQUENTIAL ) {
			ListIterator <CurrentListTrack> iterator = currentList.listIterator();
			boolean didSomething = false;
			boolean currentlyPaused = currentPlayer.isPaused();
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
		if ( currentPlayer != null ) {
			currentPlayer.stop();
		}
		
		currentPlayer = AbstractPlayer.getPlayer( track, this, Hypnos.ui.trackPositionSlider, startPaused );
		
		if ( currentPlayer == null ) return;
		
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
		
		if ( history.size() == 0 || !history.get( 0 ).equals( track ) ) {
			while ( history.size() >= MAX_HISTORY_SIZE ) {
				history.remove( history.size() - 1 );
			}
			
			history.add( 0, track );
		}
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
	
	public void loadTracks ( List <CurrentListTrack> tracks ) {
		loadTracks ( tracks, false );
	}

	public void loadTracks ( List <CurrentListTrack> tracks, boolean startPaused ) {
		currentList.clear();
		currentList.addAll( tracks );
		if ( !currentList.isEmpty() ) {
			playTrack( currentList.get( 0 ), startPaused );
		}
		currentPlaylist = null;
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
		if ( currentPlayer != null ) {
			Track track = currentPlayer.getTrack();
			if ( track instanceof CurrentListTrack ) ((CurrentListTrack)track).setIsCurrentTrack( false );
			currentPlayer.stop();
			currentPlayer = null;
		}
		
		playOnceShuffleTracksPlayedCounter = 0;
	}
	
	public int getCurrentTrackNumber() {
		for ( int k = 0 ; k < currentList.size(); k++ ) {
			if ( currentList.get( k ).isCurrentTrack ) {
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

	public ObservableList <Track> getHistory () {
		return history;
	}

	public void addAll ( int index, ArrayList <CurrentListTrack> tracks ) { //TODO: rename addTracks
		int boundedIndex = Math.min( index, currentList.size() );
		currentList.addAll( boundedIndex, tracks );
	}

	public boolean isStopped () {
		return currentPlayer == null;
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
		if ( currentPlayer != null ) {
			currentPlayer.seekPercent( percent );
		}
	}

	public void setVolumePercent ( double percent ) {
		//TODO: Does this persist to the next track? Pretty sure not. Make sure it does
		if ( currentPlayer != null ) {
			currentPlayer.setVolumePercent( percent );
		}
	}
	
	public boolean isPaused() {
		if ( currentPlayer == null ) return false;
		else return currentPlayer.isPaused();
	}

	public Playlist getCurrentPlaylist () {
		return currentPlaylist;
	}

	public Track getCurrentTrack () {
		if ( currentPlayer != null ) {
			return currentPlayer.getTrack();
		} else {
			return null;
		}
	}

	public void seekMS ( long ms ) {	
		if ( currentPlayer != null ) {
			currentPlayer.seekMS( ms );
		}
	}

	public void addToHistory ( ArrayList <Track> tracks ) {
		history.addAll ( tracks );
	}
}
