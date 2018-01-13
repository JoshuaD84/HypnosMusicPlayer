package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.ResizeFeatures;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import net.joshuad.hypnos.AlphanumComparator;
import net.joshuad.hypnos.CurrentListState;
import net.joshuad.hypnos.CurrentListTrack;
import net.joshuad.hypnos.CurrentListTrackState;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Library;
import net.joshuad.hypnos.Persister;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.AlphanumComparator.CaseHandling;
import net.joshuad.hypnos.CurrentList.DefaultSortMode;
import net.joshuad.hypnos.Persister.Setting;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.AudioSystem.RepeatMode;
import net.joshuad.hypnos.audio.AudioSystem.ShuffleMode;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;

public class CurrentListPane extends BorderPane {
	private static final Logger LOGGER = Logger.getLogger( CurrentListPane.class.getName() );

	HBox currentListControls; 

	public TableView <CurrentListTrack> currentListTable; //TODO: Make private
	TableColumn playingColumn, artistColumn, yearColumn, albumColumn, titleColumn, numberColumn, lengthColumn;
	ContextMenu currentListColumnSelectorMenu;
	
	Button toggleRepeatButton, toggleShuffleButton;
	Button showQueueButton;
	MenuItem saveMenuItem, exportToM3U, exportToFolder, loadMenuItem, historyMenuItem;
	MenuItem playMenuItem, playNextMenuItem, queueMenuItem, editTagMenuItem, infoMenuItem;
	MenuItem lyricsMenuItem, cropMenuItem, removeMenuItem, browseMenuItem, addToPlaylistMenuItem;
	
	ImageView noRepeatImage, repeatImage, repeatOneImage, sequentialImage, shuffleImage;
	ImageView queueImage, historyImage, menuImage;

	Image repeatImageSource;

	ThrottledTrackFilter currentListTableFilter;
	
	InfoFilterHybrid infoLabelAndFilter;
	
	FXUI ui;
	AudioSystem audioSystem;
	Library library;
	
	public CurrentListPane( FXUI ui, AudioSystem audioSystem, Library library ) {
		this.ui = ui;
		this.audioSystem = audioSystem;
		this.library = library;
		
		loadImages();
		setupTable();
		resetTableSettingsToDefault();
		setupControlPane();
		
		this.setTop( currentListControls );
		this.setCenter( currentListTable );
		this.setMinWidth( 0 );
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
	}
	
	@SuppressWarnings("unchecked")
	public void resetTableSettingsToDefault() {
		playingColumn.setVisible( true );
		artistColumn.setVisible( true );
		yearColumn.setVisible( true );
		albumColumn.setVisible( true );
		titleColumn.setVisible( true );
		numberColumn.setVisible( true );
		lengthColumn.setVisible( true );
		
		currentListTable.getColumns().remove( playingColumn );
		currentListTable.getColumns().add( playingColumn );
		currentListTable.getColumns().remove( numberColumn );
		currentListTable.getColumns().add( numberColumn );
		currentListTable.getColumns().remove( artistColumn );
		currentListTable.getColumns().add( artistColumn );
		currentListTable.getColumns().remove( yearColumn );
		currentListTable.getColumns().add( yearColumn );
		currentListTable.getColumns().remove( albumColumn );
		currentListTable.getColumns().add( albumColumn );
		currentListTable.getColumns().remove( titleColumn );
		currentListTable.getColumns().add( titleColumn );
		currentListTable.getColumns().remove( lengthColumn );
		currentListTable.getColumns().add( lengthColumn );

		setSortMode( audioSystem.getCurrentList().getCurrentDefaultSortMode() );

		artistColumn.setPrefWidth( 100 );
		numberColumn.setPrefWidth( 40 );
		yearColumn.setPrefWidth( 60 );
		albumColumn.setPrefWidth( 100 );
		titleColumn.setPrefWidth( 100 );
		lengthColumn.setPrefWidth( 70 );
		
		((HypnosResizePolicy)currentListTable.getColumnResizePolicy()).call(
			new ResizeFeatures (  currentListTable, null, (double)0 ) 
		);
	}
	
