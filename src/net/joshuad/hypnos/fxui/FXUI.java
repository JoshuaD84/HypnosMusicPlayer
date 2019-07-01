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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.Duration;
import net.joshuad.hypnos.CurrentList;
import net.joshuad.hypnos.CurrentList.Mode;
import net.joshuad.hypnos.Hypnos.ExitCode;
import net.joshuad.hypnos.CurrentListState;
import net.joshuad.hypnos.CurrentListTrack;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.HypnosURLS;
import net.joshuad.hypnos.Persister;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.Persister.Setting;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.PlayerListener;
import net.joshuad.hypnos.audio.AudioSystem.RepeatMode;
import net.joshuad.hypnos.audio.AudioSystem.ShuffleMode;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys;
import net.joshuad.hypnos.library.Album;
import net.joshuad.hypnos.library.Artist;
import net.joshuad.hypnos.library.Library;
import net.joshuad.hypnos.library.Playlist;
import net.joshuad.hypnos.library.Track;
import net.joshuad.hypnos.library.Library.LoaderSpeed;
import net.joshuad.hypnos.trayicon.TrayIcon;

public class FXUI implements PlayerListener {
	private static final Logger LOGGER = Logger.getLogger( FXUI.class.getName() );

	public static final DataFormat DRAGGED_TRACKS = new DataFormat( "application/hypnos-java-track" );

	public final String PROGRAM_NAME = "Hypnos";

	SplitPane primarySplitPane;
	SplitPane currentListSplitPane;
	private ImagesPanel artSplitPane; 
	private LibraryPane libraryPane; 
	private CurrentListPane currentListPane; 
	
	Image warningAlertImageSource;
	
	Transport transport;

	Scene scene;
	Stage mainStage;
	
	QueueWindow queueWindow;
	TagWindow tagWindow;
	ArtistInfoWindow artistInfoWindow;
	PlaylistInfoWindow playlistInfoWindow;
	AlbumInfoWindow albumInfoWindow;
	MusicRootWindow libraryLocationWindow;
	HistoryWindow historyWindow;
	public SettingsWindow settingsWindow;
	TrackInfoWindow trackInfoWindow;
	LyricsWindow lyricsWindow;
	ExportPlaylistPopup exportPopup;
	TrayIcon trayIcon;
	LibraryLogWindow libraryLogWindow;
	
	final AudioSystem audioSystem;
	final Library library;
	
	private double windowedWidth = 1024;
	private double windowedHeight = 768;
	private double windowedX = 50;
	private double windowedY = 50;
	
	public File darkStylesheet;
	private File baseStylesheet;
	
	private boolean isDarkTheme = false;
	
	private double primarySplitPaneDefault = .35d;
	private double currentListSplitPaneDefault = .75d;
	private double artSplitPaneDefault = .5001d;// For some reason .5 puts it at like .3. 
	
	private Double currentListSplitPaneRestoredPosition = null;
	private Double primarySplitPaneRestoredPosition = null;

	private ColorAdjust darkThemeButtonEffect = new ColorAdjust(); {
		darkThemeButtonEffect.setBrightness( -.3d );
	}
	
	ColorAdjust lightThemeButtonEffect = new ColorAdjust(); {
		lightThemeButtonEffect.setBrightness( -.75d );
		lightThemeButtonEffect.setHue( 0 );
		lightThemeButtonEffect.setSaturation( 0 );
	}
	
	//TODO: these don't really belong in the UI, but we don't have a better place atm. 
	private SimpleBooleanProperty promptBeforeOverwrite = new SimpleBooleanProperty ( true );
	private SimpleBooleanProperty showSystemTray = new SimpleBooleanProperty ( false );
	private SimpleBooleanProperty closeToSystemTray = new SimpleBooleanProperty ( false );
	private SimpleBooleanProperty minimizeToSystemTray = new SimpleBooleanProperty ( false );
	private SimpleBooleanProperty showINotifyPopup = new SimpleBooleanProperty ( true );
	
	private SimpleBooleanProperty showUpdateAvailableInUI = new SimpleBooleanProperty ( true ); 
	private SimpleBooleanProperty updateAvailable = new SimpleBooleanProperty ( false );
	SimpleBooleanProperty showLastFMWidgets = new SimpleBooleanProperty ( false );
	
	boolean doPlaylistSaveWarning = true;
	
	private double currentListSplitPanePosition = -1;
	
