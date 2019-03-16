package net.joshuad.hypnos.fxui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.joshuad.hypnos.Album;
import net.joshuad.hypnos.CurrentList;
import net.joshuad.hypnos.Library;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;

public class PlaylistInfoWindow extends Stage {
	
	private static final Logger LOGGER = Logger.getLogger( PlaylistInfoWindow.class.getName() );
	
	Playlist playlist;
	TableView <Track> trackTable;
	TextField locationField;
	FXUI ui;
	Library library;
	AudioSystem audioSystem;
	
	public PlaylistInfoWindow( FXUI ui, Library library, AudioSystem audioSystem ) {
		super();
		this.ui = ui;
		this.library = library;
		this.audioSystem = audioSystem;
		this.initModality( Modality.NONE );
		this.initOwner( ui.getMainStage() );
		this.setTitle( "Album Info" );
		this.setWidth( 600 );
		this.setHeight( 400 );
		Pane root = new Pane();
		Scene scene = new Scene( root );
		VBox primaryPane = new VBox();
		
		setupPlaylistTable( primaryPane );
		
		primaryPane.prefWidthProperty().bind( root.widthProperty() );
		primaryPane.prefHeightProperty().bind( root.heightProperty() );
		
		primaryPane.getChildren().addAll( trackTable );
		root.getChildren().add( primaryPane );
		setScene( scene );
		
		scene.addEventFilter( KeyEvent.KEY_PRESSED, new EventHandler <KeyEvent>() {
			@Override
			public void handle ( KeyEvent e ) {
				if ( e.getCode() == KeyCode.ESCAPE
				&& !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() && !e.isAltDown() ) {
					hide();
					e.consume();
				}
			}
		});
	}

	public void setPlaylist ( Playlist playlist ) { 
		this.playlist = playlist;
		if ( playlist != null ) {
			trackTable.setItems( playlist.getTracks() );
			this.setTitle( "Playlist Info: " + playlist.getName() );
		}
	}
	
