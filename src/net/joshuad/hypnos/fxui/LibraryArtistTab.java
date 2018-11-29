package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jaudiotagger.tag.FieldKey;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableView.ResizeFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextAlignment;
import net.joshuad.hypnos.Album;
import net.joshuad.hypnos.AlphanumComparator;
import net.joshuad.hypnos.Library;
import net.joshuad.hypnos.MusicSearchLocation;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.AlphanumComparator.CaseHandling;
import net.joshuad.hypnos.Artist;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;

public class LibraryArtistTab extends Tab {
	private static final Logger LOGGER = Logger.getLogger( LibraryArtistTab.class.getName() );
	
	private FXUI ui;
	private AudioSystem audioSystem;
	private Library library;
	
	private BorderPane content;
	private HBox filterPane;
	TextField filterBox;
	
	TableView<Artist> artistTable;	
	TableColumn<Artist, String> nameColumn;
	TableColumn<Artist, Integer> tracksColumn, albumsColumn, lengthColumn;
	
	Label emptyListLabel = new Label( 
		"No artists loaded. To add to your library, click on the + button or drop folders here." );
	Label filteredListLabel = new Label( "No artists match." );
	Label loadingListLabel = new Label( "Loading..." );
	
	private ThrottledArtistFilter tableFilter;
	
	private ImageView addSourceImage, filterClearImage;
	
	public LibraryArtistTab ( FXUI ui, AudioSystem audioSystem, Library library ) {
		super ( "Artists" );
		this.ui = ui;
		this.audioSystem = audioSystem;
		this.library = library;
		
		setupFilterPane();
		artistTable = setupArtistTable();
		
		content = new BorderPane();
		filterPane.prefWidthProperty().bind( filterPane.widthProperty() );
		content.setTop( filterPane );
		content.setCenter( artistTable );
		
		this.setClosable( false );
		this.setContent( content );
	}
	
	public void resetTableSettingsToDefault() {
		nameColumn.setVisible( true );
		albumsColumn.setVisible( false );
		tracksColumn.setVisible( true );
		lengthColumn.setVisible( true );
		
		artistTable.getColumns().remove( nameColumn );
		artistTable.getColumns().add( nameColumn );
		artistTable.getColumns().remove( tracksColumn );
		artistTable.getColumns().add( tracksColumn );
		artistTable.getColumns().remove( albumsColumn );
		artistTable.getColumns().add( albumsColumn );
		artistTable.getColumns().remove( lengthColumn );
		artistTable.getColumns().add( lengthColumn );

		artistTable.getSortOrder().add( nameColumn );
		artistTable.getSortOrder().add( tracksColumn );
		artistTable.getSortOrder().add( lengthColumn );
		
		nameColumn.setPrefWidth( 100 );
		tracksColumn.setPrefWidth( 60 );
		lengthColumn.setPrefWidth( 100 );
		albumsColumn.setPrefWidth ( 90 );
		
		artistTable.getColumnResizePolicy().call( new ResizeFeatures<Artist> ( artistTable, null, 0d ) );
	}
	
	
	private TableView<Artist> setupArtistTable () {
		nameColumn = new TableColumn<Artist, String>( "Artist" );
		albumsColumn = new TableColumn<Artist, Integer>( "Albums" );
		tracksColumn = new TableColumn<Artist, Integer> ( "Tracks" );
		lengthColumn = new TableColumn<Artist, Integer> ( "Length" );

		nameColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );

		nameColumn.setCellValueFactory( new PropertyValueFactory <Artist, String>( "Name" ) );
		albumsColumn.setCellValueFactory( new PropertyValueFactory <Artist, Integer>( "albumCount" ) );
		tracksColumn.setCellValueFactory( new PropertyValueFactory <Artist, Integer>( "trackCount" ) );
		lengthColumn.setCellValueFactory( new PropertyValueFactory <Artist, Integer>( "length" ) );
		
		lengthColumn.setCellFactory( col -> new TableCell<Artist, Integer> () {
			@Override
			public void updateItem ( Integer length, boolean empty ) {
				super.updateItem( length, empty );
				if ( empty ) {
					setText( null );
				} else {
					setText( Utils.getLengthDisplay( length ) );
				}
			}
		});
		
