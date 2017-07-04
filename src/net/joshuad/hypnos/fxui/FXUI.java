package net.joshuad.hypnos.fxui;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import org.jaudiotagger.tag.FieldKey;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.Duration;
import net.joshuad.hypnos.Album;
import net.joshuad.hypnos.CurrentListTrack;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Persister;
import net.joshuad.hypnos.SoundSystem;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.SocketCommand;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.Persister.Setting;
import net.joshuad.hypnos.SoundSystem.RepeatMode;
import net.joshuad.hypnos.SoundSystem.ShuffleMode;
import net.joshuad.hypnos.audio.PlayerListener;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;

@SuppressWarnings({ "rawtypes", "unchecked" }) // TODO: Maybe get rid of this when I understand things better
public class FXUI implements PlayerListener {
	
	private static final Logger LOGGER = Logger.getLogger( FXUI.class.getName() );

	public static final DataFormat DRAGGED_TRACKS = new DataFormat( "application/hypnos-java-track" );

	public final String PROGRAM_NAME = "Hypnos";

	TableView <Album> albumTable;
	TableView <Playlist> playlistTable;
	TableView <Track> trackTable;
	TableView <CurrentListTrack> currentListTable;

	BorderPane albumImage;
	BorderPane artistImage;
	
	SplitPane primarySplitPane;
	SplitPane currentListSplitPane;
	
	ImageView playImage;
	ImageView pauseImage;

	HBox albumFilterPane;
	HBox trackFilterPane;
	HBox playlistFilterPane;
	HBox playlistControls;

	Slider trackPositionSlider;

	boolean sliderMouseHeld;

	VBox transport;

	Label timeElapsedLabel = new Label( "" );
	Label timeRemainingLabel = new Label( "" );
	Label trackInfo = new Label( "" );

	ListInfoUpdater listInfoUpdater;
	
	Label emptyPlaylistLabel = new Label( 
		"You haven't created any playlists, make a playlist on the right and click 💾 to save it for later." );

	Label emptyTrackListLabel = new Label( 
		"No tracks loaded, click on the + button, or drop folders here, to add to your library." );
	
	Label emptyAlbumListLabel = new Label(
		"No albums loaded, click on the + button, or drop folders here, to add to your library." );
	
	Label filteredAlbumListLabel = new Label( "No albums match." );
	Label filteredTrackListLabel = new Label( "No tracks match." );
	Label filteredPlaylistLabel = new Label( "No playlists match." );

	Scene scene;
	Stage mainStage;
	
	QueueWindow queueWindow;
	TagWindow tagWindow;
	PlaylistInfoWindow playlistInfoWindow;
	AlbumInfoWindow albumInfoWindow;
	LibraryLocationWindow libraryLocationWindow;
	HistoryWindow historyWindow;

	Button togglePlayButton;
	Button toggleRepeatButton;
	Button toggleShuffleButton;
	Button showQueueButton;

	SplitPane artSplitPane;
	
	CheckBox trackListCheckBox;
	TextField trackFilterBox;
	
