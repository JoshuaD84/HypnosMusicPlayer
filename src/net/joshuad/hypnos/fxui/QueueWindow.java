package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.SoundSystem;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;

public class QueueWindow extends Stage {

	TableView <Track> queueTable;
	FXUI ui;
	SoundSystem player;
	
	@SuppressWarnings("unchecked")
	public QueueWindow ( FXUI ui, SoundSystem player, TagWindow tagWindow ) {
		super();
		this.ui = ui;
		this.player = player;
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
						queueTable.getItems().remove ( removeMe.get( k ).intValue() );
					}
					queueTable.getSelectionModel().clearAndSelect( selectAfterDelete );
				}
			}
		});

		TableColumn numberColumn = new TableColumn ( "#" );
		TableColumn artistColumn = new TableColumn ( "Artist" );
		TableColumn titleColumn = new TableColumn ( "Title" );
		
		numberColumn.setMaxWidth( 10000 );
		artistColumn.setMaxWidth( 45000 );
		titleColumn.setMaxWidth ( 45000 );
		
		numberColumn.setCellValueFactory( new Callback <CellDataFeatures <Track, Track>, ObservableValue <String>>() {
			@Override
			public ObservableValue <String> call ( CellDataFeatures <Track, Track> p ) {
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
					player.loadTrack ( queueTable.getSelectionModel().getSelectedItem() );
				}
			} );
			
			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					ArrayList <Integer> indices = new ArrayList <Integer>( queueTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( queueTable.getSelectionModel().getSelectedItems() );
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, null, DragSource.QUEUE );
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
					int dropIndex = row.isEmpty() ? dropIndex = queueTable.getItems().size() : row.getIndex();
					
					switch ( container.getSource() ) {
						case ALBUM_LIST:
						case PLAYLIST_LIST:
						case HISTORY: 
						case ALBUM_INFO:
						case TRACK_LIST: {
							List <Track> tracksToCopy = container.getTracks();
							player.getQueue().addAllTracks( dropIndex, tracksToCopy );
							
						} break;
						case CURRENT_LIST: {
							//TODO: Should I refactor this? 
							synchronized ( player.getCurrentList() ) {
								ArrayList <CurrentListTrack> tracksToCopy = new ArrayList <CurrentListTrack> (  );
								for ( int index : draggedIndices ) {
									if ( index >= 0 && index < player.getCurrentList().size() ) {
										tracksToCopy.add( player.getCurrentList().get( index ) );
									}
								}
								player.getQueue().addAllTracks( dropIndex, tracksToCopy );
							}
						} break;
						
												
						case QUEUE: {
							ArrayList <Track> tracksToMove = new ArrayList <Track> ( draggedIndices.size() );
							for ( int index : draggedIndices ) {
								if ( index >= 0 && index < queueTable.getItems().size() ) {
									tracksToMove.add( queueTable.getItems().get( index ) );
								}
							}
							
							for ( int k = draggedIndices.size() - 1; k >= 0; k-- ) {
								int index = draggedIndices.get( k ).intValue();
								if ( index >= 0 && index < queueTable.getItems().size() ) {
									player.getQueue().remove ( index );
								}
							}
							
							dropIndex = Math.min( player.getQueue().size(), row.getIndex() );
							
							player.getQueue().addAllTracks( dropIndex, tracksToMove );
							
							queueTable.getSelectionModel().clearSelection();
							for ( int k = 0; k < draggedIndices.size(); k++ ) {
								queueTable.getSelectionModel().select( dropIndex + k );
							}
							
							player.getQueue().updateQueueIndexes( null );
							
						} break;
					}

					player.getQueue().updateQueueIndexes( null );
					event.setDropCompleted( true );
					event.consume();

				} else if ( db.hasFiles() ) {
					ArrayList <Track> tracksToAdd = new ArrayList<Track> ();
					for ( File file : db.getFiles() ) {
						Path droppedPath = Paths.get( file.getAbsolutePath() );
						if ( Utils.isMusicFile( droppedPath ) ) {
							try {
								tracksToAdd.add( new Track( droppedPath ) );
							} catch ( IOException e ) {
								e.printStackTrace();
							}
						} else if ( Files.isDirectory( droppedPath ) ) {
							tracksToAdd.addAll( Utils.getAllTracksInDirectory( droppedPath ) );
						}
					}
					if ( !tracksToAdd.isEmpty() ) {
						int dropIndex = row.isEmpty() ? dropIndex = queueTable.getItems().size() : row.getIndex();
						queueTable.getItems().addAll( Math.min( dropIndex, queueTable.getItems().size() ), tracksToAdd );
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
					case TRACK_LIST: {
						List <Track> tracksToCopy = container.getTracks();
						player.getQueue().addAllTracks( tracksToCopy );
						
					} break;
					case CURRENT_LIST: {
						//TODO: should I refactor this
						synchronized ( player.getCurrentList() ) {
							ArrayList <CurrentListTrack> tracksToCopy = new ArrayList <CurrentListTrack> (  );
							for ( int index : draggedIndices ) {
								if ( index >= 0 && index < player.getCurrentList().size() ) {
									tracksToCopy.add( player.getCurrentList().get( index ) );
								}
							}
							player.getQueue().addAllTracks( tracksToCopy );
						}
					} break;
					
											
					case QUEUE: {
						ArrayList <Track> tracksToMove = new ArrayList <Track> ( draggedIndices.size() );
						for ( int index : draggedIndices ) {
							if ( index >= 0 && index < queueTable.getItems().size() ) {
								tracksToMove.add( queueTable.getItems().get( index ) );
							}
						}
						
						for ( int k = draggedIndices.size() - 1; k >= 0; k-- ) {
							int index = draggedIndices.get( k ).intValue();
							if ( index >= 0 && index < queueTable.getItems().size() ) {
								player.getQueue().remove ( index );
							}
						}
						
						player.getQueue().addAllTracks( tracksToMove );
						
						queueTable.getSelectionModel().clearSelection();
						for ( int k = 0; k < draggedIndices.size(); k++ ) {
							queueTable.getSelectionModel().select( k );
						}
						
						player.getQueue().updateQueueIndexes( null );
						
					} break;
				}

				player.getQueue().updateQueueIndexes( null );
				event.setDropCompleted( true );
				event.consume();

			} else if ( db.hasFiles() ) {
				ArrayList <Track> tracksToAdd = new ArrayList<Track> ();
				for ( File file : db.getFiles() ) {
					Path droppedPath = Paths.get( file.getAbsolutePath() );
					if ( Utils.isMusicFile( droppedPath ) ) {
						try {
							tracksToAdd.add( new Track( droppedPath ) );
						} catch ( IOException e ) {
							e.printStackTrace();
						}
					} else if ( Files.isDirectory( droppedPath ) ) {
						tracksToAdd.addAll( Utils.getAllTracksInDirectory( droppedPath ) );
					}
				}
				if ( !tracksToAdd.isEmpty() ) {
					queueTable.getItems().addAll( tracksToAdd );
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
		
		Hypnos.library().getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		});

		ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.loadTracks( queueTable.getSelectionModel().getSelectedItems() );
			}
		});

		apendMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.addTracks ( queueTable.getSelectionModel().getSelectedItems() );
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
}
