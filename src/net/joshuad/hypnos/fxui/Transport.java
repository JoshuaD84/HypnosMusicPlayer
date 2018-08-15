package net.joshuad.hypnos.fxui;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import net.joshuad.hypnos.CurrentListTrack;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;
import net.joshuad.hypnos.lastfm.LastFM.LovedState;

public class Transport extends VBox {
	private static final Logger LOGGER = Logger.getLogger( Transport.class.getName() );
	
	ImageView playImage, pauseImage, stopImage, nextImage, previousImage, loveImage;
	Image playImageSource, pauseImageSource;
	ImageView settingsImage;
	ImageView[] volumeImages = new ImageView[ 4 ];
	
	Button togglePlayButton, previousButton, nextButton, stopButton, loveButton;
	Button showSettingsButton;
	Hyperlink updateAvailableButton;
	
	Label timeElapsedLabel = new Label( "" );
	Label timeRemainingLabel = new Label( "" );
	Button currentTrackButton;
	Tooltip currentTrackTooltip;
		
	Slider trackPositionSlider;
	Slider volumeSlider;
	Button volumeMuteButton;

	boolean sliderMouseHeld;

	HBox volumePane;

	ColorAdjust lightThemeButtonEffect = new ColorAdjust(); {
		lightThemeButtonEffect.setBrightness( -.75d );
		lightThemeButtonEffect.setHue( 0 );
		lightThemeButtonEffect.setSaturation( 0 );
	}
	
	ColorAdjust lightThemeButtonsHover = new ColorAdjust(); {
		lightThemeButtonsHover.setBrightness( -.5d );
		lightThemeButtonsHover.setSaturation( .7d );
		lightThemeButtonsHover.setHue( -.875d );
	}
	
	ColorAdjust darkThemeButtonEffect = new ColorAdjust(); {
		darkThemeButtonEffect.setBrightness( -.4d );
		darkThemeButtonEffect.setHue( 0 );
		darkThemeButtonEffect.setSaturation( 0 );
	}
	
	ColorAdjust darkThemeButtonsHover = new ColorAdjust(); {
		darkThemeButtonsHover.setBrightness( -.2d );
		darkThemeButtonsHover.setSaturation( .4d );
		darkThemeButtonsHover.setHue( -.75d );
	}
	
	ColorAdjust darkThemeLovedEffect = new ColorAdjust(); {
		darkThemeLovedEffect.setBrightness( -.4d );
		darkThemeLovedEffect.setHue( .15d );
		darkThemeLovedEffect.setSaturation( .75d );
	}
	
	ColorAdjust darkThemeNotLovedEffect = new ColorAdjust(); {
		darkThemeNotLovedEffect.setBrightness( -.4d );
		darkThemeNotLovedEffect.setHue( 0 );
		darkThemeNotLovedEffect.setSaturation( 0 );
	}
	
	ColorAdjust lightThemeLovedEffect = new ColorAdjust(); {
		lightThemeLovedEffect.setBrightness( -.1d );
		lightThemeLovedEffect.setHue( .0d );
		lightThemeLovedEffect.setSaturation( .6d );
	}
	
	ColorAdjust lightThemeNotLovedEffect = new ColorAdjust(); {
		lightThemeNotLovedEffect.setBrightness( -.3d );
		lightThemeNotLovedEffect.setHue( -0d );
		lightThemeNotLovedEffect.setSaturation( .0d );
	}
	
	Border loveBorder = new Border ( new BorderStroke ( 
		Color.TRANSPARENT, 
		BorderStrokeStyle.NONE, 
		new CornerRadii ( 5 ),
		BorderWidths.DEFAULT
	));
	
	Border loveBorderHover = new Border ( new BorderStroke ( 
		Color.GREY, 
		BorderStrokeStyle.DOTTED, 
		new CornerRadii ( 5 ),
		BorderWidths.DEFAULT
	));
	
	FXUI ui;
	AudioSystem audioSystem;
	
