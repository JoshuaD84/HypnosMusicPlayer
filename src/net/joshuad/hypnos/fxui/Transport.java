package net.joshuad.hypnos.fxui;

import java.io.FileInputStream;
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
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import net.joshuad.hypnos.CurrentListTrack;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;

public class Transport extends VBox {
	private static final Logger LOGGER = Logger.getLogger( Transport.class.getName() );
	
	ImageView playImage, pauseImage, stopImage, nextImage, previousImage;
	Image playImageSource, pauseImageSource;
	ImageView settingsImage;
	ImageView[] volumeImages = new ImageView[ 4 ];
	
	Button togglePlayButton, previousButton, nextButton, stopButton;
	Button showSettingsButton;
	
	Label timeElapsedLabel = new Label( "" );
	Label timeRemainingLabel = new Label( "" );
	Button currentTrackButton;
	Tooltip currentTrackTooltip;
	
	Tooltip volumeDisabledTooltip = new Tooltip ( "Volume control not supported for tracks with this encoding." );
	
	Slider trackPositionSlider;
	Slider volumeSlider;
	Button volumeMuteButton;

	boolean sliderMouseHeld;

	HBox volumePane;

	ColorAdjust darkThemeTransportButtons = new ColorAdjust(); {
		darkThemeTransportButtons.setSaturation( -1 );
		darkThemeTransportButtons.setHue( 1 );
		darkThemeTransportButtons.setBrightness( .55 );
	}
	
	FXUI ui;
	AudioSystem audioSystem;
	
