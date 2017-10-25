package net.joshuad.hypnos.fxui;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.joshuad.hypnos.CurrentList.DefaultRepeatMode;
import net.joshuad.hypnos.CurrentList.DefaultShuffleMode;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Hypnos.OS;
import net.joshuad.hypnos.Library;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys.Hotkey;
import net.joshuad.hypnos.TagError;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;

public class SettingsWindow extends Stage {

	private static final Logger LOGGER = Logger.getLogger( SettingsWindow.class.getName() );
	
	private FXUI ui;
	private Library library;
	private GlobalHotkeys hotkeys;
	private AudioSystem player;
	
	private TabPane tabPane;
	private Tab hotkeysTab;
	
	private EnumMap <Hotkey, TextField> hotkeyFields = new EnumMap <Hotkey, TextField> ( Hotkey.class );
	
	private ChoiceBox <String> albumShuffleChoices;
	private ChoiceBox <String> albumRepeatChoices;
	private ChoiceBox <String> trackShuffleChoices;
	private ChoiceBox <String> trackRepeatChoices;
	private ChoiceBox <String> playlistShuffleChoices;
	private ChoiceBox <String> playlistRepeatChoices;
	
	
	private ToggleButton lightTheme, darkTheme;
	private ToggleGroup themeToggleGroup;
	
	SettingsWindow( FXUI ui, Library library, GlobalHotkeys hotkeys, AudioSystem player ) {
		super();
		
		this.ui = ui;
		this.library = library;
		this.hotkeys = hotkeys;
		this.player = player;

		initModality( Modality.NONE );
		initOwner( ui.getMainStage() );		
		setWidth ( 700 );
		setHeight ( 650 );
		setTitle( "Config and Info" );
		Pane root = new Pane();
		Scene scene = new Scene( root );
		
		tabPane = new TabPane();
		
		
		Tab settingsTab = setupSettingsTab( root, ui );
		hotkeysTab = setupHotkeysTab ( root );
		Tab logTab = setupLogTab( root );
		Tab tagTab = setupTagTab ( root );
		Tab aboutTab = setupAboutTab( root ); 
		
		tabPane.getTabs().addAll( settingsTab, hotkeysTab, logTab, tagTab, aboutTab );
		
		tabPane.prefWidthProperty().bind( root.widthProperty() );
		tabPane.prefHeightProperty().bind( root.heightProperty() );

		
		VBox primaryPane = new VBox();
		primaryPane.getChildren().addAll( tabPane );
		root.getChildren().add( primaryPane );
		setScene( scene );
		refreshHotkeyFields();
		
		setOnCloseRequest ( ( WindowEvent event ) -> {
			Hypnos.getPersister().saveHotkeys();
			Hypnos.getPersister().saveSettings();
		});
	}	
	
