package net.joshuad.hypnos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.joshuad.hypnos.DraggedTrackContainer.DragSource;

public class PlaylistInfoWindow extends Stage {
	
	Playlist playlist;
	TableView <Track> trackTable;
	TextField locationField;
	
	public PlaylistInfoWindow( Stage owner ) {
		super();
		this.initModality( Modality.NONE );
		this.initOwner( owner );
		this.setTitle( "Album Info" );
		this.setWidth( 500 );
		Group root = new Group();
		Scene scene = new Scene( root );
		VBox primaryPane = new VBox();
		
		setupPlaylistTable();

		primaryPane.getChildren().addAll( trackTable );
		root.getChildren().add( primaryPane );
		setScene( scene );
	}

	public void setPlaylist ( Playlist playlist ) { 
		this.playlist = playlist;
		if ( playlist != null ) {
			trackTable.setItems( FXCollections.observableArrayList ( playlist.getTracks() ) );
			this.setTitle( "Playlist Info: " + playlist.getName() );
		}
	}
	
	@SuppressWarnings("unchecked")
	private void setupPlaylistTable () {
		
		TableColumn<Track, Integer> trackNumberColumn = new TableColumn<Track, Integer>( "#" );
		TableColumn<Track, String> titleColumn = new TableColumn<Track, String>( "Title" );
		TableColumn<Track, Integer> lengthColumn = new TableColumn<Track, Integer>( "Length" );
		
		trackNumberColumn.setMaxWidth( 70000 );
		titleColumn.setMaxWidth( 500000 );
		lengthColumn.setMaxWidth( 90000 );
		
		trackNumberColumn.setEditable( false );
		titleColumn.setEditable( false );
		lengthColumn.setEditable( false );
		
		trackNumberColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "trackNumber" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Title" ) );
		lengthColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "LengthDisplay" ) );
		
		trackNumberColumn.setCellFactory( column -> {
			return new TableCell <Track, Integer>() {
				@Override
				protected void updateItem ( Integer value, boolean empty ) {
					super.updateItem( value, empty );

					if ( value == null || value.equals( Track.NO_TRACK_NUMBER ) || empty ) {
						setText( null );
						setStyle( "" );
					} else {
						setText( value.toString() );
					}
				}
			};
		} );
		trackTable = new TableView<Track> ();
		trackTable.getColumns().addAll( trackNumberColumn, titleColumn, lengthColumn );
		trackTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		trackTable.setEditable( true );
		trackTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		
		trackTable.prefWidthProperty().bind( this.widthProperty() );
		
		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem appendMenuItem = new MenuItem( "Append" );
		MenuItem queueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		contextMenu.getItems().addAll ( playMenuItem, appendMenuItem, queueMenuItem, editTagMenuItem, addToPlaylistMenuItem );
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );
		addToPlaylistMenuItem.getItems().add( newPlaylistButton );
		
		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				MusicPlayerUI.promptAndSavePlaylist ( 
					new ArrayList <Track> ( trackTable.getSelectionModel().getSelectedItems() ), 
					false 
				);
			}
		});

		EventHandler <ActionEvent> addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				MusicPlayerUI.addToPlaylist ( trackTable.getSelectionModel().getSelectedItems(), playlist );
			}
		};

		Library.playlistsSorted.addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			MusicPlayerUI.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		});

		MusicPlayerUI.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		queueMenuItem.setOnAction( event -> {
			Queue.addAllTracks( trackTable.getSelectionModel().getSelectedItems() );
		});
		
			
		editTagMenuItem.setOnAction( event -> {
			MusicPlayerUI.tagWindow.setTracks( (List<Track>)(List<?>)trackTable.getSelectionModel().getSelectedItems(), null );
			MusicPlayerUI.tagWindow.show();
		});
		
		appendMenuItem.setOnAction( event -> {
			MusicPlayerUI.currentListTable.getItems().addAll( Utils.convertTrackList( trackTable.getSelectionModel().getSelectedItems() ) );
		});

		playMenuItem.setOnAction( event -> {
			MusicPlayerUI.playTrack( trackTable.getSelectionModel().getSelectedItem() );
		});
		
		trackTable.setRowFactory( tv -> {
			TableRow <Track> row = new TableRow <>();

			row.setContextMenu( contextMenu );

			row.setOnMouseClicked( event -> {
				//TODO: is this what I want to happen? 
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					MusicPlayerUI.playTrack( row.getItem() );
				}
			} );
			
			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					ArrayList <Integer> indices = new ArrayList <Integer>( trackTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( trackTable.getSelectionModel().getSelectedItems() );
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, DragSource.PLAYLIST_LIST );
					Dragboard db = row.startDragAndDrop( TransferMode.COPY );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( MusicPlayerUI.DRAGGED_TRACKS, dragObject );
					db.setContent( cc );
					event.consume();
				}
			});
			
			row.setOnDragOver( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasContent( MusicPlayerUI.DRAGGED_TRACKS ) || db.hasFiles() ) {
					event.acceptTransferModes( TransferMode.COPY );
					event.consume();
				}
			} );

			row.setOnDragDropped( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasContent( MusicPlayerUI.DRAGGED_TRACKS ) ) {

					DraggedTrackContainer container = (DraggedTrackContainer) db.getContent( MusicPlayerUI.DRAGGED_TRACKS );
					int dropIndex = row.isEmpty() ? dropIndex = trackTable.getItems().size() : row.getIndex();
					
					switch ( container.getSource() ) {
						case ALBUM_LIST:
						case TRACK_LIST:
						case ALBUM_INFO:
						case HISTORY: 
						case CURRENT_LIST:
						case QUEUE: {
							List <Track> tracksToCopy = container.getTracks();
							trackTable.getItems().addAll( dropIndex, tracksToCopy );
							
							Playlist newList = new Playlist ( playlist.getName(), new ArrayList <Track> ( trackTable.getItems() ) );
							Library.removePlaylist( playlist );
							Library.addPlaylist( newList );
							
							playlist = newList;
							
						} break;
						
						case PLAYLIST_LIST: {
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

					event.setDropCompleted( true );
					event.consume();

				} else if ( db.hasFiles() ) {
					ArrayList <CurrentListTrack> tracksToAdd = new ArrayList <CurrentListTrack>();
					
					for ( File file : db.getFiles() ) {
						Path droppedPath = Paths.get( file.getAbsolutePath() );
						if ( Utils.isMusicFile( droppedPath ) ) {
							try {
								tracksToAdd.add( new CurrentListTrack( droppedPath ) );
							} catch ( CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e ) {
								e.printStackTrace();
							}
						
						} else if ( Files.isDirectory( droppedPath ) ) {
							tracksToAdd.addAll( Utils.convertTrackList( Utils.getAllTracksInDirectory( droppedPath ) ) );
						
						} else if ( Utils.isPlaylistFile ( droppedPath ) ) {
							Playlist playlist = Playlist.loadPlaylist( droppedPath );
							if ( playlist != null ) {
								tracksToAdd.addAll( Utils.convertTrackList( playlist.getTracks() ) );
							}
						}
					}
					
					if ( !tracksToAdd.isEmpty() ) {
						int dropIndex = row.isEmpty() ? dropIndex = trackTable.getItems().size() : row.getIndex();
						trackTable.getItems().addAll( Math.min( dropIndex, trackTable.getItems().size() ), tracksToAdd );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			} );
		

			return row;
		});
	}
}