	public Transport( FXUI ui, AudioSystem audioSystem ) {
		this.ui = ui;
		this.audioSystem = audioSystem;
		
		loadImages();
		
		togglePlayButton = new Button ( "" );
		togglePlayButton.setGraphic( playImage );
		togglePlayButton.setPrefSize( 42, 35 );
		togglePlayButton.setMinSize( 42, 35 );
		togglePlayButton.setMaxSize( 42, 35 );
		togglePlayButton.setTooltip( new Tooltip( "Toggle Play/Pause" ) );

		
		previousButton = new Button ( "" );
		previousButton.setGraphic( previousImage );
		previousButton.setPrefSize( 42, 35 );
		previousButton.setMinSize( 42, 35 );
		previousButton.setMaxSize( 42, 35 );
		previousButton.setTooltip( new Tooltip( "Previous Track" ) );
		
		nextButton = new Button ( "" );
		nextButton.setGraphic( nextImage );
		nextButton.setPrefSize( 42, 35 );
		nextButton.setMinSize( 42, 35 );
		nextButton.setMaxSize( 42, 35 );
		nextButton.setTooltip( new Tooltip( "Next Track" ) );
		
		stopButton = new Button ( "" );
		stopButton.setGraphic( stopImage );
		stopButton.setPrefSize( 42, 35 );
		stopButton.setMinSize( 42, 35 );
		stopButton.setMaxSize( 42, 35 );
		stopButton.setTooltip( new Tooltip( "Stop" ) );
		

		previousButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				ui.previousRequested();
			}
		} );

		nextButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				audioSystem.next();
			}
		});

		stopButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				audioSystem.stop( StopReason.USER_REQUESTED );
			}
		} );

		togglePlayButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				if ( audioSystem.isStopped() ) { 
					if ( ui.currentListPane.currentListTable.getItems().size() != 0 ) {
						CurrentListTrack selectedItem = ui.currentListPane.currentListTable.getSelectionModel().getSelectedItem();
						
						if ( selectedItem != null ) {
							audioSystem.playTrack( selectedItem );
						} else {
							audioSystem.next( false );
						}
					}
				} else {
					audioSystem.togglePause();
				}
			}
		} );

		loveButton = new Button( );
		loveButton.setTooltip( new Tooltip( "Love on LastFM" ) );
		loveButton.setVisible( false );
		loveButton.setManaged( false );
		loveButton.getStyleClass().add( "loveButton" );
		loveButton.setPadding( new Insets ( 0, 10, 0, 10 ) );
		loveButton.visibleProperty().bindBidirectional( ui.showLastFMWidgets );
		loveButton.setBorder( loveBorder );
		
		ui.showLastFMWidgets.addListener( ( observable, oldValue, newValue ) -> {
			loveButton.setManaged( newValue );
			if ( audioSystem.isStopped() ) loveButton.setGraphic( null );
			else loveButton.setGraphic( loveImage );
			updateLovedIndicator( false );
		});
		
		loveButton.setOnAction( ( ActionEvent e ) -> {
			boolean currentlyLoved = false;
			
			if ( loveImage.getEffect() == darkThemeLovedEffect || loveImage.getEffect() == lightThemeLovedEffect ) {
				currentlyLoved = true;
			}
			
			if ( currentlyLoved ) {
				loveImage.setEffect ( ui.isDarkTheme() ? darkThemeNotLovedEffect : lightThemeNotLovedEffect );
			} else {
				loveImage.setEffect ( ui.isDarkTheme() ? darkThemeLovedEffect : lightThemeLovedEffect );
			}
			
			Thread taskThread = new Thread (() -> {
				audioSystem.getLastFM().toggleLoveTrack( audioSystem.getCurrentTrack() );
				Platform.runLater( () -> updateLovedIndicator( false ) );
			});
			taskThread.setDaemon( true );
			taskThread.start();
		});
		
		loveButton.hoverProperty().addListener( ( observable, oldValue, newValue ) -> {
			loveButton.setBorder( newValue ? loveBorderHover : loveBorder );
		});
		
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
					audioSystem.seekPercent( trackPositionSlider.getValue() / trackPositionSlider.getMax() );
				}
			}
		});

		trackPositionSlider.setOnMousePressed( ( MouseEvent e ) -> {
			sliderMouseHeld = true;
		});

		trackPositionSlider.setOnMouseReleased( ( MouseEvent e ) -> {
			sliderMouseHeld = false;
			audioSystem.seekPercent( trackPositionSlider.getValue() / trackPositionSlider.getMax() );
		});
		
		volumeMuteButton = new Button ( );
		volumeMuteButton.setGraphic( volumeImages[3] );
		volumeMuteButton.getStyleClass().add( "volumeLabel" );
		volumeMuteButton.setPadding( new Insets ( 0, 5, 0, 5 ) );
		volumeMuteButton.getStyleClass().add( "volumeButton" );
		volumeMuteButton.setTooltip( new Tooltip ( "Toggle Mute" ) );
		volumeMuteButton.setOnAction( e -> audioSystem.toggleMute() );
		volumeMuteButton.hoverProperty().addListener( ( obserable, oldValue, newValue ) -> {
			for ( ImageView volumeImage : volumeImages ) {
				applyHover ( volumeImage, newValue );
			}
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
				audioSystem.setVolumePercent( percent ); //Note: this works because min is 0 
			}
		};
		
		EventHandler<ScrollEvent> volumeSliderScrollHandler = new EventHandler<ScrollEvent> () {
			@Override
			public void handle ( ScrollEvent event ) {
				if ( event.getDeltaY() < 0 ) {
					audioSystem.decrementVolume();
				} else if ( event.getDeltaY() > 0 ) {
					audioSystem.incrementVolume();
				}
			}
		};
		
		volumeMuteButton.setOnScroll ( volumeSliderScrollHandler );
		
		volumeSlider.setOnMouseDragged ( volumeSliderHandler );
		volumeSlider.setOnMouseClicked ( volumeSliderHandler );
		volumeSlider.setOnScroll( volumeSliderScrollHandler );
		
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

		showSettingsButton = new Button ( );
		showSettingsButton.setGraphic( settingsImage );
		showSettingsButton.getStyleClass().add( "settingsButton" );
		showSettingsButton.setTooltip( new Tooltip( "Configuration and Information" ) );
		showSettingsButton.setOnAction ( e -> ui.settingsWindow.show() );
		showSettingsButton.hoverProperty().addListener( ( obserable, oldValue, newValue ) -> {
			applyHover ( settingsImage, newValue );
		});
		
		updateAvailableButton = new Hyperlink ( "!" );
		updateAvailableButton.getStyleClass().add( "updateButton" );
		updateAvailableButton.setTooltip( new Tooltip( "An Update is Available!" ) );
		updateAvailableButton.setOnAction ( event -> ui.openWebBrowser( "http://hypnosplayer.org" ) );
		
		//switch ( Hypnos.getOS() ) {
		//	case WIN_10: case WIN_7: case WIN_8: case WIN_UNKNOWN: case WIN_VISTA: case WIN_XP:
				showSettingsButton.setPadding( new Insets ( 7, 5, 0, 5 ) );
				updateAvailableButton.setPadding( new Insets ( 7, 5, 5, 0 ) );
		//		break;
				
		//	case NIX: case OSX: case UNKNOWN:
		//		showSettingsButton.setPadding( new Insets ( 0, 5, 0, 5 ) );
		//		updateAvailableButton.setPadding( new Insets ( 5, 5, 5, 0 ) );
		//		break;
		//}
		
		//Make it not take up space when it is not visible.
		updateAvailableButton.managedProperty().bind( updateAvailableButton.visibleProperty() );
		updateAvailableButton.visibleProperty().bind( ui.updateAvailableProperty().and( ui.showUpdateAvailableInUIProperty() ) );
		
		HBox settingsBox = new HBox();
		settingsBox.getChildren().addAll( showSettingsButton, updateAvailableButton );
		
		Label settingsWidthPadding = new Label ( "" );
		//settingsWidthPadding.prefWidthProperty().bind( settingsBox.widthProperty() );
		
		BorderPane playingTrackInfo = new BorderPane();
		currentTrackButton = new Button( "" );
		currentTrackButton.setPadding( new Insets ( 10, 0, 0, 0 ) );
		currentTrackButton.getStyleClass().add( "trackName" );
		
		HBox currentTrackBox = new HBox();
		currentTrackBox.setAlignment( Pos.BOTTOM_CENTER );
		currentTrackBox.getChildren().addAll( currentTrackButton, loveButton );
		
		playingTrackInfo.setCenter( currentTrackBox );
		playingTrackInfo.setRight( settingsBox );
		playingTrackInfo.setLeft( settingsWidthPadding );
		settingsWidthPadding.minWidthProperty().bind( settingsBox.widthProperty() );
		
		currentTrackButton.setOnMouseClicked( ( MouseEvent event ) -> {
			if ( event.getButton() == MouseButton.PRIMARY ) {
				ui.selectCurrentTrack();
			}
		});
		
		currentTrackTooltip = new Tooltip ( "" );
		currentTrackButton.setTooltip( currentTrackTooltip );
		
		currentTrackButton.setOnDragDetected( event -> {
			if ( audioSystem.getCurrentTrack() != null ) {
				ArrayList <Track> tracks = new ArrayList<> ( Arrays.asList( audioSystem.getCurrentTrack() ) );
				DraggedTrackContainer dragObject = new DraggedTrackContainer( null, tracks, null, null, DragSource.CURRENT_TRACK );
				Dragboard db = currentTrackButton.startDragAndDrop( TransferMode.COPY );
				db.setDragView( currentTrackButton.snapshot( null, null ) );
				ClipboardContent cc = new ClipboardContent();
				cc.put( FXUI.DRAGGED_TRACKS, dragObject );
				db.setContent( cc );
				event.consume();
			}
		});
		
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem appendMenuItem = new MenuItem( "Append" );
		MenuItem playNextMenuItem = new MenuItem( "Play Next" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem infoMenuItem = new MenuItem( "Info" );
		MenuItem lyricsMenuItem = new MenuItem( "Lyrics" );
		MenuItem goToAlbumMenuItem = new MenuItem( "Go to Album" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				Track current = audioSystem.getCurrentTrack();
				if ( current != null ) {
					ui.promptAndSavePlaylist ( Arrays.asList( current ) );
				}
			}
		});

		EventHandler<ActionEvent> addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {

				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				
				Track currentTrack = audioSystem.getCurrentTrack();
				if ( currentTrack != null && playlist != null ) {
					ui.addToPlaylist ( Arrays.asList( currentTrack ), playlist );
				}
			}
		};
		
		ui.library.getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Track currentTrack = audioSystem.getCurrentTrack();
				if ( currentTrack != null ) {
					audioSystem.playTrack( currentTrack );
				}
			}
		});

		appendMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Track currentTrack = audioSystem.getCurrentTrack();
				if ( currentTrack != null ) {
					audioSystem.getCurrentList().appendTrack ( currentTrack );
				}
			}
		});
		
		playNextMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Track currentTrack = audioSystem.getCurrentTrack();
				if ( currentTrack != null ) {
					audioSystem.getQueue().queueTrack( 0, currentTrack );
				}
			}
		});
		
		enqueueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Track currentTrack = audioSystem.getCurrentTrack();
				if ( currentTrack != null ) {
					audioSystem.getQueue().queueTrack( currentTrack );
				}
			}
		});
		
		editTagMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {

				Track currentTrack = audioSystem.getCurrentTrack();
				if ( currentTrack != null ) {
					ui.tagWindow.setTracks( Arrays.asList( currentTrack ), null );
					ui.tagWindow.show();
				}
			}
		});
		
		infoMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Track currentTrack = audioSystem.getCurrentTrack();
				if ( currentTrack != null ) {
					ui.trackInfoWindow.setTrack( currentTrack );
					ui.trackInfoWindow.show();
				}
			}
		});

		lyricsMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ui.lyricsWindow.setTrack( audioSystem.getCurrentTrack() );
				ui.lyricsWindow.show();
			}
		});
		
		goToAlbumMenuItem.setOnAction( ( event ) -> {
			ui.goToAlbumOfTrack ( audioSystem.getCurrentTrack() );
		});
		
		browseMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ui.openFileBrowser(  audioSystem.getCurrentTrack().getPath() );
			}
		});
		
		Menu lastFMMenu = new Menu( "LastFM" );
		MenuItem loveMenuItem = new MenuItem ( "Love" );
		MenuItem unloveMenuItem = new MenuItem ( "Unlove" );
		MenuItem scrobbleMenuItem = new MenuItem ( "Scrobble" );
		lastFMMenu.getItems().addAll ( loveMenuItem, unloveMenuItem, scrobbleMenuItem );
		lastFMMenu.setVisible ( false );
		lastFMMenu.visibleProperty().bind( ui.showLastFMWidgets );
		
		loveMenuItem.setOnAction( ( event ) -> {
			ui.audioSystem.getLastFM().loveTrack( ui.audioSystem.getCurrentTrack() );
		});
		
		unloveMenuItem.setOnAction( ( event ) -> {
			ui.audioSystem.getLastFM().unloveTrack( ui.audioSystem.getCurrentTrack() );
		});
		
		scrobbleMenuItem.setOnAction( ( event ) -> {
			ui.audioSystem.getLastFM().scrobbleTrack( ui.audioSystem.getCurrentTrack() );
		});
		
		ContextMenu currentTrackButtonMenu = new ContextMenu();
		currentTrackButtonMenu.getItems().addAll( playMenuItem, appendMenuItem, playNextMenuItem, 
			enqueueMenuItem, editTagMenuItem, infoMenuItem, lyricsMenuItem, goToAlbumMenuItem,
			browseMenuItem, addToPlaylistMenuItem, lastFMMenu );
		
		currentTrackButton.setContextMenu( currentTrackButtonMenu );
		
		getChildren().add( playingTrackInfo );
		getChildren().add( controls );
		setPadding( new Insets( 0, 0, 10, 0 ) );
		setSpacing( 5 );
		setId( "transport" );
	}
	
	private void updateLovedIndicator( boolean fromCache ) {
		LovedState loved = audioSystem.getLastFM().isLoved( audioSystem.getCurrentTrack(), fromCache );
	
		if ( loved == LovedState.TRUE ) {
			loveImage.setEffect ( ui.isDarkTheme() ? darkThemeLovedEffect : lightThemeLovedEffect );
		} else {
			loveImage.setEffect ( ui.isDarkTheme() ? darkThemeNotLovedEffect : lightThemeNotLovedEffect );
		}
	}
	
	private void applyHover ( ImageView image, Boolean hasHover ) {
		if ( hasHover ) {
			image.setEffect ( ui.isDarkTheme() ? darkThemeButtonsHover : lightThemeButtonsHover );
		} else {
			image.setEffect ( ui.isDarkTheme() ? darkThemeButtonEffect : lightThemeButtonEffect );
		}
	}

	private void loadImages() {
		double volFitWidth = 55 * .52;
		double volFitHeight = 45 * .52;
		
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
		
		playImage = null;
		try {
			playImageSource = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/play.png" ).toFile() ) );
			playImage = new ImageView ( playImageSource );
			playImage.setFitHeight( 18 );
			playImage.setFitWidth( 18 );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/play.png", e );
		}

		pauseImage = null;
		try {
			pauseImageSource = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/pause.png" ).toFile() ) );
			pauseImage = new ImageView ( pauseImageSource );
			pauseImage.setFitHeight( 18 );
			pauseImage.setFitWidth( 18 );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load pause icon: resources/pause.png", e );
		}
		
		previousImage = null;
		try {
			previousImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/previous.png" ).toFile() ) ) );
			previousImage.setFitHeight( 18 );
			previousImage.setFitWidth( 18 );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load previous icon: resources/previous.png", e );
		}
		
		nextImage = null;
		try {
			nextImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/next.png" ).toFile() ) ) );
			nextImage.setFitHeight( 18 );
			nextImage.setFitWidth( 18 );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load previous icon: resources/next.png", e );
		}
		
		stopImage = null;
		try {
			stopImage = new ImageView ( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/stop.png" ).toFile() ) ) );
			stopImage.setFitHeight( 18 );
			stopImage.setFitWidth( 18 );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load previous icon: resources/stop.png", e );
		}
		
		loveImage = null;
		try {
			Image loveImageSource = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/love.png" ).toFile() ) );
			loveImage = new ImageView ( loveImageSource );
			loveImage.setFitHeight( 14 );
			loveImage.setFitWidth( 14 );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load play icon: resources/love.png", e );
		}
	}
	
	public void applyDarkTheme () {
		
		if ( stopImage != null ) stopImage.setEffect( darkThemeButtonEffect );
		if ( nextImage != null ) nextImage.setEffect( darkThemeButtonEffect );
		if ( previousImage != null ) previousImage.setEffect( darkThemeButtonEffect );
		if ( pauseImage != null ) pauseImage.setEffect( darkThemeButtonEffect );
		if ( playImage != null ) playImage.setEffect( darkThemeButtonEffect );
		if ( settingsImage != null ) settingsImage.setEffect( darkThemeButtonEffect );
		
		for ( int k = 0; k < volumeImages.length; k++ ) {
			if ( volumeImages[k] != null ) volumeImages[k].setEffect( darkThemeButtonEffect );
		}
		updateLovedIndicator( true );
	}	
	
	public void applyLightTheme() {
		if ( stopImage != null ) stopImage.setEffect( lightThemeButtonEffect );
		if ( nextImage != null ) nextImage.setEffect( lightThemeButtonEffect );
		if ( previousImage != null ) previousImage.setEffect( lightThemeButtonEffect );
		if ( pauseImage != null ) pauseImage.setEffect( lightThemeButtonEffect );
		if ( playImage != null ) playImage.setEffect( lightThemeButtonEffect );
		if ( settingsImage != null ) settingsImage.setEffect( lightThemeButtonEffect );
		
		for ( int k = 0; k < volumeImages.length; k++ ) {
			if ( volumeImages[k] != null ) volumeImages[k].setEffect( lightThemeButtonEffect );
		}
		updateLovedIndicator( true );
	}
	
	public void update ( int timeElapsedS, int timeRemainingS, double percent )  {
		Platform.runLater( new Runnable() {
			public void run () {

				if ( audioSystem.isPlaying() || audioSystem.isPaused() ) {
					if ( !trackPositionSlider.isValueChanging() && !sliderMouseHeld ) {
						trackPositionSlider.setValue( (trackPositionSlider.getMax() - trackPositionSlider.getMin()) * percent + trackPositionSlider.getMin() );
					}
					timeElapsedLabel.setText( Utils.getLengthDisplay( timeElapsedS ) );
					timeRemainingLabel.setText( Utils.getLengthDisplay( -timeRemainingS ) );
				} else if ( audioSystem.isStopped() ) {
					ui.currentListPane.currentListTable.refresh();
					togglePlayButton.setGraphic( playImage );
					trackPositionSlider.setValue( 0 );
					timeElapsedLabel.setText( "" );
					timeRemainingLabel.setText( "" );
					currentTrackButton.setText( "" );
					loveButton.setText( "" );
					currentTrackTooltip.setText( "No current track." );
			
					StackPane thumb = (StackPane) trackPositionSlider.lookup( ".thumb" );
					thumb.setVisible( false );
				}
			}
		});
	}
	
	//@Override
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


	public ColorAdjust getDarkThemeButtonAdjust () {
		return darkThemeButtonEffect;
	}

	public void doAfterShowProcessing () {		
		StackPane thumb = (StackPane) trackPositionSlider.lookup( ".thumb" );
		thumb.setVisible( false );
	}

	public void playerStarted ( Track track ) {
		Runnable runMe = () -> {
			StackPane thumb = (StackPane) trackPositionSlider.lookup( ".thumb" );
			thumb.setVisible( true );
			
			currentTrackButton.setText( track.getArtist() + " - " + track.getTitle() );
			currentTrackTooltip.setText( 
				"Album: " + track.getAlbumTitle() + "\n" +
				"Year: " + track.getYear() + "\n" +
				"Length: " + Utils.getLengthDisplay( track.getLengthS() ) + "\n" + 
				"Encoding: " + track.getShortEncodingString()
			);
	
			togglePlayButton.setGraphic( pauseImage );
			
			if ( ui.showLastFMWidgets.get() ) {
				loveButton.setGraphic( loveImage );
				
				Thread taskThread = new Thread (() -> {
					//Load the data to cache now, so we can access it quickly w/ Platform call
					audioSystem.getLastFM().isLoved( audioSystem.getCurrentTrack(), false ); 
					Platform.runLater( () -> updateLovedIndicator( true ) );
				});
				
				taskThread.setDaemon( true );
				taskThread.start();
			}
		};
		
		if ( Platform.isFxApplicationThread() ) {
			runMe.run();
		} else {
			Platform.runLater( runMe );
		}
	}

	public void playerStopped ( Track track, StopReason reason ) {
		
		Runnable runMe = () -> {
			update ( 0, 0, 0 ); //values don't matter.
			volumeSlider.setDisable( false );
			volumeMuteButton.setDisable( false );

			if ( ui.showLastFMWidgets.get() ) loveButton.setGraphic( null );
		};
		
		if ( Platform.isFxApplicationThread() ) {
			runMe.run();
		} else {
			Platform.runLater( runMe );
		}
		
	}
}