	private Tab setupHotkeysTab ( Pane root ) {
		
		GridPane globalContent = new GridPane();
		globalContent.setAlignment( Pos.TOP_CENTER );
		globalContent.setPadding( new Insets ( 10 ) );
		globalContent.setVgap( 2 );
				
		Tab hotkeysTab = new Tab ( "Global Hotkeys" );
		hotkeysTab.setClosable( false );
		hotkeysTab.setContent( globalContent );
		
		int row = 0;
	
		Label headerLabel = new Label ( "Global Hotkeys" );
		headerLabel.setPadding( new Insets ( 0, 0, 10, 0 ) );
		headerLabel.setWrapText( true );
		headerLabel.setTextAlignment( TextAlignment.CENTER );
		headerLabel.setStyle( "-fx-alignment: center; -fx-font-size: 20px; -fx-font-weight: bold" );
		globalContent.add( headerLabel, 0, row, 2, 1 );
		GridPane.setHalignment( headerLabel, HPos.CENTER );
		row++;
		
		Label descriptionLabel = new Label ( "These hotkeys will also work when Hypnos is minimized or out of focus." );
		descriptionLabel.setPadding( new Insets ( 0, 0, 20, 0 ) );
		descriptionLabel.setWrapText( true );
		descriptionLabel.setTextAlignment( TextAlignment.CENTER );
		globalContent.add( descriptionLabel, 0, row, 2, 1 );
		GridPane.setHalignment( descriptionLabel, HPos.CENTER );
		row++;
		
		for ( Hotkey key : Hotkey.values() ) {
			Label label = new Label ( key.getLabel() );
			label.setStyle( "-fx-alignment: center" );
			label.setPadding( new Insets ( 0, 20, 0, 0 ) );
			
			TextField field = new TextField ();
			field.setStyle( "-fx-alignment: center" );
			field.setPrefWidth( 200 );
			
			field.setOnKeyPressed( ( KeyEvent e ) -> { 
				
				String shortcut = "";
				if ( e.isControlDown() ) shortcut += "Ctrl + ";
				if ( e.isAltDown() ) shortcut += "Alt + ";
				if ( e.isShiftDown() ) shortcut += "Shift + ";
				if ( e.isMetaDown() ) shortcut += "Meta + ";
				
				boolean registered = false;
				if ( e.getCode().isModifierKey() ) {
					field.setText( shortcut );
					
				} else if ( e.getCode().equals( KeyCode.ESCAPE ) ) {
					field.setText( "" );
					hotkeys.clearHotkey ( key );
				
				} else {
					shortcut += e.getCode().getName();
					registered = hotkeys.registerLastCombination( key, shortcut );
					
					if ( registered ) {
						field.setText( shortcut );
						refreshHotkeyFields();
					}
				}

				field.positionCaret( shortcut.length() );
				
				e.consume();
			});
			
			field.setOnKeyTyped( ( KeyEvent e ) -> {
				e.consume();
			});
			
			field.setOnKeyReleased( ( KeyEvent e ) -> {
				refreshHotkeyFields();
			});
			
			globalContent.add( label, 0, row );
			globalContent.add( field, 1, row );
			
			hotkeyFields.put( key, field );
			
			row++;
		}
		
		Label clearHotkeyLabel = new Label ( "(Use <ESC> to erase a global hotkey)" );
		clearHotkeyLabel.setPadding( new Insets ( 20, 0, 20, 0 ) );
		clearHotkeyLabel.setWrapText( true );
		clearHotkeyLabel.setTextAlignment( TextAlignment.CENTER );
		globalContent.add( clearHotkeyLabel, 0, row, 2, 1 );
		GridPane.setHalignment( clearHotkeyLabel, HPos.CENTER );
		row++;
		
		if ( Hypnos.globalHotkeysDisabled() ) {
			Label disabledNote = new Label ( "Hotkeys are currently disabled by system. See the log for more info." );
			globalContent.add( disabledNote, 0, row, 2, 1 );
			disabledNote.setPadding( new Insets ( 0, 0, 20, 0 ) );
			GridPane.setHalignment( disabledNote, HPos.CENTER );
			row++;
		}
		
		if ( Hypnos.getOS() == OS.NIX ) {
			
			Hyperlink consumeHotkeyNote = new Hyperlink ( "Note: On Linux global hotkeys can not be consumed. Click here to read how to address this problem." );

			String url = "http://www.hypnosplayer.org/help/linux-global-hotkey-consume/";
			consumeHotkeyNote.setTooltip( new Tooltip ( url ) );
			
			consumeHotkeyNote.setOnAction(new EventHandler<ActionEvent>() {
			    @Override
			    public void handle(ActionEvent e) {
			    	try {
						new ProcessBuilder("x-www-browser", url ).start();
					} catch ( IOException e1 ) {
						LOGGER.log( Level.INFO, "Unable to open web browser.", e1 );
					}
			    }
			});
			
			consumeHotkeyNote.setPadding( new Insets ( 0, 0, 0, 0 ) );
			consumeHotkeyNote.setWrapText( true );
			consumeHotkeyNote.setTextAlignment( TextAlignment.CENTER );
			globalContent.add( consumeHotkeyNote, 0, row, 2, 1 );
			GridPane.setHalignment( consumeHotkeyNote, HPos.CENTER );
			row++;
			
		}
		
		return hotkeysTab;
	}
	
