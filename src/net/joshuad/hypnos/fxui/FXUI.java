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
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
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
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import net.joshuad.hypnos.Album;
import net.joshuad.hypnos.CurrentList;
import net.joshuad.hypnos.CurrentList.DefaultSortMode;
import net.joshuad.hypnos.CurrentListState;
import net.joshuad.hypnos.CurrentListTrack;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Library;
import net.joshuad.hypnos.LibraryUpdater.LoaderSpeed;
import net.joshuad.hypnos.Persister;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.Persister.Setting;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.PlayerListener;
import net.joshuad.hypnos.audio.AudioSystem.RepeatMode;
import net.joshuad.hypnos.audio.AudioSystem.ShuffleMode;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys;

@SuppressWarnings({ "rawtypes", "unchecked" }) // REFACTOR: Maybe get rid of this when I understand things better
public class FXUI implements PlayerListener {
	private static final Logger LOGGER = Logger.getLogger( FXUI.class.getName() );

	public static final DataFormat DRAGGED_TRACKS = new DataFormat( "application/hypnos-java-track" );

	public final String PROGRAM_NAME = "Hypnos";

	SplitPane primarySplitPane;
	SplitPane currentListSplitPane;
	public ImagesPanel artSplitPane; //TODO: Probably make this private
	public LibraryTabPane libraryPane; //TODO: make private
	public CurrentListPane currentListPane; //TODO: make private 
	
	Image warningAlertImageSource;
	
	Transport transport;

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
	
	final AudioSystem audioSystem;
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
	private double artSplitPaneDefault = .5001d;// For some reason .5 puts it at like .3. 
	
	private Double currentListSplitPaneRestoredPosition = null;
	private Double primarySplitPaneRestoredPosition = null;

	private ColorAdjust darkThemeButtons = new ColorAdjust(); {
		darkThemeButtons.setSaturation( -1 );
		darkThemeButtons.setHue( 1 );
		darkThemeButtons.setBrightness( .75 );
	}
	
	//TODO: this doesn't really belong in the UI, but we don't have a better place atm. 
	private SimpleBooleanProperty promptBeforeOverwrite = new SimpleBooleanProperty ( true );
	
	private SimpleBooleanProperty showUpdateAvailableInUI = new SimpleBooleanProperty ( true ); 
	private SimpleBooleanProperty updateAvailable = new SimpleBooleanProperty ( false );
	
	boolean doPlaylistSaveWarning = true;
	
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
		
		setupFont();
		loadImages();

		libraryPane = new LibraryTabPane( this, audioSystem, library );
		transport = new Transport( this, audioSystem );
		artSplitPane = new ImagesPanel ( this, audioSystem );
		currentListPane = new CurrentListPane( this, audioSystem, library );
		
