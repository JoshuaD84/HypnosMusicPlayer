package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
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
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
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
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.AlphanumComparator.CaseHandling;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.AudioSystem.RepeatMode;
import net.joshuad.hypnos.audio.AudioSystem.ShuffleMode;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;

public class CurrentListPane extends BorderPane {
	private static final Logger LOGGER = Logger.getLogger( CurrentListPane.class.getName() );

	HBox currentListControls; 

	TableView <CurrentListTrack> currentListTable;
	TableColumn clPlayingColumn, clArtistColumn, clYearColumn, clAlbumColumn, clTitleColumn, clNumberColumn, clLengthColumn;
	ContextMenu currentListColumnSelectorMenu;
	
	Button toggleRepeatButton, toggleShuffleButton;
	Button showQueueButton;
	MenuItem currentListSave, currentListExport, currentListLoad, historyMenuItem;
	
	ImageView noRepeatImage, repeatImage, repeatOneImage, sequentialImage, shuffleImage;
	ImageView queueImage, historyImage, menuImage;

	Image repeatImageSource;

	FXUI ui;
	AudioSystem audioSystem;
	Library library;
	
	public CurrentListPane( FXUI ui, AudioSystem audioSystem, Library library ) {
		this.ui = ui;
		this.audioSystem = audioSystem;
		this.library = library;
		
		loadImages();
		setupTable();
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
		currentListLength.setPadding( new Insets ( 0, 10, 0, 0 ) );
		
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
			
		
		final Label currentPlayingListInfo = new Label ( "" );
		currentPlayingListInfo.setAlignment( Pos.CENTER );
		currentPlayingListInfo.prefWidthProperty().bind( currentListControls.widthProperty() );
		
		audioSystem.getCurrentList().addListener( ( CurrentListState currentState ) -> {  
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
		currentListSave = new MenuItem ( "Save" );
		currentListExport = new MenuItem ( "Export" );
		currentListLoad = new MenuItem ( "Load Files" );
		historyMenuItem = new MenuItem ( "History" );
		MenuItem currentListShuffle = new MenuItem ( "Shuffle" );
		MenuItem jumpMenuItem = new MenuItem ( "Jump to Track" );
		
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
		
		currentListSave.setOnAction( new EventHandler <ActionEvent>() {
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
		
		currentListExport.setOnAction( ( ActionEvent e ) -> {
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
		
		jumpMenuItem.setOnAction( ( ActionEvent e ) -> {
			ui.jumpWindow.show();
		});
		
		currentListMenu.getItems().addAll ( currentListClear, currentListShuffle, jumpMenuItem, 
			currentListExport, currentListSave, currentListLoad, historyMenuItem );
		
		currentListControls.getChildren().addAll( toggleShuffleButton, toggleRepeatButton, showQueueButton,
				currentPlayingListInfo, currentListLength, currentListMenu );

		currentListControls.prefWidthProperty().bind( this.widthProperty() );
	}
	
	private void setupTable () {
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
				return new CurrentListTrackStateCell( ui, ui.transport.playImageSource, ui.transport.pauseImageSource ); 
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
		currentListTable.setItems( audioSystem.getCurrentList().getItems() );
		
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
						audioSystem.getCurrentList().appendTracks ( container.getTracks() );
					
					} break;

					case PLAYLIST_LIST: {
						audioSystem.getCurrentList().appendPlaylists ( container.getPlaylists() );
						
					} break;
					
					case ALBUM_LIST: {
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
					audioSystem.getCurrentList().insertTrackPathList ( 0, tracksToAdd );
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
				ui.jumpWindow.show();
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
				ui.promptAndSavePlaylist ( new ArrayList <Track> ( currentListTable.getSelectionModel().getSelectedItems() ) );
			}
		});

		EventHandler addToPlaylistHandler = new EventHandler <ActionEvent>() {
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
							audioSystem.getCurrentList().insertAlbums( dropIndex, container.getAlbums() );
						} break;
						
						case PLAYLIST_LIST:
						case TRACK_LIST:
						case ALBUM_INFO:
						case PLAYLIST_INFO:
						case TAG_ERROR_LIST:
						case HISTORY: {
							audioSystem.getCurrentList().insertTracks( dropIndex, Utils.convertTrackList( container.getTracks() ) );
						} break;
						
						case CURRENT_LIST: {
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