	public void updateSettings() {
		
		switch ( player.getCurrentList().getDefaultTrackRepeatMode() ) {
			case NO_CHANGE:
				trackRepeatChoices.getSelectionModel().select( 0 );
				break;
			case PLAY_ONCE:
				trackRepeatChoices.getSelectionModel().select( 1 );
				break;
			case REPEAT:
				trackRepeatChoices.getSelectionModel().select( 2 );
				break;
			
		}
		
		switch ( player.getCurrentList().getDefaultAlbumRepeatMode() ) {
			case NO_CHANGE:
				albumRepeatChoices.getSelectionModel().select( 0 );
				break;
			case PLAY_ONCE:
				albumRepeatChoices.getSelectionModel().select( 1 );
				break;
			case REPEAT:
				albumRepeatChoices.getSelectionModel().select( 2 );
				break;
			
		}
		
		switch ( player.getCurrentList().getDefaultPlaylistRepeatMode() ) {
			case NO_CHANGE:
				playlistRepeatChoices.getSelectionModel().select( 0 );
				break;
			case PLAY_ONCE:
				playlistRepeatChoices.getSelectionModel().select( 1 );
				break;
			case REPEAT:
				playlistRepeatChoices.getSelectionModel().select( 2 );
				break;
			
		}
		
		switch ( player.getCurrentList().getDefaultTrackShuffleMode() ) {
			case NO_CHANGE:
				trackShuffleChoices.getSelectionModel().select( 0 );
				break;
			case SEQUENTIAL:
				trackShuffleChoices.getSelectionModel().select( 1 );
				break;
			case SHUFFLE:
				trackShuffleChoices.getSelectionModel().select( 2 );
				break;
			
		}
		
		switch ( player.getCurrentList().getDefaultAlbumShuffleMode() ) {
			case NO_CHANGE:
				albumShuffleChoices.getSelectionModel().select( 0 );
				break;
			case SEQUENTIAL:
				albumShuffleChoices.getSelectionModel().select( 1 );
				break;
			case SHUFFLE:
				albumShuffleChoices.getSelectionModel().select( 2 );
				break;
			
		}
	
		switch ( player.getCurrentList().getDefaultPlaylistShuffleMode() ) {
			case NO_CHANGE:
				playlistShuffleChoices.getSelectionModel().select( 0 );
				break;
			case SEQUENTIAL:
				playlistShuffleChoices.getSelectionModel().select( 1 );
				break;
			case SHUFFLE:
				playlistShuffleChoices.getSelectionModel().select( 2 );
				break;
			
		}
		
		if ( ui.isDarkTheme() ) {
			themeToggleGroup.selectToggle( darkTheme );
		} else {
			themeToggleGroup.selectToggle( lightTheme );
		}
	}
	
	public void refreshHotkeyFields() {
		for ( Hotkey key : Hotkey.values() ) {
			TextField field = hotkeyFields.get( key );
			if ( field == null ) continue;
			
			field.setText( hotkeys.getDisplay( key ) );
		}
	}
	
