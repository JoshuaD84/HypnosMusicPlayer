package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import net.joshuad.hypnos.CurrentListTrack;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Library;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;

public class QueueWindow extends Stage {

	private static final Logger LOGGER = Logger.getLogger( QueueWindow.class.getName() );
	
	TableView <Track> queueTable;
	FXUI ui;
	AudioSystem audioSystem;
	Library library;
	
	@SuppressWarnings("unchecked")
	public QueueWindow ( FXUI ui, Library library, AudioSystem audioSystem, TagWindow tagWindow ) {
		super();
		this.ui = ui;
		this.audioSystem = audioSystem;
		this.library = library;
		initModality( Modality.NONE );
		initOwner( ui.getMainStage() );
		setTitle( "Queue" );
		setWidth( 500 );
		setHeight ( 400 );
		Pane root = new Pane();
		Scene scene = new Scene( root );
		
		try {
			getIcons().add( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) ) );
		} catch ( FileNotFoundException e ) {
			LOGGER.warning( "Unable to load program icon: resources/icon.png" );
		}

		queueTable = new TableView<Track> ();
		Label emptyLabel = new Label( "Queue is empty." );
		emptyLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyLabel.setWrapText( true );
		emptyLabel.setTextAlignment( TextAlignment.CENTER );

		queueTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		queueTable.setPlaceholder( emptyLabel );
		queueTable.setItems( audioSystem.getQueue().getData() );
		
		queueTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		queueTable.widthProperty().addListener( new ChangeListener <Number>() {
			@Override
			public void changed ( ObservableValue <? extends Number> source, Number oldWidth, Number newWidth ) {
				Pane header = (Pane) queueTable.lookup( "TableHeaderRow" );
				if ( header.isVisible() ) {
					header.setMaxHeight( 0 );
					header.setMinHeight( 0 );
					header.setPrefHeight( 0 );
					header.setVisible( false );
				}
			}
		});
		
		TableColumn numberColumn = new TableColumn<Track, String> ( "#" );
		TableColumn artistColumn = new TableColumn<Track, String> ( "Artist" );
		TableColumn titleColumn = new TableColumn<Track, String> ( "Title" );
		
		numberColumn.setMaxWidth( 10000 );
		artistColumn.setMaxWidth( 45000 );
		titleColumn.setMaxWidth ( 45000 );
		
		numberColumn.setCellValueFactory( new Callback <CellDataFeatures <Track, String>, ObservableValue <String>>() {
			@SuppressWarnings("rawtypes") //REFACTOR: Figure out how to get rid of this. 
			@Override
			public ObservableValue <String> call ( CellDataFeatures <Track, String> p ) {
				return new ReadOnlyObjectWrapper ( p.getValue() );
			}
		});

		numberColumn.setCellFactory( new Callback <TableColumn <Track, Track>, TableCell <Track, Track>>() {
			@Override
			public TableCell <Track, Track> call ( TableColumn <Track, Track> param ) {
				return new TableCell <Track, Track>() {
					@Override
					protected void updateItem ( Track item, boolean empty ) {
						super.updateItem( item, empty );

						if ( this.getTableRow() != null && item != null ) {
							setText( this.getTableRow().getIndex() + 1 + "" );
						} else {
							setText( "" );
						}
					}
				};
			}
		});
		numberColumn.setSortable(false);
		
		artistColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Artist" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Title" ) );
		
		queueTable.getColumns().addAll( numberColumn, artistColumn, titleColumn );
		
		Menu lastFMMenu = new Menu( "LastFM" );
		MenuItem loveMenuItem = new MenuItem ( "Love" );
		MenuItem unloveMenuItem = new MenuItem ( "Unlove" );
		MenuItem scrobbleMenuItem = new MenuItem ( "Scrobble" );
		lastFMMenu.getItems().addAll ( loveMenuItem, unloveMenuItem, scrobbleMenuItem );
		lastFMMenu.setVisible ( false );
		lastFMMenu.visibleProperty().bind( ui.showLastFMWidgets );
		
		loveMenuItem.setOnAction( ( event ) -> {
			ui.audioSystem.getLastFM().loveTrack( queueTable.getSelectionModel().getSelectedItem() );
		});
		
		unloveMenuItem.setOnAction( ( event ) -> {
			ui.audioSystem.getLastFM().unloveTrack( queueTable.getSelectionModel().getSelectedItem() );
		});
		
		scrobbleMenuItem.setOnAction( ( event ) -> {
			ui.audioSystem.getLastFM().scrobbleTrack( queueTable.getSelectionModel().getSelectedItem() );
		});
		
		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem appendMenuItem = new MenuItem( "Append" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem infoMenuItem = new MenuItem( "Info" );
		MenuItem lyricsMenuItem = new MenuItem( "Lyrics" );
		MenuItem goToAlbumMenuItem = new MenuItem( "Go to Album" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		MenuItem cropMenuItem = new MenuItem( "Crop" );
		MenuItem removeMenuItem = new MenuItem( "Remove from Queue" );
		contextMenu.getItems().addAll( 
			playMenuItem, appendMenuItem, editTagMenuItem, infoMenuItem, lyricsMenuItem,
			goToAlbumMenuItem, browseMenuItem, addToPlaylistMenuItem, 
			lastFMMenu, cropMenuItem, removeMenuItem 
		);
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );
		
		queueTable.setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ESCAPE 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				if ( queueTable.getSelectionModel().getSelectedItems().size() > 0 ) {
					queueTable.getSelectionModel().clearSelection();
					e.consume();
				} else {
					this.hide();
				}
				
			} else if ( e.getCode() == KeyCode.G && e.isControlDown()
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				goToAlbumMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.F2 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
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
				
			} else if ( e.getCode() == KeyCode.L
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				lyricsMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.ENTER
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				playMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				audioSystem.getCurrentList().insertTracks( 0, queueTable.getSelectionModel().getSelectedItems() );
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isControlDown()
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				appendMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.DELETE
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				removeMenuItem.fire();
				e.consume();
						
			} else if ( e.getCode() == KeyCode.DELETE && e.isShiftDown() 
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				cropMenuItem.fire();
				e.consume();
			}
		});
		
		queueTable.setRowFactory( tv -> {
			TableRow <Track> row = new TableRow <>();
			row.setContextMenu( contextMenu );
			
			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					audioSystem.playTrack( row.getItem(), false );
				}
			} );
			
			row.setOnContextMenuRequested( event -> { 
				goToAlbumMenuItem.setDisable( row.getItem().getAlbumPath() == null );
			});
			
			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					ArrayList <Integer> indices = new ArrayList <Integer>( queueTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( queueTable.getSelectionModel().getSelectedItems() );
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, null, null, DragSource.QUEUE );
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
				if (  db.hasContent( FXUI.DRAGGED_TRACKS ) || db.hasFiles() ) {
					event.acceptTransferModes( TransferMode.COPY );
					event.consume();
				}
			} );

			row.setOnDragDropped( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasContent( FXUI.DRAGGED_TRACKS ) ) {
					
					DraggedTrackContainer container = (DraggedTrackContainer) db.getContent( FXUI.DRAGGED_TRACKS );
					List <Integer> draggedIndices = container.getIndices();
					int dropIndex = row.isEmpty() ? dropIndex = audioSystem.getQueue().size() : row.getIndex();
					
					switch ( container.getSource() ) {
						case ALBUM_LIST:
						case PLAYLIST_LIST:
						case HISTORY: 
						case TAG_ERROR_LIST:
						case ALBUM_INFO:
						case PLAYLIST_INFO:
						case TRACK_LIST: {
							List <Track> tracksToCopy = container.getTracks();
							audioSystem.getQueue().queueAllTracks( tracksToCopy, dropIndex );
							
						} break;
						
						case CURRENT_LIST: {
							//PENDING: Should I refactor this? 
							synchronized ( audioSystem.getCurrentList() ) {
								ArrayList <CurrentListTrack> tracksToCopy = new ArrayList <CurrentListTrack> (  );
								for ( int index : draggedIndices ) {
									if ( index >= 0 && index < audioSystem.getCurrentList().getItems().size() ) {
										tracksToCopy.add( audioSystem.getCurrentList().getItems().get( index ) );
									}
								}
								audioSystem.getQueue().queueAllTracks( tracksToCopy, dropIndex );
							}
						} break;

						case QUEUE: {
							ArrayList <Track> tracksToMove = new ArrayList <Track> ( draggedIndices.size() );
							for ( int index : draggedIndices ) {
								if ( index >= 0 && index < audioSystem.getQueue().size() ) {
									tracksToMove.add( audioSystem.getQueue().get( index ) );
								}
							}
							
							for ( int k = draggedIndices.size() - 1; k >= 0; k-- ) {
								int index = draggedIndices.get( k ).intValue();
								if ( index >= 0 && index < audioSystem.getQueue().size() ) {
									audioSystem.getQueue().remove ( index );
								}
							}
							
							dropIndex = Math.min( audioSystem.getQueue().size(), row.getIndex() );
							
							audioSystem.getQueue().queueAllTracks( tracksToMove, dropIndex );
							
							queueTable.getSelectionModel().clearSelection();
							for ( int k = 0; k < draggedIndices.size(); k++ ) {
								queueTable.getSelectionModel().select( dropIndex + k );
							}
							
							audioSystem.getQueue().updateQueueIndexes();
							
						} break;
					}

					audioSystem.getQueue().updateQueueIndexes( );
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
						int dropIndex = row.isEmpty() ? dropIndex = audioSystem.getQueue().size() : row.getIndex();
						audioSystem.getQueue().queueAllTracks( tracksToAdd, Math.min( dropIndex, audioSystem.getQueue().size() ) );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});
			
			return row;
		});
		
		queueTable.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();

			if ( db.hasContent( FXUI.DRAGGED_TRACKS ) || db.hasFiles() ) {

				event.acceptTransferModes( TransferMode.COPY );
				event.consume();

			}
		} );

		queueTable.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasContent( FXUI.DRAGGED_TRACKS ) ) {
				
				DraggedTrackContainer container = (DraggedTrackContainer) db.getContent( FXUI.DRAGGED_TRACKS );
				List <Integer> draggedIndices = container.getIndices();
				
				switch ( container.getSource() ) {

					case ALBUM_LIST:
					case PLAYLIST_LIST:
					case HISTORY: 
					case ALBUM_INFO:
					case PLAYLIST_INFO:
					case TAG_ERROR_LIST:
					case TRACK_LIST: {
						List <Track> tracksToCopy = container.getTracks();
						audioSystem.getQueue().queueAllTracks( tracksToCopy );
					} break;
					
					case CURRENT_LIST: {
						//PENDING: should I refactor this
						synchronized ( audioSystem.getCurrentList() ) {
							ArrayList <CurrentListTrack> tracksToCopy = new ArrayList <CurrentListTrack> (  );
							for ( int index : draggedIndices ) {
								if ( index >= 0 && index < audioSystem.getCurrentList().getItems().size() ) {
									tracksToCopy.add( audioSystem.getCurrentList().getItems().get( index ) );
								}
							}
							audioSystem.getQueue().queueAllTracks( tracksToCopy );
						}
					} break;
					
					case QUEUE: {
						//Dragging from an empty queue to the queue has no meaning. 
						
					} break;
				}

				audioSystem.getQueue().updateQueueIndexes();
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
					audioSystem.getQueue().queueAllTracks( tracksToAdd );
				}

				event.setDropCompleted( true );
				event.consume();
			}

		} );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				ui.promptAndSavePlaylist ( queueTable.getSelectionModel().getSelectedItems() );
			}
		});

		EventHandler <ActionEvent> addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				ui.addToPlaylist ( queueTable.getSelectionModel().getSelectedItems(), playlist );
			}
		};
		
		library.getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		});

		ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List <Track> selectedItems =  new ArrayList<Track> ( queueTable.getSelectionModel().getSelectedItems() );
				
				if ( selectedItems.size() == 1 ) {
					audioSystem.playItems( selectedItems );
					
				} else if ( selectedItems.size() > 1 ) {
					if ( ui.okToReplaceCurrentList() ) {
						audioSystem.playItems( selectedItems );
					}
				}
			}
		});

		appendMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				audioSystem.getCurrentList().appendTracks ( queueTable.getSelectionModel().getSelectedItems() );
			}
		});
		
		editTagMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List<Track> tracks = queueTable.getSelectionModel().getSelectedItems();
				
				tagWindow.setTracks( tracks, null );
				tagWindow.show();
			}
		});
		
		infoMenuItem.setOnAction( event -> {
			ui.trackInfoWindow.setTrack( queueTable.getSelectionModel().getSelectedItem() );
			ui.trackInfoWindow.show();
		});
		
		lyricsMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ui.lyricsWindow.setTrack( queueTable.getSelectionModel().getSelectedItem() );
				ui.lyricsWindow.show();
			}
		});
		
		goToAlbumMenuItem.setOnAction( ( event ) -> {
			ui.goToAlbumOfTrack ( queueTable.getSelectionModel().getSelectedItem() );
		});
		
		browseMenuItem.setOnAction( ( ActionEvent event ) -> {
			ui.openFileBrowser(  queueTable.getSelectionModel().getSelectedItem().getPath() );
		});

		cropMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {

				ObservableList <Integer> selectedIndexes = queueTable.getSelectionModel().getSelectedIndices();
				
				ArrayList <Integer> removeMe = new ArrayList<Integer> ();
				
				for ( int k = 0; k < queueTable.getItems().size(); k++ ) {
					if ( !selectedIndexes.contains( k ) ) {
						removeMe.add ( k );
					}
				}
				
				if ( !removeMe.isEmpty() ) {

					for ( int k = removeMe.size() - 1; k >= 0; k-- ) {
						audioSystem.getQueue().remove ( removeMe.get( k ).intValue() );
					}

					queueTable.getSelectionModel().clearSelection();
				}
			}
		} );

		removeMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				synchronized ( queueTable.getItems() ) {
					List<Integer> selectedIndices = queueTable.getSelectionModel().getSelectedIndices();
					
					ArrayList<Integer> removeMeIndices = new ArrayList<Integer> ( selectedIndices );
					
					for ( int k = removeMeIndices.size() - 1; k >= 0 ; k-- ) {
						audioSystem.getQueue().remove( removeMeIndices.get( k ).intValue() );
					}
				}
			}
		});

		queueTable.prefWidthProperty().bind( root.widthProperty() );
		queueTable.prefHeightProperty().bind( root.heightProperty() );
		
		root.getChildren().add( queueTable );
		setScene( scene );
	}
	
	public void refresh () {
		for ( Track track : queueTable.getItems() ) {
			try {
				track.refreshTagData();
			} catch ( Exception e ) {}
		}
		queueTable.refresh();
	}
}
