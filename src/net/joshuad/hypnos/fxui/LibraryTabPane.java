package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
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
import javafx.scene.control.Alert.AlertType;
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
import net.joshuad.hypnos.Album;
import net.joshuad.hypnos.AlphanumComparator;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Library;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.AlphanumComparator.CaseHandling;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;

public class LibraryTabPane extends StretchedTabPane {
	private static final Logger LOGGER = Logger.getLogger( LibraryTabPane.class.getName() );
	
	FXUI ui;
	AudioSystem audioSystem;
	Library library;

	Tab libraryTrackTab, libraryAlbumTab, libraryPlaylistTab;
	
	ImageView albumFilterClearImage, trackFilterClearImage, playlistFilterClearImage;
	ImageView addSourceTracksImage, addSourceAlbumsImage, addSourcePlaylistsImage;
	
	TableView <Album> albumTable;
	TableView <Playlist> playlistTable;
	TableView <Track> trackTable;
	
	HBox albumFilterPane;
	HBox trackFilterPane;
	HBox playlistFilterPane;
	
	ContextMenu playlistColumnSelectorMenu, trackColumnSelectorMenu, albumColumnSelectorMenu;
	ContextMenu tabMenu;
	TableColumn playlistNameColumn, playlistLengthColumn, playlistTracksColumn;
	TableColumn trackArtistColumn, trackLengthColumn, trackNumberColumn, trackAlbumColumn, trackTitleColumn;
	TableColumn albumArtistColumn, albumYearColumn, albumAlbumColumn;
	
	CheckMenuItem showAlbums; 
	CheckMenuItem showTracks;
	CheckMenuItem showPlaylists;
	
	Label emptyPlaylistLabel = new Label( 
		"You haven't created any playlists, make a playlist on the right and click the save button." );

	Label emptyTrackListLabel = new Label( 
		"No tracks loaded. To add to your library, click on the + button or drop folders here." );
	
	Label emptyAlbumListLabel = new Label(
		"No albums loaded. To add to your library, click on the + button or drop folders here." );
	
	Label filteredAlbumListLabel = new Label( "No albums match." );
	Label filteredTrackListLabel = new Label( "No tracks match." );
	Label filteredPlaylistLabel = new Label( "No playlists match." );
	
	CheckBox trackListCheckBox;
	TextField trackFilterBox, albumFilterBox, playlistFilterBox;
	
	public LibraryTabPane ( FXUI ui, AudioSystem audioSystem, Library library ) {
		this.ui = ui;
		this.audioSystem = audioSystem;
		this.library = library; 

		loadImages();
		setupTrackListCheckBox();
		setupAlbumFilterPane();
		setupTrackFilterPane();
		setupPlaylistFilterPane();
		
		setupAlbumTable();
		setupTrackTable();
		setupPlaylistTable();

		BorderPane albumListPane = new BorderPane();
		albumFilterPane.prefWidthProperty().bind( albumListPane.widthProperty() );
		albumListPane.setTop( albumFilterPane );
		albumListPane.setCenter( albumTable );
		
		BorderPane trackListPane = new BorderPane();
		trackFilterPane.prefWidthProperty().bind( trackListPane.widthProperty() );
		trackListPane.setTop( trackFilterPane );
		trackListPane.setCenter( trackTable );

		BorderPane playlistPane = new BorderPane();
		playlistFilterPane.prefWidthProperty().bind( playlistPane.widthProperty() );
		playlistPane.setTop( playlistFilterPane );
		playlistPane.setCenter( playlistTable );
		
		libraryAlbumTab = new Tab( "Albums" );
		libraryAlbumTab.setContent( albumListPane );
		libraryAlbumTab.setClosable( false );
		Tooltip albumTabTooltip = new Tooltip ( "Album Count: " + library.getAlbums().size() );
		libraryAlbumTab.setTooltip( albumTabTooltip );
		
		library.getAlbums().addListener( new ListChangeListener<Album> () {
			public void onChanged ( Change <? extends Album> changed ) {
				albumTabTooltip.setText( "Album Count: " + library.getAlbums().size() );
			}
		});

		libraryPlaylistTab = new Tab( "Playlists" );
		libraryPlaylistTab.setContent( playlistPane );
		libraryPlaylistTab.setClosable( false );
		Tooltip playlistTabTooltip = new Tooltip ( "Playlist Count: " + library.getPlaylists().size() );
		libraryPlaylistTab.setTooltip( playlistTabTooltip );
		
		library.getPlaylists().addListener( new ListChangeListener<Playlist> () {
			public void onChanged ( Change <? extends Playlist> changed ) {
				playlistTabTooltip.setText( "Playlist Count: " + library.getPlaylists().size() );
			}
		});

		libraryTrackTab = new Tab( "Tracks" );
		libraryTrackTab.setContent( trackListPane );
		libraryTrackTab.setClosable( false );
		Tooltip trackTabTooltip = new Tooltip ( "Track Count: " + library.getTracks().size() );
		libraryTrackTab.setTooltip( trackTabTooltip );
		
		library.getTracks().addListener( new ListChangeListener<Track> () {
			public void onChanged ( Change <? extends Track> changed ) {
				trackTabTooltip.setText( "Track Count: " + library.getTracks().size() );
			}
		});
		
		tabMenu = new ContextMenu();
		showAlbums = new CheckMenuItem ( "Albums" );
		showTracks = new CheckMenuItem ( "Tracks" );
		showPlaylists = new CheckMenuItem ( "Playlists" );
		tabMenu.getItems().addAll( showAlbums, showTracks, showPlaylists );

		showAlbums.setSelected( true );
		showTracks.setSelected( true );
		showPlaylists.setSelected( true );
		
		showAlbums.selectedProperty().addListener( ( observable, oldValue, newValue ) -> {
			setAlbumsVisible ( newValue );
		});
		
		showTracks.selectedProperty().addListener( ( observable, oldValue, newValue ) -> {
			setTracksVisible ( newValue );
		});
		
		showPlaylists.selectedProperty().addListener( ( observable, oldValue, newValue ) -> {
			setPlaylistsVisible ( newValue );
		});
		
		libraryAlbumTab.setContextMenu( tabMenu );
		libraryTrackTab.setContextMenu( tabMenu );
		libraryPlaylistTab.setContextMenu( tabMenu );

		getTabs().addAll( libraryAlbumTab, libraryTrackTab, libraryPlaylistTab );
		setSide( Side.BOTTOM );
		setStyle("-fx-open-tab-animation: NONE; -fx-close-tab-animation: NONE;");
		
	}
	