	SoundSystem player;
	
	
	public FXUI ( Stage stage, SoundSystem player ) {
		mainStage = stage;
		this.player = player;
		
		scene = new Scene( new Group(), 1024, 768 );
		
		File stylesheet = new File ( Hypnos.getRootDirectory() + File.separator + "resources" + File.separator + "style.css" );
		scene.getStylesheets().add( "file:///" + stylesheet.getAbsolutePath().replace( "\\", "/" ) ); //TODO: DD
		
		//TODO: If we launch the jar from a different directory, it doesn't shwo the icon
		//we need to get the directory of the jar and load the image from there, not just from the current directory
		try {
			mainStage.getIcons().add( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) ) );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "Unable to load program icon: resources/icon.png" );
		}

		setupAlbumTable();
		setupTrackListCheckBox();
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
		
		libraryLocationWindow = new LibraryLocationWindow ( mainStage );
		tagWindow = new TagWindow ( this ); 
		queueWindow = new QueueWindow ( this, player, tagWindow );
		albumInfoWindow = new AlbumInfoWindow ( this, player );
		playlistInfoWindow = new PlaylistInfoWindow ( this, player );
		historyWindow = new HistoryWindow ( this, player );

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

		mainStage.setTitle( PROGRAM_NAME );

		((Group) scene.getRoot()).getChildren().addAll( primaryContainer );
		mainStage.setScene( scene );
		
		player.addListener ( this );
	}
	
	public void clearList() {
		player.clearList();
		listInfoUpdater.listCleared();
	}
	
	public void stopPlayer() {
		player.stopTrack();
	}
	
	public void pause() {
		player.pause();
	}
	
	public void unpause() {
		player.unpause();
	}
	
	public void togglePause() {
		player.togglePause();
	}
	
	public void nextTrack() {
		player.nextTrack();
	}
	
	private void removeFromCurrentList ( List<Integer> removeMe ) {
		
		if ( !removeMe.isEmpty() ) {
			player.removeTracksAtIndices ( removeMe );
			
			if ( currentListTable.getItems().size() == 0 ) {
				listInfoUpdater.listCleared();
			} else {
				listInfoUpdater.tracksRemoved();
			}
		}
	}
	
	public void previousRequested() {
		if ( player.isStopped() ) {
			currentListTable.getSelectionModel().clearAndSelect( currentListTable.getSelectionModel().getSelectedIndex() - 1 );
		} else {
			player.previousTrack();
		}
	}
	
	public Track getSelectedTrack () {
		return currentListTable.getSelectionModel().getSelectedItem();
	}
	
	public void updateTransport ( int timeElapsedS, int timeRemainingS, double percent ) {
		Platform.runLater( new Runnable() {
			public void run () {
				if ( !trackPositionSlider.isValueChanging() && !sliderMouseHeld ) {
					trackPositionSlider.setValue( (trackPositionSlider.getMax() - trackPositionSlider.getMin()) * percent + trackPositionSlider.getMin() );
				}
				timeElapsedLabel.setText( Utils.getLengthDisplay( timeElapsedS ) );
				timeRemainingLabel.setText( Utils.getLengthDisplay( -timeRemainingS ) );
			}
		} );
	}
	
	public void toggleMinimized() {
		mainStage.setIconified( !mainStage.isIconified() );
	}

	public void updatePlaylistMenuItems ( ObservableList <MenuItem> items, EventHandler <ActionEvent> eventHandler ) {

		items.remove( 1, items.size() );

		for ( Playlist playlist : Hypnos.library().getPlaylistSorted() ) {
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
			playImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/play.png" ).toFile() ) ) );
			playImage.setFitHeight( 18 );
			playImage.setFitWidth( 18 );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "Unable to load play icon: resources/play.png" );
		}
		
		try {
			pauseImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/pause.png" ).toFile() ) ) );
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
			previousImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/previous.png" ).toFile() ) ) );
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
			nextImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/next.png" ).toFile() ) ) );
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
			stopImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/stop.png" ).toFile() ) ) );
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
				previousRequested();
			}
		} );

		nextButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				nextTrack();
			}
		});

		stopButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				stopPlayer();
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
			public void changed ( ObservableValue <? extends Boolean> obs, Boolean wasChanging, Boolean isNowChanging ) {
				if ( !isNowChanging ) {
					player.seekRequested( trackPositionSlider.getValue() / trackPositionSlider.getMax() );
				}
			}
		});

		trackPositionSlider.setOnMousePressed( ( MouseEvent e ) -> {
			sliderMouseHeld = true;
		} );

		trackPositionSlider.setOnMouseReleased( ( MouseEvent e ) -> {
			sliderMouseHeld = false;
			player.seekRequested( trackPositionSlider.getValue() / trackPositionSlider.getMax() );
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
			double min = volumeSlider.getMin();
			double max = volumeSlider.getMax();
			double percent = (volumeSlider.getValue() - min) / (max - min);
			player.setVolumePercent( percent ); //Note: this works because min is 0 
			
			if ( percent == 0 ) {
				volumeLabel.setText ( "🔇" );
			} else if ( percent > 0 && percent <= .33f ) {
				volumeLabel.setText ( "🔈" );
			} else if ( percent > .33f && percent <= .66f ) {
				volumeLabel.setText ( "🔉" );
			} else if ( percent > .66f ) {
				volumeLabel.setText ( "🔊" );
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
	
	public void setupAlbumImage () {
		albumImage = new BorderPane();
	}

	public void setupArtistImage () {
		artistImage = new BorderPane();
	}

	public void setAlbumImage ( Image image ) {
		try {
			ResizableImageView view = new ResizableImageView( image );
			view.setPreserveRatio( true );
			albumImage.setCenter( view );
		} catch ( Exception e ) {
			albumImage.setCenter( null );
		}
	}

	public void setArtistImage ( Image image ) {
		try {
			ResizableImageView view = new ResizableImageView( image );
			view.setPreserveRatio( true );
			artistImage.setCenter( view );
		} catch ( Exception e ) {
			artistImage.setCenter( null );
		}
	}
	
	//TODO: This function probably belongs in Library
	public void addToPlaylist ( List <Track> tracks, Playlist playlist ) {
		playlist.getTracks().addAll( tracks );
		if ( player.getCurrentPlaylist() != null && player.getCurrentPlaylist().getName().equals( playlist.getName() ) ) {
			player.addAll( Utils.convertTrackList( tracks ) );
			listInfoUpdater.playlistLoaded( playlist );
		}
		playlistTable.refresh(); 
	}
	
	public void promptAndSavePlaylist ( List <Track> tracks, boolean isCurrentList ) { 
	//TODO: This should probably be refactored into promptForPlaylistName and <something>.savePlaylist( name, items )
	//TODO: I can probably figure out if it's current on my own
		String defaultName = "";
		if ( player.getCurrentPlaylist() != null ) {
			defaultName = player.getCurrentPlaylist().getName();
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

			for ( Playlist test : Hypnos.library().getPlaylists() ) {
				if ( test.getName().equals( enteredName ) ) {
					Hypnos.library().removePlaylist ( test );
					break;
				}
			}

			Playlist newPlaylist = new Playlist( enteredName, new ArrayList <Track> ( tracks ) );
			Hypnos.library().addPlaylist ( newPlaylist );
			
			if ( isCurrentList ) {
				//currentPlaylist = newPlaylist; //TODO: SOmething is probably broken because I commented this out. 
				listInfoUpdater.playlistLoaded( player.getCurrentPlaylist() );
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

			Hypnos.library().removePlaylist( playlist );
			playlist.setName ( enteredName );
			Hypnos.library().addPlaylist( playlist );
			
			listInfoUpdater.playlistLoaded( playlist );
		}
	}

	public void setupCurrentListControlPane () {

		toggleRepeatButton = new Button( player.getRepeatMode().getSymbol() );
		toggleShuffleButton = new Button( player.getShuffleMode().getSymbol() );
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
		
		player.getQueue().getData().addListener( new ListChangeListener () {
			@Override
			public void onChanged ( Change arg0 ) {
				System.out.println ( "Changed" ); //TODO: DD
				if ( player.getQueue().isEmpty() ) {
					System.out.println ( "-Empty" ); //TODO: DD
					showQueueButton.getStyleClass().removeAll ( "queueActive" );
				} else {
					showQueueButton.getStyleClass().add ( "queueActive" );
					System.out.println ( "-Full" ); //TODO: DD
				}
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
				promptAndSavePlaylist( new ArrayList <Track>( player.getCurrentList() ), true );
			}
		});
		
		clearButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				clearList();
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
				
				player.loadTracks ( tracks );
					
			}
		});

		toggleRepeatButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				player.toggleRepeatMode();

				toggleRepeatButton.setText( player.getRepeatMode().getSymbol() );

			}
		});

		toggleShuffleButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				player.toggleShuffleMode();
				toggleShuffleButton.setText( player.getShuffleMode().getSymbol() );
			}
		});

		EventHandler savePlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				String playlistName = ((Playlist) ((MenuItem) event.getSource()).getUserData()).getName();
				Playlist playlist = new Playlist( playlistName, new ArrayList <Track>( player.getCurrentList() ) );
				Hypnos.library().addPlaylist( playlist );
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
			Hypnos.library().getPlaylistsFiltered().setPredicate( playlist -> {
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
				if ( libraryLocationWindow.isShowing() ) {
					libraryLocationWindow.hide();
				} else {
					libraryLocationWindow.show();
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
				Hypnos.library().getTracksFiltered().setPredicate( track -> {
					return acceptChange ( track, oldValue, newValue );
				});
			}
		});
		
		Button settingsButton = new Button( "+" );
		settingsButton.setMinSize( Button.USE_PREF_SIZE, Button.USE_PREF_SIZE );
		settingsButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				if ( libraryLocationWindow.isShowing() ) {
					libraryLocationWindow.hide();
				} else {
					libraryLocationWindow.show();
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
	
	public boolean acceptChange ( Track track, Object oldValue, Object newValueIn ) {
				
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
			Hypnos.library().getAlbumsFiltered().setPredicate( album -> {
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
				if ( libraryLocationWindow.isShowing() ) {
					libraryLocationWindow.hide();
				} else {
					libraryLocationWindow.show();
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
				Hypnos.library().getTracksFiltered().setPredicate( track -> {
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
		albumTable.setItems( Hypnos.library().getAlbumsSorted() );
		albumTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		Hypnos.library().getAlbumsSorted().comparatorProperty().bind( albumTable.comparatorProperty() );

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

		Hypnos.library().getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( event -> {
			player.playAlbums( albumTable.getSelectionModel().getSelectedItems() );
		});

		apendMenuItem.setOnAction( event -> {
			player.appendAlbum( albumTable.getSelectionModel().getSelectedItem() );
		});

		enqueueMenuItem.setOnAction( event -> {
			player.getQueue().addAllAlbums( albumTable.getSelectionModel().getSelectedItems() );
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
					Hypnos.library().requestAddSource( file.toPath() );
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
					player.playAlbum( row.getItem() );
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
						Hypnos.library().requestAddSource( file.toPath() );
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
		trackTable.setItems( Hypnos.library().getTracksSorted() );

		Hypnos.library().getTracksSorted().comparatorProperty().bind( trackTable.comparatorProperty() );
		
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

		Hypnos.library().getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.loadTracks( trackTable.getSelectionModel().getSelectedItems() );
			}
		} );

		apendMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.addTracks ( trackTable.getSelectionModel().getSelectedItems() );
			}
		});
		
		enqueueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.getQueue().addAllTracks( trackTable.getSelectionModel().getSelectedItems() );
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
					Hypnos.library().requestAddSource( file.toPath() );
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
					player.loadTrack ( trackTable.getSelectionModel().getSelectedItem() );
				}
			});
			
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
						Hypnos.library().requestAddSource( file.toPath() );
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
			});

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
		playlistTable.setItems( Hypnos.library().getPlaylistSorted() );

		Hypnos.library().getPlaylistSorted().comparatorProperty().bind( playlistTable.comparatorProperty() );

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
				player.playPlaylists( playlistTable.getSelectionModel().getSelectedItems() );
			}
		});

		appendMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.addTracks ( playlistTable.getSelectionModel().getSelectedItem().getTracks() );
			}
		});
		
		enqueueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.getQueue().addAllPlaylists( playlistTable.getSelectionModel().getSelectedItems() );
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
				//TODO: Multiple selections, deal with or is this ok? 
				playlistInfoWindow.setPlaylist ( playlistTable.getSelectionModel().getSelectedItem() );
				playlistInfoWindow.show();
			}
		});

		removeMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Hypnos.library().removePlaylist( playlistTable.getSelectionModel().getSelectedItem() );
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
					Hypnos.library().getPlaylists().addAll( playlistsToAdd );
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
					player.playPlaylist( row.getItem() );
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
						int dropIndex = row.isEmpty() ? dropIndex = Hypnos.library().getPlaylists().size() : row.getIndex();
						Hypnos.library().getPlaylists().addAll( Math.min( dropIndex, Hypnos.library().getPlaylists().size() ), playlistsToAdd );
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
		currentListTable.setItems( player.getCurrentList() );

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
						player.addTracks ( tracksToCopy );
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
						synchronized ( player.getQueue().getData() ) {
							List <Integer> draggedIndices = container.getIndices();
							ArrayList <Track> tracksToCopy = new ArrayList <Track> ( draggedIndices.size() );
							for ( int index : draggedIndices ) {
								if ( index >= 0 && index < player.getQueue().getData().size() ) {
									Track addMe = player.getQueue().getData().get( index );
									if ( addMe instanceof CurrentListTrack ) {
										tracksToCopy.add( (CurrentListTrack)addMe );
									} else {
										try {
											CurrentListTrack newAddMe = new CurrentListTrack ( addMe );
											player.getQueue().getData().remove ( index );
											player.getQueue().getData().add( index, newAddMe );
											tracksToCopy.add( newAddMe );
											
										} catch ( IOException e ) {
											LOGGER.log( Level.WARNING, "Unable to convert queue track to CurrentListTrack, not adding to current list" );
										}
									}
								}
								player.addTracks( tracksToCopy );
								listInfoUpdater.tracksAdded();
							}
						}
						
					} break;
				}

				player.getQueue().updateQueueIndexes( null );
				event.setDropCompleted( true );
				event.consume();


		
			} else if ( db.hasFiles() ) {
				ArrayList <Track> tracksToAdd = new ArrayList();
				for ( File file : db.getFiles() ) {
					Path droppedPath = Paths.get( file.getAbsolutePath() );
					if ( Utils.isMusicFile( droppedPath ) ) {
						try {
							player.addTrack ( droppedPath );
						} catch ( IOException e ) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else if ( Files.isDirectory( droppedPath ) ) {
						player.addTracks ( Utils.getAllTracksInDirectory( droppedPath ) );
					} else if ( Utils.isPlaylistFile ( droppedPath ) ) {
						Playlist playlist = Playlist.loadPlaylist( droppedPath );
						if ( playlist != null ) {
							player.addTracks ( playlist.getTracks() );
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

		Hypnos.library().getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );

		
		queueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.getQueue().addAllTracks( currentListTable.getSelectionModel().getSelectedItems() );
			}
		});
		
		shuffleMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.shuffleList();
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
				player.playTrack( currentListTable.getSelectionModel().getSelectedItem() );
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
		});
		
		removeMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ObservableList <Integer> selectedIndexes = currentListTable.getSelectionModel().getSelectedIndices();
				int selectAfterDelete = selectedIndexes.get( 0 ) - 1;
				removeFromCurrentList ( new ArrayList<Integer> ( selectedIndexes ) );
				currentListTable.getSelectionModel().clearAndSelect( selectAfterDelete );
			}
		});
		
		cropMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {

				ObservableList <Integer> selectedIndexes = currentListTable.getSelectionModel().getSelectedIndices();
				
				List <Integer> removeMe = new ArrayList<Integer> ( selectedIndexes );
				for ( int k = 0; k < currentListTable.getItems().size(); k++ ) {
					if ( !selectedIndexes.contains( k ) ) {
						removeMe.add ( k );
					}
				}
				
				removeFromCurrentList ( removeMe );
				currentListTable.getSelectionModel().clearSelection();
			}
		});

		currentListTable.setRowFactory( tv -> {
			TableRow <CurrentListTrack> row = new TableRow <>();

			row.setContextMenu( contextMenu );

			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					player.playTrack( row.getItem() );
					//TODO: Do we want the error here? 
				}
			});

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
					int dropIndex = row.getIndex();
					
					switch ( container.getSource() ) {
						case ALBUM_LIST:
						case PLAYLIST_LIST:
						case TRACK_LIST:
						case ALBUM_INFO:
						case HISTORY: {
							player.addTracks( dropIndex, Utils.convertTrackList( container.getTracks() ) );
						} break;
						
						case CURRENT_LIST: {
							//TODO: Generalize this out into a fucntion? 
							List <Integer> draggedIndices = container.getIndices();
							ArrayList <CurrentListTrack> tracksToMove = new ArrayList <CurrentListTrack> ( draggedIndices.size() );
							for ( int index : draggedIndices ) {
								if ( index >= 0 && index < currentListTable.getItems().size() ) {
									tracksToMove.add( currentListTable.getItems().get( index ) );
								}
							}
							
							for ( int k = draggedIndices.size() - 1; k >= 0; k-- ) {
								int index = draggedIndices.get( k ).intValue();
								if ( index >= 0 && index < currentListTable.getItems().size() ) {
									currentListTable.getItems().remove ( index );
								}
							}
							
							player.addTracks ( row.getIndex(), tracksToMove );
							
							currentListTable.getSelectionModel().clearSelection();
							for ( int k = 0; k < draggedIndices.size(); k++ ) {
								currentListTable.getSelectionModel().select( dropIndex + k );
							}

							listInfoUpdater.listReordered( );
						} break;
						
						case QUEUE: {
							synchronized ( player.getQueue().getData() ) {
								List <Integer> draggedIndices = container.getIndices();
								ArrayList <CurrentListTrack> tracksToCopy = new ArrayList <CurrentListTrack> ( draggedIndices.size() );
								for ( int index : draggedIndices ) {
									if ( index >= 0 && index < player.getQueue().getData().size() ) {
										Track addMe = player.getQueue().getData().get( index );
										if ( addMe instanceof CurrentListTrack ) {
											tracksToCopy.add( (CurrentListTrack)addMe );
										} else {
											try {
												CurrentListTrack newAddMe = new CurrentListTrack ( addMe );
												player.getQueue().getData().remove ( index );
												player.getQueue().getData().add( index, newAddMe );
												tracksToCopy.add( newAddMe );
												
											} catch ( IOException e1 ) {
												//TODO: Warning or info?
												LOGGER.log( Level.WARNING, "Unable to convert queue track to CurrentListTrack, not adding to current list" );
											}
										}
									}
									player.addTracks ( dropIndex, tracksToCopy );
								}
							}
							
						} break;
					}

					player.getQueue().updateQueueIndexes( null );
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
						player.addTracks ( row.getIndex(), tracksToAdd );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});

			return row;
		});
	}

	public void hackTooltipStartTiming() {
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
	

	public void updateAlbumListPlaceholder() {

		if ( Hypnos.library().getAlbums().isEmpty() ) {
			if ( albumTable.getPlaceholder() != emptyAlbumListLabel ) {
				albumTable.setPlaceholder( emptyAlbumListLabel );
			}
		} else {
			if ( !albumTable.getPlaceholder().equals( filteredAlbumListLabel ) ) {
				albumTable.setPlaceholder( filteredAlbumListLabel );
			}
		}
	}
	
	public void updateTrackListPlaceholder() {

		if ( Hypnos.library().getTracks().isEmpty() ) {
			if ( trackTable.getPlaceholder() != emptyTrackListLabel ) {
				trackTable.setPlaceholder( emptyTrackListLabel );
			}
		} else {
			if ( !trackTable.getPlaceholder().equals( filteredTrackListLabel ) ) {
				trackTable.setPlaceholder( filteredTrackListLabel );
			}
		}
	}
	
	public void updatePlaylistPlaceholder() {

		if ( Hypnos.library().getPlaylists().isEmpty() ) {
			if ( !playlistTable.getPlaceholder().equals( emptyPlaylistLabel ) ) {
				playlistTable.setPlaceholder( emptyPlaylistLabel );
			}
		} else {
			if ( !playlistTable.getPlaceholder().equals( filteredPlaylistLabel ) ) {
				playlistTable.setPlaceholder( filteredPlaylistLabel );
			}
		}
	}
	
	public void setShowAlbumTracks ( final boolean newValue ) {
		Platform.runLater( () -> {
			trackListCheckBox.setSelected( newValue );
		});
	}
	
	public double getPrimarySplitPercent() {
		return primarySplitPane.getDividerPositions()[0];
	}
	
	public void setPrimarySplitPercent ( double value ) {
		Platform.runLater( () -> {
			primarySplitPane.setDividerPosition( 0, value );
		});
	}
	
	public double getCurrentListSplitPercent() {
		return currentListSplitPane.getDividerPositions()[0];
	}
	
	public void setCurrentListSplitPercent ( double value ) {
		Platform.runLater( () -> {
			currentListSplitPane.setDividerPosition( 0, value );
		});
	}
	
	public double getArtSplitPercent() {
		return artSplitPane.getDividerPositions()[0];
	}
	
	public void setArtSplitPercent ( double value ) {
		Platform.runLater( () -> {
			artSplitPane.setDividerPosition( 0, value );
		});
	}
	
	public boolean isMaximized () {
		return mainStage.isMaximized();
	}
	
	public void setMaximized( boolean value ) {
		mainStage.setMaximized( value );
	}
	
	public static void notifyUserError ( String message ) { 
		
		Alert alert = new Alert ( AlertType.ERROR );
		alert.setTitle( "Error" );
		alert.setContentText( message );
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.showAndWait();
	}

	public void showMainWindow() {
		mainStage.show();
	
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
	
		mainStage.widthProperty().addListener( listener );
		mainStage.heightProperty().addListener( listener );
		
		hackTooltipStartTiming();
	
		updateAlbumListPlaceholder();
		updateTrackListPlaceholder();
		updatePlaylistPlaceholder();
	}


	public EnumMap <Persister.Setting, ? extends Object> getSettings () {
		
		EnumMap <Persister.Setting, Object> retMe = new EnumMap <Persister.Setting, Object> ( Persister.Setting.class );
		
		retMe.put ( Setting.HIDE_ALBUM_TRACKS, trackListCheckBox.isSelected() );
		
		boolean isMaximized = isMaximized();
		retMe.put ( Setting.WINDOW_MAXIMIZED, isMaximized );
		retMe.put ( Setting.WINDOW_X_POSITION, mainStage.getX() ); //So we are on the right monitor
		
		if ( !isMaximized ) {
			retMe.put ( Setting.WINDOW_X_POSITION, mainStage.getX() );
			retMe.put ( Setting.WINDOW_Y_POSITION, mainStage.getY() );
			retMe.put ( Setting.WINDOW_WIDTH, mainStage.getWidth() );
			retMe.put ( Setting.WINDOW_HEIGHT, mainStage.getHeight() );
		}
		
		retMe.put ( Setting.PRIMARY_SPLIT_PERCENT, getPrimarySplitPercent() );
		retMe.put ( Setting.CURRENT_LIST_SPLIT_PERCENT, getCurrentListSplitPercent() );
		retMe.put ( Setting.ART_SPLIT_PERCENT, getArtSplitPercent() );

		return retMe;
	}


	public void refreshCurrentList () {
		currentListTable.refresh();
	}
	
	public void refreshAlbumTable () {
		albumTable.refresh();
	}


	public void refreshTrackTable () {
		trackTable.refresh();
	}

	//TODO: get rid of this. Use a listener paradigm instead
	public Slider getPositionSlider () {
		return trackPositionSlider;
	}
	
	public void applySettings( EnumMap<Persister.Setting, String> settings ) {
		settings.forEach( ( setting, value )-> {
			try {
				switch ( setting ) {
					//TODO: These don't belong here. 
						case TRACK:
							Track track = new Track ( Paths.get( value ), false );
							player.playTrack( track, true );
							break;
							
						case TRACK_POSITION:
							player.seekMS( Long.parseLong( value ) );
							break;
							
						case SHUFFLE:
							player.setShuffleMode ( SoundSystem.ShuffleMode.valueOf( value ) );
							//TODO: Update UI
							break;
							
						case REPEAT:
							player.setRepeatMode ( SoundSystem.RepeatMode.valueOf( value ) );
							//TODO: Update UI
							break;
					//END NOT BELONG
							
					case HIDE_ALBUM_TRACKS:
						setShowAlbumTracks ( Boolean.valueOf( value ) );
						break;		
						
					case WINDOW_X_POSITION:
						mainStage.setX( Double.valueOf( value ) );
						break;
						
					case WINDOW_Y_POSITION:
						mainStage.setY( Double.valueOf( value ) );
						break;
						
					case WINDOW_WIDTH:
						mainStage.setWidth( Double.valueOf( value ) );
						break;
						
					case WINDOW_HEIGHT:
						mainStage.setHeight( Double.valueOf( value ) );
						break;
						
					case WINDOW_MAXIMIZED:
						setMaximized ( Boolean.valueOf( value ) );
						break;
						
					case PRIMARY_SPLIT_PERCENT:
						setPrimarySplitPercent ( Double.valueOf( value ) );
						break;
						
					case CURRENT_LIST_SPLIT_PERCENT:
						setCurrentListSplitPercent ( Double.valueOf( value ) );
						break;
						
					case ART_SPLIT_PERCENT:
						setArtSplitPercent ( Double.valueOf( value ) );
						break;
						
					case TRACK_NUMBER:
						try {
							int tracklistNumber = Integer.parseInt( value );
							if ( tracklistNumber != -1 ) {
								//TODO: MusicPlayerUI.currentList.get( tracklistNumber ).setIsCurrentTrack( true );
							}
						} catch ( Exception e ) {
							System.out.println ( "Error loading current list track number: " + e.getMessage() );
						}
						
						break;
				}
			} catch ( Exception e ) {
				e.printStackTrace( System.out );
				LOGGER.info( "Unable to apply setting: " + setting + " to UI." );
			}
		});
	}


	public Window getMainStage () {
		return mainStage;
	}

	public void takeRemoteCommand ( ArrayList <SocketCommand> commands ) {
		Platform.runLater( () -> {
			for ( SocketCommand command : commands ) {
				if ( command.getType() == SocketCommand.CommandType.LOAD_TRACKS ) {
					ArrayList<Track> newList = new ArrayList<Track>();
					
					for ( File file : (List<File>) command.getObject() ) {
						if ( file.isDirectory() ) {
							newList.addAll( Utils.convertTrackList( Utils.getAllTracksInDirectory( file.toPath() ) ) );
						} else {
							try {
								newList.add( new CurrentListTrack ( file.toPath() ) );
							} catch ( IOException e ) {
								//TODO: Logging
								System.out.println ( "Unable to load file specified by user: " + file.toString() );
							}
						}
					}
					
					if ( newList.size() > 0 ) {
						player.loadTracks ( newList );
					}
				}
			}
	
			for ( SocketCommand command : commands ) {
				if ( command.getType() == SocketCommand.CommandType.CONTROL ) {
					int action = (Integer)command.getObject();
	
					switch ( action ) {
						case SocketCommand.NEXT: 
							nextTrack();
							break;
						case SocketCommand.PREVIOUS:
							previousRequested();
							break;
						case SocketCommand.PAUSE:
							pause();
							break;
						case SocketCommand.PLAY:
							unpause();
							break;
						case SocketCommand.TOGGLE_PAUSE:
							togglePause();
							break;
						case SocketCommand.STOP:
							stopPlayer();
							break;
						case SocketCommand.TOGGLE_MINIMIZED:
							toggleMinimized();
							break;
					}
				} 
				
				try {
					Thread.sleep( 5 ); //We have to do this because too many quick calls to any SourceDataLine.open() locks up
				} catch ( InterruptedException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		});
	}


	@Override
	public void playerPositionChanged ( int positionMS, int lengthMS ) {
		int timeElapsedS = positionMS / 1000;
		int timeRemainingS = ( lengthMS / 1000 ) - timeElapsedS;
		double percent = positionMS / (double)lengthMS;
		
		this.updateTransport( timeElapsedS, timeRemainingS, percent );
	}


	@Override
	public void playerStopped ( Track track, boolean userRequested ) {
		Platform.runLater( () -> {
			currentListTable.refresh();
	
			togglePlayButton.setGraphic( playImage );
	
			trackPositionSlider.setValue( 0 );
			timeElapsedLabel.setText( "" );
			timeRemainingLabel.setText( "" );
			trackInfo.setText( "" );
	
			StackPane thumb = (StackPane) trackPositionSlider.lookup( ".thumb" );
			thumb.setVisible( false );
			
		});
	}


	@Override
	public void playerStarted ( Track track ) {
		Platform.runLater( () -> {
			togglePlayButton.setGraphic( pauseImage );
			
			currentListTable.refresh();
	
			StackPane thumb = (StackPane) trackPositionSlider.lookup( ".thumb" );
			thumb.setVisible( true );
			
			trackInfo.setText( track.getArtist() + " - " + track.getTitle() );
	
			setAlbumImage( track.getAlbumCoverImage() );
			setArtistImage( track.getAlbumArtistImagePath( ) );
		});
	}


	@Override
	public void playerPaused () {
		Platform.runLater( () -> {
			togglePlayButton.setGraphic( playImage );
		});
	}


	@Override
	public void playerUnpaused () {
		Platform.runLater( () -> {
			togglePlayButton.setGraphic( pauseImage );
		});
		
	}


	@Override
	public void playerVolumeChanged ( double newVolumePercent ) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void playerShuffleModeChanged ( ShuffleMode newMode ) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void playerRepeatModeChanged ( RepeatMode newMode ) {
		// TODO Auto-generated method stub
		
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