	private void setupControlPane () {
		
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
				ui.queueWindow.show();
			}
		});
		
		audioSystem.getQueue().getData().addListener( new ListChangeListener () {
			@Override
			public void onChanged ( Change arg0 ) {
				if ( audioSystem.getQueue().isEmpty() ) {
					showQueueButton.getStyleClass().removeAll ( "queueActive" );
				} else {
					showQueueButton.getStyleClass().add ( "queueActive" );
				}
			} 
		});

		toggleRepeatButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				audioSystem.toggleRepeatMode();
				updateRepeatButtonImages();
			}
		});

		toggleShuffleButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				audioSystem.toggleShuffleMode();
				updateShuffleButtonImages();
			}
		});

		EventHandler savePlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				String playlistName = ((Playlist) ((MenuItem) event.getSource()).getUserData()).getName();
				Playlist playlist = new Playlist( playlistName, new ArrayList <Track>( audioSystem.getCurrentList().getItems() ) );
				library.addPlaylist( playlist );
			}
		};

		currentListControls = new HBox();
		currentListControls.setAlignment( Pos.CENTER_RIGHT );
		currentListControls.setId( "currentListControls" );
		
		final Label currentListLength = new Label ( "" );
		currentListLength.setMinWidth( Region.USE_PREF_SIZE );
		currentListLength.setPadding( new Insets ( 0, 10, 0, 10 ) );
		
		audioSystem.getCurrentList().getItems().addListener( new ListChangeListener () {
			@Override
			public void onChanged ( Change changes ) {
				int lengthS = 0;
				
				for ( Track track : audioSystem.getCurrentList().getItems() ) {
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
					for ( Track track : audioSystem.getCurrentList().getItems() ) {
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
						if ( index >= 0 && index < audioSystem.getCurrentList().getItems().size() ) {
							lengthS += audioSystem.getCurrentList().getItems().get( index ).getLengthS();
						}
					}
					
					final int lengthArgument = lengthS;
					
					Platform.runLater( () -> {
						currentListLength.setText( Utils.getLengthDisplay( lengthArgument ) );
					});
					
				}
					
				
			}
		});
		
		infoLabelAndFilter = new InfoFilterHybrid ( "" );
		infoLabelAndFilter.prefWidthProperty().bind( currentListControls.widthProperty() );
		infoLabelAndFilter.setFilteredTable( currentListTable );
		
		infoLabelAndFilter.getFilter().setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ENTER
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				audioSystem.playTrack( currentListTable.getSelectionModel().getSelectedItem() );
				
			} else if ( e.getCode() == KeyCode.F2
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				editTagMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.F3
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				infoMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.F4
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				browseMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.DOWN
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				e.consume();
				currentListTable.requestFocus();
				currentListTable.getSelectionModel().select( currentListTable.getSelectionModel().getFocusedIndex() );
			}
		});
		
		currentListTableFilter = new ThrottledTrackFilter ( audioSystem.getCurrentList().getFilteredItems() );
		
		infoLabelAndFilter.textProperty().addListener( new ChangeListener <String> () {
			@Override
			public void changed ( ObservableValue <? extends String> observable, String oldValue, String newValue ) {
				Platform.runLater( () -> {
					currentListTableFilter.setFilter( newValue, false );
				});
			}
		});
		
		audioSystem.getCurrentList().addListener( ( CurrentListState currentState ) -> {  
			Platform.runLater( () -> {
				infoLabelAndFilter.setInfoText( currentState.getDisplayString() );
			});
		});
		
		final ContextMenu queueButtonMenu = new ContextMenu();
		MenuItem clearQueue = new MenuItem ( "Clear Queue" );
		MenuItem replaceWithQueue = new MenuItem ( "Replace list with queue" );
		MenuItem dumpQueueBefore = new MenuItem ( "Prepend to list" );
		MenuItem dumpQueueAfter = new MenuItem ( "Append to list" );
		queueButtonMenu.getItems().addAll( clearQueue, replaceWithQueue, dumpQueueBefore, dumpQueueAfter );
		showQueueButton.setContextMenu( queueButtonMenu );
		
		clearQueue.setOnAction(  ( ActionEvent e ) -> { audioSystem.getQueue().clear(); });
		
		replaceWithQueue.setOnAction( ( ActionEvent e ) -> { 
			audioSystem.getCurrentList().clearList();
			audioSystem.getCurrentList().appendTracks ( audioSystem.getQueue().getData() );
			audioSystem.getQueue().clear(); 
		});
		
		dumpQueueAfter.setOnAction( ( ActionEvent e ) -> { 
			audioSystem.getCurrentList().appendTracks ( audioSystem.getQueue().getData() );
			audioSystem.getQueue().clear(); 
		});
		
		dumpQueueBefore.setOnAction( ( ActionEvent e ) -> { 
			audioSystem.getCurrentList().insertTracks( 0, audioSystem.getQueue().getData() );
			audioSystem.getQueue().clear(); 
		});
		
		final ContextMenu shuffleButtonMenu = new ContextMenu();
		toggleShuffleButton.setContextMenu( shuffleButtonMenu );
		
		MenuItem sequential = new MenuItem ( "Sequential" );
		MenuItem shuffle = new MenuItem ( "Shuffle" );
		MenuItem shuffleList = new MenuItem ( "Randomize List Order" );
		shuffleButtonMenu.getItems().addAll( sequential, shuffle, shuffleList );
		
		sequential.setOnAction( ( actionEvent ) -> { audioSystem.setShuffleMode( ShuffleMode.SEQUENTIAL ); });
		shuffle.setOnAction( ( actionEvent ) -> { audioSystem.setShuffleMode( ShuffleMode.SHUFFLE ); });
		shuffleList.setOnAction( ( actionEvent ) -> { audioSystem.shuffleList(); });
		
		final ContextMenu repeatButtonMenu = new ContextMenu();
		toggleRepeatButton.setContextMenu( repeatButtonMenu );
		MenuItem noRepeat = new MenuItem ( "No Repeat" );
		MenuItem repeatAll = new MenuItem ( "Repeat All" );
		MenuItem repeatOne = new MenuItem ( "Repeat One Track" );
		repeatButtonMenu.getItems().addAll( noRepeat, repeatAll, repeatOne );
		
		noRepeat.setOnAction( ( actionEvent ) -> { audioSystem.setRepeatMode( RepeatMode.PLAY_ONCE ); });
		repeatAll.setOnAction( ( actionEvent ) -> { audioSystem.setRepeatMode( RepeatMode.REPEAT ); });
		repeatOne.setOnAction( ( actionEvent ) -> { audioSystem.setRepeatMode( RepeatMode.REPEAT_ONE_TRACK ); });
		
		MenuButton currentListMenu = new MenuButton ( "" );
		currentListMenu.setTooltip ( new Tooltip ( "Current List Controls" ) );
		currentListMenu.setGraphic ( menuImage );
		MenuItem currentListClear = new MenuItem ( "Clear" );
		saveMenuItem = new MenuItem ( "Save" );
		exportToM3U = new MenuItem ( "Export as M3U" );
		exportToFolder = new MenuItem ( "Export as Folder" );
		loadMenuItem = new MenuItem ( "Load Files" );
		historyMenuItem = new MenuItem ( "History" );
		MenuItem currentListShuffle = new MenuItem ( "Shuffle" );
		MenuItem searchMenuItem = new MenuItem ( "Search" );
		
		currentListClear.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				audioSystem.getCurrentList().clearList();
			}
		});

		historyMenuItem.setOnAction ( new EventHandler <ActionEvent>() {
			public void handle ( ActionEvent e ) {
				ui.historyWindow.show();
			}
		});
		
		saveMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				ui.promptAndSavePlaylist( new ArrayList <Track>( audioSystem.getCurrentList().getItems() ) );
			}
		});
		
		currentListShuffle.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List<Integer> selectedIndices = currentListTable.getSelectionModel().getSelectedIndices();
				
				if ( selectedIndices.size() < 2 ) {
					audioSystem.shuffleList();
					
				} else {
					audioSystem.getCurrentList().shuffleItems( selectedIndices );
				}
			}
		});
		
		loadMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				FileChooser fileChooser = new FileChooser();
				
				ArrayList <String> filters = new ArrayList <String> ();
				
				for ( String ending : Utils.musicExtensions ) {
					filters.add( "*." + ending );
				}
				
				FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter( "Audio Files", filters );
				fileChooser.getExtensionFilters().add( fileExtensions );
				List <File> selectedFiles = fileChooser.showOpenMultipleDialog( ui.mainStage );
				
				if ( selectedFiles == null ) {
					return;
				}
				
				ArrayList <Path> paths = new ArrayList <Path> ();
				for ( File file : selectedFiles ) {
					paths.add( file.toPath() );
				}
				
				audioSystem.getCurrentList().setTracksPathList( paths );
					
			}
		});
		
		exportToM3U.setOnAction( ( ActionEvent e ) -> {
			File targetFile = ui.promptUserForPlaylistFile();
			if ( targetFile == null ) {
				return;
			}
			
			CurrentListState state = audioSystem.getCurrentList().getState();
			
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
				ui.alertUser ( AlertType.ERROR, "Warning", "Unable to save playlist.", "Unable to save the playlist to the specified location", 400 );
			}
		});
		
		exportToFolder.setOnAction( ( ActionEvent e ) -> {
			File targetFile = ui.promptUserForFolder();
			if ( targetFile == null ) {
				return;
			}
			
			List<CurrentListTrack> tracks = audioSystem.getCurrentList().getSortedItemsNoFilter();
			
			//TODO: Get rid of Hypnos.get
			Hypnos.getPersister().exportTracksToFolder ( tracks, targetFile.toPath() );
		});
		
		searchMenuItem.setOnAction( ( ActionEvent e ) -> {
			infoLabelAndFilter.beginEditing();
		});
		
		currentListMenu.getItems().addAll ( currentListClear, currentListShuffle, searchMenuItem, 
			exportToM3U, exportToFolder, saveMenuItem, loadMenuItem, historyMenuItem );
		
		currentListControls.getChildren().addAll( toggleShuffleButton, toggleRepeatButton, showQueueButton,
				infoLabelAndFilter, currentListLength, currentListMenu );

		currentListControls.prefWidthProperty().bind( this.widthProperty() );
	}
	
	private void setupTable () {
		playingColumn = new TableColumn( "" );
		artistColumn = new TableColumn( "Artist" );
		yearColumn = new TableColumn( "Year" );
		albumColumn = new TableColumn( "Album" );
		titleColumn = new TableColumn( "Title" );
		numberColumn = new TableColumn( "#" );
		lengthColumn = new TableColumn( "Length" );
		
		albumColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		artistColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		titleColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		
		playingColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, CurrentListTrackState>( "displayState" ) );
		artistColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "artist" ) );
		yearColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, Integer>( "year" ) );
		albumColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "fullAlbumTitle" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "title" ) );
		numberColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, Integer>( "trackNumber" ) );
		lengthColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "lengthDisplay" ) );
		
		albumColumn.setCellFactory( e -> new FormattedAlbumCell() );

		playingColumn.setCellFactory ( column -> { 
				return new CurrentListTrackStateCell( ui, ui.transport.playImageSource, ui.transport.pauseImageSource ); 
			} 
		);
		
		numberColumn.setCellFactory( column -> {
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
		MenuItem resetMenuItem = new MenuItem ( "Reset To Default View" );
		playingMenuItem.setSelected( true );
		artistMenuItem.setSelected( true );
		yearMenuItem.setSelected( true );
		albumMenuItem.setSelected( true );
		numberMenuItem.setSelected( true );
		titleMenuItem.setSelected( true );
		lengthMenuItem.setSelected( true );
		currentListColumnSelectorMenu.getItems().addAll( playingMenuItem, numberMenuItem,artistMenuItem, 
			yearMenuItem, albumMenuItem,  titleMenuItem, lengthMenuItem, resetMenuItem );
		playingColumn.setContextMenu( currentListColumnSelectorMenu );
		artistColumn.setContextMenu( currentListColumnSelectorMenu );
		yearColumn.setContextMenu( currentListColumnSelectorMenu );
		albumColumn.setContextMenu( currentListColumnSelectorMenu );
		titleColumn.setContextMenu( currentListColumnSelectorMenu );
		numberColumn.setContextMenu( currentListColumnSelectorMenu );
		lengthColumn.setContextMenu( currentListColumnSelectorMenu );
		playingMenuItem.selectedProperty().bindBidirectional( playingColumn.visibleProperty() );
		artistMenuItem.selectedProperty().bindBidirectional( artistColumn.visibleProperty() );
		yearMenuItem.selectedProperty().bindBidirectional( yearColumn.visibleProperty() );
		albumMenuItem.selectedProperty().bindBidirectional( albumColumn.visibleProperty() );
		numberMenuItem.selectedProperty().bindBidirectional( numberColumn.visibleProperty() );
		titleMenuItem.selectedProperty().bindBidirectional( titleColumn.visibleProperty() );
		lengthMenuItem.selectedProperty().bindBidirectional( lengthColumn.visibleProperty() );
		
		currentListTable = new TableView();
		currentListTable.getColumns().addAll( playingColumn, numberColumn, artistColumn,
			yearColumn, albumColumn, titleColumn, lengthColumn );
		currentListTable.setEditable( false );
		currentListTable.setItems( audioSystem.getCurrentList().getSortedItems() );
		audioSystem.getCurrentList().getSortedItems().comparatorProperty().bind( currentListTable.comparatorProperty() );
		
		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		currentListTable.setColumnResizePolicy( resizePolicy );
		
		currentListTable.setPlaceholder( new Label( "No tracks in playlist." ) );
		currentListTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		
		playingColumn.setMaxWidth( 38 );
		playingColumn.setMinWidth( 38 );
		playingColumn.setResizable( false );
		
		resizePolicy.registerFixedWidthColumns( yearColumn, numberColumn, lengthColumn );
		
		currentListTable.setOnDragOver( event -> {
			
			Dragboard db = event.getDragboard();
			
			if ( db.hasContent( FXUI.DRAGGED_TRACKS ) || db.hasFiles() ) {
				event.acceptTransferModes( TransferMode.COPY );
				event.consume();
			}
		});

		currentListTable.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();

			if ( db.hasContent( FXUI.DRAGGED_TRACKS ) ) {
				//REFACTOR: This code is duplicated below. Put it in a function. 

				DraggedTrackContainer container = (DraggedTrackContainer) db.getContent( FXUI.DRAGGED_TRACKS );
				
				switch ( container.getSource() ) {
					case TRACK_LIST:
					case ALBUM_INFO:
					case PLAYLIST_INFO:
					case TAG_ERROR_LIST:
					case HISTORY: {
						List <Track> tracks = container.getTracks();
						if ( tracks.size() > 0 ) {
							audioSystem.getCurrentList().setItemsToSortedOrder();
							currentListTable.getSortOrder().clear();
							audioSystem.getCurrentList().appendTracks( tracks );
						}
					
					} break;

					case PLAYLIST_LIST: {
						audioSystem.getCurrentList().setItemsToSortedOrder();
						currentListTable.getSortOrder().clear();
						audioSystem.getCurrentList().appendPlaylists( container.getPlaylists() );
						
					} break;
					
					case ALBUM_LIST: {
						audioSystem.getCurrentList().setItemsToSortedOrder();
						currentListTable.getSortOrder().clear();
						audioSystem.getCurrentList().appendAlbums ( container.getAlbums() );
					} break;
					
					case CURRENT_LIST: {
						//There is no meaning in dragging from an empty list to an empty list. 
					} break;
					
					case QUEUE: {
						synchronized ( audioSystem.getQueue().getData() ) {
							List <Integer> draggedIndices = container.getIndices();
							
							ArrayList <Track> tracksToCopy = new ArrayList <Track> ( draggedIndices.size() );
							for ( int index : draggedIndices ) {
								if ( index >= 0 && index < audioSystem.getQueue().getData().size() ) {
									Track addMe = audioSystem.getQueue().getData().get( index );
									if ( addMe instanceof CurrentListTrack ) {
										tracksToCopy.add( (CurrentListTrack)addMe );
									} else {
										CurrentListTrack newAddMe = new CurrentListTrack ( addMe );
										audioSystem.getQueue().getData().remove ( index );
										audioSystem.getQueue().getData().add( index, newAddMe );
										tracksToCopy.add( newAddMe );
									}
								}
							}

							audioSystem.getCurrentList().setItemsToSortedOrder();
							currentListTable.getSortOrder().clear();
							audioSystem.getCurrentList().appendTracks( tracksToCopy );
						}
						
					} break;
				}

				audioSystem.getQueue().updateQueueIndexes();
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
					audioSystem.getCurrentList().setItemsToSortedOrder();
					currentListTable.getSortOrder().clear();
					audioSystem.getCurrentList().insertTrackPathList ( 0, tracksToAdd );
				}

				event.setDropCompleted( true );
				event.consume();
			} 
		});

		ContextMenu contextMenu = new ContextMenu();
		playMenuItem = new MenuItem( "Play" );
		playNextMenuItem = new MenuItem( "Play Next" );
		queueMenuItem = new MenuItem( "Enqueue" );
		editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		infoMenuItem = new MenuItem( "Info" );
		lyricsMenuItem = new MenuItem( "Lyrics" );
		cropMenuItem = new MenuItem( "Crop" );
		removeMenuItem = new MenuItem( "Remove" );
		browseMenuItem = new MenuItem( "Browse Folder" );
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
			} else if ( e.getCode() == KeyCode.UP
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				if ( currentListTable.getSelectionModel().getSelectedIndex() == 0 
				&& infoLabelAndFilter.isFilterMode() ) {
					infoLabelAndFilter.beginEditing();
					e.consume();
				}
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
				ui.promptAndSavePlaylist ( new ArrayList <Track> ( currentListTable.getSelectionModel().getSelectedItems() ) );
			}
		});
		
		resetMenuItem.setOnAction ( ( e ) -> this.resetTableSettingsToDefault() );

		EventHandler<ActionEvent> addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				ui.addToPlaylist ( Utils.convertCurrentTrackList ( currentListTable.getSelectionModel().getSelectedItems() ), playlist );
			}
		};

		library.getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );

		
		playNextMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				audioSystem.getQueue().queueAllTracks( currentListTable.getSelectionModel().getSelectedItems(), 0 );
			}
		});
		
		queueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				audioSystem.getQueue().queueAllTracks( currentListTable.getSelectionModel().getSelectedItems() );
			}
		});
		
		editTagMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ui.tagWindow.setTracks( (List<Track>)(List<?>)currentListTable.getSelectionModel().getSelectedItems(), null );
				ui.tagWindow.show();
			}
		});
		
		infoMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ui.trackInfoWindow.setTrack( currentListTable.getSelectionModel().getSelectedItem() );
				ui.trackInfoWindow.show();
			}
		});
		
		lyricsMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ui.lyricsWindow.setTrack( currentListTable.getSelectionModel().getSelectedItem() );
				ui.lyricsWindow.show();
			}
		});

		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				audioSystem.playTrack( currentListTable.getSelectionModel().getSelectedItem() );
			}
		} );

		browseMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ui.openFileBrowser ( currentListTable.getSelectionModel().getSelectedItem().getPath() );
			}
		});
		
		removeMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ObservableList <Integer> selectedIndexes = currentListTable.getSelectionModel().getSelectedIndices();
				List <Integer> removeMe = new ArrayList<> ( selectedIndexes );
				int selectAfterDelete = selectedIndexes.get( 0 ) - 1;
				currentListTable.getSelectionModel().clearSelection();
				ui.removeFromCurrentList ( removeMe );
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
				
				ui.removeFromCurrentList ( removeMe );
				currentListTable.getSelectionModel().clearSelection();
			}
		});
		
		currentListTable.getSelectionModel().selectedItemProperty().addListener( ( obs, oldSelection, newSelection ) -> {
			ui.artSplitPane.setImages ( newSelection );
		});
		
		currentListTable.setRowFactory( tv -> {
			TableRow <CurrentListTrack> row = new TableRow <>();
			

			row.setContextMenu( contextMenu );
			
			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && !row.isEmpty() ) {
					audioSystem.playTrack( row.getItem() );
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
					cc.put( FXUI.DRAGGED_TRACKS, dragObject );
					db.setContent( cc );
					event.consume();
				}
			});

			row.setOnDragOver( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasContent( FXUI.DRAGGED_TRACKS ) || db.hasFiles() ) {
					event.acceptTransferModes( TransferMode.COPY );
					event.consume();
				}
			});

			row.setOnDragDropped( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasContent( FXUI.DRAGGED_TRACKS ) ) {

					DraggedTrackContainer container = (DraggedTrackContainer) db.getContent( FXUI.DRAGGED_TRACKS );
					int dropIndex = row.getIndex();
					
					switch ( container.getSource() ) {
						case ALBUM_LIST: {
							audioSystem.getCurrentList().setItemsToSortedOrder();
							currentListTable.getSortOrder().clear();
							audioSystem.getCurrentList().insertAlbums( dropIndex, container.getAlbums() );
						} break;
						
						case PLAYLIST_LIST:
						case TRACK_LIST:
						case ALBUM_INFO:
						case PLAYLIST_INFO:
						case TAG_ERROR_LIST:
						case HISTORY: {
							audioSystem.getCurrentList().setItemsToSortedOrder();
							currentListTable.getSortOrder().clear();
							audioSystem.getCurrentList().insertTracks( dropIndex, Utils.convertTrackList( container.getTracks() ) );
						} break;
						
						case CURRENT_LIST: {
							audioSystem.getCurrentList().setItemsToSortedOrder();
							currentListTable.getSortOrder().clear();
							List<Integer> draggedIndices = container.getIndices();
							
							audioSystem.getCurrentList().moveTracks ( draggedIndices, dropIndex );
							
							currentListTable.getSelectionModel().clearSelection();
							for ( int k = 0; k < draggedIndices.size(); k++ ) {
								int selectIndex = dropIndex + k;
								if ( selectIndex < currentListTable.getItems().size() ) {
									currentListTable.getSelectionModel().select( dropIndex + k );
								}
							}
						} break;
						
						case QUEUE: {
							synchronized ( audioSystem.getQueue().getData() ) {
								List <Integer> draggedIndices = container.getIndices();
								
								ArrayList <CurrentListTrack> tracksToCopy = new ArrayList <CurrentListTrack> ( draggedIndices.size() );
								for ( int index : draggedIndices ) {
									if ( index >= 0 && index < audioSystem.getQueue().getData().size() ) {
										Track addMe = audioSystem.getQueue().getData().get( index );
										if ( addMe instanceof CurrentListTrack ) {
											tracksToCopy.add( (CurrentListTrack)addMe );
										} else {
											CurrentListTrack newAddMe = new CurrentListTrack ( addMe );
											audioSystem.getQueue().getData().remove ( index );
											audioSystem.getQueue().getData().add( index, newAddMe );
											tracksToCopy.add( newAddMe );
										}
									}
								}
								audioSystem.getCurrentList().setItemsToSortedOrder();
								currentListTable.getSortOrder().clear();
								audioSystem.getCurrentList().insertTracks ( dropIndex, tracksToCopy );
							}
							
						} break;
					}

					audioSystem.getQueue().updateQueueIndexes( );
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
						audioSystem.getCurrentList().setItemsToSortedOrder();
						currentListTable.getSortOrder().clear();
						int dropIndex = row.isEmpty() ? dropIndex = currentListTable.getItems().size() : row.getIndex();
						audioSystem.getCurrentList().insertTrackPathList ( dropIndex, tracksToAdd );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});

			return row;
		});
	}
	
	void updateShuffleButtonImages() {
		switch ( audioSystem.getShuffleMode() ) {
			
			case SHUFFLE:
				toggleShuffleButton.setGraphic( shuffleImage );
				break;
				
			case SEQUENTIAL: //Fall through
			default:
				toggleShuffleButton.setGraphic( sequentialImage );
				
				break;
			
		}
	}
	
	void updateRepeatButtonImages() {
		switch ( audioSystem.getRepeatMode() ) {
			
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

	public void applyDarkTheme ( ColorAdjust darkThemeButtons ) {
		if ( noRepeatImage != null ) noRepeatImage.setEffect( darkThemeButtons );
		if ( repeatImage != null ) repeatImage.setEffect( darkThemeButtons );
		if ( repeatOneImage != null ) repeatOneImage.setEffect( darkThemeButtons );
		if ( sequentialImage != null ) sequentialImage.setEffect( darkThemeButtons );
		if ( shuffleImage != null ) shuffleImage.setEffect( darkThemeButtons );
		if ( menuImage != null ) menuImage.setEffect( darkThemeButtons );
		if ( queueImage != null ) queueImage.setEffect( darkThemeButtons );
		if ( historyImage != null ) historyImage.setEffect( darkThemeButtons );
		
		currentListTable.refresh();
	}

	public void removeDarkTheme () {
		if ( noRepeatImage != null ) noRepeatImage.setEffect( null );
		if ( repeatImage != null ) repeatImage.setEffect( null );
		if ( repeatOneImage != null ) repeatOneImage.setEffect( null );
		if ( sequentialImage != null ) sequentialImage.setEffect( null );
		if ( shuffleImage != null ) shuffleImage.setEffect( null );
		if ( menuImage != null ) menuImage.setEffect( null );
		if ( queueImage != null ) queueImage.setEffect( null );
		if ( historyImage != null ) historyImage.setEffect( null );
		
		currentListTable.refresh();
	}
	
	@SuppressWarnings("incomplete-switch")
	public void applySettingsBeforeWindowShown( EnumMap<Persister.Setting, String> settings ) {
		settings.forEach( ( setting, value )-> {
			try {
				switch ( setting ) {
					case CL_TABLE_PLAYING_COLUMN_SHOW:
						playingColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case CL_TABLE_NUMBER_COLUMN_SHOW:
						numberColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case CL_TABLE_ARTIST_COLUMN_SHOW:
						artistColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case CL_TABLE_YEAR_COLUMN_SHOW:
						yearColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case CL_TABLE_ALBUM_COLUMN_SHOW:
						albumColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case CL_TABLE_TITLE_COLUMN_SHOW:
						titleColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case CL_TABLE_LENGTH_COLUMN_SHOW:
						lengthColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;	
					case CL_SORT_ORDER:
						currentListTable.getSortOrder().clear();
						
						if ( !value.equals( "" ) ) {
							String[] order = value.split( " " );
							for ( String fullValue : order ) {
								try {
									String columnName = fullValue.split( "-" )[0];
									SortType sortType = SortType.valueOf( fullValue.split( "-" )[1] );
									
									if ( columnName.equals( "playing" ) ) {
										currentListTable.getSortOrder().add( playingColumn );
										playingColumn.setSortType( sortType );
									} else if ( columnName.equals( "artist" ) ) {
										currentListTable.getSortOrder().add( artistColumn );
										artistColumn.setSortType( sortType );
									} else if ( columnName.equals( "year" ) ) {
										currentListTable.getSortOrder().add( yearColumn );
										yearColumn.setSortType( sortType );
									} else if ( columnName.equals( "album" ) ) {
										currentListTable.getSortOrder().add( albumColumn );
										albumColumn.setSortType( sortType );
									} else if ( columnName.equals( "title" ) ) {
										currentListTable.getSortOrder().add( titleColumn );
										titleColumn.setSortType( sortType );
									} else if ( columnName.equals( "number" ) ) {
										currentListTable.getSortOrder().add( numberColumn );
										numberColumn.setSortType( sortType );
									} else if ( columnName.equals( "length" ) ) {
										currentListTable.getSortOrder().add( lengthColumn );
										lengthColumn.setSortType( sortType );
									} 
									
								} catch ( Exception e ) {
									LOGGER.log( Level.INFO, "Unable to set album table sort order: '" + value + "'", e );
								}
							}
						}
						settings.remove ( setting );
						break;
					case CL_COLUMN_ORDER: {
						String[] order = value.split( " " );
						int newIndex = 0;
						
						for ( String columnName : order ) {
							try {
								if ( columnName.equals( "playing" ) ) {
									currentListTable.getColumns().remove( playingColumn );
									currentListTable.getColumns().add( newIndex, playingColumn );
								} else if ( columnName.equals( "artist" ) ) {
									currentListTable.getColumns().remove( artistColumn );
									currentListTable.getColumns().add( newIndex, artistColumn );
								} else if ( columnName.equals( "year" ) ) {
									currentListTable.getColumns().remove( yearColumn );
									currentListTable.getColumns().add( newIndex, yearColumn );
								} else if ( columnName.equals( "album" ) ) {
									currentListTable.getColumns().remove( albumColumn );
									currentListTable.getColumns().add( newIndex, albumColumn );
								} else if ( columnName.equals( "title" ) ) {
									currentListTable.getColumns().remove( titleColumn );
									currentListTable.getColumns().add( newIndex, titleColumn );
								} else if ( columnName.equals( "number" ) ) {
									currentListTable.getColumns().remove( numberColumn );
									currentListTable.getColumns().add( newIndex, numberColumn );
								} else if ( columnName.equals( "length" ) ) {
									currentListTable.getColumns().remove( lengthColumn );
									currentListTable.getColumns().add( newIndex, lengthColumn );
								} 
								newIndex++;
							} catch ( Exception e ) {
								LOGGER.log( Level.INFO, "Unable to set album table column order: '" + value + "'", e );
							}
							
						}
						settings.remove ( setting );
						break;
					}
				}
				
				
			} catch ( Exception e ) {
				LOGGER.log( Level.INFO, "Unable to apply setting: " + setting + " to current list.", e );
			}
		});
	}
	
	public EnumMap<Persister.Setting, ? extends Object> getSettings () {

		EnumMap <Persister.Setting, Object> retMe = new EnumMap <Persister.Setting, Object> ( Persister.Setting.class );
		
		List <String> albumSortOrder = new ArrayList<> ();
		
		String sortOrderValue = "";
		
		for ( TableColumn<CurrentListTrack, ?> column : currentListTable.getSortOrder() ) {
			if ( column == playingColumn ) {
				sortOrderValue += "playing-" + playingColumn.getSortType() + " ";
			} else if ( column == artistColumn ) {
				sortOrderValue += "artist-" + artistColumn.getSortType() + " ";
			} else if ( column == yearColumn ) {
				sortOrderValue += "year-" + yearColumn.getSortType() + " ";
			} else if ( column == albumColumn ) {
				sortOrderValue += "album-" + albumColumn.getSortType() + " ";
			} else if ( column == titleColumn ) {
				sortOrderValue += "title-" + titleColumn.getSortType() + " ";
			} else if ( column == numberColumn ) {
				sortOrderValue += "number-" + numberColumn.getSortType() + " ";
			} else if ( column == lengthColumn ) {
				sortOrderValue += "length-" + lengthColumn.getSortType() + " ";
			} 
		}
		retMe.put ( Setting.CL_SORT_ORDER, sortOrderValue );
		
		String columnOrderValue = "";
		for ( TableColumn<CurrentListTrack, ?> column : currentListTable.getColumns() ) {
			if ( column == playingColumn ) {
				columnOrderValue += "playing ";
			} else if ( column == artistColumn ) {
				columnOrderValue += "artist ";
			} else if ( column == yearColumn ) {
				columnOrderValue += "year ";
			} else if ( column == albumColumn ) {
				columnOrderValue += "album ";
			} else if ( column == titleColumn ) {
				columnOrderValue += "title ";
			} else if ( column == numberColumn ) {
				columnOrderValue += "number ";
			} else if ( column == lengthColumn ) {
				columnOrderValue += "length ";
			}
		}
		retMe.put ( Setting.CL_COLUMN_ORDER, columnOrderValue );
		
		retMe.put ( Setting.CL_TABLE_PLAYING_COLUMN_SHOW, playingColumn.isVisible() );
		retMe.put ( Setting.CL_TABLE_NUMBER_COLUMN_SHOW, numberColumn.isVisible() );
		retMe.put ( Setting.CL_TABLE_ARTIST_COLUMN_SHOW, artistColumn.isVisible() );
		retMe.put ( Setting.CL_TABLE_YEAR_COLUMN_SHOW, yearColumn.isVisible() );
		retMe.put ( Setting.CL_TABLE_ALBUM_COLUMN_SHOW, albumColumn.isVisible() );
		retMe.put ( Setting.CL_TABLE_TITLE_COLUMN_SHOW, titleColumn.isVisible() );
		retMe.put ( Setting.CL_TABLE_LENGTH_COLUMN_SHOW, lengthColumn.isVisible() );
		
		return retMe;
	}

	@SuppressWarnings("unchecked")
	public void setSortMode ( DefaultSortMode sortMode ) {
		Runnable runMe = () -> {
			switch ( sortMode ) {
				case ARTIST:
					currentListTable.getSortOrder().clear();
					currentListTable.getSortOrder().add( artistColumn );
					artistColumn.setSortType( SortType.ASCENDING );
					break;
				case ARTIST_ALBUM_NUMBER:
					currentListTable.getSortOrder().clear();
					currentListTable.getSortOrder().add( artistColumn );
					currentListTable.getSortOrder().add( albumColumn );
					currentListTable.getSortOrder().add( numberColumn );
					artistColumn.setSortType( SortType.ASCENDING );
					albumColumn.setSortType( SortType.ASCENDING );
					numberColumn.setSortType( SortType.ASCENDING );
					break;
				case ARTIST_NUMBER:
					currentListTable.getSortOrder().clear();
					currentListTable.getSortOrder().add( artistColumn );
					currentListTable.getSortOrder().add( numberColumn );
					artistColumn.setSortType( SortType.ASCENDING );
					numberColumn.setSortType( SortType.ASCENDING );
					break;
				case ARTIST_TITLE:
					currentListTable.getSortOrder().clear();
					currentListTable.getSortOrder().add( artistColumn );
					currentListTable.getSortOrder().add( titleColumn );
					artistColumn.setSortType( SortType.ASCENDING );
					titleColumn.setSortType( SortType.ASCENDING );
					break;
				case ARTIST_YEAR_ALBUM_NUMBER:
					currentListTable.getSortOrder().clear();
					currentListTable.getSortOrder().add( artistColumn );
					currentListTable.getSortOrder().add( yearColumn );
					currentListTable.getSortOrder().add( albumColumn );
					currentListTable.getSortOrder().add( numberColumn );
					artistColumn.setSortType( SortType.ASCENDING );
					yearColumn.setSortType( SortType.ASCENDING );
					albumColumn.setSortType( SortType.ASCENDING );
					numberColumn.setSortType( SortType.ASCENDING );
					break;
				case NUMBER:
					currentListTable.getSortOrder().clear();
					currentListTable.getSortOrder().add( numberColumn );
					numberColumn.setSortType( SortType.ASCENDING );
					break;
				case NO_CHANGE:
					//Do nothing
					break;
				case YEAR_ALBUM_NUMBER:
					currentListTable.getSortOrder().clear();
					currentListTable.getSortOrder().add( yearColumn );
					currentListTable.getSortOrder().add( albumColumn );
					currentListTable.getSortOrder().add( numberColumn );
					yearColumn.setSortType( SortType.ASCENDING );
					albumColumn.setSortType( SortType.ASCENDING );
					numberColumn.setSortType( SortType.ASCENDING );
					break;
				case ALBUM_NUMBER:
					currentListTable.getSortOrder().clear();
					currentListTable.getSortOrder().add( albumColumn );
					currentListTable.getSortOrder().add( numberColumn );
					albumColumn.setSortType( SortType.ASCENDING );
					numberColumn.setSortType( SortType.ASCENDING );
				default:
					break;
			}
		};
		
		if ( Platform.isFxApplicationThread() ) {
			runMe.run();
		} else {
			Platform.runLater( runMe );
		}
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
				
				ImageView playPauseImage = ui.audioSystem.isPaused() ? pauseImage : playImage;
				
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
