package net.joshuad.hypnos.fxui;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.jaudiotagger.tag.FieldKey;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;
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
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.Duration;
import net.joshuad.hypnos.Album;
import net.joshuad.hypnos.CurrentList;
import net.joshuad.hypnos.CurrentListState;
import net.joshuad.hypnos.CurrentListTrack;
import net.joshuad.hypnos.CurrentListTrackState;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Library;
import net.joshuad.hypnos.LibraryUpdater.LoaderSpeed;
import net.joshuad.hypnos.Persister;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Track.ArtistTagImagePriority;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.Persister.Setting;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.PlayerListener;
import net.joshuad.hypnos.audio.AudioSystem.RepeatMode;
import net.joshuad.hypnos.audio.AudioSystem.ShuffleMode;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys;

@SuppressWarnings({ "rawtypes", "unchecked" }) // REFACTOR: Maybe get rid of this when I understand things better
public class FXUI implements PlayerListener {
	
	private static final Logger LOGGER = Logger.getLogger( FXUI.class.getName() );

	public static final DataFormat DRAGGED_TRACKS = new DataFormat( "application/hypnos-java-track" );

	public final String PROGRAM_NAME = "Hypnos";

	TableView <Album> albumTable;
	TableView <Playlist> playlistTable;
	TableView <Track> trackTable;
	TableView <CurrentListTrack> currentListTable;

	BorderPane albumImagePane;
	BorderPane artistImagePane;
	
	SplitPane primarySplitPane;
	SplitPane currentListSplitPane;
	StretchedTabPane libraryPane;
	
	ImageView playImage;
	ImageView pauseImage;
	ImageView stopImage;
	ImageView nextImage;
	ImageView previousImage;
	
	ImageView[] volumeImages = new ImageView[ 4 ];
	
	ImageView currentListClearImage, albumFilterClearImage, trackFilterClearImage, playlistFilterClearImage;
	ImageView settingsImage;
	ImageView noRepeatImage, repeatImage, repeatOneImage, sequentialImage, shuffleImage;
	ImageView exportImage, saveImage, loadTracksImage;
	ImageView queueImage, historyImage;
	ImageView addSourceTracksImage, addSourceAlbumsImage, addSourcePlaylistsImage;
	
	Image repeatImageSource, playImageSource, pauseImageSource;

	HBox albumFilterPane;
	HBox trackFilterPane;
	HBox playlistFilterPane;
	HBox playlistControls;
	HBox volumePane;
	
	Tooltip volumeDisabledTooltip = new Tooltip ( "Volume control not supported for tracks with this encoding." );
	
	Slider trackPositionSlider;
	Slider volumeSlider;
	Button volumeMuteButton;

	boolean sliderMouseHeld;

	VBox transport;

	Label timeElapsedLabel = new Label( "" );
	Label timeRemainingLabel = new Label( "" );
	Button currentTrackButton;
	Tooltip currentTrackTooltip;
	
	Label emptyPlaylistLabel = new Label( 
		"You haven't created any playlists, make a playlist on the right and click the save button." );

	Label emptyTrackListLabel = new Label( 
		"No tracks loaded. To add to your library, click on the + button or drop folders here." );
	
	Label emptyAlbumListLabel = new Label(
		"No albums loaded. To add to your library, click on the + button or drop folders here." );
	
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
	SettingsWindow settingsWindow;
	TrackInfoWindow trackInfoWindow;

	Button togglePlayButton;
	Button toggleRepeatButton;
	Button toggleShuffleButton;
	Button showQueueButton;
	Button savePlaylistButton;
	Button exportPlaylistButton;
	Button showHistoryButton;
	Button loadTracksButton;
	Button clearCurrentListButton;

	ResizableImageView albumImage;
	ResizableImageView artistImage;
	
	SplitPane artSplitPane;
	
	CheckBox trackListCheckBox;
	TextField trackFilterBox;
	
	final AudioSystem player;
	final Library library;
	
	private double windowedWidth = 1024;
	private double windowedHeight = 768;
	private double windowedX = 50;
	private double windowedY = 50;
	
	private Track currentImagesTrack = null;
	
	private File darkStylesheet;
	private File baseStylesheet;
	
	private boolean isDarkTheme = false;
	
	private double artistPaneHeightWhileMaximized = 0;
	private boolean ignoreNextArtPaneChange = false;
	
	private ColorAdjust darkThemeTransportButtons = new ColorAdjust(); 
	
	{
		darkThemeTransportButtons.setSaturation( -1 );
		darkThemeTransportButtons.setHue( 1 );
		darkThemeTransportButtons.setBrightness( .55 );
	}
	
	private ColorAdjust darkThemeButtons = new ColorAdjust(); 
	
	{
		darkThemeButtons.setSaturation( -1 );
		darkThemeButtons.setHue( 1 );
		darkThemeButtons.setBrightness( .75 );
	}
	
	private SimpleBooleanProperty promptBeforeOverwrite = new SimpleBooleanProperty ( true );
	
	boolean doPlaylistSaveWarning = true;
	