	private void fixTabOrder() {
		List<Tab> tabs = this.getTabs();
		List<Tab> reorderedTabs = new ArrayList<> ( tabs.size() );
		
		int index = 0;
		if ( tabs.contains( libraryAlbumTab ) ) {
			reorderedTabs.add( index++, libraryAlbumTab );
		}

		if ( tabs.contains( libraryTrackTab ) ) {
			reorderedTabs.add( index++, libraryTrackTab );
		}
		
		if ( tabs.contains( libraryPlaylistTab ) ) {
			reorderedTabs.add( index++, libraryPlaylistTab );
		}
		
		this.getTabs().clear();
		getTabs().addAll( reorderedTabs );
	}
	
	public void setAlbumsVisible ( boolean visible ) {
		if ( visible ) {
			if ( !getTabs().contains( libraryAlbumTab ) ) {
				getTabs().add( libraryAlbumTab );
				showAlbums.setSelected( true );
				fixTabOrder();
			}
		} else {
			if ( this.getTabs().size() >= 2 ) {
				getTabs().remove( libraryAlbumTab );
				fixTabOrder();
				showAlbums.setSelected( false );
			} else {
				showAlbums.setSelected( true );
			}
		}
	}
	
	public void setTracksVisible ( boolean visible ) {
		if ( visible ) {
			if ( !getTabs().contains( libraryTrackTab ) ) {
				getTabs().add( libraryTrackTab );
				showTracks.setSelected( true );
				fixTabOrder();
			}
		} else {
			if ( this.getTabs().size() >= 2 ) {
				getTabs().remove( libraryTrackTab );
				showTracks.setSelected( false );
				fixTabOrder();
			} else {
				showTracks.setSelected( true );
			}
		}
	}
	
	public void setPlaylistsVisible ( boolean visible ) {
		if ( visible ) {
			if ( !getTabs().contains( libraryPlaylistTab ) ) {
				getTabs().add( libraryPlaylistTab );
				showPlaylists.setSelected( true );
				fixTabOrder();
			}
		} else {
			if ( this.getTabs().size() >= 2 ) {
				getTabs().remove( libraryPlaylistTab );
				showPlaylists.setSelected( false );
				fixTabOrder();
			} else {
				showPlaylists.setSelected( true );
			}
		}
	}
	
	private void loadImages() {
		double currentListControlsButtonFitWidth = 15;
		double currentListControlsButtonFitHeight = 15;
		try {
			Image image = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/add.png" ).toFile() ) );

			addSourceTracksImage = new ImageView ( image );
			addSourceAlbumsImage = new ImageView ( image );
			addSourcePlaylistsImage = new ImageView ( image );
			
			addSourceTracksImage.setFitWidth( currentListControlsButtonFitWidth );
			addSourceTracksImage.setFitHeight( currentListControlsButtonFitHeight );
			addSourceAlbumsImage.setFitWidth( currentListControlsButtonFitWidth );
			addSourceAlbumsImage.setFitHeight( currentListControlsButtonFitHeight );
			addSourcePlaylistsImage.setFitWidth( currentListControlsButtonFitWidth );
			addSourcePlaylistsImage.setFitHeight( currentListControlsButtonFitHeight );
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load add icon: resources/add.png", e );
		}
		
		try {
			Image clearImage = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/clear.png" ).toFile() ) );
			
			albumFilterClearImage = new ImageView ( clearImage );
			trackFilterClearImage = new ImageView ( clearImage );
			playlistFilterClearImage = new ImageView ( clearImage );

			albumFilterClearImage.setFitWidth( 12 );
			albumFilterClearImage.setFitHeight( 12 );
			trackFilterClearImage.setFitWidth( 12 );
			trackFilterClearImage.setFitHeight( 12 );
			playlistFilterClearImage.setFitWidth( 12 );
			playlistFilterClearImage.setFitHeight( 12 );
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load clear icon: resources/clear.png", e );
		}
	}
	
