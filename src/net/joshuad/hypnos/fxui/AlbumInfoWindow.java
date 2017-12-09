package net.joshuad.hypnos.fxui;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.joshuad.hypnos.Album;
import net.joshuad.hypnos.Library;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;

public class AlbumInfoWindow extends Stage {

	private static final Logger LOGGER = Logger.getLogger( AlbumInfoWindow.class.getName() );
	
	Album album;
	TableView <Track> trackTable;
	TextField locationField;
	FXUI ui;
	AudioSystem player;
	Library library;
	
	public AlbumInfoWindow( FXUI ui, Library library, AudioSystem player ) {
		super();
		this.ui = ui;
		this.library = library;
		this.player = player;
		
		this.initModality( Modality.NONE );
		this.initOwner( ui.getMainStage() );
		this.setTitle( "Album Info" );
		this.setWidth( 700 );
		Pane root = new Pane();
		Scene scene = new Scene( root );
		VBox primaryPane = new VBox();
		
		setupAlbumTable( primaryPane );
		
		Label label = new Label ( "Location: " );
		label.setAlignment( Pos.CENTER_RIGHT );
		
		locationField = new TextField();
		locationField.setEditable( false );
		locationField.setMaxWidth( Double.MAX_VALUE );
		
		HBox.setHgrow( locationField, Priority.ALWAYS );
		Button browseButton = new Button( "Browse" );
		browseButton.setOnAction( new EventHandler <ActionEvent>() {
			// PENDING: Future - This is the better way, once openjdk and openjfx supports
			// it: getHostServices().showDocument(file.toURI().toString());
			@Override
			public void handle ( ActionEvent event ) {
				SwingUtilities.invokeLater( new Runnable() {
					public void run () {
						try {
							if ( album != null ) {
								Path path = album.getPath();
								if ( path != null ) {
									Desktop.getDesktop().open( path.toFile() );
								}
							}
						} catch ( IOException e ) {
							LOGGER.info( "Unable to open directory in desktop environment." );
						}
					}
				} );
			}
		} );
		
		HBox locationBox = new HBox();
		locationBox.getChildren().addAll( label, locationField, browseButton );
		label.prefHeightProperty().bind( locationBox.heightProperty() );
		locationBox.prefWidthProperty().bind( primaryPane.widthProperty() );
	
		primaryPane.getChildren().addAll( locationBox, trackTable );
	
		
		primaryPane.prefWidthProperty().bind( root.widthProperty() );
		primaryPane.prefHeightProperty().bind( root.heightProperty() );
		

		setWidth ( 700 );
		setHeight ( 500 );
		
		root.getChildren().add( primaryPane );
		setScene( scene );
	}

	public void setAlbum ( Album album ) { 
		this.album = album;
		trackTable.setItems( FXCollections.observableArrayList ( album.getTracks() ) );
		locationField.setText( album.getPath().toString() );
	}
	
	@SuppressWarnings("unchecked")
	private void setupAlbumTable ( VBox primaryPane ) {
		
		TableColumn<Track, Integer> trackNumberColumn = new TableColumn<Track, Integer>( "#" );
		TableColumn<Track, String> titleColumn = new TableColumn<Track, String>( "Title" );
		TableColumn<Track, Integer> lengthColumn = new TableColumn<Track, Integer>( "Length" );
		TableColumn<Track, String> fileColumn = new TableColumn<Track, String>( "Filename" );
		TableColumn<Track, String> encodingColumn = new TableColumn<Track, String>( "Encoding" );
		
		trackNumberColumn.setMaxWidth( 70000 );
		titleColumn.setMaxWidth( 500000 );
		lengthColumn.setMaxWidth( 90000 );
		fileColumn.setMaxWidth( 500000 );
		encodingColumn.setMaxWidth( 180000 );
		
		trackNumberColumn.setEditable( false );
		titleColumn.setEditable( false );
		lengthColumn.setEditable( false );
		fileColumn.setEditable( false );
		encodingColumn.setEditable( false );
		
		trackNumberColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "trackNumber" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Title" ) );
		lengthColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "LengthDisplay" ) );
		fileColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Filename" ) );
		encodingColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "ShortEncodingString" ) );
		
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
		trackTable.getColumns().addAll( trackNumberColumn, titleColumn, lengthColumn, fileColumn, encodingColumn );
		trackTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		trackTable.setEditable( true );
		trackTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		
		trackTable.prefWidthProperty().bind( primaryPane.widthProperty() );
		trackTable.prefHeightProperty().bind( primaryPane.heightProperty() );
		
		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem appendMenuItem = new MenuItem( "Append" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem infoMenuItem = new MenuItem( "Info" );
		MenuItem lyricsMenuItem = new MenuItem( "Lyrics" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		contextMenu.getItems().addAll ( 
			playMenuItem, appendMenuItem, enqueueMenuItem, editTagMenuItem, infoMenuItem, lyricsMenuItem, addToPlaylistMenuItem 
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

		library.getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		});

		ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		enqueueMenuItem.setOnAction( event -> {
			player.getQueue().queueAllTracks( trackTable.getSelectionModel().getSelectedItems() );
		});
			
		editTagMenuItem.setOnAction( event -> {
			ui.tagWindow.setTracks( (List<Track>)(List<?>)trackTable.getSelectionModel().getSelectedItems(), null );
			ui.tagWindow.show();
		});
		
		appendMenuItem.setOnAction( event -> {
			player.getCurrentList().appendTracks ( trackTable.getSelectionModel().getSelectedItems() );
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
		
		playMenuItem.setOnAction( event -> {
			List <Track> selectedItems =  new ArrayList<Track> ( trackTable.getSelectionModel().getSelectedItems() );
			
			if ( selectedItems.size() == 1 ) {
				player.playItems( selectedItems );
				
			} else if ( selectedItems.size() > 1 ) {
				if ( ui.okToReplaceCurrentList() ) {
					player.playItems( selectedItems );
				}
			}
		});
		
		trackTable.setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ESCAPE 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				trackTable.getSelectionModel().clearSelection();
				
			} else if ( e.getCode() == KeyCode.Q 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				enqueueMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.F2 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				editTagMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.F3
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				infoMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.ENTER
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				playMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isShiftDown() 
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				appendMenuItem.fire();
				
			}
				
		});
		
		trackTable.setRowFactory( tv -> {
			TableRow <Track> row = new TableRow <>();

			row.setContextMenu( contextMenu );

			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					player.playTrack( row.getItem() );
				}
			} );

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					ArrayList <Integer> indices = new ArrayList <Integer>( trackTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( trackTable.getSelectionModel().getSelectedItems() );
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, null, null, DragSource.ALBUM_INFO );
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
	}
}