	public FXUI ( Stage stage, Library library, AudioSystem player, GlobalHotkeys hotkeys ) {
		mainStage = stage;
		this.library = library;
		this.player = player;
		
		player.getCurrentList().addNoLoadThread( Thread.currentThread() );
		
		scene = new Scene( new Group(), windowedWidth, windowedHeight );
		
		baseStylesheet = new File ( Hypnos.getRootDirectory() + File.separator + "resources" + File.separator + "style.css" );
		darkStylesheet = new File ( Hypnos.getRootDirectory() + File.separator + "resources" + File.separator + "style-dark.css" );
		
		try {
			mainStage.getIcons().add( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) ) );
		} catch ( FileNotFoundException e ) {
			LOGGER.warning( "Unable to load program icon: resources/icon.png" );
		}
		
		setupFont();
		loadImages();
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
		
		libraryLocationWindow = new LibraryLocationWindow ( mainStage, library );
		tagWindow = new TagWindow ( this ); 
		queueWindow = new QueueWindow ( this, library, player, tagWindow );
		albumInfoWindow = new AlbumInfoWindow ( this, library, player );
		playlistInfoWindow = new PlaylistInfoWindow ( this, library, player );
		historyWindow = new HistoryWindow ( this, library, player );
		settingsWindow = new SettingsWindow ( this, library, hotkeys, player );
		trackInfoWindow = new TrackInfoWindow ( this );

		applyBaseTheme();
		applyDarkTheme();
		
		artSplitPane = new SplitPane();
		artSplitPane.getItems().addAll( albumImagePane, artistImagePane );

		BorderPane currentPlayingPane = new BorderPane();
		playlistControls.prefWidthProperty().bind( currentPlayingPane.widthProperty() );
		currentPlayingPane.setTop( playlistControls );
		currentPlayingPane.setCenter( currentListTable );

		currentListSplitPane = new SplitPane();
		currentListSplitPane.setOrientation( Orientation.VERTICAL );
		currentListSplitPane.getItems().addAll( currentPlayingPane, artSplitPane );
		
		currentListSplitPane.getDividers().get( 0 ).positionProperty().addListener ( 
			( ObservableValue <? extends Number> item, Number oldValue, Number newValue ) -> {
				if ( !ignoreNextArtPaneChange ) {
					if ( isMaximized() ) {
						artistPaneHeightWhileMaximized = newValue.doubleValue();
					}
				} else {
					ignoreNextArtPaneChange = false;
				}
			}
		);
		
		stage.maximizedProperty().addListener(new ChangeListener<Boolean>() {
		    @Override
		    public void changed( ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue ) {
				ignoreNextArtPaneChange = true;
				if ( artistPaneHeightWhileMaximized != 0 && newValue == true ) {
					currentListSplitPane.getDividers().get( 0 ).setPosition( artistPaneHeightWhileMaximized );
				}
		    }
		});

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

		libraryPane = new StretchedTabPane();

		Tab albumListTab = new Tab( "Albums" );
		albumListTab.setContent( albumListPane );
		albumListTab.setClosable( false );
		Tooltip albumTabTooltip = new Tooltip ( "Album Count: " + library.getAlbums().size() );
		albumListTab.setTooltip( albumTabTooltip );
		
		library.getAlbums().addListener( new ListChangeListener<Album> () {
			public void onChanged ( Change <? extends Album> changed ) {
				albumTabTooltip.setText( "Album Count: " + library.getAlbums().size() );
			}
		});

		Tab playlistTab = new Tab( "Playlists" );
		playlistTab.setContent( playlistPane );
		playlistTab.setClosable( false );
		Tooltip playlistTabTooltip = new Tooltip ( "Playlist Count: " + library.getPlaylists().size() );
		playlistTab.setTooltip( playlistTabTooltip );
		
		library.getPlaylists().addListener( new ListChangeListener<Playlist> () {
			public void onChanged ( Change <? extends Playlist> changed ) {
				playlistTabTooltip.setText( "Playlist Count: " + library.getPlaylists().size() );
			}
		});

		Tab songListTab = new Tab( "Tracks" );
		songListTab.setContent( trackListPane );
		songListTab.setClosable( false );
		Tooltip trackTabTooltip = new Tooltip ( "Track Count: " + library.getTracks().size() );
		songListTab.setTooltip( trackTabTooltip );
		
		library.getTracks().addListener( new ListChangeListener<Track> () {
			public void onChanged ( Change <? extends Track> changed ) {
				trackTabTooltip.setText( "Track Count: " + library.getTracks().size() );
			}
		});

		libraryPane.getTabs().addAll( albumListTab, songListTab, playlistTab );
		libraryPane.setSide( Side.BOTTOM );

		primarySplitPane = new SplitPane();
		primarySplitPane.getItems().addAll( libraryPane, currentListSplitPane );

		final BorderPane primaryContainer = new BorderPane();

		primaryContainer.prefWidthProperty().bind( scene.widthProperty() );
		primaryContainer.prefHeightProperty().bind( scene.heightProperty() );
		primaryContainer.setPadding( new Insets( 0 ) ); 
		primaryContainer.setCenter( primarySplitPane );
		primaryContainer.setTop( transport );

		
		mainStage.setTitle( ( Hypnos.isDeveloping() ? "Dev - " : "" ) + PROGRAM_NAME );

		((Group) scene.getRoot()).getChildren().addAll( primaryContainer );
		mainStage.setScene( scene );

		ChangeListener windowSizeListener = new ChangeListener () {
			@Override
			public void changed ( ObservableValue observable, Object oldValue, Object newValue ) {
				if ( !mainStage.isMaximized() ) {
					windowedWidth = mainStage.getWidth();
					windowedHeight = mainStage.getHeight();
					windowedX = mainStage.getX();
					windowedY = mainStage.getY();
				}
			}
		};
				
		primaryContainer.setOnKeyPressed( ( KeyEvent e ) -> { 

			if ( e.getCode() == KeyCode.S && e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				savePlaylistButton.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.E && e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				exportPlaylistButton.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.UP && !e.isControlDown() && e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				player.incrementVolume();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.DOWN && !e.isControlDown() && e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				player.decrementVolume();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.RIGHT && !e.isControlDown() && e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				player.skipMS( 10000 );
				e.consume();
				
			} else if ( e.getCode() == KeyCode.LEFT && !e.isControlDown() && e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				player.skipMS( -10000 );
				e.consume();
				
			} 
		});
		
		mainStage.widthProperty().addListener( windowSizeListener );
		mainStage.heightProperty().addListener( windowSizeListener );
		
		player.addPlayerListener ( this );
	}
	
	public ColorAdjust getDarkThemeTransportButtonsAdjust () {
		return darkThemeTransportButtons;
	}
	
	private void setupFont() {
		Path fontPath, fontBold, stylesheet; 
		switch ( Hypnos.getOS() ) {
			
			case OSX:
				fontPath = Hypnos.getRootDirectory().resolve( "resources/lucidagrande/lucidagrande.ttf" );
				fontBold = Hypnos.getRootDirectory().resolve( "resources/lucidagrande/lucidagrande-bold.ttf" );
				stylesheet = Hypnos.getRootDirectory().resolve( "resources/style-font-osx.css" );
				stylesheet = Hypnos.getRootDirectory().resolve( "resources/style-font-osx.css" );
				
			case WIN_10:
			case WIN_7:
			case WIN_8:
			case WIN_UNKNOWN:
			case WIN_VISTA:
			case WIN_XP:
			case UNKNOWN:
				fontPath = Hypnos.getRootDirectory().resolve( "resources/calibri/calibri.ttf" );
				fontBold = Hypnos.getRootDirectory().resolve( "resources/calibri/calibri-bold.ttf" );
				stylesheet = Hypnos.getRootDirectory().resolve( "resources/style-font-win.css" );
				break;
				
			case NIX:
			default:
				fontPath = Hypnos.getRootDirectory().resolve( "resources/dejavu/dejavusans.ttf" );
				fontBold = Hypnos.getRootDirectory().resolve( "resources/dejavu/dejavusans-bold.ttf" );
				stylesheet = Hypnos.getRootDirectory().resolve( "resources/style-font-nix.css" );
				break;
		}
		
		try {
			Font font = Font.loadFont( new FileInputStream ( fontPath.toFile() ), 12 ); 
			Font.loadFont( new FileInputStream ( fontBold.toFile() ), 12 );
			scene.getStylesheets().add( "file:///" + stylesheet.toFile().getAbsolutePath().replace( "\\", "/" ) );
		} catch ( Exception e ) {
			LOGGER.log ( Level.INFO, "Unable to set font native to system, using default font.", e );
		}
		
	}
	
	private void loadImages() {
		//REFACTOR: Move all image loading into here. 
		double volFitWidth = 55 * .58;
		double volFitHeight = 45 * .58;
		
		try {
			volumeImages[0] = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/vol-0.png" ).toFile() ) ) );
			volumeImages[0].setFitWidth( volFitWidth );
			volumeImages[0].setFitHeight( volFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/vol-0.png", e );
		}
		
		try {
			volumeImages[1] = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/vol-1.png" ).toFile() ) ) );
			volumeImages[1].setFitWidth( volFitWidth );
			volumeImages[1].setFitHeight( volFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/vol-1.png", e );
		}

		try {
			volumeImages[2] = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/vol-2.png" ).toFile() ) ) );
			volumeImages[2].setFitWidth( volFitWidth );
			volumeImages[2].setFitHeight( volFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/vol-2.png", e );
		}
		
		try {
			volumeImages[3] = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/vol-3.png" ).toFile() ) ) );
			volumeImages[3].setFitWidth( volFitWidth );
			volumeImages[3].setFitHeight( volFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/vol-3.png", e );
		}
		
		try {
			settingsImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/config.png" ).toFile() ) ) );
			settingsImage.setFitWidth( 24 );
			settingsImage.setFitHeight( 24 );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/config.png", e );
		}
		
		double currentListControlsButtonFitWidth = 15;
		double currentListControlsButtonFitHeight = 15;
		
		try {
			noRepeatImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/no-repeat.png" ).toFile() ) ) );
			noRepeatImage.setFitWidth( currentListControlsButtonFitWidth );
			noRepeatImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/no-repeat.png", e );
		}
		
		try {
			repeatImageSource = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/repeat.png" ).toFile() ) );
			repeatImage = new ImageView ( repeatImageSource );
			repeatImage.setFitWidth( currentListControlsButtonFitWidth );
			repeatImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/repeat.png", e );
		}
		
		
		try {
			repeatOneImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/repeat-one.png" ).toFile() ) ) );
			repeatOneImage.setFitWidth( currentListControlsButtonFitWidth );
			repeatOneImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/repeatOne.png", e );
		}
		
		try {
			sequentialImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/sequential.png" ).toFile() ) ) );
			sequentialImage.setFitWidth( currentListControlsButtonFitWidth );
			sequentialImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/sequential.png", e );
		}
		
		try {
			shuffleImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/shuffle.png" ).toFile() ) ) );
			shuffleImage.setFitWidth( currentListControlsButtonFitWidth );
			shuffleImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/shuffle.png", e );
		}
		
		try {
			queueImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/queue.png" ).toFile() ) ) );
			queueImage.setFitWidth( currentListControlsButtonFitWidth );
			queueImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/queue.png", e );
		}
		
		try {
			historyImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/history.png" ).toFile() ) ) );
			historyImage.setFitWidth( currentListControlsButtonFitWidth );
			historyImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/history.png", e );
		}
		
		try {
			Image image = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/add.png" ).toFile() ) );

			addSourceTracksImage = new ImageView ( image );
			addSourceAlbumsImage = new ImageView ( image );
			addSourcePlaylistsImage = new ImageView ( image );
			
			addSourceTracksImage.setFitWidth( currentListControlsButtonFitWidth );
			addSourceTracksImage.setFitHeight( currentListControlsButtonFitHeight );
			addSourceAlbumsImage.setFitWidth( currentListControlsButtonFitWidth );
			addSourceAlbumsImage.setFitHeight( currentListControlsButtonFitHeight );
			addSourcePlaylistsImage.setFitWidth( currentListControlsButtonFitWidth );
			addSourcePlaylistsImage.setFitHeight( currentListControlsButtonFitHeight );
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/add.png", e );
		}
		
		try {
			exportImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/export.png" ).toFile() ) ) );
			exportImage.setFitWidth( currentListControlsButtonFitWidth );
			exportImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/export.png", e );
		}
		
		try {
			saveImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/save.png" ).toFile() ) ) );
			saveImage.setFitWidth( currentListControlsButtonFitWidth );
			saveImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/save.png", e );
		}
		
		try {
			loadTracksImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/load.png" ).toFile() ) ) );
			loadTracksImage.setFitWidth( currentListControlsButtonFitWidth );
			loadTracksImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/load.png", e );
		}
		
		try {
			Image clearImage = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/clear.png" ).toFile() ) );
			
			currentListClearImage = new ImageView ( clearImage );
			albumFilterClearImage = new ImageView ( clearImage );
			trackFilterClearImage = new ImageView ( clearImage );
			playlistFilterClearImage = new ImageView ( clearImage );

			currentListClearImage.setFitWidth( 12 );
			currentListClearImage.setFitHeight( 12 );
			albumFilterClearImage.setFitWidth( 12 );
			albumFilterClearImage.setFitHeight( 12 );
			trackFilterClearImage.setFitWidth( 12 );
			trackFilterClearImage.setFitHeight( 12 );
			playlistFilterClearImage.setFitWidth( 12 );
			playlistFilterClearImage.setFitHeight( 12 );
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/clear.png", e );
		}	
	}		
	
	public void applyBaseTheme() {
		String baseSheet = "file:///" + baseStylesheet.getAbsolutePath().replace( "\\", "/" );
		scene.getStylesheets().add( baseSheet ); 
		libraryLocationWindow.getScene().getStylesheets().add( baseSheet );
		settingsWindow.getScene().getStylesheets().add( baseSheet );
		queueWindow.getScene().getStylesheets().add( baseSheet );
		tagWindow.getScene().getStylesheets().add( baseSheet );
		playlistInfoWindow.getScene().getStylesheets().add( baseSheet );
		albumInfoWindow.getScene().getStylesheets().add( baseSheet );
		libraryLocationWindow.getScene().getStylesheets().add( baseSheet );
		historyWindow.getScene().getStylesheets().add( baseSheet );
		settingsWindow.getScene().getStylesheets().add( baseSheet );
		trackInfoWindow.getScene().getStylesheets().add( baseSheet );
	}
	
	public void applyDarkTheme() {
		if ( !isDarkTheme ) {
			isDarkTheme = true;
			String darkSheet = "file:///" + darkStylesheet.getAbsolutePath().replace( "\\", "/" );
			scene.getStylesheets().add( darkSheet ); 
			libraryLocationWindow.getScene().getStylesheets().add( darkSheet );
			settingsWindow.getScene().getStylesheets().add( darkSheet );
			queueWindow.getScene().getStylesheets().add( darkSheet );
			tagWindow.getScene().getStylesheets().add( darkSheet );
			playlistInfoWindow.getScene().getStylesheets().add( darkSheet );
			albumInfoWindow.getScene().getStylesheets().add( darkSheet );
			libraryLocationWindow.getScene().getStylesheets().add( darkSheet );
			historyWindow.getScene().getStylesheets().add( darkSheet );
			settingsWindow.getScene().getStylesheets().add( darkSheet );
			trackInfoWindow.getScene().getStylesheets().add( darkSheet );
			
			if ( stopImage != null ) stopImage.setEffect( darkThemeTransportButtons );
			if ( nextImage != null ) nextImage.setEffect( darkThemeTransportButtons );
			if ( previousImage != null ) previousImage.setEffect( darkThemeTransportButtons );
			if ( pauseImage != null ) pauseImage.setEffect( darkThemeTransportButtons );
			if ( playImage != null ) playImage.setEffect( darkThemeTransportButtons );
			if ( settingsImage != null ) settingsImage.setEffect( darkThemeTransportButtons );
			
			for ( int k = 0; k < volumeImages.length; k++ ) {
				if ( volumeImages[k] != null ) volumeImages[k].setEffect( darkThemeTransportButtons );
			}

			if ( currentListClearImage != null ) currentListClearImage.setEffect( darkThemeButtons );
			if ( albumFilterClearImage != null ) albumFilterClearImage.setEffect( darkThemeButtons );
			if ( trackFilterClearImage != null ) trackFilterClearImage.setEffect( darkThemeButtons );
			if ( playlistFilterClearImage != null ) playlistFilterClearImage.setEffect( darkThemeButtons );
			
			if ( noRepeatImage != null ) noRepeatImage.setEffect( darkThemeButtons );
			if ( repeatImage != null ) repeatImage.setEffect( darkThemeButtons );
			if ( repeatOneImage != null ) repeatOneImage.setEffect( darkThemeButtons );
			if ( sequentialImage != null ) sequentialImage.setEffect( darkThemeButtons );
			if ( shuffleImage != null ) shuffleImage.setEffect( darkThemeButtons );
			if ( exportImage != null ) exportImage.setEffect( darkThemeButtons );
			if ( saveImage != null ) saveImage.setEffect( darkThemeButtons );
			if ( loadTracksImage != null ) loadTracksImage.setEffect( darkThemeButtons );
			if ( queueImage != null ) queueImage.setEffect( darkThemeButtons );
			if ( historyImage != null ) historyImage.setEffect( darkThemeButtons );
			if ( addSourceTracksImage != null ) addSourceTracksImage.setEffect( darkThemeButtons );
			if ( addSourceAlbumsImage != null ) addSourceAlbumsImage.setEffect( darkThemeButtons );
			if ( addSourcePlaylistsImage != null ) addSourcePlaylistsImage.setEffect( darkThemeButtons );
		}
	}
	
	public void removeDarkTheme() {	
		isDarkTheme = false;
		String darkSheet = "file:///" + darkStylesheet.getAbsolutePath().replace( "\\", "/" );
		scene.getStylesheets().remove( darkSheet ); 
		libraryLocationWindow.getScene().getStylesheets().remove( darkSheet );
		settingsWindow.getScene().getStylesheets().remove( darkSheet );
		queueWindow.getScene().getStylesheets().remove( darkSheet );
		tagWindow.getScene().getStylesheets().remove( darkSheet );
		playlistInfoWindow.getScene().getStylesheets().remove( darkSheet );
		albumInfoWindow.getScene().getStylesheets().remove( darkSheet );
		libraryLocationWindow.getScene().getStylesheets().remove( darkSheet );
		historyWindow.getScene().getStylesheets().remove( darkSheet );
		settingsWindow.getScene().getStylesheets().remove( darkSheet );
		trackInfoWindow.getScene().getStylesheets().remove( darkSheet );
		
		if ( stopImage != null ) stopImage.setEffect( null );
		if ( nextImage != null ) nextImage.setEffect( null );
		if ( previousImage != null ) previousImage.setEffect( null );
		if ( pauseImage != null ) pauseImage.setEffect( null );
		if ( playImage != null ) playImage.setEffect( null );
		if ( settingsImage != null ) settingsImage.setEffect( null );
		
		for ( int k = 0; k < volumeImages.length; k++ ) {
			if ( volumeImages[k] != null ) volumeImages[k].setEffect( null );
		}

		if ( currentListClearImage != null ) currentListClearImage.setEffect( null );
		if ( albumFilterClearImage != null ) albumFilterClearImage.setEffect( null );
		if ( trackFilterClearImage != null ) trackFilterClearImage.setEffect( null );
		if ( playlistFilterClearImage != null ) playlistFilterClearImage.setEffect( null );
		
		if ( noRepeatImage != null ) noRepeatImage.setEffect( null );
		if ( repeatImage != null ) repeatImage.setEffect( null );
		if ( repeatOneImage != null ) repeatOneImage.setEffect( null );
		if ( sequentialImage != null ) sequentialImage.setEffect( null );
		if ( shuffleImage != null ) shuffleImage.setEffect( null );
		if ( exportImage != null ) exportImage.setEffect( null );
		if ( saveImage != null ) saveImage.setEffect( null );
		if ( loadTracksImage != null ) loadTracksImage.setEffect( null );
		if ( queueImage != null ) queueImage.setEffect( null );
		if ( historyImage != null ) historyImage.setEffect( null );
		if ( addSourceTracksImage != null ) addSourceTracksImage.setEffect( null );
		if ( addSourceAlbumsImage != null ) addSourceAlbumsImage.setEffect( null );
		if ( addSourcePlaylistsImage != null ) addSourcePlaylistsImage.setEffect( null );
	}
	
	public boolean isDarkTheme() {
		return isDarkTheme;
	}
		
	//REFACTOR: Does this function need to exist? 
	private void removeFromCurrentList ( List<Integer> removeMe ) {
		
		if ( !removeMe.isEmpty() ) {
			player.getCurrentList().removeTracksAtIndices ( removeMe );
		}
	}
	
	public void previousRequested() {
		if ( player.isStopped() ) {
			currentListTable.getSelectionModel().clearAndSelect( currentListTable.getSelectionModel().getSelectedIndex() - 1 );
		} else {
			player.previous();
		}
	}
	
	public Track getSelectedTrack () {
		return currentListTable.getSelectionModel().getSelectedItem();
	}
	
	public ObservableList <CurrentListTrack> getSelectedTracks () {
		return currentListTable.getSelectionModel().getSelectedItems();
	}
	
	public void setSelectedTracks ( List <CurrentListTrack> selectMe ) {
		currentListTable.getSelectionModel().clearSelection();
		
		if ( selectMe != null ) {
			for ( CurrentListTrack track : selectMe ) {
				currentListTable.getSelectionModel().select( track );
			}
		}
	}
	
	public void updateTransport ( int timeElapsedS, int timeRemainingS, double percent ) {
		Platform.runLater( new Runnable() {
			public void run () {

				if ( player.isPlaying() || player.isPaused() ) {
					if ( !trackPositionSlider.isValueChanging() && !sliderMouseHeld ) {
						trackPositionSlider.setValue( (trackPositionSlider.getMax() - trackPositionSlider.getMin()) * percent + trackPositionSlider.getMin() );
					}
					timeElapsedLabel.setText( Utils.getLengthDisplay( timeElapsedS ) );
					timeRemainingLabel.setText( Utils.getLengthDisplay( -timeRemainingS ) );
				} else if ( player.isStopped() ) {
					currentListTable.refresh();
					togglePlayButton.setGraphic( playImage );
					trackPositionSlider.setValue( 0 );
					timeElapsedLabel.setText( "" );
					timeRemainingLabel.setText( "" );
					currentTrackButton.setText( "" );
					currentTrackTooltip.setText( "No current track." );
			
					StackPane thumb = (StackPane) trackPositionSlider.lookup( ".thumb" );
					thumb.setVisible( false );
				}
			}
		});
	}
	
	public void toggleMinimized() {
		if ( mainStage.isIconified() ) {
			mainStage.setIconified( false );
		} else {
			mainStage.setIconified( true );
		}
	}

	public void updatePlaylistMenuItems ( ObservableList <MenuItem> items, EventHandler <ActionEvent> eventHandler ) {

		items.remove( 1, items.size() );

		for ( Playlist playlist : library.getPlaylistSorted() ) {
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
			playImageSource = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/play.png" ).toFile() ) );
			playImage = new ImageView ( playImageSource );
			playImage.setFitHeight( 18 );
			playImage.setFitWidth( 18 );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/play.png", e );
		}
		
		try {
			pauseImageSource = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/pause.png" ).toFile() ) );
			pauseImage = new ImageView ( pauseImageSource );
			pauseImage.setFitHeight( 18 );
			pauseImage.setFitWidth( 18 );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load pause icon: resources/pause.png", e );
		}
		
		togglePlayButton = new Button ( "" );
		togglePlayButton.setGraphic( playImage );
		togglePlayButton.setPrefSize( 42, 35 );
		togglePlayButton.setMinSize( 42, 35 );
		togglePlayButton.setMaxSize( 42, 35 );
		togglePlayButton.setTooltip( new Tooltip( "Toggle Play/Pause" ) );
		
		previousImage = null;
		try {
			previousImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/previous.png" ).toFile() ) ) );
			previousImage.setFitHeight( 18 );
			previousImage.setFitWidth( 18 );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load previous icon: resources/previous.png", e );
		}
		
		Button previousButton = new Button ( "" );
		previousButton.setGraphic( previousImage );
		previousButton.setPrefSize( 42, 35 );
		previousButton.setMinSize( 42, 35 );
		previousButton.setMaxSize( 42, 35 );
		previousButton.setTooltip( new Tooltip( "Previous Track" ) );
		
		nextImage = null;
		try {
			nextImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/next.png" ).toFile() ) ) );
			nextImage.setFitHeight( 18 );
			nextImage.setFitWidth( 18 );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load previous icon: resources/next.png", e );
		}
		
		Button nextButton = new Button ( "" );
		nextButton.setGraphic( nextImage );
		nextButton.setPrefSize( 42, 35 );
		nextButton.setMinSize( 42, 35 );
		nextButton.setMaxSize( 42, 35 );
		nextButton.setTooltip( new Tooltip( "Next Track" ) );
		
		stopImage = null;
		try {
			stopImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/stop.png" ).toFile() ) ) );
			stopImage.setFitHeight( 18 );
			stopImage.setFitWidth( 18 );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load previous icon: resources/stop.png", e );
		}
		
		Button stopButton = new Button ( "" );
		stopButton.setGraphic( stopImage );
		stopButton.setPrefSize( 42, 35 );
		stopButton.setMinSize( 42, 35 );
		stopButton.setMaxSize( 42, 35 );
		stopButton.setTooltip( new Tooltip( "Stop" ) );

		previousButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				previousRequested();
			}
		} );

		nextButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				player.next();
			}
		});

		stopButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				player.stop( StopReason.USER_REQUESTED );
			}
		} );

		togglePlayButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				if ( player.isStopped() ) { 
					if ( currentListTable.getItems().size() != 0 ) {
						CurrentListTrack selectedItem = currentListTable.getSelectionModel().getSelectedItem();
						
						if ( selectedItem != null ) {
							player.playTrack( selectedItem );
						} else {
							player.next( false );
						}
					}
				} else {
					player.togglePause();
				}
			}
		} );

		timeElapsedLabel = new Label( "" );
		timeRemainingLabel = new Label( "" );

		timeElapsedLabel.setMinWidth( 65 );
		timeElapsedLabel.setContentDisplay( ContentDisplay.RIGHT );
		timeElapsedLabel.setTextAlignment( TextAlignment.RIGHT );
		timeElapsedLabel.setAlignment( Pos.CENTER_RIGHT );
		timeElapsedLabel.setTooltip( new Tooltip ( "Time Elapsed" ) );

		timeRemainingLabel.setMinWidth( 65 );
		timeRemainingLabel.setTooltip( new Tooltip ( "Time Remaining" ) );

		trackPositionSlider = new Slider();
		trackPositionSlider.setMin( 0 );
		trackPositionSlider.setMax( 1000 );
		trackPositionSlider.setValue( 0 );
		trackPositionSlider.setMaxWidth( 600 );
		trackPositionSlider.setMinWidth( 200 );
		trackPositionSlider.setPrefWidth( 400 );
		trackPositionSlider.setStyle( "-fx-font-size: 18px" );
		trackPositionSlider.setTooltip( new Tooltip ( "Change Track Position" ) );

		trackPositionSlider.valueChangingProperty().addListener( new ChangeListener <Boolean>() {
			public void changed ( ObservableValue <? extends Boolean> obs, Boolean wasChanging, Boolean isNowChanging ) {
				if ( !isNowChanging ) {
					player.seekPercent( trackPositionSlider.getValue() / trackPositionSlider.getMax() );
				}
			}
		});

		trackPositionSlider.setOnMousePressed( ( MouseEvent e ) -> {
			sliderMouseHeld = true;
		});

		trackPositionSlider.setOnMouseReleased( ( MouseEvent e ) -> {
			sliderMouseHeld = false;
			player.seekPercent( trackPositionSlider.getValue() / trackPositionSlider.getMax() );
		});
		
		volumeMuteButton = new Button ( );
		volumeMuteButton.setGraphic( volumeImages[3] );
		volumeMuteButton.getStyleClass().add( "volumeLabel" );
		volumeMuteButton.setMinWidth( 30 );
		volumeMuteButton.setPadding( new Insets ( 0, 5, 0, 5 ) );
		volumeMuteButton.getStyleClass().add( "volumeButton" );
		volumeMuteButton.setTooltip( new Tooltip ( "Toggle Mute" ) );
		
		volumeMuteButton.setOnAction( ( ActionEvent e ) -> {
			player.toggleMute();
		});
		
		volumeSlider = new Slider();
		volumeSlider.setMin( 0 );
		volumeSlider.setMax( 100 );
		volumeSlider.setValue( 100 );
		volumeSlider.setPrefWidth( 100 );
		volumeSlider.setMinWidth( 80 );
		volumeSlider.setTooltip( new Tooltip ( "Control Volume" ) );
		volumeSlider.setPadding( new Insets ( 0, 10, 0, 0 ) );

		
		EventHandler<MouseEvent> volumeSliderHandler = new EventHandler<MouseEvent> () {
			@Override
			public void handle ( MouseEvent event ) {
				double min = volumeSlider.getMin();
				double max = volumeSlider.getMax();
				double percent = (volumeSlider.getValue() - min) / (max - min);
				player.setVolumePercent( percent ); //Note: this works because min is 0 
				
			}
		};
		
		volumeSlider.setOnMouseDragged ( volumeSliderHandler );
		volumeSlider.setOnMouseClicked ( volumeSliderHandler );
		
		volumePane = new HBox();
		volumePane.getChildren().addAll( volumeMuteButton, volumeSlider );
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
		
		HBox controls = new HBox();
		controls.getChildren().addAll( trackControls, positionSliderPane, volumePane );
		controls.setSpacing( 10 );
		controls.setAlignment( Pos.CENTER );

		Button settingsButton = new Button ( );
		settingsButton.setGraphic( settingsImage );
		
		switch ( Hypnos.getOS() ) {
			case WIN_10: case WIN_7: case WIN_8: case WIN_UNKNOWN: case WIN_VISTA: case WIN_XP:
				settingsButton.setPadding( new Insets ( 5, 5, 0, 5 ) );
				break;
				
			case NIX: case OSX: case UNKNOWN:
				settingsButton.setPadding( new Insets ( 0, 5, 0, 5 ) );
				break;
		}
		
		settingsButton.getStyleClass().add( "settingsButton" );
		settingsButton.setTooltip( new Tooltip( "Configuration and Information" ) );
		
		settingsButton.setOnAction ( ( ActionEvent event ) -> {
			settingsWindow.show();
		});
		
		Label settingsWidthPadding = new Label ( "" );
		settingsWidthPadding.setMinWidth( 36 ); // I couldn't figure out how to make it the exact same width as settings button
		
		BorderPane playingTrackInfo = new BorderPane();
		currentTrackButton = new Button( "" );
		currentTrackButton.setPadding( new Insets ( 10, 0, 0, 0 ) );
		currentTrackButton.getStyleClass().add( "trackName" );
		playingTrackInfo.setCenter( currentTrackButton );
		playingTrackInfo.setRight( settingsButton );
		playingTrackInfo.setLeft( settingsWidthPadding );
		
		currentTrackButton.setOnMouseClicked( ( MouseEvent event ) -> {
			
			if ( event.getButton() == MouseButton.PRIMARY ) {
				Track current = player.getCurrentTrack();
				if ( current != null ) {
					synchronized ( currentListTable.getItems() ) {
						int itemIndex = currentListTable.getItems().indexOf( current );
						
						if ( itemIndex != -1 && itemIndex < currentListTable.getItems().size() ) {
							currentListTable.requestFocus();
							currentListTable.getSelectionModel().clearAndSelect( itemIndex );
							currentListTable.getFocusModel().focus( itemIndex );
							currentListTable.scrollTo( itemIndex );
						}
					}
					setImages( current );
				}
			}
		});
		
		currentTrackTooltip = new Tooltip ( "" );
		currentTrackButton.setTooltip( currentTrackTooltip );
		
		ContextMenu trackContextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem appendMenuItem = new MenuItem( "Append" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem infoMenuItem = new MenuItem( "Info" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		trackContextMenu.getItems().addAll( playMenuItem, appendMenuItem, enqueueMenuItem, editTagMenuItem, infoMenuItem, browseMenuItem, addToPlaylistMenuItem );
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				Track current = player.getCurrentTrack();
				if ( current != null ) {
					promptAndSavePlaylist ( Arrays.asList( current ) );
				}
			}
		});

		EventHandler addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {

				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				
				Track currentTrack = player.getCurrentTrack();
				if ( currentTrack != null && playlist != null ) {
					addToPlaylist ( Arrays.asList( currentTrack ), playlist );
				}
			}
		};
		
		library.getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Track currentTrack = player.getCurrentTrack();
				if ( currentTrack != null ) {
					player.playTrack( currentTrack );
				}
			}
		});

		appendMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Track currentTrack = player.getCurrentTrack();
				if ( currentTrack != null ) {
					player.getCurrentList().appendTrack ( currentTrack );
				}
			}
		});
		
		enqueueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Track currentTrack = player.getCurrentTrack();
				if ( currentTrack != null ) {
					player.getQueue().addTrack( currentTrack );
				}
			}
		});
		
		editTagMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {

				Track currentTrack = player.getCurrentTrack();
				if ( currentTrack != null ) {
					tagWindow.setTracks( Arrays.asList( currentTrack ), null );
					tagWindow.show();
				}
			}
		});
		
		
		infoMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Track currentTrack = player.getCurrentTrack();
				if ( currentTrack != null ) {
					trackInfoWindow.setTrack( currentTrack );
					trackInfoWindow.show();
				}
			}
		});

		browseMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			// PENDING: This is the better way, once openjdk and openjfx supports
			// it: getHostServices().showDocument(file.toURI().toString());
			@Override
			public void handle ( ActionEvent event ) {
				SwingUtilities.invokeLater( new Runnable() {
					public void run () {
						try {
							Track selectedTrack = player.getCurrentTrack();
							if ( selectedTrack != null ) {
								Desktop.getDesktop().open( selectedTrack.getPath().getParent().toFile() );
							}
						} catch ( Exception e ) {
							LOGGER.log( Level.INFO, "Unable to open local file browser.", e );
						}
					}
				} );
			}
		});
		
		
		ContextMenu currentTrackButtonMenu = new ContextMenu();
		currentTrackButtonMenu.getItems().addAll( playMenuItem, appendMenuItem, enqueueMenuItem, editTagMenuItem, infoMenuItem, browseMenuItem, addToPlaylistMenuItem );
		
		currentTrackButton.setContextMenu( currentTrackButtonMenu );
		
		transport = new VBox();
		transport.getChildren().add( playingTrackInfo );
		transport.getChildren().add( controls );
		transport.setPadding( new Insets( 0, 0, 10, 0 ) );
		transport.setSpacing( 5 );
		transport.setId( "transport" );
		
	}
	
	public void setupAlbumImage () {
		
		ContextMenu menu = new ContextMenu();
		
		MenuItem setImage = new MenuItem ( "Set Album Image" );
		MenuItem exportImage = new MenuItem ( "Export Image" );
		
		menu.getItems().addAll( setImage, exportImage );

		setImage.setOnAction( ( ActionEvent event ) -> {
				
			Track track = currentImagesTrack;
			if ( track == null ) return;
			
			FileChooser fileChooser = new FileChooser(); //TODO: maybe only instantianate this once
			FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter( 
				"Image Files", Arrays.asList( "*.jpg", "*.jpeg", "*.png" ) );
			
			fileChooser.getExtensionFilters().add( fileExtensions );
			fileChooser.setTitle( "Set Album Image" );
			File targetFile = fileChooser.showOpenDialog( mainStage );
			
			if ( targetFile == null ) return; 
			
			track.setAndSaveAlbumImage ( targetFile.toPath(), player );

			setImages ( currentImagesTrack );
		});
			
		exportImage.setOnAction( ( ActionEvent event ) -> {
			Track track = currentImagesTrack;
			if ( track == null ) return;
			
			FileChooser fileChooser = new FileChooser();
			FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter( 
				"Image Files", Arrays.asList( "*.png" ) );
			
			fileChooser.getExtensionFilters().add( fileExtensions );
			fileChooser.setTitle( "Export Album Image" );
			File targetFile = fileChooser.showSaveDialog( mainStage );
			
			if ( targetFile == null ) return; 

			if ( !targetFile.toString().toLowerCase().endsWith(".png") ) {
				targetFile = targetFile.toPath().resolveSibling ( targetFile.getName() + ".png" ).toFile();
			}
			
			try {
				BufferedImage bImage = SwingFXUtils.fromFXImage( albumImage.getImage(), null );
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				ImageIO.write( bImage, "png", byteStream );
				byte[] imageBytes  = byteStream.toByteArray();
				byteStream.close();
				
				Utils.saveImageToDisk( targetFile.toPath(), imageBytes );
			} catch ( IOException ex ) {
				notifyUserError ( ex.getClass().getCanonicalName() + ": Unable to export image. See log for more information." );
				LOGGER.log( Level.WARNING, "Unable to export album image.", ex );
			}
		});
				
		albumImagePane = new BorderPane();

		albumImagePane.getStyleClass().add( "artpane" );
		
		albumImagePane.setOnContextMenuRequested( ( ContextMenuEvent e ) -> {
			boolean disableMenus = currentImagesTrack == null;
			setImage.setDisable( disableMenus );
			exportImage.setDisable( disableMenus );
			
			menu.show( albumImagePane, e.getScreenX(), e.getScreenY() );
		});
		
		albumImagePane.addEventHandler( MouseEvent.MOUSE_PRESSED, ( MouseEvent e ) -> {
			menu.hide();
		});
		
		albumImagePane.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();
			event.acceptTransferModes( TransferMode.COPY );
			event.consume();
		});
		
		albumImagePane.setOnDragDropped( event -> {
			
			Track track = currentImagesTrack;
			
			if ( track == null ) return;
			
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				List <File> files = db.getFiles();
				event.setDropCompleted( true );
				event.consume();
				
				for ( File file : files ) {
					if ( Utils.isImageFile( file ) ) {
						track.setAndSaveAlbumImage ( file.toPath(), player );
						break;
					}
				}
				
				setImages ( currentImagesTrack );
		
			} else {
				for ( DataFormat contentType : db.getContentTypes() ) {
					if ( contentType == DataFormat.lookupMimeType("application/octet-stream" ) ) {
						ByteBuffer buffer = (ByteBuffer)db.getContent( contentType );
						track.setAndSaveAlbumImage( buffer.array(), player );
					}
				}
				
				event.setDropCompleted( true );
				event.consume();
			}
		});
	}
	
	public void setupArtistImage () {
		
		ContextMenu menu = new ContextMenu();
		
		MenuItem setArtistImage = new MenuItem ( "Set Artist Image" );
		MenuItem setAlbumArtistImage = new MenuItem ( "Set Artist Image for this Album" );
		MenuItem setTrackArtistImage = new MenuItem ( "Set Artist Image for this Track" );
		MenuItem exportImage = new MenuItem ( "Export Image" );
		
		menu.getItems().addAll( setArtistImage, setAlbumArtistImage, setTrackArtistImage, exportImage );
		
		artistImagePane = new BorderPane();	
		
		artistImagePane.getStyleClass().add( "artpane" );
		
		artistImagePane.setOnContextMenuRequested( ( ContextMenuEvent e ) -> {
			boolean disableAllMenus = false;
			boolean disableAlbum = true;
			boolean disableArtist = true;
			
			if ( currentImagesTrack == null ) {
				disableAllMenus = true;
				
			} else if ( currentImagesTrack.hasAlbumDirectory() ) {
				disableAlbum = false;
				
				if ( Utils.isArtistDirectory( currentImagesTrack.getAlbumPath().getParent() ) ) {
					disableArtist = false;
				}
			}
			
			setTrackArtistImage.setDisable( disableAllMenus );
			setAlbumArtistImage.setDisable( disableAllMenus || disableAlbum );
			setArtistImage.setDisable( disableAllMenus || disableArtist );
			exportImage.setDisable( disableAllMenus );
			
			menu.show( artistImagePane, e.getScreenX(), e.getScreenY() );
		});

		exportImage.setOnAction( ( ActionEvent event ) -> {
			Track track = currentImagesTrack;
			if ( track == null ) return;
			
			FileChooser fileChooser = new FileChooser();
			FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter( 
				"Image Files", Arrays.asList( "*.png" ) );
			
			fileChooser.getExtensionFilters().add( fileExtensions );
			fileChooser.setTitle( "Export Artist Image" );
			File targetFile = fileChooser.showSaveDialog( mainStage );
			
			if ( targetFile == null ) return; 

			if ( !targetFile.toString().toLowerCase().endsWith(".png") ) {
				targetFile = targetFile.toPath().resolveSibling ( targetFile.getName() + ".png" ).toFile();
			}
			
			try {
				BufferedImage bImage = SwingFXUtils.fromFXImage( artistImage.getImage(), null );
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				ImageIO.write( bImage, "png", byteStream );
				byte[] imageBytes  = byteStream.toByteArray();
				byteStream.close();
				
				Utils.saveImageToDisk( targetFile.toPath(), imageBytes );
			} catch ( IOException ex ) {
				notifyUserError ( ex.getClass().getCanonicalName() + ": Unable to export image. See log for more information." );
				LOGGER.log( Level.WARNING, "Unable to export artist image.", ex );
			}
		});
		
		FileChooser fileChooser = new FileChooser();
		FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter( 
			"Image Files", Arrays.asList( "*.jpg", "*.jpeg", "*.png" ) );
		
		fileChooser.getExtensionFilters().add( fileExtensions );
		fileChooser.setTitle( "Set Artist Image" );
		
		setTrackArtistImage.setOnAction( ( ActionEvent e ) -> {
			Track track = currentImagesTrack;
			if ( track == null ) return;
			
			File imageFile = fileChooser.showOpenDialog( mainStage );
			if ( imageFile == null ) return; 
			
			Track.saveArtistImageToTag ( track.getPath().toFile(), imageFile.toPath(), ArtistTagImagePriority.TRACK, false, player );
			setImages ( currentImagesTrack );
		});
		
		setAlbumArtistImage.setOnAction( ( ActionEvent e ) -> {
			Track track = currentImagesTrack;
			if ( track == null ) return;
			
			File imageFile = fileChooser.showOpenDialog( mainStage );
			if ( imageFile == null ) return; 
			
			try {
				byte[] buffer = Files.readAllBytes( imageFile.toPath() );
				
				//REFACTOR: put this code in a function, it's duplciated below. 
				
				if ( !track.hasAlbumDirectory() ) return;
				
				Path albumPath = track.getAlbumPath();
			
				Utils.saveImageToDisk( albumPath.resolve( "artist.png" ), buffer );
				setImages ( currentImagesTrack );
				Thread workerThread = new Thread ( () -> {
					try ( DirectoryStream <Path> stream = Files.newDirectoryStream( albumPath ) ) {
						for ( Path child : stream ) {
							if ( Utils.isMusicFile( child ) ) {
								Track.saveArtistImageToTag ( child.toFile(), buffer, ArtistTagImagePriority.ALBUM, false, player );
							}
						}
					} catch ( IOException e3 ) {
						LOGGER.log( Level.WARNING, "Unable to get directory listing, artist tags not updated for album: " + albumPath, e3 );
					}
	
					Platform.runLater( () -> setImages ( currentImagesTrack ) );
				});
				
				workerThread.setDaemon( false );
				workerThread.start();
			} catch ( Exception e2 ) {
				LOGGER.log( Level.WARNING, "Unable to load image data from file: " + imageFile, e2 );
			}
			
		});
		
		artistImagePane.addEventHandler( MouseEvent.MOUSE_PRESSED, ( MouseEvent e ) -> {
			menu.hide();
		});
		
		artistImagePane.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();
			event.acceptTransferModes( TransferMode.COPY );
			event.consume();
		});
		
		artistImagePane.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();

			if ( db.hasFiles() ) {
				List <File> files = db.getFiles();
				
				for ( File file : files ) {
					if ( Utils.isImageFile( file ) ) {
						try {
							byte[] buffer = Files.readAllBytes( file.toPath() );
							promptAndSaveArtistImage ( buffer );
							break;
						} catch ( Exception e ) {
							LOGGER.log( Level.WARNING, "Unable to load data from image file: " + file.toString(), e );
						}
					}
				}
		
			} else {
				for ( DataFormat contentType : db.getContentTypes() ) {
					if ( contentType == DataFormat.lookupMimeType("application/octet-stream" ) ) {
						
						ByteBuffer buffer = (ByteBuffer)db.getContent( contentType );
						promptAndSaveArtistImage ( buffer.array() );
					}
				}
			}
			
			event.setDropCompleted( true );
			event.consume();		
		});
	}
	
	private void promptAndSaveArtistImage ( byte[] buffer ) {
		Track targetTrack = currentImagesTrack;
		
		boolean saved = false;
		if ( targetTrack != null ) {
			
			
			Path albumPath = targetTrack.getAlbumPath();
			Path artistPath = null;
			
			if ( albumPath != null && Utils.isArtistDirectory( albumPath.getParent() ) ) {
				artistPath = albumPath.getParent();
			}
			
			List <ArtistImageSaveDialog.Choice> choices = new ArrayList<>();
			
			if ( artistPath != null ) {
				choices.add ( ArtistImageSaveDialog.Choice.ALL );
			}
			
			if (albumPath != null ) {
				choices.add ( ArtistImageSaveDialog.Choice.ALBUM );
			} 
			
			ArtistImageSaveDialog prompt = new ArtistImageSaveDialog ( mainStage, choices );
			
			prompt.showAndWait();
			
			ArtistImageSaveDialog.Choice choice = prompt.getSelectedChoice();
			boolean overwriteAll = prompt.getOverwriteAllSelected();
	
			switch ( choice ) {
				
				case CANCEL:
					break;
				
				case ALL:
					Utils.saveImageToDisk( artistPath.resolve( "artist.png" ), buffer );
					setImages ( currentImagesTrack );
					//PENDING: What about ID3 tags? Set them only if they're not already set?  
					break;
					
				case ALBUM:
					
					//REFACTOR: put this code in a function, it's duplicated above. 
					Utils.saveImageToDisk( albumPath.resolve( "artist.png" ), buffer );
					setImages ( currentImagesTrack );
					Thread workerThread = new Thread ( () -> {
						try ( DirectoryStream <Path> stream = Files.newDirectoryStream( albumPath ) ) {
							for ( Path child : stream ) {
								if ( Utils.isMusicFile( child ) ) {
									Track.saveArtistImageToTag ( child.toFile(), buffer, ArtistTagImagePriority.ALBUM, overwriteAll, player );
								}
							}
						} catch ( IOException e ) {
							LOGGER.log( Level.WARNING, "Unable to list files in directory, artist tags not updated for album: " + albumPath, e );
						}

						Platform.runLater( () -> setImages ( currentImagesTrack ) );
					});
					
					workerThread.setDaemon( false );
					workerThread.start();
					break;
					
				case TRACK:
					Track.saveArtistImageToTag ( targetTrack.getPath().toFile(), buffer, ArtistTagImagePriority.TRACK, overwriteAll, player );
					setImages ( currentImagesTrack );
					break;
					
			}
		}
	}
	
	private Thread imageLoader = null;
	public void setImages ( Track track ) {
		
		if ( track != null && Files.exists( track.getPath() ) ) {
			if ( imageLoader != null ) {
				imageLoader.interrupt();
			}
			
			FXUI ui = this;
			
			imageLoader = new Thread ( ) {
				public void run() {
					Image albumImage = track.getAlbumCoverImage();
					Image artistImage = track.getAlbumArtistImage();
									
					if ( !this.isInterrupted() ) { 
						
						Platform.runLater( () -> {
							setAlbumImage( albumImage );
							setArtistImage( artistImage );
							currentImagesTrack = track;
						});
					}
				}
			};
			
			imageLoader.setDaemon( true );
			imageLoader.start();
			
		} else if ( track == null && currentImagesTrack != null ) {
			setImages ( currentImagesTrack );
			
		} else {
			setAlbumImage ( null );
			setArtistImage ( null );
		}
	}

	public void setAlbumImage ( Image image ) {
		try {
			albumImage = new ResizableImageView( image );
			albumImage.setSmooth(true);
			albumImage.setCache(true);
			albumImage.setPreserveRatio( true );
			albumImagePane.setCenter( albumImage );
		} catch ( Exception e ) {
			albumImagePane.setCenter( null );
		}
	}

	public void setArtistImage ( Image image ) {
		try {
			artistImage = new ResizableImageView( image );
			artistImage.setSmooth(true);
			artistImage.setCache(true);
			artistImage.setPreserveRatio( true );
			artistImagePane.setCenter( artistImage );
		} catch ( Exception e ) {
			artistImagePane.setCenter( null );
		}
	}
	
	//REFACTOR: This function probably belongs in Library
	public void addToPlaylist ( List <Track> tracks, Playlist playlist ) {
		playlist.getTracks().addAll( tracks );
		playlistTable.refresh(); 
		
		//TODO: playlist.equals ( playlist ) instead of name .equals ( name ) ?
		if ( player.getCurrentPlaylist() != null && player.getCurrentPlaylist().getName().equals( playlist.getName() ) ) {
			player.getCurrentList().appendTracks( tracks );
		}
	}

	public void setPromptBeforeOverwrite ( boolean prompt ) {
		promptBeforeOverwrite.set( prompt );
	}
	
	public BooleanProperty promptBeforeOverwriteProperty ( ) {
		return promptBeforeOverwrite;
	}
	
	public boolean okToReplaceCurrentList () {
		if ( player.getCurrentList().getState().getMode() != CurrentList.Mode.PLAYLIST_UNSAVED ) {
			return true;
		}
		
		if ( !promptBeforeOverwrite.getValue() ) {
			return true;
		}
			
		Alert alert = new Alert( AlertType.CONFIRMATION );
		alert.getDialogPane().applyCss();
		double x = mainStage.getX() + mainStage.getWidth() / 2 - 220; //It'd be nice to use alert.getWidth() / 2, but it's NAN now. 
		double y = mainStage.getY() + mainStage.getHeight() / 2 - 50;
		
		alert.setX( x );
		alert.setY( y );
		
		alert.setDialogPane( new DialogPane() {
			@Override
			protected Node createDetailsButton () {
				CheckBox optOut = new CheckBox();
				optOut.setText( "Do not ask again" );
				optOut.setOnAction( e -> {
					promptBeforeOverwrite.set( !optOut.isSelected() );
				});
				return optOut;
			}
		});
		
		alert.getDialogPane().getButtonTypes().addAll( ButtonType.YES, ButtonType.NO, ButtonType.CANCEL );
		alert.getDialogPane().setContentText( "You have an unsaved playlist, do you wish to save it?" );
		alert.getDialogPane().setExpandableContent( new Group() );
		alert.getDialogPane().setExpanded( false );
		alert.getDialogPane().setGraphic( alert.getDialogPane().getGraphic() );
		alert.setTitle( "Save Unsaved Playlist" );
		alert.setHeaderText( null );
		
		Optional <ButtonType> result = alert.showAndWait();
		
		if ( result.isPresent() ) {
				
			if ( result.get() == ButtonType.NO ) {
				return true;
				
			} else if (result.get() == ButtonType.YES ) {
				promptAndSavePlaylist ( Utils.convertCurrentTrackList( player.getCurrentList().getItems() ) ); 
				return true;
			
			} else {
				return false;
			}
		}
		return false;
	}
	
	public void promptAndSavePlaylist ( List <Track> tracks ) { 
	//REFACTOR: This should probably be refactored into promptForPlaylistName and <something>.savePlaylist( name, items )
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

			for ( Playlist test : library.getPlaylists() ) {
				if ( test.getName().equals( enteredName ) ) {
					library.removePlaylist ( test );
					break;
				}
			}

			Playlist newPlaylist = new Playlist( enteredName, new ArrayList <Track> ( tracks ) );
			library.addPlaylist ( newPlaylist );
			
			CurrentListState state = player.getCurrentList().getState();
			
			CurrentListState newState = new CurrentListState ( state.getItems(), state.getAlbums(), newPlaylist, CurrentList.Mode.PLAYLIST );
			
			player.getCurrentList().setState( newState );
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

			renamePlaylist ( playlist, enteredName );
		}
	}
	
	public void renamePlaylist ( Playlist playlist, String rawName ) {
		String oldFileBasename = playlist.getBaseFilename();
		library.removePlaylist( playlist );
		playlist.setName ( rawName );
		library.addPlaylist( playlist );
		playlistTable.refresh();
		Hypnos.getPersister().saveLibraryPlaylists();
		Hypnos.getPersister().deletePlaylistFile( oldFileBasename );
	}
	
	private void updateShuffleButtonImages() {
		switch ( player.getShuffleMode() ) {
			
			case SHUFFLE:
				toggleShuffleButton.setGraphic( shuffleImage );
				break;
				
			case SEQUENTIAL: //Fall through
			default:
				toggleShuffleButton.setGraphic( sequentialImage );
				
				break;
			
		}
	}
	
	private void updateRepeatButtonImages() {
		switch ( player.getRepeatMode() ) {
			
			case REPEAT:
				toggleRepeatButton.setGraphic( repeatImage );
				break;
			case REPEAT_ONE_TRACK:
				toggleRepeatButton.setGraphic( repeatOneImage );
				break;
				
			default: //Fall through
			case PLAY_ONCE:
				toggleRepeatButton.setGraphic( noRepeatImage );
				break;
		}
	}

	public void setupCurrentListControlPane () {

		toggleRepeatButton = new Button( );
		toggleShuffleButton = new Button( );
		showQueueButton = new Button ( );
		showHistoryButton = new Button ( );
		loadTracksButton = new Button( );
		savePlaylistButton = new Button( );
		exportPlaylistButton = new Button ();
		clearCurrentListButton = new Button ( );
		
		clearCurrentListButton.setGraphic( currentListClearImage );
		exportPlaylistButton.setGraphic( exportImage );
		savePlaylistButton.setGraphic( saveImage );
		loadTracksButton.setGraphic( loadTracksImage );
		showQueueButton.setGraphic( queueImage );
		showHistoryButton.setGraphic( historyImage );
		updateRepeatButtonImages();
		updateShuffleButtonImages();
		
		float width = 33;
		float height = 26;
		
		toggleRepeatButton.setMinSize( width, height );
		toggleShuffleButton.setMinSize( width, height );
		showQueueButton.setMinSize( width, height );
		showHistoryButton.setMinSize( width, height );
		loadTracksButton.setMinSize( width, height );
		savePlaylistButton.setMinSize( width, height );
		exportPlaylistButton.setMinSize( width, height );
		clearCurrentListButton.setMinSize( width, height );
		
		toggleRepeatButton.setPrefSize( width, height );
		toggleShuffleButton.setPrefSize( width, height );
		showQueueButton.setPrefSize( width, height );
		showHistoryButton.setPrefSize( width, height );
		loadTracksButton.setPrefSize( width, height );
		savePlaylistButton.setPrefSize( width, height );
		exportPlaylistButton.setPrefSize( width, height );
		clearCurrentListButton.setPrefSize( width, height );
		
		toggleRepeatButton.setTooltip( new Tooltip( "Toggle Repeat Type" ) );
		toggleShuffleButton.setTooltip( new Tooltip( "Toggle Shuffle" ) );
		showQueueButton.setTooltip( new Tooltip( "Show Queue" ) );
		showHistoryButton.setTooltip( new Tooltip( "Show Play History" ) );
		loadTracksButton.setTooltip( new Tooltip( "Load tracks from the filesystem" ) );
		savePlaylistButton.setTooltip( new Tooltip( "Save this playlist" ) );
		exportPlaylistButton.setTooltip( new Tooltip( "Export current list as m3u" ) );
		clearCurrentListButton.setTooltip( new Tooltip( "Clear the current list" ) );
		
		showQueueButton.setOnAction ( new EventHandler <ActionEvent>() {
			public void handle ( ActionEvent e ) {
				queueWindow.show();
			}
		});
		
		player.getQueue().getData().addListener( new ListChangeListener () {
			@Override
			public void onChanged ( Change arg0 ) {
				if ( player.getQueue().isEmpty() ) {
					showQueueButton.getStyleClass().removeAll ( "queueActive" );
				} else {
					showQueueButton.getStyleClass().add ( "queueActive" );
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
				promptAndSavePlaylist( new ArrayList <Track>( player.getCurrentList().getItems() ) );
			}
		});
		
		clearCurrentListButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				player.getCurrentList().clearList();
			}
		});
		
		
		exportPlaylistButton.setOnAction( ( ActionEvent e ) -> {
			File targetFile = promptUserForPlaylistFile();
			if ( targetFile == null ) {
				return;
			}
			
			CurrentListState state = player.getCurrentList().getState();
			
			Playlist saveMe = null; 
		
			switch ( state.getMode() ) {
				case ALBUM:
				case ALBUM_REORDERED: {
					saveMe = new Playlist( targetFile.getName(), Utils.convertCurrentTrackList( state.getItems() ) );
			
				} break;
				
				case PLAYLIST:
				case PLAYLIST_UNSAVED: {
					saveMe = state.getPlaylist();
					if ( saveMe == null ) saveMe = new Playlist( targetFile.getName() );
					saveMe.setTracks( Utils.convertCurrentTrackList( state.getItems() ) );
		
				} break;
				
				case EMPTY:
					break;
				
			}
			
			try {
				saveMe.saveAs( targetFile );
				
			} catch ( IOException e1 ) {
				alertUser ( AlertType.ERROR, "Warning", "Unable to save playlist.", "Unable to save the playlist to the specified location", 400 );
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
				
				ArrayList <Path> paths = new ArrayList <Path> ();
				for ( File file : selectedFiles ) {
					paths.add( file.toPath() );
				}
				
				player.getCurrentList().setTracksPathList( paths );
					
			}
		});

		toggleRepeatButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				player.toggleRepeatMode();
				updateRepeatButtonImages();
			}
		});

		toggleShuffleButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				player.toggleShuffleMode();
				updateShuffleButtonImages();
			}
		});

		EventHandler savePlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				String playlistName = ((Playlist) ((MenuItem) event.getSource()).getUserData()).getName();
				Playlist playlist = new Playlist( playlistName, new ArrayList <Track>( player.getCurrentList().getItems() ) );
				library.addPlaylist( playlist );
			}
		};

		playlistControls = new HBox();
		playlistControls.setAlignment( Pos.CENTER_RIGHT );
		playlistControls.setId( "playlist-controls" );
		
		final Label currentListLength = new Label ( "" );
		currentListLength.setMinWidth( Region.USE_PREF_SIZE );
		currentListLength.setPadding( new Insets ( 0, 10, 0, 0 ) );
		
		player.getCurrentList().getItems().addListener( new ListChangeListener () {
			@Override
			public void onChanged ( Change changes ) {
				int lengthS = 0;
				
				for ( Track track : player.getCurrentList().getItems() ) {
					if ( track != null ) {
						lengthS += track.getLengthS();
					}
				}
				
				final int lengthArgument = lengthS;
				
				Platform.runLater( () -> {
					currentListLength.setText( Utils.getLengthDisplay( lengthArgument ) );
				});
			}
		});
		
		final Label currentPlayingListInfo = new Label ( "" );
		currentPlayingListInfo.setAlignment( Pos.CENTER );
		currentPlayingListInfo.prefWidthProperty().bind( playlistControls.widthProperty() );
		
		player.getCurrentList().addListener( ( CurrentListState currentState ) -> {  
			Platform.runLater( () -> {
				currentPlayingListInfo.setText( currentState.getDisplayString() );
			});
		});
		
		final ContextMenu queueButtonMenu = new ContextMenu();
		MenuItem clearQueue = new MenuItem ( "Clear" );
		clearQueue.setOnAction(  ( ActionEvent e ) -> { player.getQueue().clear(); });
		
		queueButtonMenu.getItems().addAll( clearQueue );
		showQueueButton.setContextMenu( queueButtonMenu );
		
		final ContextMenu shuffleButtonMenu = new ContextMenu();
		toggleShuffleButton.setContextMenu( shuffleButtonMenu );
		
		MenuItem sequential = new MenuItem ( "Sequential" );
		MenuItem shuffle = new MenuItem ( "Shuffle" );
		MenuItem shuffleList = new MenuItem ( "Randomize List Order" );
		shuffleButtonMenu.getItems().addAll( sequential, shuffle, shuffleList );
		
		sequential.setOnAction( ( actionEvent ) -> { player.setShuffleMode( ShuffleMode.SEQUENTIAL ); });
		shuffle.setOnAction( ( actionEvent ) -> { player.setShuffleMode( ShuffleMode.SHUFFLE ); });
		shuffleList.setOnAction( ( actionEvent ) -> { player.shuffleList(); });
		
		final ContextMenu repeatButtonMenu = new ContextMenu();
		toggleRepeatButton.setContextMenu( repeatButtonMenu );
		MenuItem noRepeat = new MenuItem ( "No Repeat" );
		MenuItem repeatAll = new MenuItem ( "Repeat All" );
		MenuItem repeatOne = new MenuItem ( "Repeat One Track" );
		repeatButtonMenu.getItems().addAll( noRepeat, repeatAll, repeatOne );
		
		noRepeat.setOnAction( ( actionEvent ) -> { player.setRepeatMode( RepeatMode.PLAY_ONCE ); });
		repeatAll.setOnAction( ( actionEvent ) -> { player.setRepeatMode( RepeatMode.REPEAT ); });
		repeatOne.setOnAction( ( actionEvent ) -> { player.setRepeatMode( RepeatMode.REPEAT_ONE_TRACK ); });
		

		playlistControls.getChildren().addAll( toggleShuffleButton, toggleRepeatButton, showQueueButton, showHistoryButton,
			currentPlayingListInfo, currentListLength, loadTracksButton, exportPlaylistButton, savePlaylistButton, clearCurrentListButton );
	}

	public void setupPlaylistFilterPane () {
		playlistFilterPane = new HBox();
		TextField filterBox = new TextField();
		filterBox.setPrefWidth( 500000 );
		
		filterBox.textProperty().addListener( ( observable, oldValue, newValue ) -> {
			Platform.runLater( () -> {
				library.getPlaylistsFiltered().setPredicate( playlist -> {
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
		});
		
		filterBox.setOnKeyPressed( ( KeyEvent event ) -> {
			if ( event.getCode() == KeyCode.ESCAPE ) {
				filterBox.clear();
			}
		});
		
		double width = 33;
		double height = 26;
		
		filterBox.setPrefHeight( height );
		
		Button libraryButton = new Button( );
		libraryButton.setGraphic ( addSourcePlaylistsImage );
		libraryButton.setMinSize( width, height );
		libraryButton.setPrefSize( width, height );
		libraryButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				if ( libraryLocationWindow.isShowing() ) {
					libraryLocationWindow.hide();
				} else {
					libraryLocationWindow.show();
				}
			}
		});
		
		Button clearButton = new Button ( );
		clearButton.setGraphic( playlistFilterClearImage );
		clearButton.setMinSize( width, height );
		clearButton.setPrefSize( width, height );
		clearButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				filterBox.setText( "" );
			}
		});
		

		libraryButton.setTooltip( new Tooltip( "Add or Remove Music Folders" ) );
		filterBox.setTooltip ( new Tooltip ( "Filter/Search playlists" ) );
		clearButton.setTooltip( new Tooltip( "Clear the filter text" ) );

		playlistFilterPane.getChildren().addAll( libraryButton, filterBox, clearButton );
	}

	public void setupTrackFilterPane () {
		trackFilterPane = new HBox();
		trackFilterBox = new TextField();
		trackFilterBox.setPrefWidth( 500000 );
		
		trackFilterBox.textProperty().addListener( new ChangeListener <String> () {

			@Override
			public void changed ( ObservableValue <? extends String> observable, String oldValue, String newValue ) {
				Platform.runLater( () -> {
					library.getTracksFiltered().setPredicate( track -> {
						return acceptTrackFilterChange ( track, oldValue, newValue ); 
					});
				});
			}
		});
		
		trackFilterBox.setOnKeyPressed( ( KeyEvent event ) -> {
			if ( event.getCode() == KeyCode.ESCAPE ) {
				trackFilterBox.clear();
			}
		});
		
		double width = 33;
		double height = 26;

		trackFilterBox.setPrefHeight( height );
		
		Button libraryButton = new Button( );
		libraryButton.setGraphic( addSourceTracksImage );
		libraryButton.setMinSize( width, height );
		libraryButton.setPrefSize( width, height );
		libraryButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				if ( libraryLocationWindow.isShowing() ) {
					libraryLocationWindow.hide();
				} else {
					libraryLocationWindow.show();
				}
			}
		} );
		
		Button clearButton = new Button ( );
		clearButton.setGraphic( trackFilterClearImage );
		clearButton.setMinSize( width, height );
		clearButton.setPrefSize( width, height );
		clearButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				trackFilterBox.setText( "" );
			}
		});

		libraryButton.setTooltip( new Tooltip( "Add or Remove Music Folders" ) );
		trackFilterBox.setTooltip ( new Tooltip ( "Filter/Search tracks" ) );
		clearButton.setTooltip( new Tooltip( "Clear the filter text" ) );
		
		HBox checkBoxMargins = new HBox();
		checkBoxMargins.setPadding( new Insets ( 4, 0, 0, 6 ) );
		checkBoxMargins.getChildren().add( trackListCheckBox );
		
		trackFilterPane.getChildren().addAll( libraryButton, trackFilterBox, clearButton, checkBoxMargins );
	}
	
	public boolean acceptTrackFilterChange ( Track track, Object oldValue, Object newValueIn ) {
				
		String newValue = trackFilterBox.getText();
		if ( newValueIn instanceof String ) {
			newValue = (String)newValueIn;
		}
		
		Boolean boxSelected = trackListCheckBox.isSelected();
		if ( newValueIn instanceof Boolean ) {
			boxSelected = (Boolean)newValueIn;
		}
			
		if ( track.hasAlbumDirectory() && boxSelected ) {
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
			Platform.runLater( () -> {
				library.getAlbumsFiltered().setPredicate( album -> {
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
				});
			});
		});
		
		filterBox.setOnKeyPressed( ( KeyEvent event ) -> {
			if ( event.getCode() == KeyCode.ESCAPE ) {
				filterBox.clear();
			}
		});
		
		
		float width = 33;
		float height = 26;

		filterBox.setPrefHeight( height );
		
		Button libraryButton = new Button( );
		libraryButton.setGraphic( addSourceAlbumsImage );
		libraryButton.setMinSize( width, height );
		libraryButton.setPrefSize( width, height );
		libraryButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				if ( libraryLocationWindow.isShowing() ) {
					libraryLocationWindow.hide();
				} else {
					libraryLocationWindow.show();
				}
			}
		} );

		Button clearButton = new Button( );
		clearButton.setGraphic( albumFilterClearImage );
		clearButton.setMinSize( width, height );
		clearButton.setPrefSize( width, height );
		clearButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				filterBox.setText( "" );
			}
		});
		

		libraryButton.setTooltip( new Tooltip( "Add or Remove Music Folders" ) );
		filterBox.setTooltip ( new Tooltip ( "Filter/Search albums" ) );
		clearButton.setTooltip( new Tooltip( "Clear the filter text" ) );

		albumFilterPane.getChildren().addAll( libraryButton, filterBox, clearButton );
	}

	public void setupTrackListCheckBox() {
		trackListCheckBox = new CheckBox( "" );
		trackListCheckBox.selectedProperty().addListener( new ChangeListener <Boolean> () {
			@Override
			public void changed ( ObservableValue <? extends Boolean> observable, Boolean oldValue, Boolean newValue ) {
				library.getTracksFiltered().setPredicate( track -> {
					return acceptTrackFilterChange ( track, oldValue, newValue );
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
		albumTable.setItems( library.getAlbumsSorted() );
		albumTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		library.getAlbumsSorted().comparatorProperty().bind( albumTable.comparatorProperty() );

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
		MenuItem appendMenuItem = new MenuItem( "Append" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		MenuItem infoMenuItem = new MenuItem( "Info" );
		
		albumTable.setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ESCAPE 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				albumTable.getSelectionModel().clearSelection();
				
			} else if ( e.getCode() == KeyCode.Q 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				enqueueMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.F2 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				editTagMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.F3
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				infoMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.F4
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				browseMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.ENTER
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				playMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isShiftDown() 
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				appendMenuItem.fire();
				
			}
		});
		
		contextMenu.getItems().addAll( 
			playMenuItem, appendMenuItem, enqueueMenuItem, editTagMenuItem, infoMenuItem, 
			browseMenuItem, addToPlaylistMenuItem
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
				promptAndSavePlaylist ( tracks );
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

		library.getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( event -> {
			if ( okToReplaceCurrentList() ) {
				player.getCurrentList().setAlbums( albumTable.getSelectionModel().getSelectedItems() );
				player.next( false );
			}
		});

		appendMenuItem.setOnAction( event -> {
			player.getCurrentList().appendAlbums( albumTable.getSelectionModel().getSelectedItems() );
		});

		enqueueMenuItem.setOnAction( event -> {
			player.getQueue().addAllAlbums( albumTable.getSelectionModel().getSelectedItems() );
		});
		
		editTagMenuItem.setOnAction( event -> {
			List<Album> albums = albumTable.getSelectionModel().getSelectedItems();
			ArrayList<Track> editMe = new ArrayList<Track>();
			
			for ( Album album : albums ) {
				if ( album != null ) {
					editMe.addAll( album.getTracks() );
				}
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
						LOGGER.log( Level.INFO, "Unable to open native file browser.", e );
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
					library.requestAddSource( file.toPath() );
				}

				event.setDropCompleted( true );
				event.consume();
			}
		});
		
		albumTable.getSelectionModel().selectedItemProperty().addListener( ( obs, oldSelection, newSelection ) -> {
			
		    if ( newSelection != null ) {
		    	setImages ( newSelection.getTracks().get( 0 ) );
		    	albumInfoWindow.setAlbum( newSelection );
		    	
		    } else if ( player.getCurrentTrack() != null ) {
		    	setImages ( player.getCurrentTrack() );
		    	
		    } else {
		    	//Do nothing, leave the old artwork there. We can set to null if we like that better, I don't think so though
		    }
		});

		albumTable.setRowFactory( tv -> {
			TableRow <Album> row = new TableRow <>();
			
			row.setContextMenu( contextMenu );

			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					boolean doOverwrite = okToReplaceCurrentList();
					
					if ( doOverwrite ) {
						player.getCurrentList().setAlbum( row.getItem() );
						player.next( false );
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
						library.requestAddSource( file.toPath() );
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
					
					DraggedTrackContainer dragObject = new DraggedTrackContainer( null, tracks, albums, null, DragSource.ALBUM_LIST );
					Dragboard db = row.startDragAndDrop( TransferMode.COPY );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( DRAGGED_TRACKS, dragObject );
					db.setContent( cc );
					event.consume();
				}
			});

			return row;
		});
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
		trackTable.setItems( library.getTracksSorted() );

		library.getTracksSorted().comparatorProperty().bind( trackTable.comparatorProperty() );
		
		trackTable.getSelectionModel().clearSelection();
		trackTable.getSortOrder().add( artistColumn );
		trackTable.getSortOrder().add( albumColumn );
		trackTable.getSortOrder().add( trackColumn );
		FixedWidthCustomResizePolicy resizePolicy = new FixedWidthCustomResizePolicy();
		trackTable.setColumnResizePolicy( resizePolicy );
		
		emptyTrackListLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyTrackListLabel.setWrapText( true );
		emptyTrackListLabel.setTextAlignment( TextAlignment.CENTER );
		trackTable.setPlaceholder( emptyTrackListLabel );
		
		trackTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		ContextMenu trackContextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem appendMenuItem = new MenuItem( "Append" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem infoMenuItem = new MenuItem( "Info" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		trackContextMenu.getItems().addAll( playMenuItem, appendMenuItem, enqueueMenuItem, editTagMenuItem, infoMenuItem, browseMenuItem, addToPlaylistMenuItem );
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				promptAndSavePlaylist ( trackTable.getSelectionModel().getSelectedItems() );
			}
		});

		EventHandler addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				addToPlaylist ( trackTable.getSelectionModel().getSelectedItems(), playlist );
			}
		};

		library.getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List <Track> selectedItems = new ArrayList <Track> ( trackTable.getSelectionModel().getSelectedItems() );
				
				if ( selectedItems.size() == 1 ) {
					player.playItems( selectedItems );
					
				} else if ( selectedItems.size() > 1 ) {
					if ( okToReplaceCurrentList() ) {
						player.playItems( selectedItems );
					}
				}
			}
		});

		appendMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.getCurrentList().appendTracks ( trackTable.getSelectionModel().getSelectedItems() );
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
		
		
		infoMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				trackInfoWindow.setTrack( trackTable.getSelectionModel().getSelectedItem() );
				trackInfoWindow.show();
			}
		});

		browseMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			// PENDING: This is the better way, once openjdk and openjfx supports
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
						} catch ( Exception e ) {
							LOGGER.log( Level.INFO, "Unable to open native file browser.", e );
						}
					}
				} );
			}
		});
		
		trackTable.setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ESCAPE 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				albumTable.getSelectionModel().clearSelection();
				
			} else if ( e.getCode() == KeyCode.Q 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				enqueueMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.F2 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				editTagMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.F3
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				infoMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.F4
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				browseMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.ENTER
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				playMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isShiftDown() 
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				appendMenuItem.fire();
				
			}
		});
		
		trackTable.getSelectionModel().selectedItemProperty().addListener( ( obs, oldSelection, newSelection ) -> {
		    if (newSelection != null) {
		    	setImages ( newSelection );
		    	trackInfoWindow.setTrack( newSelection );
		    	
		    } else if ( player.getCurrentTrack() != null ) {
		    	setImages ( player.getCurrentTrack() );
		    	
		    } else {
		    	//Do nothing, leave the old artwork there. We can set to null if we like that better, I don't think so though
		    }
		});
		
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
					library.requestAddSource( file.toPath() );
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
					player.playTrack( row.getItem(), false );
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
						library.requestAddSource( file.toPath() );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					ArrayList <Integer> indices = new ArrayList <Integer>( trackTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( trackTable.getSelectionModel().getSelectedItems() );
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, null, null, DragSource.TRACK_LIST );
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
		playlistTable.setItems( library.getPlaylistSorted() );

		library.getPlaylistSorted().comparatorProperty().bind( playlistTable.comparatorProperty() );

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
		MenuItem exportMenuItem = new MenuItem( "Export" );
		MenuItem removeMenuItem = new MenuItem( "Remove" );
		contextMenu.getItems().addAll( playMenuItem, appendMenuItem, enqueueMenuItem, renameMenuItem, infoMenuItem, exportMenuItem, removeMenuItem );

		playMenuItem.setOnAction( ( ActionEvent event ) -> {
			if ( okToReplaceCurrentList() ) {
				player.getCurrentList().setPlaylists( playlistTable.getSelectionModel().getSelectedItems() );
				player.next( false );
			}
		});

		appendMenuItem.setOnAction( ( ActionEvent event ) -> {
			player.getCurrentList().appendPlaylists( playlistTable.getSelectionModel().getSelectedItems() );
		});
		
		enqueueMenuItem.setOnAction( ( ActionEvent event ) -> {
			player.getQueue().addAllPlaylists( playlistTable.getSelectionModel().getSelectedItems() );
		});
		
		renameMenuItem.setOnAction( ( ActionEvent event ) -> {
			promptAndRenamePlaylist ( playlistTable.getSelectionModel().getSelectedItem() );
		});
		
		infoMenuItem.setOnAction( ( ActionEvent event ) -> {
			playlistInfoWindow.setPlaylist ( playlistTable.getSelectionModel().getSelectedItem() );
			playlistInfoWindow.show();
		});
		
		exportMenuItem.setOnAction( ( ActionEvent event ) -> {
			File targetFile = promptUserForPlaylistFile();
			
			if ( targetFile == null ) {
				return;
			}
			
			Playlist saveMe = playlistTable.getSelectionModel().getSelectedItem(); 
			
			if ( saveMe == null ) {
				return;
			}
			
			try {
				saveMe.saveAs( targetFile );
				
			} catch ( IOException e1 ) {
				alertUser ( AlertType.ERROR, "Warning", "Unable to save playlist.", "Unable to save the playlist to the specified location", 400 );
			}
			
		});

		removeMenuItem.setOnAction( ( ActionEvent event ) -> {
			
			List <Playlist> deleteMe = playlistTable.getSelectionModel().getSelectedItems();
			if ( deleteMe.size() == 0 ) return;
			
			Alert alert = new Alert( AlertType.CONFIRMATION );
			alert.getDialogPane().applyCss();
			double x = mainStage.getX() + mainStage.getWidth() / 2 - 220; //It'd be nice to use alert.getWidth() / 2, but it's NAN now. 
			double y = mainStage.getY() + mainStage.getHeight() / 2 - 50;
			
			alert.setX( x );
			alert.setY( y );
			
			alert.setTitle( "Confirm" );
			alert.setHeaderText( "Delete Playlist Requested" );
			String text = "Are you sure you want to delete theses playlists?\n";
			int count = 0;
			for ( Playlist playlist : deleteMe ) { 
				text += "\n  " + playlist.getName(); 
				count++;
				if ( count > 6 ) { 
					text += "\n  <... and more>";
					break;
				}
			};
			
			alert.setContentText( text );

			Optional <ButtonType> result = alert.showAndWait();
			
			if ( result.get() == ButtonType.OK ) {
				library.removePlaylists( playlistTable.getSelectionModel().getSelectedItems() );
				playlistTable.getSelectionModel().clearSelection();
			}
		});
		
		playlistTable.getSelectionModel().selectedItemProperty().addListener( ( obs, oldSelection, newSelection ) -> {
		    if (newSelection != null) {
		    	playlistInfoWindow.setPlaylist( newSelection );
		    }
		});

		playlistTable.setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ESCAPE ) {
				playlistTable.getSelectionModel().clearSelection();
				
			} else if ( e.getCode() == KeyCode.F2         
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && !e.isShiftDown() ) {
				renameMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.F3         
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && !e.isShiftDown() ) {
				infoMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.Q
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && !e.isShiftDown() ) {
				enqueueMenuItem.fire();

			}  else if ( e.getCode() == KeyCode.ENTER
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && !e.isShiftDown() ) {
				playMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				appendMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.DELETE
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && !e.isShiftDown() ) {
				removeMenuItem.fire();
				
			} 
		});
		
		playlistTable.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				//REFACTOR: I can check for file extensions...
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
					library.addPlaylists( playlistsToAdd );
				}

				event.setDropCompleted( true );
				event.consume();
			}
			
		});
		
		playlistTable.setRowFactory( tv -> {
			TableRow <Playlist> row = new TableRow <>();
			
			row.setContextMenu ( contextMenu );

			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && !row.isEmpty() ) {
					boolean doOverwrite = okToReplaceCurrentList();
					if ( doOverwrite ) {
						player.getCurrentList().setPlaylist( row.getItem() );
						player.next( false );
					}
				}
			});

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					List <Playlist> selectedPlaylists = playlistTable.getSelectionModel().getSelectedItems();
					List <Track> tracks = new ArrayList <Track> ();
					
					List <Playlist> serializableList = new ArrayList ( selectedPlaylists );
					
					for ( Playlist playlist : selectedPlaylists ) {
						tracks.addAll ( playlist.getTracks() );
					}
					
					DraggedTrackContainer dragObject = new DraggedTrackContainer( null, tracks, null, serializableList, DragSource.PLAYLIST_LIST );
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
					//REFACTOR: I can check for file extensions...
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
						int dropIndex = row.isEmpty() ? dropIndex = library.getPlaylists().size() : row.getIndex();
						library.addPlaylists( playlistsToAdd );
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
		
		playingColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, CurrentListTrackState>( "displayState" ) );
		artistColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "artist" ) );
		yearColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, Integer>( "year" ) );
		albumColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "fullAlbumTitle" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "title" ) );
		trackColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, Integer>( "trackNumber" ) );
		lengthColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "lengthDisplay" ) );

		playingColumn.setCellFactory ( column -> { 
				return new CurrentListTrackStateCell( this, playImageSource, pauseImageSource ); 
			} 
		);
		
		trackColumn.setCellFactory( column -> {
			return new TableCell <CurrentListTrack, Integer>() {
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
		});
		
		currentListTable = new TableView();
		currentListTable.getColumns().addAll( playingColumn, trackColumn, artistColumn, yearColumn, albumColumn, titleColumn, lengthColumn );
		albumTable.getSortOrder().add( trackColumn );
		currentListTable.setEditable( false );
		currentListTable.setItems( player.getCurrentList().getItems() );

		FixedWidthCustomResizePolicy resizePolicy = new FixedWidthCustomResizePolicy();
		currentListTable.setColumnResizePolicy( resizePolicy );

		resizePolicy.registerColumns( yearColumn, trackColumn );
		currentListTable.setPlaceholder( new Label( "No tracks in playlist." ) );
		currentListTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		playingColumn.setMaxWidth( 38 );
		playingColumn.setMinWidth( 38 );
		playingColumn.setResizable( false );
		
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
				//REFACTOR: This code is duplicated below. Put it in a function. 

				DraggedTrackContainer container = (DraggedTrackContainer) db.getContent( DRAGGED_TRACKS );
				
				switch ( container.getSource() ) {
					case TRACK_LIST:
					case ALBUM_INFO:
					case PLAYLIST_INFO:
					case TAG_ERROR_LIST:
					case HISTORY: {
						player.getCurrentList().appendTracks ( container.getTracks() );
					
					} break;

					case PLAYLIST_LIST: {
						player.getCurrentList().appendPlaylists ( container.getPlaylists() );
						
					} break;
					
					case ALBUM_LIST: {
						player.getCurrentList().appendAlbums ( container.getAlbums() );
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
										CurrentListTrack newAddMe = new CurrentListTrack ( addMe );
										player.getQueue().getData().remove ( index );
										player.getQueue().getData().add( index, newAddMe );
										tracksToCopy.add( newAddMe );
									}
								}
							}
							player.getCurrentList().appendTracks( tracksToCopy );
						}
						
					} break;
				}

				player.getQueue().updateQueueIndexes();
				event.setDropCompleted( true );
				event.consume();


		
			} else if ( db.hasFiles() ) {
				ArrayList <Track> tracksToAdd = new ArrayList();
				for ( File file : db.getFiles() ) {
					Path droppedPath = Paths.get( file.getAbsolutePath() );
					if ( Utils.isMusicFile( droppedPath ) ) {
						player.getCurrentList().appendTrack ( droppedPath );
						
					} else if ( Files.isDirectory( droppedPath ) ) {
						player.getCurrentList().appendTracksPathList ( Utils.getAllTracksInDirectory( droppedPath ) );
					} else if ( Utils.isPlaylistFile ( droppedPath ) ) {
						Playlist playlist = Playlist.loadPlaylist( droppedPath );
						if ( playlist != null ) {
							player.getCurrentList().appendPlaylist ( playlist );
						}
					}
				}

				event.setDropCompleted( true );
				event.consume();
			}

		});

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem queueMenuItem = new MenuItem( "Enqueue" );
		MenuItem shuffleMenuItem = new MenuItem( "Shuffle Items" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem infoMenuItem = new MenuItem( "Info" );
		MenuItem cropMenuItem = new MenuItem( "Crop" );
		MenuItem removeMenuItem = new MenuItem( "Remove" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );

		currentListTable.setOnKeyPressed( ( KeyEvent e ) -> {
			
			if ( e.getCode() == KeyCode.ESCAPE
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				currentListTable.getSelectionModel().clearSelection();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.DELETE      
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				removeMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.DELETE && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				cropMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.Q
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				queueMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.F2
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				editTagMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.F3
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				infoMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.F4
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				browseMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.R && e.isShiftDown() //PENDING: Put this on the hotkey list? 
			&& !e.isControlDown() && !e.isAltDown() && !e.isMetaDown() ) {
				shuffleMenuItem.fire();
				e.consume();
			}
		});
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );
		contextMenu.getItems().addAll( 
			playMenuItem, queueMenuItem, shuffleMenuItem, editTagMenuItem, infoMenuItem, 
			browseMenuItem, addToPlaylistMenuItem, cropMenuItem, removeMenuItem 
		);
		
		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				promptAndSavePlaylist ( new ArrayList <Track> ( currentListTable.getSelectionModel().getSelectedItems() ) );
			}
		});

		EventHandler addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				addToPlaylist ( Utils.convertCurrentTrackList ( currentListTable.getSelectionModel().getSelectedItems() ), playlist );
			}
		};

		library.getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
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
				List<Integer> selectedIndices = currentListTable.getSelectionModel().getSelectedIndices();
				
				if ( selectedIndices.size() < 2 ) {
					player.shuffleList();
					
				} else {
					player.getCurrentList().shuffleItems( selectedIndices );
				}
			}
		});
		
		editTagMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				
				tagWindow.setTracks( (List<Track>)(List<?>)currentListTable.getSelectionModel().getSelectedItems(), null );
				tagWindow.show();
			}
		});
		
		infoMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				trackInfoWindow.setTrack( currentListTable.getSelectionModel().getSelectedItem() );
				trackInfoWindow.show();
			}
		});

		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.playTrack( currentListTable.getSelectionModel().getSelectedItem() );
			}
		} );

		browseMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			// PENDING: This is the better way, once openjdk and openjfx supports
			// it: getHostServices().showDocument(file.toURI().toString());
			@Override
			public void handle ( ActionEvent event ) {
				SwingUtilities.invokeLater( new Runnable() {
					public void run () {
						try {
							Desktop.getDesktop().open( currentListTable.getSelectionModel().getSelectedItem().getPath().getParent().toFile() );
						} catch ( Exception e ) {
							LOGGER.log( Level.INFO, "Unable to native file browser.", e );
						}
					}
				} );
			}
		});
		
		removeMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ObservableList <Integer> selectedIndexes = currentListTable.getSelectionModel().getSelectedIndices();
				List <Integer> removeMe = new ArrayList<> ( selectedIndexes );
				int selectAfterDelete = selectedIndexes.get( 0 ) - 1;
				currentListTable.getSelectionModel().clearSelection();
				removeFromCurrentList ( removeMe );
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
		
		currentListTable.getSelectionModel().selectedItemProperty().addListener( ( obs, oldSelection, newSelection ) -> {
		    setImages ( newSelection );
		});
		
		currentListTable.setRowFactory( tv -> {
			TableRow <CurrentListTrack> row = new TableRow <>();
			

			row.setContextMenu( contextMenu );
			
			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && !row.isEmpty() ) {
					player.playTrack( row.getItem() );
				}
			});
			
			row.itemProperty().addListener( (obs, oldValue, newValue ) -> {
				if ( newValue != null ) {
			        if ( newValue.isMissingFile() ) {
			            row.getStyleClass().add( "file-missing" );
			        } else {
			            row.getStyleClass().remove( "file-missing" );
			        }
				}
		    });
			
			row.itemProperty().addListener( ( obs, oldValue, newTrackValue ) -> {
				if ( newTrackValue != null ) {
					newTrackValue.fileIsMissingProperty().addListener( ( o, old, newValue ) -> {
						if ( newValue ) {
							row.getStyleClass().add( "file-missing" );
						} else {
							row.getStyleClass().remove( "file-missing" );
						}
					});
				}
			});

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					ArrayList <Integer> indices = new ArrayList <Integer>( currentListTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( currentListTable.getSelectionModel().getSelectedItems() );
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, null, null, DragSource.CURRENT_LIST );
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
			});

			row.setOnDragDropped( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasContent( DRAGGED_TRACKS ) ) {

					DraggedTrackContainer container = (DraggedTrackContainer) db.getContent( DRAGGED_TRACKS );
					int dropIndex = row.getIndex();
					
					switch ( container.getSource() ) {
						case ALBUM_LIST: {
							player.getCurrentList().insertAlbums( dropIndex, container.getAlbums() );
						} break;
						
						case PLAYLIST_LIST:
						case TRACK_LIST:
						case ALBUM_INFO:
						case PLAYLIST_INFO:
						case TAG_ERROR_LIST:
						case HISTORY: {
							player.getCurrentList().insertTracks( dropIndex, Utils.convertTrackList( container.getTracks() ) );
						} break;
						
						case CURRENT_LIST: {
							List<Integer> draggedIndices = container.getIndices();
							
							player.getCurrentList().moveTracks ( draggedIndices, dropIndex );
							
							currentListTable.getSelectionModel().clearSelection();
							for ( int k = 0; k < draggedIndices.size(); k++ ) {
								int selectIndex = dropIndex + k;
								if ( selectIndex < currentListTable.getItems().size() ) {
									currentListTable.getSelectionModel().select( dropIndex + k );
								}
							}
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
											CurrentListTrack newAddMe = new CurrentListTrack ( addMe );
											player.getQueue().getData().remove ( index );
											player.getQueue().getData().add( index, newAddMe );
											tracksToCopy.add( newAddMe );
										}
									}
								}
								player.getCurrentList().insertTracks ( dropIndex, tracksToCopy );
							}
							
						} break;
					}

					player.getQueue().updateQueueIndexes( );
					event.setDropCompleted( true );
					event.consume();

				} else if ( db.hasFiles() ) {
					//REFACTOR: this code is in a bunch of places. We should probably make it a function
					ArrayList <Path> tracksToAdd = new ArrayList<Path> ();
					
					for ( File file : db.getFiles() ) {
						Path droppedPath = Paths.get( file.getAbsolutePath() );
						if ( Utils.isMusicFile( droppedPath ) ) {
							tracksToAdd.add( droppedPath );
						
						} else if ( Files.isDirectory( droppedPath ) ) {
							tracksToAdd.addAll( Utils.getAllTracksInDirectory( droppedPath ) );
						
						} else if ( Utils.isPlaylistFile ( droppedPath ) ) {
							List<Path> paths = Playlist.getTrackPaths( droppedPath );
							tracksToAdd.addAll( paths );
						}
					}
					
					if ( !tracksToAdd.isEmpty() ) {
						int dropIndex = row.isEmpty() ? dropIndex = trackTable.getItems().size() : row.getIndex();
						player.getCurrentList().insertTrackPathList ( dropIndex, tracksToAdd );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});

			return row;
		});
	}

	private void hackTooltipStartTiming() {
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
	    } catch ( Exception e ) {
	    	LOGGER.log( Level.INFO, "Unable accelerate tooltip popup speed.", e );
	    }
	    
	}
	
	public void setLoaderSpeedDisplay ( LoaderSpeed speed ) {
		this.libraryLocationWindow.setLoaderSpeedDisplay ( speed );
	}

	public void updateLibraryListPlaceholder() {

		if ( library.getAlbums().isEmpty() ) {
			if ( albumTable.getPlaceholder() != emptyAlbumListLabel ) {
				albumTable.setPlaceholder( emptyAlbumListLabel );
			}
		} else {
			if ( !albumTable.getPlaceholder().equals( filteredAlbumListLabel ) ) {
				albumTable.setPlaceholder( filteredAlbumListLabel );
			}
		}
		
		if ( library.getTracks().isEmpty() ) {
			if ( trackTable.getPlaceholder() != emptyTrackListLabel ) {
				trackTable.setPlaceholder( emptyTrackListLabel );
			}
		} else {
			if ( !trackTable.getPlaceholder().equals( filteredTrackListLabel ) ) {
				trackTable.setPlaceholder( filteredTrackListLabel );
			}
		}
		
		if ( library.getPlaylists().isEmpty() ) {
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
	
	public static void notifyUserHypnosRunning() {
		Alert alert = new Alert ( AlertType.INFORMATION );
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
		try {
			Image icon = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) );
			stage.getIcons().add( icon );
		} catch ( Exception e ) {}
		
		alert.setTitle( "Information" );
		alert.setHeaderText( "Unable to launch Hypnos" );
		alert.setContentText( "Hypnos is already running, and only one instance can run at a time. If you don't see it, please try terminating the orphaned process and try again." );
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.showAndWait();
		
	}
	public static void notifyUserError ( String message ) { 
		
		Alert alert = new Alert ( AlertType.ERROR );
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
		try {
			Image icon = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) );
			stage.getIcons().add( icon );
		} catch ( Exception e ) {}
		
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
		
		//These are the default positions.
		primarySplitPane.setDividerPositions( .35d );
		currentListSplitPane.setDividerPositions( .65d );
		artSplitPane.setDividerPosition( 0, .51d ); // For some reason .5 doesn't work...
			
		hackTooltipStartTiming();
	
		updateLibraryListPlaceholder();
	}


	public EnumMap <Persister.Setting, ? extends Object> getSettings () {
		
		EnumMap <Persister.Setting, Object> retMe = new EnumMap <Persister.Setting, Object> ( Persister.Setting.class );
		
		retMe.put ( Setting.HIDE_ALBUM_TRACKS, trackListCheckBox.isSelected() );
		
		boolean isMaximized = isMaximized();
		retMe.put ( Setting.WINDOW_MAXIMIZED, isMaximized );
		
		String theme = isDarkTheme ? "Dark" : "Light";
		
		if ( isMaximized ) {
			retMe.put ( Setting.WINDOW_X_POSITION, windowedX ); 
			retMe.put ( Setting.WINDOW_Y_POSITION, windowedY ); 
			retMe.put ( Setting.WINDOW_WIDTH, windowedWidth );
			retMe.put ( Setting.WINDOW_HEIGHT, windowedHeight );
		
		} else {
			retMe.put ( Setting.WINDOW_X_POSITION, mainStage.getX() );
			retMe.put ( Setting.WINDOW_Y_POSITION, mainStage.getY() );
			retMe.put ( Setting.WINDOW_WIDTH, mainStage.getWidth() );
			retMe.put ( Setting.WINDOW_HEIGHT, mainStage.getHeight() );
			retMe.put ( Setting.MAXIMIZED_ARTIST_PANE_HEIGHT, artistPaneHeightWhileMaximized );
		}
		
		retMe.put ( Setting.PRIMARY_SPLIT_PERCENT, getPrimarySplitPercent() );
		retMe.put ( Setting.CURRENT_LIST_SPLIT_PERCENT, getCurrentListSplitPercent() );
		retMe.put ( Setting.ART_SPLIT_PERCENT, getArtSplitPercent() );
		retMe.put ( Setting.LIBRARY_TAB, libraryPane.getSelectionModel().getSelectedIndex() );
		retMe.put ( Setting.PROMPT_BEFORE_OVERWRITE, promptBeforeOverwrite.getValue() );
		retMe.put ( Setting.THEME, theme );
		
		return retMe;
	}

	public void refreshQueueList() {
		queueWindow.refresh();
	}
	
	public void refreshHistory() {
		historyWindow.refresh();
	}
	
	public void refreshCurrentList () {
		for ( CurrentListTrack track : currentListTable.getItems() ) {
			try {
				track.refreshTagData();
			} catch ( Exception e ) {
				track.setIsMissingFile( true ); //TODO: Do we want to make another flag or rename isMissingFile?
			}
		}
		currentListTable.refresh();
	}
	
	public void refreshAlbumTable () {
		albumTable.refresh();
	}


	public void refreshTrackTable () {
		trackTable.refresh();
	}

	public void applySettings( EnumMap<Persister.Setting, String> settings ) {
		settings.forEach( ( setting, value )-> {
			try {
				switch ( setting ) {
					//REFACTOR: These don't belong here. 
					case TRACK:
						Path trackPath = Paths.get( value );
						Path albumPath = null;
						if ( Utils.isAlbumDirectory( trackPath.toAbsolutePath().getParent() ) ) {
							albumPath = trackPath.toAbsolutePath().getParent();
						}
						Track track = new Track ( trackPath, albumPath );
						setImages( track );
						player.playTrack( track, true );
						break;
						
					case TRACK_POSITION:
						player.seekMS( Long.parseLong( value ) );
						break;
						
					case SHUFFLE:
						player.setShuffleMode ( AudioSystem.ShuffleMode.valueOf( value ) );
						break;
						
					case REPEAT:
						player.setRepeatMode ( AudioSystem.RepeatMode.valueOf( value ) );
						break;
						
					case VOLUME:
						player.setVolumePercent( Double.valueOf ( value ) );
						playerVolumeChanged ( Double.valueOf ( value ) ); //This is kind of a hack. 
						break;
						
					case TRACK_NUMBER:
						try {
							int tracklistNumber = Integer.parseInt( value );
							if ( tracklistNumber != -1 ) {
								player.getCurrentList().getItems().get( tracklistNumber ).setIsCurrentTrack( true );
							}
						} catch ( Exception e ) {
							LOGGER.info( "Error loading current list track number: " + e.getMessage() );
						}
						
						break;
						
					case DEFAULT_REPEAT_ALBUMS:
						player.getCurrentList().setDefaultAlbumRepeatMode( CurrentList.DefaultRepeatMode.valueOf( value ) );
						break;
						
					case DEFAULT_REPEAT_PLAYLISTS:
						player.getCurrentList().setDefaultPlaylistRepeatMode( CurrentList.DefaultRepeatMode.valueOf( value ) );
						
						break;
						
					case DEFAULT_REPEAT_TRACKS:
						player.getCurrentList().setDefaultTrackRepeatMode( CurrentList.DefaultRepeatMode.valueOf( value ) );
						break;
						
					case DEFAULT_SHUFFLE_ALBUMS:
						player.getCurrentList().setDefaultAlbumShuffleMode( CurrentList.DefaultShuffleMode.valueOf( value ) );
						break;
						
					case DEFAULT_SHUFFLE_PLAYLISTS:
						player.getCurrentList().setDefaultPlaylistShuffleMode( CurrentList.DefaultShuffleMode.valueOf( value ) );
						break;
						
					case DEFAULT_SHUFFLE_TRACKS:
						player.getCurrentList().setDefaultTrackShuffleMode( CurrentList.DefaultShuffleMode.valueOf( value )  );
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
						
					case LIBRARY_TAB:
						libraryPane.getSelectionModel().select( Integer.valueOf ( value ) );
						break;
					
					case MAXIMIZED_ARTIST_PANE_HEIGHT:
						artistPaneHeightWhileMaximized = Double.valueOf( value );
						
					case PROMPT_BEFORE_OVERWRITE:
						promptBeforeOverwrite.setValue( Boolean.valueOf( value ) );
						break;
					
					case THEME:
						if ( value.equalsIgnoreCase( "dark" ) ) {
							applyDarkTheme();
						} else {
							removeDarkTheme();
						}
						break;
						
					case LOADER_SPEED:
					default:
						//Do nothing
						break;
				}
			} catch ( Exception e ) {
				LOGGER.log( Level.INFO, "Unable to apply setting: " + setting + " to UI.", e );
			}
		});
		

		settingsWindow.updateSettings();
	}


	public Window getMainStage () {
		return mainStage;
	}

	@Override
	public void playerPositionChanged ( int positionMS, int lengthMS ) {
		Platform.runLater( () -> {
			int timeElapsedS = positionMS / 1000;
			int timeRemainingS = ( lengthMS / 1000 ) - timeElapsedS;
			double percent = positionMS / (double)lengthMS;
			updateTransport( timeElapsedS, timeRemainingS, percent );
		});
	}


	@Override
	public void playerStopped ( Track track, StopReason reason ) {
		Platform.runLater( () -> {
			updateTransport( 0, 0, 0 ); //values don't matter. 
			volumeSlider.setDisable( false );
			volumeMuteButton.setDisable( false );
		});
	}


	@Override
	public void playerStarted ( Track track ) {
		Platform.runLater( () -> {
			togglePlayButton.setGraphic( pauseImage );
			
			currentListTable.refresh();
	
			StackPane thumb = (StackPane) trackPositionSlider.lookup( ".thumb" );
			thumb.setVisible( true );
			
			currentTrackButton.setText( track.getArtist() + " - " + track.getTitle() );
			currentTrackTooltip.setText( 
				"Album: " + track.getSimpleAlbumTitle() + "\n" +
				"Year: " + track.getYear() + "\n" +
				"Length: " + Utils.getLengthDisplay( track.getLengthS() ) + "\n" + 
				"Encoding: " + track.getShortEncodingString()
			);
			
			setImages( track );
			
			boolean disableVolume = !player.volumeChangeSupported();
			
			if ( disableVolume ) {
				volumeSlider.setDisable( true );
				volumeMuteButton.setDisable( true );
				Tooltip.install( volumePane, volumeDisabledTooltip );
			} else {
				volumeSlider.setDisable( false );
				volumeMuteButton.setDisable( false );
				Tooltip.uninstall( volumePane, volumeDisabledTooltip );
			}
		});
	}

	@Override
	public void playerPaused () {
		Platform.runLater( () -> {
			togglePlayButton.setGraphic( playImage );
			currentListTable.refresh(); //To get the play/pause image to update. 
		});
	}


	@Override
	public void playerUnpaused () {
		Platform.runLater( () -> {
			togglePlayButton.setGraphic( pauseImage );
			currentListTable.refresh();//To get the play/pause image to update. 
		});
		
	}


	@Override
	public void playerVolumeChanged ( double newVolumePercent ) {
		Platform.runLater( () -> {
			double min = volumeSlider.getMin();
			double max = volumeSlider.getMax();
			
			volumeSlider.setValue( newVolumePercent * ( max - min ) );
			
			if ( newVolumePercent == 0 ) {
				volumeMuteButton.setGraphic( volumeImages[0] );
			} else if ( newVolumePercent > 0 && newVolumePercent <= .33f ) {
				volumeMuteButton.setGraphic( volumeImages[1] );
			} else if ( newVolumePercent > .33f && newVolumePercent <= .66f ) {
				volumeMuteButton.setGraphic( volumeImages[2] );
			} else if ( newVolumePercent > .66f ) {
				volumeMuteButton.setGraphic( volumeImages[3] );
			}
		});
	}

	@Override
	public void playerShuffleModeChanged ( ShuffleMode newMode ) {
		Platform.runLater( () -> {
			updateShuffleButtonImages();
		});
		
	}

	@Override
	public void playerRepeatModeChanged ( RepeatMode newMode ) {
		Platform.runLater( () -> {
			updateRepeatButtonImages();
		});
		
	}

	public boolean hotkeysDisabledForConfig () {
		return settingsWindow.hotkeysDisabledForConfig();
	}

	public void refreshHotkeyList () {
		settingsWindow.refreshHotkeyFields();
	}
	
	public void warnUserVolumeNotSet() {
		Platform.runLater( () -> {
			Alert alert = new Alert( AlertType.ERROR );
			alert.getDialogPane().applyCss();
			double x = mainStage.getX() + mainStage.getWidth() / 2 - 220; //It'd be nice to use alert.getWidth() / 2, but it's NAN now. 
			double y = mainStage.getY() + mainStage.getHeight() / 2 - 50;
			
			alert.setX( x );
			alert.setY( y );
			
			alert.setTitle( "Warning" );
			alert.setHeaderText( "Unable to set volume." );
				
			Text text = new Text(
				"System does not support software volume control for this audio format.\n\n" +
				"Please set your physical speakers and system sound " +
				"to a reasonable level to avoid damaging your ear drums and audio system.\n\n" +
				"When you have done so, set Hypnos's volume to 100 and start play again.");
			
			text.setWrappingWidth(500);
			text.applyCss();
			HBox holder = new HBox();
			holder.getChildren().add( text );
			holder.setPadding( new Insets ( 10, 10, 10, 10 ) );
			alert.getDialogPane().setContent( holder );
			
			alert.showAndWait();
		});
	}

	public void warnUserPlaylistsNotSaved ( List <Playlist> errors ) {
		if ( doPlaylistSaveWarning ) {
			Platform.runLater( () -> {
				doPlaylistSaveWarning = false;
				Alert alert = new Alert( AlertType.ERROR );
				alert.getDialogPane().applyCss();
				double x = mainStage.getX() + mainStage.getWidth() / 2 - 220; //It'd be nice to use alert.getWidth() / 2, but it's NAN now. 
				double y = mainStage.getY() + mainStage.getHeight() / 2 - 50;
				
				alert.setX( x );
				alert.setY( y );
				
				alert.setTitle( "Warning" );
				alert.setHeaderText( "Unable to save playlists." );
					
				String message = "Unable to save the following playlists to the default playlist directory. " +
						"You may want to manually export them before exiting, so your data is not lost.\n";
						
				for ( Playlist playlist : errors ) {
					
					if ( playlist != null ) {
						message += "\n" + playlist.getName();
					}
				}
					
				Text text = new Text( message );
				
				text.setWrappingWidth(500);
				text.applyCss();
				HBox holder = new HBox();
				holder.getChildren().add( text );
				holder.setPadding( new Insets ( 10, 10, 10, 10 ) );
				alert.getDialogPane().setContent( holder );
				
				alert.showAndWait();
			});
		}
	}
	
	public void warnUserAlbumsMissing ( List <Album> missing ) {
		Platform.runLater( () -> {

			Alert alert = new Alert( AlertType.ERROR );
			alert.getDialogPane().applyCss();
			double x = mainStage.getX() + mainStage.getWidth() / 2 - 220; //It'd be nice to use alert.getWidth() / 2, but it's NAN now. 
			double y = mainStage.getY() + mainStage.getHeight() / 2 - 50;
			
			alert.setX( x );
			alert.setY( y );
			
			alert.setTitle( "Unable to load Albums" );
			alert.setHeaderText( "Albums have been deleted or moved." );
				
			String message = "Unable to load the following albums because the folder is missing. If you recently moved or renamed the folder " +
				"hypnos will find and load the new location soon (as long as its in your library load path).";
					
			for ( Album album : missing ) {
				if ( album != null ) {
					message += "\n\n" + album.getPath();
				}
			}
				
			Text text = new Text( message );
			
			text.setWrappingWidth(500);
			text.applyCss();
			HBox holder = new HBox();
			holder.getChildren().add( text );
			holder.setPadding( new Insets ( 10, 10, 10, 10 ) );
			alert.getDialogPane().setContent( holder );
			
			alert.showAndWait();
		});
	}
	
	public File promptUserForPlaylistFile() {
		FileChooser fileChooser = new FileChooser();
		FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter( "M3U Playlists", Arrays.asList( "*.m3u" ) );
		fileChooser.getExtensionFilters().add( fileExtensions );
		fileChooser.setTitle( "Export Playlist" );
		fileChooser.setInitialFileName( "new-playlist.m3u" );
		File targetFile = fileChooser.showSaveDialog( mainStage );
		return targetFile;
	}
	
	
	public void alertUser ( AlertType type, String title, String header, String content, double textWidth ) {
		Alert alert = new Alert( type );
		alert.getDialogPane().applyCss();
		double x = mainStage.getX() + mainStage.getWidth() / 2 - 220; //It'd be nice to use alert.getWidth() / 2, but it's NAN now. 
		double y = mainStage.getY() + mainStage.getHeight() / 2 - 50;
		
		alert.setX( x );
		alert.setY( y );
		
		alert.setTitle( title );
		alert.setHeaderText( header );
						
		Text text = new Text( content );
		
		text.setWrappingWidth( textWidth );
		text.applyCss();
		HBox holder = new HBox();
		holder.getChildren().add( text );
		holder.setPadding( new Insets ( 10, 10, 10, 10 ) );
		alert.getDialogPane().setContent( holder );
		
		alert.showAndWait(); 
	}
	
	public Track getCurrentImagesTrack() {
		return currentImagesTrack;
	}
	
	public void openWebBrowser ( String url ) {
		
		switch ( Hypnos.getOS() ) {
			case NIX: {
				try {
					new ProcessBuilder("x-www-browser", url ).start();
				} catch ( Exception e ) {
					LOGGER.log( Level.INFO, "Unable to open web browser.", e );
				}
				break;
			}
				
			case OSX: {
				Runtime rt = Runtime.getRuntime();
				try {
					rt.exec("open " + url);
				} catch ( Exception e ) {
					LOGGER.log( Level.INFO, "Unable to open web browser.", e );
				}
				break;
			}
				
			case WIN_10: case WIN_7: case WIN_8: case WIN_UNKNOWN: case WIN_VISTA: case WIN_XP:
			case UNKNOWN: {
				
				try {
					Desktop.getDesktop().browse( new URI ( url ) );
					
				} catch ( Exception e ) {
					try {
						Runtime rt = Runtime.getRuntime();
						rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
						
					} catch ( Exception e2 ) {
						LOGGER.log( Level.INFO, "Unable to open web browser.", e );
					}
				}
				break;
			}
		}
	}
}

