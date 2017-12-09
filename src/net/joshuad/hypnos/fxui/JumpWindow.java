package net.joshuad.hypnos.fxui;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
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
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.joshuad.hypnos.CurrentListTrack;
import net.joshuad.hypnos.Library;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;

public class JumpWindow extends Stage {
	private static transient final Logger LOGGER = Logger.getLogger( JumpWindow.class.getName() );
	
	private FXUI ui;
	private Library library;
	final AudioSystem player;
	
	private TextField trackFilterBox;
	
	TableView <CurrentListTrack> trackTable;

	final FilteredList <CurrentListTrack> currentListFiltered;
	
	public JumpWindow ( FXUI ui, Library library, AudioSystem player ) {
		super();
		
		this.ui = ui;
		this.library = library;
		this.player = player;
		
		initModality( Modality.NONE );
		initOwner( ui.getMainStage() );
		setTitle( "Jump to Song" );
		setWidth( 600 );
		setHeight( 500 );
		
		Pane root = new Pane();
		Scene scene = new Scene( root );
		VBox primaryPane = new VBox();
		primaryPane.setAlignment( Pos.CENTER );
		
		currentListFiltered = new FilteredList <CurrentListTrack> ( player.getCurrentList().getItems(), p -> true );
		
		HBox trackFilterPane = new HBox();
		trackFilterBox = new TextField();
		trackFilterBox.setPrefWidth( 500000 );
		
		trackFilterBox.textProperty().addListener( new ChangeListener <String> () {

			@Override
			public void changed ( ObservableValue <? extends String> observable, String oldValue, String newValue ) {
				Platform.runLater( () -> {
					currentListFiltered.setPredicate( track -> {
						return acceptTrackFilterChange ( track, oldValue, newValue ); 
					});
					trackTable.getSelectionModel().clearSelection();
					trackTable.getSelectionModel().selectFirst();
				});
			}
		});
		
		trackFilterBox.setOnKeyPressed( ( KeyEvent event ) -> {
			if ( event.getCode() == KeyCode.ESCAPE ) {
				if ( !trackFilterBox.getText().isEmpty() ) {
					trackFilterBox.clear();
				} else {
					this.hide();
				}
				event.consume();
				
			} else if ( event.getCode() == KeyCode.ENTER ) {
				this.hide();
				Track playMe = trackTable.getSelectionModel().getSelectedItem();
				
				if ( event.isShiftDown() ) {
					player.getQueue().queueTrack ( playMe );
					
				} else {
					player.playTrack ( playMe );
				}
				
				event.consume();
				
				//NOTE: I was getting a core dump by calling this directly. putting it in a run later seems to fix it.
				//Exact steps:
				// 0. comment out the platform.runlater code
				// 1. Launch hypnos
				// 2. Click on current list
				// 3. Hit the letter J
				// 4. Press Enter
				// 5. Core Dump immediately every time. 
				Platform.runLater ( () -> { 
					ui.selectTrackOnCurrentList( playMe );
				});
				
			} else if ( event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.UP ) {
				trackTable.fireEvent( event );
				event.consume();
			} 
		});
		
		this.setOnShowing( ( WindowEvent event ) -> {
			trackFilterBox.clear();
			trackTable.getSelectionModel().clearSelection();
			trackTable.getSelectionModel().selectFirst();
			trackFilterBox.requestFocus();
		} );
		
		double width = 33;
		double height = 26;

		trackFilterBox.setPrefHeight( height );

		trackFilterBox.setTooltip ( new Tooltip ( "Filter/Search tracks" ) );
		
		trackFilterPane.getChildren().addAll( trackFilterBox );
		
		TableColumn artistColumn = new TableColumn( "Artist" );
		TableColumn yearColumn = new TableColumn( "Year" );
		TableColumn albumColumn = new TableColumn( "Album" );
		TableColumn titleColumn = new TableColumn( "Title" );

		artistColumn.setMaxWidth( 22000 );
		yearColumn.setMaxWidth( 8000 );
		albumColumn.setMaxWidth( 25000 );
		titleColumn.setMaxWidth( 25000 );
		
		artistColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "artist" ) );
		yearColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, Integer>( "year" ) );
		albumColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "fullAlbumTitle" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <CurrentListTrack, String>( "title" ) );

		trackTable = new TableView<CurrentListTrack>();
		trackTable.getColumns().addAll( artistColumn, yearColumn, albumColumn, titleColumn );
		trackTable.setEditable( false );
		trackTable.setItems( currentListFiltered );
		
		FixedWidthCustomResizePolicy resizePolicy = new FixedWidthCustomResizePolicy();
		trackTable.setColumnResizePolicy( resizePolicy );

		resizePolicy.registerColumns( yearColumn );
		trackTable.setPlaceholder( new Label( "No tracks in playlist." ) );
		trackTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		
		trackTable.setOnDragOver( event -> {
			
			Dragboard db = event.getDragboard();
			
			if ( db.hasContent( FXUI.DRAGGED_TRACKS ) || db.hasFiles() ) {
				event.acceptTransferModes( TransferMode.COPY );
				event.consume();
			}
		});

		trackTable.setOnDragDropped( event -> {
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
						player.getCurrentList().appendTracks ( container.getTracks() );
					
					} break;

					case PLAYLIST_LIST: {
						player.getCurrentList().appendPlaylists ( container.getPlaylists() );
						
					} break;
					
					case ALBUM_LIST: {
						player.getCurrentList().appendAlbums ( container.getAlbums() );
					} break;
					
					case CURRENT_LIST: {
						//There is no meaning in dragging from an empty list to an empty list. 
					} break;
					
					case QUEUE: {
						synchronized ( player.getQueue().getData() ) {
							List <Integer> draggedIndices = container.getIndices();
							
							ArrayList <Track> tracksToCopy = new ArrayList <Track> ( draggedIndices.size() );
							for ( int index : draggedIndices ) {
								if ( index >= 0 && index < player.getQueue().getData().size() ) {
									Track addMe = player.getQueue().getData().get( index );
									if ( addMe instanceof CurrentListTrack ) {
										tracksToCopy.add( (CurrentListTrack)addMe );
									} else {
										CurrentListTrack newAddMe = new CurrentListTrack ( addMe );
										player.getQueue().getData().remove ( index );
										player.getQueue().getData().add( index, newAddMe );
										tracksToCopy.add( newAddMe );
									}
								}
							}
							player.getCurrentList().appendTracks( tracksToCopy );
						}
						
					} break;
				}

				player.getQueue().updateQueueIndexes();
				event.setDropCompleted( true );
				event.consume();


		
			} else if ( db.hasFiles() ) {
				ArrayList <Track> tracksToAdd = new ArrayList<Track>();
				for ( File file : db.getFiles() ) {
					Path droppedPath = Paths.get( file.getAbsolutePath() );
					if ( Utils.isMusicFile( droppedPath ) ) {
						player.getCurrentList().appendTrack ( droppedPath );
						
					} else if ( Files.isDirectory( droppedPath ) ) {
						player.getCurrentList().appendTracksPathList ( Utils.getAllTracksInDirectory( droppedPath ) );
					} else if ( Utils.isPlaylistFile ( droppedPath ) ) {
						Playlist playlist = Playlist.loadPlaylist( droppedPath );
						if ( playlist != null ) {
							player.getCurrentList().appendPlaylist ( playlist );
						}
					}
				}

				event.setDropCompleted( true );
				event.consume();
			}

		});

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem playNextMenuItem = new MenuItem( "Play Next" );
		MenuItem queueMenuItem = new MenuItem( "Enqueue" );
		MenuItem shuffleMenuItem = new MenuItem( "Shuffle Items" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem infoMenuItem = new MenuItem( "Info" );
		MenuItem lyricsMenuItem = new MenuItem( "Lyrics" );
		MenuItem cropMenuItem = new MenuItem( "Crop" );
		MenuItem removeMenuItem = new MenuItem( "Remove" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );

		trackTable.setOnKeyPressed( ( KeyEvent e ) -> {
			
			if ( e.getCode() == KeyCode.ESCAPE
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				trackTable.getSelectionModel().clearSelection();
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
				
			} else if ( e.getCode() == KeyCode.R && e.isShiftDown() //PENDING: Put this on the hotkey list? 
			&& !e.isControlDown() && !e.isAltDown() && !e.isMetaDown() ) {
				shuffleMenuItem.fire();
				e.consume();
			}
		});
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );
		contextMenu.getItems().addAll( 
			playMenuItem, playNextMenuItem, queueMenuItem, shuffleMenuItem, editTagMenuItem, infoMenuItem, lyricsMenuItem,
			browseMenuItem, addToPlaylistMenuItem, cropMenuItem, removeMenuItem 
		);
		
		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				ui.promptAndSavePlaylist ( new ArrayList <Track> ( trackTable.getSelectionModel().getSelectedItems() ) );
			}
		});

		EventHandler<ActionEvent> addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				ui.addToPlaylist ( Utils.convertCurrentTrackList ( trackTable.getSelectionModel().getSelectedItems() ), playlist );
			}
		};

		library.getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );

		
		playNextMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.getQueue().queueAllTracks( trackTable.getSelectionModel().getSelectedItems(), 0 );
			}
		});
		
		queueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.getQueue().queueAllTracks( trackTable.getSelectionModel().getSelectedItems() );
			}
		});
		
		shuffleMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List<Integer> selectedIndices = trackTable.getSelectionModel().getSelectedIndices();
				
				if ( selectedIndices.size() < 2 ) {
					player.shuffleList();
					
				} else {
					player.getCurrentList().shuffleItems( selectedIndices );
				}
			}
		});
		
		editTagMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				
				ui.tagWindow.setTracks( (List<Track>)(List<?>)trackTable.getSelectionModel().getSelectedItems(), null );
				ui.tagWindow.show();
			}
		});
		
		infoMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ui.trackInfoWindow.setTrack( trackTable.getSelectionModel().getSelectedItem() );
				ui.trackInfoWindow.show();
			}
		});
		
		lyricsMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ui.lyricsWindow.setTrack( trackTable.getSelectionModel().getSelectedItem() );
				ui.lyricsWindow.show();
			}
		});

		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				player.playTrack( trackTable.getSelectionModel().getSelectedItem() );
			}
		} );

		browseMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			// PENDING: This is the better way, once openjdk and openjfx supports
			// it: getHostServices().showDocument(file.toURI().toString());
			@Override
			public void handle ( ActionEvent event ) {
				SwingUtilities.invokeLater( new Runnable() {
					public void run () {
						try {
							Desktop.getDesktop().open( trackTable.getSelectionModel().getSelectedItem().getPath().getParent().toFile() );
						} catch ( Exception e ) {
							LOGGER.log( Level.INFO, "Unable to native file browser.", e );
						}
					}
				} );
			}
		});
		
		removeMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ObservableList <Integer> selectedIndexes = trackTable.getSelectionModel().getSelectedIndices();
				List <Integer> removeMe = new ArrayList<> ( selectedIndexes );
				int selectAfterDelete = selectedIndexes.get( 0 ) - 1;
				trackTable.getSelectionModel().clearSelection();
				ui.removeFromCurrentList ( removeMe );
			}
		});
				
		cropMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {

				ObservableList <Integer> selectedIndexes = trackTable.getSelectionModel().getSelectedIndices();
				
				List <Integer> removeMe = new ArrayList<Integer> ( selectedIndexes );
				for ( int k = 0; k < trackTable.getItems().size(); k++ ) {
					if ( !selectedIndexes.contains( k ) ) {
						removeMe.add ( k );
					}
				}
				
				ui.removeFromCurrentList ( removeMe );
				trackTable.getSelectionModel().clearSelection();
			}
		});
		
		trackTable.getSelectionModel().selectedItemProperty().addListener( ( obs, oldSelection, newSelection ) -> {
			ui.setImages ( newSelection );
		});
		
		trackTable.setRowFactory( tv -> {
			TableRow <CurrentListTrack> row = new TableRow <>();
			

			row.setContextMenu( contextMenu );
			
			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && !row.isEmpty() ) {
					player.playTrack( row.getItem() );
				}
			});
			
			row.itemProperty().addListener( (obs, oldValue, newValue ) -> {
				if ( newValue != null ) {
			        if ( newValue.isMissingFile() ) {
			            row.getStyleClass().add( "file-missing" );
			        } else {
			            row.getStyleClass().remove( "file-missing" );
			        }
				}
		    });
			
			row.itemProperty().addListener( ( obs, oldValue, newTrackValue ) -> {
				if ( newTrackValue != null ) {
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
					ArrayList <Integer> indices = new ArrayList <Integer>( trackTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( trackTable.getSelectionModel().getSelectedItems() );
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
							player.getCurrentList().insertAlbums( dropIndex, container.getAlbums() );
						} break;
						
						case PLAYLIST_LIST:
						case TRACK_LIST:
						case ALBUM_INFO:
						case PLAYLIST_INFO:
						case TAG_ERROR_LIST:
						case HISTORY: {
							player.getCurrentList().insertTracks( dropIndex, Utils.convertTrackList( container.getTracks() ) );
						} break;
						
						case CURRENT_LIST: {
							List<Integer> draggedIndices = container.getIndices();
							
							player.getCurrentList().moveTracks ( draggedIndices, dropIndex );
							
							trackTable.getSelectionModel().clearSelection();
							for ( int k = 0; k < draggedIndices.size(); k++ ) {
								int selectIndex = dropIndex + k;
								if ( selectIndex < trackTable.getItems().size() ) {
									trackTable.getSelectionModel().select( dropIndex + k );
								}
							}
						} break;
						
						case QUEUE: {
							synchronized ( player.getQueue().getData() ) {
								List <Integer> draggedIndices = container.getIndices();
								
								ArrayList <CurrentListTrack> tracksToCopy = new ArrayList <CurrentListTrack> ( draggedIndices.size() );
								for ( int index : draggedIndices ) {
									if ( index >= 0 && index < player.getQueue().getData().size() ) {
										Track addMe = player.getQueue().getData().get( index );
										if ( addMe instanceof CurrentListTrack ) {
											tracksToCopy.add( (CurrentListTrack)addMe );
										} else {
											CurrentListTrack newAddMe = new CurrentListTrack ( addMe );
											player.getQueue().getData().remove ( index );
											player.getQueue().getData().add( index, newAddMe );
											tracksToCopy.add( newAddMe );
										}
									}
								}
								player.getCurrentList().insertTracks ( dropIndex, tracksToCopy );
							}
							
						} break;
					}

					player.getQueue().updateQueueIndexes( );
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
						int dropIndex = row.isEmpty() ? dropIndex = trackTable.getItems().size() : row.getIndex();
						player.getCurrentList().insertTrackPathList ( dropIndex, tracksToAdd );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});

			return row;
		});
		
		
		primaryPane.getChildren().addAll ( trackFilterPane, trackTable );
		
		trackTable.prefHeightProperty().bind( primaryPane.heightProperty().subtract( trackFilterPane.heightProperty() ) );
		trackTable.prefWidthProperty().bind( primaryPane.widthProperty() );
		
		primaryPane.prefWidthProperty().bind( root.widthProperty() );
		primaryPane.prefHeightProperty().bind( root.heightProperty() );
		
		root.getChildren().add( primaryPane );
		setScene( scene );
	}
	
	public boolean acceptTrackFilterChange ( Track track, Object oldValue, Object newValueIn ) {
		
		String newValue = trackFilterBox.getText();
		if ( newValueIn instanceof String ) {
			newValue = (String)newValueIn;
		}
	
		if ( newValue == null || newValue.isEmpty() ) {
			return true;
		}
		
		String[] lowerCaseFilterTokens = newValue.toLowerCase().split( "\\s+" );

		ArrayList <String> matchableText = new ArrayList <String>();

		matchableText.add( Normalizer.normalize( track.getArtist(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
		matchableText.add( track.getArtist().toLowerCase() );
		matchableText.add( Normalizer.normalize( track.getTitle(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
		matchableText.add( track.getTitle().toLowerCase() );
		matchableText.add( Normalizer.normalize( track.getFullAlbumTitle(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
		matchableText.add( track.getFullAlbumTitle().toLowerCase() );

		for ( String token : lowerCaseFilterTokens ) {
			boolean tokenMatches = false;
			for ( String test : matchableText ) {
				if ( test.contains( token ) ) {
					tokenMatches = true;
				}
			}

			if ( !tokenMatches ) {
				return false;
			}
		}

		return true;
	}
}

