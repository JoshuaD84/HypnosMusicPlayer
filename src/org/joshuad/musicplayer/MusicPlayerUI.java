package org.joshuad.musicplayer;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.joshuad.musicplayer.players.AACPlayer;
import org.joshuad.musicplayer.players.AbstractPlayer;
import org.joshuad.musicplayer.players.MP3Player;
import org.joshuad.musicplayer.players.FlacPlayer;
import org.joshuad.musicplayer.players.OggPlayer;
import org.joshuad.musicplayer.players.WavPlayer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

@SuppressWarnings({ "rawtypes", "unchecked" }) // TODO: Maybe get rid of this when I understand things better
public class MusicPlayerUI extends Application {

	private static final int MAX_TRACK_HISTORY = 10000;

	// private static final String SYMBOL_REPEAT_ONE_TRACK = "🔂";

	private static final DataFormat DRAGGED_TRACKS = new DataFormat( "application/x-java-track" );
	private static final DataFormat DRAGGED_TRACK_INDICES = new DataFormat( "application/x-java-track-indices" );
	private static final DataFormat DRAGGED_ALBUM_INDICES = new DataFormat( "application/x-java-album-indices" );
	private static final DataFormat DRAGGED_PLAYLIST_INDEX = new DataFormat( "application/x-java-playlist-index" );

	public static final String PROGRAM_NAME = "Hypnos Music Player";

	final static ObservableList <CurrentListTrack> currentListData = FXCollections.observableArrayList(); //TODO: rename to currentList
	
	static TableView <Album> albumTable;
	static TableView <Playlist> playlistTable;
	static TableView <Track> trackTable;
	static TableView <CurrentListTrack> currentListTable;
	static TableView <Path> musicSourceTable;
	static TableView <Track> queueTable;
	
	static ArrayList <Track> recentlyPlayedTracks = new ArrayList( MAX_TRACK_HISTORY );

	static BorderPane albumImage;
	static BorderPane artistImage;

	static HBox albumFilterPane;
	static HBox trackFilterPane;
	static HBox playlistFilterPane;
	static HBox playlistControls;

	static Slider trackPositionSlider;

	static boolean sliderMouseHeld;

	static VBox transport;

	static AbstractPlayer currentPlayer;

	static Label timeElapsedLabel = new Label( "" );
	static Label timeRemainingLabel = new Label( "" );
	static Label currentPlayingListInfo = new Label( "" );
	static Label trackInfo = new Label( "" );

	static Stage mainStage;
	static Stage libraryWindow;
	static Stage queueWindow;
	static TagWindow tagWindow;

	static Button togglePlayButton;
	static Button toggleRepeatButton;
	static Button toggleShuffleButton;

	static SplitPane artSplitPane;

	static Random randomGenerator = new Random();

	static int playOnceShuffleTracksPlayedCounter = 1;

	static Playlist currentPlaylist = null;

	static boolean playlistChanged = false;
	
	static CheckBox trackListCheckBox;
	static TextField trackFilterBox;

	static ShuffleMode shuffleMode = ShuffleMode.SEQUENTIAL;

	enum ShuffleMode {
		SEQUENTIAL ( "⇉" ), SHUFFLE ( "🔀" );

		String symbol;

		ShuffleMode ( String symbol ) {
			this.symbol = symbol;
		}

		public String getSymbol () {
			return symbol;
		}
	}

	static RepeatMode repeatMode = RepeatMode.PLAY_ONCE;
	

	enum RepeatMode {
		PLAY_ONCE ( "⇥" ), REPEAT ( "🔁" );

		String symbol;

		RepeatMode ( String symbol ) {
			this.symbol = symbol;
		}

		public String getSymbol () {
			return symbol;
		}
	}
	
	public static void updateTransport ( int timeElapsed, int timeRemaining, double percent ) {
		Platform.runLater( new Runnable() {
			public void run () {
				if ( !trackPositionSlider.isValueChanging() && !sliderMouseHeld ) {
					trackPositionSlider.setValue(
							(trackPositionSlider.getMax() - trackPositionSlider.getMin()) * percent
									+ trackPositionSlider.getMin() );
				}
				timeElapsedLabel.setText( Utils.getLengthDisplay( timeElapsed ) );
				timeRemainingLabel.setText( Utils.getLengthDisplay( timeRemaining ) );
			}
		} );
	}

	// This is called by the various players
	public static void songFinishedPlaying ( boolean userRequested ) {
		Platform.runLater( new Runnable() {
			public void run () {
				if ( !userRequested ) {
					playNextTrack();
				}
			}
		} );
	}
	
	public static void playPreviousTrack() {

		if ( currentPlayer == null ) {
			currentListTable.getSelectionModel().clearAndSelect( currentListTable.getSelectionModel().getSelectedIndex() - 1 );
			return;
		}
		
		boolean isPaused = currentPlayer.isPaused();
		

		Track previousTrack = null;
		
		while ( !recentlyPlayedTracks.isEmpty() && previousTrack == null ) {
			Track candidate;
			synchronized ( recentlyPlayedTracks ) {
				candidate = recentlyPlayedTracks.remove( 0 );
				if ( playOnceShuffleTracksPlayedCounter > 0 ) playOnceShuffleTracksPlayedCounter--;
			}
			
			if ( currentListData.contains( candidate ) ) {
				previousTrack = candidate;
			}
		}
		
		if ( previousTrack != null ) {
			playTrack ( previousTrack, isPaused, false );
			
		} else if ( repeatMode == RepeatMode.PLAY_ONCE ) {
			playOnceShuffleTracksPlayedCounter = 1;
			Track previousTrackInList = null;
			for ( CurrentListTrack track : currentListData ) {
				if ( track.getIsCurrentTrack() ) {
					if ( previousTrackInList != null ) {
						playTrack( previousTrackInList, isPaused );
					} else {
						playTrack( track, isPaused );
					}
					break;
				} else {
					previousTrackInList = track;
				}
			}
		} else if ( repeatMode == RepeatMode.REPEAT ) {
			Track previousTrackInList = null;
			for ( CurrentListTrack track : currentListData ) {
				if ( track.getIsCurrentTrack() ) {
					if ( previousTrackInList != null ) {
						playTrack( previousTrackInList, isPaused );
					} else {
						playTrack( currentListData.get( currentListData.size() - 1 ), isPaused );
					}
					break;
				} else {
					previousTrackInList = track;
				}
			}
		}
	}
	
	public static void togglePause() {
		if ( currentPlayer != null && !currentPlayer.isPaused() ) {
			pause();

		} else {
			play();
		}
	}
	
	public static void play() {
		if ( currentPlayer != null && currentPlayer.isPaused() ) {
			currentPlayer.play();
			togglePlayButton.setText( "𝍪" );
			
		} else if ( Queue.hasNext() ) {
			playTrack ( Queue.getNextTrack() );
		
		} else {
			Track selectedTrack = currentListTable.getSelectionModel().getSelectedItem();

			if ( selectedTrack != null ) {
				playTrack( selectedTrack );

			} else if ( !currentListTable.getItems().isEmpty() ) {
				selectedTrack = currentListTable.getItems().get( 0 );
				playTrack( selectedTrack );
			}
		}
	}
		
	
	public static void pause() {
		if ( currentPlayer != null ) {
			currentPlayer.pause();
			togglePlayButton.setText( "▶" );
		}
	}
		
