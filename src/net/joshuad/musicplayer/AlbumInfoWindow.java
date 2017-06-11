package net.joshuad.musicplayer;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.joshuad.musicplayer.DraggedTrackContainer.DragSource;

public class AlbumInfoWindow extends Stage {
	
	Album album;
	TableView <Track> trackTable;
	TextField locationField;
	
	public AlbumInfoWindow( Stage owner ) {
		super();
		this.initModality( Modality.NONE );
		this.initOwner( owner );
		this.setTitle( "Album Info" );
		this.setWidth( 700 );
		Pane root = new Pane();
		Scene scene = new Scene( root );
		VBox primaryPane = new VBox();
		
		setupAlbumTable();
		
		Label label = new Label ( "Location: " );
		label.setAlignment( Pos.CENTER_RIGHT );
		
		locationField = new TextField();
		locationField.setEditable( false );
		locationField.setMaxWidth( Double.MAX_VALUE );
		
		HBox.setHgrow( locationField, Priority.ALWAYS );
		Button browseButton = new Button( "Browse" );
		browseButton.setOnAction( new EventHandler <ActionEvent>() {
			// TODO: This is the better way, once openjdk and openjfx supports
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
							//TODO: 
							e.printStackTrace();
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
	
		root.getChildren().add( primaryPane );
		setScene( scene );
	}

	public void setAlbum ( Album album ) { 
		this.album = album;
		trackTable.setItems( FXCollections.observableArrayList ( album.getTracks() ) );
		locationField.setText( album.getPath().toString() );
	}
	
	@SuppressWarnings("unchecked")
	private void setupAlbumTable () {
		
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
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, DragSource.ALBUM_INFO );
					Dragboard db = row.startDragAndDrop( TransferMode.COPY );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( MusicPlayerUI.DRAGGED_TRACKS, dragObject );
					db.setContent( cc );
					event.consume();
				}
			});

			return row;
		});
	}
}