		ContextMenu columnSelectorMenu = new ContextMenu ();
		CheckMenuItem nameMenuItem = new CheckMenuItem ( "Show Name Column" );
		CheckMenuItem albumsMenuItem = new CheckMenuItem ( "Show Albums Column" );
		CheckMenuItem tracksMenuItem = new CheckMenuItem ( "Show Tracks Column" );
		CheckMenuItem lengthMenuItem = new CheckMenuItem ( "Show Length Column" );
		MenuItem defaultMenuItem = new MenuItem ( "Reset to Default View" );
		
		nameMenuItem.setSelected( true );
		albumsMenuItem.setSelected( true );
		tracksMenuItem.setSelected( true );
		lengthMenuItem.setSelected( false );
		columnSelectorMenu.getItems().addAll( nameMenuItem, albumsMenuItem, tracksMenuItem, lengthMenuItem, defaultMenuItem );
		nameColumn.setContextMenu( columnSelectorMenu );
		albumsColumn.setContextMenu( columnSelectorMenu );
		tracksColumn.setContextMenu( columnSelectorMenu );
		lengthColumn.setContextMenu( columnSelectorMenu );
		nameMenuItem.selectedProperty().bindBidirectional( nameColumn.visibleProperty() );
		albumsMenuItem.selectedProperty().bindBidirectional( albumsColumn.visibleProperty() );
		tracksMenuItem.selectedProperty().bindBidirectional( tracksColumn.visibleProperty() );
		lengthMenuItem.selectedProperty().bindBidirectional( lengthColumn.visibleProperty() );
		defaultMenuItem.setOnAction( e -> this.resetTableSettingsToDefault() );

		TableView<Artist> retMe = new TableView<Artist>();
		retMe.getColumns().addAll( nameColumn, albumsColumn, tracksColumn, lengthColumn );
		retMe.setEditable( false );
		retMe.setItems( library.getArtistsSorted() );
		retMe.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		
		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		retMe.setColumnResizePolicy( resizePolicy );
		resizePolicy.registerFixedWidthColumns( albumsColumn, tracksColumn, lengthColumn );
		albumsColumn.setPrefWidth( 50 );
		tracksColumn.setPrefWidth( 50 );
		lengthColumn.setPrefWidth( 80 );

		library.getArtistsSorted().comparatorProperty().bind( retMe.comparatorProperty() );
		
		emptyListLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyListLabel.setWrapText( true );
		emptyListLabel.setTextAlignment( TextAlignment.CENTER );
		
		filteredListLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		filteredListLabel.setWrapText( true );
		filteredListLabel.setTextAlignment( TextAlignment.CENTER );
		
