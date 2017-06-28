package net.joshuad.hypnos;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import org.jaudiotagger.tag.FieldKey;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
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
import javafx.scene.control.Alert;
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
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import net.joshuad.hypnos.DraggedTrackContainer.DragSource;
import net.joshuad.hypnos.players.AbstractPlayer;

@SuppressWarnings({ "rawtypes", "unchecked" }) // TODO: Maybe get rid of this when I understand things better
public class MusicPlayerUI extends Application {
	
	private static final Logger LOGGER = Logger.getLogger( MusicPlayerUI.class.getName() );
	
	enum ExitCode {
		NORMAL ( 0 );
		
		private int value;
		ExitCode ( int value ) { this.value = value; }
		public int getValue() { return value; }
	}
	
	public static boolean IS_STANDALONE = false;
	public static boolean IS_DEVELOPING = false;
	public static Path ROOT;
	

	private static final int MAX_PREVIOUS_NEXT_STACK_SIZE = 10000;
	private static final int MAX_HISTORY_SIZE = 100;

	public static final DataFormat DRAGGED_TRACKS = new DataFormat( "application/hypnos-java-track" );

	public static final String PROGRAM_NAME = "Hypnos";

	final static ObservableList <CurrentListTrack> currentList = FXCollections.observableArrayList(); 
	
	static TableView <Album> albumTable;
	static TableView <Playlist> playlistTable;
	static TableView <Track> trackTable;
	static TableView <CurrentListTrack> currentListTable;
	static TableView <Path> musicSourceTable;
	static TableView <Track> queueTable;
	static TableView <Track> historyTable;

	final static ArrayList <Track> previousNextStack = new ArrayList <Track>(MAX_PREVIOUS_NEXT_STACK_SIZE);
	final static ObservableList <Track> history = FXCollections.observableArrayList( new ArrayList <Track>(MAX_HISTORY_SIZE) );

	static BorderPane albumImage;
	static BorderPane artistImage;
	
	static SplitPane primarySplitPane;
	static SplitPane currentListSplitPane;
	
	static ImageView playImage;
	static ImageView pauseImage;

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
	static Label trackInfo = new Label( "" );

	static ListInfoUpdater listInfoUpdater;
	
	static Label emptyPlaylistLabel = new Label( 
		"You haven't created any playlists, make a playlist on the right and click 💾 to save it for later." );

	static Label emptyTrackListLabel = new Label( 
		"No tracks loaded, click on the + button, or drop folders here, to add to your library." );
	
	static Label emptyAlbumListLabel = new Label(
		"No albums loaded, click on the + button, or drop folders here, to add to your library." );
	
	static Label filteredAlbumListLabel = new Label( "No albums match." );
	static Label filteredTrackListLabel = new Label( "No tracks match." );
	static Label filteredPlaylistLabel = new Label( "No playlists match." );

	Scene scene;
	static Stage mainStage;
	static Stage libraryWindow;
	static Stage queueWindow;
	static Stage historyWindow;
	static TagWindow tagWindow;
	static PlaylistInfoWindow playlistInfoWindow;
	static AlbumInfoWindow albumInfoWindow;

	static Button togglePlayButton;
	static Button toggleRepeatButton;
	static Button toggleShuffleButton;
	static Button showQueueButton;

	static SplitPane artSplitPane;

	static Random randomGenerator = new Random();

	static int playOnceShuffleTracksPlayedCounter = 1;

	static Playlist currentPlaylist = null;
	
	static CheckBox trackListCheckBox;
	static TextField trackFilterBox;

	static ShuffleMode shuffleMode = ShuffleMode.SEQUENTIAL;
	
