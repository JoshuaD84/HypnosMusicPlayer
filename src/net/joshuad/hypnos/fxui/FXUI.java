package net.joshuad.hypnos.fxui;

import java.awt.Desktop;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
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
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import net.joshuad.hypnos.Album;
import net.joshuad.hypnos.AlphanumComparator;
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
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.AlphanumComparator.CaseHandling;
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

	SplitPane primarySplitPane;
	SplitPane currentListSplitPane;
	public ImagesPanel artSplitPane; //TODO: Probably make this private
	StretchedTabPane libraryPane;
	
	ImageView albumFilterClearImage, trackFilterClearImage, playlistFilterClearImage;
	ImageView noRepeatImage, repeatImage, repeatOneImage, sequentialImage, shuffleImage;
	ImageView queueImage, historyImage, menuImage;
	ImageView addSourceTracksImage, addSourceAlbumsImage, addSourcePlaylistsImage;
	
	Image warningAlertImageSource;
	Image repeatImageSource;
	
	HBox albumFilterPane;
	HBox trackFilterPane;
	HBox playlistFilterPane;
	HBox playlistControls;
	
	ContextMenu playlistColumnSelectorMenu, trackColumnSelectorMenu, albumColumnSelectorMenu, currentListColumnSelectorMenu;
	TableColumn playlistNameColumn, playlistLengthColumn, playlistTracksColumn;
	TableColumn trackArtistColumn, trackLengthColumn, trackNumberColumn, trackAlbumColumn, trackTitleColumn;
	TableColumn albumArtistColumn, albumYearColumn, albumAlbumColumn;
	TableColumn clPlayingColumn, clArtistColumn, clYearColumn, clAlbumColumn, clTitleColumn, clNumberColumn, clLengthColumn;
	
	Transport transport;

	Tab libraryTrackTab, libraryAlbumTab, libraryPlaylistTab;
	
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
	LyricsWindow lyricsWindow;
	JumpWindow jumpWindow;

	Button toggleRepeatButton, toggleShuffleButton;
	Button showQueueButton;
	MenuItem currentListSave, currentListExport, currentListLoad, historyMenuItem;
	
	CheckBox trackListCheckBox;
	TextField trackFilterBox, albumFilterBox, playlistFilterBox;
	
	final AudioSystem player;
	final Library library;
	
	private double windowedWidth = 1024;
	private double windowedHeight = 768;
	private double windowedX = 50;
	private double windowedY = 50;
	
	public Track currentImagesTrack = null; //TODO: Make private
	public Album currentImagesAlbum = null; //TODO: Make private
	
	public File darkStylesheet; //TODO: Make private
	private File baseStylesheet;
	
	private boolean isDarkTheme = false;
	
	private double primarySplitPaneDefault = .35d;
	private double currentListSplitPaneDefault = .75d;
	private double artSplitPaneDeafult = .51d;// For some reason .5 doesn't work...
	
	private Double currentListSplitPaneRestoredPosition = null;
	private Double primarySplitPaneRestoredPosition = null;

	private ColorAdjust darkThemeButtons = new ColorAdjust(); {
		darkThemeButtons.setSaturation( -1 );
		darkThemeButtons.setHue( 1 );
		darkThemeButtons.setBrightness( .75 );
	}

	private SimpleBooleanProperty promptBeforeOverwrite = new SimpleBooleanProperty ( true );
	
	boolean doPlaylistSaveWarning = true;
	
	public FXUI ( Stage stage, Library library, AudioSystem audioSystem, GlobalHotkeys hotkeys ) {
		mainStage = stage;
		this.library = library;
		this.player = audioSystem;
		
		audioSystem.getCurrentList().addNoLoadThread( Thread.currentThread() );
		
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
		transport = new Transport( this, audioSystem );
		
		libraryLocationWindow = new LibraryLocationWindow ( mainStage, library );
		tagWindow = new TagWindow ( this ); 
		queueWindow = new QueueWindow ( this, library, audioSystem, tagWindow );
		albumInfoWindow = new AlbumInfoWindow ( this, library, audioSystem );
		playlistInfoWindow = new PlaylistInfoWindow ( this, library, audioSystem );
		historyWindow = new HistoryWindow ( this, library, audioSystem );
		settingsWindow = new SettingsWindow ( this, library, hotkeys, audioSystem );
		trackInfoWindow = new TrackInfoWindow ( this );
		lyricsWindow = new LyricsWindow ( this );
		jumpWindow = new JumpWindow ( this, library, audioSystem );

		applyBaseTheme();
		applyDarkTheme();
		
		artSplitPane = new ImagesPanel ( this, audioSystem );

		BorderPane currentPlayingPane = new BorderPane();
		playlistControls.prefWidthProperty().bind( currentPlayingPane.widthProperty() );
		currentPlayingPane.setTop( playlistControls );
		currentPlayingPane.setCenter( currentListTable );

		currentListSplitPane = new SplitPane();
		currentListSplitPane.setOrientation( Orientation.VERTICAL );
		currentListSplitPane.getItems().addAll( currentPlayingPane, artSplitPane );
		currentPlayingPane.setMinWidth( 0 );
		artSplitPane.setMinWidth( 0 );

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
		
		libraryAlbumTab = new Tab( "Albums" );
		libraryAlbumTab.setContent( albumListPane );
		libraryAlbumTab.setClosable( false );
		Tooltip albumTabTooltip = new Tooltip ( "Album Count: " + library.getAlbums().size() );
		libraryAlbumTab.setTooltip( albumTabTooltip );
		
		library.getAlbums().addListener( new ListChangeListener<Album> () {
			public void onChanged ( Change <? extends Album> changed ) {
				albumTabTooltip.setText( "Album Count: " + library.getAlbums().size() );
			}
		});

		libraryPlaylistTab = new Tab( "Playlists" );
		libraryPlaylistTab.setContent( playlistPane );
		libraryPlaylistTab.setClosable( false );
		Tooltip playlistTabTooltip = new Tooltip ( "Playlist Count: " + library.getPlaylists().size() );
		libraryPlaylistTab.setTooltip( playlistTabTooltip );
		
		library.getPlaylists().addListener( new ListChangeListener<Playlist> () {
			public void onChanged ( Change <? extends Playlist> changed ) {
				playlistTabTooltip.setText( "Playlist Count: " + library.getPlaylists().size() );
			}
		});

		libraryTrackTab = new Tab( "Tracks" );
		libraryTrackTab.setContent( trackListPane );
		libraryTrackTab.setClosable( false );
		Tooltip trackTabTooltip = new Tooltip ( "Track Count: " + library.getTracks().size() );
		libraryTrackTab.setTooltip( trackTabTooltip );
		
		library.getTracks().addListener( new ListChangeListener<Track> () {
			public void onChanged ( Change <? extends Track> changed ) {
				trackTabTooltip.setText( "Track Count: " + library.getTracks().size() );
			}
		});

		libraryPane.getTabs().addAll( libraryAlbumTab, libraryTrackTab, libraryPlaylistTab );
		libraryPane.setSide( Side.BOTTOM );

		primarySplitPane = new SplitPane();
		primarySplitPane.getItems().addAll( libraryPane, currentListSplitPane );

		final BorderPane primaryContainer = new BorderPane();

		primaryContainer.prefWidthProperty().bind( scene.widthProperty() );
		primaryContainer.prefHeightProperty().bind( scene.heightProperty() );
		primaryContainer.setPadding( new Insets( 0 ) ); 
		primaryContainer.setCenter( primarySplitPane );
		primaryContainer.setTop( transport );
		
		SplitPane.setResizableWithParent( libraryPane, Boolean.FALSE );
		SplitPane.setResizableWithParent( artSplitPane, Boolean.FALSE );
		SplitPane.setResizableWithParent( currentListSplitPane, Boolean.FALSE );
		SplitPane.setResizableWithParent( primarySplitPane, Boolean.FALSE );
		SplitPane.setResizableWithParent( currentPlayingPane, Boolean.FALSE );
		
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
			if ( e.getCode() == KeyCode.S && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				currentListSave.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.F && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				Tab currentLibraryTab = libraryPane.getSelectionModel().getSelectedItem();
				
				if ( libraryAlbumTab == currentLibraryTab ) {
					albumFilterBox.requestFocus();
					
				} else if ( libraryTrackTab == currentLibraryTab ) {
					trackFilterBox.requestFocus();
					
				} else if ( libraryPlaylistTab == currentLibraryTab ) {
					playlistFilterBox.requestFocus();
				}
				
				e.consume();
				
			} else if ( e.getCode() == KeyCode.E && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				currentListExport.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.O && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				currentListLoad.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.P && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				showSettingsWindow();
				e.consume();
			
			} else if ( e.getCode() == KeyCode.F 
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				jumpWindow.show();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.R
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				toggleRepeatButton.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.S
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				toggleShuffleButton.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.H
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				historyWindow.show();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.Q && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				queueWindow.show();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.L && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				lyricsWindow.setTrack( audioSystem.getCurrentTrack() );
				lyricsWindow.show();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.L && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				toggleLibraryCollapsed();
				e.consume();
						
			} else if ( e.getCode() == KeyCode.SEMICOLON && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				toggleArtPaneCollapsed();
				e.consume();
				
			} else if ( ( e.getCode() == KeyCode.NUMPAD1 || e.getCode() == KeyCode.KP_UP ) 
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				audioSystem.skipMS( -5000 );
				e.consume();
				
			} else if ( ( e.getCode() == KeyCode.NUMPAD2 || e.getCode() == KeyCode.KP_DOWN ) 
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				transport.stopButton.fire();
				e.consume();
				
			} else if ( ( e.getCode() == KeyCode.NUMPAD3 || e.getCode() == KeyCode.KP_RIGHT ) 
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				audioSystem.skipMS( 5000 );
				e.consume();
				
			} else if ( ( e.getCode() == KeyCode.NUMPAD4 || e.getCode() == KeyCode.KP_LEFT )
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				transport.previousButton.fire();
				e.consume();
				
			} else if ( ( e.getCode() == KeyCode.NUMPAD5 || e.getCode() == KeyCode.KP_UP ) 
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				transport.togglePlayButton.fire();
				e.consume();
				
			} else if ( ( e.getCode() == KeyCode.NUMPAD6 || e.getCode() == KeyCode.KP_DOWN ) 
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				transport.nextButton.fire();
				e.consume();
				
			} else if ( ( e.getCode() == KeyCode.NUMPAD7 || e.getCode() == KeyCode.KP_RIGHT ) 
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				audioSystem.decrementVolume();
				e.consume();
				
			} else if ( ( e.getCode() == KeyCode.NUMPAD8 || e.getCode() == KeyCode.KP_LEFT )
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				transport.volumeMuteButton.fire();
				e.consume();
				
			} else if ( ( e.getCode() == KeyCode.NUMPAD9 || e.getCode() == KeyCode.KP_LEFT )
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				audioSystem.incrementVolume();
				e.consume();
				
			} 
		});
		
		primarySplitPane.setDividerPositions( primarySplitPaneDefault );
		currentListSplitPane.setDividerPositions( currentListSplitPaneDefault );
		artSplitPane.setDividerPosition( 0, artSplitPaneDeafult ); 
		
		mainStage.widthProperty().addListener( windowSizeListener );
		mainStage.heightProperty().addListener( windowSizeListener );
		
		audioSystem.addPlayerListener ( this );
	}
	
	public void showSettingsWindow() {
		settingsWindow.show();
	}
	
	public ColorAdjust getDarkThemeTransportButtonsAdjust () {
		return transport.getDarkThemeButtonAdjust();
	}
	
	private void setupFont() {
		Path stylesheet; 
		switch ( Hypnos.getOS() ) {
			case OSX:
				stylesheet = Hypnos.getRootDirectory().resolve( "resources/style-font-osx.css" );
				
			case WIN_10:
			case WIN_7:
			case WIN_8:
			case WIN_UNKNOWN:
			case WIN_VISTA:
			case WIN_XP:
			case UNKNOWN:
				stylesheet = Hypnos.getRootDirectory().resolve( "resources/style-font-win.css" );
				break;
				
			case NIX:
			default:
				stylesheet = Hypnos.getRootDirectory().resolve( "resources/style-font-nix.css" );
				break;
		}
		
		scene.getStylesheets().add( "file:///" + stylesheet.toFile().getAbsolutePath().replace( "\\", "/" ) );
	}
	
	private void loadImages() {
		double currentListControlsButtonFitWidth = 15;
		double currentListControlsButtonFitHeight = 15;
		
		//TODO: Fix warning messages
		try {
			noRepeatImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/no-repeat.png" ).toFile() ) ) );
			noRepeatImage.setFitWidth( currentListControlsButtonFitWidth );
			noRepeatImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load no repeat icon: resources/no-repeat.png", e );
		}
		
		try {
			repeatImageSource = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/repeat.png" ).toFile() ) );
			repeatImage = new ImageView ( repeatImageSource );
			repeatImage.setFitWidth( currentListControlsButtonFitWidth );
			repeatImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load repeat icon: resources/repeat.png", e );
		}
		
		
		try {
			repeatOneImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/repeat-one.png" ).toFile() ) ) );
			repeatOneImage.setFitWidth( currentListControlsButtonFitWidth );
			repeatOneImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load repeat one icon: resources/repeat-one", e );
		}
		
		try {
			sequentialImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/sequential.png" ).toFile() ) ) );
			sequentialImage.setFitWidth( currentListControlsButtonFitWidth );
			sequentialImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load sequential icon: resources/sequential.png", e );
		}
		
		try {
			shuffleImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/shuffle.png" ).toFile() ) ) );
			shuffleImage.setFitWidth( currentListControlsButtonFitWidth );
			shuffleImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load shuffle icon: resources/shuffle.png", e );
		}
		
		try {
			queueImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/queue.png" ).toFile() ) ) );
			queueImage.setFitWidth( currentListControlsButtonFitWidth );
			queueImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load queue icon: resources/queue.png", e );
		}
		
		try {
			historyImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/history.png" ).toFile() ) ) );
			historyImage.setFitWidth( currentListControlsButtonFitWidth );
			historyImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load history icon: resources/history.png", e );
		}
		
		
		try {
			menuImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/menu.png" ).toFile() ) ) );
			menuImage.setFitWidth( currentListControlsButtonFitWidth );
			menuImage.setFitHeight( currentListControlsButtonFitHeight );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load menu icon: resources/menu.png", e );
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
			LOGGER.log( Level.WARNING, "Unable to load add icon: resources/add.png", e );
		}
		
		try {
			warningAlertImageSource = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/alert-warning.png" ).toFile() ) );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load warning alert icon: resources/alert-warning.png", e );
		}
			
		try {
			Image clearImage = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/clear.png" ).toFile() ) );
			
			albumFilterClearImage = new ImageView ( clearImage );
			trackFilterClearImage = new ImageView ( clearImage );
			playlistFilterClearImage = new ImageView ( clearImage );

			albumFilterClearImage.setFitWidth( 12 );
			albumFilterClearImage.setFitHeight( 12 );
			trackFilterClearImage.setFitWidth( 12 );
			trackFilterClearImage.setFitHeight( 12 );
			playlistFilterClearImage.setFitWidth( 12 );
			playlistFilterClearImage.setFitHeight( 12 );
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load clear icon: resources/clear.png", e );
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
		lyricsWindow.getScene().getStylesheets().add( baseSheet );
		jumpWindow.getScene().getStylesheets().add( baseSheet );
	}
	
	public void applyDarkTheme() {
		if ( !isDarkTheme ) {
			String darkSheet = "file:///" + darkStylesheet.getAbsolutePath().replace( "\\", "/" );
			isDarkTheme = true;
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
			lyricsWindow.getScene().getStylesheets().add( darkSheet );
			jumpWindow.getScene().getStylesheets().add( darkSheet );
			
			transport.applyDarkTheme();
		
			if ( albumFilterClearImage != null ) albumFilterClearImage.setEffect( darkThemeButtons );
			if ( trackFilterClearImage != null ) trackFilterClearImage.setEffect( darkThemeButtons );
			if ( playlistFilterClearImage != null ) playlistFilterClearImage.setEffect( darkThemeButtons );
			
			if ( noRepeatImage != null ) noRepeatImage.setEffect( darkThemeButtons );
			if ( repeatImage != null ) repeatImage.setEffect( darkThemeButtons );
			if ( repeatOneImage != null ) repeatOneImage.setEffect( darkThemeButtons );
			if ( sequentialImage != null ) sequentialImage.setEffect( darkThemeButtons );
			if ( shuffleImage != null ) shuffleImage.setEffect( darkThemeButtons );
			if ( menuImage != null ) menuImage.setEffect( darkThemeButtons );
			if ( queueImage != null ) queueImage.setEffect( darkThemeButtons );
			if ( historyImage != null ) historyImage.setEffect( darkThemeButtons );
			if ( addSourceTracksImage != null ) addSourceTracksImage.setEffect( darkThemeButtons );
			if ( addSourceAlbumsImage != null ) addSourceAlbumsImage.setEffect( darkThemeButtons );
			if ( addSourcePlaylistsImage != null ) addSourcePlaylistsImage.setEffect( darkThemeButtons );
			
			currentListTable.refresh();
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
		lyricsWindow.getScene().getStylesheets().remove( darkSheet );
		jumpWindow.getScene().getStylesheets().remove( darkSheet );
		
		transport.removeDarkTheme();

		if ( albumFilterClearImage != null ) albumFilterClearImage.setEffect( null );
		if ( trackFilterClearImage != null ) trackFilterClearImage.setEffect( null );
		if ( playlistFilterClearImage != null ) playlistFilterClearImage.setEffect( null );
		
		if ( noRepeatImage != null ) noRepeatImage.setEffect( null );
		if ( repeatImage != null ) repeatImage.setEffect( null );
		if ( repeatOneImage != null ) repeatOneImage.setEffect( null );
		if ( sequentialImage != null ) sequentialImage.setEffect( null );
		if ( shuffleImage != null ) shuffleImage.setEffect( null );
		if ( menuImage != null ) menuImage.setEffect( null );
		if ( queueImage != null ) queueImage.setEffect( null );
		if ( historyImage != null ) historyImage.setEffect( null );
		if ( addSourceTracksImage != null ) addSourceTracksImage.setEffect( null );
		if ( addSourceAlbumsImage != null ) addSourceAlbumsImage.setEffect( null );
		if ( addSourcePlaylistsImage != null ) addSourcePlaylistsImage.setEffect( null );
		
		currentListTable.refresh();
	}
	
	public boolean isDarkTheme() {
		return isDarkTheme;
	}
		
	//REFACTOR: Does this function need to exist? 
	void removeFromCurrentList ( List<Integer> removeMe ) {
		
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
		transport.update ( timeElapsedS, timeRemainingS, percent );
	}
	
	public void toggleMinimized() {
		if ( mainStage.isIconified() ) {
			mainStage.setIconified( false );
		} else {
			mainStage.setIconified( true );
		}
	}
	
	public void restoreWindow() {
		mainStage.setIconified( false );
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

	public void selectCurrentTrack () {
		Track current = player.getCurrentTrack();
		selectTrackOnCurrentList ( current );
	}
	
	public void selectTrackOnCurrentList ( Track selectMe ) {
		if ( selectMe != null ) {
			synchronized ( currentListTable.getItems() ) {
				int itemIndex = currentListTable.getItems().indexOf( selectMe );
				
				if ( itemIndex != -1 && itemIndex < currentListTable.getItems().size() ) {
					currentListTable.requestFocus();
					currentListTable.getSelectionModel().clearAndSelect( itemIndex );
					currentListTable.getFocusModel().focus( itemIndex );
					currentListTable.scrollTo( itemIndex );
				}
			}
			artSplitPane.setImages( selectMe );
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
		
		
		double x = mainStage.getX() + mainStage.getWidth() / 2 - 220; //It'd be nice to use alert.getWidth() / 2, but it's NAN now. 
		double y = mainStage.getY() + mainStage.getHeight() / 2 - 50;
		
		alert.setX( x );
		alert.setY( y );
		
		alert.setDialogPane( new DialogPane() {
			@Override
			protected Node createDetailsButton () {
				CheckBox optOut = new CheckBox();
				optOut.setPadding( new Insets ( 0, 20, 0, 0 ) );
				optOut.setText( "Do not ask again" );
				optOut.setOnAction( e -> {
					promptBeforeOverwrite.set( !optOut.isSelected() );
				});
				return optOut;
			}
			
			@Override
			protected Node createButtonBar () {
				//This lets me specify my own button order below
				ButtonBar node = (ButtonBar) super.createButtonBar();
				node.setButtonOrder( ButtonBar.BUTTON_ORDER_NONE );
				return node;
			}
			
		});

		if ( warningAlertImageSource != null ) {
			ImageView warningImage = new ImageView ( warningAlertImageSource );
			if ( isDarkTheme() ) {
				warningImage.setEffect( darkThemeButtons );
			}
				
			warningImage.setFitHeight( 50 );
			warningImage.setFitWidth( 50 );
			alert.setGraphic( warningImage );
		}
		
		setAlertWindowIcon ( alert );
		applyCurrentTheme ( alert );

		alert.getDialogPane().getButtonTypes().addAll( ButtonType.YES, ButtonType.NO,  ButtonType.CANCEL );
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
				return promptAndSavePlaylist ( Utils.convertCurrentTrackList( player.getCurrentList().getItems() ) );
			
			} else {
				return false;
			}
		}
		return false;
	}
	
	public boolean promptAndSavePlaylist ( List <Track> tracks ) { 
	//REFACTOR: This should probably be refactored into promptForPlaylistName and <something>.savePlaylist( name, items )
		String defaultName = "";
		if ( player.getCurrentPlaylist() != null ) {
			defaultName = player.getCurrentPlaylist().getName();
		}
		TextInputDialog dialog = new TextInputDialog( defaultName );
		applyCurrentTheme ( dialog );
		setDialogIcon ( dialog );

		dialog.setX( mainStage.getX() + mainStage.getWidth() / 2 - 150 );
		dialog.setY( mainStage.getY() + mainStage.getHeight() / 2 - 100 );
		dialog.setTitle( "Save Playlist" );
		dialog.setHeaderText( null );
		Optional <String> result = dialog.showAndWait();
		if ( result.isPresent() ) {
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
			return true;
		}
		return false;
	}
	
	public void promptAndRenamePlaylist ( Playlist playlist ) {
		TextInputDialog dialog = new TextInputDialog( playlist.getName() );
		dialog.setX( mainStage.getX() + mainStage.getWidth() / 2 - 150 );
		dialog.setY( mainStage.getY() + mainStage.getHeight() / 2 - 100 );
		dialog.setTitle( "Rename Playlist" );
		dialog.setHeaderText( null );
		applyCurrentTheme( dialog );
		setDialogIcon( dialog );
		Optional <String> result = dialog.showAndWait();
		
		if ( result.isPresent() ) {
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
		
		showQueueButton.setGraphic( queueImage );
		updateRepeatButtonImages();
		updateShuffleButtonImages();
		
		float width = 33;
		float height = 26;
		
		toggleRepeatButton.setMinSize( width, height );
		toggleShuffleButton.setMinSize( width, height );
		showQueueButton.setMinSize( width, height );
		
		toggleRepeatButton.setPrefSize( width, height );
		toggleShuffleButton.setPrefSize( width, height );
		showQueueButton.setPrefSize( width, height );
		
		toggleRepeatButton.setTooltip( new Tooltip( "Toggle Repeat Type" ) );
		toggleShuffleButton.setTooltip( new Tooltip( "Toggle Shuffle" ) );
		showQueueButton.setTooltip( new Tooltip( "Show Queue" ) );
		
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
		
		currentListTable.getSelectionModel().getSelectedIndices().addListener ( new ListChangeListener() {
			@Override
			public void onChanged ( Change c ) {
				List<Integer> selected = currentListTable.getSelectionModel().getSelectedIndices();
				
				if ( selected.size() == 0 || selected.size() == 1 ) {
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
					
				} else {
					int lengthS = 0;
					for ( int index : selected ) {
						if ( index >= 0 && index < player.getCurrentList().getItems().size() ) {
							lengthS += player.getCurrentList().getItems().get( index ).getLengthS();
						}
					}
					
					final int lengthArgument = lengthS;
					
					Platform.runLater( () -> {
						currentListLength.setText( Utils.getLengthDisplay( lengthArgument ) );
					});
					
				}
					
				
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
		MenuItem clearQueue = new MenuItem ( "Clear Queue" );
		MenuItem replaceWithQueue = new MenuItem ( "Replace list with queue" );
		MenuItem dumpQueueBefore = new MenuItem ( "Prepend to list" );
		MenuItem dumpQueueAfter = new MenuItem ( "Append to list" );
		queueButtonMenu.getItems().addAll( clearQueue, replaceWithQueue, dumpQueueBefore, dumpQueueAfter );
		showQueueButton.setContextMenu( queueButtonMenu );
		
		clearQueue.setOnAction(  ( ActionEvent e ) -> { player.getQueue().clear(); });
		
		replaceWithQueue.setOnAction( ( ActionEvent e ) -> { 
			player.getCurrentList().clearList();
			player.getCurrentList().appendTracks ( player.getQueue().getData() );
			player.getQueue().clear(); 
		});
		
		dumpQueueAfter.setOnAction( ( ActionEvent e ) -> { 
			player.getCurrentList().appendTracks ( player.getQueue().getData() );
			player.getQueue().clear(); 
		});
		
		dumpQueueBefore.setOnAction( ( ActionEvent e ) -> { 
			player.getCurrentList().insertTracks( 0, player.getQueue().getData() );
			player.getQueue().clear(); 
		});
		
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
		
		MenuButton currentListMenu = new MenuButton ( "" );
		currentListMenu.setTooltip ( new Tooltip ( "Current List Controls" ) );
		currentListMenu.setGraphic ( menuImage );
		MenuItem currentListClear = new MenuItem ( "Clear" );
		currentListSave = new MenuItem ( "Save" );
		currentListExport = new MenuItem ( "Export" );
		currentListLoad = new MenuItem ( "Load Files" );
		historyMenuItem = new MenuItem ( "History" );
		MenuItem currentListShuffle = new MenuItem ( "Shuffle" );
		MenuItem jumpMenuItem = new MenuItem ( "Jump to Track" );
		
		currentListClear.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				player.getCurrentList().clearList();
			}
		});

		historyMenuItem.setOnAction ( new EventHandler <ActionEvent>() {
			public void handle ( ActionEvent e ) {
				historyWindow.show();
			}
		});
		
		currentListSave.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				promptAndSavePlaylist( new ArrayList <Track>( player.getCurrentList().getItems() ) );
			}
		});
		
		currentListShuffle.setOnAction( new EventHandler <ActionEvent>() {
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
		
		currentListLoad.setOnAction( new EventHandler <ActionEvent>() {
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
		
		currentListExport.setOnAction( ( ActionEvent e ) -> {
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
		
		jumpMenuItem.setOnAction( ( ActionEvent e ) -> {
			jumpWindow.show();
		});
		
		currentListMenu.getItems().addAll ( currentListClear, currentListShuffle, jumpMenuItem, 
			currentListExport, currentListSave, currentListLoad, historyMenuItem );
		
		playlistControls.getChildren().addAll( toggleShuffleButton, toggleRepeatButton, showQueueButton,
				currentPlayingListInfo, currentListLength, currentListMenu );
	}

	public void setupPlaylistFilterPane () {
		playlistFilterPane = new HBox();
		playlistFilterBox = new TextField();
		playlistFilterBox.setPrefWidth( 500000 );
		
		playlistFilterBox.textProperty().addListener( ( observable, oldValue, newValue ) -> {
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
		
		playlistFilterBox.setOnKeyPressed( ( KeyEvent event ) -> {
			if ( event.getCode() == KeyCode.ESCAPE ) {
				playlistFilterBox.clear();
			}
		});
		
		double width = 33;
		double height = 26;
		
		playlistFilterBox.setPrefHeight( height );
		
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
				playlistFilterBox.setText( "" );
			}
		});
		

		libraryButton.setTooltip( new Tooltip( "Add or Remove Music Folders" ) );
		playlistFilterBox.setTooltip ( new Tooltip ( "Filter/Search playlists" ) );
		clearButton.setTooltip( new Tooltip( "Clear the filter text" ) );

		playlistFilterPane.getChildren().addAll( libraryButton, playlistFilterBox, clearButton );
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
		albumFilterBox = new TextField();
		albumFilterBox.setPrefWidth( 500000 );
		albumFilterBox.textProperty().addListener( ( observable, oldValue, newValue ) -> {
			Platform.runLater( () -> {
				library.getAlbumsFiltered().setPredicate( album -> {
					if ( newValue == null || newValue.isEmpty() ) {
						return true;
					}
	
					String[] lowerCaseFilterTokens = newValue.toLowerCase().split( "\\s+" );
	
					ArrayList <String> matchableText = new ArrayList <String>();
	
					matchableText.add( Normalizer.normalize( album.getAlbumArtist(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
					matchableText.add( album.getAlbumArtist().toLowerCase() );
					matchableText.add( Normalizer.normalize( album.getFullAlbumTitle(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
					matchableText.add( album.getFullAlbumTitle().toLowerCase() );
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
		
		albumFilterBox.setOnKeyPressed( ( KeyEvent event ) -> {
			if ( event.getCode() == KeyCode.ESCAPE ) {
				albumFilterBox.clear();
			}
		});
		
		
		float width = 33;
		float height = 26;

		albumFilterBox.setPrefHeight( height );
		
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
				albumFilterBox.setText( "" );
			}
		});
		

		libraryButton.setTooltip( new Tooltip( "Add or Remove Music Folders" ) );
		albumFilterBox.setTooltip ( new Tooltip ( "Filter/Search albums" ) );
		clearButton.setTooltip( new Tooltip( "Clear the filter text" ) );

		albumFilterPane.getChildren().addAll( libraryButton, albumFilterBox, clearButton );
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
		
		trackListCheckBox.setTooltip( new Tooltip( "Only show tracks not in albums" ) );
	}

	public void setupAlbumTable () {
		albumArtistColumn = new TableColumn( "Artist" );
		albumYearColumn = new TableColumn( "Year" );
		albumAlbumColumn = new TableColumn( "Album" );

		albumArtistColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		albumAlbumColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );

		albumArtistColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "albumArtist" ) );
		albumYearColumn.setCellValueFactory( new PropertyValueFactory <Album, Integer>( "year" ) );
		
		albumAlbumColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "FullAlbumTitle" ) );
		albumAlbumColumn.setCellFactory( e -> new FormattedAlbumCell() );
		
		albumColumnSelectorMenu = new ContextMenu ();
		CheckMenuItem artistMenuItem = new CheckMenuItem ( "Show Artist Column" );
		CheckMenuItem yearMenuItem = new CheckMenuItem ( "Show Year Column" );
		CheckMenuItem albumMenuItem = new CheckMenuItem ( "Show Album Column" );
		artistMenuItem.setSelected( true );
		yearMenuItem.setSelected( true );
		albumMenuItem.setSelected( true );
		albumColumnSelectorMenu.getItems().addAll( artistMenuItem, yearMenuItem, albumMenuItem );
		albumArtistColumn.setContextMenu( albumColumnSelectorMenu );
		albumYearColumn.setContextMenu( albumColumnSelectorMenu );
		albumAlbumColumn.setContextMenu( albumColumnSelectorMenu );
		artistMenuItem.selectedProperty().bindBidirectional( albumArtistColumn.visibleProperty() );
		yearMenuItem.selectedProperty().bindBidirectional( albumYearColumn.visibleProperty() );
		albumMenuItem.selectedProperty().bindBidirectional( albumAlbumColumn.visibleProperty() );

		albumTable = new TableView();
		albumTable.getColumns().addAll( albumArtistColumn, albumYearColumn, albumAlbumColumn );
		albumTable.setEditable( false );
		albumTable.setItems( library.getAlbumsSorted() );
		albumTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		library.getAlbumsSorted().comparatorProperty().bind( albumTable.comparatorProperty() );
		
		albumTable.getSortOrder().add( albumArtistColumn );
		albumTable.getSortOrder().add( albumYearColumn );
		albumTable.getSortOrder().add( albumAlbumColumn );
		
		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		albumTable.setColumnResizePolicy( resizePolicy );
		albumArtistColumn.setPrefWidth( 100 );
		albumYearColumn.setPrefWidth( 60 );
		albumAlbumColumn.setPrefWidth( 100 );
		resizePolicy.registerFixedWidthColumns( albumYearColumn );
		
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
		MenuItem playNextMenuItem = new MenuItem( "Play Next" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		MenuItem infoMenuItem = new MenuItem( "Track List" );
		
		albumTable.setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ESCAPE 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				albumTable.getSelectionModel().clearSelection();
				
			} else if ( e.getCode() == KeyCode.Q 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				enqueueMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.Q && e.isShiftDown() 
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				playNextMenuItem.fire();
				
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
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				appendMenuItem.fire();
				
			}
		});
		
		contextMenu.getItems().addAll( 
			playMenuItem, appendMenuItem, playNextMenuItem, enqueueMenuItem, editTagMenuItem, infoMenuItem, 
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
				player.getCurrentList().setAndPlayAlbums( albumTable.getSelectionModel().getSelectedItems() );
			}
		});

		appendMenuItem.setOnAction( event -> {
			player.getCurrentList().appendAlbums( albumTable.getSelectionModel().getSelectedItems() );
		});

		playNextMenuItem.setOnAction( event -> {
			player.getQueue().queueAllAlbums( albumTable.getSelectionModel().getSelectedItems(), 0 );
		});
		
		enqueueMenuItem.setOnAction( event -> {
			player.getQueue().queueAllAlbums( albumTable.getSelectionModel().getSelectedItems() );
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
			openFileBrowser ( albumTable.getSelectionModel().getSelectedItem().getPath() );
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
		    	artSplitPane.setImages ( newSelection );
		    	albumInfoWindow.setAlbum( newSelection );
		    	
		    } else if ( player.getCurrentTrack() != null ) {
		    	artSplitPane.setImages ( player.getCurrentTrack() );
		    	
		    } else {
		    	//Do nothing, leave the old artwork there. We can set to null if we like that better, I don't think so though
		    }
		});

		albumTable.setRowFactory( tv -> {
			TableRow <Album> row = new TableRow <>();
			
			row.setContextMenu( contextMenu );

			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					if ( okToReplaceCurrentList() ) {
						player.getCurrentList().setAndPlayAlbum( row.getItem() );
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

	public void openFileBrowser ( Path path ) {

		// PENDING: This is the better way once openjdk and openjfx supports it:
		// getHostServices().showDocument(file.toURI().toString());
		
		if ( !Files.isDirectory( path ) ) path = path.getParent();
		final File showMe = path.toFile();
		
		SwingUtilities.invokeLater( new Runnable() {
			public void run () {
				try {
					Desktop.getDesktop().open( showMe );
				} catch ( IOException e ) {
					//TODO: Notify user
					LOGGER.log( Level.INFO, "Unable to open native file browser.", e );
				}
			}
		});
	}

	public void setupTrackTable () {
		trackArtistColumn = new TableColumn( "Artist" );
		trackLengthColumn = new TableColumn( "Length" );
		trackNumberColumn = new TableColumn( "#" );
		trackAlbumColumn = new TableColumn( "Album" );
		trackTitleColumn = new TableColumn( "Title" );
		
		trackArtistColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		trackTitleColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		trackLengthColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );

		trackArtistColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Artist" ) );
		trackTitleColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Title" ) );
		trackLengthColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "LengthDisplay" ) );
		trackNumberColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "TrackNumber" ) );
		trackAlbumColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "albumTitle" ) );
		
		trackArtistColumn.setSortType( TableColumn.SortType.ASCENDING );

		trackNumberColumn.setCellFactory( column -> {
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
		
		trackColumnSelectorMenu = new ContextMenu ();
		CheckMenuItem artistMenuItem = new CheckMenuItem ( "Show Artist Column" );
		CheckMenuItem albumMenuItem = new CheckMenuItem ( "Show Album Column" );
		CheckMenuItem numberMenuItem = new CheckMenuItem ( "Show Track # Column" );
		CheckMenuItem titleMenuItem = new CheckMenuItem ( "Show Title Column" );
		CheckMenuItem lengthMenuItem = new CheckMenuItem ( "Show Length Column" );
		artistMenuItem.setSelected( true );
		albumMenuItem.setSelected( true );
		numberMenuItem.setSelected( true );
		titleMenuItem.setSelected( true );
		lengthMenuItem.setSelected( true );
		trackColumnSelectorMenu.getItems().addAll( artistMenuItem, albumMenuItem, numberMenuItem, titleMenuItem, lengthMenuItem );
		trackArtistColumn.setContextMenu( trackColumnSelectorMenu );
		trackAlbumColumn.setContextMenu( trackColumnSelectorMenu );
		trackTitleColumn.setContextMenu( trackColumnSelectorMenu );
		trackNumberColumn.setContextMenu( trackColumnSelectorMenu );
		trackLengthColumn.setContextMenu( trackColumnSelectorMenu );
		artistMenuItem.selectedProperty().bindBidirectional( trackArtistColumn.visibleProperty() );
		albumMenuItem.selectedProperty().bindBidirectional( trackAlbumColumn.visibleProperty() );
		numberMenuItem.selectedProperty().bindBidirectional( trackNumberColumn.visibleProperty() );
		titleMenuItem.selectedProperty().bindBidirectional( trackTitleColumn.visibleProperty() );
		lengthMenuItem.selectedProperty().bindBidirectional( trackLengthColumn.visibleProperty() );
		
		trackTable = new TableView();
		trackTable.getColumns().addAll( trackArtistColumn, trackAlbumColumn, trackNumberColumn, trackTitleColumn, trackLengthColumn );
		trackTable.setEditable( false );
		trackTable.setItems( library.getTracksSorted() );

		library.getTracksSorted().comparatorProperty().bind( trackTable.comparatorProperty() );
		
		trackTable.getSelectionModel().clearSelection();
		trackTable.getSortOrder().add( trackArtistColumn );
		trackTable.getSortOrder().add( trackAlbumColumn );
		trackTable.getSortOrder().add( trackNumberColumn );
		
		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		trackTable.setColumnResizePolicy( resizePolicy );
		trackArtistColumn.setPrefWidth( 100 );
		trackNumberColumn.setPrefWidth( 40 );
		trackAlbumColumn.setPrefWidth( 100 );
		trackTitleColumn.setPrefWidth( 100 );
		trackLengthColumn.setPrefWidth( 60 );
		resizePolicy.registerFixedWidthColumns( trackNumberColumn, trackLengthColumn );
		
		emptyTrackListLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyTrackListLabel.setWrapText( true );
		emptyTrackListLabel.setTextAlignment( TextAlignment.CENTER );
		trackTable.setPlaceholder( emptyTrackListLabel );
		
		trackTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		ContextMenu trackContextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem playNextMenuItem = new MenuItem( "Play Next" );
		MenuItem appendMenuItem = new MenuItem( "Append" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem infoMenuItem = new MenuItem( "Info" );
		MenuItem lyricsMenuItem = new MenuItem( "Lyrics" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		trackContextMenu.getItems().addAll ( 
			playMenuItem, playNextMenuItem, appendMenuItem, enqueueMenuItem, 
			editTagMenuItem, infoMenuItem, lyricsMenuItem, browseMenuItem, addToPlaylistMenuItem );
		
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
		
		playNextMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.getQueue().queueAllTracks( trackTable.getSelectionModel().getSelectedItems(), 0 );
			}
		});
		
		enqueueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.getQueue().queueAllTracks( trackTable.getSelectionModel().getSelectedItems() );
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
		
		lyricsMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				lyricsWindow.setTrack( trackTable.getSelectionModel().getSelectedItem() );
				lyricsWindow.show();
			}
		});

		browseMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			// PENDING: This is the better way, once openjdk and openjfx supports
			// it: getHostServices().showDocument(file.toURI().toString());
			@Override
			public void handle ( ActionEvent event ) {
				openFileBrowser ( trackTable.getSelectionModel().getSelectedItem().getPath() );
			}
		});
		
		trackTable.setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ESCAPE 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				trackTable.getSelectionModel().clearSelection();
				
			} else if ( e.getCode() == KeyCode.L
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				lyricsMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.Q 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				enqueueMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.Q && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown()  && !e.isMetaDown() ) {
				playNextMenuItem.fire();
							
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
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isControlDown() 
			&& !e.isShiftDown() && !e.isAltDown() && !e.isMetaDown() ) {
				appendMenuItem.fire();
			}
		});
		
		trackTable.getSelectionModel().selectedItemProperty().addListener( ( obs, oldSelection, newSelection ) -> {
		    if (newSelection != null) {
		    	artSplitPane.setImages ( newSelection );
		    	trackInfoWindow.setTrack( newSelection );
		    	
		    } else if ( player.getCurrentTrack() != null ) {
		    	artSplitPane.setImages ( player.getCurrentTrack() );
		    	
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
		playlistNameColumn = new TableColumn( "Playlist" );
		playlistLengthColumn = new TableColumn( "Length" );
		playlistTracksColumn = new TableColumn( "Tracks" );

		playlistLengthColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		
		//TODO: Are these the right types? Integer/String look wrong. 
		playlistNameColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "Name" ) );
		playlistLengthColumn.setCellValueFactory( new PropertyValueFactory <Album, Integer>( "LengthDisplay" ) );
		playlistTracksColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "SongCount" ) );

		playlistNameColumn.setSortType( TableColumn.SortType.ASCENDING );
		
		playlistColumnSelectorMenu = new ContextMenu ();
		CheckMenuItem nameMenuItem = new CheckMenuItem ( "Show Name Column" );
		CheckMenuItem tracksMenuItem = new CheckMenuItem ( "Show Tracks Column" );
		CheckMenuItem lengthMenuItem = new CheckMenuItem ( "Show Length Column" );
		nameMenuItem.setSelected( true );
		tracksMenuItem.setSelected( true );
		lengthMenuItem.setSelected( true );
		playlistColumnSelectorMenu.getItems().addAll( nameMenuItem, tracksMenuItem, lengthMenuItem );
		playlistNameColumn.setContextMenu( playlistColumnSelectorMenu );
		playlistTracksColumn.setContextMenu( playlistColumnSelectorMenu );
		playlistLengthColumn.setContextMenu( playlistColumnSelectorMenu );
		nameMenuItem.selectedProperty().bindBidirectional( playlistNameColumn.visibleProperty() );
		tracksMenuItem.selectedProperty().bindBidirectional( playlistTracksColumn.visibleProperty() );
		lengthMenuItem.selectedProperty().bindBidirectional( playlistLengthColumn.visibleProperty() );

		playlistTable = new TableView<Playlist>();
		playlistTable.getColumns().addAll( playlistNameColumn, playlistTracksColumn, playlistLengthColumn );
		playlistTable.setEditable( false );
		playlistTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		playlistTable.setItems( library.getPlaylistSorted() );

		library.getPlaylistSorted().comparatorProperty().bind( playlistTable.comparatorProperty() );

		playlistTable.getSortOrder().add( playlistNameColumn );
		
		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		playlistTable.setColumnResizePolicy( resizePolicy );
		playlistNameColumn.setPrefWidth( 100 );
		playlistTracksColumn.setPrefWidth( 90 );
		playlistLengthColumn.setPrefWidth( 90 );
		resizePolicy.registerFixedWidthColumns( playlistTracksColumn, playlistLengthColumn );
		
		emptyPlaylistLabel.setWrapText( true );
		emptyPlaylistLabel.setTextAlignment( TextAlignment.CENTER );
		emptyPlaylistLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		playlistTable.setPlaceholder( emptyPlaylistLabel );

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem appendMenuItem = new MenuItem( "Append" );		
		MenuItem playNextMenuItem = new MenuItem( "Play Next" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem renameMenuItem = new MenuItem( "Rename" );
		MenuItem infoMenuItem = new MenuItem( "Track List" );
		MenuItem exportMenuItem = new MenuItem( "Export" );
		MenuItem removeMenuItem = new MenuItem( "Remove" );
		contextMenu.getItems().addAll( playMenuItem, appendMenuItem, playNextMenuItem, enqueueMenuItem, 
				renameMenuItem, infoMenuItem, exportMenuItem, removeMenuItem );

		playMenuItem.setOnAction( ( ActionEvent event ) -> {
			if ( okToReplaceCurrentList() ) {
				player.getCurrentList().setAndPlayPlaylists( playlistTable.getSelectionModel().getSelectedItems() );
			}
		});

		appendMenuItem.setOnAction( ( ActionEvent event ) -> {
			player.getCurrentList().appendPlaylists( playlistTable.getSelectionModel().getSelectedItems() );
		});
		
		playNextMenuItem.setOnAction( ( ActionEvent event ) -> {
			player.getQueue().queueAllPlaylists( playlistTable.getSelectionModel().getSelectedItems(), 0 );
		});
		
		enqueueMenuItem.setOnAction( ( ActionEvent event ) -> {
			player.getQueue().queueAllPlaylists( playlistTable.getSelectionModel().getSelectedItems() );
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
			applyCurrentTheme( alert );
			setAlertWindowIcon( alert );
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
				
			} else if ( e.getCode() == KeyCode.Q && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				playNextMenuItem.fire();

			}  else if ( e.getCode() == KeyCode.ENTER
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && !e.isShiftDown() ) {
				playMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isControlDown()
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
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
						player.getCurrentList().setAndPlayPlaylist( row.getItem() );
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
		clPlayingColumn = new TableColumn( "" );
		clArtistColumn = new TableColumn( "Artist" );
		clYearColumn = new TableColumn( "Year" );
		clAlbumColumn = new TableColumn( "Album" );
		clTitleColumn = new TableColumn( "Title" );
		clNumberColumn = new TableColumn( "#" );
		clLengthColumn = new TableColumn( "Length" );
		
		clAlbumColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		clArtistColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		clTitleColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		
		clPlayingColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, CurrentListTrackState>( "displayState" ) );
		clArtistColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "artist" ) );
		clYearColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, Integer>( "year" ) );
		clAlbumColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "fullAlbumTitle" ) );
		clTitleColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "title" ) );
		clNumberColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, Integer>( "trackNumber" ) );
		clLengthColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "lengthDisplay" ) );
		
		clAlbumColumn.setCellFactory( e -> new FormattedAlbumCell() );

		clPlayingColumn.setCellFactory ( column -> { 
				return new CurrentListTrackStateCell( this, transport.playImageSource, transport.pauseImageSource ); 
			} 
		);
		
		clNumberColumn.setCellFactory( column -> {
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
		
		currentListColumnSelectorMenu = new ContextMenu ();
		CheckMenuItem playingMenuItem = new CheckMenuItem ( "Show Playing Column" );
		CheckMenuItem artistMenuItem = new CheckMenuItem ( "Show Artist Column" );
		CheckMenuItem yearMenuItem = new CheckMenuItem ( "Show Year Column" );
		CheckMenuItem albumMenuItem = new CheckMenuItem ( "Show Album Column" );
		CheckMenuItem numberMenuItem = new CheckMenuItem ( "Show Track # Column" );
		CheckMenuItem titleMenuItem = new CheckMenuItem ( "Show Title Column" );
		CheckMenuItem lengthMenuItem = new CheckMenuItem ( "Show Length Column" );
		playingMenuItem.setSelected( true );
		artistMenuItem.setSelected( true );
		yearMenuItem.setSelected( true );
		albumMenuItem.setSelected( true );
		numberMenuItem.setSelected( true );
		titleMenuItem.setSelected( true );
		lengthMenuItem.setSelected( true );
		currentListColumnSelectorMenu.getItems().addAll( playingMenuItem, numberMenuItem,artistMenuItem, 
			yearMenuItem, albumMenuItem,  titleMenuItem, lengthMenuItem );
		clPlayingColumn.setContextMenu( currentListColumnSelectorMenu );
		clArtistColumn.setContextMenu( currentListColumnSelectorMenu );
		clYearColumn.setContextMenu( currentListColumnSelectorMenu );
		clAlbumColumn.setContextMenu( currentListColumnSelectorMenu );
		clTitleColumn.setContextMenu( currentListColumnSelectorMenu );
		clNumberColumn.setContextMenu( currentListColumnSelectorMenu );
		clLengthColumn.setContextMenu( currentListColumnSelectorMenu );
		playingMenuItem.selectedProperty().bindBidirectional( clPlayingColumn.visibleProperty() );
		artistMenuItem.selectedProperty().bindBidirectional( clArtistColumn.visibleProperty() );
		yearMenuItem.selectedProperty().bindBidirectional( clYearColumn.visibleProperty() );
		albumMenuItem.selectedProperty().bindBidirectional( clAlbumColumn.visibleProperty() );
		numberMenuItem.selectedProperty().bindBidirectional( clNumberColumn.visibleProperty() );
		titleMenuItem.selectedProperty().bindBidirectional( clTitleColumn.visibleProperty() );
		lengthMenuItem.selectedProperty().bindBidirectional( clLengthColumn.visibleProperty() );
		
		currentListTable = new TableView();
		currentListTable.getColumns().addAll( clPlayingColumn, clNumberColumn, clArtistColumn, clYearColumn, clAlbumColumn, clTitleColumn, clLengthColumn );
		currentListTable.getSortOrder().add( clNumberColumn ); 
		currentListTable.setEditable( false );
		currentListTable.setItems( player.getCurrentList().getItems() );
		
		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		currentListTable.setColumnResizePolicy( resizePolicy );
		
		currentListTable.setPlaceholder( new Label( "No tracks in playlist." ) );
		currentListTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		
		clPlayingColumn.setMaxWidth( 38 );
		clPlayingColumn.setMinWidth( 38 );
		clPlayingColumn.setResizable( false );

		clArtistColumn.setPrefWidth( 100 );
		clNumberColumn.setPrefWidth( 40 );
		clYearColumn.setPrefWidth( 60 );
		clAlbumColumn.setPrefWidth( 100 );
		clTitleColumn.setPrefWidth( 100 );
		clLengthColumn.setPrefWidth( 70 );
		
		resizePolicy.registerFixedWidthColumns( clYearColumn, clNumberColumn, clLengthColumn );
		
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
				ArrayList <Path> tracksToAdd = new ArrayList<Path> ();
				
				for ( File file : db.getFiles() ) {
					Path droppedPath = Paths.get( file.getAbsolutePath() );
					if ( Utils.isMusicFile( droppedPath ) ) {
						tracksToAdd.add ( droppedPath );
						
					} else if ( Files.isDirectory( droppedPath ) ) {
						tracksToAdd.addAll ( Utils.getAllTracksInDirectory( droppedPath ) );
						
					} else if ( Utils.isPlaylistFile ( droppedPath ) ) {
						List<Path> paths = Playlist.getTrackPaths( droppedPath );
						tracksToAdd.addAll( paths );
					}
				}
				
				if ( !tracksToAdd.isEmpty() ) {
					player.getCurrentList().insertTrackPathList ( 0, tracksToAdd );
				}

				event.setDropCompleted( true );
				event.consume();
			} 

		});

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem playNextMenuItem = new MenuItem( "Play Next" );
		MenuItem queueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem infoMenuItem = new MenuItem( "Info" );
		MenuItem lyricsMenuItem = new MenuItem( "Lyrics" );
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
				
			} else if ( e.getCode() == KeyCode.Q && e.isShiftDown()
			&& !e.isControlDown() && !e.isAltDown() && !e.isMetaDown() ) {
				playNextMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.J
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				jumpWindow.show();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.L
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				lyricsMenuItem.fire();
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
				
			} else if ( e.getCode() == KeyCode.ENTER
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				playMenuItem.fire();
				e.consume();
				
			}
		});
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );
		contextMenu.getItems().addAll( 
			playMenuItem, playNextMenuItem, queueMenuItem, editTagMenuItem, infoMenuItem, lyricsMenuItem,
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

		
		playNextMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.getQueue().queueAllTracks( currentListTable.getSelectionModel().getSelectedItems(), 0 );
			}
		});
		
		queueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.getQueue().queueAllTracks( currentListTable.getSelectionModel().getSelectedItems() );
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
		
		lyricsMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				lyricsWindow.setTrack( currentListTable.getSelectionModel().getSelectedItem() );
				lyricsWindow.show();
			}
		});

		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.playTrack( currentListTable.getSelectionModel().getSelectedItem() );
			}
		} );

		browseMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				openFileBrowser ( currentListTable.getSelectionModel().getSelectedItem().getPath() );
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
			artSplitPane.setImages ( newSelection );
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
				if ( newValue != null && row != null ) {
			        if ( newValue.isMissingFile() ) {
			            row.getStyleClass().add( "file-missing" );
			        } else {
			            row.getStyleClass().remove( "file-missing" );
			        }
				}
		    });
			
			row.itemProperty().addListener( ( obs, oldValue, newTrackValue ) -> {
				if ( newTrackValue != null  && row != null ) {
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
						int dropIndex = row.isEmpty() ? dropIndex = currentListTable.getItems().size() : row.getIndex();
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
		runThreadSafe ( () -> trackListCheckBox.setSelected( newValue ) );
	}
	
	public double getPrimarySplitPercent() {
		return primarySplitPane.getDividerPositions()[0];
	}
	
	//TODO: probably rename this
	public void runThreadSafe( Runnable runMe ) {
		if ( Platform.isFxApplicationThread() ) {
			runMe.run();
		} else {
			Platform.runLater( runMe );
		}
	}
	
	public double getCurrentListSplitPercent() {
		return currentListSplitPane.getDividerPositions()[0];
	}
	
	public double getArtSplitPercent() {
		return artSplitPane.getDividerPositions()[0];
	}
	
	public boolean isMaximized () {
		return mainStage.isMaximized();
	}
	
	public void setMaximized( boolean value ) {
		mainStage.setMaximized( value );
	}
	
	public static void notifyUserHypnosRunning() {
		Alert alert = new Alert ( AlertType.INFORMATION );
		setAlertWindowIcon ( alert );
		
		alert.setTitle( "Information" );
		alert.setHeaderText( "Unable to launch Hypnos" );
		alert.setContentText( "Hypnos is already running, and only one instance can run at a time. If you don't see it, please try terminating the orphaned process and try again." );
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		int width = gd.getDisplayMode().getWidth();
		int height = gd.getDisplayMode().getHeight();
		
		//NOTE: I can't get the alert's width and height yet, so i just have to eyeball it. Hopefully this is good. 
		alert.setX ( width / 2 - 320 / 2 );
		alert.setY ( height / 2 - 300 / 2 );
		
		alert.showAndWait();
	}
	
	public static void setAlertWindowIcon ( Alert alert ) {
		Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
		try {
			Image icon = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) );
			stage.getIcons().add( icon );
		} catch ( Exception e ) {
			LOGGER.log ( Level.INFO, "Unable to set icon on alert.", e ); 
		}
	}
	
	public static void setDialogIcon ( TextInputDialog dialog ) {
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		try {
			Image icon = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) );
			stage.getIcons().add( icon );
		} catch ( Exception e ) {
			LOGGER.log ( Level.INFO, "Unable to set icon on alert.", e ); 
		}
	}
	
	public void applyCurrentTheme ( Alert alert ) {
		String darkSheet = "file:///" + darkStylesheet.getAbsolutePath().replace( "\\", "/" );
		if ( isDarkTheme() ) {
			((Stage) alert.getDialogPane().getScene().getWindow()).getScene().getStylesheets().add( darkSheet );
		} else {
			((Stage) alert.getDialogPane().getScene().getWindow()).getScene().getStylesheets().remove( darkSheet );
		}
	}
	
	public void applyCurrentTheme ( TextInputDialog dialog ) {
		String darkSheet = "file:///" + darkStylesheet.getAbsolutePath().replace( "\\", "/" );
		if ( isDarkTheme() ) {
			((Stage) dialog.getDialogPane().getScene().getWindow()).getScene().getStylesheets().add( darkSheet );
		
		} else {
			((Stage) dialog.getDialogPane().getScene().getWindow()).getScene().getStylesheets().remove( darkSheet );
		}
	}
		
	public static void notifyUserError ( String message ) { 
		//TODO: Make this not static so we can set the theme on it the right way and because it being static is bad. 
		
		Alert alert = new Alert ( AlertType.ERROR );
		setAlertWindowIcon( alert );
		
		
		alert.setTitle( "Error" );
		alert.setContentText( message );
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		alert.showAndWait();
	}

	public void showMainWindow() {
		
		Rectangle2D screenSize = javafx.stage.Screen.getPrimary().getVisualBounds();

		if ( !mainStage.isMaximized() && mainStage.getWidth() > screenSize.getWidth() ) {
			mainStage.setWidth( screenSize.getWidth() * .8f );
		}
		
		if ( !mainStage.isMaximized() && mainStage.getHeight() > screenSize.getHeight() ) {
			mainStage.setHeight( screenSize.getHeight() * .8f );
		}
		
		mainStage.show();
		
		// This stuff has to be done after show
		transport.doAfterShowProcessing();
		
		Node blankPlaylistHeader = playlistTable.lookup(".column-header-background");
		blankPlaylistHeader.setOnContextMenuRequested ( 
			event -> playlistColumnSelectorMenu.show( blankPlaylistHeader, event.getScreenX(), event.getScreenY() ) );
		
		Node blankTrackHeader = trackTable.lookup(".column-header-background");
		blankTrackHeader.setOnContextMenuRequested ( 
			event -> trackColumnSelectorMenu.show( blankTrackHeader, event.getScreenX(), event.getScreenY() ) );
		
		Node blankAlbumHeader = albumTable.lookup(".column-header-background");
		blankAlbumHeader.setOnContextMenuRequested ( 
			event -> albumColumnSelectorMenu.show( blankAlbumHeader, event.getScreenX(), event.getScreenY() ) );
		
		Node blankCurrentlistHeader = currentListTable.lookup(".column-header-background");
		blankCurrentlistHeader.setOnContextMenuRequested ( 
			event -> currentListColumnSelectorMenu.show( blankCurrentlistHeader, event.getScreenX(), event.getScreenY() ) );

		Set<Node> dividers = primarySplitPane.lookupAll(".split-pane-divider");
		
		for ( Node divider : dividers ) {
			if ( divider.getParent() == currentListSplitPane ) {
				divider.setOnMouseClicked( ( e ) -> {
					if ( e.getClickCount() == 2 ) {
						toggleArtPaneCollapsed();
					}
					
				});
			} else if ( divider.getParent() == primarySplitPane ) {
				divider.setOnMouseClicked( ( e ) -> {
					if ( e.getClickCount() == 2 ) {
						toggleLibraryCollapsed();
					}
				});
			}
		}
			
		SplitPane.setResizableWithParent( artSplitPane, Boolean.FALSE );
		SplitPane.setResizableWithParent( currentListSplitPane, Boolean.FALSE );
		SplitPane.setResizableWithParent( primarySplitPane, Boolean.FALSE );
			
		hackTooltipStartTiming();
	
		updateLibraryListPlaceholder();
	}
	
	public void toggleArtPaneCollapsed() {
		if ( currentListSplitPaneRestoredPosition != null && currentListSplitPane.getDividerPositions()[0] >= .99d  ) {
			currentListSplitPane.setDividerPosition( 0, currentListSplitPaneRestoredPosition );
			currentListSplitPaneRestoredPosition = null;
		} else if ( currentListSplitPane.getDividerPositions()[0] >= .99d ) {
			currentListSplitPane.setDividerPosition( 0, currentListSplitPaneDefault );
		} else {
			currentListSplitPaneRestoredPosition = currentListSplitPane.getDividerPositions()[0];
			currentListSplitPane.setDividerPosition( 0, 1 );
		}
	}
	
	public void toggleLibraryCollapsed() {
		if ( primarySplitPaneRestoredPosition != null && primarySplitPane.getDividerPositions()[0] <= .01d ) {
			primarySplitPane.setDividerPosition( 0, primarySplitPaneRestoredPosition );
			primarySplitPaneRestoredPosition = null;
		} else if ( primarySplitPane.getDividerPositions()[0] <= .01d ) {
			primarySplitPane.setDividerPosition( 0, primarySplitPaneDefault );
		} else {
			primarySplitPaneRestoredPosition = primarySplitPane.getDividerPositions()[0];
			primarySplitPane.setDividerPosition( 0, 0 );
		}
	}
	
	public void fixTables() {
		Platform.runLater( () -> {
			albumTable.refresh();
			playlistTable.refresh();
			trackTable.refresh();
		});
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
		}
		
		retMe.put ( Setting.PRIMARY_SPLIT_PERCENT, getPrimarySplitPercent() );
		retMe.put ( Setting.CURRENT_LIST_SPLIT_PERCENT, getCurrentListSplitPercent() );
		retMe.put ( Setting.ART_SPLIT_PERCENT, getArtSplitPercent() );
		retMe.put ( Setting.LIBRARY_TAB, libraryPane.getSelectionModel().getSelectedIndex() );
		retMe.put ( Setting.PROMPT_BEFORE_OVERWRITE, promptBeforeOverwrite.getValue() );
		retMe.put ( Setting.THEME, theme );
		
		retMe.put ( Setting.AL_TABLE_SHOW_ARTIST_COLUMN, albumArtistColumn.isVisible() );
		retMe.put ( Setting.AL_TABLE_SHOW_YEAR_COLUMN, albumYearColumn.isVisible() );
		retMe.put ( Setting.AL_TABLE_SHOW_ALBUM_COLUMN, albumAlbumColumn.isVisible() );
		retMe.put ( Setting.TR_TABLE_SHOW_ARTIST_COLUMN, trackArtistColumn.isVisible() );
		retMe.put ( Setting.TR_TABLE_SHOW_NUMBER_COLUMN, trackNumberColumn.isVisible() );
		retMe.put ( Setting.TR_TABLE_SHOW_TITLE_COLUMN, trackTitleColumn.isVisible() );
		retMe.put ( Setting.TR_TABLE_SHOW_ALBUM_COLUMN, trackAlbumColumn.isVisible() );
		retMe.put ( Setting.TR_TABLE_SHOW_LENGTH_COLUMN, trackLengthColumn.isVisible() );
		retMe.put ( Setting.PL_TABLE_SHOW_PLAYLIST_COLUMN, playlistNameColumn.isVisible() );
		retMe.put ( Setting.PL_TABLE_SHOW_TRACKS_COLUMN, playlistTracksColumn.isVisible() );
		retMe.put ( Setting.PL_TABLE_SHOW_LENGTH_COLUMN, playlistLengthColumn.isVisible() );
		retMe.put ( Setting.CL_TABLE_SHOW_PLAYING_COLUMN, clPlayingColumn.isVisible() );
		retMe.put ( Setting.CL_TABLE_SHOW_NUMBER_COLUMN, clNumberColumn.isVisible() );
		retMe.put ( Setting.CL_TABLE_SHOW_ARTIST_COLUMN, clArtistColumn.isVisible() );
		retMe.put ( Setting.CL_TABLE_SHOW_YEAR_COLUMN, clYearColumn.isVisible() );
		retMe.put ( Setting.CL_TABLE_SHOW_ALBUM_COLUMN, clAlbumColumn.isVisible() );
		retMe.put ( Setting.CL_TABLE_SHOW_TITLE_COLUMN, clTitleColumn.isVisible() );
		retMe.put ( Setting.CL_TABLE_SHOW_LENGTH_COLUMN, clLengthColumn.isVisible() );

		return retMe;
	}

	public void refreshQueueList() {
		queueWindow.refresh();
	}
	
	public void refreshHistory() {
		historyWindow.refresh();
	}
	
	public void refreshImages() {
		artSplitPane.setImages ( getCurrentImagesTrack() );
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
						artSplitPane.setImages( track );
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
					//TODO: Get rid of function calls. Just do the thing directly. 
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
						switch ( Hypnos.getOS() ) {
							case NIX:
								Platform.runLater ( () -> primarySplitPane.setDividerPosition( 0, Double.valueOf ( value ) ) );
								break;
							default:
								primarySplitPane.setDividerPosition( 0, Double.valueOf ( value ) );
								break;
							
						}
						
					case CURRENT_LIST_SPLIT_PERCENT:
						switch ( Hypnos.getOS() ) {
							case NIX:
								Platform.runLater ( () -> currentListSplitPane.setDividerPosition( 0, Double.valueOf ( value ) ) );
								break;
							default:
								currentListSplitPane.setDividerPosition( 0, Double.valueOf ( value ) );
								break;
							
						}
						break;
						
					case ART_SPLIT_PERCENT:
						switch ( Hypnos.getOS() ) {
							case NIX:
								Platform.runLater ( () -> artSplitPane.setDividerPosition( 0, Double.valueOf ( value ) ) );
								break;
							default:
								artSplitPane.setDividerPosition( 0, Double.valueOf ( value ) );
								break;
							
						}
						break;
						
					case LIBRARY_TAB:
						libraryPane.getSelectionModel().select( Integer.valueOf ( value ) );
						break;
					
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
						//Do nothing
						break;
						
					case AL_TABLE_SHOW_ARTIST_COLUMN:
						albumArtistColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case AL_TABLE_SHOW_YEAR_COLUMN:
						albumYearColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case AL_TABLE_SHOW_ALBUM_COLUMN:
						albumAlbumColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case TR_TABLE_SHOW_ARTIST_COLUMN:
						trackArtistColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case TR_TABLE_SHOW_NUMBER_COLUMN:
						trackNumberColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case TR_TABLE_SHOW_TITLE_COLUMN:
						trackTitleColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case TR_TABLE_SHOW_ALBUM_COLUMN:
						trackAlbumColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case TR_TABLE_SHOW_LENGTH_COLUMN:
						trackLengthColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case PL_TABLE_SHOW_PLAYLIST_COLUMN:
						playlistNameColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case PL_TABLE_SHOW_TRACKS_COLUMN: 
						playlistTracksColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case PL_TABLE_SHOW_LENGTH_COLUMN:
						playlistLengthColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case CL_TABLE_SHOW_PLAYING_COLUMN:
						clPlayingColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case CL_TABLE_SHOW_NUMBER_COLUMN:
						clNumberColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case CL_TABLE_SHOW_ARTIST_COLUMN:
						clArtistColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case CL_TABLE_SHOW_YEAR_COLUMN:
						clYearColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case CL_TABLE_SHOW_ALBUM_COLUMN:
						clAlbumColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case CL_TABLE_SHOW_TITLE_COLUMN:
						clTitleColumn.setVisible( Boolean.valueOf ( value ) );
						break;
					case CL_TABLE_SHOW_LENGTH_COLUMN:
						clLengthColumn.setVisible( Boolean.valueOf ( value ) );
						break;
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


	public Stage getMainStage () {
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
			transport.volumeSlider.setDisable( false );
			transport.volumeMuteButton.setDisable( false );
		});
	}


	@Override
	public void playerStarted ( Track track ) {
		Platform.runLater( () -> {
			transport.togglePlayButton.setGraphic( transport.pauseImage );
			
			currentListTable.refresh();
	
			StackPane thumb = (StackPane) transport.trackPositionSlider.lookup( ".thumb" );
			thumb.setVisible( true );
			
			transport.currentTrackButton.setText( track.getArtist() + " - " + track.getTitle() );
			transport.currentTrackTooltip.setText( 
				"Album: " + track.getAlbumTitle() + "\n" +
				"Year: " + track.getYear() + "\n" +
				"Length: " + Utils.getLengthDisplay( track.getLengthS() ) + "\n" + 
				"Encoding: " + track.getShortEncodingString()
			);
			
			artSplitPane.setImages( track );
			
			boolean disableVolume = !player.volumeChangeSupported();
			
			if ( disableVolume ) {
				transport.volumeSlider.setDisable( true );
				transport.volumeMuteButton.setDisable( true );
				Tooltip.install( transport.volumePane, transport.volumeDisabledTooltip );
			} else {
				transport.volumeSlider.setDisable( false );
				transport.volumeMuteButton.setDisable( false );
				Tooltip.uninstall( transport.volumePane, transport.volumeDisabledTooltip );
			}
		});
	}

	@Override
	public void playerPaused () {
		Platform.runLater( () -> {
			transport.togglePlayButton.setGraphic( transport.playImage );
			currentListTable.refresh(); //To get the play/pause image to update. 
		});
	}

	@Override
	public void playerUnpaused () {
		Platform.runLater( () -> {
			transport.togglePlayButton.setGraphic( transport.pauseImage );
			artSplitPane.setImages( player.getCurrentTrack() );
			currentListTable.refresh();//To get the play/pause image to update. 
		});
		
	}

	@Override
	public void playerVolumeChanged ( double newVolumePercent ) {
		transport.playerVolumeChanged( newVolumePercent );
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
			double x = mainStage.getX() + mainStage.getWidth() / 2 - 220; //It'd be nice to use alert.getWidth() / 2, but it's NAN now. 
			double y = mainStage.getY() + mainStage.getHeight() / 2 - 150;
			
			setAlertWindowIcon( alert );
			applyCurrentTheme( alert );
			
			alert.setX( x );
			alert.setY( y );
			
			alert.setTitle( "Warning" );
			alert.setHeaderText( "Unable to set volume." );
				
			Text text = new Text(
				"Hypnos is unable to set the volume to less than 100% for this audio format.\n\n" +
				"Please set your physical speakers and system sound " +
				"to a reasonable level to avoid damaging your ear drums and audio system " +
				"before unpausing.");
			
			text.setWrappingWidth( 500 );
			text.getStyleClass().add( "alert-text" );
			HBox holder = new HBox();
			holder.getChildren().add( text );
			holder.setPadding( new Insets ( 10, 10, 10, 10 ) );
			alert.getDialogPane().setContent( holder );
			player.setVolumePercent( 1 );

			Tooltip.uninstall( transport.volumePane, transport.volumeDisabledTooltip );
			
			alert.showAndWait();
		});
	}

	public void warnUserPlaylistsNotSaved ( List <Playlist> errors ) {
		if ( doPlaylistSaveWarning ) {
			Platform.runLater( () -> {
				doPlaylistSaveWarning = false;
				Alert alert = new Alert( AlertType.ERROR );
				double x = mainStage.getX() + mainStage.getWidth() / 2 - 220; //It'd be nice to use alert.getWidth() / 2, but it's NAN now. 
				double y = mainStage.getY() + mainStage.getHeight() / 2 - 50;

				setAlertWindowIcon( alert );
				applyCurrentTheme( alert );
				
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
				text.getStyleClass().add( "alert-text" );
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
			double x = mainStage.getX() + mainStage.getWidth() / 2 - 220; //It'd be nice to use alert.getWidth() / 2, but it's NAN now. 
			double y = mainStage.getY() + mainStage.getHeight() / 2 - 50;
			
			alert.setX( x );
			alert.setY( y );

			setAlertWindowIcon( alert );
			applyCurrentTheme( alert );
			
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
			text.getStyleClass().add( "alert-text" );
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

		setAlertWindowIcon ( alert );
		
		applyCurrentTheme ( alert );
						
		Text text = new Text( content );
		
		text.setWrappingWidth( textWidth );
		text.getStyleClass().add( "alert-text" );
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
		this.setAlignment( Pos.BOTTOM_LEFT );
	}
	
	protected void updateImageThemes ( ) {
		if ( ui.isDarkTheme() && !isDarkTheme ) {
			playImage.setEffect( ui.getDarkThemeTransportButtonsAdjust() );
			pauseImage.setEffect( ui.getDarkThemeTransportButtonsAdjust() );
			isDarkTheme = true;
			
		} else if ( !ui.isDarkTheme() && isDarkTheme ) {
			playImage.setEffect( null );
			pauseImage.setEffect( null );	
			isDarkTheme = false;
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

class FixedOrderButtonDialog extends DialogPane {
	@Override
	protected Node createButtonBar () {
		ButtonBar node = (ButtonBar) super.createButtonBar();
		node.setButtonOrder( ButtonBar.BUTTON_ORDER_NONE );
		return node;
	}
}