	public FXUI ( Stage stage, Library library, AudioSystem audioSystem, GlobalHotkeys hotkeys ) {
		mainStage = stage;
		this.library = library;
		this.audioSystem = audioSystem;
		
		audioSystem.getCurrentList().addNoLoadThread( Thread.currentThread() );
		
		scene = new Scene( new Group(), windowedWidth, windowedHeight );
		
		baseStylesheet = new File ( Hypnos.getRootDirectory() + File.separator + "resources" + File.separator + "style.css" );
		darkStylesheet = new File ( Hypnos.getRootDirectory() + File.separator + "resources" + File.separator + "style-dark.css" );
		
		try {
			mainStage.getIcons().add( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) ) );
		} catch ( FileNotFoundException e ) {
			LOGGER.warning( "Unable to load program icon: resources/icon.png" );
		}

		loadImages();
		
		trayIcon = new TrayIcon ( this, audioSystem );
		
		showSystemTray.addListener( ( ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue ) -> {
			if ( newValue ) {
				trayIcon.show();
			} else {
				trayIcon.hide();
			}
		});

		libraryPane = new LibraryPane( this, audioSystem, library );
		transport = new Transport( this, audioSystem );
		artSplitPane = new ImagesPanel ( this, audioSystem );
		currentListPane = new CurrentListPane( this, audioSystem, library );
		
		libraryLocationWindow = new MusicRootWindow ( this, mainStage, library );
		tagWindow = new TagWindow ( this ); 
		queueWindow = new QueueWindow ( this, library, audioSystem, tagWindow );
		albumInfoWindow = new AlbumInfoWindow ( this, library, audioSystem );
		playlistInfoWindow = new PlaylistInfoWindow ( this, library, audioSystem );
		artistInfoWindow = new ArtistInfoWindow ( this, library, audioSystem );
		historyWindow = new HistoryWindow ( this, library, audioSystem );
		settingsWindow = new SettingsWindow ( this, library, hotkeys, audioSystem );
		trackInfoWindow = new TrackInfoWindow ( this );
		lyricsWindow = new LyricsWindow ( this );
		exportPopup = new ExportPlaylistPopup ( this );
		libraryLogWindow = new LibraryLogWindow ( this, library );
		
		setupNativeStylesheet();
		applyBaseTheme();
		applyDarkTheme();
		
		currentListSplitPane = new SplitPane();
		currentListSplitPane.setOrientation( Orientation.VERTICAL );
		currentListSplitPane.getItems().addAll( currentListPane, artSplitPane );
		artSplitPane.setMinWidth( 0 );
		
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
		SplitPane.setResizableWithParent( currentListPane, Boolean.FALSE );
		
		mainStage.setTitle( ( Hypnos.isDeveloping() ? "Dev - " : "" ) + PROGRAM_NAME );

		((Group) scene.getRoot()).getChildren().addAll( primaryContainer );
		mainStage.setScene( scene );

		ChangeListener<Number> windowSizeListener = new ChangeListener<Number> () {
			@Override
			public void changed ( ObservableValue<? extends Number> observable, Number oldValue, Number newValue ) {
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
				e.consume();
				currentListPane.saveMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.E && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				currentListPane.exportToM3U.fire();

			} else if ( e.getCode() == KeyCode.O && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				currentListPane.loadMenuItem.fire();

			} else if ( e.getCode() == KeyCode.P && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				settingsWindow.show();
			 
			} else if ( e.getCode() == KeyCode.DIGIT1 // With or without control
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				setLibraryCollapsed( false );
				libraryPane.selectPane( 0 );
				Platform.runLater( () -> libraryPane.focusFilterOfCurrentTab() );

			} else if ( e.getCode() == KeyCode.DIGIT2 // With or without control
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				setLibraryCollapsed( false );
				libraryPane.selectPane( 1 );
				Platform.runLater( () -> libraryPane.focusFilterOfCurrentTab() );
	
			} else if ( e.getCode() == KeyCode.DIGIT3 // With or without control
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				setLibraryCollapsed( false );
				libraryPane.selectPane( 2 );
				Platform.runLater( () -> libraryPane.focusFilterOfCurrentTab() );
				
			} else if ( e.getCode() == KeyCode.DIGIT4 // With or without control
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				setLibraryCollapsed( false );
				libraryPane.selectPane( 3 );
				Platform.runLater( () -> libraryPane.focusFilterOfCurrentTab() );
					
			} else if ( e.getCode() == KeyCode.F
			&& !e.isControlDown() && !e.isShiftDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				//We put it in runlater to keep the key from being passed down to the filter box
				Platform.runLater( () -> {
					currentListPane.infoLabelAndFilter.beginEditing();
					currentListPane.currentListTable.getSelectionModel().clearSelection();
				});
			
			
			} else if ( e.getCode() == KeyCode.F && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				
				if ( libraryPane.isFocused() || libraryPane.albumPane.isFocused()
				|| libraryPane.trackPane.isFocused() || libraryPane.playlistPane.isFocused() ) {
					libraryPane.focusFilterOfCurrentTab();
					
				} else {
					//We put it in runlater to keep the key from being passed down to the filter box
					Platform.runLater( () -> {
						currentListPane.infoLabelAndFilter.beginEditing();
						currentListPane.currentListTable.getSelectionModel().clearSelection();
					});
				}
				
			} else if ( e.getCode() == KeyCode.R
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				currentListPane.toggleRepeatButton.fire();

			} else if ( e.getCode() == KeyCode.S
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				currentListPane.toggleShuffleButton.fire();

			} else if ( e.getCode() == KeyCode.H && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				historyWindow.show();

			} else if ( e.getCode() == KeyCode.Q && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				queueWindow.show();
			
			} else if ( e.getCode() == KeyCode.L && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				if ( !libraryPane.trackPane.filterBox.isFocused() && !libraryPane.albumPane.filterBox.isFocused()
				&& !libraryPane.playlistPane.filterBox.isFocused() ) {
					e.consume();
					lyricsWindow.setTrack( audioSystem.getCurrentTrack() );
					lyricsWindow.show();
				}
			
			} else if ( e.getCode() == KeyCode.L && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				toggleLibraryCollapsed();

			} else if ( e.getCode() == KeyCode.SEMICOLON && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				toggleArtPaneCollapsed();

			} else if ( ( e.getCode() == KeyCode.NUMPAD1 || e.getCode() == KeyCode.KP_UP ) 
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				audioSystem.skipMS( -5000 );

			} else if ( ( e.getCode() == KeyCode.NUMPAD2 || e.getCode() == KeyCode.KP_DOWN ) 
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				transport.stopButton.fire();

			} else if ( ( e.getCode() == KeyCode.NUMPAD3 || e.getCode() == KeyCode.KP_RIGHT ) 
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				audioSystem.skipMS( 5000 );

			} else if ( ( e.getCode() == KeyCode.NUMPAD4 || e.getCode() == KeyCode.KP_LEFT )
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				transport.previousButton.fire();

			} else if ( ( e.getCode() == KeyCode.NUMPAD5 || e.getCode() == KeyCode.KP_UP ) 
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				transport.togglePlayButton.fire();

			} else if ( ( e.getCode() == KeyCode.NUMPAD6 || e.getCode() == KeyCode.KP_DOWN ) 
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				transport.nextButton.fire();

			} else if ( ( e.getCode() == KeyCode.NUMPAD7 || e.getCode() == KeyCode.KP_RIGHT ) 
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				audioSystem.decrementVolume();

			} else if ( ( e.getCode() == KeyCode.NUMPAD8 || e.getCode() == KeyCode.KP_LEFT )
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				transport.volumeMuteButton.fire();

			} else if ( ( e.getCode() == KeyCode.NUMPAD9 || e.getCode() == KeyCode.KP_LEFT )
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				audioSystem.incrementVolume();
			} 
		});
		
		Runnable setDefaultDividerPositions = () -> {
			primarySplitPane.setDividerPositions( primarySplitPaneDefault );
			currentListSplitPane.setDividerPositions( currentListSplitPaneDefault );
			artSplitPane.setDividerPositions( artSplitPaneDefault ); 
		};
		
		if ( Hypnos.getOS().isLinux() ) {
			Platform.runLater( setDefaultDividerPositions );
		} else {
			setDefaultDividerPositions.run();
		}
		
		mainStage.widthProperty().addListener( windowSizeListener );
		mainStage.heightProperty().addListener( windowSizeListener );
		
		Platform.setImplicitExit( false );
		
		mainStage.setOnCloseRequest( (WindowEvent t) -> {
			if ( closeToSystemTray.get() && trayIcon.isSupported() ) {
				hideMainWindow();
			} else {
				mainStage.hide();
				Hypnos.exit( ExitCode.NORMAL );
			}
		});
		
		mainStage.iconifiedProperty().addListener( ( obs, oldValue, newValue ) -> {
			if ( newValue  && trayIcon.isSupported() && minimizeToSystemTray.get() ) {
				hideMainWindow();
			}
		});
		
		audioSystem.addPlayerListener ( this );
	}
	
	private void setupNativeStylesheet() {
		Path stylesheet; 
		switch ( Hypnos.getOS() ) {
			case OSX:
				stylesheet = Hypnos.getRootDirectory().resolve( "resources/style-osx.css" );
				
			case WIN_10:
			case WIN_7:
			case WIN_8:
			case WIN_UNKNOWN:
			case WIN_VISTA:
			case WIN_XP:
			case UNKNOWN:
				stylesheet = Hypnos.getRootDirectory().resolve( "resources/style-win.css" );
				break;
				
			case NIX:
			default:
				stylesheet = Hypnos.getRootDirectory().resolve( "resources/style-nix.css" );
				break;
		}
		
		String fontSheet = fileToStylesheetString( stylesheet.toFile() );
		if ( fontSheet == null ) {
			LOGGER.log( Level.WARNING, "Unable to load native stylesheet, hypnos will not look right." + 
				stylesheet.toString()
			);
			return;
		}
		scene.getStylesheets().add( fontSheet );
		libraryLocationWindow.getScene().getStylesheets().add( fontSheet );
		settingsWindow.getScene().getStylesheets().add( fontSheet );
		queueWindow.getScene().getStylesheets().add( fontSheet );
		tagWindow.getScene().getStylesheets().add( fontSheet );
		playlistInfoWindow.getScene().getStylesheets().add( fontSheet );
		artistInfoWindow.getScene().getStylesheets().add( fontSheet );
		albumInfoWindow.getScene().getStylesheets().add( fontSheet );
		libraryLogWindow.getScene().getStylesheets().add( fontSheet );
		historyWindow.getScene().getStylesheets().add( fontSheet );
		trackInfoWindow.getScene().getStylesheets().add( fontSheet );
		lyricsWindow.getScene().getStylesheets().add( fontSheet );
		exportPopup.getScene().getStylesheets().add( fontSheet );
	}
	
	private void loadImages() {
		try {
			warningAlertImageSource = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/alert-warning.png" ).toFile() ) );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load warning alert icon: resources/alert-warning.png", e );
		}
	}		
	
	public String fileToStylesheetString ( File stylesheetFile ) {
		try {
			return stylesheetFile.toURI().toURL().toString();
		} catch ( Exception e ) {
			return null;
		}
	}
	
	public void applyBaseTheme() {
		String baseSheet = fileToStylesheetString( baseStylesheet );
		if ( baseSheet == null ) {
			LOGGER.log( Level.WARNING, "Unable to load base style sheet hypnos will not look right." + 
				baseStylesheet.toString()
			);
			return;
		}
			
		scene.getStylesheets().add( baseSheet ); 
		libraryLocationWindow.getScene().getStylesheets().add( baseSheet );
		settingsWindow.getScene().getStylesheets().add( baseSheet );
		queueWindow.getScene().getStylesheets().add( baseSheet );
		tagWindow.getScene().getStylesheets().add( baseSheet );
		playlistInfoWindow.getScene().getStylesheets().add( baseSheet );
		artistInfoWindow.getScene().getStylesheets().add( baseSheet );
		albumInfoWindow.getScene().getStylesheets().add( baseSheet );
		libraryLogWindow.getScene().getStylesheets().add( baseSheet );
		historyWindow.getScene().getStylesheets().add( baseSheet );
		trackInfoWindow.getScene().getStylesheets().add( baseSheet );
		lyricsWindow.getScene().getStylesheets().add( baseSheet );
		exportPopup.getScene().getStylesheets().add( baseSheet );
	}
	
	public void applyDarkTheme() {
		if ( !isDarkTheme ) {
			String darkSheet = fileToStylesheetString( darkStylesheet );
			if ( darkSheet == null ) {
				LOGGER.log( Level.WARNING, "Unable to load dark style sheet hypnos will not look right." + 
						darkStylesheet.toString()
				);
				return;
			}
			
			isDarkTheme = true;
			scene.getStylesheets().add( darkSheet ); 
			libraryLocationWindow.getScene().getStylesheets().add( darkSheet );
			queueWindow.getScene().getStylesheets().add( darkSheet );
			tagWindow.getScene().getStylesheets().add( darkSheet );
			playlistInfoWindow.getScene().getStylesheets().add( darkSheet );
			artistInfoWindow.getScene().getStylesheets().add( darkSheet );
			albumInfoWindow.getScene().getStylesheets().add( darkSheet );
			libraryLogWindow.getScene().getStylesheets().add( darkSheet );
			historyWindow.getScene().getStylesheets().add( darkSheet );
			settingsWindow.getScene().getStylesheets().add( darkSheet );
			trackInfoWindow.getScene().getStylesheets().add( darkSheet );
			lyricsWindow.getScene().getStylesheets().add( darkSheet );
			exportPopup.getScene().getStylesheets().add( darkSheet );
			
			transport.applyDarkTheme();
			libraryPane.applyDarkTheme( darkThemeButtonEffect );
			currentListPane.applyDarkTheme ( darkThemeButtonEffect );
		}
	}
	
	public void applyLightTheme() {	
		isDarkTheme = false;
		
		String darkSheet = fileToStylesheetString( darkStylesheet );
		if ( darkSheet == null ) {
			LOGGER.log( Level.WARNING, "Unable to load dark style sheet, hypnos will not look right." + 
					darkStylesheet.toString()
			);
			return;
		}
		
		scene.getStylesheets().remove( darkSheet ); 
		libraryLocationWindow.getScene().getStylesheets().remove( darkSheet );
		settingsWindow.getScene().getStylesheets().remove( darkSheet );
		queueWindow.getScene().getStylesheets().remove( darkSheet );
		tagWindow.getScene().getStylesheets().remove( darkSheet );
		playlistInfoWindow.getScene().getStylesheets().remove( darkSheet );
		artistInfoWindow.getScene().getStylesheets().remove( darkSheet );
		albumInfoWindow.getScene().getStylesheets().remove( darkSheet );
		libraryLogWindow.getScene().getStylesheets().remove( darkSheet );
		historyWindow.getScene().getStylesheets().remove( darkSheet );
		trackInfoWindow.getScene().getStylesheets().remove( darkSheet );
		lyricsWindow.getScene().getStylesheets().remove( darkSheet );
		exportPopup.getScene().getStylesheets().remove( darkSheet );
		
		transport.applyLightTheme();
		libraryPane.applyLightTheme();
		currentListPane.applyLightTheme();
	}
	
	public boolean isDarkTheme() {
		return isDarkTheme;
	}
		
	//REFACTOR: Does this function need to exist? 
	void removeFromCurrentList ( List<Integer> removeMe ) {
		if ( !removeMe.isEmpty() ) {
			audioSystem.getCurrentList().removeTracksAtIndices ( removeMe );
		}
	}
	
	public void previousRequested() {
		if ( audioSystem.isStopped() ) {
			currentListPane.currentListTable.getSelectionModel().clearAndSelect( 
					currentListPane.currentListTable.getSelectionModel().getSelectedIndex() - 1 );
		} else {
			audioSystem.previous();
		}
	}
	
	public Track getSelectedTrack () {
		return currentListPane.currentListTable.getSelectionModel().getSelectedItem();
	}
	
	public ObservableList <CurrentListTrack> getSelectedTracks () {
		return currentListPane.currentListTable.getSelectionModel().getSelectedItems();
	}
	
	public void setSelectedTracks ( List <CurrentListTrack> selectMe ) {
		currentListPane.currentListTable.getSelectionModel().clearSelection();
		
		if ( selectMe != null ) {
			for ( CurrentListTrack track : selectMe ) {
				currentListPane.currentListTable.getSelectionModel().select( track );
			}
		}
	}
	
	public void updateTransport ( int timeElapsedS, int timeRemainingS, double percent ) {
		transport.update ( timeElapsedS, timeRemainingS, percent );
	}
	
	public void toggleMinimized() {
		Platform.runLater( () -> {
			if ( !mainStage.isShowing() ) {
				restoreWindow();
			} else if ( mainStage.isIconified() ) {
				mainStage.setIconified( false );
				mainStage.toFront();
			} else {
				mainStage.setIconified( true );
			}
		});
	}
	
	public void restoreWindow() {
		if ( !mainStage.isShowing() ) {
			mainStage.setIconified( false );
			mainStage.show();
			currentListSplitPane.setDividerPosition( 0, currentListSplitPanePosition );
		}
		mainStage.setIconified( false );
		mainStage.toFront();
	}
	
	public void hideMainWindow() {
		switch ( Hypnos.getOS() ) {
		case NIX:
			currentListSplitPanePosition = currentListSplitPane.getDividerPositions()[0];
			mainStage.hide();
			break;
			
		case WIN_10:
		case WIN_7:
		case WIN_8:
		case WIN_UNKNOWN:
		case WIN_VISTA:
		case WIN_XP:
			currentListSplitPanePosition = currentListSplitPane.getDividerPositions()[0];
			mainStage.setIconified( false );
			mainStage.hide();
			break;
			
		default:
			break;
		
		}
	}
	
	public void toggleHidden() {
		Platform.runLater(() -> {
			if ( closeToSystemTray.get() ) {
				if ( mainStage.isIconified() ) {
					mainStage.setIconified( false );
				} else if ( !mainStage.isShowing() ) {
					restoreWindow();
				} else {
					mainStage.setIconified( false ); //This is necessary for at least windows, seems good to keep it for every system
					hideMainWindow();
				} 
			} else {
				toggleMinimized();
			}
		});
	}

	public void updatePlaylistMenuItems ( ObservableList <MenuItem> items, EventHandler <ActionEvent> eventHandler ) {

		items.remove( 1, items.size() );

		for ( Playlist playlist : library.getPlaylistData() ) {
			MenuItem newItem = new MenuItem( playlist.getName() );
			newItem.setUserData( playlist );
			newItem.setOnAction( eventHandler );
			items.add( newItem );
		}
	}

	public void selectCurrentTrack () {
		Track current = audioSystem.getCurrentTrack();
		selectTrackOnCurrentList ( current );
	}
	
	public void selectTrackOnCurrentList ( Track selectMe ) {
		if ( selectMe != null ) {
			synchronized ( currentListPane.currentListTable.getItems() ) {
				int itemIndex = currentListPane.currentListTable.getItems().indexOf( selectMe );
				
				if ( itemIndex != -1 && itemIndex < currentListPane.currentListTable.getItems().size() ) {
					currentListPane.currentListTable.requestFocus();
					currentListPane.currentListTable.getSelectionModel().clearAndSelect( itemIndex );
					currentListPane.currentListTable.getFocusModel().focus( itemIndex );
					currentListPane.currentListTable.scrollTo( itemIndex );
				}
			}
			artSplitPane.trackSelected( selectMe );
		}
	}
	
	//REFACTOR: This function probably belongs in Library
	public void addToPlaylist ( List <Track> tracks, Playlist playlist ) {
		playlist.getTracks().addAll( tracks );
		libraryPane.playlistPane.playlistTable.refresh(); 
		
		//TODO: playlist.equals ( playlist ) instead of name .equals ( name ) ?
		if ( audioSystem.getCurrentPlaylist() != null && audioSystem.getCurrentPlaylist().getName().equals( playlist.getName() ) ) {
			audioSystem.getCurrentList().appendTracks( tracks );
		}
	}
	
	public BooleanProperty promptBeforeOverwriteProperty ( ) {
		return promptBeforeOverwrite;
	}
	
	public BooleanProperty closeToSystemTrayProperty ( ) {
		return closeToSystemTray;
	}
	
	public BooleanProperty minimizeToSystemTrayProperty ( ) {
		return minimizeToSystemTray;
	}
	
	public BooleanProperty showSystemTrayProperty ( ) {
		return showSystemTray;
	}

	public BooleanProperty showUpdateAvailableInUIProperty ( ) {
		return showUpdateAvailableInUI;
	}
	
	public BooleanProperty updateAvailableProperty ( ) {
		return updateAvailable;
	}
	
	public boolean okToReplaceCurrentList () {
		if ( audioSystem.getCurrentList().getState().getMode() != CurrentList.Mode.PLAYLIST_UNSAVED ) {
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
				warningImage.setEffect( darkThemeButtonEffect );
			} else {
				warningImage.setEffect( lightThemeButtonEffect );
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
				return promptAndSavePlaylist ( Utils.convertCurrentTrackList( audioSystem.getCurrentList().getItems() ) );
			
			} else {
				return false;
			}
		}
		return false;
	}
	
	public boolean promptAndSavePlaylist ( List <Track> tracks ) { 
	//REFACTOR: This should probably be refactored into promptForPlaylistName and <something>.savePlaylist( name, items )
		String defaultName = "";
		if ( audioSystem.getCurrentPlaylist() != null ) {
			defaultName = audioSystem.getCurrentPlaylist().getName();
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
			
			Playlist updatedPlaylist = null;
			for ( Playlist test : library.getPlaylistData() ) {
				if ( test.getName().equals( enteredName ) ) {
					test.setTracks( tracks );
					updatedPlaylist = test;
					libraryPane.playlistPane.playlistTable.refresh(); 
					break;
				}
			}
			
			if ( updatedPlaylist == null ) {
				updatedPlaylist = new Playlist( enteredName, new ArrayList <Track> ( tracks ) );
				library.addPlaylist ( updatedPlaylist );
			}	
			
			CurrentListState state = audioSystem.getCurrentList().getState();
			CurrentListState newState;
			if ( state.getMode() == Mode.PLAYLIST || state.getMode() == Mode.PLAYLIST_UNSAVED ) {
				newState = new CurrentListState ( state.getItems(), state.getArtist(), state.getAlbums(), updatedPlaylist, CurrentList.Mode.PLAYLIST );
			} else {
				newState = new CurrentListState ( state.getItems(), state.getArtist(), state.getAlbums(), null, state.getMode() );
			}
			
			audioSystem.getCurrentList().setState( newState );
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
		libraryPane.playlistPane.playlistTable.refresh();
		Hypnos.getPersister().saveLibraryPlaylists();
		Hypnos.getPersister().deletePlaylistFile( oldFileBasename );
	}
	
	public void openFileBrowser ( Path path ) {

		// PENDING: This is the better way once openjdk and openjfx supports it:
		// getHostServices().showDocument(file.toURI().toString());
		
		if ( !Files.isDirectory( path ) ) path = path.getParent();
		final File showMe = path.toFile();
		
		SwingUtilities.invokeLater( new Runnable() {
			public void run () {
				try {
					Desktop.getDesktop().browse( showMe.toURI() );
				} catch ( IOException e ) {
					notifyUserError( "Unable to open native file browser." );
					LOGGER.log( Level.INFO, "Unable to open native file browser.", e );
				}
			}
		});
	}
	
	public void openFileNatively ( Path logFileBackup ) {
		// PENDING: This is the better way once openjdk and openjfx supports it:
		// getHostServices().showDocument(file.toURI().toString());
				
		SwingUtilities.invokeLater( new Runnable() {
			public void run () {
				try {
					Desktop.getDesktop().open( logFileBackup.toFile() );
				} catch ( IOException e ) {
					notifyUserError( "Unable to open native file file viewer for:\n" + logFileBackup );
					LOGGER.log( Level.INFO, "Unable to open native file viewer.", e );
				}
			}
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
	
	public static void notifyUserHypnosNonResponsive() {
		Alert alert = new Alert ( AlertType.INFORMATION );
		setAlertWindowIcon ( alert );
		
		alert.setTitle( "Error" );
		alert.setHeaderText( "Unable to Launch Hypnos" );
		alert.setContentText( "A Hypnos process is running, but it has become non-responsive. " + 
		   "Please end the existing process and then relaunch Hypnos." );
		alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		int width = gd.getDisplayMode().getWidth();
		int height = gd.getDisplayMode().getHeight();
		
		//NOTE: I can't get the alert's width and height yet, so i just have to eyeball it. Hopefully this is good. 
		alert.setX ( width / 2 - 320 / 2 );
		alert.setY ( height / 2 - 300 / 2 );
		
		alert.showAndWait();
	}
	
	public static void notifyUserVLCLibraryError() {
		Alert alert = new Alert ( AlertType.INFORMATION );
		setAlertWindowIcon ( alert );
		
		alert.setTitle( "Information" );
		alert.setHeaderText( "Unable to launch Hypnos" );
		
		String message = "Hypnos's libraries have been removed or corrupted. Please reinstall hypnos.";
			
		alert.setContentText( message );
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
		String darkSheet = fileToStylesheetString( darkStylesheet );
		
		if ( darkSheet == null ) {
			LOGGER.log( Level.INFO, "Unable to load dark style sheet, alert will not look right." + 
					darkStylesheet.toString()
			);
			return;
		}
		
		if ( isDarkTheme() ) {
			((Stage) alert.getDialogPane().getScene().getWindow()).getScene().getStylesheets().add( darkSheet );
		} else {
			((Stage) alert.getDialogPane().getScene().getWindow()).getScene().getStylesheets().remove( darkSheet );
		}
	}
	
	public void applyCurrentTheme ( TextInputDialog dialog ) {
		String darkSheet = fileToStylesheetString( darkStylesheet );
		if ( darkSheet == null ) {
			LOGGER.log( Level.INFO, "Unable to load dark style sheet, input dialog will not look right." + 
					darkStylesheet.toString()
			);
			return;
		}
		if ( isDarkTheme() ) {
			((Stage) dialog.getDialogPane().getScene().getWindow()).getScene().getStylesheets().add( darkSheet );
		
		} else {
			((Stage) dialog.getDialogPane().getScene().getWindow()).getScene().getStylesheets().remove( darkSheet );
		}
	}
		
	public void notifyUserError ( String message ) { 
		Platform.runLater( () -> {
			Alert alert = new Alert ( AlertType.ERROR );
			setAlertWindowIcon( alert );
			applyCurrentTheme( alert );
			alert.setTitle( "Error" );
			alert.setContentText( message );
			alert.getDialogPane().setMinHeight( Region.USE_PREF_SIZE );
			alert.showAndWait();
		});
	}

	public void showMainWindow() {
		
		Rectangle2D screenSize = javafx.stage.Screen.getPrimary().getVisualBounds();

		if ( !mainStage.isMaximized() && mainStage.getWidth() > screenSize.getWidth() ) {
			mainStage.setWidth( screenSize.getWidth() * .8f );
		}
		
		if ( !mainStage.isMaximized() && mainStage.getHeight() > screenSize.getHeight() ) {
			mainStage.setHeight( screenSize.getHeight() * .8f );
		}
		
		mainStage.show(); //PENDING: This is where I get a crash with oracle's java 9, no info and can't catch it. 
		
		// This stuff has to be done after show
		transport.doAfterShowProcessing();
		libraryPane.doAfterShowProcessing();
		
		Node blankCurrentlistHeader = currentListPane.currentListTable.lookup(".column-header-background");
		blankCurrentlistHeader.setOnContextMenuRequested ( 
			event -> currentListPane.currentListColumnSelectorMenu.show( blankCurrentlistHeader, event.getScreenX(), event.getScreenY() ) );

		Set<Node> dividers = primarySplitPane.lookupAll(".split-pane-divider");
		
		for ( Node divider : dividers ) {
			if ( divider.getParent() == currentListSplitPane ) {
				divider.setOnMouseClicked ( ( e ) -> {
					if ( e.getClickCount() == 2 ) {
						toggleArtPaneCollapsed();
					}
					
				});
			} else if ( divider.getParent() == primarySplitPane ) {
				divider.setOnMouseClicked ( ( e ) -> {
					if ( e.getClickCount() == 2 ) {
						toggleLibraryCollapsed();
					}
				});
			} else if ( divider.getParent() == artSplitPane ) {
				divider.setOnMouseClicked ( ( e ) -> {
					if ( e.getClickCount() == 2 ) {
						artSplitPane.setDividerPosition( 0, .5f );
					}
				});
			}
		}
			
		SplitPane.setResizableWithParent( artSplitPane, Boolean.FALSE );
		SplitPane.setResizableWithParent( currentListSplitPane, Boolean.FALSE );
		SplitPane.setResizableWithParent( primarySplitPane, Boolean.FALSE );
			
		hackTooltipStartTiming();
			
		Platform.runLater( () -> {
			//This is a bad hack to give the album /track pane data time to load
			try {
				Thread.sleep ( 100 );
			} catch ( InterruptedException e ) {
				//Do nothing
			}
		});
	}
	
	public void toggleArtPaneCollapsed() {
		setArtPaneCollapsed( !isArtPaneCollapsed() );
	}
	
	public boolean isArtPaneCollapsed() {
		return currentListSplitPane.getDividerPositions()[0] >= .99d;
	}
	
	public void setArtPaneCollapsed( boolean target ) {
		if( target ) {
			currentListSplitPaneRestoredPosition = currentListSplitPane.getDividerPositions()[0];
			currentListSplitPane.setDividerPosition( 0, 1 );
		} else {
			if ( currentListSplitPaneRestoredPosition != null ) {
				currentListSplitPane.setDividerPosition( 0, currentListSplitPaneRestoredPosition );
				currentListSplitPaneRestoredPosition = null;
			} else if ( currentListSplitPane.getDividerPositions()[0] >= .99d ) {
				currentListSplitPane.setDividerPosition( 0, currentListSplitPaneDefault );
			}
		}
	}
	
	public void toggleLibraryCollapsed() {
		setLibraryCollapsed( !isLibraryCollapsed() );
	}
		
		
	public void setLibraryCollapsed( boolean target ) {
		if ( target ) {
			primarySplitPaneRestoredPosition = primarySplitPane.getDividerPositions()[0];
			primarySplitPane.setDividerPosition( 0, 0 );
		} else {
			if ( primarySplitPaneRestoredPosition != null ) {
				primarySplitPane.setDividerPosition( 0, primarySplitPaneRestoredPosition );
				primarySplitPaneRestoredPosition = null;
			} else if ( primarySplitPane.getDividerPositions()[0] <= .01d ) {
				primarySplitPane.setDividerPosition( 0, primarySplitPaneDefault );
			} 
		}
	}
	
	public boolean isLibraryCollapsed() {
		return primarySplitPane.getDividerPositions()[0] <= .01d;
	}
	
	public void fixTables() {
		Platform.runLater( () -> {
			libraryPane.albumPane.albumTable.refresh();
			libraryPane.playlistPane.playlistTable.refresh();
			libraryPane.trackPane.trackTable.refresh();
			libraryPane.artistPane.artistTable.refresh();
		});
	}

	public EnumMap <Persister.Setting, ? extends Object> getSettings () {
		
		EnumMap <Persister.Setting, Object> retMe = new EnumMap <Persister.Setting, Object> ( Persister.Setting.class );
		
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
		retMe.put ( Setting.ART_CURRENT_SPLIT_PERCENT, getCurrentListSplitPercent() );
		retMe.put ( Setting.ART_SPLIT_PERCENT, getArtSplitPercent() );
		retMe.put ( Setting.PROMPT_BEFORE_OVERWRITE, promptBeforeOverwrite.getValue() );
		retMe.put ( Setting.SHOW_SYSTEM_TRAY_ICON, showSystemTray.getValue() );
		retMe.put ( Setting.CLOSE_TO_SYSTEM_TRAY, closeToSystemTray.getValue() );
		retMe.put ( Setting.MINIMIZE_TO_SYSTEM_TRAY, minimizeToSystemTray.getValue() );
		retMe.put ( Setting.SHOW_INOTIFY_ERROR_POPUP, showINotifyPopup.getValue() );
		retMe.put ( Setting.SHOW_UPDATE_AVAILABLE_IN_MAIN_WINDOW, showUpdateAvailableInUI.getValue() );
		retMe.put ( Setting.THEME, theme );
		retMe.put ( Setting.SHOW_LASTFM_IN_UI, showLastFMWidgets.getValue().toString() );
		
		EnumMap <Persister.Setting, ? extends Object> librarySettings = libraryPane.getSettings();
		for ( Persister.Setting setting : librarySettings.keySet() ) {
			retMe.put( setting, librarySettings.get( setting ) );
		}
		
		EnumMap <Persister.Setting, ? extends Object> currentListSettings = currentListPane.getSettings();
		for ( Persister.Setting setting : currentListSettings.keySet() ) {
			retMe.put( setting, currentListSettings.get( setting ) );
		}
		
		return retMe;
	}

	public void refreshQueueList() {
		queueWindow.refresh();
	}
	
	public void refreshHistory() {
		historyWindow.refresh();
	}
	
	public void refreshImages() {
		artSplitPane.refreshImages ();
	}
	
	public void refreshCurrentList () {
		for ( CurrentListTrack track : currentListPane.currentListTable.getItems() ) {
			try {
				track.refreshTagData();
			} catch ( Exception e ) {
				track.setIsMissingFile( true ); //TODO: Do we want to make another flag or rename isMissingFile?
			}
		}
		currentListPane.currentListTable.refresh();
	}
	
	public void refreshAlbumTable () {
		libraryPane.albumPane.albumTable.refresh();
	}

	public void refreshTrackTable () {
		libraryPane.trackPane.trackTable.refresh();
	}

	public void applySettingsAfterWindowShown ( EnumMap<Persister.Setting, String> settings ) {
		libraryPane.applySettingsAfterWindowShown( settings );
	}

	@SuppressWarnings("incomplete-switch")
	public void applySettingsBeforeWindowShown( EnumMap<Persister.Setting, String> settings ) {
		settings.forEach( ( setting, value )-> {
			try {
				switch ( setting ) {
					case WINDOW_X_POSITION:
						double xPosition = Math.max( Double.valueOf( value ) , 0 );
						mainStage.setX( xPosition );
						settings.remove ( setting );
						break;
						
					case WINDOW_Y_POSITION:
						double yPosition = Math.max( Double.valueOf( value ) , 0 );
						mainStage.setY( yPosition );
						settings.remove ( setting );
						break;
						
					case WINDOW_WIDTH:
						mainStage.setWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
						
					case WINDOW_HEIGHT:
						mainStage.setHeight( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
						
					case WINDOW_MAXIMIZED:
						mainStage.setMaximized( Boolean.valueOf( value ) );
						settings.remove ( setting );
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
						settings.remove ( setting );
						break;
						
					case ART_CURRENT_SPLIT_PERCENT:
						switch ( Hypnos.getOS() ) {
							case NIX:
								Platform.runLater ( () -> currentListSplitPane.setDividerPosition( 0, Double.valueOf ( value ) ) );
								break;
							default:
								currentListSplitPane.setDividerPosition( 0, Double.valueOf ( value ) );
								break;
						}
						settings.remove ( setting );
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
						settings.remove ( setting );
						break;
					
					case SHOW_SYSTEM_TRAY_ICON: 
						showSystemTray.setValue( Boolean.valueOf( value ) );
						settings.remove ( setting );
						break;
						
					case CLOSE_TO_SYSTEM_TRAY: 
						closeToSystemTray.setValue( Boolean.valueOf( value ) );
						settings.remove ( setting );
						break;
						
					case MINIMIZE_TO_SYSTEM_TRAY: 
						minimizeToSystemTray.setValue( Boolean.valueOf( value ) );
						settings.remove ( setting );
						break;
						
					case PROMPT_BEFORE_OVERWRITE:
						promptBeforeOverwrite.setValue( Boolean.valueOf( value ) );
						settings.remove ( setting );
						break;
						
					case SHOW_INOTIFY_ERROR_POPUP:
						showINotifyPopup.setValue( Boolean.valueOf( value ) );
						settings.remove ( setting );
						break;
						
					case SHOW_UPDATE_AVAILABLE_IN_MAIN_WINDOW:
						showUpdateAvailableInUI.setValue( Boolean.valueOf( value ) );
						settings.remove ( setting );
						break;
					
					case THEME:
						if ( value.equalsIgnoreCase( "dark" ) ) {
							applyDarkTheme();
						} else {
							applyLightTheme();
						}
						settings.remove ( setting );
						break;
						
					case SHOW_LASTFM_IN_UI:
						this.showLastFMWidgets.set( Boolean.valueOf( value )  );
						settings.remove( setting );
						break;
				}
			} catch ( Exception e ) {
				LOGGER.log( Level.INFO, "Unable to apply setting: " + setting + " to UI.", e );
			}
		});
		
		libraryPane.applySettingsBeforeWindowShown( settings );
		currentListPane.applySettingsBeforeWindowShown( settings );
		settingsWindow.updateSettingsBeforeWindowShown();
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
		transport.playerStopped ( track, reason );
	}


	@Override
	public void playerStarted ( Track track ) {
		Platform.runLater( () -> {
			transport.playerStarted ( track );
			currentListPane.currentListTable.refresh();
		});
	}

	@Override
	public void playerPaused () {
		Platform.runLater( () -> {
			transport.togglePlayButton.setGraphic( transport.playImage );
			currentListPane.currentListTable.refresh(); //To get the play/pause image to update. 
		});
	}

	@Override
	public void playerUnpaused () {
		Platform.runLater( () -> {
			transport.togglePlayButton.setGraphic( transport.pauseImage );
			currentListPane.currentListTable.refresh();//To get the play/pause image to update. 
		});
	}

	@Override
	public void playerVolumeChanged ( double newVolumePercent ) {
		transport.playerVolumeChanged( newVolumePercent );
	}

	@Override
	public void playerShuffleModeChanged ( ShuffleMode newMode ) {
		Platform.runLater( () -> {
			currentListPane.updateShuffleButtonImages();
		});
	}

	@Override
	public void playerRepeatModeChanged ( RepeatMode newMode ) {
		Platform.runLater( () -> {
			currentListPane.updateRepeatButtonImages();
		});
	}
	
	public void libraryCleared() {
		artSplitPane.libraryCleared();
	}

	public void refreshHotkeyList () {
		settingsWindow.refreshHotkeyFields();
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
	
	public File promptUserForFolder() {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle( "Export Playlist As Folder" );
		File targetFile = dirChooser.showDialog( mainStage );
		return targetFile;
	}
	
	public void alertUser ( AlertType type, String title, String header, String content ) {
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
		TextArea textArea = new TextArea();
		textArea.setEditable( false );
		textArea.setWrapText( true );
		textArea.setText( content );
		
		alert.getDialogPane().setContent( textArea );
		
		alert.showAndWait(); 
	}
	
	public Track getCurrentImagesTrack() {
		return artSplitPane.getCurrentImagesTrack();
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

	public void setUpdateAvailable ( boolean updateAvailable ) {
		this.updateAvailable.setValue( updateAvailable );
	}

	public void goToAlbumOfTrack ( Track track ) {
		Album album = track.getAlbum();
		if ( album == null ) {
			LOGGER.info( "Requested to 'go to album' of a track that is not part of an album, ignoring." );
			return;
		}
		
		libraryPane.clearAlbumFilter();
		libraryPane.albumPane.albumTable.getSelectionModel().clearSelection();
		libraryPane.albumPane.albumTable.getSelectionModel().select( album );
		libraryPane.albumPane.albumTable.requestFocus();
		libraryPane.albumPane.albumTable.scrollTo( album );
		libraryPane.setAlbumsVisible( true );
		if ( isLibraryCollapsed() ) setLibraryCollapsed( false );
		libraryPane.showAndSelectAlbumTab();
	}

	public void setLibraryLabelsToLoading () {
		libraryPane.setLabelsToLoading();
	}

	public void trackSelected ( Track newSelection ) {
		artSplitPane.trackSelected( newSelection );
		if ( lyricsWindow.isShowing() ) {
			lyricsWindow.setTrack( newSelection );
		}
		
		if ( trackInfoWindow.isShowing() ) {
			trackInfoWindow.setTrack( newSelection );
		}
	}
	
	public void albumSelected ( Album album ) {
		artSplitPane.albumSelected( album );
		
		if ( albumInfoWindow.isShowing() ) {
			albumInfoWindow.setAlbum( album );
		}
	}
	
	public void artistSelected ( Artist artist ) {
		//TODO: 
	}

	public LibraryPane getLibraryPane () {
		return libraryPane;
	}

	public CurrentListPane getCurrentListPane () {
		return currentListPane;
	}
	
	public void setCurrentListFilterText ( String string ) {
		currentListPane.infoLabelAndFilter.setText ( string );
	}
	
	/* Give it a source for the UI change so only that source can set it to standby
	 * Avoiding overlapping modifications issues w/ threads terminating after other ones start */
	private Object statusSource = null;
	public void setLibraryLoaderStatus ( String message, double percentDone, Object source ) {
		Platform.runLater( () -> {
			this.libraryLocationWindow.setLoaderStatus ( message, percentDone );
			statusSource = source;
		});		
	}

	//The programmer can send a null source in if he wants to force a standby status
	public void setLibraryLoaderStatusToStandby ( Object source ) {
		if ( statusSource == null || source == null || statusSource == source ) {
			Platform.runLater( () -> {
				this.libraryLocationWindow.setLibraryLoaderStatusToStandby ( );
			});	
		}
	}

	public void notifyUserLinuxInotifyIssue () {
		if ( showINotifyPopup.get() ) {
			Platform.runLater( () -> {
				Alert alert = new Alert( AlertType.WARNING );
				
				double x = mainStage.getX() + mainStage.getWidth() / 2 - 220; //It'd be nice to use alert.getWidth() / 2, but it's NAN now. 
				double y = mainStage.getY() + mainStage.getHeight() / 2 - 50;
				
				alert.setX( x );
				alert.setY( y );
				
				alert.setDialogPane( new DialogPane() {
					@Override
					protected Node createDetailsButton () {
						CheckBox optOut = new CheckBox();
						optOut.setPadding( new Insets ( 0, 20, 0, 0 ) );
						optOut.setText( "Do not show popup again" );
						optOut.setOnAction( e -> {
							showINotifyPopup.set( !optOut.isSelected() );
						});
						return optOut;
					}
				});
		
				if ( warningAlertImageSource != null ) {
					ImageView warningImage = new ImageView ( warningAlertImageSource );
					if ( isDarkTheme() ) {
						warningImage.setEffect( darkThemeButtonEffect );
					} else {
						warningImage.setEffect( lightThemeButtonEffect );
					}
						
					warningImage.setFitHeight( 50 );
					warningImage.setFitWidth( 50 );
					alert.setGraphic( warningImage );
				}
				
				setAlertWindowIcon ( alert );
				applyCurrentTheme ( alert );
		
				alert.getDialogPane().getButtonTypes().addAll( ButtonType.OK );
				
				Label message = new Label ( 
					"Hypnos is unable to watch for changes in some library\n" +
					"directories due to system defined Linux inotify count\n" + 
					"limitations.\n\n" + 
					"This error is non-fatal: your full library will be\n" + 
					"rescanned each time Hypnos is started, so any changes\n" + 
					"will be included in Hypnos's library eventually. However\n" + 
					"you will not get real-time updates when the file system\n" + 
					"changes.\n\n" +
					"If you would like real time updates (which are kind of nice)\n" +
					"You can adjust this limit by editing your sysctl.conf file.\n\n"
				);
				
				Hyperlink link = new Hyperlink ( "Click here for detailed instructions." );
				link.setOnAction( e -> { openWebBrowser( HypnosURLS.HELP_INOTIFY ); } );
				
				VBox content = new VBox();
				content.getChildren().addAll ( message, link );
				
				alert.getDialogPane().setContent( content );
				alert.getDialogPane().setExpandableContent( new Group() );
				alert.getDialogPane().setExpanded( false );
				alert.getDialogPane().setGraphic( alert.getDialogPane().getGraphic() );
				alert.setTitle( "Unable to Watch for Changes" );
				alert.setHeaderText( null );
				
				Optional <ButtonType> result = alert.showAndWait();
				
				if ( result.isPresent() ) {
					return;
				}
				return;
			});
		}
	}

	public TrayIcon getTrayIcon () {
		return trayIcon;
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

class FixedOrderButtonDialog extends DialogPane {
	@Override
	protected Node createButtonBar () {
		ButtonBar node = (ButtonBar) super.createButtonBar();
		node.setButtonOrder( ButtonBar.BUTTON_ORDER_NONE );
		return node;
	}
}
