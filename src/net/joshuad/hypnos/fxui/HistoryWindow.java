package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
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
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Library;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;

public class HistoryWindow extends Stage {

	private static final Logger LOGGER = Logger.getLogger( HistoryWindow.class.getName() );
	

	private TableView <Track> historyTable;
	
	public HistoryWindow ( FXUI ui, Library library, AudioSystem audioSystem ) {
		super();
		
		initModality( Modality.NONE );
		initOwner( ui.getMainStage() );
		setTitle( "History" );
		setWidth( 600 );
		setHeight( 400 );
		
		try {
			getIcons().add( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) ) );
		} catch ( FileNotFoundException e ) {
			LOGGER.warning( "Unable to load program icon: resources/icon.png" );
		}
		
		Pane root = new Pane();
		Scene scene = new Scene( root );
		VBox primaryPane = new VBox();

		historyTable = new TableView<Track>();
		Label emptyLabel = new Label( "History is empty." );
		emptyLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyLabel.setWrapText( true );
		emptyLabel.setTextAlignment( TextAlignment.CENTER );
		
		historyTable.getSelectionModel().selectedItemProperty().addListener( ( obs, oldSelection, newSelection ) -> {
			ui.trackSelected ( newSelection );
		});

		historyTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		historyTable.setPlaceholder( emptyLabel );
		historyTable.setItems( audioSystem.getHistory().getItems() );
		historyTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		TableColumn<Track, Integer> numberColumn = new TableColumn<Track, Integer>( "#" );
		TableColumn<Track, String> artistColumn = new TableColumn<Track, String>( "Artist" );
		TableColumn<Track, String> albumColumn = new TableColumn<Track, String>( "Album" );
		TableColumn<Track, String> titleColumn = new TableColumn<Track, String>( "Title" );
		
		
		numberColumn.setMaxWidth( 10000 );
		artistColumn.setMaxWidth( 30000 );
		albumColumn.setMaxWidth ( 30000 );
		titleColumn.setMaxWidth ( 30000 );
		
		Menu lastFMMenu = new Menu( "LastFM" );
		MenuItem loveMenuItem = new MenuItem ( "Love" );
		MenuItem unloveMenuItem = new MenuItem ( "Unlove" );
		MenuItem scrobbleMenuItem = new MenuItem ( "Scrobble" );
		lastFMMenu.getItems().addAll ( loveMenuItem, unloveMenuItem, scrobbleMenuItem );
		lastFMMenu.setVisible ( false );
		lastFMMenu.visibleProperty().bind( ui.showLastFMWidgets );
		
		loveMenuItem.setOnAction( ( event ) -> {
			audioSystem.getLastFM().loveTrack( historyTable.getSelectionModel().getSelectedItem() );
		});
		
		unloveMenuItem.setOnAction( ( event ) -> {
			audioSystem.getLastFM().unloveTrack( historyTable.getSelectionModel().getSelectedItem() );
		});
		
		scrobbleMenuItem.setOnAction( ( event ) -> {
			audioSystem.getLastFM().scrobbleTrack( historyTable.getSelectionModel().getSelectedItem() );
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
		MenuItem removeMenuItem = new MenuItem( "Remove from History" );
		contextMenu.getItems().addAll( 
			playMenuItem, appendMenuItem, playNextMenuItem, enqueueMenuItem,
			editTagMenuItem, infoMenuItem, lyricsMenuItem, goToAlbumMenuItem, 
			browseMenuItem, addToPlaylistMenuItem, lastFMMenu, removeMenuItem 
		);
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		historyTable.setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ESCAPE 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				if ( historyTable.getSelectionModel().getSelectedItems().size() > 0 ) {
					historyTable.getSelectionModel().clearSelection();
					e.consume();
				} else {
					this.hide();
				}
			
			} else if ( e.getCode() == KeyCode.Q 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				enqueueMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.G && e.isControlDown()
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				goToAlbumMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.Q && e.isShiftDown() 
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				playNextMenuItem.fire();
				e.consume();
						
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
				audioSystem.getCurrentList().insertTracks( 0, historyTable.getSelectionModel().getSelectedItems() );
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isControlDown()
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				appendMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.DELETE
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				removeMenuItem.fire();
				e.consume();
			} 
		});
		
		historyTable.setRowFactory( tv -> {
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
					audioSystem.playTrack( row.getItem(), false );
				}
			});
			
			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					ArrayList <Integer> indices = new ArrayList <Integer>( historyTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( historyTable.getSelectionModel().getSelectedItems() );
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, null, null, null, DragSource.HISTORY );
					Dragboard db = row.startDragAndDrop( TransferMode.COPY );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( FXUI.DRAGGED_TRACKS, dragObject );
					db.setContent( cc );
					event.consume();
				}
			} );
			
			return row;
		});

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				ui.promptAndSavePlaylist ( historyTable.getSelectionModel().getSelectedItems() );
			}
		});

		EventHandler<ActionEvent> addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				ui.addToPlaylist ( historyTable.getSelectionModel().getSelectedItems(), playlist );
			}
		};
		
		library.getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		});

		ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List <Track> selectedItems =  new ArrayList<Track> ( historyTable.getSelectionModel().getSelectedItems() );
				
				if ( selectedItems.size() == 1 ) {
					audioSystem.playItems( selectedItems );
					
				} else if ( selectedItems.size() > 1 ) {
					if ( ui.okToReplaceCurrentList() ) {
						audioSystem.playItems( selectedItems );
					}
				}
			}
		});
		
		playNextMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				audioSystem.getQueue().queueAllTracks( historyTable.getSelectionModel().getSelectedItems(), 0 );
			}
		});
		
		enqueueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				audioSystem.getQueue().queueAllTracks( historyTable.getSelectionModel().getSelectedItems() );
			}
		});
		
		infoMenuItem.setOnAction( event -> {
			ui.trackInfoWindow.setTrack( historyTable.getSelectionModel().getSelectedItem() );
			ui.trackInfoWindow.show();
		});

		lyricsMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				ui.lyricsWindow.setTrack( historyTable.getSelectionModel().getSelectedItem() );
				ui.lyricsWindow.show();
			}
		});
		appendMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				audioSystem.getCurrentList().appendTracks ( historyTable.getSelectionModel().getSelectedItems() );
			}
		});
		
		editTagMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List<Track> tracks = historyTable.getSelectionModel().getSelectedItems();
				
				ui.tagWindow.setTracks( tracks, null );
				ui.tagWindow.show();
			}
		});

		removeMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				synchronized ( historyTable.getItems() ) {
					List<Integer> selectedIndices = historyTable.getSelectionModel().getSelectedIndices();
					
					ArrayList<Integer> removeMeIndices = new ArrayList<Integer> ( selectedIndices );
					
					for ( int k = removeMeIndices.size() - 1; k >= 0 ; k-- ) {
						//REFACTOR: probably put the remove code somewhere else. 
						historyTable.getItems().remove( removeMeIndices.get( k ).intValue() );
					}
				}
			}
		});
		
		goToAlbumMenuItem.setOnAction( ( event ) -> {
			ui.goToAlbumOfTrack ( historyTable.getSelectionModel().getSelectedItem() );
		});

		browseMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			// PENDING: This is the better way, once openjdk and openjfx supports
			// it: getHostServices().showDocument(file.toURI().toString());
			@Override
			public void handle ( ActionEvent event ) {
				ui.openFileBrowser( historyTable.getSelectionModel().getSelectedItem().getPath() );
			}
		});
		
		
		numberColumn.setCellValueFactory( new Callback <CellDataFeatures <Track, Integer>, ObservableValue <Integer>>() {
			@Override
			public ObservableValue <Integer> call ( CellDataFeatures <Track, Integer> p ) {
				return new ReadOnlyObjectWrapper <Integer>( p.getValue(), "number" );
			}
		});

		numberColumn.setCellFactory( new Callback <TableColumn <Track, Integer>, TableCell <Track, Integer>>() {
			@Override
			public TableCell <Track, Integer> call ( TableColumn <Track, Integer> param ) {
				return new TableCell <Track, Integer>() {
					@Override
					protected void updateItem ( Integer item, boolean empty ) {
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
		artistColumn.setSortable(false);
		albumColumn.setSortable(false);
		titleColumn.setSortable(false);
		
		artistColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Artist" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Title" ) );
		albumColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "FullAlbumTitle" ) );
		
		historyTable.getColumns().addAll( numberColumn, artistColumn, albumColumn, titleColumn );

		historyTable.prefWidthProperty().bind( root.widthProperty() );
		historyTable.prefHeightProperty().bind( root.heightProperty() );
		
		primaryPane.getChildren().addAll( historyTable );
		root.getChildren().add( primaryPane );
		setScene( scene );
		
	}

	public void refresh () {
		for ( Track track : historyTable.getItems() ) {
			try {
				track.refreshTagData();
			} catch ( Exception e ) {}
		}
	}
}