		retMe.setPlaceholder( emptyListLabel );

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem appendMenuItem = new MenuItem( "Append" );
		MenuItem playNextMenuItem = new MenuItem( "Play Next" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		MenuItem trackListItem = new MenuItem( "Track List" );
		
		retMe.setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ESCAPE 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				if ( filterBox.getText().length() > 0 ) {
					filterBox.clear();
					Platform.runLater( ()-> retMe.scrollTo( retMe.getSelectionModel().getSelectedItem() ) );
				} else {
					retMe.getSelectionModel().clearSelection();
				}
				
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
				trackListItem.fire();
				
			} else if ( e.getCode() == KeyCode.ENTER
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				playMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				audioSystem.getCurrentList().insertArtists( 0, retMe.getSelectionModel().getSelectedItems() );
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				appendMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.UP ) {
				if ( retMe.getSelectionModel().getSelectedIndex() == 0 ) {
					filterBox.requestFocus();
				}
			}
		});
		
		contextMenu.getItems().addAll( 
			playMenuItem, appendMenuItem, playNextMenuItem, enqueueMenuItem, editTagMenuItem, trackListItem, 
			addToPlaylistMenuItem
		);
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			
			@Override
			public void handle ( ActionEvent e ) {
				ObservableList <Artist> selectedArtists = retMe.getSelectionModel().getSelectedItems();
				ArrayList <Track> tracks = new ArrayList <Track> ();
				
				for ( Artist artist : selectedArtists ) {
					tracks.addAll( artist.getAllTracks() );
				}
				ui.promptAndSavePlaylist ( tracks );
			}
		});
		
		

		EventHandler<ActionEvent> addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				
				ArrayList <Artist> artists = new ArrayList <Artist> ( retMe.getSelectionModel().getSelectedItems() );
				ArrayList <Track> tracksToAdd = new ArrayList <Track> ();
				
				for ( Artist artist : artists ) {
					tracksToAdd.addAll( artist.getAllTracks() );
				}
				
				ui.addToPlaylist ( tracksToAdd, playlist );
			}
		};

		library.getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( event -> {
			if ( ui.okToReplaceCurrentList() ) {
				List <Artist> playMe = retMe.getSelectionModel().getSelectedItems();
				audioSystem.getCurrentList().setAndPlayArtists( playMe );
			}
		});

		appendMenuItem.setOnAction( event -> {
			audioSystem.getCurrentList().appendArtists( retMe.getSelectionModel().getSelectedItems() );
		});

		playNextMenuItem.setOnAction( event -> {
			audioSystem.getQueue().queueAllArtists( retMe.getSelectionModel().getSelectedItems(), 0 );
		});
		
		enqueueMenuItem.setOnAction( event -> {
			audioSystem.getQueue().queueAllArtists( retMe.getSelectionModel().getSelectedItems() );
		});
		
		editTagMenuItem.setOnAction( event -> {
			List<Artist> artists = retMe.getSelectionModel().getSelectedItems();
			ArrayList<Track> editMe = new ArrayList<Track>();
			
			for ( Artist artist : artists ) {
				if ( artist != null ) {
					editMe.addAll( artist.getAllTracks() );
				}
			}
			
			ui.tagWindow.setTracks( editMe, null, true, FieldKey.TRACK, FieldKey.TITLE, 
				FieldKey.ALBUM, FieldKey.YEAR, FieldKey.ORIGINAL_YEAR, FieldKey.DISC_SUBTITLE,
				FieldKey.DISC_NO, FieldKey.DISC_TOTAL, FieldKey.MUSICBRAINZ_RELEASE_TYPE, FieldKey.ARTIST );
			ui.tagWindow.show();
		});
		
		trackListItem.setOnAction( event -> {
			ui.artistInfoWindow.setArtist ( artistTable.getSelectionModel().getSelectedItem() );
			ui.artistInfoWindow.show();
		});

		retMe.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();
			
			if ( db.hasFiles() ) {
				event.acceptTransferModes( TransferMode.COPY );
				event.consume();
			} 
		});
		
		retMe.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				List <File> files = db.getFiles();
				
				for ( File file : files ) {
					library.requestAddSource( new MusicSearchLocation ( file.toPath() ) );
				}

				event.setDropCompleted( true );
				event.consume();
			}
		});
		
		retMe.getSelectionModel().selectedItemProperty().addListener( ( obs, oldSelection, newSelection ) -> {
			
		    if ( newSelection != null ) {
		    	ui.artistSelected ( newSelection );
		    	
		    } else if ( audioSystem.getCurrentTrack() != null ) {
		    	ui.trackSelected ( audioSystem.getCurrentTrack() );
		    	
		    } else {
		    	//Do nothing, leave the old artwork there. We can set to null if we like that better
		    	//I don't think so though
		    }
		});

		retMe.setRowFactory( tv -> {
			TableRow <Artist> row = new TableRow <>();
			
			row.setContextMenu( contextMenu );

			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					if ( ui.okToReplaceCurrentList() ) {
						audioSystem.getCurrentList().setAndPlayArtist( row.getItem() );
					}
				}
			} );

			row.setOnDragOver( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasFiles() ) {
					event.acceptTransferModes( TransferMode.COPY );
					event.consume();

				}
			});
			
			row.setOnDragDropped( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasFiles() ) {
					List <File> files = db.getFiles();
					
					for ( File file : files ) {
						library.requestAddSource( new MusicSearchLocation ( file.toPath() ) );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {

					ArrayList <Artist> artists = new ArrayList <Artist>( retMe.getSelectionModel().getSelectedItems() );
					ArrayList <Album> albums = new ArrayList <Album>();
					ArrayList <Track> tracks = new ArrayList <Track> ();
					
					for ( Artist artist : artists ) {
						albums.addAll ( artist.getAlbums() );
						tracks.addAll( artist.getAllTracks() );
					}
					
					DraggedTrackContainer dragObject = new DraggedTrackContainer( null, tracks, albums, null, artists, DragSource.ARTIST_LIST );
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
		
		return retMe;
	}
	
	private void setupFilterPane () {
		tableFilter = new ThrottledArtistFilter ( library.getArtistsFiltered() );
		
		filterPane = new HBox();
		filterBox = new TextField();
		filterBox.setPrefWidth( 500000 );
		
		filterBox.textProperty().addListener( new ChangeListener <String> () {
			@Override
			public void changed ( ObservableValue <? extends String> observable, String oldValue, String newValue ) {
				tableFilter.setFilter( newValue );
			}
		});
		
		filterBox.setOnKeyPressed( ( KeyEvent event ) -> {
			if ( event.getCode() == KeyCode.ESCAPE ) {
				event.consume();
				if ( filterBox.getText().length() > 0 ) {
					filterBox.clear();
				} else {
					artistTable.requestFocus();
				}
				
			} else if ( event.getCode() == KeyCode.DOWN ) {
				event.consume();
				artistTable.requestFocus();
				artistTable.getSelectionModel().select( artistTable.getSelectionModel().getFocusedIndex() );
			} else if ( event.getCode() == KeyCode.ENTER && 
			!event.isAltDown() && !event.isShiftDown() && !event.isControlDown() && !event.isMetaDown() ) {
				event.consume();
				Artist playMe = artistTable.getSelectionModel().getSelectedItem();
				if( playMe == null ) artistTable.getItems().get( 0 );
				audioSystem.getCurrentList().setAndPlayArtist( playMe );
			} else if ( event.getCode() == KeyCode.ENTER && event.isShiftDown()
			&& !event.isAltDown() && !event.isControlDown() && !event.isMetaDown() ) {
				event.consume();
				Artist playMe = artistTable.getSelectionModel().getSelectedItem();
				if( playMe == null ) artistTable.getItems().get( 0 );
				audioSystem.getQueue().queueAllArtists( Arrays.asList( playMe ) );
			} else if ( event.getCode() == KeyCode.ENTER && event.isControlDown()
			&& !event.isAltDown() && !event.isShiftDown() && !event.isMetaDown() ) {
				event.consume();
				Artist playMe = artistTable.getSelectionModel().getSelectedItem();
				if( playMe == null ) artistTable.getItems().get( 0 );
				audioSystem.getCurrentList().appendArtist( playMe );
			}
		});
		
		float width = 33;
		float height = 26;

		filterBox.setPrefHeight( height );
		try {		
			double currentListControlsButtonFitWidth = 15;
			double currentListControlsButtonFitHeight = 15;
			Image image = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/add.png" ).toFile() ) );
			addSourceImage = new ImageView ( image );
			addSourceImage.setFitWidth( currentListControlsButtonFitWidth );
			addSourceImage.setFitHeight( currentListControlsButtonFitHeight );
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load add icon: resources/add.png", e );
		}
		
		try {
			Image clearImage = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/clear.png" ).toFile() ) );
			filterClearImage = new ImageView ( clearImage );
			filterClearImage.setFitWidth( 12 );
			filterClearImage.setFitHeight( 12 );
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load clear icon: resources/clear.png", e );
		}
		
		Button libraryButton = new Button( );
		libraryButton.setGraphic( addSourceImage );
		libraryButton.setMinSize( width, height );
		libraryButton.setPrefSize( width, height );
		libraryButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				if ( ui.libraryLocationWindow.isShowing() ) {
					ui.libraryLocationWindow.hide();
				} else {
					ui.libraryLocationWindow.show();
				}
			}
		});

		Button clearButton = new Button( );
		clearButton.setGraphic( filterClearImage );
		clearButton.setMinSize( width, height );
		clearButton.setPrefSize( width, height );
		clearButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				filterBox.setText( "" );
			}
		});
		

		libraryButton.setTooltip( new Tooltip( "Add or Remove Music Folders" ) );
		filterBox.setTooltip ( new Tooltip ( "Filter/Search albums" ) );
		clearButton.setTooltip( new Tooltip( "Clear the filter text" ) );

		filterPane.getChildren().addAll( libraryButton, filterBox, clearButton );
	}
}