	public void setupPlaylistFilterPane () {
		playlistFilterPane = new HBox();
		playlistFilterBox = new TextField();
		playlistFilterBox.setPrefWidth( 500000 );
		
		playlistFilterBox.textProperty().addListener( ( observable, oldValue, newValue ) -> {
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
		
		playlistFilterBox.setOnKeyPressed( ( KeyEvent event ) -> {
			if ( event.getCode() == KeyCode.ESCAPE ) {
				playlistFilterBox.clear();
			} else if ( event.getCode() == KeyCode.DOWN ) {
				playlistTable.requestFocus();
				playlistTable.getSelectionModel().select( playlistTable.getSelectionModel().getFocusedIndex() );
			}
		});
		
		double width = 33;
		double height = 26;
		
		playlistFilterBox.setPrefHeight( height );
		
		Button libraryButton = new Button( );
		libraryButton.setGraphic ( addSourcePlaylistsImage );
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
		clearButton.setGraphic( playlistFilterClearImage );
		clearButton.setMinSize( width, height );
		clearButton.setPrefSize( width, height );
		clearButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				playlistFilterBox.setText( "" );
			}
		});
		

		libraryButton.setTooltip( new Tooltip( "Add or Remove Music Folders" ) );
		playlistFilterBox.setTooltip ( new Tooltip ( "Filter/Search playlists" ) );
		clearButton.setTooltip( new Tooltip( "Clear the filter text" ) );