		libraryLocationWindow = new LibraryLocationWindow ( mainStage, library );
		tagWindow = new TagWindow ( this ); 
		queueWindow = new QueueWindow ( this, library, audioSystem, tagWindow );
		albumInfoWindow = new AlbumInfoWindow ( this, library, audioSystem );
		playlistInfoWindow = new PlaylistInfoWindow ( this, library, audioSystem );
		historyWindow = new HistoryWindow ( this, library, audioSystem );
		settingsWindow = new SettingsWindow ( this, library, hotkeys, audioSystem );
		trackInfoWindow = new TrackInfoWindow ( this );
		lyricsWindow = new LyricsWindow ( this );

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
				e.consume();
				currentListPane.saveMenuItem.fire();

			} else if ( e.getCode() == KeyCode.W && !e.isControlDown()  //TODO: DELETE THSI
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				currentListPane.currentListTable.getSortOrder().clear();
				
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

			} else if ( e.getCode() == KeyCode.DIGIT1 /* With or without control */
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				libraryPane.getSelectionModel().select( 0 );
				Platform.runLater( () -> libraryPane.focusFilterOfCurrentTab() );

			} else if ( e.getCode() == KeyCode.DIGIT2 /* With or without control */
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				libraryPane.getSelectionModel().select( 1 );
				Platform.runLater( () -> libraryPane.focusFilterOfCurrentTab() );
	
			} else if ( e.getCode() == KeyCode.DIGIT3 /* With or without control */
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				libraryPane.getSelectionModel().select( 2 );
				Platform.runLater( () -> libraryPane.focusFilterOfCurrentTab() );
				
			} else if ( e.getCode() == KeyCode.F /* With or without control */
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				//We put it in runlater to keep the key from being passed down to the filter box
				Platform.runLater( () -> {
					currentListPane.infoLabelAndFilter.beginEditing();
					currentListPane.currentListTable.getSelectionModel().clearSelection();
				});

			} else if ( e.getCode() == KeyCode.R
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				currentListPane.toggleRepeatButton.fire();

			} else if ( e.getCode() == KeyCode.S
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				currentListPane.toggleShuffleButton.fire();

			} else if ( e.getCode() == KeyCode.H
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				historyWindow.show();

			} else if ( e.getCode() == KeyCode.Q && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				queueWindow.show();

			} else if ( e.getCode() == KeyCode.L && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				if ( !libraryPane.trackFilterBox.isFocused() && !libraryPane.albumFilterBox.isFocused()
				&& !libraryPane.playlistFilterBox.isFocused() ) {
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
		
		audioSystem.addPlayerListener ( this );
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
		
		String fontSheet = fileToStylesheetString( stylesheet.toFile() );
		if ( fontSheet == null ) {
			LOGGER.log( Level.WARNING, "Unable to load font style sheet, hypnos will not look right." + 
				stylesheet.toString()
			);
			return;
		}
		
		scene.getStylesheets().add( fontSheet );
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
		albumInfoWindow.getScene().getStylesheets().add( baseSheet );
		libraryLocationWindow.getScene().getStylesheets().add( baseSheet );
		historyWindow.getScene().getStylesheets().add( baseSheet );
		settingsWindow.getScene().getStylesheets().add( baseSheet );
		trackInfoWindow.getScene().getStylesheets().add( baseSheet );
		lyricsWindow.getScene().getStylesheets().add( baseSheet );
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
			
			transport.applyDarkTheme();
			libraryPane.applyDarkTheme( darkThemeButtons );
			currentListPane.applyDarkTheme ( darkThemeButtons );
		}
	}
	
	public void removeDarkTheme() {	
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
		albumInfoWindow.getScene().getStylesheets().remove( darkSheet );
		libraryLocationWindow.getScene().getStylesheets().remove( darkSheet );
		historyWindow.getScene().getStylesheets().remove( darkSheet );
		settingsWindow.getScene().getStylesheets().remove( darkSheet );
		trackInfoWindow.getScene().getStylesheets().remove( darkSheet );
		lyricsWindow.getScene().getStylesheets().remove( darkSheet );
		
		transport.removeDarkTheme();
		libraryPane.removeDarkTheme();
		currentListPane.removeDarkTheme();
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
			artSplitPane.setImages( selectMe );
		}
	}
	
	//REFACTOR: This function probably belongs in Library
	public void addToPlaylist ( List <Track> tracks, Playlist playlist ) {
		playlist.getTracks().addAll( tracks );
		libraryPane.playlistTable.refresh(); 
		
		//TODO: playlist.equals ( playlist ) instead of name .equals ( name ) ?
		if ( audioSystem.getCurrentPlaylist() != null && audioSystem.getCurrentPlaylist().getName().equals( playlist.getName() ) ) {
			audioSystem.getCurrentList().appendTracks( tracks );
		}
	}
	
	public BooleanProperty promptBeforeOverwriteProperty ( ) {
		return promptBeforeOverwrite;
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

			for ( Playlist test : library.getPlaylists() ) {
				if ( test.getName().equals( enteredName ) ) {
					library.removePlaylist ( test );
					break;
				}
			}

			Playlist newPlaylist = new Playlist( enteredName, new ArrayList <Track> ( tracks ) );
			library.addPlaylist ( newPlaylist );
			
			CurrentListState state = audioSystem.getCurrentList().getState();
			
			CurrentListState newState = new CurrentListState ( state.getItems(), state.getAlbums(), newPlaylist, CurrentList.Mode.PLAYLIST );
			
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
		libraryPane.playlistTable.refresh();
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
					//TODO: Notify user
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
					//TODO: Notify user
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
	
		libraryPane.updateLibraryListPlaceholder();
		
		Platform.runLater( () -> {
			try {
				Thread.sleep ( 100 );
			} catch ( InterruptedException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
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
			libraryPane.albumTable.refresh();
			libraryPane.playlistTable.refresh();
			libraryPane.trackTable.refresh();
			libraryPane.fixTabs();
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
		retMe.put ( Setting.SHOW_UPDATE_AVAILABLE_IN_MAIN_WINDOW, showUpdateAvailableInUI.getValue() );
		retMe.put ( Setting.THEME, theme );
		
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
		artSplitPane.setImages ( getCurrentImagesTrack() );
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
		libraryPane.albumTable.refresh();
	}

	public void refreshTrackTable () {
		libraryPane.trackTable.refresh();
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
						mainStage.setX( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
						
					case WINDOW_Y_POSITION:
						mainStage.setY( Double.valueOf( value ) );
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
						
					case PROMPT_BEFORE_OVERWRITE:
						promptBeforeOverwrite.setValue( Boolean.valueOf( value ) );
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
							removeDarkTheme();
						}
						settings.remove ( setting );
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
			
			currentListPane.currentListTable.refresh();
	
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
			
			boolean disableVolume = !audioSystem.volumeChangeSupported();
			
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
			currentListPane.currentListTable.refresh(); //To get the play/pause image to update. 
		});
	}

	@Override
	public void playerUnpaused () {
		Platform.runLater( () -> {
			transport.togglePlayButton.setGraphic( transport.pauseImage );
			artSplitPane.setImages( audioSystem.getCurrentTrack() );
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
			audioSystem.setVolumePercent( 1 );

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
	
	public File promptUserForFolder() {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setTitle( "Export Playlist As Folder" );
		File targetFile = dirChooser.showDialog( mainStage );
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

	public void setUpdateAvailable ( boolean updateAvailable ) {
		this.updateAvailable.setValue( updateAvailable );
	}

	public void setCurrentListSortMode ( DefaultSortMode sortMode ) {
		currentListPane.setSortMode ( sortMode );
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