	private Tab setupSettingsTab ( Pane root, FXUI ui ) {
		
		Tab settingsTab = new Tab ( "Settings" );
		settingsTab.setClosable( false );
		VBox settingsPane = new VBox( 40 );
		settingsTab.setContent ( settingsPane );
		settingsPane.setAlignment( Pos.TOP_CENTER );		
		settingsPane.setPadding( new Insets ( 10 ) );
		
		Label headerLabel = new Label ( "Settings" );
		headerLabel.setPadding( new Insets ( 0, 0, 10, 0 ) );
		headerLabel.setWrapText( true );
		headerLabel.setTextAlignment( TextAlignment.CENTER );
		headerLabel.setStyle( "-fx-font-size: 20px; -fx-font-weight: bold" );
		settingsPane.getChildren().add( headerLabel );

		Insets labelInsets = new Insets ( 10, 10, 10, 10 );
		Insets checkBoxInsets = new Insets ( 10, 10, 10, 10 );
		
		
		Label themeLabel = new Label ( "Theme: " );
		themeLabel.setPadding( labelInsets );
		
		
		lightTheme = new ToggleButton ( "Light" );
		darkTheme = new ToggleButton ( "Dark" );
		
		themeToggleGroup = new ToggleGroup();
		lightTheme.setToggleGroup( themeToggleGroup );
		darkTheme.setToggleGroup( themeToggleGroup );
		
		lightTheme.setPrefWidth( 150 );
		darkTheme.setPrefWidth( 150 );
		
		lightTheme.setSelected( true );
		
		themeToggleGroup.selectedToggleProperty().addListener( new ChangeListener <Toggle>() {
			public void changed ( ObservableValue <? extends Toggle> oldValue, Toggle toggle, Toggle newValue ) {
				if ( newValue == null ) {
					//Do nothing
				} else if ( newValue == lightTheme ) {
					ui.removeDarkTheme();
					
				} else if ( newValue == darkTheme ) {
					ui.applyDarkTheme();
				}
			}
		});
		
		HBox themeBox = new HBox();
		themeBox.setAlignment( Pos.TOP_CENTER );	
		themeBox.getChildren().addAll( themeLabel, lightTheme, darkTheme );
		
		
		Label warnLabel = new Label ( "Warn before erasing unsaved playlists" );
		warnLabel.setPadding( labelInsets );
		
		CheckBox warnCheckBox = new CheckBox ();
		warnCheckBox.setPadding( checkBoxInsets );
		
		warnCheckBox.selectedProperty().bindBidirectional( ui.promptBeforeOverwriteProperty() );
		
		warnCheckBox.selectedProperty().addListener( (obs, wasSelected, isNowSelected) -> {
	    	ui.setPromptBeforeOverwrite( isNowSelected );
		});
				
		HBox warnBox = new HBox();
		warnBox.setAlignment( Pos.TOP_CENTER );
		warnBox.getChildren().addAll( warnCheckBox, warnLabel );
						
		GridPane shuffleGrid = new GridPane();
		shuffleGrid.setPadding( new Insets ( 0, 0, 0, 0 ) );
		shuffleGrid.setHgap( 15 );
		shuffleGrid.setVgap( 5 );
		
		int row = 0;
		
		Label shuffleLabel = new Label ( "Shuffle" );
		GridPane.setHalignment( shuffleLabel, HPos.CENTER );
		shuffleGrid.add( shuffleLabel, 1, row );
		
		Label repeatLabel = new Label ( "Repeat" );
		GridPane.setHalignment( repeatLabel, HPos.CENTER );
		shuffleGrid.add( repeatLabel, 2, row );
		row++;
		
		final ObservableList<String> shuffleOptions = FXCollections.observableArrayList( "No Change", "Sequential", "Shuffle" );
		final ObservableList<String> repeatOptions = FXCollections.observableArrayList( "No Change", "Play Once", "Repeat" );
		
		Label albumsLabel = new Label ( "Default setting for albums:" );
		GridPane.setHalignment( albumsLabel, HPos.RIGHT );
		shuffleGrid.add ( albumsLabel, 0, row );
		
		albumShuffleChoices = new ChoiceBox <String>( shuffleOptions );
		shuffleGrid.add ( albumShuffleChoices, 1, row );
		albumShuffleChoices.getSelectionModel().select( 1 );
		albumShuffleChoices.getSelectionModel().selectedIndexProperty().addListener( new ChangeListener<Number>() {
			@Override
			public void changed ( ObservableValue <? extends Number> observableValue, Number oldValue, Number newValue ) {
				switch ( newValue.intValue() ) {
					case 0: 
						player.getCurrentList().setDefaultAlbumShuffleMode ( DefaultShuffleMode.NO_CHANGE );
						break;
						
					case 1:
						player.getCurrentList().setDefaultAlbumShuffleMode ( DefaultShuffleMode.SEQUENTIAL );
						break;
					
					case 2:
						player.getCurrentList().setDefaultAlbumShuffleMode ( DefaultShuffleMode.SHUFFLE );
						break;
				}
			}
		});
		
		albumRepeatChoices = new ChoiceBox <String>( repeatOptions );
		shuffleGrid.add ( albumRepeatChoices, 2, row );
		albumRepeatChoices.getSelectionModel().select( 1 );
		albumRepeatChoices.getSelectionModel().selectedIndexProperty().addListener( new ChangeListener<Number>() {
			@Override
			public void changed ( ObservableValue <? extends Number> observableValue, Number oldValue, Number newValue ) {
				switch ( newValue.intValue() ) {
					case 0: 
						player.getCurrentList().setDefaultAlbumRepeatMode( DefaultRepeatMode.NO_CHANGE );
						break;
						
					case 1:
						player.getCurrentList().setDefaultAlbumRepeatMode ( DefaultRepeatMode.PLAY_ONCE );
						break;
					
					case 2:
						player.getCurrentList().setDefaultAlbumRepeatMode ( DefaultRepeatMode.REPEAT );
						break;
				}
			}
		});
		row++;
		
		Label trackLabel = new Label ( "Default setting for tracks:" );
		GridPane.setHalignment( trackLabel, HPos.RIGHT );
		shuffleGrid.add ( trackLabel, 0, row );
		
		trackShuffleChoices = new ChoiceBox <String>( shuffleOptions );
		shuffleGrid.add ( trackShuffleChoices, 1, row );
		trackShuffleChoices.getSelectionModel().select( 0 );
		trackShuffleChoices.getSelectionModel().selectedIndexProperty().addListener( new ChangeListener<Number>() {
			@Override
			public void changed ( ObservableValue <? extends Number> observableValue, Number oldValue, Number newValue ) {
				switch ( newValue.intValue() ) {
					case 0: 
						player.getCurrentList().setDefaultTrackShuffleMode( DefaultShuffleMode.NO_CHANGE );
						break;
						
					case 1:
						player.getCurrentList().setDefaultTrackShuffleMode ( DefaultShuffleMode.SEQUENTIAL );
						break;
					
					case 2:
						player.getCurrentList().setDefaultTrackShuffleMode ( DefaultShuffleMode.SHUFFLE );
						break;
				}
			}
		});
		
		trackRepeatChoices = new ChoiceBox <String>( repeatOptions );
		shuffleGrid.add ( trackRepeatChoices, 2, row );
		trackRepeatChoices.getSelectionModel().select( 0 );
		trackRepeatChoices.getSelectionModel().selectedIndexProperty().addListener( new ChangeListener<Number>() {
			@Override
			public void changed ( ObservableValue <? extends Number> observableValue, Number oldValue, Number newValue ) {
				switch ( newValue.intValue() ) {
					case 0: 
						player.getCurrentList().setDefaultTrackRepeatMode( DefaultRepeatMode.NO_CHANGE );
						break;
						
					case 1:
						player.getCurrentList().setDefaultTrackRepeatMode ( DefaultRepeatMode.PLAY_ONCE );
						break;
					
					case 2:
						player.getCurrentList().setDefaultTrackRepeatMode ( DefaultRepeatMode.REPEAT );
						break;
				}
			}
		});
		row++;
		
		Label playlistLabel = new Label ( "Default setting for playlists:" );
		GridPane.setHalignment( playlistLabel, HPos.RIGHT );
		shuffleGrid.add ( playlistLabel, 0, row );
		
		playlistShuffleChoices = new ChoiceBox <String>( shuffleOptions );
		shuffleGrid.add ( playlistShuffleChoices, 1, row );
		playlistShuffleChoices.getSelectionModel().select( 2 );
		playlistShuffleChoices.getSelectionModel().selectedIndexProperty().addListener( new ChangeListener<Number>() {
			@Override
			public void changed ( ObservableValue <? extends Number> observableValue, Number oldValue, Number newValue ) {
				switch ( newValue.intValue() ) {
					case 0: 
						player.getCurrentList().setDefaultPlaylistShuffleMode( DefaultShuffleMode.NO_CHANGE );
						break;
						
					case 1:
						player.getCurrentList().setDefaultPlaylistShuffleMode ( DefaultShuffleMode.SEQUENTIAL );
						break;
					
					case 2:
						player.getCurrentList().setDefaultPlaylistShuffleMode ( DefaultShuffleMode.SHUFFLE );
						break;
				}
			}
		});
		
		playlistRepeatChoices = new ChoiceBox <String>( repeatOptions );
		shuffleGrid.add ( playlistRepeatChoices, 2, row );
		playlistRepeatChoices.getSelectionModel().select( 2 );
		playlistRepeatChoices.getSelectionModel().selectedIndexProperty().addListener( new ChangeListener<Number>() {
			@Override
			public void changed ( ObservableValue <? extends Number> observableValue, Number oldValue, Number newValue ) {
				switch ( newValue.intValue() ) {
					case 0: 
						player.getCurrentList().setDefaultPlaylistRepeatMode( DefaultRepeatMode.NO_CHANGE );
						break;
						
					case 1:
						player.getCurrentList().setDefaultPlaylistRepeatMode ( DefaultRepeatMode.PLAY_ONCE );
						break;
					
					case 2:
						player.getCurrentList().setDefaultPlaylistRepeatMode ( DefaultRepeatMode.REPEAT );
						break;
				}
			}
		});
		row++;
		

		shuffleGrid.setAlignment( Pos.TOP_CENTER );	
		
		settingsPane.getChildren().addAll( shuffleGrid, themeBox, warnBox );
		
		return settingsTab;
	}
	
