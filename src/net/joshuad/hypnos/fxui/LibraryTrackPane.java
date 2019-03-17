package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView.ResizeFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.ColorAdjust;
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
import net.joshuad.hypnos.AlphanumComparator;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Library;
import net.joshuad.hypnos.MusicSearchLocation;
import net.joshuad.hypnos.Persister;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.AlphanumComparator.CaseHandling;
import net.joshuad.hypnos.Persister.Setting;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;

public class LibraryTrackPane extends BorderPane {
	private static final Logger LOGGER = Logger.getLogger( LibraryTrackPane.class.getName() );
	private FXUI ui;
	private AudioSystem audioSystem;
	private Library library;

	CheckBox filterAlbumsCheckBox;
	ThrottledTrackFilter tableFilter;
	TextField filterBox;
	HBox filterPane;

	ContextMenu columnSelectorMenu;
	
	TableView <Track> trackTable;
	
	TableColumn<Track, String> artistColumn, lengthColumn, albumColumn, titleColumn;
	TableColumn<Track, Integer> numberColumn;

	Label filteredListLabel = new Label( "No tracks match." );
	Label loadingListLabel = new Label( "Loading..." );
	Label noColumnsLabel = new Label( "All columns hidden." );
	Label emptyListLabel = new Label( 
		"No tracks loaded. To add to your library, click on the + button or drop folders here." );

	private ImageView addSourceImage, filterClearImage;
	
	public LibraryTrackPane ( FXUI ui, AudioSystem audioSystem, Library library ) {
		this.library = library;
		this.audioSystem = audioSystem;
		this.ui = ui;
		setupCheckBox();
		setupTrackFilterPane();
		setupTrackTable();
		resetTableSettingsToDefault();
		
		filterPane.prefWidthProperty().bind( widthProperty() );
		setTop( filterPane );
		setCenter( trackTable );
		
		library.getTracksSorted().addListener( new ListChangeListener<Track>() {
			@Override
			public void onChanged(Change<? extends Track> arg0) {
				updatePlaceholders();
			}
		});
		
		artistColumn.visibleProperty().addListener( e -> updatePlaceholders() );
		lengthColumn.visibleProperty().addListener( e -> updatePlaceholders() );
		albumColumn.visibleProperty().addListener( e -> updatePlaceholders() );
		titleColumn.visibleProperty().addListener( e -> updatePlaceholders() );
		numberColumn.visibleProperty().addListener( e -> updatePlaceholders() );
		
		resetTableSettingsToDefault();
	}
	
	public void updatePlaceholders() {
		Platform.runLater( () -> {
			boolean someVisible = false;
			for ( TableColumn<?,?> column : trackTable.getColumns() ) {
				if ( column.isVisible() ) {
					someVisible = true;
					break;
				}
			}
			
			if ( !someVisible ) {
				trackTable.setPlaceholder( noColumnsLabel );
			} else if ( library.getAlbums().isEmpty() ) {
				if ( trackTable.getPlaceholder() != emptyListLabel ) {
					trackTable.setPlaceholder( emptyListLabel );
				}
			} else {
				if ( !trackTable.getPlaceholder().equals( filteredListLabel ) ) {
					trackTable.setPlaceholder( filteredListLabel );
				}
			}
		});
	}
	
	public void setupCheckBox() {
		filterAlbumsCheckBox = new CheckBox( "" );
		filterAlbumsCheckBox.selectedProperty().addListener( new ChangeListener <Boolean> () {
			@Override
			public void changed( ObservableValue <? extends Boolean> observable, Boolean oldValue, Boolean newValue ) {
				tableFilter.setFilter( filterBox.getText(), newValue );
			}
		});
		filterAlbumsCheckBox.getStyleClass().add ( "filterAlbumsCheckBox" );
		
		filterAlbumsCheckBox.setTooltip( new Tooltip( "Only show tracks not in albums" ) );
	}
	