	public Transport( FXUI ui, AudioSystem player ) {
		this.ui = ui;
		this.audioSystem = player;
		
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
					if ( ui.currentListPane.currentListTable.getItems().size() != 0 ) {
						CurrentListTrack selectedItem = ui.currentListPane.currentListTable.getSelectionModel().getSelectedItem();
						
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
		volumeMuteButton.setPadding( new Insets ( 0, 5, 0, 5 ) );
		volumeMuteButton.getStyleClass().add( "volumeButton" );
		volumeMuteButton.setTooltip( new Tooltip ( "Toggle Mute" ) );
		volumeMuteButton.setOnAction( e -> player.toggleMute() );

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

		showSettingsButton = new Button ( );
		showSettingsButton.setGraphic( settingsImage );

		switch ( Hypnos.getOS() ) {
			case WIN_10: case WIN_7: case WIN_8: case WIN_UNKNOWN: case WIN_VISTA: case WIN_XP:
				showSettingsButton.setPadding( new Insets ( 5, 5, 0, 5 ) );
				break;
				
			case NIX: case OSX: case UNKNOWN:
				showSettingsButton.setPadding( new Insets ( 0, 5, 0, 5 ) );
				break;
		}
		
		showSettingsButton.getStyleClass().add( "settingsButton" );
		showSettingsButton.setTooltip( new Tooltip( "Configuration and Information" ) );
		
		showSettingsButton.setOnAction ( ( ActionEvent event ) -> {
			ui.showSettingsWindow();
		});
		
		Label settingsWidthPadding = new Label ( "" );
		settingsWidthPadding.setMinWidth( 36 ); // I couldn't figure out how to make it the exact same width as settings button
		
		BorderPane playingTrackInfo = new BorderPane();
		currentTrackButton = new Button( "" );
		currentTrackButton.setPadding( new Insets ( 10, 0, 0, 0 ) );
		currentTrackButton.getStyleClass().add( "trackName" );
		playingTrackInfo.setCenter( currentTrackButton );
		playingTrackInfo.setRight( showSettingsButton );
		playingTrackInfo.setLeft( settingsWidthPadding );
		
		currentTrackButton.setOnMouseClicked( ( MouseEvent event ) -> {
			
			if ( event.getButton() == MouseButton.PRIMARY ) {
				ui.selectCurrentTrack();
			}
		});
		
		currentTrackTooltip = new Tooltip ( "" );
		currentTrackButton.setTooltip( currentTrackTooltip );
		
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem appendMenuItem = new MenuItem( "Append" );
		MenuItem playNextMenuItem = new MenuItem( "Play Next" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem infoMenuItem = new MenuItem( "Info" );
		MenuItem lyricsMenuItem = new MenuItem( "Lyrics" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				Track current = player.getCurrentTrack();
				if ( current != null ) {
					ui.promptAndSavePlaylist ( Arrays.asList( current ) );
				}
			}
		});

		EventHandler<ActionEvent> addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {

				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				
				Track currentTrack = player.getCurrentTrack();
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
		
		playNextMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Track currentTrack = player.getCurrentTrack();
				if ( currentTrack != null ) {
					player.getQueue().queueTrack( 0, currentTrack );
				}
			}
		});
		
		enqueueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Track currentTrack = player.getCurrentTrack();
				if ( currentTrack != null ) {
					player.getQueue().queueTrack( currentTrack );
				}
			}
		});
		
		editTagMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {

				Track currentTrack = player.getCurrentTrack();
				if ( currentTrack != null ) {
					ui.tagWindow.setTracks( Arrays.asList( currentTrack ), null );
					ui.tagWindow.show();
				}
			}
		});
		
		
		infoMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Track currentTrack = player.getCurrentTrack();
				if ( currentTrack != null ) {
					ui.trackInfoWindow.setTrack( currentTrack );
					ui.trackInfoWindow.show();
				}
			}
		});

		lyricsMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ui.lyricsWindow.setTrack( player.getCurrentTrack() );
				ui.lyricsWindow.show();
			}
		});
		
		browseMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ui.openFileBrowser(  player.getCurrentTrack().getPath() );
			}
		});
		
		ContextMenu currentTrackButtonMenu = new ContextMenu();
		currentTrackButtonMenu.getItems().addAll( playMenuItem, appendMenuItem, playNextMenuItem, 
			enqueueMenuItem, editTagMenuItem, infoMenuItem, lyricsMenuItem, browseMenuItem, addToPlaylistMenuItem );
		
		currentTrackButton.setContextMenu( currentTrackButtonMenu );
		
		getChildren().add( playingTrackInfo );
		getChildren().add( controls );
		setPadding( new Insets( 0, 0, 10, 0 ) );
		setSpacing( 5 );
		setId( "transport" );
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
	}
	
	public void applyDarkTheme () {
		
		if ( stopImage != null ) stopImage.setEffect( darkThemeTransportButtons );
		if ( nextImage != null ) nextImage.setEffect( darkThemeTransportButtons );
		if ( previousImage != null ) previousImage.setEffect( darkThemeTransportButtons );
		if ( pauseImage != null ) pauseImage.setEffect( darkThemeTransportButtons );
		if ( playImage != null ) playImage.setEffect( darkThemeTransportButtons );
		if ( settingsImage != null ) settingsImage.setEffect( darkThemeTransportButtons );
		
		for ( int k = 0; k < volumeImages.length; k++ ) {
			if ( volumeImages[k] != null ) volumeImages[k].setEffect( darkThemeTransportButtons );
		}
	}	
	
	public void removeDarkTheme() {
		if ( stopImage != null ) stopImage.setEffect( null );
		if ( nextImage != null ) nextImage.setEffect( null );
		if ( previousImage != null ) previousImage.setEffect( null );
		if ( pauseImage != null ) pauseImage.setEffect( null );
		if ( playImage != null ) playImage.setEffect( null );
		if ( settingsImage != null ) settingsImage.setEffect( null );
		
		for ( int k = 0; k < volumeImages.length; k++ ) {
			if ( volumeImages[k] != null ) volumeImages[k].setEffect( null );
		}
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
		return darkThemeTransportButtons;
	}

	public void doAfterShowProcessing () {		
		StackPane thumb = (StackPane) trackPositionSlider.lookup( ".thumb" );
		thumb.setVisible( false );
	}
}