	private Tab setupLogTab( Pane root ) {
		Tab logTab = new Tab ( "Log" );
		logTab.setClosable( false );
		VBox logPane = new VBox();
		logTab.setContent( logPane );
		logPane.setAlignment( Pos.CENTER );
		logPane.setPadding( new Insets ( 10 ) );
		
		Label headerLabel = new Label ( "Error Log" );
		headerLabel.setPadding( new Insets ( 0, 0, 10, 0 ) );
		headerLabel.setWrapText( true );
		headerLabel.setTextAlignment( TextAlignment.CENTER );
		headerLabel.setStyle( "-fx-font-size: 20px; -fx-font-weight: bold" );
		logPane.getChildren().add( headerLabel );
		
		TextArea logView = new TextArea();
		logView.setEditable( false );
		logView.prefHeightProperty().bind( root.heightProperty() );
		logView.setWrapText( true );
		
		Thread logReader = new Thread( () -> {
			try ( 
				BufferedReader reader = new BufferedReader( new FileReader( Hypnos.getLogFile().toFile() ) ); 
			){
				while ( true ) {
					String line = reader.readLine();
					if ( line == null ) {
						try {
							Thread.sleep( 500 );
						} catch ( InterruptedException ie ) {
						}
					} else {
						Platform.runLater( () -> {
							logView.appendText( line + "\n" );

							if ( line.matches( "^[A-Z]+:.*" ) ) {
								logView.appendText( "\n" );
							}
						});
					}
				}
			} catch ( Exception e ) {
				LOGGER.log( Level.WARNING, "Unable to link log window to log file.", e );
			}
		});
		
		logReader.setDaemon( true );
		logReader.start();
		 
		logPane.getChildren().add( logView );
		
		return logTab;
	}
	
