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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.joshuad.musicplayer.players.AbstractPlayer;
import org.joshuad.musicplayer.players.FlacPlayer;
import org.joshuad.musicplayer.players.MP3Player;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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

	final static ObservableList <Path> musicSourcePaths = FXCollections.observableArrayList();

	final static ObservableList <Track> currentListData = FXCollections.observableArrayList();

	// These are all three representations of the same data. Add stuff to the
	// Observable List, the other two can't accept add.
	static ObservableList <Album> albums = FXCollections.observableArrayList( new ArrayList <Album>() );
	static FilteredList <Album> albumsFiltered = new FilteredList <Album>( albums, p -> true );
	static SortedList <Album> albumsSorted = new SortedList <Album>( albumsFiltered );

	static ObservableList <Track> tracks = FXCollections.observableArrayList( new ArrayList <Track>() );
	static FilteredList <Track> tracksFiltered = new FilteredList <Track>( tracks, p -> true );
	static SortedList <Track> tracksSorted = new SortedList <Track>( tracksFiltered );

	static ObservableList <Playlist> playlists = FXCollections.observableArrayList( new ArrayList <Playlist>() );
	static FilteredList <Playlist> playlistsFiltered = new FilteredList <Playlist>( playlists, p -> true );
	static SortedList <Playlist> playlistsSorted = new SortedList <Playlist>( playlistsFiltered );
	
	static TableView <Album> albumTable;
	static TableView <Playlist> playlistTable;
	static TableView <Track> trackTable;
	static TableView <Track> currentListTable;
	TableView <Path> musicSourceList;

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

	static AbstractPlayer currentPlayingTrack;

	static Label timeElapsedLabel = new Label( "" );
	static Label timeRemainingLabel = new Label( "" );
	static Label currentPlayingListInfo = new Label( "" );
	static Label trackInfo = new Label( "" );

	static Stage mainStage;
	static Stage libraryWindow;

	static Button togglePlayButton;
	static Button toggleRepeatButton;
	static Button toggleShuffleButton;

	static SplitPane artSplitPane;

	static Random randomGenerator = new Random();

	static int playOnceShuffleTracksPlayedCounter = 1;

	static Playlist currentPlaylist = null;

	static boolean playlistChanged = false;

	static ShuffleMode shuffleMode = ShuffleMode.SEQUENTIAL;
	
	static private MusicLoaderDaemon musicLoader;

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

	public static void main ( String[] args ) throws Exception {

		Logger.getLogger( "org.jaudiotagger" ).setLevel( Level.OFF );
		
		musicLoader = new MusicLoaderDaemon ();
		musicSourcePaths.addListener( musicLoader );
		musicLoader.start();

		Application.launch( args );

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

	public static void playNextTrack () {
		if ( currentPlayingTrack != null ) {

			switch ( shuffleMode ) {
				case SEQUENTIAL: {
					ListIterator <Track> iterator = currentListData.listIterator();
					while ( iterator.hasNext() ) {
						if ( iterator.next().getIsCurrentTrack() ) {
							if ( iterator.hasNext() ) {
								playTrack( iterator.next() );
							} else if ( repeatMode == RepeatMode.PLAY_ONCE ) {
								playOnceShuffleTracksPlayedCounter = 1;
								stopTrack();

							} else if ( repeatMode == RepeatMode.REPEAT
									&& currentListData.size() > 0 ) {
								playTrack( currentListData.get( 0 ) );

							} else {
								stopTrack();
							}
							break;
						}
					}
				}
					break;

				case SHUFFLE: {
					switch ( repeatMode ) {
						case PLAY_ONCE: {

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

						}
							break;

						case REPEAT: {
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
							break;

						default: {
							// TODO: this should never occur. Maybe put in some default advance just in case? throw an error too?
						}
							break;

					}

				}
					break;

			}
		}
	}

	public static void playTrack ( Track track ) {
		if ( currentPlayingTrack != null ) {
			currentPlayingTrack.stop();
			togglePlayButton.setText( "▶" );
		}

		switch ( track.getFormat() ) {
			case FLAC:
				currentPlayingTrack = new FlacPlayer( track, trackPositionSlider );
				togglePlayButton.setText( "𝍪" );
				break;
			case MP3:
				currentPlayingTrack = new MP3Player( track, trackPositionSlider );
				togglePlayButton.setText( "𝍪" );
				break;
			case UNKNOWN:
				break;
			default:
				break;
		}

		while ( recentlyPlayedTracks.size() >= MAX_TRACK_HISTORY ) {
			recentlyPlayedTracks.remove( recentlyPlayedTracks.size() - 1 );
		}

		recentlyPlayedTracks.add( 0, track );

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
		currentListData.addAll( album.getTracks() );
		Track firstTrack = currentListData.get( 0 );
		if ( firstTrack != null ) {
			playTrack( firstTrack );
		}

		playlistChanged = false;
		currentPlayingListInfo.setText( "Album: " + album.getArtist() + " - " + album.getYear() + " - " + album.getTitle() );
	}
	
	public static void loadTrack ( Track track ) {
		ArrayList loadMe = new ArrayList <Track> ( 1 );
		loadMe.add ( track );
		loadTracks ( loadMe );
	}

	public static void loadTracks ( ArrayList <Track> track ) {
		currentListTable.getItems().clear();
		currentListTable.getItems().addAll( track );
		currentPlayingListInfo.setText( "Playlist: New" );
		if ( !currentListTable.getItems().isEmpty() ) {
			playTrack( currentListTable.getItems().get( 0 ) );
		}
		currentPlaylist = null;
	}

	public static void playPlaylist ( Playlist playlist ) {

		stopTrack();
		currentListData.clear();
		currentListData.addAll( playlist.getTracks() );
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
		currentListData.addAll( album.getTracks() );
	}

	public static void stopTrack () {
		if ( currentPlayingTrack != null ) {
			currentPlayingTrack.stop();
			currentPlayingTrack = null;
			currentListTable.refresh();
			togglePlayButton.setText( "▶" );
		}

		trackPositionSlider.setValue( 0 );
		timeElapsedLabel.setText( "" );
		timeRemainingLabel.setText( "" );
		trackInfo.setText( "" );

		StackPane thumb = (StackPane) trackPositionSlider.lookup( ".thumb" );
		thumb.setVisible( false );

	}

	@Override
	public void start ( Stage stage ) {
		mainStage = stage;
		Scene scene = new Scene( new Group(), 1024, 768 );

		setupAlbumTable();
		setupAlbumFilterPane();
		setupTrackFilterPane();
		setupPlaylistFilterPane();
		setupCurrentListTable();
		setupPlaylistTable();
		setupCurrentListControlPane();
		setupTrackTable();
		setupAlbumImage();
		setupArtistImage();
		setupTransport();
		setupLibraryWindow();

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

		primaryContainer.prefWidthProperty().bind( scene.widthProperty() );
		primaryContainer.prefHeightProperty().bind( scene.heightProperty() );
		primaryContainer.setPadding( new Insets( 0 ) ); // TODO:
		primaryContainer.setCenter( primarySplitPane );
		primaryContainer.setTop( transport );

		stage.setTitle( PROGRAM_NAME );
		((Group) scene.getRoot()).getChildren().addAll( primaryContainer );
		stage.setScene( scene );
		stage.show();

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

	}

	private void updatePlaylistMenuItems ( ObservableList <MenuItem> items,
			EventHandler eventHandler ) {

		items.remove( 1, items.size() );

		for ( Playlist playlist : playlistsSorted ) {
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
				if ( currentPlayingTrack != null ) {
					Track previousTrack = null;
					for ( Track track : currentListData ) {
						if ( track.getIsCurrentTrack() ) {
							if ( previousTrack != null ) {
								MusicPlayerUI.playTrack( previousTrack );
							} else {
								MusicPlayerUI.playTrack( track );
							}
							break;
						} else {
							previousTrack = track;
						}
					}
				}
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
				if ( currentPlayingTrack != null ) {
					stopTrack();
				}
			}
		} );

		togglePlayButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {

				if ( currentPlayingTrack != null && currentPlayingTrack.isPaused() ) {
					currentPlayingTrack.play();
					togglePlayButton.setText( "𝍪" );

				} else if ( currentPlayingTrack != null && !currentPlayingTrack.isPaused() ) {
					currentPlayingTrack.pause();
					togglePlayButton.setText( "▶" );

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
					if ( currentPlayingTrack != null ) {
						currentPlayingTrack.seek(
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
			if ( currentPlayingTrack != null ) {
				currentPlayingTrack
						.seek( trackPositionSlider.getValue() / trackPositionSlider.getMax() );
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

	public void setupLibraryWindow () {
		libraryWindow = new Stage();
		libraryWindow.initModality( Modality.NONE );
		libraryWindow.initOwner( mainStage );
		libraryWindow.setTitle( "Music Search Locations" );
		libraryWindow.setWidth( 350 );
		Group root = new Group();
		Scene scene = new Scene( root );
		VBox primaryPane = new VBox();

		musicSourceList = new TableView();
		Label emptyLabel = new Label( "No directories in your library. Either '+ Add' or drop directories here." );
		emptyLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyLabel.setWrapText( true );
		emptyLabel.setTextAlignment( TextAlignment.CENTER );

		musicSourceList.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		musicSourceList.setPlaceholder( emptyLabel );
		musicSourceList.setItems( musicSourcePaths );
		musicSourceList.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		musicSourceList.widthProperty().addListener( new ChangeListener <Number>() {
			@Override
			public void changed ( ObservableValue <? extends Number> source, Number oldWidth, Number newWidth ) {
				Pane header = (Pane) musicSourceList.lookup( "TableHeaderRow" );
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
		
		musicSourceList.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				List <File> files = db.getFiles();
				
				for ( File file : files ) {
					addSearchLocation ( file.toPath() );
				}

				event.setDropCompleted( true );
				event.consume();
			}
		});
			
		musicSourceList.getColumns().add( dirListColumn );
		
		musicSourceList.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				event.acceptTransferModes( TransferMode.MOVE );
				event.consume();

			}
		});

		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle( "Music Folder" );
		File defaultDirectory = new File( System.getProperty( "user.home" ) ); 
		// TODO: put windows on desktop maybe.
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
				addSearchLocation ( selectedFile.toPath() );
			}
		});

		removeButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				removeMusicSource ( musicSourceList.getSelectionModel().getSelectedItems() );
			}
		});

		musicSourceList.setOnKeyPressed( new EventHandler <KeyEvent>() {
			@Override
			public void handle ( final KeyEvent keyEvent ) {
				if ( keyEvent.getCode().equals( KeyCode.DELETE ) ) {
					removeMusicSource ( musicSourceList.getSelectionModel().getSelectedItems() );
				}
			}
		});

		HBox controlBox = new HBox();
		controlBox.getChildren().addAll( addButton, removeButton );
		controlBox.setAlignment( Pos.CENTER );
		controlBox.prefWidthProperty().bind( libraryWindow.widthProperty() );
		controlBox.setPadding( new Insets( 5 ) );

		primaryPane.getChildren().addAll( musicSourceList, controlBox );
		root.getChildren().add( primaryPane );
		libraryWindow.setScene( scene );
	}
	
	public void removeMusicSource ( List <Path> input ) {
		ArrayList <Path> sources = new ArrayList ( input ) ;
		musicSourcePaths.removeAll( sources );
		musicSourceList.getSelectionModel().clearSelection();	

		ArrayList <Album> albumsCopy = new ArrayList <Album> ( albums );

		for ( Album album : albumsCopy ) {
			
			for ( Path sourcePath : sources ) {
				if ( album.getPath().toAbsolutePath().startsWith( sourcePath ) ) {
					boolean remove = true;
					for ( Path otherSourcePath : musicSourcePaths ) {
						if ( album.getPath().toAbsolutePath().startsWith( otherSourcePath ) ) {
							remove = false;
						}
					}
					
					if ( remove ) {
						albums.remove ( album );
						tracks.removeAll( album.getTracks() );
					}
				}
			}
		}
	}	

	public void addSearchLocation ( Path path ) {
		if ( path != null ) {
			path = path.toAbsolutePath();
			
			if ( path.toFile().exists() && path.toFile().isDirectory() ) {

				boolean addSelectedPathToList = true;
				for ( Path alreadyAddedPath : musicSourcePaths ) {
					try {
						if ( Files.isSameFile( path, alreadyAddedPath ) ) {
							addSelectedPathToList = false;
						}
					} catch ( IOException e1 ) {} // Do nothing, assume they don't match.
				}
	
				if ( addSelectedPathToList ) {
					musicSourcePaths.add( path );
				}
			}
		}
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
			currentListData.addAll( tracks );
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

			for ( Playlist test : playlists ) {
				if ( test.getName().equals( enteredName ) ) {
					playlists.remove( test );
					break;
				}
			}

			Playlist newPlaylist = new Playlist( enteredName, new ArrayList <Track> ( tracks ) );
			playlists.add( newPlaylist );
			
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

			playlists.remove( playlist );
			playlist.setName ( enteredName );
			playlists.add( playlist );
			
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
		Button loadTracksButton = new Button( "⏏" );
		Button savePlaylistButton = new Button( "💾" );

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
					try {
						tracks.add( new Track ( file.toPath() ) );
					} catch ( CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e1 ) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
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
				playlists.add( playlist );
			}
		};

		currentPlayingListInfo.setAlignment( Pos.CENTER );

		playlistControls = new HBox();
		playlistControls.setAlignment( Pos.CENTER_RIGHT );

		currentPlayingListInfo.prefWidthProperty().bind( playlistControls.widthProperty() );

		playlistControls.getChildren().addAll( toggleRepeatButton, toggleShuffleButton, 
				currentPlayingListInfo, loadTracksButton, savePlaylistButton );
	}

	public void setupPlaylistFilterPane () {
		playlistFilterPane = new HBox();
		TextField filterBox = new TextField();
		filterBox.setPrefWidth( 500000 );

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

		playlistFilterPane.getChildren().add( filterBox );
		playlistFilterPane.getChildren().add( settingsButton );
	}

	public void setupTrackFilterPane () {
		trackFilterPane = new HBox();
		TextField trackFilterBox = new TextField();
		trackFilterBox.setPrefWidth( 500000 );
		trackFilterBox.textProperty().addListener( ( observable, oldValue, newValue ) -> {
			tracksFiltered.setPredicate( track -> {
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

					if ( !tokenMatches )
						return false;
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

		trackFilterPane.getChildren().add( trackFilterBox );
		trackFilterPane.getChildren().add( settingsButton );
	}

	public void setupAlbumFilterPane () {
		albumFilterPane = new HBox();
		TextField albumFilterBox = new TextField();
		albumFilterBox.setPrefWidth( 500000 );
		albumFilterBox.textProperty().addListener( ( observable, oldValue, newValue ) -> {
			albumsFiltered.setPredicate( album -> {
				if ( newValue == null || newValue.isEmpty() ) {
					return true;
				}

				String[] lowerCaseFilterTokens = newValue.toLowerCase().split( "\\s+" );

				ArrayList <String> matchableText = new ArrayList <String>();

				matchableText.add( Normalizer.normalize( album.getArtist(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
				matchableText.add( album.getArtist().toLowerCase() );
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

		albumFilterPane.getChildren().add( albumFilterBox );
		albumFilterPane.getChildren().add( settingsButton );
	}

	public void setupAlbumTable () {
		TableColumn artistColumn = new TableColumn( "Artist" );
		TableColumn yearColumn = new TableColumn( "Year" );
		TableColumn albumColumn = new TableColumn( "Album" );

		artistColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "Artist" ) );
		yearColumn.setCellValueFactory( new PropertyValueFactory <Album, Integer>( "Year" ) );
		albumColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "Title" ) );

		artistColumn.setMaxWidth( 45000 );
		yearColumn.setMaxWidth( 10000 );
		albumColumn.setMaxWidth( 45000 );

		albumTable = new TableView();
		albumTable.getColumns().addAll( artistColumn, yearColumn, albumColumn );
		albumTable.setEditable( false );
		albumTable.setItems( albumsSorted );
		albumTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		albumsSorted.comparatorProperty().bind( albumTable.comparatorProperty() );

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
		MenuItem addMenuItem = new MenuItem( "Enqueue" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		
		contextMenu.getItems().addAll( playMenuItem, addMenuItem, browseMenuItem, addToPlaylistMenuItem );
		
		MenuItem newPlaylistButton = new MenuItem( "<New Playlist>" );

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

		playlistsSorted.addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
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
					addSearchLocation ( file.toPath() );
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
						addSearchLocation ( file.toPath() );
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
		trackTable.setItems( tracksSorted );

		tracksSorted.comparatorProperty().bind( trackTable.comparatorProperty() );

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
		MenuItem addMenuItem = new MenuItem( "Enqueue" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		contextMenu.getItems().addAll( playMenuItem, addMenuItem, browseMenuItem, addToPlaylistMenuItem );
		
		MenuItem newPlaylistButton = new MenuItem( "<New Playlist>" );

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

		playlistsSorted.addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
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
				currentListTable.getItems().addAll( trackTable.getSelectionModel().getSelectedItems() );
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
					addSearchLocation ( file.toPath() );
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
					currentListTable.getItems().add( trackTable.getSelectionModel().getSelectedItem() );
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
						addSearchLocation ( file.toPath() );
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
		playlistTable.setItems( playlistsSorted );

		playlistsSorted.comparatorProperty().bind( playlistTable.comparatorProperty() );

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
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem renameMenuItem = new MenuItem( "Rename" );
		MenuItem removeMenuItem = new MenuItem( "Remove" );
		contextMenu.getItems().addAll( playMenuItem, enqueueMenuItem, renameMenuItem, removeMenuItem );

		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				playPlaylist( playlistTable.getSelectionModel().getSelectedItem() );
			}
		});

		enqueueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				currentListData.addAll( playlistTable.getSelectionModel().getSelectedItem().getTracks() );
			}
		});
		
		renameMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				promptAndRenamePlaylist ( playlistTable.getSelectionModel().getSelectedItem() );
			}
		});

		removeMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			// TODO: This is the better way, once openjdk and openjfx supports
			// it: getHostServices().showDocument(file.toURI().toString());
			@Override
			public void handle ( ActionEvent event ) {
				playlists.remove( playlistTable.getSelectionModel().getSelectedItem() );
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

		playingColumn.setCellFactory( column -> {
			return new TableCell <Track, Boolean>() {
				@Override
				protected void updateItem ( Boolean trackPlaying, boolean empty ) {
					super.updateItem( trackPlaying, empty );
					if ( empty || trackPlaying == null || trackPlaying == false ) {
						setText( null );
					} else {
						setText( "▶" );
					}
				}
			};
		} );

		playingColumn.setCellValueFactory( new PropertyValueFactory <Track, Boolean>( "IsCurrentTrack" ) );
		artistColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Artist" ) );
		yearColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "Year" ) );
		albumColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Album" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Title" ) );
		trackColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "TrackNumber" ) );
		lengthColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "LengthDisplay" ) );

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

		} );

		FixedWidthCustomResizePolicy resizePolicy = new FixedWidthCustomResizePolicy();
		currentListTable.setColumnResizePolicy( resizePolicy );

		resizePolicy.registerColumns( yearColumn, trackColumn );
		// TODO: Length column policy
		currentListTable.setPlaceholder( new Label( "No tracks in playlist." ) );
		currentListTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		playingColumn.setMaxWidth( 20 );
		playingColumn.setMinWidth( 20 );

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
				currentListTable.getItems().addAll( draggedTracks );

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
					currentListTable.getItems().addAll( dropIndex, addMe );

					event.setDropCompleted( true );
					event.consume();
				}

			} else if ( db.hasContent( DRAGGED_PLAYLIST_INDEX ) ) {
				if ( currentListTable.getItems().isEmpty() ) {
					// If the list is empty, we handle the drop, otherwise let
					// the rows handle it
					Playlist draggedPlaylist = (Playlist) db.getContent( DRAGGED_PLAYLIST_INDEX );

					int dropIndex = 0;
					currentListTable.getItems().addAll( dropIndex, draggedPlaylist.getTracks() );

					event.setDropCompleted( true );
					event.consume();
				}

			} else if ( db.hasFiles() ) {
				ArrayList <Track> tracksToAdd = new ArrayList();
				for ( File file : db.getFiles() ) {
					Path droppedPath = Paths.get( file.getAbsolutePath() );
					if ( Utils.isMusicFile( droppedPath ) ) {
						try {
							currentListTable.getItems().add( new Track( droppedPath ) );
						} catch ( CannotReadException | IOException | TagException 
						| ReadOnlyFileException | InvalidAudioFrameException e ) {
							e.printStackTrace();
						}
					} else if ( Files.isDirectory( droppedPath ) ) {
						currentListTable.getItems().addAll( Utils.getAllTracksInDirectory( droppedPath ) );
					}
				}

				event.setDropCompleted( true );
				event.consume();
			}

		} );

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem cropMenuItem = new MenuItem( "Crop" );
		MenuItem deleteMenuItem = new MenuItem( "Delete" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );

		MenuItem newPlaylistButton = new MenuItem( "<New Playlist>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

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
				addToPlaylist ( currentListTable.getSelectionModel().getSelectedItems(), playlist );
			}
		};

		playlistsSorted.addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );

		contextMenu.getItems().addAll( playMenuItem, browseMenuItem, addToPlaylistMenuItem, cropMenuItem, deleteMenuItem );

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
				ObservableList <Track> selectedItems = currentListTable.getSelectionModel().getSelectedItems();

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
				ObservableList <Track> selectedItems = currentListTable.getSelectionModel().getSelectedItems();

				if ( !selectedItems.isEmpty() ) {
					int selectAfterDelete = selectedIndexes.get( 0 ) - 1;
					currentListData.retainAll( selectedItems );
					currentListTable.getSelectionModel().clearSelection();
				}
			}
		} );

		currentListTable.setOnKeyPressed( new EventHandler <KeyEvent>() {
			@Override
			public void handle ( final KeyEvent keyEvent ) {
				if ( keyEvent.getCode().equals( KeyCode.DELETE ) ) {
					ObservableList <Integer> selectedIndexes = currentListTable.getSelectionModel().getSelectedIndices();
					ObservableList <Track> selectedItems = currentListTable.getSelectionModel().getSelectedItems();

					if ( !selectedItems.isEmpty() ) {
						int selectAfterDelete = selectedIndexes.get( 0 ) - 1;
						currentListData.removeAll( selectedItems );
						currentListTable.getSelectionModel().clearAndSelect( selectAfterDelete );
					}
				}
			}
		} );

		currentListTable.setRowFactory( tv -> {
			TableRow <Track> row = new TableRow <>();

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
					ArrayList <Track> tracksToMove = new ArrayList( draggedIndexes.size() );
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
					currentListTable.getItems().addAll( dropIndex, draggedTracks );

					event.setDropCompleted( true );
					event.consume();

				} else if ( db.hasContent( DRAGGED_ALBUM_INDICES ) ) {
					
					ArrayList <Integer> draggedIndices = (ArrayList<Integer>) db.getContent( DRAGGED_ALBUM_INDICES );
					
					ArrayList <Track> addMe = new ArrayList <Track> ();
					
					for ( int index : draggedIndices ) {
						System.out.println ( "index: " + index );
						addMe.addAll ( albumTable.getItems().get( index ).getTracks() );
					}

					int dropIndex = row.isEmpty() ? dropIndex = currentListTable.getItems().size() : row.getIndex();
					currentListTable.getItems().addAll( dropIndex, addMe );
					
					if ( !addMe.isEmpty() ) {
						currentListTable.getItems().addAll( Math.min( dropIndex, currentListTable.getItems().size() ), addMe );
					}
					event.setDropCompleted( true );
					event.consume();

				} else if ( db.hasContent( DRAGGED_PLAYLIST_INDEX ) ) {
					Playlist draggedPlaylist = (Playlist) db.getContent( DRAGGED_PLAYLIST_INDEX );

					int dropIndex = row.getIndex();
					currentListTable.getItems().addAll( dropIndex, draggedPlaylist.getTracks() );

					event.setDropCompleted( true );
					event.consume();

				} else if ( db.hasFiles() ) {
					ArrayList <Track> tracksToAdd = new ArrayList();
					for ( File file : db.getFiles() ) {
						Path droppedPath = Paths.get( file.getAbsolutePath() );
						if ( Utils.isMusicFile( droppedPath ) ) {
							try {
								tracksToAdd.add( new Track( droppedPath ) );
							} catch ( CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e ) {
								e.printStackTrace();
							}
						} else if ( Files.isDirectory( droppedPath ) ) {
							tracksToAdd.addAll( Utils.getAllTracksInDirectory( droppedPath ) );
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
}
