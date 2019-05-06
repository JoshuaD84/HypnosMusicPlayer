package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableColumn.SortType;
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
import net.joshuad.hypnos.Persister;
import net.joshuad.hypnos.AlphanumComparator.CaseHandling;
import net.joshuad.hypnos.Persister.Setting;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;
import net.joshuad.hypnos.library.Album;
import net.joshuad.hypnos.library.Library;
import net.joshuad.hypnos.library.Playlist;
import net.joshuad.hypnos.library.Track;

public class LibraryAlbumPane extends BorderPane {
private static final Logger LOGGER = Logger.getLogger( LibraryArtistPane.class.getName() );
	
	private FXUI ui;
	private AudioSystem audioSystem;
	private Library library;
	
	private HBox filterPane;
	TextField filterBox;
	
	TableView<Album> albumTable;	

	//TODO: are these all actually strings? 
	TableColumn<Album, String> artistColumn, yearColumn, addedDateColumn, albumColumn;
	
	ContextMenu columnSelectorMenu;
	
	Label emptyListLabel = new Label( 
		"No albums loaded. To add to your library, click on the + button or drop folders here." );
	Label filteredListLabel = new Label( "No albums match." );
	Label noColumnsLabel = new Label( "All columns hidden." );
	Label loadingListLabel = new Label( "Loading..." );
	
	private ThrottledAlbumFilter tableFilter;
	
	private ImageView addSourceImage, filterClearImage;
	
	public LibraryAlbumPane ( FXUI ui, AudioSystem audioSystem, Library library ) {
		this.ui = ui;
		this.audioSystem = audioSystem;
		this.library = library;
		
		setupFilterPane();
		setupTable();
		
		filterPane.prefWidthProperty().bind( filterPane.widthProperty() );
		setTop( filterPane );
		setCenter( albumTable );
		
		library.getAlbumsSorted().addListener( new ListChangeListener<Album>() {
			@Override
			public void onChanged(Change<? extends Album> arg0) {
				updatePlaceholders();
			}
		});
		
		artistColumn.visibleProperty().addListener( e -> updatePlaceholders() );
		yearColumn.visibleProperty().addListener( e -> updatePlaceholders() );
		addedDateColumn.visibleProperty().addListener( e -> updatePlaceholders() );
		albumColumn.visibleProperty().addListener( e -> updatePlaceholders() );
		
		resetTableSettingsToDefault();
	}
	
	public void updatePlaceholders() {
		Platform.runLater( () -> {
			boolean someVisible = false;
			for ( TableColumn<?,?> column : albumTable.getColumns() ) {
				if ( column.isVisible() ) {
					someVisible = true;
					break;
				}
			}
			
			if ( !someVisible ) {
				albumTable.setPlaceholder( noColumnsLabel );
			} else if ( library.getAlbums().isEmpty() ) {
				if ( albumTable.getPlaceholder() != emptyListLabel ) {
					albumTable.setPlaceholder( emptyListLabel );
				}
			} else {
				if ( !albumTable.getPlaceholder().equals( filteredListLabel ) ) {
					albumTable.setPlaceholder( filteredListLabel );
				}
			}
		});
	}
	
	public void resetTableSettingsToDefault() {
		artistColumn.setVisible( true );
		addedDateColumn.setVisible( false );
		yearColumn.setVisible( true );
		albumColumn.setVisible( true );
		
		albumTable.getColumns().remove( artistColumn );
		albumTable.getColumns().add( artistColumn );
		albumTable.getColumns().remove( yearColumn );
		albumTable.getColumns().add( yearColumn );
		albumTable.getColumns().remove( addedDateColumn );
		albumTable.getColumns().add( addedDateColumn );
		albumTable.getColumns().remove( albumColumn );
		albumTable.getColumns().add( albumColumn );

		//Note: setSortType needs to be called before getSortOrder().add
		artistColumn.setSortType( SortType.ASCENDING );
		albumColumn.setSortType( SortType.ASCENDING );
		yearColumn.setSortType( SortType.ASCENDING );
		addedDateColumn.setSortType( SortType.ASCENDING );
		
		albumTable.getSortOrder().clear();
		albumTable.getSortOrder().add( artistColumn );
		albumTable.getSortOrder().add( yearColumn );
		albumTable.getSortOrder().add( albumColumn );
		
		artistColumn.setPrefWidth( 100 );
		yearColumn.setPrefWidth( 60 );
		albumColumn.setPrefWidth( 100 );
		addedDateColumn.setPrefWidth ( 90 );
		
		albumTable.getColumnResizePolicy().call( new ResizeFeatures<Album> ( albumTable, null, 0d ) );
		updatePlaceholders();
	}
	
	
	public void setupTable () {
		artistColumn = new TableColumn<Album, String>( "Artist" );
		yearColumn = new TableColumn<Album, String>( "Year" );
		albumColumn = new TableColumn<Album, String>( "Album" );
		addedDateColumn = new TableColumn<Album, String> ( "Added" );

		artistColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		albumColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );

		artistColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "albumArtist" ) );
		yearColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "year" ) );
		albumColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "FullAlbumTitle" ) );
		addedDateColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "dateAddedString" ) );
		
		albumColumn.setCellFactory( e -> new FormattedAlbumCell<> () );
		
		columnSelectorMenu = new ContextMenu ();
		CheckMenuItem artistMenuItem = new CheckMenuItem ( "Show Artist Column" );
		CheckMenuItem yearMenuItem = new CheckMenuItem ( "Show Year Column" );
		CheckMenuItem albumMenuItem = new CheckMenuItem ( "Show Album Column" );
		CheckMenuItem dateAddedMenuItem = new CheckMenuItem ( "Show Added Column" );
		MenuItem defaultMenuItem = new MenuItem ( "Reset to Default View" );
		
		artistMenuItem.setSelected( true );
		yearMenuItem.setSelected( true );
		albumMenuItem.setSelected( true );
		dateAddedMenuItem.setSelected( false );
		columnSelectorMenu.getItems().addAll( artistMenuItem, yearMenuItem, dateAddedMenuItem, albumMenuItem, defaultMenuItem );
		artistColumn.setContextMenu( columnSelectorMenu );
		yearColumn.setContextMenu( columnSelectorMenu );
		albumColumn.setContextMenu( columnSelectorMenu );
		addedDateColumn.setContextMenu( columnSelectorMenu );
		artistMenuItem.selectedProperty().bindBidirectional( artistColumn.visibleProperty() );
		yearMenuItem.selectedProperty().bindBidirectional( yearColumn.visibleProperty() );
		albumMenuItem.selectedProperty().bindBidirectional( albumColumn.visibleProperty() );
		dateAddedMenuItem.selectedProperty().bindBidirectional( addedDateColumn.visibleProperty() );
		defaultMenuItem.setOnAction( e -> this.resetTableSettingsToDefault() );

		albumTable = new TableView<Album>();
		albumTable.getColumns().addAll( artistColumn, yearColumn, addedDateColumn, albumColumn );
		albumTable.setEditable( false );
		albumTable.setItems( library.getAlbumsSorted() );
		albumTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		library.getAlbumsSorted().comparatorProperty().bind( albumTable.comparatorProperty() );
		
		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		albumTable.setColumnResizePolicy( resizePolicy );
		resizePolicy.registerFixedWidthColumns( yearColumn );
		resizePolicy.registerFixedWidthColumns( addedDateColumn );
		
		emptyListLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyListLabel.setWrapText( true );
		emptyListLabel.setTextAlignment( TextAlignment.CENTER );
		
		filteredListLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		filteredListLabel.setWrapText( true );
		filteredListLabel.setTextAlignment( TextAlignment.CENTER );
		
		albumTable.setPlaceholder( emptyListLabel );

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem appendMenuItem = new MenuItem( "Append" );
		MenuItem playNextMenuItem = new MenuItem( "Play Next" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem sortByNewestMenuItem = new MenuItem ( "Sort by Add Date" );
		MenuItem rescanMenuItem = new MenuItem ( "Rescan" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		MenuItem infoMenuItem = new MenuItem( "Track List" );
		
		albumTable.setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ESCAPE 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				if ( filterBox.getText().length() > 0 ) {
					filterBox.clear();
					Platform.runLater( ()-> albumTable.scrollTo( albumTable.getSelectionModel().getSelectedItem() ) );
				} else {
					albumTable.getSelectionModel().clearSelection();
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
				infoMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.F4
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				browseMenuItem.fire();
				e.consume();
				
			} else if ( e.getCode() == KeyCode.ENTER
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				playMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				audioSystem.getCurrentList().insertAlbums( 0, albumTable.getSelectionModel().getSelectedItems() );
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				appendMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.UP ) {
				if ( albumTable.getSelectionModel().getSelectedIndex() == 0 ) {
					filterBox.requestFocus();
				}
			}
		});
		
		contextMenu.getItems().addAll( 
			playMenuItem, appendMenuItem, playNextMenuItem, enqueueMenuItem, editTagMenuItem, infoMenuItem, 
			sortByNewestMenuItem, rescanMenuItem, browseMenuItem, addToPlaylistMenuItem
		);
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			
			@Override
			public void handle ( ActionEvent e ) {
				ObservableList <Album> selectedAlbums = albumTable.getSelectionModel().getSelectedItems();
				ArrayList <Track> tracks = new ArrayList <Track> ();
				
				for ( Album album : selectedAlbums ) {
					tracks.addAll( album.getTracks() );
				}
				ui.promptAndSavePlaylist ( tracks );
			}
		});
		
		

		EventHandler<ActionEvent> addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				
				ArrayList <Album> albums = new ArrayList <Album> ( albumTable.getSelectionModel().getSelectedItems() );
				ArrayList <Track> tracksToAdd = new ArrayList <Track> ();
				
				for ( Album album : albums ) {
					tracksToAdd.addAll( album.getTracks() );
				}
				
				ui.addToPlaylist ( tracksToAdd, playlist );
			}
		};

		library.getPlaylistsSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( event -> {
			if ( ui.okToReplaceCurrentList() ) {
				List <Album> playMe = albumTable.getSelectionModel().getSelectedItems();
				audioSystem.getCurrentList().setAndPlayAlbums( playMe );
			}
		});

		appendMenuItem.setOnAction( event -> {
			audioSystem.getCurrentList().appendAlbums( albumTable.getSelectionModel().getSelectedItems() );
		});

		playNextMenuItem.setOnAction( event -> {
			audioSystem.getQueue().queueAllAlbums( albumTable.getSelectionModel().getSelectedItems(), 0 );
		});
		
		enqueueMenuItem.setOnAction( event -> {
			audioSystem.getQueue().queueAllAlbums( albumTable.getSelectionModel().getSelectedItems() );
		});
		
		sortByNewestMenuItem.setOnAction( event -> {
			albumTable.getSortOrder().clear();
			addedDateColumn.setSortType( SortType.DESCENDING );
			albumTable.getSortOrder().add( addedDateColumn );
		});
		
		editTagMenuItem.setOnAction( event -> {
			List<Album> albums = albumTable.getSelectionModel().getSelectedItems();
			ArrayList<Track> editMe = new ArrayList<Track>();
			
			for ( Album album : albums ) {
				if ( album != null ) {
					editMe.addAll( album.getTracks() );
				}
			}
			
			ui.tagWindow.setTracks( editMe, albums, FieldKey.TRACK, FieldKey.TITLE );
			ui.tagWindow.show();
		});
		
		infoMenuItem.setOnAction( event -> {
			ui.albumInfoWindow.setAlbum( albumTable.getSelectionModel().getSelectedItem() );
			ui.albumInfoWindow.show();
		});
		
		rescanMenuItem.setOnAction( event -> {
			library.requestRescan( albumTable.getSelectionModel().getSelectedItems() );
		});

		browseMenuItem.setOnAction( event -> {
			ui.openFileBrowser ( albumTable.getSelectionModel().getSelectedItem().getPath() );
		});
		
		albumTable.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();
			
			if ( db.hasFiles() ) {
				event.acceptTransferModes( TransferMode.COPY );
				event.consume();
			} 
		});
		
		albumTable.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				List <File> files = db.getFiles();
				
				for ( File file : files ) {
					library.addMusicRoot( file.toPath() );
				}

				event.setDropCompleted( true );
				event.consume();
			}
		});
		
		albumTable.getSelectionModel().selectedItemProperty().addListener( ( obs, oldSelection, newSelection ) -> {
			
		    if ( newSelection != null ) {
		    	ui.albumSelected ( newSelection );
		    	
		    } else if ( audioSystem.getCurrentTrack() != null ) {
		    	ui.trackSelected ( audioSystem.getCurrentTrack() );
		    	
		    } else {
		    	//Do nothing, leave the old artwork there. We can set to null if we like that better
		    	//I don't think so though
		    }
		});

		albumTable.setRowFactory( tv -> {
			TableRow <Album> row = new TableRow <>();
			
			row.itemProperty().addListener( (obs, oldValue, newValue ) -> {
				if ( newValue != null ) {
					row.setContextMenu( contextMenu );
				} else {
					row.setContextMenu( null );
				}
			});

			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					if ( ui.okToReplaceCurrentList() ) {
						audioSystem.getCurrentList().setAndPlayAlbum( row.getItem() );
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
	                    library.addMusicRoot( file.toPath() );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					
					ArrayList <Album> albums = new ArrayList <Album>( albumTable.getSelectionModel().getSelectedItems() );
					ArrayList <Track> tracks = new ArrayList <Track> ();
					
					for ( Album album : albums ) {
						if ( album != null ) {
							tracks.addAll( album.getTracks() );
						}
					}
					
					DraggedTrackContainer dragObject = new DraggedTrackContainer( null, tracks, albums, null, null, DragSource.ALBUM_LIST );
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
	
	
	public void setupFilterPane () {
		tableFilter = new ThrottledAlbumFilter ( library.getAlbumsFiltered() );
		
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
					albumTable.requestFocus();
				}
				
			} else if ( event.getCode() == KeyCode.DOWN ) {
				event.consume();
				albumTable.requestFocus();
				albumTable.getSelectionModel().select( albumTable.getSelectionModel().getFocusedIndex() );
			} else if ( event.getCode() == KeyCode.ENTER && 
			!event.isAltDown() && !event.isShiftDown() && !event.isControlDown() && !event.isMetaDown() ) {
				event.consume();
				Album playMe = albumTable.getSelectionModel().getSelectedItem();
				if( playMe == null ) albumTable.getItems().get( 0 );
				audioSystem.getCurrentList().setAndPlayAlbum( playMe );
			} else if ( event.getCode() == KeyCode.ENTER && event.isShiftDown()
			&& !event.isAltDown() && !event.isControlDown() && !event.isMetaDown() ) {
				event.consume();
				Album playMe = albumTable.getSelectionModel().getSelectedItem();
				if( playMe == null ) albumTable.getItems().get( 0 );
				audioSystem.getQueue().queueAllAlbums( Arrays.asList( playMe ) );
			} else if ( event.getCode() == KeyCode.ENTER && event.isControlDown()
			&& !event.isAltDown() && !event.isShiftDown() && !event.isMetaDown() ) {
				event.consume();
				Album playMe = albumTable.getSelectionModel().getSelectedItem();
				if( playMe == null ) albumTable.getItems().get( 0 );
				audioSystem.getCurrentList().appendAlbum( playMe );
			}
		});
		
		float width = 33;
		float height = 26;

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
	
	public void applyDarkTheme ( ColorAdjust buttonColor ) {
		if ( filterClearImage != null ) filterClearImage.setEffect( buttonColor );
		if ( addSourceImage != null ) addSourceImage.setEffect( buttonColor );
	}

	public void applyLightTheme () {
		if ( filterClearImage != null ) filterClearImage.setEffect( ui.lightThemeButtonEffect );
		if ( addSourceImage != null ) addSourceImage.setEffect( ui.lightThemeButtonEffect );
	}
	
	@SuppressWarnings("incomplete-switch")
	public void applySettingsBeforeWindowShown ( EnumMap<Persister.Setting, String> settings ) {
		settings.forEach( ( setting, value )-> {
			try {
				switch ( setting ) {
					case AL_TABLE_ARTIST_COLUMN_SHOW:
						artistColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case AL_TABLE_YEAR_COLUMN_SHOW:
						yearColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case AL_TABLE_ALBUM_COLUMN_SHOW:
						albumColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;					
					case AL_TABLE_ADDED_COLUMN_SHOW:
						addedDateColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case AL_TABLE_ARTIST_COLUMN_WIDTH: 
						artistColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case AL_TABLE_YEAR_COLUMN_WIDTH: 
						yearColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case AL_TABLE_ADDED_COLUMN_WIDTH: 
						addedDateColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case AL_TABLE_ALBUM_COLUMN_WIDTH: 
						albumColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case ALBUM_COLUMN_ORDER: {
						String[] order = value.split( " " );
						int newIndex = 0;
						
						for ( String columnName : order ) {
							try {
								if ( columnName.equals( "artist" ) ) {
									albumTable.getColumns().remove( artistColumn );
									albumTable.getColumns().add( newIndex, artistColumn );
								} else if ( columnName.equals( "year" ) ) {
									albumTable.getColumns().remove( yearColumn );
									albumTable.getColumns().add( newIndex, yearColumn );
								} else if ( columnName.equals( "album" ) ) {
									albumTable.getColumns().remove( albumColumn );
									albumTable.getColumns().add( newIndex, albumColumn );
								} else if ( columnName.equals( "added" ) ) {
									albumTable.getColumns().remove( addedDateColumn );
									albumTable.getColumns().add( newIndex, addedDateColumn );
								}
								newIndex++;
							} catch ( Exception e ) {
								LOGGER.log( Level.INFO, "Unable to set album table column order: '" + value + "'", e );
							}
							
						}
						settings.remove ( setting );
						break;
					}

					case ALBUM_SORT_ORDER: {
						albumTable.getSortOrder().clear();
						
						if ( !value.equals( "" ) ) {
							String[] order = value.split( " " );
							for ( String fullValue : order ) {
								try {
									String columnName = fullValue.split( "-" )[0];
									SortType sortType = SortType.valueOf( fullValue.split( "-" )[1] );
									
									if ( columnName.equals( "artist" ) ) {
										albumTable.getSortOrder().add( artistColumn );
										artistColumn.setSortType( sortType );
									} else if ( columnName.equals( "year" ) ) {
										albumTable.getSortOrder().add( yearColumn );
										yearColumn.setSortType( sortType );
									} else if ( columnName.equals( "album" ) ) {
										albumTable.getSortOrder().add( albumColumn );
										albumColumn.setSortType( sortType );
									} else if ( columnName.equals( "added" ) ) {
										albumTable.getSortOrder().add( addedDateColumn );
										addedDateColumn.setSortType( sortType );
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
	
	public EnumMap<Persister.Setting, ? extends Object> getSettings () {

		EnumMap <Persister.Setting, Object> retMe = new EnumMap <Persister.Setting, Object> ( Persister.Setting.class );
		
		String albumColumnOrderValue = "";
		for ( TableColumn<Album, ?> column : albumTable.getColumns() ) {
			if ( column == artistColumn ) {
				albumColumnOrderValue += "artist ";
			} else if ( column == yearColumn ) {
				albumColumnOrderValue += "year ";
			} else if ( column == addedDateColumn ) {
				albumColumnOrderValue += "added ";
			} else if ( column == albumColumn ) {
				albumColumnOrderValue += "album ";
			}
		}
		retMe.put ( Setting.ALBUM_COLUMN_ORDER, albumColumnOrderValue );
		
		String albumSortValue = "";
		for ( TableColumn<Album, ?> column : albumTable.getSortOrder() ) {
			if ( column == artistColumn ) {
				albumSortValue += "artist-" + artistColumn.getSortType() + " ";
			} else if ( column == yearColumn ) {
				albumSortValue += "year-" + yearColumn.getSortType() + " ";
			} else if ( column == albumColumn ) {
				albumSortValue += "album-" + albumColumn.getSortType() + " ";
			} else if ( column == this.addedDateColumn ) {
				albumSortValue += "added-" + addedDateColumn.getSortType() + " ";
			}
		}
		retMe.put ( Setting.ALBUM_SORT_ORDER, albumSortValue );
		
		retMe.put ( Setting.AL_TABLE_ARTIST_COLUMN_SHOW, artistColumn.isVisible() );
		retMe.put ( Setting.AL_TABLE_YEAR_COLUMN_SHOW, yearColumn.isVisible() );
		retMe.put ( Setting.AL_TABLE_ALBUM_COLUMN_SHOW, albumColumn.isVisible() );
		retMe.put ( Setting.AL_TABLE_ADDED_COLUMN_SHOW, addedDateColumn.isVisible() );
		retMe.put ( Setting.AL_TABLE_ARTIST_COLUMN_WIDTH, artistColumn.getPrefWidth() );
		retMe.put ( Setting.AL_TABLE_YEAR_COLUMN_WIDTH, yearColumn.getPrefWidth() );
		retMe.put ( Setting.AL_TABLE_ADDED_COLUMN_WIDTH, addedDateColumn.getPrefWidth() );
		retMe.put ( Setting.AL_TABLE_ALBUM_COLUMN_WIDTH, albumColumn.getPrefWidth() );
				
		return retMe;
	}

	public void focusFilter() {
		albumTable.requestFocus();
		filterBox.requestFocus();
		albumTable.getSelectionModel().clearSelection();
	}
}