	static boolean uiLoaded = false;

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
		PLAY_ONCE ( "⇥" ), REPEAT ( "🔁" ), REPEAT_ONE_TRACK ( "🔂" );

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
		});
	}
	
	public static void toggleMinimized() {
		mainStage.setIconified( !mainStage.isIconified() );
	}

	// This is called by the various players
	public static void songFinishedPlaying ( boolean userRequested ) {
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
	
	public static void previousTrack() {
		if ( currentPlayer == null ) {
			currentListTable.getSelectionModel().clearAndSelect( currentListTable.getSelectionModel().getSelectedIndex() - 1 );
			return;
		}
		
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
			togglePlayButton.setGraphic( pauseImage );
			
		} else if ( Queue.hasNext() ) {
			playTrack ( Queue.getNextTrack() );
		
		} else {
			Track selectedTrack = currentListTable.getSelectionModel().getSelectedItem();

			if ( selectedTrack != null ) {
				playTrack( selectedTrack );

			} else if ( !currentList.isEmpty() ) {
				selectedTrack = currentList.get( 0 );
				playTrack( selectedTrack );
			}
		}
	}
	
	public static void pause() {
		if ( currentPlayer != null ) {
			currentPlayer.pause();
			togglePlayButton.setGraphic( playImage );
		}
	}
		
	public static void nextTrack () {
		if ( currentPlayer == null ) {
			currentListTable.getSelectionModel().clearAndSelect( currentListTable.getSelectionModel().getSelectedIndex() + 1 );
		} else {
			if ( Queue.hasNext() ) {
				playTrack( Queue.getNextTrack() );
				
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
	}
	
	public static void playTrack ( Track track ) {
		playTrack ( track, false );
	}
	
	public static void playTrack ( Track track, boolean startPaused ) {
		playTrack ( track, startPaused, true );
	}
	                                                                 
	public static void playTrack ( Track track, boolean startPaused, boolean addToPreviousNextStack ) {
		if ( currentPlayer != null ) {
			currentPlayer.stop();
			if ( currentPlayer.getTrack() instanceof CurrentListTrack ) {
				((CurrentListTrack)currentPlayer.getTrack()).setIsCurrentTrack( false );
			}
			togglePlayButton.setGraphic( playImage );
		}
		
		currentPlayer = AbstractPlayer.getPlayer( track, trackPositionSlider, startPaused );
		
		if ( currentPlayer == null ) return;
		
		if ( track instanceof CurrentListTrack ) {
			((CurrentListTrack)track).setIsCurrentTrack( true );
		}
		
		if ( startPaused ) {
			togglePlayButton.setGraphic( playImage );
		} else {
			togglePlayButton.setGraphic( pauseImage );
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

		currentListTable.refresh();

		StackPane thumb = (StackPane) trackPositionSlider.lookup( ".thumb" );
		thumb.setVisible( true );

		trackInfo.setText( track.getArtist() + " - " + track.getTitle() );

		setAlbumImage( track.getAlbumCoverImage() );
		setArtistImage( track.getAlbumArtistImagePath( ) );
		
	}
	
	

	public static void playAlbum ( Album album ) {
		currentPlaylist = null;
		currentList.clear();
		currentList.addAll( Utils.convertTrackList( album.getTracks() ) );
		Track firstTrack = currentList.get( 0 );
		if ( firstTrack != null ) {
			playTrack( firstTrack );
		}

		listInfoUpdater.albumLoaded( album );
	}
	
	public static void playAlbums ( List<Album> albums ) {
		currentPlaylist = null;
		currentList.clear();
		for ( Album album : albums ) {
			currentList.addAll( Utils.convertTrackList( album.getTracks() ) );
		}
		Track firstTrack = currentList.get( 0 );
		if ( firstTrack != null ) {
			playTrack( firstTrack );
		}

		listInfoUpdater.albumsLoaded( albums );
	}
	
	public static void loadTrack ( Track track ) {
		loadTrack ( track, false );
	}
	
	public static void loadTrack ( Track track, boolean startPaused ) {
		ArrayList<Track> loadMe = new ArrayList <Track> ( 1 );
		loadMe.add ( track );
		loadTracks ( loadMe, startPaused );
	}
	
	public static void loadTrack ( String trackFile ) {
		loadTrack ( trackFile, false );
	}
	
	public static void loadTrack ( String trackFile, boolean startPaused ) {
		try {
			ArrayList<Track> loadMe = new ArrayList <Track> ( 1 );
			loadMe.add ( new Track ( Paths.get( trackFile ) ) );
			loadTracks ( loadMe, startPaused );
		} catch ( IOException e ) {
			System.out.println( "Unable to load track: " + trackFile + ", continuing." );
			System.out.println( e.getMessage() );
		}
	}
	
	public static void loadTracks ( List <Track> tracks ) {
		loadTracks ( tracks, false );
	}

	public static void loadTracks ( List <Track> tracks, boolean startPaused ) {
		currentList.clear();
		currentList.addAll( Utils.convertTrackList( tracks ) );
		listInfoUpdater.tracksLoaded ( );
		if ( !currentList.isEmpty() ) {
			playTrack( currentList.get( 0 ), startPaused );
		}
		currentPlaylist = null;
	}

	public static void playPlaylist ( Playlist playlist ) {
		playPlaylists ( Arrays.asList( playlist ) );
		currentPlaylist = playlist;
	}
	
	public static void playPlaylists ( List<Playlist> playlists ) {

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

		listInfoUpdater.playlistsLoaded ( playlists );
	}


	public static void appendAlbum ( Album album ) {
		currentList.addAll( Utils.convertTrackList( album.getTracks() ) );
		listInfoUpdater.tracksAdded();
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

		togglePlayButton.setGraphic( playImage );

		trackPositionSlider.setValue( 0 );
		timeElapsedLabel.setText( "" );
		timeRemainingLabel.setText( "" );
		trackInfo.setText( "" );

		StackPane thumb = (StackPane) trackPositionSlider.lookup( ".thumb" );
		thumb.setVisible( false );

	}

	public static void updatePlaylistMenuItems ( ObservableList <MenuItem> items, EventHandler <ActionEvent> eventHandler ) {

		items.remove( 1, items.size() );

		for ( Playlist playlist : Library.playlistsSorted ) {
			MenuItem newItem = new MenuItem( playlist.getName() );
			newItem.setUserData( playlist );
			newItem.setOnAction( eventHandler );
			items.add( newItem );
		}
	}

	public void setupTransport () {
	
		playImage = null;
		pauseImage = null;
		
		try {
			playImage = new ImageView ( new Image( new FileInputStream ( ROOT.resolve( "resources/play.png" ).toFile() ) ) );
			playImage.setFitHeight( 18 );
			playImage.setFitWidth( 18 );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "Unable to load play icon: resources/play.png" );
		}
		
		try {
			pauseImage = new ImageView ( new Image( new FileInputStream ( ROOT.resolve( "resources/pause.png" ).toFile() ) ) );
			pauseImage.setFitHeight( 18 );
			pauseImage.setFitWidth( 18 );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "Unable to load pause icon: resources/pause.png" );
		}
		
		togglePlayButton = new Button ( "" );
		togglePlayButton.setGraphic( playImage );
		togglePlayButton.setPrefSize( 42, 35 );
		togglePlayButton.setMinSize( 42, 35 );
		togglePlayButton.setMaxSize( 42, 35 );
		
		ImageView previousImage = null;
		try {
			previousImage = new ImageView ( new Image( new FileInputStream ( ROOT.resolve( "resources/previous.png" ).toFile() ) ) );
			previousImage.setFitHeight( 18 );
			previousImage.setFitWidth( 18 );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "Unable to load previous icon: resources/previous.png" );
		}
		
		Button previousButton = new Button ( "" );
		previousButton.setGraphic( previousImage );
		previousButton.setPrefSize( 42, 35 );
		previousButton.setMinSize( 42, 35 );
		previousButton.setMaxSize( 42, 35 );
		
		ImageView nextImage = null;
		try {
			nextImage = new ImageView ( new Image( new FileInputStream ( ROOT.resolve( "resources/next.png" ).toFile() ) ) );
			nextImage.setFitHeight( 18 );
			nextImage.setFitWidth( 18 );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "Unable to load previous icon: resources/next.png" );
		}
		
		Button nextButton = new Button ( "" );
		nextButton.setGraphic( nextImage );
		nextButton.setPrefSize( 42, 35 );
		nextButton.setMinSize( 42, 35 );
		nextButton.setMaxSize( 42, 35 );
		
		ImageView stopImage = null;
		try {
			stopImage = new ImageView ( new Image( new FileInputStream ( ROOT.resolve( "resources/stop.png" ).toFile() ) ) );
			stopImage.setFitHeight( 18 );
			stopImage.setFitWidth( 18 );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "Unable to load previous icon: resources/stop.png" );
		}
		
		Button stopButton = new Button ( "" );
		stopButton.setGraphic( stopImage );
		stopButton.setPrefSize( 42, 35 );
		stopButton.setMinSize( 42, 35 );
		nextButton.setMaxSize( 42, 35 );
		
		previousButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				previousTrack();
			}
		} );

		nextButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				nextTrack();
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
						currentPlayer.seekPercent( trackPositionSlider.getValue() / trackPositionSlider.getMax() );
					}
				}
			}
		});

		trackPositionSlider.setOnMousePressed( ( MouseEvent e ) -> {
			sliderMouseHeld = true;
		} );

		trackPositionSlider.setOnMouseReleased( ( MouseEvent e ) -> {
			sliderMouseHeld = false;
			if ( currentPlayer != null ) {
				currentPlayer.seekPercent( trackPositionSlider.getValue() / trackPositionSlider.getMax() );
			}
		});
		

		Label volumeLabel = new Label ( "🔊" );
		volumeLabel.getStyleClass().add( "volumeLabel" );
		volumeLabel.setMinWidth( 30 );
		
		Slider volumeSlider = new Slider();
		volumeSlider.setMin( 0 );
		volumeSlider.setMax( 100 );
		volumeSlider.setValue( 100 );
		volumeSlider.setPrefWidth( 100 );
		
		volumeSlider.setOnMouseDragged( ( MouseEvent e ) -> {
			if ( currentPlayer != null ) {
				double min = volumeSlider.getMin();
				double max = volumeSlider.getMax();
				double percent = (volumeSlider.getValue() - min) / (max - min);
				currentPlayer.setVolumePercent( percent ); //Note: this works because min is 0 
				
				if ( percent == 0 ) {
					volumeLabel.setText ( "🔇" );
				} else if ( percent > 0 && percent <= .33f ) {
					volumeLabel.setText ( "🔈" );
				} else if ( percent > .33f && percent <= .66f ) {
					volumeLabel.setText ( "🔉" );
				} else if ( percent > .66f ) {
					volumeLabel.setText ( "🔊" );
				}
			}
		});
		
		HBox volumePane = new HBox();
		volumePane.getChildren().addAll( volumeLabel, volumeSlider );
		volumePane.setAlignment( Pos.CENTER );
		volumePane.setSpacing( 5 );
		
		HBox positionSliderPane = new HBox();
		positionSliderPane.getChildren().addAll( timeElapsedLabel, trackPositionSlider, timeRemainingLabel );
		positionSliderPane.setAlignment( Pos.CENTER );
		positionSliderPane.setSpacing( 5 );

		HBox trackControls = new HBox();
		trackControls.getChildren().addAll( previousButton, togglePlayButton, stopButton, nextButton );
		trackControls.setPadding( new Insets( 5 ) );
		trackControls.setSpacing( 5 );
		
		VBox whatever = new VBox(); //TODO: Rename me
		whatever.getChildren().addAll ( volumePane, trackControls );

		HBox controls = new HBox();
		controls.getChildren().addAll( whatever, positionSliderPane, volumePane );
		controls.setSpacing( 10 );
		controls.setAlignment( Pos.CENTER );

		HBox playingTrackInfo = new HBox();
		trackInfo = new Label( "" );
		trackInfo.getStyleClass().add( "trackInfo" );
			
		playingTrackInfo.getChildren().add( trackInfo );
		playingTrackInfo.setAlignment( Pos.CENTER );

		transport = new VBox();
		transport.getChildren().add( playingTrackInfo );
		transport.getChildren().add( controls );
		transport.setPadding( new Insets( 10, 0, 10, 0 ) );
		transport.setSpacing( 5 );
	}
	
	public void setupHistoryWindow () {
		historyWindow = new Stage();
		historyWindow.initModality( Modality.NONE );
		historyWindow.initOwner( mainStage );
		historyWindow.setTitle( "History" );
		historyWindow.setWidth( 600 );
		historyWindow.setHeight ( 400 );
		Pane root = new Pane();
		Scene scene = new Scene( root );
		VBox primaryPane = new VBox();

		historyTable = new TableView();
		Label emptyLabel = new Label( "History is empty." );
		emptyLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyLabel.setWrapText( true );
		emptyLabel.setTextAlignment( TextAlignment.CENTER );

		historyTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		historyTable.setPlaceholder( emptyLabel );
		historyTable.setItems( history );
		historyTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		historyTable.setOnKeyPressed( new EventHandler <KeyEvent>() {
			@Override
			public void handle ( final KeyEvent keyEvent ) {
				if ( keyEvent.getCode().equals( KeyCode.DELETE ) ) {
					ObservableList <Integer> indexes = historyTable.getSelectionModel().getSelectedIndices();
					for ( int index : indexes ) { //TODO: removeAll
						Queue.remove( index );
					}
				}
			}
		});

		TableColumn numberColumn = new TableColumn( "#" );
		TableColumn artistColumn = new TableColumn( "Artist" );
		TableColumn albumColumn = new TableColumn( "Album" );
		TableColumn titleColumn = new TableColumn( "Title" );
		
		numberColumn.setMaxWidth( 10000 );
		artistColumn.setMaxWidth( 30000 );
		albumColumn.setMaxWidth ( 30000 );
		titleColumn.setMaxWidth ( 30000 );
		
		ContextMenu trackContextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem apendMenuItem = new MenuItem( "Append" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		MenuItem removeMenuItem = new MenuItem( "Remove from History" );
		trackContextMenu.getItems().addAll( 
			playMenuItem, apendMenuItem, enqueueMenuItem,
			editTagMenuItem, browseMenuItem, addToPlaylistMenuItem, removeMenuItem 
		);
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		historyTable.setRowFactory( tv -> {
			TableRow <Track> row = new TableRow <>();
			row.setContextMenu( trackContextMenu );
			
			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					Track playMe = historyTable.getSelectionModel().getSelectedItem();
					try {
						CurrentListTrack addMe = new CurrentListTrack ( playMe );
						currentList.clear();
						currentList.add( addMe );
						playTrack( addMe );
						listInfoUpdater.tracksLoaded ( );
					} catch ( IOException e ) {
						LOGGER.log( Level.INFO, e.getMessage() + "\nError opening file from history: " + playMe.getPath() );
						notifyUserError( "Cannot play.\n\n" + e.getMessage() );
					}
				}
			});
			
			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					ArrayList <Integer> indices = new ArrayList <Integer>( historyTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( historyTable.getSelectionModel().getSelectedItems() );
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, null, DragSource.HISTORY );
					Dragboard db = row.startDragAndDrop( TransferMode.COPY );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( DRAGGED_TRACKS, dragObject );
					db.setContent( cc );
					event.consume();

				}
			} );
			
			return row;
		});

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				promptAndSavePlaylist ( historyTable.getSelectionModel().getSelectedItems(), false );
			}
		});

		EventHandler addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				addToPlaylist ( historyTable.getSelectionModel().getSelectedItems(), playlist );
			}
		};
		
		//TODO: I don't know if this is right; 
		Library.playlistsSorted.addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		});

		updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				loadTracks( historyTable.getSelectionModel().getSelectedItems() );
			}
		});
		
		enqueueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Queue.addAllTracks( historyTable.getSelectionModel().getSelectedItems() );
			}
		});

		apendMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				//TODO: have currentList do this conversion for us
				currentList.addAll( Utils.convertTrackList( historyTable.getSelectionModel().getSelectedItems() ) );
				listInfoUpdater.tracksAdded();
			}
		});
		
		editTagMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List<Track> tracks = historyTable.getSelectionModel().getSelectedItems();
				
				tagWindow.setTracks( tracks, null );
				tagWindow.show();
			}
		});
		

		removeMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				synchronized ( history ) {
					List<Integer> selectedIndices = historyTable.getSelectionModel().getSelectedIndices();
					
					ArrayList<Integer> removeMeIndices = new ArrayList ( selectedIndices );
					
					for ( int k = removeMeIndices.size() - 1; k >= 0 ; k-- ) {
						history.remove( removeMeIndices.get( k ).intValue() );
					}
				}
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
							Track selectedTrack = historyTable.getSelectionModel().getSelectedItem();
							if ( selectedTrack != null ) {
								Desktop.getDesktop().open( selectedTrack.getPath().getParent().toFile() );
							}
						} catch ( IOException e ) {
							e.printStackTrace();
						}
					}
				} );
			}
		} );
		
		
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
		artistColumn.setSortable(false);
		albumColumn.setSortable(false);
		titleColumn.setSortable(false);
		
		artistColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Artist" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Title" ) );
		albumColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Album" ) );
		
		historyTable.getColumns().addAll( numberColumn, artistColumn, albumColumn, titleColumn );

		historyTable.prefWidthProperty().bind( root.widthProperty() );
		historyTable.prefHeightProperty().bind( root.heightProperty() );
		
		primaryPane.getChildren().addAll( historyTable );
		root.getChildren().add( primaryPane );
		historyWindow.setScene( scene );
	}
	
	
	public void setupQueueWindow () {
		queueWindow = new Stage();
		queueWindow.initModality( Modality.NONE );
		queueWindow.initOwner( mainStage );
		queueWindow.setTitle( "Queue" );
		queueWindow.setWidth( 500 );
		queueWindow.setHeight ( 400 );
		Pane root = new Pane();
		Scene scene = new Scene( root );

		queueTable = new TableView();
		Label emptyLabel = new Label( "Queue is empty." );
		emptyLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyLabel.setWrapText( true );
		emptyLabel.setTextAlignment( TextAlignment.CENTER );

		queueTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		queueTable.setPlaceholder( emptyLabel );
		queueTable.setItems( Queue.getData() );
		
		Queue.getData().addListener( new ListChangeListener () {
			@Override
			public void onChanged ( Change arg0 ) {
				if ( Queue.isEmpty() ) {
					showQueueButton.getStyleClass().removeAll ( "queueActive" );
				} else {
					showQueueButton.getStyleClass().add ( "queueActive" );
				}
			} 
		});
		
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
		
		queueTable.setOnKeyPressed( keyEvent -> {

			if ( keyEvent.getCode().equals( KeyCode.DELETE ) ) {
				ObservableList <Integer> selectedIndexes = queueTable.getSelectionModel().getSelectedIndices();
				
				List<Integer> removeMe = new ArrayList ( selectedIndexes );
				
				if ( !removeMe.isEmpty() ) {
	
					int selectAfterDelete = selectedIndexes.get( 0 ) - 1;
					for ( int k = removeMe.size() - 1; k >= 0; k-- ) {
						queueTable.getItems().remove ( removeMe.get( k ).intValue() );
					}
					queueTable.getSelectionModel().clearAndSelect( selectAfterDelete );
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
		
		
		artistColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Artist" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Title" ) );
		
		queueTable.getColumns().addAll( numberColumn, artistColumn, titleColumn );
		
		
		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem apendMenuItem = new MenuItem( "Append" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		MenuItem cropMenuItem = new MenuItem( "Crop" );
		MenuItem removeMenuItem = new MenuItem( "Remove from Queue" );
		contextMenu.getItems().addAll( 
			playMenuItem, apendMenuItem, editTagMenuItem, browseMenuItem, addToPlaylistMenuItem, cropMenuItem, removeMenuItem 
		);
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		queueTable.setRowFactory( tv -> {
			TableRow <Track> row = new TableRow <>();
			row.setContextMenu( contextMenu );
			
			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					
					Track playMe = queueTable.getSelectionModel().getSelectedItem();
					try {
						CurrentListTrack addMe = new CurrentListTrack ( playMe );
						currentList.clear();
						currentList.add( addMe );
						playTrack( addMe );
						listInfoUpdater.tracksLoaded( );
					} catch ( IOException e ) {
						LOGGER.log( Level.INFO, e.getMessage() + "\nError opening file from queue: " + playMe.getPath() );
						notifyUserError( "Cannot play.\n\n" + e.getMessage() );
					}
				}
			} );
			
			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					ArrayList <Integer> indices = new ArrayList <Integer>( queueTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( queueTable.getSelectionModel().getSelectedItems() );
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, null, DragSource.QUEUE );
					Dragboard db = row.startDragAndDrop( TransferMode.COPY );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( DRAGGED_TRACKS, dragObject );
					db.setContent( cc );
					event.consume();
				}
			});
			
			row.setOnDragOver( event -> {

				Dragboard db = event.getDragboard();
				if (  db.hasContent( DRAGGED_TRACKS ) || db.hasFiles() ) {
					event.acceptTransferModes( TransferMode.COPY );
					event.consume();
				}
			} );

			row.setOnDragDropped( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasContent( DRAGGED_TRACKS ) ) {
					
					DraggedTrackContainer container = (DraggedTrackContainer) db.getContent( DRAGGED_TRACKS );
					List <Integer> draggedIndices = container.getIndices();
					int dropIndex = row.isEmpty() ? dropIndex = queueTable.getItems().size() : row.getIndex();
					
					switch ( container.getSource() ) {
						case ALBUM_LIST:
						case PLAYLIST_LIST:
						case HISTORY: 
						case ALBUM_INFO:
						case TRACK_LIST: {
							List <Track> tracksToCopy = container.getTracks();
							Queue.addAllTracks( dropIndex, tracksToCopy );
							
						} break;
						case CURRENT_LIST: {
							synchronized ( currentList ) {
								ArrayList <CurrentListTrack> tracksToCopy = new ArrayList <CurrentListTrack> (  );
								for ( int index : draggedIndices ) {
									if ( index >= 0 && index < currentList.size() ) {
										tracksToCopy.add( currentList.get( index ) );
									}
								}
								Queue.addAllTracks( dropIndex, tracksToCopy );
							}
						} break;
						
												
						case QUEUE: {
							ArrayList <Track> tracksToMove = new ArrayList <Track> ( draggedIndices.size() );
							for ( int index : draggedIndices ) {
								if ( index >= 0 && index < queueTable.getItems().size() ) {
									tracksToMove.add( queueTable.getItems().get( index ) );
								}
							}
							
							for ( int k = draggedIndices.size() - 1; k >= 0; k-- ) {
								int index = draggedIndices.get( k ).intValue();
								if ( index >= 0 && index < queueTable.getItems().size() ) {
									Queue.remove ( index );
								}
							}
							
							dropIndex = Math.min( Queue.size(), row.getIndex() );
							
							Queue.addAllTracks( dropIndex, tracksToMove );
							
							queueTable.getSelectionModel().clearSelection();
							for ( int k = 0; k < draggedIndices.size(); k++ ) {
								queueTable.getSelectionModel().select( dropIndex + k );
							}
							
							Queue.updateQueueIndexes( null );
							
						} break;
					}

					Queue.updateQueueIndexes( null );
					event.setDropCompleted( true );
					event.consume();

				} else if ( db.hasFiles() ) {
					ArrayList <Track> tracksToAdd = new ArrayList();
					for ( File file : db.getFiles() ) {
						Path droppedPath = Paths.get( file.getAbsolutePath() );
						if ( Utils.isMusicFile( droppedPath ) ) {
							try {
								tracksToAdd.add( new Track( droppedPath ) );
							} catch ( IOException e ) {
								e.printStackTrace();
							}
						} else if ( Files.isDirectory( droppedPath ) ) {
							tracksToAdd.addAll( Utils.getAllTracksInDirectory( droppedPath ) );
						}
					}
					if ( !tracksToAdd.isEmpty() ) {
						int dropIndex = row.isEmpty() ? dropIndex = currentList.size() : row.getIndex();
						queueTable.getItems().addAll( Math.min( dropIndex, currentList.size() ), tracksToAdd );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});
			
			return row;
		});
		
		queueTable.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();

			if ( db.hasContent( DRAGGED_TRACKS ) || db.hasFiles() ) {

				event.acceptTransferModes( TransferMode.COPY );
				event.consume();

			}
		} );

		queueTable.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasContent( DRAGGED_TRACKS ) ) {
				
				DraggedTrackContainer container = (DraggedTrackContainer) db.getContent( DRAGGED_TRACKS );
				List <Integer> draggedIndices = container.getIndices();
				
				switch ( container.getSource() ) {

					case ALBUM_LIST:
					case PLAYLIST_LIST:
					case HISTORY: 
					case ALBUM_INFO:
					case TRACK_LIST: {
						List <Track> tracksToCopy = container.getTracks();
						Queue.addAllTracks( tracksToCopy );
						
					} break;
					case CURRENT_LIST: {
						synchronized ( currentList ) {
							ArrayList <CurrentListTrack> tracksToCopy = new ArrayList <CurrentListTrack> (  );
							for ( int index : draggedIndices ) {
								if ( index >= 0 && index < currentList.size() ) {
									tracksToCopy.add( currentList.get( index ) );
								}
							}
							Queue.addAllTracks( tracksToCopy );
						}
					} break;
					
											
					case QUEUE: {
						ArrayList <Track> tracksToMove = new ArrayList <Track> ( draggedIndices.size() );
						for ( int index : draggedIndices ) {
							if ( index >= 0 && index < queueTable.getItems().size() ) {
								tracksToMove.add( queueTable.getItems().get( index ) );
							}
						}
						
						for ( int k = draggedIndices.size() - 1; k >= 0; k-- ) {
							int index = draggedIndices.get( k ).intValue();
							if ( index >= 0 && index < queueTable.getItems().size() ) {
								Queue.remove ( index );
							}
						}
						
						Queue.addAllTracks( tracksToMove );
						
						queueTable.getSelectionModel().clearSelection();
						for ( int k = 0; k < draggedIndices.size(); k++ ) {
							queueTable.getSelectionModel().select( k );
						}
						
						Queue.updateQueueIndexes( null );
						
					} break;
				}

				Queue.updateQueueIndexes( null );
				event.setDropCompleted( true );
				event.consume();

			} else if ( db.hasFiles() ) {
				ArrayList <Track> tracksToAdd = new ArrayList();
				for ( File file : db.getFiles() ) {
					Path droppedPath = Paths.get( file.getAbsolutePath() );
					if ( Utils.isMusicFile( droppedPath ) ) {
						try {
							tracksToAdd.add( new Track( droppedPath ) );
						} catch ( IOException e ) {
							e.printStackTrace();
						}
					} else if ( Files.isDirectory( droppedPath ) ) {
						tracksToAdd.addAll( Utils.getAllTracksInDirectory( droppedPath ) );
					}
				}
				if ( !tracksToAdd.isEmpty() ) {
					queueTable.getItems().addAll( tracksToAdd );
				}

				event.setDropCompleted( true );
				event.consume();
			}

		} );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				promptAndSavePlaylist ( queueTable.getSelectionModel().getSelectedItems(), false );
			}
		});

		EventHandler addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				addToPlaylist ( queueTable.getSelectionModel().getSelectedItems(), playlist );
			}
		};
		
		Library.playlistsSorted.addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		});

		updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				loadTracks( queueTable.getSelectionModel().getSelectedItems() );
			}
		});

		apendMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				currentList.addAll( Utils.convertTrackList( queueTable.getSelectionModel().getSelectedItems() ) );
				listInfoUpdater.tracksAdded();
			}
		});
		
		editTagMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List<Track> tracks = queueTable.getSelectionModel().getSelectedItems();
				
				tagWindow.setTracks( tracks, null );
				tagWindow.show();
			}
		});

		cropMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {

				ObservableList <Integer> selectedIndexes = queueTable.getSelectionModel().getSelectedIndices();
				
				ArrayList <Integer> removeMe = new ArrayList<Integer> ();
				
				for ( int k = 0; k < queueTable.getItems().size(); k++ ) {
					if ( !selectedIndexes.contains( k ) ) {
						removeMe.add ( k );
					}
				}
				
				if ( !removeMe.isEmpty() ) {
					for ( int k = removeMe.size() - 1; k >= 0; k-- ) {
						Queue.remove ( removeMe.get( k ).intValue() );
					}

					queueTable.getSelectionModel().clearSelection();
				}
			}
		} );


		removeMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				synchronized ( history ) {
					List<Integer> selectedIndices = queueTable.getSelectionModel().getSelectedIndices();
					
					ArrayList<Integer> removeMeIndices = new ArrayList ( selectedIndices );
					
					for ( int k = removeMeIndices.size() - 1; k >= 0 ; k-- ) {
						Queue.remove( removeMeIndices.get( k ).intValue() );
					}
				}
			}
		});
		

		queueTable.prefWidthProperty().bind( root.widthProperty() );
		queueTable.prefHeightProperty().bind( root.heightProperty() );
		
		root.getChildren().add( queueTable );
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
				event.acceptTransferModes( TransferMode.COPY );
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
	
	public void setupAlbumImage () {
		albumImage = new BorderPane();
	}

	public void setupArtistImage () {
		artistImage = new BorderPane();
	}

	public static void setAlbumImage ( Image image ) {
		try {
			ResizableImageView view = new ResizableImageView( image );
			view.setPreserveRatio( true );
			albumImage.setCenter( view );
		} catch ( Exception e ) {
			albumImage.setCenter( null );
		}
	}

	public static void setArtistImage ( Image image ) {
		try {
			ResizableImageView view = new ResizableImageView( image );
			view.setPreserveRatio( true );
			artistImage.setCenter( view );
		} catch ( Exception e ) {
			artistImage.setCenter( null );
		}
	}
	
	//TODO: This function probably belongs in Library
	public static void addToPlaylist ( List <Track> tracks, Playlist playlist ) {
		playlist.getTracks().addAll( tracks );
		if ( currentPlaylist != null && currentPlaylist.getName().equals( playlist.getName() ) ) {
			currentList.addAll( Utils.convertTrackList( tracks ) );
			listInfoUpdater.playlistLoaded( playlist );
		}
		playlistTable.refresh(); 
	}
	
	public static void promptAndSavePlaylist ( List <Track> tracks, boolean isCurrentList ) { 
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
			String enteredName = result.get().trim();

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
				listInfoUpdater.playlistLoaded( currentPlaylist );
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
			String enteredName = result.get().trim();

			Library.removePlaylist( playlist );
			playlist.setName ( enteredName );
			Library.addPlaylist( playlist );
			
			listInfoUpdater.playlistLoaded( playlist );
		}
	}

	public void setupCurrentListControlPane () {

		toggleRepeatButton = new Button( repeatMode.getSymbol() );
		toggleShuffleButton = new Button( shuffleMode.getSymbol() );
		showQueueButton = new Button ( "Q" );
		Button showHistoryButton = new Button ( "H" );
		Button loadTracksButton = new Button( "⏏" );
		Button savePlaylistButton = new Button( "💾" );
		Button clearButton = new Button ( "✘" );

		toggleRepeatButton.setMinSize( Button.USE_PREF_SIZE, Button.USE_PREF_SIZE );
		toggleShuffleButton.setMinSize( Button.USE_PREF_SIZE, Button.USE_PREF_SIZE );
		showQueueButton.setMinSize( Button.USE_PREF_SIZE, Button.USE_PREF_SIZE );
		showHistoryButton.setMinSize( Button.USE_PREF_SIZE, Button.USE_PREF_SIZE );;
		loadTracksButton.setMinSize( Button.USE_PREF_SIZE, Button.USE_PREF_SIZE );
		savePlaylistButton.setMinSize( Button.USE_PREF_SIZE, Button.USE_PREF_SIZE );
		clearButton.setMinSize( Button.USE_PREF_SIZE, Button.USE_PREF_SIZE );
		
		toggleRepeatButton.setTooltip( new Tooltip( "Toggle Repeat Type" ) );
		toggleShuffleButton.setTooltip( new Tooltip( "Toggle Shuffle" ) );
		showQueueButton.setTooltip( new Tooltip( "Show Queue" ) );
		showHistoryButton.setTooltip( new Tooltip( "Show Play History" ) );
		loadTracksButton.setTooltip( new Tooltip( "Load tracks from the filesystem" ) );
		savePlaylistButton.setTooltip( new Tooltip( "Save this playlist" ) );
		clearButton.setTooltip( new Tooltip( "Clear the current list" ) );
		
		showQueueButton.setOnAction ( new EventHandler <ActionEvent>() {
			public void handle ( ActionEvent e ) {
				queueWindow.show();
			}
		});
		
		showHistoryButton.setOnAction ( new EventHandler <ActionEvent>() {
			public void handle ( ActionEvent e ) {
				historyWindow.show();
			}
		});

		savePlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				promptAndSavePlaylist( new ArrayList <Track>( currentList ), true );
			}
		});
		
		clearButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				currentList.clear();
				listInfoUpdater.listCleared();
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
					try {
						tracks.add( new Track ( file.toPath() ) );
					} catch ( IOException ioex ) {
						LOGGER.log( Level.INFO, "Unable to load track", ioex );
					}
				}
				
				loadTracks ( tracks );
					
			}
		});

		toggleRepeatButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				if ( repeatMode == RepeatMode.PLAY_ONCE ) {
					repeatMode = RepeatMode.REPEAT;
				} else if ( repeatMode == RepeatMode.REPEAT ) {
					repeatMode = RepeatMode.REPEAT_ONE_TRACK;
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
				Playlist playlist = new Playlist( playlistName, new ArrayList <Track>( currentList ) );
				Library.addPlaylist( playlist );
			}
		};

		playlistControls = new HBox();
		playlistControls.setAlignment( Pos.CENTER_RIGHT );
		
		Label currentPlayingListInfo = new Label ( "" );
		currentPlayingListInfo.setAlignment( Pos.CENTER );
		currentPlayingListInfo.prefWidthProperty().bind( playlistControls.widthProperty() );
		listInfoUpdater = new ListInfoUpdater ( currentPlayingListInfo );

		playlistControls.getChildren().addAll( toggleRepeatButton, toggleShuffleButton, showQueueButton, showHistoryButton,
				currentPlayingListInfo, loadTracksButton, savePlaylistButton, clearButton );
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
		
		Button settingsButton = new Button( "+" );
		settingsButton.setMinSize( Button.USE_PREF_SIZE, Button.USE_PREF_SIZE );
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
		clearButton.setMinSize( Button.USE_PREF_SIZE, Button.USE_PREF_SIZE );
		clearButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				filterBox.setText( "" );
			}
		});
		

		settingsButton.setTooltip( new Tooltip( "Add or Remove Music Folders" ) );
		filterBox.setTooltip ( new Tooltip ( "Filter/Search playlists" ) );
		clearButton.setTooltip( new Tooltip( "Clear the filter text" ) );

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
		
		Button settingsButton = new Button( "+" );
		settingsButton.setMinSize( Button.USE_PREF_SIZE, Button.USE_PREF_SIZE );
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
		clearButton.setMinSize( Button.USE_PREF_SIZE, Button.USE_PREF_SIZE );
		clearButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				trackFilterBox.setText( "" );
			}
		});

		settingsButton.setTooltip( new Tooltip( "Add or Remove Music Folders" ) );
		trackFilterBox.setTooltip ( new Tooltip ( "Filter/Search tracks" ) );
		clearButton.setTooltip( new Tooltip( "Clear the filter text" ) );
		
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
		matchableText.add( Normalizer.normalize( track.getFullAlbumTitle(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
		matchableText.add( track.getFullAlbumTitle().toLowerCase() );

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
				matchableText.add( Normalizer.normalize( album.getFullTitle(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
				matchableText.add( album.getFullTitle().toLowerCase() );
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

		Button settingsButton = new Button( "+" );
		settingsButton.setMinSize( Button.USE_PREF_SIZE, Button.USE_PREF_SIZE );
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

		Button clearButton = new Button( "✘" );
		clearButton.setMinSize( Button.USE_PREF_SIZE, Button.USE_PREF_SIZE );
		clearButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				filterBox.setText( "" );
			}
		});
		

		settingsButton.setTooltip( new Tooltip( "Add or Remove Music Folders" ) );
		filterBox.setTooltip ( new Tooltip ( "Filter/Search albums" ) );
		clearButton.setTooltip( new Tooltip( "Clear the filter text" ) );

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
		
		trackListCheckBox.setTooltip( new Tooltip( "" ) );
	}

	public void setupAlbumTable () {
		TableColumn artistColumn = new TableColumn( "Artist" );
		TableColumn yearColumn = new TableColumn( "Year" );
		TableColumn albumColumn = new TableColumn( "Album" );

		artistColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "albumArtist" ) );
		yearColumn.setCellValueFactory( new PropertyValueFactory <Album, Integer>( "year" ) );
		albumColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "fullTitle" ) );

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
		
		emptyAlbumListLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyAlbumListLabel.setWrapText( true );
		emptyAlbumListLabel.setTextAlignment( TextAlignment.CENTER );
		
		filteredAlbumListLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		filteredAlbumListLabel.setWrapText( true );
		filteredAlbumListLabel.setTextAlignment( TextAlignment.CENTER );
		
		albumTable.setPlaceholder( emptyAlbumListLabel );

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem apendMenuItem = new MenuItem( "Append" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		MenuItem infoMenuItem = new MenuItem( "Info" );
		
		contextMenu.getItems().addAll( 
			playMenuItem, apendMenuItem, enqueueMenuItem, editTagMenuItem, 
			browseMenuItem, addToPlaylistMenuItem, infoMenuItem
		);
		
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
		
		playMenuItem.setOnAction( event -> {
			//TODO: Handle multiple selections
			playAlbums( albumTable.getSelectionModel().getSelectedItems() );
		});

		apendMenuItem.setOnAction( event -> {
			//TODO: Handle multiple selections
			appendAlbum( albumTable.getSelectionModel().getSelectedItem() );
		});

		enqueueMenuItem.setOnAction( event -> {
			Queue.addAllAlbums( albumTable.getSelectionModel().getSelectedItems() );
		});
		
		editTagMenuItem.setOnAction( event -> {
			List<Album> albums = albumTable.getSelectionModel().getSelectedItems();
			ArrayList<Track> editMe = new ArrayList<Track>();
			
			for ( Album album : albums ) {
				editMe.addAll( album.getTracks() );
			}
			
			tagWindow.setTracks( editMe, albums, FieldKey.TRACK, FieldKey.TITLE );
			tagWindow.show();
		});
		
		infoMenuItem.setOnAction( event -> {
			albumInfoWindow.setAlbum( albumTable.getSelectionModel().getSelectedItem() );
			albumInfoWindow.show();
		});

		browseMenuItem.setOnAction( event -> {
			SwingUtilities.invokeLater( new Runnable() {
				public void run () {
					try {
						Desktop.getDesktop().open( albumTable.getSelectionModel().getSelectedItem().getPath().toFile() );
					} catch ( IOException e ) {
						e.printStackTrace();
					}
				}
			});
		});
		
		albumTable.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				event.acceptTransferModes( TransferMode.COPY );
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
					event.acceptTransferModes( TransferMode.COPY );
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
					
					ArrayList <Integer> indices = new ArrayList <Integer>( albumTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Album> albums = new ArrayList <Album>( albumTable.getSelectionModel().getSelectedItems() );
					
					ArrayList <Track> tracks = new ArrayList <Track> ();
					
					for ( Album album : albums ) {
						if ( album != null ) {
							tracks.addAll( album.getTracks() );
						}
					}
					
					DraggedTrackContainer dragObject = new DraggedTrackContainer( null, tracks, albums, DragSource.ALBUM_LIST );
					Dragboard db = row.startDragAndDrop( TransferMode.COPY );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( DRAGGED_TRACKS, dragObject );
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
		TableColumn trackColumn = new TableColumn( "#" );
		TableColumn albumColumn = new TableColumn( "Album" );
		TableColumn titleColumn = new TableColumn( "Title" );

		artistColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Artist" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Title" ) );
		lengthColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "LengthDisplay" ) );
		trackColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "TrackNumber" ) );
		albumColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "fullAlbumTitle" ) );

		artistColumn.setSortType( TableColumn.SortType.ASCENDING );

		artistColumn.setMaxWidth( 45000 );
		titleColumn.setMaxWidth( 45000 );
		lengthColumn.setMaxWidth( 15000 );
		albumColumn.setMaxWidth( 45000 );
		trackColumn.setMaxWidth( 15000 );

		trackColumn.setCellFactory( column -> {
			return new TableCell <Track, Integer>() {
				@Override
				protected void updateItem ( Integer value, boolean empty ) {
					super.updateItem( value, empty );

					if ( value == null || value.equals( Track.NO_TRACK_NUMBER ) || empty ) {
						setText( null );
					} else {
						setText( value.toString() );
					}
				}
			};
		} );
		trackTable = new TableView();
		trackTable.getColumns().addAll( artistColumn, albumColumn, trackColumn, titleColumn, lengthColumn );
		trackTable.setEditable( false );
		trackTable.setItems( Library.tracksSorted );

		Library.tracksSorted.comparatorProperty().bind( trackTable.comparatorProperty() );
		
		trackTable.getSelectionModel().clearSelection();
		trackTable.getSortOrder().add( artistColumn );
		trackTable.getSortOrder().add( albumColumn );
		trackTable.getSortOrder().add( trackColumn );
		FixedWidthCustomResizePolicy resizePolicy = new FixedWidthCustomResizePolicy();
		// TODO resizePolicy.registerColumns ( lengthColumn );
		trackTable.setColumnResizePolicy( resizePolicy );
		
		emptyTrackListLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyTrackListLabel.setWrapText( true );
		emptyTrackListLabel.setTextAlignment( TextAlignment.CENTER );
		trackTable.setPlaceholder( emptyTrackListLabel );
		
		trackTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		ContextMenu trackContextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem apendMenuItem = new MenuItem( "Append" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		trackContextMenu.getItems().addAll( playMenuItem, apendMenuItem, enqueueMenuItem, editTagMenuItem, browseMenuItem, addToPlaylistMenuItem );
		
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
				loadTracks( trackTable.getSelectionModel().getSelectedItems() );
			}
		} );

		apendMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				currentList.addAll( Utils.convertTrackList( trackTable.getSelectionModel().getSelectedItems() ) );
				listInfoUpdater.tracksAdded();
			}
		} );
		
		enqueueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Queue.addAllTracks( trackTable.getSelectionModel().getSelectedItems() );
			}
		});
		
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
							Track selectedTrack = trackTable.getSelectionModel().getSelectedItem();
							if ( selectedTrack != null ) {
								Desktop.getDesktop().open( trackTable.getSelectionModel().getSelectedItem().getPath().getParent().toFile() );
							}
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
				event.acceptTransferModes( TransferMode.COPY );
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
			
			row.setContextMenu( trackContextMenu );
			
			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					
					Track playMe = trackTable.getSelectionModel().getSelectedItem();
					try {
						CurrentListTrack addMe = new CurrentListTrack ( playMe );
						currentList.clear();
						currentList.add( addMe );
						playTrack( addMe );
						listInfoUpdater.tracksLoaded ( );
					} catch ( IOException e ) {
						LOGGER.log( Level.INFO, e.getMessage() + "\nError opening file from track list: " + playMe.getPath() );
						notifyUserError( e.getMessage() + "\n\nUnable to play." );
					}
				}
			} );
			
			row.setOnDragOver( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasFiles() ) {
					event.acceptTransferModes( TransferMode.COPY );
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
					
					ArrayList <Integer> indices = new ArrayList <Integer>( trackTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( trackTable.getSelectionModel().getSelectedItems() );
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, null, DragSource.TRACK_LIST );
					Dragboard db = row.startDragAndDrop( TransferMode.COPY );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( DRAGGED_TRACKS, dragObject );
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
		playlistTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		playlistTable.setItems( Library.playlistsSorted );

		Library.playlistsSorted.comparatorProperty().bind( playlistTable.comparatorProperty() );

		playlistTable.getSortOrder().add( nameColumn );
		playlistTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		
		FixedWidthCustomResizePolicy resizePolicy = new FixedWidthCustomResizePolicy();
		resizePolicy.registerColumns( tracksColumn );
		playlistTable.setColumnResizePolicy( resizePolicy );

		emptyPlaylistLabel.setWrapText( true );
		emptyPlaylistLabel.setTextAlignment( TextAlignment.CENTER );
		emptyPlaylistLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		playlistTable.setPlaceholder( emptyPlaylistLabel );

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem appendMenuItem = new MenuItem( "Append" );		
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem renameMenuItem = new MenuItem( "Rename" );
		MenuItem infoMenuItem = new MenuItem( "Info" );
		MenuItem removeMenuItem = new MenuItem( "Remove" );
		contextMenu.getItems().addAll( playMenuItem, appendMenuItem, enqueueMenuItem, renameMenuItem, infoMenuItem, removeMenuItem );

		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				playPlaylists( playlistTable.getSelectionModel().getSelectedItems() );
			}
		});

		appendMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				currentList.addAll( Utils.convertTrackList( playlistTable.getSelectionModel().getSelectedItem().getTracks() ) );
				listInfoUpdater.tracksAdded();
			}
		});
		
		enqueueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Queue.addAllPlaylists( playlistTable.getSelectionModel().getSelectedItems() );
			}
		});
		
		renameMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				promptAndRenamePlaylist ( playlistTable.getSelectionModel().getSelectedItem() );
			}
		});
		
		infoMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				playlistInfoWindow.setPlaylist ( playlistTable.getSelectionModel().getSelectedItem() );
				playlistInfoWindow.show();
			}
		});

		removeMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Library.removePlaylist( playlistTable.getSelectionModel().getSelectedItem() );
			}
		});

		playlistTable.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				//TODO: I can check for file extensions...
				event.acceptTransferModes( TransferMode.COPY );
				event.consume();
			}
		});
		
		playlistTable.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				ArrayList <Playlist> playlistsToAdd = new ArrayList <Playlist> ();
				
				for ( File file : db.getFiles() ) {
					Path droppedPath = Paths.get( file.getAbsolutePath() );
					if ( Utils.isPlaylistFile ( droppedPath ) ) {
						Playlist playlist = Playlist.loadPlaylist( droppedPath );
						if ( playlist != null ) {
							playlistsToAdd.add( playlist );
						}
					}
				}
				
				if ( !playlistsToAdd.isEmpty() ) {
					Library.playlists.addAll( playlistsToAdd );
				}

				event.setDropCompleted( true );
				event.consume();
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
					List <Playlist> selectedPlaylists = playlistTable.getSelectionModel().getSelectedItems();
					List <Track> tracks = new ArrayList <Track> ();
					
					for ( Playlist playlist : selectedPlaylists ) {
						tracks.addAll ( playlist.getTracks() );
					}
					
					DraggedTrackContainer dragObject = new DraggedTrackContainer( null, tracks, null, DragSource.PLAYLIST_LIST );
					Dragboard db = row.startDragAndDrop( TransferMode.COPY );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( DRAGGED_TRACKS, dragObject );
					db.setContent( cc );
					event.consume();
				
				}
			});

			row.setOnDragOver( event -> {

				Dragboard db = event.getDragboard();
				if ( db.hasContent( DRAGGED_TRACKS ) ) {
					if ( !row.isEmpty() ) {
						event.acceptTransferModes( TransferMode.COPY );
						event.consume();
					}
				} else if ( db.hasFiles() ) {
					//TODO: I can check for file extensions...
					event.acceptTransferModes( TransferMode.COPY );
					event.consume();
				}
			});

			row.setOnDragDropped( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasContent( DRAGGED_TRACKS ) ) {
					if ( !row.isEmpty() ) {
						DraggedTrackContainer container = (DraggedTrackContainer) db.getContent( DRAGGED_TRACKS );
						Playlist playlist = row.getItem();
						addToPlaylist( container.getTracks(), playlist );
						playlistTable.refresh();
						event.setDropCompleted( true );
						event.consume();
					}
				} else if ( db.hasFiles() ) {
					ArrayList <Playlist> playlistsToAdd = new ArrayList <Playlist> ();
					
					for ( File file : db.getFiles() ) {
						Path droppedPath = Paths.get( file.getAbsolutePath() );
						if ( Utils.isPlaylistFile ( droppedPath ) ) {
							Playlist playlist = Playlist.loadPlaylist( droppedPath );
							if ( playlist != null ) {
								playlistsToAdd.add( playlist );
							}
						}
					}
					
					if ( !playlistsToAdd.isEmpty() ) {
						int dropIndex = row.isEmpty() ? dropIndex = Library.playlists.size() : row.getIndex();
						Library.playlists.addAll( Math.min( dropIndex, Library.playlists.size() ), playlistsToAdd );
					}

					event.setDropCompleted( true );
					event.consume();
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
		albumColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "fullAlbumTitle" ) );
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
		currentListTable.setItems( currentList );

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
			
			if ( db.hasContent( DRAGGED_TRACKS ) || db.hasFiles() ) {
				event.acceptTransferModes( TransferMode.COPY );
				event.consume();
			}
		});

		currentListTable.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();

			if ( db.hasContent( DRAGGED_TRACKS ) ) {
				//TODO: This code is duplicated below. Put it in a function. 

				DraggedTrackContainer container = (DraggedTrackContainer) db.getContent( DRAGGED_TRACKS );
				
				switch ( container.getSource() ) {
					case ALBUM_LIST:
					case PLAYLIST_LIST:
					case TRACK_LIST:
					case ALBUM_INFO:
					case HISTORY: {
						List <Track> tracksToCopy = container.getTracks();
						currentList.addAll( Utils.convertTrackList( tracksToCopy ) );
						if ( container.getAlbums() != null ) {
							listInfoUpdater.albumsLoaded( container.getAlbums() );
						} else {
							listInfoUpdater.tracksLoaded();
						}
					
					} break;
					
					case CURRENT_LIST: {
						//There is no meaning in dragging from an empty list to an empty list. 
					} break;
					
					case QUEUE: {
						synchronized ( queueTable.getItems() ) {
							List <Integer> draggedIndices = container.getIndices();
							ArrayList <CurrentListTrack> tracksToCopy = new ArrayList <CurrentListTrack> ( draggedIndices.size() );
							for ( int index : draggedIndices ) {
								if ( index >= 0 && index < queueTable.getItems().size() ) {
									Track addMe = queueTable.getItems().get( index );
									if ( addMe instanceof CurrentListTrack ) {
										tracksToCopy.add( (CurrentListTrack)addMe );
									} else {
										try {
											CurrentListTrack newAddMe = new CurrentListTrack ( addMe );
											queueTable.getItems().remove ( index );
											queueTable.getItems().add( index, newAddMe );
											tracksToCopy.add( newAddMe );
											
										} catch ( IOException e ) {
											LOGGER.log( Level.WARNING, "Unable to convert queue track to CurrentListTrack, not adding to current list" );
										}
									}
								}
								currentList.addAll( tracksToCopy );
								listInfoUpdater.tracksAdded();
							}
						}
						
					} break;
				}

				Queue.updateQueueIndexes( null );
				event.setDropCompleted( true );
				event.consume();


		
			} else if ( db.hasFiles() ) {
				ArrayList <Track> tracksToAdd = new ArrayList();
				for ( File file : db.getFiles() ) {
					Path droppedPath = Paths.get( file.getAbsolutePath() );
					if ( Utils.isMusicFile( droppedPath ) ) {
						try {
							currentList.add( new CurrentListTrack( droppedPath ) );
							listInfoUpdater.tracksAdded();
						} catch ( IOException e ) {
							e.printStackTrace();
						}
					
					} else if ( Files.isDirectory( droppedPath ) ) {
						currentList.addAll( Utils.convertTrackList( Utils.getAllTracksInDirectory( droppedPath ) ) );
						listInfoUpdater.tracksAdded();
					} else if ( Utils.isPlaylistFile ( droppedPath ) ) {
						Playlist playlist = Playlist.loadPlaylist( droppedPath );
						if ( playlist != null ) {
							currentList.addAll( Utils.convertTrackList( playlist.getTracks() ) );
							listInfoUpdater.tracksAdded();
						}
					}
				}

				event.setDropCompleted( true );
				event.consume();
			}

		} );

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem queueMenuItem = new MenuItem( "Enqueue" );
		MenuItem shuffleMenuItem = new MenuItem( "Shuffle Current List" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem cropMenuItem = new MenuItem( "Crop" );
		MenuItem removeMenuItem = new MenuItem( "Remove" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );

		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );
		
		//TODO: These don't work right
		queueMenuItem.setAccelerator( new KeyCodeCombination ( KeyCode.Q, KeyCombination.SHIFT_ANY ) );
		playMenuItem.setAccelerator( new KeyCodeCombination ( KeyCode.ENTER ) );
		cropMenuItem.setAccelerator( new KeyCodeCombination ( KeyCode.DELETE, KeyCombination.SHIFT_DOWN ) );
		removeMenuItem.setAccelerator( new KeyCodeCombination ( KeyCode.DELETE, KeyCombination.SHIFT_ANY ) );
		contextMenu.getItems().addAll( playMenuItem, queueMenuItem, shuffleMenuItem, editTagMenuItem, browseMenuItem, addToPlaylistMenuItem, cropMenuItem, removeMenuItem );
		
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

		
		queueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Queue.addAllTracks( currentListTable.getSelectionModel().getSelectedItems() );
			}
		});
		
		shuffleMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Collections.shuffle( currentList );
				listInfoUpdater.listReordered();
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

		removeMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {

				ObservableList <Integer> selectedIndexes = currentListTable.getSelectionModel().getSelectedIndices();
				
				List<Integer> removeMe = new ArrayList ( selectedIndexes );
				
				if ( !removeMe.isEmpty() ) {

					int selectAfterDelete = selectedIndexes.get( 0 ) - 1;
					for ( int k = removeMe.size() - 1; k >= 0; k-- ) {
						currentList.remove ( removeMe.get( k ).intValue() );
						if ( currentList.size() == 0 ) {
							listInfoUpdater.listCleared();
						} else {
							listInfoUpdater.tracksRemoved();
						}
					}
					currentListTable.getSelectionModel().clearAndSelect( selectAfterDelete );
				}
			}
		} );
		
		cropMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {

				ObservableList <Integer> selectedIndexes = currentListTable.getSelectionModel().getSelectedIndices();
				
				ArrayList <Integer> removeMe = new ArrayList<Integer> ();
				
				for ( int k = 0; k < currentList.size(); k++ ) {
					if ( !selectedIndexes.contains( k ) ) {
						removeMe.add ( k );
					}
				}
				
				if ( !removeMe.isEmpty() ) {
					for ( int k = removeMe.size() - 1; k >= 0; k-- ) {
						currentList.remove ( removeMe.get( k ).intValue() );
					}

					currentListTable.getSelectionModel().clearSelection();
				}
			}
		} );

		currentListTable.setRowFactory( tv -> {
			TableRow <CurrentListTrack> row = new TableRow <>();

			row.setContextMenu( contextMenu );

			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					playTrack( row.getItem() );
					//TODO: Do we want the error here? 
				}
			} );

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					ArrayList <Integer> indices = new ArrayList <Integer>( currentListTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( currentListTable.getSelectionModel().getSelectedItems() );
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, null, DragSource.CURRENT_LIST );
					Dragboard db = row.startDragAndDrop( TransferMode.COPY );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( DRAGGED_TRACKS, dragObject );
					db.setContent( cc );
					event.consume();
				}
			});

			row.setOnDragOver( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasContent( DRAGGED_TRACKS ) || db.hasFiles() ) {
					event.acceptTransferModes( TransferMode.COPY );
					event.consume();
				}
			} );

			row.setOnDragDropped( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasContent( DRAGGED_TRACKS ) ) {

					DraggedTrackContainer container = (DraggedTrackContainer) db.getContent( DRAGGED_TRACKS );
					int dropIndex = row.isEmpty() ? dropIndex = currentList.size() : row.getIndex();
					
					switch ( container.getSource() ) {
						case ALBUM_LIST:
						case PLAYLIST_LIST:
						case TRACK_LIST:
						case ALBUM_INFO:
						case HISTORY: {
							List <Track> tracksToCopy = container.getTracks();
							currentList.addAll( dropIndex, Utils.convertTrackList( tracksToCopy ) );
							listInfoUpdater.tracksAdded();
						
						} break;
						
						case CURRENT_LIST: {
							List <Integer> draggedIndices = container.getIndices();
							ArrayList <CurrentListTrack> tracksToMove = new ArrayList <CurrentListTrack> ( draggedIndices.size() );
							for ( int index : draggedIndices ) {
								if ( index >= 0 && index < currentList.size() ) {
									tracksToMove.add( currentList.get( index ) );
								}
							}
							
							for ( int k = draggedIndices.size() - 1; k >= 0; k-- ) {
								int index = draggedIndices.get( k ).intValue();
								if ( index >= 0 && index < currentList.size() ) {
									currentList.remove ( index );
								}
							}
							
							dropIndex = Math.min( currentList.size(), row.getIndex() );
							
							currentList.addAll( dropIndex, tracksToMove );
							
							currentListTable.getSelectionModel().clearSelection();
							for ( int k = 0; k < draggedIndices.size(); k++ ) {
								currentListTable.getSelectionModel().select( dropIndex + k );
							}

							listInfoUpdater.listReordered( );
						} break;
						
						case QUEUE: {
							synchronized ( queueTable.getItems() ) {
								List <Integer> draggedIndices = container.getIndices();
								ArrayList <CurrentListTrack> tracksToCopy = new ArrayList <CurrentListTrack> ( draggedIndices.size() );
								for ( int index : draggedIndices ) {
									if ( index >= 0 && index < queueTable.getItems().size() ) {
										Track addMe = queueTable.getItems().get( index );
										if ( addMe instanceof CurrentListTrack ) {
											tracksToCopy.add( (CurrentListTrack)addMe );
										} else {
											try {
												CurrentListTrack newAddMe = new CurrentListTrack ( addMe );
												queueTable.getItems().remove ( index );
												queueTable.getItems().add( index, newAddMe );
												tracksToCopy.add( newAddMe );
												
											} catch ( IOException e1 ) {
												//TODO: Warning or info?
												LOGGER.log( Level.WARNING, "Unable to convert queue track to CurrentListTrack, not adding to current list" );
											}
										}
									}
									currentList.addAll( dropIndex, tracksToCopy );
									listInfoUpdater.tracksAdded();
								}
							}
							
						} break;
					}

					Queue.updateQueueIndexes( null );
					event.setDropCompleted( true );
					event.consume();

				} else if ( db.hasFiles() ) {
					ArrayList <CurrentListTrack> tracksToAdd = new ArrayList();
					
					for ( File file : db.getFiles() ) {
						Path droppedPath = Paths.get( file.getAbsolutePath() );
						if ( Utils.isMusicFile( droppedPath ) ) {
							try {
								tracksToAdd.add( new CurrentListTrack( droppedPath ) );
							} catch ( IOException e ) {
								//TODO: 
								e.printStackTrace();
							}
						
						} else if ( Files.isDirectory( droppedPath ) ) {
							tracksToAdd.addAll( Utils.convertTrackList( Utils.getAllTracksInDirectory( droppedPath ) ) );
						
						} else if ( Utils.isPlaylistFile ( droppedPath ) ) {
							Playlist playlist = Playlist.loadPlaylist( droppedPath );
							if ( playlist != null ) {
								tracksToAdd.addAll( Utils.convertTrackList( playlist.getTracks() ) );
							}
						}
					}
					
					if ( !tracksToAdd.isEmpty() ) {
						int dropIndex = row.isEmpty() ? dropIndex = currentList.size() : row.getIndex();
						currentList.addAll( Math.min( dropIndex, currentList.size() ), tracksToAdd );
						listInfoUpdater.tracksAdded();
					}

					event.setDropCompleted( true );
					event.consume();
				}
			} );

			return row;
		} );
	}
	
	public static void hackTooltipStartTiming() {
	    try {
	    	Tooltip tooltip = new Tooltip ();
	        Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
	        fieldBehavior.setAccessible(true);
	        Object objBehavior = fieldBehavior.get(tooltip);

	        Field fieldTimer = objBehavior.getClass().getDeclaredField("activationTimer");
	        fieldTimer.setAccessible(true);
	        Timeline objTimer = (Timeline) fieldTimer.get(objBehavior);

	        objTimer.getKeyFrames().clear();
	        objTimer.getKeyFrames().add(new KeyFrame(new Duration(350)));
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public static void parseSystemProperties() {
		IS_STANDALONE = Boolean.getBoolean( "hypnos.standalone" );
		IS_DEVELOPING = Boolean.getBoolean( "hypnos.developing" );
		
		if ( IS_STANDALONE ) System.out.println ( "Running as standalone" );
		if ( IS_DEVELOPING ) System.out.println ( "Running on development port" );
	}
	
	public static void setupRootDirectory () {
		String path = MusicPlayerUI.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		
		try {
			String decodedPath = URLDecoder.decode(path, "UTF-8");
			decodedPath = decodedPath.replaceFirst("^/(.:/)", "$1");
			ROOT = Paths.get( decodedPath ).getParent();
			
		} catch ( UnsupportedEncodingException e ) {
			ROOT = Paths.get( path ).getParent();
		}
	}
	

	public static void updateAlbumListPlaceholder() {

		if ( Library.albums.isEmpty() ) {
			if ( albumTable.getPlaceholder() != emptyAlbumListLabel ) {
				albumTable.setPlaceholder( emptyAlbumListLabel );
			}
		} else {
			if ( MusicPlayerUI.albumTable.getPlaceholder() != MusicPlayerUI.filteredAlbumListLabel ) {
				MusicPlayerUI.albumTable.setPlaceholder( MusicPlayerUI.filteredAlbumListLabel );
			}
		}
	}
	
	public static void updateTrackListPlaceholder() {

		if ( Library.tracks.isEmpty() ) {
			if ( trackTable.getPlaceholder() != emptyTrackListLabel ) {
				trackTable.setPlaceholder( emptyTrackListLabel );
			}
		} else {
			if ( MusicPlayerUI.trackTable.getPlaceholder() != MusicPlayerUI.filteredTrackListLabel ) {
				MusicPlayerUI.trackTable.setPlaceholder( MusicPlayerUI.filteredTrackListLabel );
			}
		}
	}
	
	public static void updatePlaylistPlaceholder() {

		if ( Library.playlists.isEmpty() ) {
			if ( playlistTable.getPlaceholder() != emptyPlaylistLabel ) {
				playlistTable.setPlaceholder( emptyPlaylistLabel );
			}
		} else {
			if ( MusicPlayerUI.playlistTable.getPlaceholder() != MusicPlayerUI.filteredPlaylistLabel ) {
				MusicPlayerUI.playlistTable.setPlaceholder( MusicPlayerUI.filteredPlaylistLabel );
			}
		}
	}
	

	public static void setShuffleMode ( final ShuffleMode newMode ) {
		Platform.runLater( () -> {
			shuffleMode = newMode;
			toggleShuffleButton.setText( shuffleMode.getSymbol() );
		});
	}
	
	public static void setRepeatMode ( final RepeatMode newMode ) {
		Platform.runLater( () -> {
			repeatMode = newMode;
			toggleRepeatButton.setText( repeatMode.getSymbol() );
		});
	}
	
	public static void setShowAlbumTracks ( final boolean newValue ) {
		Platform.runLater( () -> {
			trackListCheckBox.setSelected( newValue );
		});
	}
	
	public static double getPrimarySplitPercent() {
		return primarySplitPane.getDividerPositions()[0];
	}
	
	public static void setPrimarySplitPercent ( double value ) {
		Platform.runLater( () -> {
			primarySplitPane.setDividerPosition( 0, value );
		});
	}
	
	public static double getCurrentListSplitPercent() {
		return currentListSplitPane.getDividerPositions()[0];
	}
	
	public static void setCurrentListSplitPercent ( double value ) {
		Platform.runLater( () -> {
			currentListSplitPane.setDividerPosition( 0, value );
		});
	}
	
	public static double getArtSplitPercent() {
		return artSplitPane.getDividerPositions()[0];
	}
	
	public static void setArtSplitPercent ( double value ) {
		Platform.runLater( () -> {
			artSplitPane.setDividerPosition( 0, value );
		});
	}
	
	public static boolean isMaximized () {
		return mainStage.isMaximized();
	}
	
	public static void setMaximized( boolean value ) {
		mainStage.setMaximized( value );
	}
	
	public static int getCurrentTrackNumber() {
		for ( int k = 0 ; k < currentList.size(); k++ ) {
			if ( currentList.get( k ).isCurrentTrack ) {
				return k;
			}
		}
		
		return -1;
	}
	
	@Override
	public void stop(){
		//TODO: Use this to get rid of main
	}

	public static void notifyUserError ( String message ) { 
		
		Alert alert = new Alert ( AlertType.ERROR );
		alert.setTitle( "Error" );
		alert.setContentText( message );
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.showAndWait();
	}
	
	public static void exit ( ExitCode exitCode ) {
		if ( currentPlayer != null ) {
			currentPlayer.stop();
		}
		
		Persister.saveAllData();
		System.exit ( exitCode.getValue() );
	}
	
	@Override
	public void start ( Stage stage ) {
		
		mainStage = stage;
		scene = new Scene( new Group(), 1024, 768 );
		
		File stylesheet = new File ( ROOT + File.separator + "resources" + File.separator + "style.css" );
		scene.getStylesheets().add( "file:///" + stylesheet.getAbsolutePath().replace( "\\", "/" ) ); //TODO: DD
		
		//TODO: If we launch the jar from a different directory, it doesn't shwo the icon
		//we need to get the directory of the jar and load the image from there, not just from the current directory
		try {
			mainStage.getIcons().add( new Image( new FileInputStream ( ROOT.resolve( "resources" + File.separator + "icon.png" ).toFile() ) ) );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "Unable to load program icon: resources/icon.png" );
		}

		setupAlbumTable();
		setupTrackListCheckBox();
		setupAlbumFilterPane();
		setupTrackFilterPane();
		setupQueueWindow();
		setupHistoryWindow();
		setupPlaylistFilterPane();
		setupCurrentListTable();
		setupPlaylistTable();
		setupCurrentListControlPane();
		setupTrackTable();
		setupAlbumImage();
		setupArtistImage();
		setupTransport();
		setupLibraryWindow();
		
		tagWindow = new TagWindow ( mainStage );
		albumInfoWindow = new AlbumInfoWindow ( mainStage );
		playlistInfoWindow = new PlaylistInfoWindow ( mainStage );

		artSplitPane = new SplitPane();
		artSplitPane.getItems().addAll( albumImage, artistImage );

		BorderPane currentPlayingPane = new BorderPane();
		playlistControls.prefWidthProperty().bind( currentPlayingPane.widthProperty() );
		currentPlayingPane.setTop( playlistControls );
		currentPlayingPane.setCenter( currentListTable );

		currentListSplitPane = new SplitPane();
		currentListSplitPane.setOrientation( Orientation.VERTICAL );
		currentListSplitPane.getItems().addAll( currentPlayingPane, artSplitPane );

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

		primarySplitPane = new SplitPane();
		primarySplitPane.getItems().addAll( leftTabPane, currentListSplitPane );

		final BorderPane primaryContainer = new BorderPane();

		primaryContainer.prefWidthProperty().bind( scene.widthProperty() );
		primaryContainer.prefHeightProperty().bind( scene.heightProperty() );
		primaryContainer.setPadding( new Insets( 0 ) ); // TODO:
		primaryContainer.setCenter( primarySplitPane );
		primaryContainer.setTop( transport );

		stage.setTitle( PROGRAM_NAME );
		
		Persister.loadDataBeforeShowWindow();
		
		((Group) scene.getRoot()).getChildren().addAll( primaryContainer );
		stage.setScene( scene );
		stage.show();

		// This stuff has to be done after setScene
		StackPane thumb = (StackPane) trackPositionSlider.lookup( ".thumb" );
		thumb.setVisible( false );

		primarySplitPane.setDividerPositions( .35d );
		currentListSplitPane.setDividerPositions( .65d );
		artSplitPane.setDividerPosition( 0, .51d ); // For some reason .5 doesn't work...

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
		
		hackTooltipStartTiming();

		Persister.loadDataAfterShowWindow();

		updateAlbumListPlaceholder();
		updateTrackListPlaceholder();
		updatePlaylistPlaceholder();
		
		uiLoaded = true;
	}
		
	//TODO: I'm not supposed to depend on main() in javafx programs. What's up w/ that? 
	public static void main ( String[] args ) {
		
		parseSystemProperties();
		setupRootDirectory(); 
		Persister.init();
		CLIParser parser = new CLIParser( );
		ArrayList <SocketCommand> commands = parser.parseCommands( args );
		boolean firstInstance = SingleInstanceController.startCLICommandListener();
		
		if ( firstInstance ) {
			Library.init();
			UIUpdater.init();
			
			Thread uiThread = new Thread (() -> {
				Application.launch( args );
				exit ( ExitCode.NORMAL );
			});
			
			uiThread.start();
			
			while ( !uiLoaded ) {
				try {
					Thread.sleep( 50 );
				} catch ( InterruptedException e ) {
					System.out.println ( "InterruptedException in ui wait block, MusicPlayerUI.main()" );
				}
			}

			SingleInstanceController.sendCommandToUI( commands );
			
			Library.startLoader();
			
		} else {
			SingleInstanceController.sendCommandsThroughSocket( commands );
			System.out.println ( "Not first instance, sent commands, now exiting." ); //TODO: Better loggings
			System.exit ( 0 ); //TODO: Use exit ()
		}
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