class LineNumbersCellFactory<T, E> implements Callback<TableColumn<T, E>, TableCell<T, E>> {

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

class CurrentListTrackStateCell extends TableCell <CurrentListTrack, CurrentListTrackState> {
	
	private ImageView pauseImage, playImage;
	private FXUI ui;
	private boolean isDarkTheme = false;
	
	public CurrentListTrackStateCell ( FXUI ui, Image playImageSource, Image pauseImageSource ) {
		this.ui = ui;
		
		playImage = new ImageView ( playImageSource );
		playImage.setFitHeight( 13 );
		playImage.setFitWidth( 13 );
		
		pauseImage = new ImageView ( pauseImageSource );
		pauseImage.setFitHeight( 13 );
		pauseImage.setFitWidth( 13 );

		setContentDisplay ( ContentDisplay.LEFT );
		setGraphicTextGap ( 2 );
		this.setAlignment( Pos.CENTER_LEFT );
	}
	
	protected void updateImageThemes ( ) {
		if ( ui.isDarkTheme() && !isDarkTheme ) {
			playImage.setEffect( ui.getDarkThemeTransportButtonsAdjust() );
			pauseImage.setEffect( ui.getDarkThemeTransportButtonsAdjust() );
			
		} else if ( !ui.isDarkTheme() && isDarkTheme ) {
			playImage.setEffect( null );
			pauseImage.setEffect( null );
		}
		
	}
	
	@Override
	protected void updateItem ( CurrentListTrackState state, boolean empty ) {
		super.updateItem( state, empty );
		
		updateImageThemes();

		if ( state != null ) {
			if ( state.getIsCurrentTrack() ) {
				
				ImageView playPauseImage = ui.player.isPaused() ? pauseImage : playImage;
				
				if ( state.getQueueIndices().size() > 0 ) {
					setGraphic ( playPauseImage );
					setText ( "+" );
					
				} else {
					setGraphic ( playPauseImage );
					setText ( null );
				}

			} else if ( state.getQueueIndices().size() == 1 ) {
				setText ( state.getQueueIndices().get( 0 ).toString() );
				setGraphic ( null );
				
			} else if ( state.getQueueIndices().size() >= 1 ) {
				setText ( state.getQueueIndices().get( 0 ).toString() + "+" );
				setGraphic ( null );
				
			} else {
				setText ( "" );
				setGraphic ( null );
				
			}
		} else {
			setText( null );
			setGraphic( null );
		}
	}
}
