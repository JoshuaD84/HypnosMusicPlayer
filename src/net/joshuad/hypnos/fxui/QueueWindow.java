package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.IOException;
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
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import net.joshuad.hypnos.CurrentListTrack;
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
	AudioSystem player;
	Library library;
	
	@SuppressWarnings("unchecked")
	public QueueWindow ( FXUI ui, Library library, AudioSystem player, TagWindow tagWindow ) {
		super();
		this.ui = ui;
		this.player = player;
		this.library = library;
		initModality( Modality.NONE );
		initOwner( ui.getMainStage() );
		setTitle( "Queue" );
		setWidth( 500 );
		setHeight ( 400 );
		Pane root = new Pane();
		Scene scene = new Scene( root );

		queueTable = new TableView<Track> ();
		Label emptyLabel = new Label( "Queue is empty." );
		emptyLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyLabel.setWrapText( true );
		emptyLabel.setTextAlignment( TextAlignment.CENTER );

		queueTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		queueTable.setPlaceholder( emptyLabel );
		queueTable.setItems( player.getQueue().getData() );
		
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
		
		queueTable.setOnKeyPressed( keyEvent -> {

			if ( keyEvent.getCode().equals( KeyCode.DELETE ) ) {
				ObservableList <Integer> selectedIndexes = queueTable.getSelectionModel().getSelectedIndices();
				
				List<Integer> removeMe = new ArrayList<Integer> ( selectedIndexes );
				
				if ( !removeMe.isEmpty() ) {
					int selectAfterDelete = selectedIndexes.get( 0 ) - 1;
					for ( int k = removeMe.size() - 1; k >= 0; k-- ) {
						player.getQueue().remove ( removeMe.get( k ) );
					}
					queueTable.getSelectionModel().clearAndSelect( selectAfterDelete );
					
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
			@SuppressWarnings("rawtypes") //TODO: Figure out how to get rid of this. 
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
		
		
		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem apendMenuItem = new MenuItem( "Append" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		MenuItem cropMenuItem = new MenuItem( "Crop" );
		MenuItem removeMenuItem = new MenuItem( "Remove from Queue" );
		contextMenu.getItems().addAll( 
			playMenuItem, apendMenuItem, editTagMenuItem, browseMenuItem, addToPlaylistMenuItem, cropMenuItem, removeMenuItem 
		);
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		queueTable.setRowFactory( tv -> {
			TableRow <Track> row = new TableRow <>();
			row.setContextMenu( contextMenu );
			
			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					player.playTrack( row.getItem(), false );
				}
			} );
			
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
					int dropIndex = row.isEmpty() ? dropIndex = player.getQueue().size() : row.getIndex();
					
					switch ( container.getSource() ) {
						case ALBUM_LIST:
						case PLAYLIST_LIST:
						case HISTORY: 
						case ALBUM_INFO:
						case PLAYLIST_INFO:
						case TRACK_LIST: {
							List <Track> tracksToCopy = container.getTracks();
							for ( Track track : tracksToCopy ) {
								System.out.println ( track.getTitle() );
							}
							player.getQueue().addAllTracks( dropIndex, tracksToCopy );
							
						} break;
						
						case CURRENT_LIST: {
							//TODO: Should I refactor this? 
							synchronized ( player.getCurrentList() ) {
								ArrayList <CurrentListTrack> tracksToCopy = new ArrayList <CurrentListTrack> (  );
								for ( int index : draggedIndices ) {
									if ( index >= 0 && index < player.getCurrentList().getItems().size() ) {
										tracksToCopy.add( player.getCurrentList().getItems().get( index ) );
									}
								}
								player.getQueue().addAllTracks( dropIndex, tracksToCopy );
							}
						} break;

						case QUEUE: {
							ArrayList <Track> tracksToMove = new ArrayList <Track> ( draggedIndices.size() );
							for ( int index : draggedIndices ) {
								if ( index >= 0 && index < player.getQueue().size() ) {
									tracksToMove.add( player.getQueue().get( index ) );
								}
							}
							
							for ( int k = draggedIndices.size() - 1; k >= 0; k-- ) {
								int index = draggedIndices.get( k ).intValue();
								if ( index >= 0 && index < player.getQueue().size() ) {
									player.getQueue().remove ( index );
								}
							}
							
							dropIndex = Math.min( player.getQueue().size(), row.getIndex() );
							
							player.getQueue().addAllTracks( dropIndex, tracksToMove );
							
							queueTable.getSelectionModel().clearSelection();
							for ( int k = 0; k < draggedIndices.size(); k++ ) {
								queueTable.getSelectionModel().select( dropIndex + k );
							}
							
							player.getQueue().updateQueueIndexes();
							
						} break;
					}

					player.getQueue().updateQueueIndexes( );
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
						try {
							tracksToAdd.add( new Track ( path ) );
						} catch ( IOException e ) {
							LOGGER.info ( "Error loading track: " + path );
						}
					}
										
					if ( !tracksToAdd.isEmpty() ) {
						int dropIndex = row.isEmpty() ? dropIndex = player.getQueue().size() : row.getIndex();
						player.getQueue().addAllTracks( Math.min( dropIndex, player.getQueue().size() ), tracksToAdd );
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
					case TRACK_LIST: {
						List <Track> tracksToCopy = container.getTracks();
						player.getQueue().addAllTracks( tracksToCopy );
					} break;
					
					case CURRENT_LIST: {
						//TODO: should I refactor this
						synchronized ( player.getCurrentList() ) {
							ArrayList <CurrentListTrack> tracksToCopy = new ArrayList <CurrentListTrack> (  );
							for ( int index : draggedIndices ) {
								if ( index >= 0 && index < player.getCurrentList().getItems().size() ) {
									tracksToCopy.add( player.getCurrentList().getItems().get( index ) );
								}
							}
							player.getQueue().addAllTracks( tracksToCopy );
						}
					} break;
					
					case QUEUE: {
						//Dragging from an empty queue to the queue has no meaning. 
						
					} break;
				}

				player.getQueue().updateQueueIndexes();
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
					try {
						tracksToAdd.add( new Track ( path ) );
					} catch ( IOException e ) {
						LOGGER.info ( "Error loading track: " + path );
					}
				}
				
				if ( !tracksToAdd.isEmpty() ) {
					player.getQueue().addAllTracks( tracksToAdd );
				}

				event.setDropCompleted( true );
				event.consume();
			}

		} );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				ui.promptAndSavePlaylist ( queueTable.getSelectionModel().getSelectedItems(), false );
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
				player.playItems( new ArrayList<Track> ( queueTable.getSelectionModel().getSelectedItems() ) );
			}
		});

		apendMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.getCurrentList().appendTracks ( queueTable.getSelectionModel().getSelectedItems() );
				player.play();
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
						player.getQueue().remove ( removeMe.get( k ).intValue() );
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
						player.getQueue().remove( removeMeIndices.get( k ).intValue() );
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
			} catch ( IOException e ) {
				LOGGER.info( "Unable to update the tag info for track in queue, removing it: " + track.getFilename() );
				queueTable.getItems().remove( track );
			}
		}
		queueTable.refresh();
	}
}