	private void setupPlaylistTable ( VBox primaryPane ) {
		
		TableColumn<Track, String> artistColumn = new TableColumn<Track, String>( "Artist" );
		TableColumn<Track, String> titleColumn = new TableColumn<Track, String>( "Title" );
		TableColumn<Track, Integer> lengthColumn = new TableColumn<Track, Integer>( "Length" );
		
		artistColumn.setMaxWidth( 400000 );
		titleColumn.setMaxWidth( 500000 );
		lengthColumn.setMaxWidth( 90000 );
		
		artistColumn.setEditable( false );
		titleColumn.setEditable( false );
		lengthColumn.setEditable( false );
		
		artistColumn.setReorderable( false );
		titleColumn.setReorderable( false );
		lengthColumn.setReorderable( false );
		
		artistColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Artist" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Title" ) );
		lengthColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "LengthDisplay" ) );
		
		trackTable = new TableView<Track> ();
		trackTable.getColumns().addAll( artistColumn, titleColumn, lengthColumn );
		trackTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		trackTable.setEditable( true );
		trackTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		trackTable.prefWidthProperty().bind( primaryPane.widthProperty() );
		trackTable.prefHeightProperty().bind( primaryPane.heightProperty() );
		trackTable.setPlaceholder( new Label( "Empty Playlist." ) );
		
		trackTable.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasContent( FXUI.DRAGGED_TRACKS ) || db.hasFiles() ) {
				event.acceptTransferModes( TransferMode.COPY );
				event.consume();
			}
		} );
		
		trackTable.getSelectionModel().selectedItemProperty().addListener( ( obs, oldSelection, newSelection ) -> {
			ui.trackSelected ( newSelection );
		});
		
		trackTable.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasContent( FXUI.DRAGGED_TRACKS ) ) {

				DraggedTrackContainer container = (DraggedTrackContainer) db.getContent( FXUI.DRAGGED_TRACKS );
				
				switch ( container.getSource() ) {
					case PLAYLIST_LIST: {
						if ( container.getPlaylists() == null ) {
							LOGGER.fine ( "Recieved null data from playlist list, ignoring." );
							
						} else {
							List <Track> tracksToCopy = new ArrayList<Track>();
							for ( Playlist playlist : container.getPlaylists() ) {
								if ( playlist == null ) {
									LOGGER.fine ( "Recieved null playlist from playlist list, ignoring." );
								} else {
									tracksToCopy.addAll( playlist.getTracks() );
								}
									
							}
							trackTable.getItems().addAll ( tracksToCopy );
						}
					} break;
					
					case ALBUM_LIST: {
						if ( container.getAlbums() == null ) {
							LOGGER.fine ( "Recieved null data from playlist list, ignoring." );
							
						} else {
							List <Track> tracksToCopy = new ArrayList<Track>();
							for ( Album album : container.getAlbums() ) {
								if ( album == null ) {
									LOGGER.fine ( "Null album dropped in playlist window, ignoring." );
								} else {
									tracksToCopy.addAll( album.getTracks() );
								}
							}
							trackTable.getItems().addAll ( tracksToCopy );
						}
					} break;

					case ARTIST_LIST:
					case TRACK_LIST:
					case ALBUM_INFO:
					case HISTORY: 
					case CURRENT_LIST:
					case TAG_ERROR_LIST:
					case QUEUE: 
					case CURRENT_TRACK: {
						List <Track> tracksToCopy = container.getTracks();
						trackTable.getItems().addAll( tracksToCopy );
					} break;
					
					case PLAYLIST_INFO: {
						//Not possible; table is currently empty
					} break;
				}

				event.setDropCompleted( true );
				event.consume();

			} else if ( db.hasFiles() ) {
				ArrayList <Path> pathsToAdd = new ArrayList<Path> ();
				
				for ( File file : db.getFiles() ) {
					Path droppedPath = Paths.get( file.getAbsolutePath() );
					if ( Utils.isMusicFile( droppedPath ) ) {
						pathsToAdd.add( droppedPath );
					
					} else if ( Files.isDirectory( droppedPath ) ) {
						pathsToAdd.addAll( Utils.getAllTracksInDirectory( droppedPath ) );
					
					} else if ( Utils.isPlaylistFile ( droppedPath ) ) {
						List<Path> paths = Playlist.getTrackPaths( droppedPath );
						pathsToAdd.addAll( paths );
					}
				}
				
				ArrayList <Track> tracksToAdd = new ArrayList<Track> ( pathsToAdd.size() );
				
				for ( Path path : pathsToAdd ) {
					tracksToAdd.add( new Track ( path ) );
				}
				
				if ( !tracksToAdd.isEmpty() ) {
					trackTable.getItems().addAll( tracksToAdd );
				}

				event.setDropCompleted( true );
				event.consume();
			}
		} );
		
		Menu lastFMMenu = new Menu( "LastFM" );
		MenuItem loveMenuItem = new MenuItem ( "Love" );
		MenuItem unloveMenuItem = new MenuItem ( "Unlove" );
		MenuItem scrobbleMenuItem = new MenuItem ( "Scrobble" );
		lastFMMenu.getItems().addAll ( loveMenuItem, unloveMenuItem, scrobbleMenuItem );
		lastFMMenu.setVisible ( false );
		lastFMMenu.visibleProperty().bind( ui.showLastFMWidgets );
		
		loveMenuItem.setOnAction( ( event ) -> {
			ui.audioSystem.getLastFM().loveTrack( trackTable.getSelectionModel().getSelectedItem() );
		});
		
		unloveMenuItem.setOnAction( ( event ) -> {
			ui.audioSystem.getLastFM().unloveTrack( trackTable.getSelectionModel().getSelectedItem() );
		});
		
		scrobbleMenuItem.setOnAction( ( event ) -> {
			ui.audioSystem.getLastFM().scrobbleTrack( trackTable.getSelectionModel().getSelectedItem() );
		});
		
		ContextMenu contextMenu = new ContextMenu();
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
		MenuItem removeMenuItem = new MenuItem ( "Remove" );
		contextMenu.getItems().addAll ( 
			playMenuItem, appendMenuItem, playNextMenuItem, enqueueMenuItem, editTagMenuItem, 
			infoMenuItem, lyricsMenuItem, goToAlbumMenuItem, browseMenuItem, addToPlaylistMenuItem, 
			lastFMMenu, removeMenuItem 
		);
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );
		addToPlaylistMenuItem.getItems().add( newPlaylistButton );
		
		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				ui.promptAndSavePlaylist ( new ArrayList <Track> ( trackTable.getSelectionModel().getSelectedItems() ) );
			}
		});

		EventHandler <ActionEvent> addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				ui.addToPlaylist ( trackTable.getSelectionModel().getSelectedItems(), playlist );
			}
		};

		library.getPlaylistsSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		});

		ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		

		trackTable.setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ESCAPE 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				trackTable.getSelectionModel().clearSelection();
			
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
				
			} else if ( e.getCode() == KeyCode.F3 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				infoMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.F4 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				ui.openFileBrowser( trackTable.getSelectionModel().getSelectedItem().getPath() );
				
			} else if ( e.getCode() == KeyCode.L
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				lyricsMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.ENTER
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				playMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				audioSystem.getCurrentList().insertTracks( 0, trackTable.getSelectionModel().getSelectedItems() );
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				appendMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.DELETE
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				removeMenuItem.fire();
			}
		});
		
		playNextMenuItem.setOnAction( event -> {
			audioSystem.getQueue().queueAllTracks( trackTable.getSelectionModel().getSelectedItems(), 0 );
		});
		
		enqueueMenuItem.setOnAction( event -> {
			audioSystem.getQueue().queueAllTracks( trackTable.getSelectionModel().getSelectedItems() );
		});
		
		infoMenuItem.setOnAction( event -> {
			ui.trackInfoWindow.setTrack( trackTable.getSelectionModel().getSelectedItem() );
			ui.trackInfoWindow.show();
		});
		
		lyricsMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ui.lyricsWindow.setTrack( trackTable.getSelectionModel().getSelectedItem() );
				ui.lyricsWindow.show();
			}
		});
		
		goToAlbumMenuItem.setOnAction( ( event ) -> {
			ui.goToAlbumOfTrack ( trackTable.getSelectionModel().getSelectedItem() );
		});
		
		browseMenuItem.setOnAction( event -> {
			ui.openFileBrowser( trackTable.getSelectionModel().getSelectedItem().getPath() );
		});
		
		editTagMenuItem.setOnAction( event -> {
			ui.tagWindow.setTracks( (List<Track>)(List<?>)trackTable.getSelectionModel().getSelectedItems(), null );
			ui.tagWindow.show();
		});
		
		appendMenuItem.setOnAction( event -> {
			ui.getCurrentListPane().currentListTable.getItems().addAll( Utils.convertTrackList( trackTable.getSelectionModel().getSelectedItems() ) );
		});

		removeMenuItem.setOnAction( event -> {
			List <Track> removeMe = new ArrayList<> ();
			
			for ( int k = 0; k < playlist.getTracks().size(); k++ ) {
				if ( trackTable.getSelectionModel().getSelectedIndices().contains( k ) ) {
					removeMe.add( playlist.getTracks().get( k ) );
				}
			}

			trackTable.getSelectionModel().clearSelection();
			playlist.getTracks().removeAll( removeMe );
		});
		
		playMenuItem.setOnAction( event -> {
			List <Track> selectedItems =  new ArrayList<Track> ( trackTable.getSelectionModel().getSelectedItems() );
			
			if ( selectedItems.size() == 1 ) {
				audioSystem.playItems( selectedItems );
				
			} else if ( selectedItems.size() > 1 ) {
				if ( ui.okToReplaceCurrentList() ) {
					audioSystem.playItems( selectedItems );
				}
			}
		});
		
		trackTable.setRowFactory( tv -> {
			TableRow <Track> row = new TableRow <>();

			row.itemProperty().addListener( (obs, oldValue, newValue ) -> {
				if ( newValue != null ) {
					row.setContextMenu( contextMenu );
				} else {
					row.setContextMenu( null );
				}
			});
			
			row.setOnContextMenuRequested( event -> { 
				goToAlbumMenuItem.setDisable( row.getItem().getAlbumPath() == null );
			});

			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					audioSystem.playTrack( row.getItem() );
				}
			} );
			
			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					ArrayList <Integer> indices = new ArrayList <Integer>( trackTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( trackTable.getSelectionModel().getSelectedItems() );
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, null, null, null, DragSource.PLAYLIST_INFO );
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
			} );

			row.setOnDragDropped( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasContent( FXUI.DRAGGED_TRACKS ) ) {

					DraggedTrackContainer container = (DraggedTrackContainer) db.getContent( FXUI.DRAGGED_TRACKS );
					int dropIndex = row.isEmpty() ? dropIndex = trackTable.getItems().size() : row.getIndex();
					
					switch ( container.getSource() ) {
						case PLAYLIST_LIST: {
							if ( container.getPlaylists() == null ) {
								LOGGER.fine ( "Recieved null data from playlist list, ignoring." );
								
							} else {
								List <Track> tracksToCopy = new ArrayList<Track>();
								for ( Playlist playlist : container.getPlaylists() ) {
									if ( playlist == null ) {
										LOGGER.fine ( "Recieved null playlist from playlist list, ignoring." );
									} else {
										tracksToCopy.addAll( playlist.getTracks() );
									}
										
								}
								trackTable.getItems().addAll ( tracksToCopy );
							}
						} break;
						
						case ALBUM_LIST: {
							if ( container.getAlbums() == null ) {
								LOGGER.fine ( "Recieved null data from playlist list, ignoring." );
								
							} else {
								List <Track> tracksToCopy = new ArrayList<Track>();
								for ( Album album : container.getAlbums() ) {
									if ( album == null ) {
										LOGGER.fine ( "Null album dropped in playlist window, ignoring." );
									} else {
										tracksToCopy.addAll( album.getTracks() );
									}
								}
								trackTable.getItems().addAll ( dropIndex, tracksToCopy );
							}
						} break;

						case ARTIST_LIST:
						case TRACK_LIST:
						case ALBUM_INFO:
						case HISTORY: 
						case CURRENT_LIST:
						case TAG_ERROR_LIST:
						case QUEUE:
						case CURRENT_TRACK: {
							List <Track> tracksToCopy = container.getTracks();
							trackTable.getItems().addAll( dropIndex, tracksToCopy );
						} break;
						
						case PLAYLIST_INFO: {
							List <Integer> draggedIndices = container.getIndices();
							ArrayList <Track> tracksToMove = new ArrayList <Track> ( draggedIndices.size() );
							for ( int index : draggedIndices ) {
								if ( index >= 0 && index < trackTable.getItems().size() ) {
									tracksToMove.add( trackTable.getItems().get( index ) );
								}
							}
							
							for ( int k = draggedIndices.size() - 1; k >= 0; k-- ) {
								int index = draggedIndices.get( k ).intValue();
								if ( index >= 0 && index < trackTable.getItems().size() ) {
									trackTable.getItems().remove ( index );
								}
							}
							
							dropIndex = Math.min( trackTable.getItems().size(), row.getIndex() );
							
							trackTable.getItems().addAll( dropIndex, tracksToMove );
							
							trackTable.getSelectionModel().clearSelection();
							for ( int k = 0; k < draggedIndices.size(); k++ ) {
								trackTable.getSelectionModel().select( dropIndex + k );
							}

							playlist.setTracks( new ArrayList <Track> ( trackTable.getItems() ) );
						} break;
					}
					
					if ( audioSystem.getCurrentList().getState().getMode() == CurrentList.Mode.PLAYLIST ) {
						Playlist currentListPlaylist = audioSystem.getCurrentList().getState().getPlaylist();
						if ( currentListPlaylist != null && currentListPlaylist.equals( this.playlist ) ) {
							audioSystem.getCurrentList().setPlaylist( playlist );
						}
					}

					event.setDropCompleted( true );
					event.consume();

				} else if ( db.hasFiles() ) {
					ArrayList <Path> pathsToAdd = new ArrayList<Path> ();
					
					for ( File file : db.getFiles() ) {
						Path droppedPath = Paths.get( file.getAbsolutePath() );
						if ( Utils.isMusicFile( droppedPath ) ) {
							pathsToAdd.add( droppedPath );
						
						} else if ( Files.isDirectory( droppedPath ) ) {
							pathsToAdd.addAll( Utils.getAllTracksInDirectory( droppedPath ) );
						
						} else if ( Utils.isPlaylistFile ( droppedPath ) ) {
							List<Path> paths = Playlist.getTrackPaths( droppedPath );
							pathsToAdd.addAll( paths );
						}
					}
					
					ArrayList <Track> tracksToAdd = new ArrayList<Track> ( pathsToAdd.size() );
					
					for ( Path path : pathsToAdd ) {
						tracksToAdd.add( new Track ( path ) );
					}
					
					if ( !tracksToAdd.isEmpty() ) {
						int dropIndex = row.isEmpty() ? dropIndex = trackTable.getItems().size() : row.getIndex();
						trackTable.getItems().addAll( Math.min( dropIndex, trackTable.getItems().size() ), tracksToAdd );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});
		

			return row;
		});
	}
}




