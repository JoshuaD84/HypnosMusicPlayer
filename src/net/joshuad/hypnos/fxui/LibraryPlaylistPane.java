package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
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
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextAlignment;
import net.joshuad.hypnos.AlphanumComparator;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Library;
import net.joshuad.hypnos.Persister;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.AlphanumComparator.CaseHandling;
import net.joshuad.hypnos.Persister.Setting;
import net.joshuad.hypnos.Playlist.PlaylistRepeatMode;
import net.joshuad.hypnos.Playlist.PlaylistShuffleMode;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;

public class LibraryPlaylistPane extends BorderPane {
	private static final Logger LOGGER = Logger.getLogger( LibraryPlaylistPane.class.getName() );
	private FXUI ui;
	private AudioSystem audioSystem;
	private Library library;
	
	TableView <Playlist> playlistTable;
	HBox FilterPane;
	ContextMenu columnSelectorMenu;
	TableColumn<Playlist, String> nameColumn, lengthColumn;
	TableColumn<Playlist, Integer> tracksColumn;
	
	Label emptyLabel = new Label( 
		"You haven't created any playlists, make a playlist on the right and click the save button." );
		
	Label filteredLabel = new Label( "No playlists match." );
	Label noColumnsLabel = new Label( "All columns hidden." );
	
	Label loadingLabel = new Label( "Loading..." );
	
	TextField filterBox;

	private ImageView addSourceImage, filterClearImage;
	
	public LibraryPlaylistPane ( FXUI ui, AudioSystem audioSystem, Library library ) {
		this.library = library;
		this.audioSystem = audioSystem;
		this.ui = ui;
		
		setupPlaylistFilterPane();
		setupPlaylistTable();
		resetTableSettingsToDefault();
		
		FilterPane.prefWidthProperty().bind( this.widthProperty() );
		this.setTop( FilterPane );
		this.setCenter( playlistTable );
		
		library.getPlaylistsSorted().addListener( new ListChangeListener<Playlist>() {
			@Override
			public void onChanged(Change<? extends Playlist> arg0) {
				updatePlaceholders();
			}
		});
		
		nameColumn.visibleProperty().addListener( e -> updatePlaceholders() );
		lengthColumn.visibleProperty().addListener( e -> updatePlaceholders() );
		tracksColumn.visibleProperty().addListener( e -> updatePlaceholders() );
		resetTableSettingsToDefault();
	}
	
	public void updatePlaceholders() {
		boolean someVisible = false;
		for ( TableColumn<?,?> column : playlistTable.getColumns() ) {
			if ( column.isVisible() ) {
				someVisible = true;
				break;
			}
		}
		
		if ( !someVisible ) {
			playlistTable.setPlaceholder( noColumnsLabel );
		} else if ( library.getAlbums().isEmpty() ) {
			if ( playlistTable.getPlaceholder() != emptyLabel ) {
				playlistTable.setPlaceholder( emptyLabel );
			}
		} else {
			if ( !playlistTable.getPlaceholder().equals( filteredLabel ) ) {
				playlistTable.setPlaceholder( filteredLabel );
			}
		}
	}
	