		playlistFilterPane.getChildren().addAll( libraryButton, playlistFilterBox, clearButton );
	}

	public void setupTrackFilterPane () {
		trackFilterPane = new HBox();
		trackFilterBox = new TextField();
		trackFilterBox.setPrefWidth( 500000 );
		
		trackFilterBox.textProperty().addListener( new ChangeListener <String> () {

			@Override
			public void changed ( ObservableValue <? extends String> observable, String oldValue, String newValue ) {
				Platform.runLater( () -> {
					library.getTracksFiltered().setPredicate( track -> {
						return acceptTrackFilterChange ( track, oldValue, newValue ); 
					});
				});
			}
		});
		
		trackFilterBox.setOnKeyPressed( ( KeyEvent event ) -> {
			if ( event.getCode() == KeyCode.ESCAPE ) {
				trackFilterBox.clear();
			} else if ( event.getCode() == KeyCode.DOWN ) {
				trackTable.requestFocus();
				trackTable.getSelectionModel().select( trackTable.getSelectionModel().getFocusedIndex() );
			}
		});
		
		double width = 33;
		double height = 26;

		trackFilterBox.setPrefHeight( height );
		
		Button libraryButton = new Button( );
		libraryButton.setGraphic( addSourceTracksImage );
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
		clearButton.setGraphic( trackFilterClearImage );
		clearButton.setMinSize( width, height );
		clearButton.setPrefSize( width, height );
		clearButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				trackFilterBox.setText( "" );
			}
		});

		libraryButton.setTooltip( new Tooltip( "Add or Remove Music Folders" ) );
		trackFilterBox.setTooltip ( new Tooltip ( "Filter/Search tracks" ) );
		clearButton.setTooltip( new Tooltip( "Clear the filter text" ) );
		
		HBox checkBoxMargins = new HBox();
		checkBoxMargins.setPadding( new Insets ( 4, 0, 0, 6 ) );
		checkBoxMargins.getChildren().add( trackListCheckBox );
		
		trackFilterPane.getChildren().addAll( libraryButton, trackFilterBox, clearButton, checkBoxMargins );
	}
	
	public boolean acceptTrackFilterChange ( Track track, Object oldValue, Object newValueIn ) {
				
		String newValue = trackFilterBox.getText();
		if ( newValueIn instanceof String ) {
			newValue = (String)newValueIn;
		}
		
		Boolean boxSelected = trackListCheckBox.isSelected();
		if ( newValueIn instanceof Boolean ) {
			boxSelected = (Boolean)newValueIn;
		}
			
		if ( track.hasAlbumDirectory() && boxSelected ) {
			return false;
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

	public void setupAlbumFilterPane () {
		albumFilterPane = new HBox();
		albumFilterBox = new TextField();
		albumFilterBox.setPrefWidth( 500000 );
		albumFilterBox.textProperty().addListener( ( observable, oldValue, newValue ) -> {
			Platform.runLater( () -> {
				library.getAlbumsFiltered().setPredicate( album -> {
					if ( newValue == null || newValue.isEmpty() ) {
						return true;
					}
	
					String[] lowerCaseFilterTokens = newValue.toLowerCase().split( "\\s+" );
	
					ArrayList <String> matchableText = new ArrayList <String>();
	
					matchableText.add( 
						Normalizer.normalize( album.getAlbumArtist(), Normalizer.Form.NFD )
						.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() 
					);
					
					matchableText.add( album.getAlbumArtist().toLowerCase() );
					
					matchableText.add( 
						Normalizer.normalize( album.getFullAlbumTitle(), Normalizer.Form.NFD )
						.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() 
					);
					
					matchableText.add( album.getFullAlbumTitle().toLowerCase() );
					
					matchableText.add( 
						Normalizer.normalize( album.getYear(), Normalizer.Form.NFD )
						.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase()
					);
					
					matchableText.add( album.getYear().toLowerCase() );
	
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
		
		albumFilterBox.setOnKeyPressed( ( KeyEvent event ) -> {
			if ( event.getCode() == KeyCode.ESCAPE ) {
				albumFilterBox.clear();
			} else if ( event.getCode() == KeyCode.DOWN ) {
				albumTable.requestFocus();
				albumTable.getSelectionModel().select( albumTable.getSelectionModel().getFocusedIndex() );
			}
		});
		
		
		float width = 33;
		float height = 26;

		albumFilterBox.setPrefHeight( height );
		
		Button libraryButton = new Button( );
		libraryButton.setGraphic( addSourceAlbumsImage );
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
		clearButton.setGraphic( albumFilterClearImage );
		clearButton.setMinSize( width, height );
		clearButton.setPrefSize( width, height );
		clearButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				albumFilterBox.setText( "" );
			}
		});
		

		libraryButton.setTooltip( new Tooltip( "Add or Remove Music Folders" ) );
		albumFilterBox.setTooltip ( new Tooltip ( "Filter/Search albums" ) );
		clearButton.setTooltip( new Tooltip( "Clear the filter text" ) );

		albumFilterPane.getChildren().addAll( libraryButton, albumFilterBox, clearButton );
	}

	public void setupTrackListCheckBox() {
		trackListCheckBox = new CheckBox( "" );
		trackListCheckBox.selectedProperty().addListener( new ChangeListener <Boolean> () {
			@Override
			public void changed( ObservableValue <? extends Boolean> observable, Boolean oldValue, Boolean newValue ) {
				library.getTracksFiltered().setPredicate( track -> {
					return acceptTrackFilterChange ( track, oldValue, newValue );
				});
			}
		});
		
		trackListCheckBox.setTooltip( new Tooltip( "Only show tracks not in albums" ) );
	}
	
	public void setupAlbumTable () {
		albumArtistColumn = new TableColumn( "Artist" );
		albumYearColumn = new TableColumn( "Year" );
		albumAlbumColumn = new TableColumn( "Album" );

		albumArtistColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		albumAlbumColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );

		albumArtistColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "albumArtist" ) );
		albumYearColumn.setCellValueFactory( new PropertyValueFactory <Album, Integer>( "year" ) );
		
		albumAlbumColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "FullAlbumTitle" ) );
		albumAlbumColumn.setCellFactory( e -> new FormattedAlbumCell() );
		
		albumColumnSelectorMenu = new ContextMenu ();
		CheckMenuItem artistMenuItem = new CheckMenuItem ( "Show Artist Column" );
		CheckMenuItem yearMenuItem = new CheckMenuItem ( "Show Year Column" );
		CheckMenuItem albumMenuItem = new CheckMenuItem ( "Show Album Column" );
		artistMenuItem.setSelected( true );
		yearMenuItem.setSelected( true );
		albumMenuItem.setSelected( true );
		albumColumnSelectorMenu.getItems().addAll( artistMenuItem, yearMenuItem, albumMenuItem );
		albumArtistColumn.setContextMenu( albumColumnSelectorMenu );
		albumYearColumn.setContextMenu( albumColumnSelectorMenu );
		albumAlbumColumn.setContextMenu( albumColumnSelectorMenu );
		artistMenuItem.selectedProperty().bindBidirectional( albumArtistColumn.visibleProperty() );
		yearMenuItem.selectedProperty().bindBidirectional( albumYearColumn.visibleProperty() );
		albumMenuItem.selectedProperty().bindBidirectional( albumAlbumColumn.visibleProperty() );

		albumTable = new TableView();
		albumTable.getColumns().addAll( albumArtistColumn, albumYearColumn, albumAlbumColumn );
		albumTable.setEditable( false );
		albumTable.setItems( library.getAlbumsSorted() );
		albumTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		library.getAlbumsSorted().comparatorProperty().bind( albumTable.comparatorProperty() );
		
		albumTable.getSortOrder().add( albumArtistColumn );
		albumTable.getSortOrder().add( albumYearColumn );
		albumTable.getSortOrder().add( albumAlbumColumn );
		
		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		albumTable.setColumnResizePolicy( resizePolicy );
		albumArtistColumn.setPrefWidth( 100 );
		albumYearColumn.setPrefWidth( 60 );
		albumAlbumColumn.setPrefWidth( 100 );
		resizePolicy.registerFixedWidthColumns( albumYearColumn );
		
		emptyAlbumListLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyAlbumListLabel.setWrapText( true );
		emptyAlbumListLabel.setTextAlignment( TextAlignment.CENTER );
		
		filteredAlbumListLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		filteredAlbumListLabel.setWrapText( true );
		filteredAlbumListLabel.setTextAlignment( TextAlignment.CENTER );
		
		albumTable.setPlaceholder( emptyAlbumListLabel );

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem appendMenuItem = new MenuItem( "Append" );
		MenuItem playNextMenuItem = new MenuItem( "Play Next" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		MenuItem infoMenuItem = new MenuItem( "Track List" );
		
		albumTable.setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ESCAPE 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				albumTable.getSelectionModel().clearSelection();
				
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
				audioSystem.getCurrentList().insertAlbums( 0,  albumTable.getSelectionModel().getSelectedItems() );
				
			} else if ( e.getCode() == KeyCode.ENTER && e.isControlDown() 
			&& !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				appendMenuItem.fire();
				
			}
		});
		
		contextMenu.getItems().addAll( 
			playMenuItem, appendMenuItem, playNextMenuItem, enqueueMenuItem, editTagMenuItem, infoMenuItem, 
			browseMenuItem, addToPlaylistMenuItem
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

		EventHandler addToPlaylistHandler = new EventHandler <ActionEvent>() {
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

		library.getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
			ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		} );

		ui.updatePlaylistMenuItems( addToPlaylistMenuItem.getItems(), addToPlaylistHandler );
		
		playMenuItem.setOnAction( event -> {
			if ( ui.okToReplaceCurrentList() ) {
				audioSystem.getCurrentList().setAndPlayAlbums( albumTable.getSelectionModel().getSelectedItems() );
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
					library.requestAddSource( file.toPath() );
				}

				event.setDropCompleted( true );
				event.consume();
			}
		});
		
		albumTable.getSelectionModel().selectedItemProperty().addListener( ( obs, oldSelection, newSelection ) -> {
			
		    if ( newSelection != null ) {
		    	ui.artSplitPane.setImages ( newSelection );
		    	ui.albumInfoWindow.setAlbum ( newSelection );
		    	
		    } else if ( audioSystem.getCurrentTrack() != null ) {
		    	ui.artSplitPane.setImages ( audioSystem.getCurrentTrack() );
		    	
		    } else {
		    	//Do nothing, leave the old artwork there. We can set to null if we like that better
		    	//I don't think so though
		    }
		});

		albumTable.setRowFactory( tv -> {
			TableRow <Album> row = new TableRow <>();
			
			row.setContextMenu( contextMenu );

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
						library.requestAddSource( file.toPath() );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					
					ArrayList <Integer> indices = new ArrayList <Integer>( albumTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Album> albums = new ArrayList <Album>( albumTable.getSelectionModel().getSelectedItems() );
					ArrayList <Track> tracks = new ArrayList <Track> ();
					
					for ( Album album : albums ) {
						if ( album != null ) {
							tracks.addAll( album.getTracks() );
						}
					}
					
					DraggedTrackContainer dragObject = new DraggedTrackContainer( null, tracks, albums, null, DragSource.ALBUM_LIST );
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
	
	public void setupTrackTable () {
		trackArtistColumn = new TableColumn( "Artist" );
		trackLengthColumn = new TableColumn( "Length" );
		trackNumberColumn = new TableColumn( "#" );
		trackAlbumColumn = new TableColumn( "Album" );
		trackTitleColumn = new TableColumn( "Title" );
		
		trackArtistColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		trackTitleColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		trackLengthColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );

		trackArtistColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Artist" ) );
		trackTitleColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Title" ) );
		trackLengthColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "LengthDisplay" ) );
		trackNumberColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "TrackNumber" ) );
		trackAlbumColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "albumTitle" ) );
		
		trackArtistColumn.setSortType( TableColumn.SortType.ASCENDING );

		trackNumberColumn.setCellFactory( column -> {
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
		
		trackColumnSelectorMenu = new ContextMenu ();
		CheckMenuItem artistMenuItem = new CheckMenuItem ( "Show Artist Column" );
		CheckMenuItem albumMenuItem = new CheckMenuItem ( "Show Album Column" );
		CheckMenuItem numberMenuItem = new CheckMenuItem ( "Show Track # Column" );
		CheckMenuItem titleMenuItem = new CheckMenuItem ( "Show Title Column" );
		CheckMenuItem lengthMenuItem = new CheckMenuItem ( "Show Length Column" );
		artistMenuItem.setSelected( true );
		albumMenuItem.setSelected( true );
		numberMenuItem.setSelected( true );
		titleMenuItem.setSelected( true );
		lengthMenuItem.setSelected( true );
		trackColumnSelectorMenu.getItems().addAll( 
			artistMenuItem, albumMenuItem, numberMenuItem, titleMenuItem, lengthMenuItem );
		
		trackArtistColumn.setContextMenu( trackColumnSelectorMenu );
		trackAlbumColumn.setContextMenu( trackColumnSelectorMenu );
		trackTitleColumn.setContextMenu( trackColumnSelectorMenu );
		trackNumberColumn.setContextMenu( trackColumnSelectorMenu );
		trackLengthColumn.setContextMenu( trackColumnSelectorMenu );
		artistMenuItem.selectedProperty().bindBidirectional( trackArtistColumn.visibleProperty() );
		albumMenuItem.selectedProperty().bindBidirectional( trackAlbumColumn.visibleProperty() );
		numberMenuItem.selectedProperty().bindBidirectional( trackNumberColumn.visibleProperty() );
		titleMenuItem.selectedProperty().bindBidirectional( trackTitleColumn.visibleProperty() );
		lengthMenuItem.selectedProperty().bindBidirectional( trackLengthColumn.visibleProperty() );
		
		trackTable = new TableView();
		trackTable.getColumns().addAll( 
			trackArtistColumn, trackAlbumColumn, trackNumberColumn, trackTitleColumn, trackLengthColumn );
		
		trackTable.setEditable( false );
		trackTable.setItems( library.getTracksSorted() );

		library.getTracksSorted().comparatorProperty().bind( trackTable.comparatorProperty() );
		
		trackTable.getSelectionModel().clearSelection();
		trackTable.getSortOrder().add( trackArtistColumn );
		trackTable.getSortOrder().add( trackAlbumColumn );
		trackTable.getSortOrder().add( trackNumberColumn );
		
		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		trackTable.setColumnResizePolicy( resizePolicy );
		trackArtistColumn.setPrefWidth( 100 );
		trackNumberColumn.setPrefWidth( 40 );
		trackAlbumColumn.setPrefWidth( 100 );
		trackTitleColumn.setPrefWidth( 100 );
		trackLengthColumn.setPrefWidth( 60 );
		resizePolicy.registerFixedWidthColumns( trackNumberColumn, trackLengthColumn );
		
		emptyTrackListLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyTrackListLabel.setWrapText( true );
		emptyTrackListLabel.setTextAlignment( TextAlignment.CENTER );
		trackTable.setPlaceholder( emptyTrackListLabel );
		
		trackTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		ContextMenu trackContextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem playNextMenuItem = new MenuItem( "Play Next" );
		MenuItem appendMenuItem = new MenuItem( "Append" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem editTagMenuItem = new MenuItem( "Edit Tag(s)" );
		MenuItem infoMenuItem = new MenuItem( "Info" );
		MenuItem lyricsMenuItem = new MenuItem( "Lyrics" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		trackContextMenu.getItems().addAll ( 
			playMenuItem, playNextMenuItem, appendMenuItem, enqueueMenuItem, 
			editTagMenuItem, infoMenuItem, lyricsMenuItem, browseMenuItem, addToPlaylistMenuItem );
		
		MenuItem newPlaylistButton = new MenuItem( "<New>" );

		addToPlaylistMenuItem.getItems().add( newPlaylistButton );

		newPlaylistButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				ui.promptAndSavePlaylist ( trackTable.getSelectionModel().getSelectedItems() );
			}
		});

		EventHandler addToPlaylistHandler = new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				Playlist playlist = (Playlist) ((MenuItem) event.getSource()).getUserData();
				ui.addToPlaylist ( trackTable.getSelectionModel().getSelectedItems(), playlist );
			}
		};

		library.getPlaylistSorted().addListener( ( ListChangeListener.Change <? extends Playlist> change ) -> {
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
				trackTable.getSelectionModel().clearSelection();
				
			} else if ( e.getCode() == KeyCode.L
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				lyricsMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.Q 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				enqueueMenuItem.fire();
				
			} else if ( e.getCode() == KeyCode.Q && e.isShiftDown()
			&& !e.isAltDown() && !e.isControlDown()  && !e.isMetaDown() ) {
				playNextMenuItem.fire();
							
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
			}
		});
		
		trackTable.getSelectionModel().selectedItemProperty().addListener( ( obs, oldSelection, newSelection ) -> {
		    if (newSelection != null) {
		    	ui.artSplitPane.setImages ( newSelection );
		    	ui.trackInfoWindow.setTrack( newSelection );
		    	
		    } else if ( audioSystem.getCurrentTrack() != null ) {
		    	ui.artSplitPane.setImages ( audioSystem.getCurrentTrack() );
		    	
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
					library.requestAddSource( file.toPath() );
				}

				event.setDropCompleted( true );
				event.consume();
			}
		});

		trackTable.setRowFactory( tv -> {
			TableRow <Track> row = new TableRow <>();
			
			row.setContextMenu( trackContextMenu );
			
			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && (!row.isEmpty()) ) {
					audioSystem.playTrack( row.getItem(), false );
				}
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
						library.requestAddSource( file.toPath() );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					ArrayList <Integer> indices = new ArrayList <Integer>( trackTable.getSelectionModel().getSelectedIndices() );
					ArrayList <Track> tracks = new ArrayList <Track>( trackTable.getSelectionModel().getSelectedItems() );
					DraggedTrackContainer dragObject = new DraggedTrackContainer( indices, tracks, null, null, DragSource.TRACK_LIST );
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

	public void setupPlaylistTable () {
		playlistNameColumn = new TableColumn( "Playlist" );
		playlistLengthColumn = new TableColumn( "Length" );
		playlistTracksColumn = new TableColumn( "Tracks" );

		playlistLengthColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		
		//TODO: Are these the right types? Integer/String look wrong. 
		playlistNameColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "Name" ) );
		playlistLengthColumn.setCellValueFactory( new PropertyValueFactory <Album, Integer>( "LengthDisplay" ) );
		playlistTracksColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "SongCount" ) );

		playlistNameColumn.setSortType( TableColumn.SortType.ASCENDING );
		
		playlistColumnSelectorMenu = new ContextMenu ();
		CheckMenuItem nameMenuItem = new CheckMenuItem ( "Show Name Column" );
		CheckMenuItem tracksMenuItem = new CheckMenuItem ( "Show Tracks Column" );
		CheckMenuItem lengthMenuItem = new CheckMenuItem ( "Show Length Column" );
		nameMenuItem.setSelected( true );
		tracksMenuItem.setSelected( true );
		lengthMenuItem.setSelected( true );
		playlistColumnSelectorMenu.getItems().addAll( nameMenuItem, tracksMenuItem, lengthMenuItem );
		playlistNameColumn.setContextMenu( playlistColumnSelectorMenu );
		playlistTracksColumn.setContextMenu( playlistColumnSelectorMenu );
		playlistLengthColumn.setContextMenu( playlistColumnSelectorMenu );
		nameMenuItem.selectedProperty().bindBidirectional( playlistNameColumn.visibleProperty() );
		tracksMenuItem.selectedProperty().bindBidirectional( playlistTracksColumn.visibleProperty() );
		lengthMenuItem.selectedProperty().bindBidirectional( playlistLengthColumn.visibleProperty() );

		playlistTable = new TableView<Playlist>();
		playlistTable.getColumns().addAll( playlistNameColumn, playlistTracksColumn, playlistLengthColumn );
		playlistTable.setEditable( false );
		playlistTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		playlistTable.setItems( library.getPlaylistSorted() );

		library.getPlaylistSorted().comparatorProperty().bind( playlistTable.comparatorProperty() );

		playlistTable.getSortOrder().add( playlistNameColumn );
		
		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		playlistTable.setColumnResizePolicy( resizePolicy );
		playlistNameColumn.setPrefWidth( 100 );
		playlistTracksColumn.setPrefWidth( 90 );
		playlistLengthColumn.setPrefWidth( 90 );
		resizePolicy.registerFixedWidthColumns( playlistTracksColumn, playlistLengthColumn );
		
		emptyPlaylistLabel.setWrapText( true );
		emptyPlaylistLabel.setTextAlignment( TextAlignment.CENTER );
		emptyPlaylistLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		playlistTable.setPlaceholder( emptyPlaylistLabel );

		ContextMenu contextMenu = new ContextMenu();
		MenuItem playMenuItem = new MenuItem( "Play" );
		MenuItem appendMenuItem = new MenuItem( "Append" );		
		MenuItem playNextMenuItem = new MenuItem( "Play Next" );
		MenuItem enqueueMenuItem = new MenuItem( "Enqueue" );
		MenuItem renameMenuItem = new MenuItem( "Rename" );
		MenuItem infoMenuItem = new MenuItem( "Track List" );
		MenuItem exportMenuItem = new MenuItem( "Export" );
		MenuItem removeMenuItem = new MenuItem( "Remove" );
		contextMenu.getItems().addAll( playMenuItem, appendMenuItem, playNextMenuItem, enqueueMenuItem, 
				renameMenuItem, infoMenuItem, exportMenuItem, removeMenuItem );

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
		
		exportMenuItem.setOnAction( ( ActionEvent event ) -> {
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
				ui.alertUser ( AlertType.ERROR, "Warning", "Unable to save playlist.", "Unable to save the playlist to the specified location", 400 );
			}
			
		});

		removeMenuItem.setOnAction( ( ActionEvent event ) -> {
			
			List <Playlist> deleteMe = playlistTable.getSelectionModel().getSelectedItems();
			if ( deleteMe.size() == 0 ) return;
			
			Alert alert = new Alert( AlertType.CONFIRMATION );
			ui.applyCurrentTheme( alert );
			ui.setAlertWindowIcon( alert );
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
				playlistTable.getSelectionModel().clearSelection();
				
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
			
			row.setContextMenu ( contextMenu );

			row.setOnMouseClicked( event -> {
				if ( event.getClickCount() == 2 && !row.isEmpty() ) {
					boolean doOverwrite = ui.okToReplaceCurrentList();
					if ( doOverwrite ) {
						audioSystem.getCurrentList().setAndPlayPlaylist( row.getItem() );
					}
				}
			});

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					List <Playlist> selectedPlaylists = playlistTable.getSelectionModel().getSelectedItems();
					List <Track> tracks = new ArrayList <Track> ();
					
					List <Playlist> serializableList = new ArrayList ( selectedPlaylists );
					
					for ( Playlist playlist : selectedPlaylists ) {
						tracks.addAll ( playlist.getTracks() );
					}
					
					DraggedTrackContainer dragObject = new DraggedTrackContainer( null, tracks, null, serializableList, DragSource.PLAYLIST_LIST );
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
						int dropIndex = row.isEmpty() ? dropIndex = library.getPlaylists().size() : row.getIndex();
						library.addPlaylists( playlistsToAdd );
					}

					event.setDropCompleted( true );
					event.consume();
				}
			});

			return row;
		});
	}
	
	public void updateLibraryListPlaceholder() {

		if ( library.getAlbums().isEmpty() ) {
			if ( albumTable.getPlaceholder() != emptyAlbumListLabel ) {
				albumTable.setPlaceholder( emptyAlbumListLabel );
			}
		} else {
			if ( !albumTable.getPlaceholder().equals( filteredAlbumListLabel ) ) {
				albumTable.setPlaceholder( filteredAlbumListLabel );
			}
		}
		
		if ( library.getTracks().isEmpty() ) {
			if ( trackTable.getPlaceholder() != emptyTrackListLabel ) {
				trackTable.setPlaceholder( emptyTrackListLabel );
			}
		} else {
			if ( !trackTable.getPlaceholder().equals( filteredTrackListLabel ) ) {
				trackTable.setPlaceholder( filteredTrackListLabel );
			}
		}
		
		if ( library.getPlaylists().isEmpty() ) {
			if ( !playlistTable.getPlaceholder().equals( emptyPlaylistLabel ) ) {
				playlistTable.setPlaceholder( emptyPlaylistLabel );
			}
		} else {
			if ( !playlistTable.getPlaceholder().equals( filteredPlaylistLabel ) ) {
				playlistTable.setPlaceholder( filteredPlaylistLabel );
			}
		}
	}

	public void doAfterShowProcessing () {
		Node blankPlaylistHeader = playlistTable.lookup(".column-header-background");
		blankPlaylistHeader.setOnContextMenuRequested ( 
			event -> playlistColumnSelectorMenu.show( blankPlaylistHeader, event.getScreenX(), event.getScreenY() ) );
		
		Node blankTrackHeader = trackTable.lookup(".column-header-background");
		blankTrackHeader.setOnContextMenuRequested ( 
			event -> trackColumnSelectorMenu.show( blankTrackHeader, event.getScreenX(), event.getScreenY() ) );
		
		Node blankAlbumHeader = albumTable.lookup(".column-header-background");
		blankAlbumHeader.setOnContextMenuRequested ( 
			event -> albumColumnSelectorMenu.show( blankAlbumHeader, event.getScreenX(), event.getScreenY() ) );
	}

	public void applyDarkTheme ( ColorAdjust buttonColor ) {
		if ( albumFilterClearImage != null ) albumFilterClearImage.setEffect( buttonColor );
		if ( trackFilterClearImage != null ) trackFilterClearImage.setEffect( buttonColor );
		if ( playlistFilterClearImage != null ) playlistFilterClearImage.setEffect( buttonColor );
		if ( addSourceTracksImage != null ) addSourceTracksImage.setEffect( buttonColor );
		if ( addSourceAlbumsImage != null ) addSourceAlbumsImage.setEffect( buttonColor );
		if ( addSourcePlaylistsImage != null ) addSourcePlaylistsImage.setEffect( buttonColor );
		
	}

	public void removeDarkTheme () {
		if ( albumFilterClearImage != null ) albumFilterClearImage.setEffect( null );
		if ( trackFilterClearImage != null ) trackFilterClearImage.setEffect( null );
		if ( playlistFilterClearImage != null ) playlistFilterClearImage.setEffect( null );
		if ( addSourceTracksImage != null ) addSourceTracksImage.setEffect( null );
		if ( addSourceAlbumsImage != null ) addSourceAlbumsImage.setEffect( null );
		if ( addSourcePlaylistsImage != null ) addSourcePlaylistsImage.setEffect( null );
	}
}