	public void setupTrackFilterPane () {
		tableFilter = new ThrottledTrackFilter ( library.getTracksFiltered() );
		
		filterPane = new HBox();
		filterBox = new TextField();
		filterBox.setPrefWidth( 500000 );
		
		filterBox.textProperty().addListener( new ChangeListener <String> () {
			@Override
			public void changed ( ObservableValue <? extends String> observable, String oldValue, String newValue ) {
				tableFilter.setFilter( newValue, filterAlbumsCheckBox.isSelected() );
			}
		});
		
		filterBox.setOnKeyPressed( ( KeyEvent event ) -> {
			if ( event.getCode() == KeyCode.ESCAPE ) {
				
				filterBox.clear();
				if ( filterBox.getText().length() > 0 ) {
					filterBox.clear();
				} else {
					trackTable.requestFocus();
				}
				event.consume();
				
			} else if ( event.getCode() == KeyCode.DOWN ) {
				trackTable.requestFocus();
				trackTable.getSelectionModel().select( trackTable.getSelectionModel().getFocusedIndex() );
				
			} else if ( event.getCode() == KeyCode.ENTER && 
			!event.isAltDown() && !event.isShiftDown() && !event.isControlDown() && !event.isMetaDown() ) {
				event.consume();
				Track playMe = trackTable.getSelectionModel().getSelectedItem();
				if( playMe == null ) trackTable.getItems().get( 0 );
				audioSystem.playTrack( playMe );
				
			} else if ( event.getCode() == KeyCode.ENTER && event.isShiftDown()
			&& !event.isAltDown() && !event.isControlDown() && !event.isMetaDown() ) {
				event.consume();
				Track playMe = trackTable.getSelectionModel().getSelectedItem();
				if( playMe == null ) trackTable.getItems().get( 0 );
				audioSystem.getQueue().queueTrack( playMe );
				
			} else if ( event.getCode() == KeyCode.ENTER && event.isControlDown()
			&& !event.isAltDown() && !event.isShiftDown() && !event.isMetaDown() ) {
				event.consume();
				Track playMe = trackTable.getSelectionModel().getSelectedItem();
				if( playMe == null ) trackTable.getItems().get( 0 );
				audioSystem.getCurrentList().appendTrack( playMe );
			}
		});
		
		double width = 33;
		double height = 26;

		filterBox.setPrefHeight( height );
		String addLocation = "resources/add.png";
		String clearLocation = "resources/clear.png";
		try {		
			double currentListControlsButtonFitWidth = 15;
			double currentListControlsButtonFitHeight = 15;
			Image image = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( addLocation ).toFile() ) );
			addSourceImage = new ImageView ( image );
			addSourceImage.setFitWidth( currentListControlsButtonFitWidth );
			addSourceImage.setFitHeight( currentListControlsButtonFitHeight );
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load add icon: " + addLocation, e );
		}
		
		try {
			Image clearImage = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( clearLocation ).toFile() ) );
			filterClearImage = new ImageView ( clearImage );
			filterClearImage.setFitWidth( 12 );
			filterClearImage.setFitHeight( 12 );
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load clear icon: " + clearLocation, e );
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
		} );
		
		Button clearButton = new Button ( );
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
		filterBox.setTooltip ( new Tooltip ( "Filter/Search tracks" ) );
		clearButton.setTooltip( new Tooltip( "Clear the filter text" ) );
		
		HBox checkBoxMargins = new HBox();
		checkBoxMargins.setPadding( new Insets ( 4, 0, 0, 6 ) );
		checkBoxMargins.getChildren().add( filterAlbumsCheckBox );
		
		filterPane.getChildren().addAll( libraryButton, filterBox, clearButton, checkBoxMargins );
	}
	
	public void setupTrackTable () {
		artistColumn = new TableColumn<Track, String>( "Artist" );
		lengthColumn = new TableColumn<Track, String>( "Length" );
		numberColumn = new TableColumn<Track, Integer>( "#" );
		albumColumn = new TableColumn<Track, String>( "Album" );
		titleColumn = new TableColumn<Track, String>( "Title" );
		
		artistColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		titleColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		lengthColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );

		artistColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Artist" ) );
		titleColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Title" ) );
		lengthColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "LengthDisplay" ) );
		numberColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "TrackNumber" ) );
		albumColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "albumTitle" ) );
		
		artistColumn.setSortType( TableColumn.SortType.ASCENDING );

		numberColumn.setCellFactory( column -> {
			return new TableCell <Track, Integer>() {
				@Override
				protected void updateItem ( Integer value, boolean empty ) {
					super.updateItem( value, empty );

					if ( value == null || value.equals( Track.NO_TRACK_NUMBER ) || empty ) {
						setText( null );
					} else {
						setText( value.toString() );
					}
				}
			};
		} );
		
		columnSelectorMenu = new ContextMenu ();
		CheckMenuItem artistMenuItem = new CheckMenuItem ( "Show Artist Column" );
		CheckMenuItem albumMenuItem = new CheckMenuItem ( "Show Album Column" );
		CheckMenuItem numberMenuItem = new CheckMenuItem ( "Show Track # Column" );
		CheckMenuItem titleMenuItem = new CheckMenuItem ( "Show Title Column" );
		CheckMenuItem lengthMenuItem = new CheckMenuItem ( "Show Length Column" );
		MenuItem defaultMenuItem = new MenuItem ( "Reset to Default View" );
		artistMenuItem.setSelected( true );
		albumMenuItem.setSelected( true );
		numberMenuItem.setSelected( true );
		titleMenuItem.setSelected( true );
		lengthMenuItem.setSelected( true );
		columnSelectorMenu.getItems().addAll( 
			artistMenuItem, albumMenuItem, numberMenuItem, titleMenuItem, lengthMenuItem, defaultMenuItem );
		
		artistColumn.setContextMenu( columnSelectorMenu );
		albumColumn.setContextMenu( columnSelectorMenu );
		titleColumn.setContextMenu( columnSelectorMenu );
		numberColumn.setContextMenu( columnSelectorMenu );
		lengthColumn.setContextMenu( columnSelectorMenu );
		artistMenuItem.selectedProperty().bindBidirectional( artistColumn.visibleProperty() );
		albumMenuItem.selectedProperty().bindBidirectional( albumColumn.visibleProperty() );
		numberMenuItem.selectedProperty().bindBidirectional( numberColumn.visibleProperty() );
		titleMenuItem.selectedProperty().bindBidirectional( titleColumn.visibleProperty() );
		lengthMenuItem.selectedProperty().bindBidirectional( lengthColumn.visibleProperty() );
		defaultMenuItem.setOnAction( ( e ) -> this.resetTableSettingsToDefault() );
		
		trackTable = new TableView<Track>();
		trackTable.getColumns().addAll( artistColumn, albumColumn, numberColumn, titleColumn, lengthColumn );
		
		trackTable.setEditable( false );
		trackTable.setItems( library.getTracksSorted() );

		library.getTracksSorted().comparatorProperty().bind( trackTable.comparatorProperty() );
		
		trackTable.getSelectionModel().clearSelection();
		
		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		trackTable.setColumnResizePolicy( resizePolicy );
		resizePolicy.registerFixedWidthColumns( numberColumn, lengthColumn );
		
		emptyListLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyListLabel.setWrapText( true );
		emptyListLabel.setTextAlignment( TextAlignment.CENTER );
		trackTable.setPlaceholder( emptyListLabel );
		
		trackTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

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
		
		ContextMenu trackContextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem playNextMenuItem = new MenuItem( "Play Next" );
		MenuItem appendMenuItem = new MenuItem( "Append" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem infoMenuItem = new MenuItem( "Info" );
		MenuItem lyricsMenuItem = new MenuItem( "Lyrics" );
		MenuItem goToAlbumMenuItem = new MenuItem( "Go to Album" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		trackContextMenu.getItems().addAll ( 
			playMenuItem, playNextMenuItem, appendMenuItem, enqueueMenuItem, editTagMenuItem, 
			infoMenuItem, lyricsMenuItem, goToAlbumMenuItem, browseMenuItem, addToPlaylistMenuItem,
			lastFMMenu );
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				ui.promptAndSavePlaylist ( trackTable.getSelectionModel().getSelectedItems() );
			}
		});

		EventHandler<ActionEvent> addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				ui.addToPlaylist ( trackTable.getSelectionModel().getSelectedItems(), playlist );
			}
		};

		library.getPlaylistsSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List <Track> selectedItems = new ArrayList <> ( trackTable.getSelectionModel().getSelectedItems() );
				
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
				audioSystem.getCurrentList().appendTracks ( trackTable.getSelectionModel().getSelectedItems() );
			}
		});
		
		playNextMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				audioSystem.getQueue().queueAllTracks( trackTable.getSelectionModel().getSelectedItems(), 0 );
			}
		});
		
		enqueueMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				audioSystem.getQueue().queueAllTracks( trackTable.getSelectionModel().getSelectedItems() );
			}
		});
		
		editTagMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				List<Track> tracks = trackTable.getSelectionModel().getSelectedItems();
				
				ui.tagWindow.setTracks( tracks, null );
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

		goToAlbumMenuItem.setOnAction( ( event ) -> {
			ui.goToAlbumOfTrack ( trackTable.getSelectionModel().getSelectedItem() );
		});
		
		browseMenuItem.setOnAction( new EventHandler <ActionEvent>() {
			// PENDING: This is the better way, once openjdk and openjfx supports
			// it: getHostServices().showDocument(file.toURI().toString());
			@Override
			public void handle ( ActionEvent event ) {
				ui.openFileBrowser ( trackTable.getSelectionModel().getSelectedItem().getPath() );
			}
		});
		
		trackTable.setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ESCAPE 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				if ( filterBox.getText().length() > 0 ) {
					filterBox.clear();
					Platform.runLater( ()-> trackTable.scrollTo( trackTable.getSelectionModel().getSelectedItem() ) );
				} else {
					trackTable.getSelectionModel().clearSelection();
				}
			} else if ( e.getCode() == KeyCode.L
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				lyricsMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.Q 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				enqueueMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.Q && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown()  && !e.isMetaDown() ) {
				playNextMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.G && e.isControlDown()
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				goToAlbumMenuItem.fire();
							
			} else if ( e.getCode() == KeyCode.F2 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				editTagMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.F3
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				infoMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.F4
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				browseMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.ENTER
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				playMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				audioSystem.getCurrentList().insertTracks( 0, trackTable.getSelectionModel().getSelectedItems() );
				e.consume();
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isControlDown() 
			&& !e.isShiftDown() && !e.isAltDown() && !e.isMetaDown() ) {
				appendMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.UP ) {
				if ( trackTable.getSelectionModel().getSelectedIndex() == 0 ) {
					filterBox.requestFocus();
				}
			}
		});
		
		trackTable.getSelectionModel().selectedItemProperty().addListener( ( obs, oldSelection, newSelection ) -> {
		    if (newSelection != null) {
		    	ui.trackSelected ( newSelection );
		    	
		    } else if ( audioSystem.getCurrentTrack() != null ) {
		    	ui.trackSelected ( audioSystem.getCurrentTrack() );
		    	
		    } else {
		    	//Do nothing, leave the old artwork there. We can set to null if we like that better, 
		    	//I don't think so though
		    }
		});
		
		trackTable.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				event.acceptTransferModes( TransferMode.COPY );
				event.consume();

			}
		});
		
		trackTable.setOnDragDropped( event -> {
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

		trackTable.setRowFactory( tv -> {

			TableRow <Track> row = new TableRow <>();
			
			row.itemProperty().addListener( (obs, oldValue, newValue ) -> {
				if ( newValue != null ) {
					row.setContextMenu( trackContextMenu );
				} else {
					row.setContextMenu( null );
				}
			});
			
			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					audioSystem.playTrack( row.getItem(), false );
				}
			});
			
			row.setOnContextMenuRequested( event -> { 
				goToAlbumMenuItem.setDisable( row.getItem().getAlbumPath() == null );
			});
			
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
					ArrayList <Integer> indices = new ArrayList <Integer>( trackTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( trackTable.getSelectionModel().getSelectedItems() );
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, null, null, null, DragSource.TRACK_LIST );
					Dragboard db = row.startDragAndDrop( TransferMode.COPY );
					db.setDragView( row.snapshot( null, null ) );
					ClipboardContent cc = new ClipboardContent();
					cc.put( FXUI.DRAGGED_TRACKS, dragObject );
					db.setContent( cc );
					event.consume();
				}
			});

			return row;
		} );
	}
	
	public void resetTableSettingsToDefault() {
		artistColumn.setVisible( true );
		lengthColumn.setVisible( false );
		numberColumn.setVisible( true );
		albumColumn.setVisible( true );
		titleColumn.setVisible( true );
		
		trackTable.getColumns().remove( artistColumn );
		trackTable.getColumns().add( artistColumn );
		trackTable.getColumns().remove( albumColumn );
		trackTable.getColumns().add( albumColumn );
		trackTable.getColumns().remove( numberColumn );
		trackTable.getColumns().add( numberColumn );
		trackTable.getColumns().remove( titleColumn );
		trackTable.getColumns().add( titleColumn );
		trackTable.getColumns().remove( lengthColumn );
		trackTable.getColumns().add( lengthColumn );

		trackTable.getSortOrder().clear();
		artistColumn.setSortType( SortType.ASCENDING );
		albumColumn.setSortType( SortType.ASCENDING );
		numberColumn.setSortType( SortType.ASCENDING );
		trackTable.getSortOrder().add( artistColumn );
		trackTable.getSortOrder().add( albumColumn );
		trackTable.getSortOrder().add( numberColumn );
		
		artistColumn.setPrefWidth( 100 );
		numberColumn.setPrefWidth( 40 );
		albumColumn.setPrefWidth( 100 );
		lengthColumn.setPrefWidth( 60 );
		titleColumn.setPrefWidth( 100 );
		trackTable.getColumnResizePolicy().call(new ResizeFeatures<Track> ( trackTable, null, 0d ) );
	}
	
	@SuppressWarnings("incomplete-switch")
	public void applySettingsBeforeWindowShown ( EnumMap<Persister.Setting, String> settings ) {
		settings.forEach( ( setting, value )-> {
			try {
				switch ( setting ) {
					case HIDE_ALBUM_TRACKS:
						ui.runThreadSafe ( () -> filterAlbumsCheckBox.setSelected( Boolean.valueOf ( value ) ) );
						settings.remove ( setting );
						break;		
					case TR_TABLE_ARTIST_COLUMN_SHOW:
						artistColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_NUMBER_COLUMN_SHOW:
						numberColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_TITLE_COLUMN_SHOW:
						titleColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_ALBUM_COLUMN_SHOW:
						albumColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_LENGTH_COLUMN_SHOW:
						lengthColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_ARTIST_COLUMN_WIDTH: 
						artistColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
					break;
					case TR_TABLE_NUMBER_COLUMN_WIDTH: 
						numberColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_TITLE_COLUMN_WIDTH:
						titleColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_ALBUM_COLUMN_WIDTH:
						albumColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_LENGTH_COLUMN_WIDTH:
						lengthColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case TRACK_COLUMN_ORDER: {
						String[] order = value.split( " " );
						int newIndex = 0;

						for ( String columnName : order ) {
							try {
								if ( columnName.equals( "artist" ) ) {
									trackTable.getColumns().remove( artistColumn );
									trackTable.getColumns().add( newIndex, artistColumn );
								} else if ( columnName.equals( "length" ) ) {
									trackTable.getColumns().remove( lengthColumn );
									trackTable.getColumns().add( newIndex, lengthColumn );
								} else if ( columnName.equals( "number" ) ) {
									trackTable.getColumns().remove( numberColumn );
									trackTable.getColumns().add( newIndex, numberColumn );
								} else if ( columnName.equals( "album" ) ) {
									trackTable.getColumns().remove( albumColumn );
									trackTable.getColumns().add( newIndex, albumColumn );
								} else if ( columnName.equals( "title" ) ) {
									trackTable.getColumns().remove( titleColumn );
									trackTable.getColumns().add( newIndex, titleColumn );
								} 
								newIndex++;
							} catch ( Exception e ) {
								LOGGER.log( Level.INFO, "Unable to set album table column order: '" + value + "'", e );
							}
							
						}
						settings.remove ( setting );
						break;
					}
					
					case TRACK_SORT_ORDER: {
						trackTable.getSortOrder().clear();
						
						if ( !value.equals( "" ) ) {
							String[] order = value.split( " " );
							for ( String fullValue : order ) {
								try {
									String columnName = fullValue.split( "-" )[0];
									SortType sortType = SortType.valueOf( fullValue.split( "-" )[1] );

									if ( columnName.equals( "artist" ) ) {
										trackTable.getSortOrder().add( artistColumn );
										artistColumn.setSortType( sortType );
									} else if ( columnName.equals( "length" ) ) {
										trackTable.getSortOrder().add( lengthColumn );
										lengthColumn.setSortType( sortType );
									} else if ( columnName.equals( "number" ) ) {
										trackTable.getSortOrder().add( numberColumn );
										numberColumn.setSortType( sortType );
									} else if ( columnName.equals( "album" ) ) {
										trackTable.getSortOrder().add( albumColumn );
										albumColumn.setSortType( sortType );
									} else if ( columnName.equals( "title" ) ) {
										trackTable.getSortOrder().add( titleColumn );
										titleColumn.setSortType( sortType );
									}
									
								} catch ( Exception e ) {
									LOGGER.log( Level.INFO, "Unable to set album table sort order: '" + value + "'", e );
								}
							}
						}
						settings.remove ( setting );
						break;
					}
					
				}
			} catch ( Exception e ) {
				LOGGER.log( Level.INFO, "Unable to apply setting: " + setting + " to UI.", e );
			}
		});
	}
	
	public void applyDarkTheme ( ColorAdjust buttonColor ) {
		if ( filterClearImage != null ) filterClearImage.setEffect( buttonColor );
		if ( addSourceImage != null ) addSourceImage.setEffect( buttonColor );
	}

	public void applyLightTheme () {
		if ( filterClearImage != null ) filterClearImage.setEffect( ui.lightThemeButtonEffect );
		if ( addSourceImage != null ) addSourceImage.setEffect( ui.lightThemeButtonEffect );
	}
	
	public EnumMap<Persister.Setting, ? extends Object> getSettings () {

		EnumMap <Persister.Setting, Object> retMe = new EnumMap <Persister.Setting, Object> ( Persister.Setting.class );
		
		String trackColumnOrderValue = "";
		for ( TableColumn<Track, ?> column : trackTable.getColumns() ) {
			if ( column == artistColumn ) {
				trackColumnOrderValue += "artist ";
			} else if ( column == lengthColumn ) {
				trackColumnOrderValue += "length ";
			} else if ( column == numberColumn ) {
				trackColumnOrderValue += "number ";
			} else if ( column == albumColumn ) {
				trackColumnOrderValue += "album ";
			} else if ( column == titleColumn ) {
				trackColumnOrderValue += "title ";
			} 
		}
		retMe.put ( Setting.TRACK_COLUMN_ORDER, trackColumnOrderValue );
		
		String trackSortValue = "";
		for ( TableColumn<Track, ?> column : trackTable.getSortOrder() ) {
			if ( column == artistColumn ) {
				trackSortValue += "artist-" + artistColumn.getSortType() + " ";
			} else if ( column == lengthColumn ) {
				trackSortValue += "length-" + lengthColumn.getSortType() + " ";
			} else if ( column == numberColumn ) {
				trackSortValue += "number-" + numberColumn.getSortType() + " ";
			} else if ( column == albumColumn ) {
				trackSortValue += "album-" + albumColumn.getSortType() + " ";
			} else if ( column == titleColumn ) {
				trackSortValue += "title-" + titleColumn.getSortType() + " ";
			} 
		}
		retMe.put ( Setting.TRACK_SORT_ORDER, trackSortValue );
		
		retMe.put ( Setting.TR_TABLE_ARTIST_COLUMN_SHOW, artistColumn.isVisible() );
		retMe.put ( Setting.TR_TABLE_NUMBER_COLUMN_SHOW, numberColumn.isVisible() );
		retMe.put ( Setting.TR_TABLE_TITLE_COLUMN_SHOW, titleColumn.isVisible() );
		retMe.put ( Setting.TR_TABLE_ALBUM_COLUMN_SHOW, albumColumn.isVisible() );
		retMe.put ( Setting.TR_TABLE_LENGTH_COLUMN_SHOW, lengthColumn.isVisible() );
		retMe.put ( Setting.TR_TABLE_ARTIST_COLUMN_WIDTH, artistColumn.getPrefWidth() );
		retMe.put ( Setting.TR_TABLE_NUMBER_COLUMN_WIDTH, numberColumn.getPrefWidth() );
		retMe.put ( Setting.TR_TABLE_TITLE_COLUMN_WIDTH, titleColumn.getPrefWidth() );
		retMe.put ( Setting.TR_TABLE_ALBUM_COLUMN_WIDTH, albumColumn.getPrefWidth() );
		retMe.put ( Setting.TR_TABLE_LENGTH_COLUMN_WIDTH, lengthColumn.getPrefWidth() );
		retMe.put ( Setting.HIDE_ALBUM_TRACKS, filterAlbumsCheckBox.isSelected() );
		return retMe;
	}
	
	public void focusFilter() {
		trackTable.requestFocus();
		filterBox.requestFocus();
		trackTable.getSelectionModel().clearSelection();
	}
}