	public static void playNextTrack () {
		if ( currentPlayer == null ) {
			currentListTable.getSelectionModel().clearAndSelect( currentListTable.getSelectionModel().getSelectedIndex() + 1 );
		} else {
			if ( Queue.hasNext() ) {
				playTrack( Queue.getNextTrack() );
				
			} else if ( shuffleMode == ShuffleMode.SEQUENTIAL ) {
				ListIterator <CurrentListTrack> iterator = currentListData.listIterator();
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
						} else if ( repeatMode == RepeatMode.REPEAT && currentListData.size() > 0 ) {
							playTrack( currentListData.get( 0 ), currentlyPaused );
							didSomething = true;
						} else {
							stopTrack();
							didSomething = true;
						} 
						break;
					}
				}
				if ( !didSomething ) {
					if ( currentListData.size() > 0 ) {
						playTrack ( currentListData.get( 0 ), currentlyPaused );
					}
				}
				
			} else if ( shuffleMode == ShuffleMode.SHUFFLE && repeatMode == RepeatMode.PLAY_ONCE ) {

				if ( playOnceShuffleTracksPlayedCounter < currentListData.size() ) {
					List <Track> alreadyPlayed = recentlyPlayedTracks.subList( 0, playOnceShuffleTracksPlayedCounter );
					ArrayList <Track> viableTracks = new ArrayList <Track>( currentListData );
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
				int currentListSize = currentListData.size();
				int collisionWindowSize = currentListSize / 3; // TODO: Fine tune this amount
				int permittedRetries = 3; // TODO: fine tune this number

				boolean foundMatch = false;
				int retryCount = 0;
				Track playMe;

				List <Track> collisionWindow;

				if ( recentlyPlayedTracks.size() >= collisionWindowSize ) {
					collisionWindow = recentlyPlayedTracks.subList( 0,
							collisionWindowSize );
				} else {
					collisionWindow = recentlyPlayedTracks;
				}

				do {
					playMe = currentListData.get( randomGenerator.nextInt( currentListData.size() ) );
					if ( !collisionWindow.contains( playMe ) ) {
						foundMatch = true;
					} else {
						++retryCount;
					}
				} while ( !foundMatch && retryCount < permittedRetries );

				playTrack( playMe );
			}
		}
	}
	
	private static void playTrack ( Track track ) {
		playTrack ( track, false );
	}
	
	private static void playTrack ( Track track, boolean startPaused ) {
		playTrack ( track, startPaused, true );
	}
	                                                                 
	private static void playTrack ( Track track, boolean startPaused, boolean addToHistory ) {
		if ( currentPlayer != null ) {
			currentPlayer.stop();
			if ( currentPlayer.getTrack() instanceof CurrentListTrack ) {
				((CurrentListTrack)currentPlayer.getTrack()).setIsCurrentTrack( false );
			}
			togglePlayButton.setText( "▶" );
		}

		switch ( track.getFormat() ) {
			case FLAC:
				currentPlayer = new FlacPlayer( track, trackPositionSlider, startPaused );
				if ( track instanceof CurrentListTrack ) ((CurrentListTrack)track).setIsCurrentTrack( true );
				if ( startPaused ) {
					togglePlayButton.setText( "▶" );
				} else {
					togglePlayButton.setText( "𝍪" );
				}
				break;
			case MP3:
				currentPlayer = new MP3Player( track, trackPositionSlider, startPaused );
				if ( track instanceof CurrentListTrack ) ((CurrentListTrack)track).setIsCurrentTrack( true );
				if ( startPaused ) {
					togglePlayButton.setText( "▶" );
				} else {
					togglePlayButton.setText( "𝍪" );
				}
				break;
			case AAC:
				currentPlayer = new AACPlayer( track, trackPositionSlider, startPaused );
				if ( track instanceof CurrentListTrack ) ((CurrentListTrack)track).setIsCurrentTrack( true );
				if ( startPaused ) {
					togglePlayButton.setText( "▶" );
				} else {
					togglePlayButton.setText( "𝍪" );
				}
				break;
			case OGG:
				currentPlayer = new OggPlayer( track, trackPositionSlider, startPaused );
				if ( track instanceof CurrentListTrack ) ((CurrentListTrack)track).setIsCurrentTrack( true );
				if ( startPaused ) {
					togglePlayButton.setText( "▶" );
				} else {
					togglePlayButton.setText( "𝍪" );
				}
				break;
			case WAV:
				currentPlayer = new WavPlayer ( track, trackPositionSlider, startPaused );
				if ( track instanceof CurrentListTrack ) ((CurrentListTrack)track).setIsCurrentTrack( true );
				if ( startPaused ) {
					togglePlayButton.setText( "▶" );
				} else {
					togglePlayButton.setText( "𝍪" );
				}
				break;
			case UNKNOWN:
			default:
				System.out.println ( "Unknown music file type" );
				return;
		}


		if ( addToHistory ) {
			while ( recentlyPlayedTracks.size() >= MAX_TRACK_HISTORY ) {
				recentlyPlayedTracks.remove( recentlyPlayedTracks.size() - 1 );
			}
			
			recentlyPlayedTracks.add( 0, track );
		}

		currentListTable.refresh();

		StackPane thumb = (StackPane) trackPositionSlider.lookup( ".thumb" );
		thumb.setVisible( true );

		trackInfo.setText( track.getArtist() + " - " + track.getYear() + " - " + track.getAlbum()
				+ " - " + track.getTrackNumber() + " - " + track.getTitle() );

		setAlbumImage( Utils.getAlbumCoverImagePath( track ) );
		setArtistImage( Utils.getAlbumArtistImagePath( track ) );
	}

	public static void playAlbum ( Album album ) {
		currentPlaylist = null;
		currentListData.clear();
		currentListData.addAll( Utils.convertTrackList( album.getTracks() ) );
		Track firstTrack = currentListData.get( 0 );
		if ( firstTrack != null ) {
			playTrack( firstTrack );
		}

		playlistChanged = false;
		currentPlayingListInfo.setText( "Album: " + album.getAlbumArtist() + " - " + album.getYear() + " - " + album.getTitle() );
	}
	
	public static void loadTrack ( Track track ) {
		ArrayList loadMe = new ArrayList <Track> ( 1 );
		loadMe.add ( track );
		loadTracks ( loadMe );
	}

	public static void loadTracks ( ArrayList <Track> tracks ) {
		currentListTable.getItems().clear();
		currentListTable.getItems().addAll( Utils.convertTrackList( tracks ) );
		currentPlayingListInfo.setText( "Playlist: New" );
		if ( !currentListTable.getItems().isEmpty() ) {
			playTrack( currentListTable.getItems().get( 0 ) );
		}
		currentPlaylist = null;
	}

	public static void playPlaylist ( Playlist playlist ) {

		stopTrack();
		currentListData.clear();
		currentListData.addAll( Utils.convertTrackList( playlist.getTracks() ) );
		if ( !currentListData.isEmpty() ) {
			Track firstTrack = currentListData.get( 0 );
			if ( firstTrack != null ) {
				playTrack( firstTrack );
			}

		}
		currentPlaylist = playlist;

		currentPlayingListInfo.setText( "Playlist: " + playlist.getName() );
		playlistChanged = false;
	}

	public static void appendAlbum ( Album album ) {
		currentListData.addAll( Utils.convertTrackList( album.getTracks() ) );
	}

	public static void stopTrack () {
		if ( currentPlayer != null ) {
			Track track = currentPlayer.getTrack();
			if ( track instanceof CurrentListTrack ) ((CurrentListTrack)track).setIsCurrentTrack( false );
			currentPlayer.stop();
			currentPlayer = null;
			currentListTable.refresh();
		}
		
		playOnceShuffleTracksPlayedCounter = 0;
		
		togglePlayButton.setText( "▶" );

		trackPositionSlider.setValue( 0 );
		timeElapsedLabel.setText( "" );
		timeRemainingLabel.setText( "" );
		trackInfo.setText( "" );

		StackPane thumb = (StackPane) trackPositionSlider.lookup( ".thumb" );
		thumb.setVisible( false );

	}

	@Override
	public void start ( Stage stage ) {
		
		long startTime = System.currentTimeMillis();
		
		mainStage = stage;
		Scene scene = new Scene( new Group(), 1024, 768 );
		mainStage.getIcons().add(new Image("file:icon.png"));

		System.out.println ( "Setup Stage: " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();

		setupAlbumTable();
		
		System.out.println ( "Setup Album Table: " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();
		
		setupTrackListCheckBox();
		
		System.out.println ( "Track list Check Box: " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();
		
		setupAlbumFilterPane();
		
		System.out.println ( "Setup Album Filter Paine: " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();
		
		setupTrackFilterPane();
		
		System.out.println ( "Setup Track Filter Pane: " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();
		
		setupQueueWindow();
		System.out.println ( "Setup Queue Window: " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();
		
		setupPlaylistFilterPane();
		
		System.out.println ( "Setup Playlist Filter: " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();
		
		setupCurrentListTable();
		
		System.out.println ( "Setup Current List: " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();
		
		setupPlaylistTable();
		
		System.out.println ( "Setup Playlist Table: " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();
		
		setupCurrentListControlPane();
		
		System.out.println ( "Setup Current List Controllers " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();
		
		setupTrackTable();

		System.out.println ( "Setup track table: " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();
		
		setupAlbumImage();
		setupArtistImage();
		
		System.out.println ( "Setup image sections: " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();
		
		setupTransport();
		
		System.out.println ( "Setup Transport: " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();
		
		setupLibraryWindow();
		tagWindow = new TagWindow ( mainStage );
		
		System.out.println ( "Setup Library Window: " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();

		artSplitPane = new SplitPane();
		artSplitPane.getItems().addAll( albumImage, artistImage );

		BorderPane currentPlayingPane = new BorderPane();
		playlistControls.prefWidthProperty().bind( currentPlayingPane.widthProperty() );
		currentPlayingPane.setTop( playlistControls );
		currentPlayingPane.setCenter( currentListTable );

		SplitPane playingArtSplitPane = new SplitPane();
		playingArtSplitPane.setOrientation( Orientation.VERTICAL );
		playingArtSplitPane.getItems().addAll( currentPlayingPane, artSplitPane );

		BorderPane albumListPane = new BorderPane();
		albumFilterPane.prefWidthProperty().bind( albumListPane.widthProperty() );
		albumListPane.setTop( albumFilterPane );
		albumListPane.setCenter( albumTable );
		
		BorderPane trackListPane = new BorderPane();
		trackFilterPane.prefWidthProperty().bind( trackListPane.widthProperty() );
		trackListPane.setTop( trackFilterPane );
		trackListPane.setCenter( trackTable );

		BorderPane playlistPane = new BorderPane();
		playlistFilterPane.prefWidthProperty().bind( playlistPane.widthProperty() );
		playlistPane.setTop( playlistFilterPane );
		playlistPane.setCenter( playlistTable );

		StretchedTabPane leftTabPane = new StretchedTabPane(); // TODO: I can probably name this better.

		Tab albumListTab = new Tab( "Albums" );
		albumListTab.setContent( albumListPane );
		albumListTab.setClosable( false );

		Tab playlistTab = new Tab( "Playlists" );
		playlistTab.setContent( playlistPane );
		playlistTab.setClosable( false );

		Tab songListTab = new Tab( "Tracks" );
		songListTab.setContent( trackListPane );
		songListTab.setClosable( false );

		leftTabPane.getTabs().addAll( albumListTab, songListTab, playlistTab );
		leftTabPane.setSide( Side.BOTTOM );

		SplitPane primarySplitPane = new SplitPane();
		primarySplitPane.getItems().addAll( leftTabPane, playingArtSplitPane );

		final BorderPane primaryContainer = new BorderPane();

		System.out.println ( "Setup highlevel panels: " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();

		primaryContainer.prefWidthProperty().bind( scene.widthProperty() );
		primaryContainer.prefHeightProperty().bind( scene.heightProperty() );
		primaryContainer.setPadding( new Insets( 0 ) ); // TODO:
		primaryContainer.setCenter( primarySplitPane );
		primaryContainer.setTop( transport );

		stage.setTitle( PROGRAM_NAME );

		System.out.println ( "bind stuff " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();
		
		((Group) scene.getRoot()).getChildren().addAll( primaryContainer );
		stage.setScene( scene );
		stage.show();
		
		System.out.println ( "set scene " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();

		// This stuff has to be done after setScene
		StackPane thumb = (StackPane) trackPositionSlider.lookup( ".thumb" );
		thumb.setVisible( false );

		primarySplitPane.setDividerPositions( .35d );
		playingArtSplitPane.setDividerPositions( .65d );
		artSplitPane.setDividerPosition( 0, .51d ); // For some reason .5 doesn't work...

		double width = togglePlayButton.getWidth();
		double height = togglePlayButton.getHeight();

		togglePlayButton.setMaxWidth( width );
		togglePlayButton.setMinWidth( width );
		togglePlayButton.setMaxHeight( height );
		togglePlayButton.setMinHeight( height );

		// TODO: This is such a crappy hack
		final ChangeListener <Number> listener = new ChangeListener <Number>() {
			final Timer timer = new Timer();

			TimerTask task = null;

			final long delayTime = 500;

			@Override
			public void changed ( ObservableValue <? extends Number> observable, Number oldValue,
					final Number newValue ) {
				if ( task != null ) {
					task.cancel();
				}

				task = new TimerTask() {
					@Override
					public void run () {
						SplitPane.setResizableWithParent( artSplitPane, false );
					}
				};
				timer.schedule( task, delayTime );
			}
		};

		stage.widthProperty().addListener( listener );
		stage.heightProperty().addListener( listener );
		
		
		System.out.println ( "Misc UI Adjustments " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();	
		
		
		Persister.loadData();
		System.out.println ( "Load Data " + ( System.currentTimeMillis() - startTime ) );
		
		Library.startLoader();
		
		System.out.println ( "Start Library Loader " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();
	}
	
	private void updatePlaylistMenuItems ( ObservableList <MenuItem> items,
			EventHandler eventHandler ) {

		items.remove( 1, items.size() );

		for ( Playlist playlist : Library.playlistsSorted ) {
			MenuItem newItem = new MenuItem( playlist.getName() );
			newItem.setUserData( playlist );
			newItem.setOnAction( eventHandler );
			items.add( newItem );
		}
	}

	public void setupTransport () {

		Button previousButton = new Button( "⏪" );
		togglePlayButton = new Button( "▶" );
		Button stopButton = new Button( "◼" );
		Button nextButton = new Button( "⏩" );

		int fontSize = 22;

		previousButton.setStyle( "-fx-font-size: " + fontSize + "px" );
		togglePlayButton.setStyle( "-fx-font-size: " + fontSize + "px" );
		stopButton.setStyle( "-fx-font-size: " + fontSize + "px" );
		nextButton.setStyle( "-fx-font-size: " + fontSize + "px" );

		Insets buttonInsets = new Insets( 3, 12, 6, 12 );
		previousButton.setPadding( buttonInsets );
		togglePlayButton.setPadding( buttonInsets );
		stopButton.setPadding( buttonInsets );
		nextButton.setPadding( buttonInsets );

		previousButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				playPreviousTrack();
			}
		} );

		nextButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				playNextTrack();
			}
		} );

		stopButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				if ( currentPlayer != null ) {
					stopTrack();
				}
			}
		} );

		togglePlayButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				togglePause();
			}
		} );

		timeElapsedLabel = new Label( "" );
		timeRemainingLabel = new Label( "" );

		timeElapsedLabel.setMinWidth( 40 );
		timeElapsedLabel.setStyle( "" );
		timeElapsedLabel.setAlignment( Pos.CENTER_RIGHT );

		timeRemainingLabel.setMinWidth( 40 );

		trackPositionSlider = new Slider();
		trackPositionSlider.setMin( 0 );
		trackPositionSlider.setMax( 1000 );
		trackPositionSlider.setValue( 0 );
		trackPositionSlider.setMaxWidth( 600 );
		trackPositionSlider.setMinWidth( 200 );
		trackPositionSlider.setPrefWidth( 400 );

		trackPositionSlider.valueChangingProperty().addListener( new ChangeListener <Boolean>() {
			public void changed ( ObservableValue <? extends Boolean> obs, Boolean wasChanging,
					Boolean isNowChanging ) {
				if ( !isNowChanging ) {
					if ( currentPlayer != null ) {
						currentPlayer.seek(
								trackPositionSlider.getValue() / trackPositionSlider.getMax() );
					}
				}
			}
		} );

		trackPositionSlider.setOnMousePressed( ( MouseEvent e ) -> {
			sliderMouseHeld = true;
		} );

		trackPositionSlider.setOnMouseReleased( ( MouseEvent e ) -> {
			sliderMouseHeld = false;
			if ( currentPlayer != null ) {
				currentPlayer.seek( trackPositionSlider.getValue() / trackPositionSlider.getMax() );
			}
		} );

		HBox sliderPane = new HBox();
		sliderPane.getChildren().addAll( timeElapsedLabel, trackPositionSlider,
				timeRemainingLabel );
		sliderPane.setAlignment( Pos.CENTER );
		sliderPane.setSpacing( 5 );

		HBox trackControls = new HBox();
		trackControls.getChildren().addAll( previousButton, togglePlayButton, stopButton,
				nextButton );
		trackControls.setPadding( new Insets( 5 ) );
		trackControls.setSpacing( 5 );

		HBox controls = new HBox();
		controls.getChildren().addAll( trackControls, sliderPane );
		controls.setSpacing( 10 );
		controls.setAlignment( Pos.CENTER );

		HBox playingTrackInfo = new HBox();
		trackInfo = new Label( "" );
		trackInfo.setStyle( "-fx-font-weight: bold; -fx-font-size: 16" );
		playingTrackInfo.getChildren().add( trackInfo );
		playingTrackInfo.setAlignment( Pos.CENTER );

		transport = new VBox();
		transport.getChildren().add( playingTrackInfo );
		transport.getChildren().add( controls );
		transport.setPadding( new Insets( 10, 0, 10, 0 ) );
		transport.setSpacing( 5 );
	}
	
	
	public void setupQueueWindow () {
		queueWindow = new Stage();
		queueWindow.initModality( Modality.NONE );
		queueWindow.initOwner( mainStage );
		queueWindow.setTitle( "Queue" );
		queueWindow.setWidth( 500 );
		queueWindow.setHeight ( 400 );
		Group root = new Group();
		Scene scene = new Scene( root );
		VBox primaryPane = new VBox();

		queueTable = new TableView();
		Label emptyLabel = new Label( "Queue is empty." );
		emptyLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyLabel.setWrapText( true );
		emptyLabel.setTextAlignment( TextAlignment.CENTER );

		queueTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		queueTable.setPlaceholder( emptyLabel );
		queueTable.setItems( Queue.getData() );
		queueTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		queueTable.widthProperty().addListener( new ChangeListener <Number>() {
			@Override
			public void changed ( ObservableValue <? extends Number> source, Number oldWidth, Number newWidth ) {
				Pane header = (Pane) queueTable.lookup( "TableHeaderRow" );
				if ( header.isVisible() ) {
					header.setMaxHeight( 0 );
					header.setMinHeight( 0 );
					header.setPrefHeight( 0 );
					header.setVisible( false );
				}
			}
		});
		
		queueTable.setOnKeyPressed( new EventHandler <KeyEvent>() {
			@Override
			public void handle ( final KeyEvent keyEvent ) {
				if ( keyEvent.getCode().equals( KeyCode.DELETE ) ) {
					ObservableList <Integer> indexes = queueTable.getSelectionModel().getSelectedIndices();
					for ( int index : indexes ) { //TODO: removeAll
						Queue.remove( index );
					}
				}
			}
		});

		TableColumn numberColumn = new TableColumn ( "#" );
		TableColumn artistColumn = new TableColumn( "Artist" );
		TableColumn titleColumn = new TableColumn( "Title" );
		
		numberColumn.setMaxWidth( 10000 );
		artistColumn.setMaxWidth( 45000 );
		titleColumn.setMaxWidth ( 45000 );
		
		numberColumn.setCellValueFactory( new Callback <CellDataFeatures <Track, Track>, ObservableValue <Track>>() {
			@Override
			public ObservableValue <Track> call ( CellDataFeatures <Track, Track> p ) {
				return new ReadOnlyObjectWrapper( p.getValue() );
			}
		});

		numberColumn.setCellFactory( new Callback <TableColumn <Track, Track>, TableCell <Track, Track>>() {
			@Override
			public TableCell <Track, Track> call ( TableColumn <Track, Track> param ) {
				return new TableCell <Track, Track>() {
					@Override
					protected void updateItem ( Track item, boolean empty ) {
						super.updateItem( item, empty );

						if ( this.getTableRow() != null && item != null ) {
							setText( this.getTableRow().getIndex() + 1 + "" );
						} else {
							setText( "" );
						}
					}
				};
			}
		});
		numberColumn.setSortable(false);
		
		
		artistColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "Artist" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "Title" ) );
		
		queueTable.getColumns().addAll( numberColumn, artistColumn, titleColumn );

		queueTable.prefWidthProperty().bind( queueWindow.widthProperty() );
		queueTable.prefHeightProperty().bind( queueWindow.heightProperty() );
		
		primaryPane.getChildren().addAll( queueTable );
		root.getChildren().add( primaryPane );
		queueWindow.setScene( scene );
	}
	

	public void setupLibraryWindow () {
		libraryWindow = new Stage();
		libraryWindow.initModality( Modality.NONE );
		libraryWindow.initOwner( mainStage );
		libraryWindow.setTitle( "Music Search Locations" );
		libraryWindow.setWidth( 350 );
		Group root = new Group();
		Scene scene = new Scene( root );
		VBox primaryPane = new VBox();

		musicSourceTable = new TableView();
		Label emptyLabel = new Label( "No directories in your library. Either '+ Add' or drop directories here." );
		emptyLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyLabel.setWrapText( true );
		emptyLabel.setTextAlignment( TextAlignment.CENTER );

		musicSourceTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		musicSourceTable.setPlaceholder( emptyLabel );
		musicSourceTable.setItems( Library.musicSourcePaths );
		musicSourceTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		musicSourceTable.widthProperty().addListener( new ChangeListener <Number>() {
			@Override
			public void changed ( ObservableValue <? extends Number> source, Number oldWidth, Number newWidth ) {
				Pane header = (Pane) musicSourceTable.lookup( "TableHeaderRow" );
				if ( header.isVisible() ) {
					header.setMaxHeight( 0 );
					header.setMinHeight( 0 );
					header.setPrefHeight( 0 );
					header.setVisible( false );
				}
			}
		} );

		TableColumn <Path, String> dirListColumn = new TableColumn( "Location" );
		dirListColumn.setCellValueFactory( new Callback <TableColumn.CellDataFeatures <Path, String>, ObservableValue <String>>() {

			@Override
			public ObservableValue <String> call ( TableColumn.CellDataFeatures <Path, String> p ) {
				if ( p.getValue() != null ) {
					return new SimpleStringProperty( p.getValue().toAbsolutePath().toString() );
				} else {
					return new SimpleStringProperty( "<no name>" );
				}
			}
		} );
		
		musicSourceTable.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				List <File> files = db.getFiles();
				
				for ( File file : files ) {
					Library.requestAddSource( file.toPath() );
				}

				event.setDropCompleted( true );
				event.consume();
			}
		});
			
		musicSourceTable.getColumns().add( dirListColumn );
		
		musicSourceTable.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				event.acceptTransferModes( TransferMode.MOVE );
				event.consume();

			}
		});

		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle( "Music Folder" );
		File defaultDirectory = new File( System.getProperty( "user.home" ) ); // TODO: start windows on desktop maybe.
		chooser.setInitialDirectory( defaultDirectory );

		Button addButton = new Button( "+ Add" );
		Button removeButton = new Button( "- Remove" );

		addButton.setPrefWidth( 100 );
		removeButton.setPrefWidth( 100 );
		addButton.setMinWidth( 100 );
		removeButton.setMinWidth( 100 );

		addButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				File selectedFile = chooser.showDialog( libraryWindow );
				if ( selectedFile != null ) {
					Library.requestAddSource( selectedFile.toPath() );
				}
			}
		});

		removeButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				Library.requestRemoveSources( musicSourceTable.getSelectionModel().getSelectedItems() );
				musicSourceTable.getSelectionModel().clearSelection();	
			}
		});

		musicSourceTable.setOnKeyPressed( new EventHandler <KeyEvent>() {
			@Override
			public void handle ( final KeyEvent keyEvent ) {
				if ( keyEvent.getCode().equals( KeyCode.DELETE ) ) {
					Library.requestRemoveSources( musicSourceTable.getSelectionModel().getSelectedItems() );
				}
			}
		});
		
		HBox controlBox = new HBox();
		controlBox.getChildren().addAll( addButton, removeButton);
		controlBox.setAlignment( Pos.CENTER );
		controlBox.prefWidthProperty().bind( libraryWindow.widthProperty() );
		controlBox.setPadding( new Insets( 5 ) );

		primaryPane.getChildren().addAll( musicSourceTable, controlBox );
		root.getChildren().add( primaryPane );
		libraryWindow.setScene( scene );
	}
	
	private static boolean acceptTrackFromFilterRules ( Track track ) {
		boolean checkBoxSelected = trackListCheckBox.isSelected();
		String filterText = trackFilterBox.getText();
		
		if ( track.hasAlbum() && checkBoxSelected ) {
			return false;
		} 
		
		if ( filterText == null || filterText.isEmpty() ) {
			return true;
		}

		String[] lowerCaseFilterTokens = filterText.toLowerCase().split( "\\s+" );

		ArrayList <String> matchableText = new ArrayList <String>();

		matchableText.add( Normalizer.normalize( track.getTitle(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
		matchableText.add( track.getTitle().toLowerCase() );

		for ( String token : lowerCaseFilterTokens ) {
			boolean tokenMatches = false;
			for ( String test : matchableText ) {
				if ( test.contains( token ) ) {
					tokenMatches = true;
				}
			}

			if ( !tokenMatches ) {
				return false;
			}
		}

		return true;
	}
	
	public void setupAlbumImage () {
		albumImage = new BorderPane();
	}

	public void setupArtistImage () {
		artistImage = new BorderPane();
	}

	public static void setAlbumImage ( Path imagePath ) {
		try {
			ResizableImageView view = new ResizableImageView(
					new Image( imagePath.toUri().toString() ) );
			view.setPreserveRatio( true );
			albumImage.setCenter( view );
		} catch ( Exception e ) {
			albumImage.setCenter( null );
		}
	}

	public static void setArtistImage ( Path imagePath ) {
		try {
			ResizableImageView view = new ResizableImageView(
					new Image( imagePath.toUri().toString() ) );
			view.setPreserveRatio( true );
			artistImage.setCenter( view );
		} catch ( Exception e ) {
			artistImage.setCenter( null );
		}
	}
	
	public void addToPlaylist ( List <Track> tracks, Playlist playlist ) {
		playlist.getTracks().addAll( tracks );
		if ( currentPlaylist != null && currentPlaylist.getName().equals( playlist.getName() ) ) {
			currentListData.addAll( Utils.convertTrackList( tracks ) );
		}
		playlistTable.refresh(); 
	}
	
	public void promptAndSavePlaylist ( List <Track> tracks, boolean isCurrentList ) { 
	//TODO: I can probably figure out if it's current on my own
		String defaultName = "";
		if ( currentPlaylist != null ) {
			defaultName = currentPlaylist.getName();
		}
		TextInputDialog dialog = new TextInputDialog( defaultName );

		dialog.setX( mainStage.getX() + mainStage.getWidth() / 2 - 150 );
		dialog.setY( mainStage.getY() + mainStage.getHeight() / 2 - 100 );
		dialog.setTitle( "Save Playlist" );
		dialog.setHeaderText( null );
		Optional <String> result = dialog.showAndWait();
		if ( result.isPresent() ) {
			Playlist replaceMe = null;
			String enteredName = result.get();

			for ( Playlist test : Library.playlists ) {
				if ( test.getName().equals( enteredName ) ) {
					Library.removePlaylist ( test );
					break;
				}
			}

			Playlist newPlaylist = new Playlist( enteredName, new ArrayList <Track> ( tracks ) );
			Library.addPlaylist ( newPlaylist );
			
			if ( isCurrentList ) {
				currentPlaylist = newPlaylist;
				currentPlayingListInfo.setText( "Playlist: " + newPlaylist.getName() );
				playlistChanged = false;
			}
		}
	}
	
	public void promptAndRenamePlaylist ( Playlist playlist ) {
		TextInputDialog dialog = new TextInputDialog( playlist.getName() );
		dialog.setX( mainStage.getX() + mainStage.getWidth() / 2 - 150 );
		dialog.setY( mainStage.getY() + mainStage.getHeight() / 2 - 100 );
		dialog.setTitle( "Rename Playlist" );
		dialog.setHeaderText( null );
		Optional <String> result = dialog.showAndWait();
		
		if ( result.isPresent() ) {
			Playlist replaceMe = null;
			String enteredName = result.get();

			Library.removePlaylist( playlist );
			playlist.setName ( enteredName );
			Library.addPlaylist( playlist );
			
			if ( currentPlaylist.equals( playlist ) ) {
				String title = "Playlist: " + playlist.getName();
				if ( playlistChanged ) title += " *";
				currentPlayingListInfo.setText( title );
			}
		}
	}

	public void setupCurrentListControlPane () {

		toggleRepeatButton = new Button( repeatMode.getSymbol() );
		toggleShuffleButton = new Button( shuffleMode.getSymbol() );
		Button showQueueButton = new Button ( "Q" );
		Button loadTracksButton = new Button( "⏏" );
		Button savePlaylistButton = new Button( "💾" );
		
		showQueueButton.setOnAction ( new EventHandler <ActionEvent>() {
			public void handle ( ActionEvent e ) {
				queueWindow.show();
			}
		} );

		savePlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				promptAndSavePlaylist( new ArrayList <Track>( currentListData ), true );
			}
		});
		
		loadTracksButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				FileChooser fileChooser = new FileChooser();
				
				ArrayList <String> filters = new ArrayList <String> ();
				
				for ( String ending : Utils.musicExtensions ) {
					filters.add( "*." + ending );
				}
				
				FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter( "Audio Files", filters );
				fileChooser.getExtensionFilters().add( fileExtensions );
				List <File> selectedFiles = fileChooser.showOpenMultipleDialog( mainStage );
				
				if ( selectedFiles == null ) {
					return;
				}
				
				ArrayList <Track> tracks = new ArrayList <Track> ();
				for ( File file : selectedFiles ) {
					tracks.add( new Track ( file.toPath() ) );
				}
				
				loadTracks ( tracks );
					
			}
		});

		toggleRepeatButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				if ( repeatMode == RepeatMode.PLAY_ONCE ) {
					repeatMode = RepeatMode.REPEAT;
				} else {
					repeatMode = RepeatMode.PLAY_ONCE;
				}

				toggleRepeatButton.setText( repeatMode.getSymbol() );

			}
		});

		toggleShuffleButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				if ( shuffleMode == ShuffleMode.SEQUENTIAL ) {
					shuffleMode = ShuffleMode.SHUFFLE;
				} else {
					shuffleMode = ShuffleMode.SEQUENTIAL;
				}

				toggleShuffleButton.setText( shuffleMode.getSymbol() );

			}
		});

		EventHandler savePlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				String playlistName = ((Playlist) ((MenuItem) event.getSource()).getUserData()).getName();
				Playlist playlist = new Playlist( playlistName, new ArrayList <Track>( currentListTable.getItems() ) );
				Library.addPlaylist( playlist );
			}
		};

		currentPlayingListInfo.setAlignment( Pos.CENTER );

		playlistControls = new HBox();
		playlistControls.setAlignment( Pos.CENTER_RIGHT );

		currentPlayingListInfo.prefWidthProperty().bind( playlistControls.widthProperty() );

		playlistControls.getChildren().addAll( toggleRepeatButton, toggleShuffleButton, showQueueButton,
				currentPlayingListInfo, loadTracksButton, savePlaylistButton );
	}

	public void setupPlaylistFilterPane () {
		playlistFilterPane = new HBox();
		TextField filterBox = new TextField();
		filterBox.setPrefWidth( 500000 );
		
		filterBox.textProperty().addListener( ( observable, oldValue, newValue ) -> {
			Library.playlistsFiltered.setPredicate( playlist -> {
				if ( newValue == null || newValue.isEmpty() ) {
					return true;
				}

				String[] lowerCaseFilterTokens = newValue.toLowerCase().split( "\\s+" );

				ArrayList <String> matchableText = new ArrayList <String>();

				matchableText.add( Normalizer.normalize( playlist.getName(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
				matchableText.add( playlist.getName().toLowerCase() );

				for ( String token : lowerCaseFilterTokens ) {
					boolean tokenMatches = false;
					for ( String test : matchableText ) {
						if ( test.contains( token ) ) {
							tokenMatches = true;
						}
					}

					if ( !tokenMatches ) {
						return false;
					}
				}

				return true;
			});
		});
		
		Button settingsButton = new Button( "≡" );
		settingsButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				if ( libraryWindow.isShowing() ) {
					libraryWindow.hide();
				} else {
					libraryWindow.show();
				}
			}
		});
		
		Button clearButton = new Button ( "✘" );
		clearButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				filterBox.setText( "" );
			}
		});

		playlistFilterPane.getChildren().addAll( settingsButton, filterBox, clearButton );
	}

	public void setupTrackFilterPane () {
		trackFilterPane = new HBox();
		trackFilterBox = new TextField();
		trackFilterBox.setPrefWidth( 500000 );
		
		trackFilterBox.textProperty().addListener( new ChangeListener <String> () {

			@Override
			public void changed ( ObservableValue <? extends String> observable, String oldValue, String newValue ) {
				Library.tracksFiltered.setPredicate( track -> {
					return acceptChange ( track, oldValue, newValue );
				});
			}
		});
		
		Button settingsButton = new Button( "≡" );
		settingsButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				if ( libraryWindow.isShowing() ) {
					libraryWindow.hide();
				} else {
					libraryWindow.show();
				}
			}
		} );
		
		Button clearButton = new Button ( "✘" );
		clearButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				trackFilterBox.setText( "" );
			}
		});
		
		HBox checkBoxMargins = new HBox();
		checkBoxMargins.setPadding( new Insets ( 4, 0, 0, 6 ) );
		checkBoxMargins.getChildren().add( trackListCheckBox );
		
		trackFilterPane.getChildren().addAll( settingsButton, trackFilterBox, clearButton, checkBoxMargins );
	}
	
	public static boolean acceptChange ( Track track, Object oldValue, Object newValueIn ) {
				
		String newValue = trackFilterBox.getText();
		if ( newValueIn instanceof String ) {
			newValue = (String)newValueIn;
		}
		
		Boolean boxSelected = trackListCheckBox.isSelected();
		if ( newValueIn instanceof Boolean ) {
			boxSelected = (Boolean)newValueIn;
		}
			
		if ( track.hasAlbum() && boxSelected ) {
			return false;
		} 
	
		if ( newValue == null || newValue.isEmpty() ) {
			return true;
		}
		
		String[] lowerCaseFilterTokens = newValue.toLowerCase().split( "\\s+" );

		ArrayList <String> matchableText = new ArrayList <String>();

		matchableText.add( Normalizer.normalize( track.getArtist(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
		matchableText.add( track.getArtist().toLowerCase() );
		matchableText.add( Normalizer.normalize( track.getTitle(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
		matchableText.add( track.getTitle().toLowerCase() );

		for ( String token : lowerCaseFilterTokens ) {
			boolean tokenMatches = false;
			for ( String test : matchableText ) {
				if ( test.contains( token ) ) {
					tokenMatches = true;
				}
			}

			if ( !tokenMatches ) {
				return false;
			}
		}

		return true;
	}

	public void setupAlbumFilterPane () {
		albumFilterPane = new HBox();
		TextField filterBox = new TextField();
		filterBox.setPrefWidth( 500000 );
		filterBox.textProperty().addListener( ( observable, oldValue, newValue ) -> {
			Library.albumsFiltered.setPredicate( album -> {
				if ( newValue == null || newValue.isEmpty() ) {
					return true;
				}

				String[] lowerCaseFilterTokens = newValue.toLowerCase().split( "\\s+" );

				ArrayList <String> matchableText = new ArrayList <String>();

				matchableText.add( Normalizer.normalize( album.getAlbumArtist(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
				matchableText.add( album.getAlbumArtist().toLowerCase() );
				matchableText.add( Normalizer.normalize( album.getTitle(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
				matchableText.add( album.getTitle().toLowerCase() );
				matchableText.add( Normalizer.normalize( album.getYear(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
				matchableText.add( album.getYear().toLowerCase() );

				for ( String token : lowerCaseFilterTokens ) {
					boolean tokenMatches = false;
					for ( String test : matchableText ) {
						if ( test.contains( token ) ) {
							tokenMatches = true;
						}
					}

					if ( !tokenMatches ) {
						return false;
					}
				}

				return true;
			} );
		} );

		Button settingsButton = new Button( "≡" );
		settingsButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				if ( libraryWindow.isShowing() ) {
					libraryWindow.hide();
				} else {
					libraryWindow.show();
				}
			}
		} );

		Button clearButton = new Button ( "✘" );
		clearButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				filterBox.setText( "" );
			}
		});

		albumFilterPane.getChildren().addAll( settingsButton, filterBox, clearButton );
	}

	public void setupTrackListCheckBox() {
		trackListCheckBox = new CheckBox( "" );
		trackListCheckBox.selectedProperty().addListener( new ChangeListener <Boolean> () {
			@Override
			public void changed ( ObservableValue <? extends Boolean> observable, Boolean oldValue, Boolean newValue ) {
				Library.tracksFiltered.setPredicate( track -> {
					return acceptChange ( track, oldValue, newValue );
				});
			}
		});
		
		Tooltip tooltip = new Tooltip( "Unchecked: List all tracks.\nChecked: Hide tracks that are part of an album." );
		Tooltip.install( new Rectangle( 0, 0, 10, 10 ), tooltip );
		
		trackListCheckBox.setTooltip( tooltip );
	}

	public void setupAlbumTable () {
		TableColumn artistColumn = new TableColumn( "Artist" );
		TableColumn yearColumn = new TableColumn( "Year" );
		TableColumn albumColumn = new TableColumn( "Album" );

		artistColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "albumArtist" ) );
		yearColumn.setCellValueFactory( new PropertyValueFactory <Album, Integer>( "year" ) );
		albumColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "title" ) );

		artistColumn.setMaxWidth( 45000 );
		yearColumn.setMaxWidth( 10000 );
		albumColumn.setMaxWidth( 45000 );

		albumTable = new TableView();
		albumTable.getColumns().addAll( artistColumn, yearColumn, albumColumn );
		albumTable.setEditable( false );
		albumTable.setItems( Library.albumsSorted );
		albumTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		Library.albumsSorted.comparatorProperty().bind( albumTable.comparatorProperty() );

		albumTable.getSortOrder().add( artistColumn );
		albumTable.getSortOrder().add( yearColumn );
		albumTable.getSortOrder().add( albumColumn );
		FixedWidthCustomResizePolicy resizePolicy = new FixedWidthCustomResizePolicy();
		resizePolicy.registerColumns( yearColumn );
		albumTable.setColumnResizePolicy( resizePolicy );
		
		Label placeholder = new Label( "No albums loaded, click on the ≡ menu, or drop folders here, to add to your library." );
		placeholder.setPadding( new Insets( 20, 10, 20, 10 ) );
		placeholder.setWrapText( true );
		placeholder.setTextAlignment( TextAlignment.CENTER );
		
		albumTable.setPlaceholder( placeholder );

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem addMenuItem = new MenuItem( "Append" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		
		contextMenu.getItems().addAll( playMenuItem, addMenuItem, editTagMenuItem, browseMenuItem, addToPlaylistMenuItem );
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			
			@Override
			public void handle ( ActionEvent e ) {
				ObservableList <Album> selectedAlbums = albumTable.getSelectionModel().getSelectedItems();
				ArrayList <Track> tracks = new ArrayList <Track> ();
				
				for ( Album album : selectedAlbums ) {
					tracks.addAll( album.getTracks() );
				}
				promptAndSavePlaylist ( tracks, false );
			}
		});

		EventHandler addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {

				
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				
				ArrayList <Album> albums = new ArrayList <Album> ( albumTable.getSelectionModel().getSelectedItems() );
				ArrayList <Track> tracksToAdd = new ArrayList <Track> ();
				
				for ( Album album : albums ) {
					tracksToAdd.addAll( album.getTracks() );
				}
				
				addToPlaylist ( tracksToAdd, playlist );
			}
		};

		Library.playlistsSorted.addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				playAlbum( albumTable.getSelectionModel().getSelectedItem() );
			}
		} );

		addMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				appendAlbum( albumTable.getSelectionModel().getSelectedItem() );
			}
		} );
		
		editTagMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List<Album> albums = albumTable.getSelectionModel().getSelectedItems();
				ArrayList<Track> editMe = new ArrayList<Track>();
				
				for ( Album album : albums ) {
					editMe.addAll( album.getTracks() );
				}
				
				tagWindow.setTracks( editMe, albums, FieldKey.TRACK, FieldKey.TITLE );
				tagWindow.show();
			}
		});

		browseMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			// TODO: This is the better way, once openjdk and openjfx supports
			// it: getHostServices().showDocument(file.toURI().toString());
			@Override
			public void handle ( ActionEvent event ) {
				SwingUtilities.invokeLater( new Runnable() {
					public void run () {
						try {
							Desktop.getDesktop().open( albumTable.getSelectionModel().getSelectedItem().getPath().toFile() );
						} catch ( IOException e ) {
							e.printStackTrace();
						}
					}
				} );
			}
		} );
		
		albumTable.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				event.acceptTransferModes( TransferMode.MOVE );
				event.consume();

			}
		});
		
		albumTable.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				List <File> files = db.getFiles();
				
				for ( File file : files ) {
					Library.requestAddSource( file.toPath() );
				}

				event.setDropCompleted( true );
				event.consume();
			}
		});

		albumTable.setRowFactory( tv -> {
			TableRow <Album> row = new TableRow <>();
			
			row.setContextMenu( contextMenu );

			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					playAlbum( row.getItem() );
				}
			} );
			
			row.setOnDragOver( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasFiles() ) {
					event.acceptTransferModes( TransferMode.MOVE );
					event.consume();

				}
			});
			
			row.setOnDragDropped( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasFiles() ) {
					List <File> files = db.getFiles();
					
					for ( File file : files ) {
						Library.requestAddSource( file.toPath() );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					ArrayList <Integer> index = new ArrayList ( albumTable.getSelectionModel().getSelectedIndices() );
					Dragboard db = row.startDragAndDrop( TransferMode.MOVE );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( DRAGGED_ALBUM_INDICES, index );
					db.setContent( cc );
					event.consume();

				}
			} );

			return row;
		} );
	}

	public void setupTrackTable () {
		TableColumn artistColumn = new TableColumn( "Artist" );
		TableColumn lengthColumn = new TableColumn( "Length" );
		TableColumn titleColumn = new TableColumn( "Title" );

		artistColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Artist" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Title" ) );
		lengthColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "LengthDisplay" ) );

		artistColumn.setSortType( TableColumn.SortType.ASCENDING );

		artistColumn.setMaxWidth( 45000 );
		titleColumn.setMaxWidth( 45000 );
		lengthColumn.setMaxWidth( 10000 );

		trackTable = new TableView();
		trackTable.getColumns().addAll( artistColumn, titleColumn, lengthColumn );
		trackTable.setEditable( false );
		trackTable.setItems( Library.tracksSorted );

		Library.tracksSorted.comparatorProperty().bind( trackTable.comparatorProperty() );
		
		trackTable.getSelectionModel().clearSelection();
		trackTable.getSortOrder().add( artistColumn );
		trackTable.getSortOrder().add( titleColumn );
		FixedWidthCustomResizePolicy resizePolicy = new FixedWidthCustomResizePolicy();
		// TODO resizePolicy.registerColumns ( lengthColumn );
		trackTable.setColumnResizePolicy( resizePolicy );
		
		Label placeholder = new Label( "No tracks loaded, click on the ≡ menu, or drop folders here, to add to your library." );
		placeholder.setPadding( new Insets( 20, 10, 20, 10 ) );
		placeholder.setWrapText( true );
		placeholder.setTextAlignment( TextAlignment.CENTER );
		trackTable.setPlaceholder( placeholder );
		
		trackTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem addMenuItem = new MenuItem( "Append" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		contextMenu.getItems().addAll( playMenuItem, addMenuItem, editTagMenuItem, browseMenuItem, addToPlaylistMenuItem );
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				promptAndSavePlaylist ( trackTable.getSelectionModel().getSelectedItems(), false );
			}
		});

		EventHandler addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				addToPlaylist ( trackTable.getSelectionModel().getSelectedItems(), playlist );
			}
		};

		Library.playlistsSorted.addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				loadTrack( trackTable.getSelectionModel().getSelectedItem() );
			}
		} );

		addMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				currentListTable.getItems().addAll( Utils.convertTrackList( trackTable.getSelectionModel().getSelectedItems() ) );
			}
		} );
		
		editTagMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List<Track> tracks = trackTable.getSelectionModel().getSelectedItems();
				
				tagWindow.setTracks( tracks, null );
				tagWindow.show();
			}
		});

		browseMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			// TODO: This is the better way, once openjdk and openjfx supports
			// it: getHostServices().showDocument(file.toURI().toString());
			@Override
			public void handle ( ActionEvent event ) {
				SwingUtilities.invokeLater( new Runnable() {
					public void run () {
						try {
							Desktop.getDesktop().open( trackTable.getSelectionModel().getSelectedItem().getPath().getParent().toFile() );
						} catch ( IOException e ) {
							e.printStackTrace();
						}
					}
				} );
			}
		} );
		
		trackTable.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				event.acceptTransferModes( TransferMode.MOVE );
				event.consume();

			}
		});
		
		trackTable.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				List <File> files = db.getFiles();
				
				for ( File file : files ) {
					Library.requestAddSource( file.toPath() );
				}

				event.setDropCompleted( true );
				event.consume();
			}
		});

		trackTable.setRowFactory( tv -> {
			TableRow <Track> row = new TableRow <>();
			
			row.setContextMenu( contextMenu );
			
			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					currentListTable.getItems().clear();
					try {
						currentListTable.getItems().add( new CurrentListTrack ( trackTable.getSelectionModel().getSelectedItem() ) );
					} catch ( CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e1 ) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					playTrack( trackTable.getSelectionModel().getSelectedItem() );
					currentPlayingListInfo.setText( "Playlist: New Playlist *" );
				}
			} );
			
			row.setOnDragOver( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasFiles() ) {
					event.acceptTransferModes( TransferMode.MOVE );
					event.consume();

				}
			});
			
			row.setOnDragDropped( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasFiles() ) {
					List <File> files = db.getFiles();
					
					for ( File file : files ) {
						Library.requestAddSource( file.toPath() );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					ArrayList <Track> tracks = new ArrayList <Track>( trackTable.getSelectionModel().getSelectedItems() );
					Dragboard db = row.startDragAndDrop( TransferMode.MOVE );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( DRAGGED_TRACKS, tracks );
					db.setContent( cc );
					event.consume();

				}
			} );

			return row;
		} );
	}

	public void setupPlaylistTable () {
		TableColumn nameColumn = new TableColumn( "Playlist" );
		TableColumn lengthColumn = new TableColumn( "Length" );
		TableColumn tracksColumn = new TableColumn( "Tracks" );

		nameColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "Name" ) );
		lengthColumn.setCellValueFactory( new PropertyValueFactory <Album, Integer>( "LengthDisplay" ) );
		tracksColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "SongCount" ) );

		nameColumn.setSortType( TableColumn.SortType.ASCENDING );

		nameColumn.setMaxWidth( 70000 );
		lengthColumn.setMaxWidth( 15000 );
		tracksColumn.setMaxWidth( 15000 );

		playlistTable = new TableView();
		playlistTable.getColumns().addAll( nameColumn, tracksColumn, lengthColumn );
		playlistTable.setEditable( false );
		playlistTable.setItems( Library.playlistsSorted );

		Library.playlistsSorted.comparatorProperty().bind( playlistTable.comparatorProperty() );

		playlistTable.getSortOrder().add( nameColumn );
		playlistTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );

		FixedWidthCustomResizePolicy resizePolicy = new FixedWidthCustomResizePolicy();
		resizePolicy.registerColumns( tracksColumn );
		playlistTable.setColumnResizePolicy( resizePolicy );

		Label emptyLabel = new Label( "You haven't created any playlists, make a playlist on the right and click 💾 to save it for later." );
		emptyLabel.setWrapText( true );
		emptyLabel.setTextAlignment( TextAlignment.CENTER );
		emptyLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		playlistTable.setPlaceholder( emptyLabel );

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem appendMenuItem = new MenuItem( "Append" );
		MenuItem renameMenuItem = new MenuItem( "Rename" );
		MenuItem removeMenuItem = new MenuItem( "Remove" );
		contextMenu.getItems().addAll( playMenuItem, appendMenuItem, renameMenuItem, removeMenuItem );

		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				playPlaylist( playlistTable.getSelectionModel().getSelectedItem() );
			}
		});

		appendMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				currentListData.addAll( Utils.convertTrackList( playlistTable.getSelectionModel().getSelectedItem().getTracks() ) );
			}
		});
		
		renameMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				promptAndRenamePlaylist ( playlistTable.getSelectionModel().getSelectedItem() );
			}
		});

		removeMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Library.removePlaylist( playlistTable.getSelectionModel().getSelectedItem() );
			}
		});

        
		playlistTable.setRowFactory( tv -> {
			TableRow <Playlist> row = new TableRow <>();
			
			row.setContextMenu ( contextMenu );

			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					playPlaylist( row.getItem() );
				}
			});

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					Playlist playlist = row.getItem();
					Dragboard db = row.startDragAndDrop( TransferMode.MOVE );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( DRAGGED_PLAYLIST_INDEX, playlist );
					db.setContent( cc );
					event.consume();
				}
			});

			row.setOnDragOver( event -> {

				Dragboard db = event.getDragboard();
				if ( db.hasContent( DRAGGED_TRACK_INDICES ) ) {
					if ( !row.isEmpty() ) {
						event.acceptTransferModes( TransferMode.MOVE );
						event.consume();
					}
				}
			});

			row.setOnDragDropped( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasContent( DRAGGED_TRACK_INDICES ) ) {
					if ( !row.isEmpty() ) {
						ArrayList <Integer> draggedIndexes = (ArrayList <Integer>) db.getContent( DRAGGED_TRACK_INDICES );
						ArrayList <Track> draggedTracks = new ArrayList <Track>( draggedIndexes.size() );

						for ( int index : draggedIndexes ) {
							draggedTracks.add( currentListData.get( index ) );
						}

						Playlist playlist = row.getItem();
						playlist.getTracks().addAll( draggedTracks );
						playlistTable.refresh();
						event.setDropCompleted( true );
						event.consume();
					}
				}
			});

			return row;
		});
	}
	
	public void setupCurrentListTable () {
		TableColumn playingColumn = new TableColumn( "" );
		TableColumn artistColumn = new TableColumn( "Artist" );
		TableColumn yearColumn = new TableColumn( "Year" );
		TableColumn albumColumn = new TableColumn( "Album" );
		TableColumn titleColumn = new TableColumn( "Title" );
		TableColumn trackColumn = new TableColumn( "#" );
		TableColumn lengthColumn = new TableColumn( "Length" );

		artistColumn.setMaxWidth( 22000 );
		trackColumn.setMaxWidth( 4000 );
		yearColumn.setMaxWidth( 8000 );
		albumColumn.setMaxWidth( 25000 );
		titleColumn.setMaxWidth( 25000 );
		lengthColumn.setMaxWidth( 8000 );
		
		playingColumn.setCellValueFactory( new PropertyValueFactory ( "display" ) );
		artistColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "artist" ) );
		yearColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, Integer>( "year" ) );
		albumColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "album" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "title" ) );
		trackColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, Integer>( "trackNumber" ) );
		lengthColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "lengthDisplay" ) );
		
		trackColumn.setCellFactory( column -> {
			return new TableCell <Track, Integer>() {
				@Override
				protected void updateItem ( Integer value, boolean empty ) {
					super.updateItem( value, empty );

					if ( value == null || value.equals( Track.NO_TRACK_NUMBER ) || empty ) {
						setText( null );
						setStyle( "" );
					} else {
						setText( value.toString() );
					}
				}
			};
		} );

		currentListTable = new TableView();
		currentListTable.getColumns().addAll( playingColumn, trackColumn, artistColumn, yearColumn, albumColumn, titleColumn, lengthColumn );
		albumTable.getSortOrder().add( trackColumn );
		currentListTable.setEditable( false );
		currentListTable.setItems( currentListData );

		currentListData.addListener( new InvalidationListener() {
			@Override
			public void invalidated ( Observable arg0 ) {
				if ( !playlistChanged ) {
					currentPlayingListInfo.setText( currentPlayingListInfo.getText() + " *" );
					playlistChanged = true;
				}
			}
		});

		FixedWidthCustomResizePolicy resizePolicy = new FixedWidthCustomResizePolicy();
		currentListTable.setColumnResizePolicy( resizePolicy );

		resizePolicy.registerColumns( yearColumn, trackColumn );
		// TODO: Length column policy
		currentListTable.setPlaceholder( new Label( "No tracks in playlist." ) );
		currentListTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		playingColumn.setMaxWidth( 28 );
		playingColumn.setMinWidth( 28 );

		currentListTable.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();

			if ( db.hasContent( DRAGGED_TRACKS ) 
			|| db.hasContent( DRAGGED_ALBUM_INDICES ) 
			|| db.hasContent( DRAGGED_PLAYLIST_INDEX ) 
			|| db.hasFiles() ) {

				event.acceptTransferModes( TransferMode.MOVE );
				event.consume();

			}
		} );

		currentListTable.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();

			if ( db.hasContent( DRAGGED_TRACKS ) ) {
				ArrayList <Track> draggedTracks = (ArrayList <Track>) db.getContent( DRAGGED_TRACKS );
				currentListTable.getItems().addAll( Utils.convertTrackList( draggedTracks ) );

				event.setDropCompleted( true );
				currentListTable.getSelectionModel().clearSelection();
				event.consume();

			} else if ( db.hasContent( DRAGGED_ALBUM_INDICES ) ) {
				if ( currentListTable.getItems().isEmpty() ) {
					// If the list is empty, we handle the drop, otherwise let
					// the rows handle it
					ArrayList <Integer> draggedIndices = (ArrayList<Integer>) db.getContent( DRAGGED_ALBUM_INDICES );
					
					ArrayList <Track> addMe = new ArrayList <Track> ();
					
					for ( int index : draggedIndices ) {
						addMe.addAll ( albumTable.getItems().get( index ).getTracks() );
					}

					int dropIndex = 0;
					currentListTable.getItems().addAll( dropIndex, Utils.convertTrackList( addMe ) );

					event.setDropCompleted( true );
					event.consume();
				}

			} else if ( db.hasContent( DRAGGED_PLAYLIST_INDEX ) ) {
				if ( currentListTable.getItems().isEmpty() ) {
					// If the list is empty, we handle the drop, otherwise let the rows handle it
					Playlist draggedPlaylist = (Playlist) db.getContent( DRAGGED_PLAYLIST_INDEX );

					int dropIndex = 0;
					currentListTable.getItems().addAll( dropIndex, Utils.convertTrackList( draggedPlaylist.getTracks() ) );

					event.setDropCompleted( true );
					event.consume();
				}

			} else if ( db.hasFiles() ) {
				ArrayList <Track> tracksToAdd = new ArrayList();
				for ( File file : db.getFiles() ) {
					Path droppedPath = Paths.get( file.getAbsolutePath() );
					if ( Utils.isMusicFile( droppedPath ) ) {
						try {
							currentListTable.getItems().add( new CurrentListTrack( droppedPath ) );
						} catch ( CannotReadException | IOException | TagException 
						| ReadOnlyFileException | InvalidAudioFrameException e ) {
							e.printStackTrace();
						}
					} else if ( Files.isDirectory( droppedPath ) ) {
						currentListTable.getItems().addAll( Utils.convertTrackList( Utils.getAllTracksInDirectory( droppedPath ) ) );
					}
				}

				event.setDropCompleted( true );
				event.consume();
			}

		} );

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem queueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem cropMenuItem = new MenuItem( "Crop" );
		MenuItem deleteMenuItem = new MenuItem( "Delete" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );

		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );
		
		//TODO: These don't work right
		queueMenuItem.setAccelerator( new KeyCodeCombination ( KeyCode.Q, KeyCombination.SHIFT_ANY ) );
		playMenuItem.setAccelerator( new KeyCodeCombination ( KeyCode.ENTER ) );
		cropMenuItem.setAccelerator( new KeyCodeCombination ( KeyCode.DELETE, KeyCombination.SHIFT_DOWN ) );
		deleteMenuItem.setAccelerator( new KeyCodeCombination ( KeyCode.DELETE, KeyCombination.SHIFT_ANY ) );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				promptAndSavePlaylist ( new ArrayList <Track> ( currentListTable.getSelectionModel().getSelectedItems() ), false );
			}
		});

		EventHandler addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				addToPlaylist ( Utils.convertCurrentTrackList ( currentListTable.getSelectionModel().getSelectedItems() ), playlist );
			}
		};

		Library.playlistsSorted.addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );

		contextMenu.getItems().addAll( playMenuItem, queueMenuItem, editTagMenuItem, browseMenuItem, addToPlaylistMenuItem, cropMenuItem, deleteMenuItem );
		
		queueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Queue.addAll( currentListTable.getSelectionModel().getSelectedItems() );
			}
		});
		
		editTagMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				
				tagWindow.setTracks( (List<Track>)(List<?>)currentListTable.getSelectionModel().getSelectedItems(), null );
				tagWindow.show();
			}
		});

		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				playTrack( currentListTable.getSelectionModel().getSelectedItem() );
			}
		} );

		browseMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			// TODO: This is the better way, once openjdk and openjfx supports
			// it: getHostServices().showDocument(file.toURI().toString());
			@Override
			public void handle ( ActionEvent event ) {
				SwingUtilities.invokeLater( new Runnable() {
					public void run () {
						try {
							Desktop.getDesktop().open( currentListTable.getSelectionModel().getSelectedItem().getPath().getParent().toFile() );
						} catch ( IOException e ) {
							e.printStackTrace();
						}
					}
				} );
			}
		} );

		// TODO: right click delete and key delete are same code....
		deleteMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {

				ObservableList <Integer> selectedIndexes = currentListTable.getSelectionModel().getSelectedIndices();
				ObservableList <CurrentListTrack> selectedItems = currentListTable.getSelectionModel().getSelectedItems();

				if ( !selectedItems.isEmpty() ) {
					int selectAfterDelete = selectedIndexes.get( 0 ) - 1;
					currentListData.removeAll( selectedItems );
					currentListTable.getSelectionModel().clearAndSelect( selectAfterDelete );
				}
			}
		} );
		
		cropMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {

				ObservableList <Integer> selectedIndexes = currentListTable.getSelectionModel().getSelectedIndices();
				ObservableList <CurrentListTrack> selectedItems = currentListTable.getSelectionModel().getSelectedItems();

				if ( !selectedItems.isEmpty() ) {
					int selectAfterDelete = selectedIndexes.get( 0 ) - 1;
					currentListData.retainAll( selectedItems );
					currentListTable.getSelectionModel().clearSelection();
				}
			}
		} );

		/*
		currentListTable.setOnKeyPressed( new EventHandler <KeyEvent>() {
			@Override
			public void handle ( final KeyEvent keyEvent ) {
				if ( keyEvent.getCode().equals( KeyCode.DELETE ) ) {
					ObservableList <Integer> selectedIndexes = currentListTable.getSelectionModel().getSelectedIndices();
					ObservableList <CurrentListTrack> selectedItems = currentListTable.getSelectionModel().getSelectedItems();

					if ( !selectedItems.isEmpty() ) {
						int selectAfterDelete = selectedIndexes.get( 0 ) - 1;
						currentListData.removeAll( selectedItems );
						currentListTable.getSelectionModel().clearAndSelect( selectAfterDelete );
					}
				} else if ( keyEvent.getCode().equals ( KeyCode.Q ) ) {
					ObservableList <Integer> selectedIndexes = currentListTable.getSelectionModel().getSelectedIndices();
					ObservableList <CurrentListTrack> selectedItems = currentListTable.getSelectionModel().getSelectedItems();

					if ( !selectedItems.isEmpty() ) {
						Queue.addAll( selectedItems );
					}
				} else if ( keyEvent.getCode().equals( KeyCode.ENTER ) ) {
					playTrack( currentListTable.getSelectionModel().getSelectedItem() );
				}
			}
		});
		*/

		currentListTable.setRowFactory( tv -> {
			TableRow <CurrentListTrack> row = new TableRow <>();

			row.setContextMenu( contextMenu );

			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					playTrack( row.getItem() );
				}
			} );

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {

					ArrayList <Integer> indexes = new ArrayList( currentListTable.getSelectionModel().getSelectedIndices() );
					Dragboard db = row.startDragAndDrop( TransferMode.MOVE );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( DRAGGED_TRACK_INDICES, indexes );
					db.setContent( cc );
					event.consume();
				}
			} );

			row.setOnDragOver( event -> {

				Dragboard db = event.getDragboard();
				if ( db.hasContent( DRAGGED_TRACK_INDICES ) 
				|| db.hasContent( DRAGGED_TRACKS )
				|| db.hasContent( DRAGGED_ALBUM_INDICES )
				|| db.hasContent( DRAGGED_PLAYLIST_INDEX )
				|| db.hasFiles() ) {
					event.acceptTransferModes( TransferMode.MOVE );
					event.consume();
				}
			} );

			row.setOnDragDropped( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasContent( DRAGGED_TRACK_INDICES ) ) {
					ArrayList <Integer> draggedIndexes = (ArrayList <Integer>) db.getContent( DRAGGED_TRACK_INDICES );
					ArrayList <CurrentListTrack> tracksToMove = new ArrayList( draggedIndexes.size() );
					for ( int index : draggedIndexes ) {
						tracksToMove.add( currentListTable.getItems().get( index ) );
					}
					currentListTable.getItems().removeAll( tracksToMove );

					int dropIndex = row.isEmpty() ? dropIndex = currentListTable.getItems().size() : row.getIndex();
					currentListTable.getItems().addAll( dropIndex, tracksToMove );

					event.setDropCompleted( true );
					currentListTable.getSelectionModel().clearSelection();

					for ( int k = 0; k < tracksToMove.size(); k++ ) {
						currentListTable.getSelectionModel().select( dropIndex + k );
					}
					event.consume();

				} else if ( db.hasContent( DRAGGED_TRACKS ) ) {

					ArrayList <Track> draggedTracks = (ArrayList <Track>) db.getContent( DRAGGED_TRACKS );
					int dropIndex = row.isEmpty() ? dropIndex = currentListTable.getItems().size() : row.getIndex();
					currentListTable.getItems().addAll( dropIndex, Utils.convertTrackList ( draggedTracks ) );
					

					event.setDropCompleted( true );
					event.consume();

				} else if ( db.hasContent( DRAGGED_ALBUM_INDICES ) ) {
					
					ArrayList <Integer> draggedIndices = (ArrayList<Integer>) db.getContent( DRAGGED_ALBUM_INDICES );
					
					ArrayList <Track> addMe = new ArrayList <Track> ();
					
					for ( int index : draggedIndices ) {
						addMe.addAll ( albumTable.getItems().get( index ).getTracks() );
					}

					int dropIndex = row.isEmpty() ? dropIndex = currentListTable.getItems().size() : row.getIndex();
					
					if ( !addMe.isEmpty() ) {
						currentListTable.getItems().addAll( Math.min( dropIndex, currentListTable.getItems().size() ), Utils.convertTrackList( addMe ) );
					}
					event.setDropCompleted( true );
					event.consume();

				} else if ( db.hasContent( DRAGGED_PLAYLIST_INDEX ) ) {
					Playlist draggedPlaylist = (Playlist) db.getContent( DRAGGED_PLAYLIST_INDEX );

					int dropIndex = row.getIndex();
					if ( dropIndex >= currentListTable.getItems().size() ) {
						dropIndex = currentListTable.getItems().size();
					}
					currentListTable.getItems().addAll( dropIndex, Utils.convertTrackList( draggedPlaylist.getTracks() ) );

					event.setDropCompleted( true );
					event.consume();

				} else if ( db.hasFiles() ) {
					ArrayList <CurrentListTrack> tracksToAdd = new ArrayList();
					for ( File file : db.getFiles() ) {
						Path droppedPath = Paths.get( file.getAbsolutePath() );
						if ( Utils.isMusicFile( droppedPath ) ) {
							try {
								tracksToAdd.add( new CurrentListTrack( droppedPath ) );
							} catch ( CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e ) {
								e.printStackTrace();
							}
						} else if ( Files.isDirectory( droppedPath ) ) {
							tracksToAdd.addAll( Utils.convertTrackList( Utils.getAllTracksInDirectory( droppedPath ) ) );
						}
					}
					if ( !tracksToAdd.isEmpty() ) {
						int dropIndex = row.isEmpty() ? dropIndex = currentListTable.getItems().size() : row.getIndex();
						currentListTable.getItems().addAll( Math.min( dropIndex, currentListTable.getItems().size() ), tracksToAdd );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			} );

			return row;
		} );
	}
	
	public static void main ( String[] args ) {
		
		System.out.println( "Init stats: " );
		long startTime = System.currentTimeMillis();
		
		boolean firstInstance = SingleInstanceController.startCLICommandListener();
		
		System.out.println ( "CLI Listener: " + ( System.currentTimeMillis() - startTime ) );
		startTime = System.currentTimeMillis();
		
//		if ( firstInstance ) {
			Library.init();
			
			System.out.println ( "Library Init: " + ( System.currentTimeMillis() - startTime ) );
			startTime = System.currentTimeMillis();
			
			System.out.println ( "Persister Load " + ( System.currentTimeMillis() - startTime ) );
			startTime = System.currentTimeMillis();
	
			Application.launch( args );
			
			if ( currentPlayer != null ) {
				currentPlayer.stop();
			}
			
			Persister.saveData();
			System.exit ( 0 );
			
//		} else {
//			CLIParser parser = new CLIParser ( );
//			ArrayList <Integer> commands = parser.parseCommands( args );
//			SingleInstanceController.sendCommands( commands );
//			System.exit ( 0 );
//		}
	}
}

class LineNumbersCellFactory<T, E> implements Callback<TableColumn<T, E>, TableCell<T, E>> {

    public LineNumbersCellFactory() {
    }

    @Override
    public TableCell<T, E> call(TableColumn<T, E> param) {
        return new TableCell<T, E>() {
            @Override
            protected void updateItem(E item, boolean empty) {
                super.updateItem(item, empty);

                if (!empty) {
                    setText(this.getTableRow().getIndex() + 1 + "");
                } else {
                    setText("");
                }
            }
        };
    }
}