	public void setupPlaylistFilterPane () {
		FilterPane = new HBox();
		filterBox = new TextField();
		filterBox.setPrefWidth( 500000 );
		
		filterBox.textProperty().addListener( ( observable, oldValue, newValue ) -> {
			Platform.runLater( () -> {
				library.getPlaylistsFiltered().setPredicate( playlist -> {
					if ( newValue == null || newValue.isEmpty() ) {
						return true;
					}
	
					String[] lowerCaseFilterTokens = newValue.toLowerCase().split( "\\s+" );
	
					ArrayList <String> matchableText = new ArrayList <String>();
	
					matchableText.add( Normalizer.normalize( playlist.getName(), Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
					matchableText.add( playlist.getName().toLowerCase() );
	
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
				});
			});
		});
		
		filterBox.setOnKeyPressed( ( KeyEvent event ) -> {
			if ( event.getCode() == KeyCode.ESCAPE ) {
				filterBox.clear();
				if ( filterBox.getText().length() > 0 ) {
					filterBox.clear();
				} else {
					playlistTable.requestFocus();
				}
				event.consume();
			
			} else if ( event.getCode() == KeyCode.DOWN ) {
				playlistTable.requestFocus();
				playlistTable.getSelectionModel().select( playlistTable.getSelectionModel().getFocusedIndex() );
			
			} else if ( event.getCode() == KeyCode.ENTER && 
			!event.isAltDown() && !event.isShiftDown() && !event.isControlDown() && !event.isMetaDown() ) {
				event.consume();
				Playlist playMe = playlistTable.getSelectionModel().getSelectedItem();
				if( playMe == null ) playlistTable.getItems().get( 0 );
				audioSystem.getCurrentList().setAndPlayPlaylist( playMe );
				
			} else if ( event.getCode() == KeyCode.ENTER && event.isShiftDown()
			&& !event.isAltDown() && !event.isControlDown() && !event.isMetaDown() ) {
				event.consume();
				Playlist playMe = playlistTable.getSelectionModel().getSelectedItem();
				if( playMe == null ) playlistTable.getItems().get( 0 );
				audioSystem.getQueue().queueAllPlaylists( Arrays.asList( playMe ) );
				
			} else if ( event.getCode() == KeyCode.ENTER && event.isControlDown()
			&& !event.isAltDown() && !event.isShiftDown() && !event.isMetaDown() ) {
				event.consume();
				Playlist playMe = playlistTable.getSelectionModel().getSelectedItem();
				if( playMe == null ) playlistTable.getItems().get( 0 );
				audioSystem.getCurrentList().appendPlaylist( playMe );
			
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
		libraryButton.setGraphic ( addSourceImage );
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
		filterBox.setTooltip ( new Tooltip ( "Filter/Search playlists" ) );
		clearButton.setTooltip( new Tooltip( "Clear the filter text" ) );

		FilterPane.getChildren().addAll( libraryButton, filterBox, clearButton );
	}

	
	public void resetTableSettingsToDefault() {
		nameColumn.setVisible( true );
		lengthColumn.setVisible( true );
		tracksColumn.setVisible( true );
		
		playlistTable.getColumns().remove( nameColumn );
		playlistTable.getColumns().add( nameColumn );
		playlistTable.getColumns().remove( lengthColumn );
		playlistTable.getColumns().add( lengthColumn );
		playlistTable.getColumns().remove( tracksColumn );
		playlistTable.getColumns().add( tracksColumn );

		playlistTable.getSortOrder().clear();
		
		nameColumn.setPrefWidth( 100 );
		tracksColumn.setPrefWidth( 90 );
		lengthColumn.setPrefWidth( 90 );
		playlistTable.getColumnResizePolicy().call(new ResizeFeatures<Playlist> ( playlistTable, null, 0d ) );
	}

	public void setupPlaylistTable () {
		nameColumn = new TableColumn<Playlist, String>( "Playlist" );
		lengthColumn = new TableColumn<Playlist, String>( "Length" );
		tracksColumn = new TableColumn<Playlist, Integer>( "Tracks" );

		lengthColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		
		//TODO: Are these the right types? Integer/String look wrong. 
		nameColumn.setCellValueFactory( new PropertyValueFactory <Playlist, String>( "Name" ) );
		lengthColumn.setCellValueFactory( new PropertyValueFactory <Playlist, String>( "LengthDisplay" ) );
		tracksColumn.setCellValueFactory( new PropertyValueFactory <Playlist, Integer>( "SongCount" ) );

		columnSelectorMenu = new ContextMenu ();
		CheckMenuItem nameMenuItem = new CheckMenuItem ( "Show Name Column" );
		CheckMenuItem tracksMenuItem = new CheckMenuItem ( "Show Tracks Column" );
		CheckMenuItem lengthMenuItem = new CheckMenuItem ( "Show Length Column" );
		MenuItem defaultMenuItem = new MenuItem ( "Reset to Default View" );
		nameMenuItem.setSelected( true );
		tracksMenuItem.setSelected( true );
		lengthMenuItem.setSelected( true );
		columnSelectorMenu.getItems().addAll( nameMenuItem, tracksMenuItem, lengthMenuItem, defaultMenuItem );
		nameColumn.setContextMenu( columnSelectorMenu );
		tracksColumn.setContextMenu( columnSelectorMenu );
		lengthColumn.setContextMenu( columnSelectorMenu );
		nameMenuItem.selectedProperty().bindBidirectional( nameColumn.visibleProperty() );
		tracksMenuItem.selectedProperty().bindBidirectional( tracksColumn.visibleProperty() );
		lengthMenuItem.selectedProperty().bindBidirectional( lengthColumn.visibleProperty() );
		defaultMenuItem.setOnAction( ( e ) -> this.resetTableSettingsToDefault() );

		playlistTable = new TableView<Playlist>();
		playlistTable.getColumns().addAll( nameColumn, tracksColumn, lengthColumn );
		playlistTable.setEditable( false );
		playlistTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		playlistTable.setItems( library.getPlaylistsSorted() );

		library.getPlaylistsSorted().comparatorProperty().bind( playlistTable.comparatorProperty() );
		
		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		playlistTable.setColumnResizePolicy( resizePolicy );
		resizePolicy.registerFixedWidthColumns( tracksColumn, lengthColumn );
		
		emptyLabel.setWrapText( true );
		emptyLabel.setTextAlignment( TextAlignment.CENTER );
		emptyLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		playlistTable.setPlaceholder( emptyLabel );
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem appendMenuItem = new MenuItem( "Append" );		
		MenuItem playNextMenuItem = new MenuItem( "Play Next" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem renameMenuItem = new MenuItem( "Rename" );
		MenuItem infoMenuItem = new MenuItem( "Track List" );
		MenuItem exportM3UMenuItem = new MenuItem( "Export as M3U" );
		MenuItem exportFolderMenuItem = new MenuItem ( "Export as Folder" );
		MenuItem removeMenuItem = new MenuItem( "Remove" );
		
		RadioMenuItem shuffleNoChange = new RadioMenuItem ( "Use Default" );
		RadioMenuItem shuffleSequential = new RadioMenuItem ( "Sequential" );
		RadioMenuItem shuffleShuffle = new RadioMenuItem ( "Shuffle" );
		Menu shuffleMode = new Menu ( "Shuffle Mode" );
		shuffleMode.getItems().addAll( shuffleNoChange, shuffleSequential, shuffleShuffle );

		ToggleGroup shuffleGroup = new ToggleGroup();
		shuffleNoChange.setToggleGroup( shuffleGroup );
		shuffleSequential.setToggleGroup( shuffleGroup );
		shuffleShuffle.setToggleGroup( shuffleGroup );
		
		shuffleGroup.selectedToggleProperty().addListener( ( observale, oldValue, newValue ) -> {
			PlaylistShuffleMode targetMode;
			if ( newValue.equals( shuffleSequential ) )  {
				targetMode = PlaylistShuffleMode.SEQUENTIAL;
			} else if ( newValue.equals( shuffleShuffle ) )  {
				targetMode = PlaylistShuffleMode.SHUFFLE;
			} else {
				targetMode = PlaylistShuffleMode.USE_DEFAULT;
			}
			playlistTable.getSelectionModel().getSelectedItem().setShuffleMode( targetMode );
		});
		
		RadioMenuItem repeatNoChange = new RadioMenuItem ( "Use Default" );
		RadioMenuItem repeatPlayOnce = new RadioMenuItem ( "Play Once" );
		RadioMenuItem repeatRepeat = new RadioMenuItem ( "Repeat" );
		Menu repeatMode = new Menu ( "Repeat Mode" );
		repeatMode.getItems().addAll( repeatNoChange, repeatPlayOnce, repeatRepeat );

		ToggleGroup repeatGroup = new ToggleGroup();
		repeatNoChange.setToggleGroup( repeatGroup );
		repeatPlayOnce.setToggleGroup( repeatGroup );
		repeatRepeat.setToggleGroup( repeatGroup );
		
		shuffleGroup.selectedToggleProperty().addListener( ( observable, oldValue, newValue ) -> {
			PlaylistShuffleMode targetMode;
			if ( newValue.equals( shuffleSequential ) )  {
				targetMode = PlaylistShuffleMode.SEQUENTIAL;
			} else if ( newValue.equals( shuffleShuffle ) )  {
				targetMode = PlaylistShuffleMode.SHUFFLE;
			} else {
				targetMode = PlaylistShuffleMode.USE_DEFAULT;
			}
			playlistTable.getSelectionModel().getSelectedItem().setShuffleMode( targetMode );
		});

		repeatGroup.selectedToggleProperty().addListener( ( observable, oldValue, newValue ) -> {
			PlaylistRepeatMode targetMode;
			if ( newValue.equals( repeatPlayOnce ) )  {
				targetMode = PlaylistRepeatMode.PLAY_ONCE;
			} else if ( newValue.equals( repeatRepeat ) )  {
				targetMode = PlaylistRepeatMode.REPEAT;
			} else {
				targetMode = PlaylistRepeatMode.USE_DEFAULT;
			}
			playlistTable.getSelectionModel().getSelectedItem().setRepeatMode( targetMode );
		});

		ContextMenu contextMenu = new ContextMenu();
		contextMenu.getItems().addAll( playMenuItem, appendMenuItem, playNextMenuItem, enqueueMenuItem, 
			shuffleMode, repeatMode, renameMenuItem, infoMenuItem, exportM3UMenuItem, exportFolderMenuItem, 
			removeMenuItem );

		playMenuItem.setOnAction( ( ActionEvent event ) -> {
			if ( ui.okToReplaceCurrentList() ) {
				audioSystem.getCurrentList().setAndPlayPlaylists( playlistTable.getSelectionModel().getSelectedItems() );
			}
		});

		appendMenuItem.setOnAction( ( ActionEvent event ) -> {
			audioSystem.getCurrentList().appendPlaylists( playlistTable.getSelectionModel().getSelectedItems() );
		});
		
		playNextMenuItem.setOnAction( ( ActionEvent event ) -> {
			audioSystem.getQueue().queueAllPlaylists( playlistTable.getSelectionModel().getSelectedItems(), 0 );
		});
		
		enqueueMenuItem.setOnAction( ( ActionEvent event ) -> {
			audioSystem.getQueue().queueAllPlaylists( playlistTable.getSelectionModel().getSelectedItems() );
		});
		
		renameMenuItem.setOnAction( ( ActionEvent event ) -> {
			ui.promptAndRenamePlaylist ( playlistTable.getSelectionModel().getSelectedItem() );
		});
		
		infoMenuItem.setOnAction( ( ActionEvent event ) -> {
			ui.playlistInfoWindow.setPlaylist ( playlistTable.getSelectionModel().getSelectedItem() );
			ui.playlistInfoWindow.show();
		});
		
		exportM3UMenuItem.setOnAction( ( ActionEvent event ) -> {
			File targetFile = ui.promptUserForPlaylistFile();
			
			if ( targetFile == null ) {
				return;
			}
			
			Playlist saveMe = playlistTable.getSelectionModel().getSelectedItem(); 
			
			if ( saveMe == null ) {
				return;
			}
			
			try {
				saveMe.saveAs( targetFile );
				
			} catch ( IOException e1 ) {
				ui.alertUser ( AlertType.ERROR, "Warning", "Unable to save playlist.", "Unable to save the playlist to the specified location" );
			}
			
		});
		
		exportFolderMenuItem.setOnAction( ( ActionEvent event ) -> {
			List<Track> exportMe = playlistTable.getSelectionModel().getSelectedItem().getTracks();
			ui.exportPopup.export( exportMe );
		});

		removeMenuItem.setOnAction( ( ActionEvent event ) -> {
			
			List <Playlist> deleteMe = playlistTable.getSelectionModel().getSelectedItems();
			if ( deleteMe.size() == 0 ) return;
			
			Alert alert = new Alert( AlertType.CONFIRMATION );
			ui.applyCurrentTheme( alert );
			FXUI.setAlertWindowIcon( alert );
			double x = ui.mainStage.getX() + ui.mainStage.getWidth() / 2 - 220; //It'd be nice to use alert.getWidth() / 2, but it's NAN now. 
			double y = ui.mainStage.getY() + ui.mainStage.getHeight() / 2 - 50;
			
			alert.setX( x );
			alert.setY( y );
			
			alert.setTitle( "Confirm" );
			alert.setHeaderText( "Delete Playlist Requested" );
			String text = "Are you sure you want to delete theses playlists?\n";
			int count = 0;
			for ( Playlist playlist : deleteMe ) { 
				text += "\n  " + playlist.getName(); 
				count++;
				if ( count > 6 ) { 
					text += "\n  <... and more>";
					break;
				}
			};
			
			alert.setContentText( text );

			Optional <ButtonType> result = alert.showAndWait();
			
			if ( result.get() == ButtonType.OK ) {
				library.removePlaylists( playlistTable.getSelectionModel().getSelectedItems() );
				playlistTable.getSelectionModel().clearSelection();
			}
		});
		
		playlistTable.getSelectionModel().selectedItemProperty().addListener( ( obs, oldSelection, newSelection ) -> {
		    if (newSelection != null) {
		    	ui.playlistInfoWindow.setPlaylist( newSelection );
		    }
		});

		playlistTable.setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ESCAPE ) {
				if ( filterBox.getText().length() > 0 ) {
					filterBox.clear();
					Platform.runLater( ()-> playlistTable.scrollTo( playlistTable.getSelectionModel().getSelectedItem() ) );
				} else {
					playlistTable.getSelectionModel().clearSelection();
				}
				
			} else if ( e.getCode() == KeyCode.F2         
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && !e.isShiftDown() ) {
				renameMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.F3         
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && !e.isShiftDown() ) {
				infoMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.Q
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && !e.isShiftDown() ) {
				enqueueMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.Q && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				playNextMenuItem.fire();

			}  else if ( e.getCode() == KeyCode.ENTER
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && !e.isShiftDown() ) {
				playMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() ) {
				audioSystem.getCurrentList().insertPlaylists ( 0, playlistTable.getSelectionModel().getSelectedItems() );
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isControlDown()
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				appendMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.DELETE
			&& !e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && !e.isShiftDown() ) {
				removeMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.UP ) {
				if ( playlistTable.getSelectionModel().getSelectedIndex() == 0 ) {
					filterBox.requestFocus();
				}
			}
		});
		
		playlistTable.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				//REFACTOR: I can check for file extensions...
				event.acceptTransferModes( TransferMode.COPY );
				event.consume();
			}
		});
		
		playlistTable.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				ArrayList <Playlist> playlistsToAdd = new ArrayList <Playlist> ();
				
				for ( File file : db.getFiles() ) {
					Path droppedPath = Paths.get( file.getAbsolutePath() );
					if ( Utils.isPlaylistFile ( droppedPath ) ) {
						Playlist playlist = Playlist.loadPlaylist( droppedPath );
						if ( playlist != null ) {
							playlistsToAdd.add( playlist );
						}
					}
				}
				
				if ( !playlistsToAdd.isEmpty() ) {
					library.addPlaylists( playlistsToAdd );
				}

				event.setDropCompleted( true );
				event.consume();
			}
			
		});
		
		playlistTable.setRowFactory( tv -> {
			TableRow <Playlist> row = new TableRow <>();
			
			row.itemProperty().addListener( (obs, oldValue, newValue ) -> {
				if ( newValue != null ) {
					row.setContextMenu( contextMenu );
				} else {
					row.setContextMenu( null );
				}
			});

			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && !row.isEmpty() ) {
					boolean doOverwrite = ui.okToReplaceCurrentList();
					if ( doOverwrite ) {
						audioSystem.getCurrentList().setAndPlayPlaylist( row.getItem() );
					}
				} else if ( event.getButton() == MouseButton.SECONDARY ) {
					Playlist playlist = playlistTable.getSelectionModel().getSelectedItem();
					if ( playlist != null ) {
						switch ( playlist.getShuffleMode() ) {
							case SEQUENTIAL:
								shuffleGroup.selectToggle( shuffleSequential );
								break;
							case SHUFFLE:
								shuffleGroup.selectToggle( shuffleShuffle );
								break;
							case USE_DEFAULT:
								shuffleGroup.selectToggle( shuffleNoChange );
								break;
							default:
								break;
						}
						
						switch ( playlist.getRepeatMode() ) {
							case PLAY_ONCE:
								repeatGroup.selectToggle ( repeatPlayOnce );
								break;
							case REPEAT:
								repeatGroup.selectToggle ( repeatRepeat );
								break;
							case USE_DEFAULT:
								repeatGroup.selectToggle ( repeatNoChange );
								break;
							default:
								break;
						}
					}
				}
			});

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					List <Playlist> selectedPlaylists = playlistTable.getSelectionModel().getSelectedItems();
					List <Track> tracks = new ArrayList <Track> ();
					
					List <Playlist> serializableList = new ArrayList<Playlist> ( selectedPlaylists );
					
					for ( Playlist playlist : selectedPlaylists ) {
						tracks.addAll ( playlist.getTracks() );
					}
					
					DraggedTrackContainer dragObject = new DraggedTrackContainer( null, tracks, null, serializableList, null, DragSource.PLAYLIST_LIST );
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
				if ( db.hasContent( FXUI.DRAGGED_TRACKS ) ) {
					if ( !row.isEmpty() ) {
						event.acceptTransferModes( TransferMode.COPY );
						event.consume();
					}
				} else if ( db.hasFiles() ) {
					//REFACTOR: I can check for file extensions...
					event.acceptTransferModes( TransferMode.COPY );
					event.consume();
				}
			});

			row.setOnDragDropped( event -> {
				Dragboard db = event.getDragboard();
				if ( db.hasContent( FXUI.DRAGGED_TRACKS ) ) {
					if ( !row.isEmpty() ) {
						DraggedTrackContainer container = (DraggedTrackContainer) db.getContent( FXUI.DRAGGED_TRACKS );
						Playlist playlist = row.getItem();
						ui.addToPlaylist( container.getTracks(), playlist );
						playlistTable.refresh();
						event.setDropCompleted( true );
						event.consume();
					}
				} else if ( db.hasFiles() ) {
					ArrayList <Playlist> playlistsToAdd = new ArrayList <Playlist> ();
					
					for ( File file : db.getFiles() ) {
						Path droppedPath = Paths.get( file.getAbsolutePath() );
						if ( Utils.isPlaylistFile ( droppedPath ) ) {
							Playlist playlist = Playlist.loadPlaylist( droppedPath );
							if ( playlist != null ) {
								playlistsToAdd.add( playlist );
							}
						}
					}
					
					if ( !playlistsToAdd.isEmpty() ) {
						library.addPlaylists( playlistsToAdd );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});

			return row;
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
	
	@SuppressWarnings("incomplete-switch")
	public void applySettingsBeforeWindowShown ( EnumMap<Persister.Setting, String> settings ) {
		settings.forEach( ( setting, value )-> {
			try {
				switch ( setting ) {
					case PL_TABLE_PLAYLIST_COLUMN_SHOW:
						nameColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case PL_TABLE_TRACKS_COLUMN_SHOW: 
						tracksColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case PL_TABLE_LENGTH_COLUMN_SHOW:
						lengthColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case PL_TABLE_PLAYLIST_COLUMN_WIDTH: 
						nameColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case PL_TABLE_TRACKS_COLUMN_WIDTH:
						tracksColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case PL_TABLE_LENGTH_COLUMN_WIDTH:
						lengthColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					
					case PLAYLIST_COLUMN_ORDER: {
						String[] order = value.split( " " );
						int newIndex = 0;

						for ( String columnName : order ) {
							try {
								if ( columnName.equals( "name" ) ) {
									playlistTable.getColumns().remove( nameColumn );
									playlistTable.getColumns().add( newIndex, nameColumn );
								} else if ( columnName.equals( "tracks" ) ) {
									playlistTable.getColumns().remove( tracksColumn );
									playlistTable.getColumns().add( newIndex, tracksColumn );
								} else if ( columnName.equals( "length" ) ) {
									playlistTable.getColumns().remove( lengthColumn );
									playlistTable.getColumns().add( newIndex, lengthColumn );
								} 
								newIndex++;
							} catch ( Exception e ) {
								LOGGER.log( Level.INFO, "Unable to set album table column order: '" + value + "'", e );
							}
							
						}
						settings.remove ( setting );
						break;
					}
					
					case PLAYLIST_SORT_ORDER: {
						playlistTable.getSortOrder().clear();
						
						if ( !value.equals( "" ) ) {
							String[] order = value.split( " " );
							for ( String fullValue : order ) {
								try {
									String columnName = fullValue.split( "-" )[0];
									SortType sortType = SortType.valueOf( fullValue.split( "-" )[1] );

									if ( columnName.equals( "name" ) ) {
										playlistTable.getSortOrder().add( nameColumn );
										nameColumn.setSortType( sortType );
									} else if ( columnName.equals( "tracks" ) ) {
										playlistTable.getSortOrder().add( tracksColumn );
										tracksColumn.setSortType( sortType );
									} else if ( columnName.equals( "length" ) ) {
										playlistTable.getSortOrder().add( lengthColumn );
										lengthColumn.setSortType( sortType );
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
		
		String playlistColumnOrderValue = "";
		for ( TableColumn<Playlist, ?> column : playlistTable.getColumns() ) {
			if ( column == this.nameColumn ) {
				playlistColumnOrderValue += "name ";
			} else if ( column == this.tracksColumn ) {
				playlistColumnOrderValue += "tracks ";
			} else if ( column == this.lengthColumn ) {
				playlistColumnOrderValue += "length ";
			}
		}
		retMe.put ( Setting.PLAYLIST_COLUMN_ORDER, playlistColumnOrderValue );
		
		String playlistSortValue = "";
		for ( TableColumn<Playlist, ?> column : playlistTable.getSortOrder() ) {
			if ( column == this.nameColumn ) {
				playlistSortValue += "name-" + nameColumn.getSortType() + " ";
			} else if ( column == this.tracksColumn ) {
				playlistSortValue += "tracks-" + tracksColumn.getSortType() + " ";
			} else if ( column == this.lengthColumn ) {
				playlistSortValue += "length-" + lengthColumn.getSortType() + " ";
			}
		}
		retMe.put ( Setting.PLAYLIST_SORT_ORDER, playlistSortValue );
		
		retMe.put ( Setting.PL_TABLE_PLAYLIST_COLUMN_SHOW, nameColumn.isVisible() );
		retMe.put ( Setting.PL_TABLE_TRACKS_COLUMN_SHOW, tracksColumn.isVisible() );
		retMe.put ( Setting.PL_TABLE_LENGTH_COLUMN_SHOW, lengthColumn.isVisible() );
		retMe.put ( Setting.PL_TABLE_PLAYLIST_COLUMN_WIDTH, nameColumn.getPrefWidth() );
		retMe.put ( Setting.PL_TABLE_TRACKS_COLUMN_WIDTH, tracksColumn.getPrefWidth() );
		retMe.put ( Setting.PL_TABLE_LENGTH_COLUMN_WIDTH, lengthColumn.getPrefWidth() );

		return retMe;
	}
	
	public void focusFilter() {
		playlistTable.requestFocus();
		filterBox.requestFocus();
		playlistTable.getSelectionModel().clearSelection();
	}
}