	@SuppressWarnings("unchecked")
	private Tab setupTagTab ( Pane root ) {
		Tab tab = new Tab ( "Tags" );
		tab.setClosable( false );
		VBox pane = new VBox();
		tab.setContent ( pane );
		pane.setAlignment( Pos.TOP_CENTER );
		pane.setPadding( new Insets ( 10 ) );
		
		Label headerLabel = new Label ( "Tag Errors" );
		headerLabel.setPadding( new Insets ( 0, 0, 10, 0 ) );
		headerLabel.setWrapText( true );
		headerLabel.setTextAlignment( TextAlignment.CENTER );
		headerLabel.setStyle( "-fx-font-size: 20px; -fx-font-weight: bold" );
		pane.getChildren().add( headerLabel );
		
		TableColumn<TagError, String> pathColumn = new TableColumn<TagError, String> ( "Location" );
		TableColumn<TagError, String> filenameColumn = new TableColumn<TagError, String> ( "Filename" );
		TableColumn<TagError, String> messageColumn = new TableColumn<TagError, String> ( "Error Message" );

		pathColumn.setCellValueFactory( new PropertyValueFactory <TagError, String>( "FolderDisplay" ) );
		filenameColumn.setCellValueFactory( new PropertyValueFactory <TagError, String>( "FilenameDisplay" ) );
		messageColumn.setCellValueFactory( new PropertyValueFactory <TagError, String>( "Message" ) );

		pathColumn.setMaxWidth( 30000 );
		filenameColumn.setMaxWidth ( 40000 );
		messageColumn.setMaxWidth( 30000 );


		TableView <TagError> table = new TableView <TagError> ();
		table.getColumns().addAll( pathColumn, filenameColumn, messageColumn );
		table.getSortOrder().addAll( pathColumn, messageColumn, filenameColumn );
		table.setPlaceholder( new Label( "There are no tag errors." ) );

		library.getTagErrorsSorted().comparatorProperty().bind( table.comparatorProperty() );
		
		table.setEditable( false );
		table.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		table.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		table.setItems( library.getTagErrorsSorted() );
		table.prefWidthProperty().bind( pane.widthProperty() );
		table.prefHeightProperty().bind( pane.heightProperty() );
		
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
				List <Track> tracks = new ArrayList <Track> ();
				
				for ( TagError error : table.getSelectionModel().getSelectedItems() ) {
					tracks.add( error.getTrack() );
				}
				
				ui.promptAndSavePlaylist ( tracks );
			}
		});

		EventHandler<ActionEvent> addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List <Track> tracks = new ArrayList <Track> ();
				
				for ( TagError error : table.getSelectionModel().getSelectedItems() ) {
					tracks.add( error.getTrack() );
				}
				
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				ui.addToPlaylist ( tracks, playlist );
			}
		};

		library.getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List <Track> tracks = new ArrayList <Track> ();
				
				for ( TagError error : table.getSelectionModel().getSelectedItems() ) {
					tracks.add( error.getTrack() );
				}
				
				
				if ( tracks.size() == 1 ) {
					player.playItems( tracks );
					
				} else if ( tracks.size() > 1 ) {
					if ( ui.okToReplaceCurrentList() ) {
						player.playItems( tracks );
					}
				}
			}
		});

		appendMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List <Track> tracks = new ArrayList <Track> ();
				
				for ( TagError error : table.getSelectionModel().getSelectedItems() ) {
					tracks.add( error.getTrack() );
				}
				
				player.getCurrentList().appendTracks ( tracks );
			}
		});
		
		enqueueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List <Track> tracks = new ArrayList <Track> ();
				
				for ( TagError error : table.getSelectionModel().getSelectedItems() ) {
					tracks.add( error.getTrack() );
				}
				
				player.getQueue().addAllTracks( tracks );
			}
		});
		
		editTagMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List <Track> tracks = new ArrayList <Track> ();
				
				for ( TagError error : table.getSelectionModel().getSelectedItems() ) {
					if ( error != null ) {
						tracks.add( error.getTrack() );
					}
				}
				
				ui.tagWindow.setTracks( tracks, null );
				ui.tagWindow.show();
			}
		});
		
		
		infoMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ui.trackInfoWindow.setTrack( table.getSelectionModel().getSelectedItem().getTrack() );
				ui.trackInfoWindow.show();
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
							TagError selectedTrack = table.getSelectionModel().getSelectedItem();
							if ( selectedTrack != null ) {
								Desktop.getDesktop().open( table.getSelectionModel().getSelectedItem().getPath().getParent().toFile() );
							}
						} catch ( Exception e ) {
							LOGGER.log( Level.INFO, "Unable to open native file browser.", e );
						}
					}
				} );
			}
		});
		
		table.setRowFactory( tv -> {
			TableRow <TagError> row = new TableRow <>();

			row.setContextMenu( trackContextMenu );
			
			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					ui.tagWindow.setTracks( Arrays.asList ( row.getItem().getTrack() ), null );
					ui.tagWindow.show();
				}
			});
			
			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					ArrayList <Integer> indices = new ArrayList <Integer>( table.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( );
					for ( Integer index : indices ) {
						TagError error = table.getItems().get( index );
						if ( error != null ) {
							tracks.add( error.getTrack() );
						}
					}
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, null, null, DragSource.TAG_ERROR_LIST );
					Dragboard db = row.startDragAndDrop( TransferMode.COPY );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( FXUI.DRAGGED_TRACKS, dragObject );
					db.setContent( cc );
					event.consume();
				}
			});
			
			return row;
		});
		
		
		
		
		
		pane.getChildren().addAll( table );
		
		return tab;
	}
	
	private Tab setupAboutTab ( Pane root ) {
		Tab aboutTab = new Tab ( "About" );
		aboutTab.setClosable( false );
		VBox aboutPane = new VBox();
		aboutTab.setContent( aboutPane );
		aboutPane.setStyle( "-fx-background-color: wheat" );
		
		aboutPane.setAlignment( Pos.CENTER );
		Label name = new Label ( "Hypnos Music Player" );
		name.setStyle( "-fx-font-size: 36px; -fx-text-fill: #020202" );
		
		Hyperlink website = new Hyperlink ( "http://www.hypnosplayer.org" );
		website.setTooltip( new Tooltip ( "http://www.hypnosplayer.org" ) );
		website.setOnAction( ( ActionEvent e ) -> {
			try {
				new ProcessBuilder("x-www-browser", "http://hypnosplayer.org" ).start();
			} catch ( IOException e1 ) {
				LOGGER.log( Level.INFO, "Unable to open web browser.", e1 );
			}
		});
		website.setStyle( "-fx-font-size: 20px; -fx-text-fill: #0A95C8" );

		Label versionNumber = new Label ( Hypnos.getVersionString() );
		versionNumber.setStyle( "-fx-font-size: 16px; -fx-text-fill: #020202" );
		versionNumber.setPadding( new Insets ( 0, 0, 20, 0 ) );
		
		Image image = null;
		try {
			image = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) );
		} catch ( Exception e1 ) {
			LOGGER.log( Level.INFO, "Unable to load hypnos icon for settings -> about tab.", e1 );
		}
		ImageView logo = new ImageView( image );
		logo.setFitWidth( 200 );
		logo.setPreserveRatio( true );
		logo.setSmooth( true );
		logo.setCache( true );
        
		HBox authorBox = new HBox();
		authorBox.setAlignment( Pos.CENTER );
		authorBox.setPadding ( new Insets ( 20, 0, 0, 0 ) );
		authorBox.setStyle( "-fx-font-size: 16px; -fx-background-color: transparent;" );
		Label authorLabel = new Label ( "Author:" );
		authorLabel.setStyle( "-fx-text-fill: #020202" );
		Hyperlink authorLink = new Hyperlink ( "Joshua Hartwell" );

		authorLink.setTooltip( new Tooltip ( "http://joshuad.net" ) );
		
		authorLink.setStyle( "-fx-text-fill: #0A95C8" );
		authorLink.setOnAction( ( ActionEvent e ) -> {
			try {
				new ProcessBuilder("x-www-browser", "http://joshuad.net" ).start();
			} catch ( IOException e1 ) {
				LOGGER.log( Level.INFO, "Unable to open web browser.", e1 );
			}
		});
		
		authorBox.getChildren().addAll( authorLabel, authorLink );
		
		
		HBox sourceBox = new HBox();
		sourceBox.setStyle( "-fx-background-color: transparent" );
		sourceBox.setAlignment( Pos.CENTER );
		sourceBox.setStyle( "-fx-background-color: transparent" );
		Label sourceLabel = new Label ( "Source Code:" );
		sourceLabel.setStyle( "-fx-text-fill: #020202" );
		Hyperlink sourceLink = new Hyperlink ( "GitHub" );

		sourceLink.setTooltip( new Tooltip ( "https://github.com/JoshuaD84/HypnosMusicPlayer" ) );
		
		sourceLink.setStyle( "-fx-text-fill: #0A95C8" );
		sourceLink.setOnAction( ( ActionEvent e ) -> {
			try {
				new ProcessBuilder("x-www-browser", "https://github.com/JoshuaD84/HypnosMusicPlayer" ).start();
			} catch ( IOException e1 ) {
				LOGGER.log( Level.INFO, "Unable to open web browser.", e1 );
			}
		});
		

		sourceBox.getChildren().addAll( sourceLabel, sourceLink );
		
		HBox licenseBox = new HBox();
		licenseBox.setStyle( "-fx-background-color: transparent" );
		licenseBox.setAlignment( Pos.CENTER );
		Label licenseLabel = new Label ( "License:" );
		licenseLabel.setStyle( "-fx-text-fill: #020202" );
		Hyperlink licenseLink = new Hyperlink ( "GNU GPLv3" );
		licenseLink.setStyle( "-fx-text-fill: #0A95C8" );
		licenseLink.setTooltip ( new Tooltip ( "https://www.gnu.org/licenses/gpl-3.0-standalone.html" ) );
		licenseLink.setOnAction( ( ActionEvent e ) -> {
			try {
				new ProcessBuilder("x-www-browser", "https://www.gnu.org/licenses/gpl-3.0-standalone.html" ).start();
			} catch ( IOException e1 ) {
				LOGGER.log( Level.INFO, "Unable to open web browser.", e1 );
			}
		});
		
		licenseBox.getChildren().addAll( licenseLabel, licenseLink );
		

		aboutPane.getChildren().addAll ( name, website, versionNumber, logo, authorBox, sourceBox, licenseBox);
		
		return aboutTab;
	}

	public boolean hotkeysDisabledForConfig () {
		if ( tabPane == null ) return false;
		if ( hotkeysTab == null ) return false;
		if ( !this.isShowing() ) return false;
		
		if ( tabPane.getSelectionModel().getSelectedItem().equals( this.hotkeysTab ) )  {
			return true;
		}
		
		return false;
	}

}
