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
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView.ResizeFeatures;
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
import net.joshuad.hypnos.Persister;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.AlphanumComparator.CaseHandling;
import net.joshuad.hypnos.Persister.Setting;
import net.joshuad.hypnos.fxui.DraggedTrackContainer.DragSource;

public class LibraryTabPane extends StretchedTabPane {
	private static final Logger LOGGER = Logger.getLogger( LibraryTabPane.class.getName() );
	
	FXUI ui;
	AudioSystem audioSystem;
	Library library;

	Tab trackTab, albumTab, playlistTab;
	
	ImageView albumFilterClearImage, trackFilterClearImage, playlistFilterClearImage;
	ImageView addSourceTracksImage, addSourceAlbumsImage, addSourcePlaylistsImage;
	
	TableView <Album> albumTable;
	TableView <Playlist> playlistTable;
	TableView <Track> trackTable;
	
	HBox albumFilterPane;
	HBox trackFilterPane;
	HBox playlistFilterPane;
	
	ThrottledTrackFilter trackTableFilter;
	ThrottledAlbumFilter albumTableFilter;
	
	ContextMenu playlistColumnSelectorMenu, trackColumnSelectorMenu, albumColumnSelectorMenu;
	ContextMenu tabMenu;
	TableColumn playlistNameColumn, playlistLengthColumn, playlistTracksColumn;
	TableColumn trackArtistColumn, trackLengthColumn, trackNumberColumn, trackAlbumColumn, trackTitleColumn;
	TableColumn albumArtistColumn, albumYearColumn, albumAddedDateColumn, albumAlbumColumn;
	
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
	
	Label loadingAlbumListLabel = new Label( "Loading..." );
	Label loadingTrackListLabel = new Label( "Loading..." );
	Label loadingPlaylistListLabel = new Label( "Loading..." );
	
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
		resetAlbumTableSettingsToDefault();
		resetTrackTableSettingsToDefault();
		resetPlaylistTableSettingsToDefault();

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
		
		albumTab = new Tab( "Albums" );
		albumTab.setContent( albumListPane );
		albumTab.setClosable( false );
		Tooltip albumTabTooltip = new Tooltip ( "Album Count: " + library.getAlbums().size() );
		albumTab.setTooltip( albumTabTooltip );
		
		library.getAlbums().addListener( new ListChangeListener<Album> () {
			public void onChanged ( Change <? extends Album> changed ) {
				albumTabTooltip.setText( "Album Count: " + library.getAlbums().size() );
			}
		});

		playlistTab = new Tab( "Playlists" );
		playlistTab.setContent( playlistPane );
		playlistTab.setClosable( false );
		Tooltip playlistTabTooltip = new Tooltip ( "Playlist Count: " + library.getPlaylists().size() );
		playlistTab.setTooltip( playlistTabTooltip );
		
		library.getPlaylists().addListener( new ListChangeListener<Playlist> () {
			public void onChanged ( Change <? extends Playlist> changed ) {
				playlistTabTooltip.setText( "Playlist Count: " + library.getPlaylists().size() );
			}
		});

		trackTab = new Tab( "Tracks" );
		trackTab.setContent( trackListPane );
		trackTab.setClosable( false );
		Tooltip trackTabTooltip = new Tooltip ( "Track Count: " + library.getTracks().size() );
		trackTab.setTooltip( trackTabTooltip );
		
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
		
		albumTab.setContextMenu( tabMenu );
		trackTab.setContextMenu( tabMenu );
		playlistTab.setContextMenu( tabMenu );

		getTabs().addAll( albumTab, trackTab, playlistTab );
		setSide( Side.BOTTOM );
		setStyle("-fx-open-tab-animation: NONE; -fx-close-tab-animation: NONE;");
		
	}
	
	private void fixTabOrder() {
		List<Tab> tabs = this.getTabs();
		List<Tab> reorderedTabs = new ArrayList<> ( tabs.size() );
		
		int index = 0;
		if ( tabs.contains( albumTab ) ) {
			reorderedTabs.add( index++, albumTab );
		}

		if ( tabs.contains( trackTab ) ) {
			reorderedTabs.add( index++, trackTab );
		}
		
		if ( tabs.contains( playlistTab ) ) {
			reorderedTabs.add( index++, playlistTab );
		}
		
		this.getTabs().clear();
		getTabs().addAll( reorderedTabs );
	}
	
	public void setAlbumsVisible ( boolean visible ) {
		if ( visible ) {
			if ( !getTabs().contains( albumTab ) ) {
				getTabs().add( albumTab );
				showAlbums.setSelected( true );
				fixTabOrder();
			}
		} else {
			if ( this.getTabs().size() >= 2 ) {
				getTabs().remove( albumTab );
				fixTabOrder();
				showAlbums.setSelected( false );
			} else {
				showAlbums.setSelected( true );
			}
		}
	}
	
	public void setTracksVisible ( boolean visible ) {
		if ( visible ) {
			if ( !getTabs().contains( trackTab ) ) {
				getTabs().add( trackTab );
				showTracks.setSelected( true );
				fixTabOrder();
			}
		} else {
			if ( this.getTabs().size() >= 2 ) {
				getTabs().remove( trackTab );
				showTracks.setSelected( false );
				fixTabOrder();
			} else {
				showTracks.setSelected( true );
			}
		}
	}
	
	public void setPlaylistsVisible ( boolean visible ) {
		if ( visible ) {
			if ( !getTabs().contains( playlistTab ) ) {
				getTabs().add( playlistTab );
				showPlaylists.setSelected( true );
				fixTabOrder();
			}
		} else {
			if ( this.getTabs().size() >= 2 ) {
				getTabs().remove( playlistTab );
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
				if ( playlistFilterBox.getText().length() > 0 ) {
					playlistFilterBox.clear();
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
		trackTableFilter = new ThrottledTrackFilter ( library.getTracksFiltered() );
		
		trackFilterPane = new HBox();
		trackFilterBox = new TextField();
		trackFilterBox.setPrefWidth( 500000 );
		
		trackFilterBox.textProperty().addListener( new ChangeListener <String> () {
			@Override
			public void changed ( ObservableValue <? extends String> observable, String oldValue, String newValue ) {
				trackTableFilter.setFilter( newValue, trackListCheckBox.isSelected() );
			}
		});
		
		trackFilterBox.setOnKeyPressed( ( KeyEvent event ) -> {
			if ( event.getCode() == KeyCode.ESCAPE ) {
				
				trackFilterBox.clear();
				if ( trackFilterBox.getText().length() > 0 ) {
					trackFilterBox.clear();
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
	
	public void setupAlbumFilterPane () {
		albumTableFilter = new ThrottledAlbumFilter ( library.getAlbumsFiltered() );
		
		albumFilterPane = new HBox();
		albumFilterBox = new TextField();
		albumFilterBox.setPrefWidth( 500000 );
		
		albumFilterBox.textProperty().addListener( new ChangeListener <String> () {
			@Override
			public void changed ( ObservableValue <? extends String> observable, String oldValue, String newValue ) {
				albumTableFilter.setFilter( newValue );
			}
		});
		
		albumFilterBox.setOnKeyPressed( ( KeyEvent event ) -> {
			if ( event.getCode() == KeyCode.ESCAPE ) {
				event.consume();
				if ( albumFilterBox.getText().length() > 0 ) {
					albumFilterBox.clear();
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
				trackTableFilter.setFilter( trackFilterBox.getText(), newValue );
			}
		});
		
		trackListCheckBox.setTooltip( new Tooltip( "Only show tracks not in albums" ) );
	}
	
	@SuppressWarnings("unchecked")
	public void resetAlbumTableSettingsToDefault() {
		albumArtistColumn.setVisible( true );
		albumAddedDateColumn.setVisible( false );
		albumYearColumn.setVisible( true );
		albumAlbumColumn.setVisible( true );
		
		albumTable.getColumns().remove( albumArtistColumn );
		albumTable.getColumns().add( albumArtistColumn );
		albumTable.getColumns().remove( albumYearColumn );
		albumTable.getColumns().add( albumYearColumn );
		albumTable.getColumns().remove( albumAddedDateColumn );
		albumTable.getColumns().add( albumAddedDateColumn );
		albumTable.getColumns().remove( albumAlbumColumn );
		albumTable.getColumns().add( albumAlbumColumn );

		albumTable.getSortOrder().add( albumArtistColumn );
		albumTable.getSortOrder().add( albumYearColumn );
		albumTable.getSortOrder().add( albumAlbumColumn );
		
		albumArtistColumn.setPrefWidth( 100 );
		albumYearColumn.setPrefWidth( 60 );
		albumAlbumColumn.setPrefWidth( 100 );
		albumAddedDateColumn.setPrefWidth ( 90 );
		
		albumTable.getColumnResizePolicy().call( new ResizeFeatures ( albumTable, null, 0d ) );
	}
	
	public void setupAlbumTable () {
		albumArtistColumn = new TableColumn( "Artist" );
		albumYearColumn = new TableColumn( "Year" );
		albumAlbumColumn = new TableColumn( "Album" );
		albumAddedDateColumn = new TableColumn ( "Added" );

		albumArtistColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );
		albumAlbumColumn.setComparator( new AlphanumComparator( CaseHandling.CASE_INSENSITIVE ) );

		albumArtistColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "albumArtist" ) );
		albumYearColumn.setCellValueFactory( new PropertyValueFactory <Album, Integer>( "year" ) );
		albumAlbumColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "FullAlbumTitle" ) );
		albumAddedDateColumn.setCellValueFactory( new PropertyValueFactory <Album, String>( "dateAddedString" ) );
		
		albumAlbumColumn.setCellFactory( e -> new FormattedAlbumCell() );
		
		albumColumnSelectorMenu = new ContextMenu ();
		CheckMenuItem artistMenuItem = new CheckMenuItem ( "Show Artist Column" );
		CheckMenuItem yearMenuItem = new CheckMenuItem ( "Show Year Column" );
		CheckMenuItem albumMenuItem = new CheckMenuItem ( "Show Album Column" );
		CheckMenuItem dateAddedMenuItem = new CheckMenuItem ( "Show Added Column" );
		MenuItem defaultMenuItem = new MenuItem ( "Reset to Default View" );
		
		artistMenuItem.setSelected( true );
		yearMenuItem.setSelected( true );
		albumMenuItem.setSelected( true );
		dateAddedMenuItem.setSelected( false );
		albumColumnSelectorMenu.getItems().addAll( artistMenuItem, yearMenuItem, dateAddedMenuItem, albumMenuItem, defaultMenuItem );
		albumArtistColumn.setContextMenu( albumColumnSelectorMenu );
		albumYearColumn.setContextMenu( albumColumnSelectorMenu );
		albumAlbumColumn.setContextMenu( albumColumnSelectorMenu );
		albumAddedDateColumn.setContextMenu( albumColumnSelectorMenu );
		artistMenuItem.selectedProperty().bindBidirectional( albumArtistColumn.visibleProperty() );
		yearMenuItem.selectedProperty().bindBidirectional( albumYearColumn.visibleProperty() );
		albumMenuItem.selectedProperty().bindBidirectional( albumAlbumColumn.visibleProperty() );
		dateAddedMenuItem.selectedProperty().bindBidirectional( albumAddedDateColumn.visibleProperty() );
		defaultMenuItem.setOnAction( e -> this.resetAlbumTableSettingsToDefault() );

		albumTable = new TableView();
		albumTable.getColumns().addAll( albumArtistColumn, albumYearColumn, albumAddedDateColumn, albumAlbumColumn );
		albumTable.setEditable( false );
		albumTable.setItems( library.getAlbumsSorted() );
		albumTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		library.getAlbumsSorted().comparatorProperty().bind( albumTable.comparatorProperty() );
		
		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		albumTable.setColumnResizePolicy( resizePolicy );
		resizePolicy.registerFixedWidthColumns( albumYearColumn );
		resizePolicy.registerFixedWidthColumns( albumAddedDateColumn );
		
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
		MenuItem sortByNewestMenuItem = new MenuItem ( "Sort by Add Date" );
		MenuItem browseMenuItem = new MenuItem( "Browse Folder" );
		Menu addToPlaylistMenuItem = new Menu( "Add to Playlist" );
		MenuItem infoMenuItem = new MenuItem( "Track List" );
		
		albumTable.setOnKeyPressed( ( KeyEvent e ) -> {
			if ( e.getCode() == KeyCode.ESCAPE 
			&& !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				if ( albumFilterBox.getText().length() > 0 ) {
					albumFilterBox.clear();
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
					albumFilterBox.requestFocus();
				}
			}
		});
		
		contextMenu.getItems().addAll( 
			playMenuItem, appendMenuItem, playNextMenuItem, enqueueMenuItem, editTagMenuItem, infoMenuItem, 
			sortByNewestMenuItem, browseMenuItem, addToPlaylistMenuItem
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
			albumTable.getSortOrder().add( albumAddedDateColumn );
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
	
	@SuppressWarnings("unchecked")
	public void resetTrackTableSettingsToDefault() {
		trackArtistColumn.setVisible( true );
		trackLengthColumn.setVisible( true );
		trackNumberColumn.setVisible( true );
		trackAlbumColumn.setVisible( true );
		trackTitleColumn.setVisible( true );
		
		trackTable.getColumns().remove( trackArtistColumn );
		trackTable.getColumns().add( trackArtistColumn );
		trackTable.getColumns().remove( trackLengthColumn );
		trackTable.getColumns().add( trackLengthColumn );
		trackTable.getColumns().remove( trackNumberColumn );
		trackTable.getColumns().add( trackNumberColumn );
		trackTable.getColumns().remove( trackAlbumColumn );
		trackTable.getColumns().add( trackAlbumColumn );
		trackTable.getColumns().remove( trackTitleColumn );
		trackTable.getColumns().add( trackTitleColumn );

		trackTable.getSortOrder().clear();
		trackTable.getSortOrder().add( trackArtistColumn );
		trackTable.getSortOrder().add( trackAlbumColumn );
		trackTable.getSortOrder().add( trackNumberColumn );
		trackArtistColumn.setSortType( SortType.ASCENDING );
		trackAlbumColumn.setSortType( SortType.ASCENDING );
		trackNumberColumn.setSortType( SortType.ASCENDING );
		
		trackArtistColumn.setPrefWidth( 100 );
		trackNumberColumn.setPrefWidth( 40 );
		trackAlbumColumn.setPrefWidth( 100 );
		trackTitleColumn.setPrefWidth( 100 );
		trackLengthColumn.setPrefWidth( 60 );
		trackTable.getColumnResizePolicy().call(new ResizeFeatures ( trackTable, null, 0d ) );
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
		MenuItem defaultMenuItem = new MenuItem ( "Reset to Default View" );
		artistMenuItem.setSelected( true );
		albumMenuItem.setSelected( true );
		numberMenuItem.setSelected( true );
		titleMenuItem.setSelected( true );
		lengthMenuItem.setSelected( true );
		trackColumnSelectorMenu.getItems().addAll( 
			artistMenuItem, albumMenuItem, numberMenuItem, titleMenuItem, lengthMenuItem, defaultMenuItem );
		
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
		defaultMenuItem.setOnAction( ( e ) -> this.resetTrackTableSettingsToDefault() );
		
		trackTable = new TableView();
		trackTable.getColumns().addAll( 
			trackArtistColumn, trackAlbumColumn, trackNumberColumn, trackTitleColumn, trackLengthColumn );
		
		trackTable.setEditable( false );
		trackTable.setItems( library.getTracksSorted() );

		library.getTracksSorted().comparatorProperty().bind( trackTable.comparatorProperty() );
		
		trackTable.getSelectionModel().clearSelection();
		
		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		trackTable.setColumnResizePolicy( resizePolicy );
		resizePolicy.registerFixedWidthColumns( trackNumberColumn, trackLengthColumn );
		
		emptyTrackListLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyTrackListLabel.setWrapText( true );
		emptyTrackListLabel.setTextAlignment( TextAlignment.CENTER );
		trackTable.setPlaceholder( emptyTrackListLabel );
		
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
				if ( trackFilterBox.getText().length() > 0 ) {
					trackFilterBox.clear();
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
					trackFilterBox.requestFocus();
				}
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
	
	@SuppressWarnings("unchecked")
	public void resetPlaylistTableSettingsToDefault() {
		playlistNameColumn.setVisible( true );
		playlistLengthColumn.setVisible( true );
		playlistTracksColumn.setVisible( true );
		
		playlistTable.getColumns().remove( playlistNameColumn );
		playlistTable.getColumns().add( playlistNameColumn );
		playlistTable.getColumns().remove( playlistLengthColumn );
		playlistTable.getColumns().add( playlistLengthColumn );
		playlistTable.getColumns().remove( playlistTracksColumn );
		playlistTable.getColumns().add( playlistTracksColumn );

		playlistTable.getSortOrder().clear();
		
		playlistNameColumn.setPrefWidth( 100 );
		playlistTracksColumn.setPrefWidth( 90 );
		playlistLengthColumn.setPrefWidth( 90 );
		playlistTable.getColumnResizePolicy().call(new ResizeFeatures ( playlistTable, null, 0d ) );
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

		playlistColumnSelectorMenu = new ContextMenu ();
		CheckMenuItem nameMenuItem = new CheckMenuItem ( "Show Name Column" );
		CheckMenuItem tracksMenuItem = new CheckMenuItem ( "Show Tracks Column" );
		CheckMenuItem lengthMenuItem = new CheckMenuItem ( "Show Length Column" );
		MenuItem defaultMenuItem = new MenuItem ( "Reset to Default View" );
		nameMenuItem.setSelected( true );
		tracksMenuItem.setSelected( true );
		lengthMenuItem.setSelected( true );
		playlistColumnSelectorMenu.getItems().addAll( nameMenuItem, tracksMenuItem, lengthMenuItem, defaultMenuItem );
		playlistNameColumn.setContextMenu( playlistColumnSelectorMenu );
		playlistTracksColumn.setContextMenu( playlistColumnSelectorMenu );
		playlistLengthColumn.setContextMenu( playlistColumnSelectorMenu );
		nameMenuItem.selectedProperty().bindBidirectional( playlistNameColumn.visibleProperty() );
		tracksMenuItem.selectedProperty().bindBidirectional( playlistTracksColumn.visibleProperty() );
		lengthMenuItem.selectedProperty().bindBidirectional( playlistLengthColumn.visibleProperty() );
		defaultMenuItem.setOnAction( ( e ) -> this.resetPlaylistTableSettingsToDefault() );

		playlistTable = new TableView<Playlist>();
		playlistTable.getColumns().addAll( playlistNameColumn, playlistTracksColumn, playlistLengthColumn );
		playlistTable.setEditable( false );
		playlistTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		playlistTable.setItems( library.getPlaylistSorted() );

		library.getPlaylistSorted().comparatorProperty().bind( playlistTable.comparatorProperty() );
		
		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		playlistTable.setColumnResizePolicy( resizePolicy );
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
		MenuItem exportM3UMenuItem = new MenuItem( "Export as M3U" );
		MenuItem exportFolderMenuItem = new MenuItem ( "Export as Folder" );
		MenuItem removeMenuItem = new MenuItem( "Remove" );
		
		
		contextMenu.getItems().addAll( playMenuItem, appendMenuItem, playNextMenuItem, enqueueMenuItem, 
				renameMenuItem, infoMenuItem, exportM3UMenuItem, exportFolderMenuItem, removeMenuItem );

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
				ui.alertUser ( AlertType.ERROR, "Warning", "Unable to save playlist.", "Unable to save the playlist to the specified location", 400 );
			}
			
		});
		
		exportFolderMenuItem.setOnAction( ( ActionEvent event ) -> {
			File targetFile = ui.promptUserForFolder();
			if ( targetFile == null ) {
				return;
			}
			
			List<Track> tracks = playlistTable.getSelectionModel().getSelectedItem().getTracks();
			
			//TODO: Get rid of Hypnos.get
			Hypnos.getPersister().exportTracksToFolder ( tracks, targetFile.toPath() );
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
				if ( playlistFilterBox.getText().length() > 0 ) {
					playlistFilterBox.clear();
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
					playlistFilterBox.requestFocus();
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
					
					List <Playlist> serializableList = new ArrayList<Playlist> ( selectedPlaylists );
					
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
						//TODO: use dropIndex
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

	public void applyLightTheme () {
		if ( albumFilterClearImage != null ) albumFilterClearImage.setEffect( ui.lightThemeButtonEffect );
		if ( trackFilterClearImage != null ) trackFilterClearImage.setEffect( ui.lightThemeButtonEffect );
		if ( playlistFilterClearImage != null ) playlistFilterClearImage.setEffect( ui.lightThemeButtonEffect );
		if ( addSourceTracksImage != null ) addSourceTracksImage.setEffect( ui.lightThemeButtonEffect );
		if ( addSourceAlbumsImage != null ) addSourceAlbumsImage.setEffect( ui.lightThemeButtonEffect );
		if ( addSourcePlaylistsImage != null ) addSourcePlaylistsImage.setEffect( ui.lightThemeButtonEffect );
	}

	public void focusFilterOfCurrentTab () {
		if( getSelectionModel().getSelectedItem() == albumTab ) {
			albumTable.requestFocus();
			albumFilterBox.requestFocus();
			albumTable.getSelectionModel().clearSelection();
			
		} else if( getSelectionModel().getSelectedItem() == trackTab ) {
			trackTable.requestFocus();
			trackFilterBox.requestFocus();
			trackTable.getSelectionModel().clearSelection();
			
		} else if( getSelectionModel().getSelectedItem() == playlistTab ) {
			playlistTable.requestFocus();
			playlistFilterBox.requestFocus();
			playlistTable.getSelectionModel().clearSelection();
		}
	}

	@SuppressWarnings("incomplete-switch")
	public void applySettingsBeforeWindowShown ( EnumMap<Persister.Setting, String> settings ) {
		settings.forEach( ( setting, value )-> {
			try {
				switch ( setting ) {
					case HIDE_ALBUM_TRACKS:
						ui.runThreadSafe ( () -> trackListCheckBox.setSelected( Boolean.valueOf ( value ) ) );
						settings.remove ( setting );
						break;		
					case LIBRARY_TAB:
						getSelectionModel().select( Integer.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case AL_TABLE_ARTIST_COLUMN_SHOW:
						albumArtistColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case AL_TABLE_YEAR_COLUMN_SHOW:
						albumYearColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case AL_TABLE_ALBUM_COLUMN_SHOW:
						albumAlbumColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;					
					case AL_TABLE_ADDED_COLUMN_SHOW:
						albumAddedDateColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_ARTIST_COLUMN_SHOW:
						trackArtistColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_NUMBER_COLUMN_SHOW:
						trackNumberColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_TITLE_COLUMN_SHOW:
						trackTitleColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_ALBUM_COLUMN_SHOW:
						trackAlbumColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_LENGTH_COLUMN_SHOW:
						trackLengthColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case PL_TABLE_PLAYLIST_COLUMN_SHOW:
						playlistNameColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case PL_TABLE_TRACKS_COLUMN_SHOW: 
						playlistTracksColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case PL_TABLE_LENGTH_COLUMN_SHOW:
						playlistLengthColumn.setVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case AL_TABLE_ARTIST_COLUMN_WIDTH: 
						albumArtistColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case AL_TABLE_YEAR_COLUMN_WIDTH: 
						albumYearColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case AL_TABLE_ADDED_COLUMN_WIDTH: 
						albumAddedDateColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case AL_TABLE_ALBUM_COLUMN_WIDTH: 
						albumAlbumColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_ARTIST_COLUMN_WIDTH: 
						trackArtistColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
					break;
					case TR_TABLE_NUMBER_COLUMN_WIDTH: 
						trackNumberColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_TITLE_COLUMN_WIDTH:
						trackTitleColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_ALBUM_COLUMN_WIDTH:
						trackAlbumColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case TR_TABLE_LENGTH_COLUMN_WIDTH:
						trackLengthColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case PL_TABLE_PLAYLIST_COLUMN_WIDTH: 
						playlistNameColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case PL_TABLE_TRACKS_COLUMN_WIDTH:
						playlistTracksColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case PL_TABLE_LENGTH_COLUMN_WIDTH:
						playlistLengthColumn.setPrefWidth( Double.valueOf( value ) );
						settings.remove ( setting );
						break;
					case ALBUM_COLUMN_ORDER: {
						String[] order = value.split( " " );
						int newIndex = 0;
						
						for ( String columnName : order ) {
							try {
								if ( columnName.equals( "artist" ) ) {
									albumTable.getColumns().remove( albumArtistColumn );
									albumTable.getColumns().add( newIndex, albumArtistColumn );
								} else if ( columnName.equals( "year" ) ) {
									albumTable.getColumns().remove( albumYearColumn );
									albumTable.getColumns().add( newIndex, albumYearColumn );
								} else if ( columnName.equals( "album" ) ) {
									albumTable.getColumns().remove( albumAlbumColumn );
									albumTable.getColumns().add( newIndex, albumAlbumColumn );
								} else if ( columnName.equals( "added" ) ) {
									albumTable.getColumns().remove( albumAddedDateColumn );
									albumTable.getColumns().add( newIndex, albumAddedDateColumn );
								}
								newIndex++;
							} catch ( Exception e ) {
								LOGGER.log( Level.INFO, "Unable to set album table column order: '" + value + "'", e );
							}
							
						}
						settings.remove ( setting );
						break;
					}
					
					case TRACK_COLUMN_ORDER: {
						String[] order = value.split( " " );
						int newIndex = 0;

						for ( String columnName : order ) {
							try {
								if ( columnName.equals( "artist" ) ) {
									trackTable.getColumns().remove( trackArtistColumn );
									trackTable.getColumns().add( newIndex, trackArtistColumn );
								} else if ( columnName.equals( "length" ) ) {
									trackTable.getColumns().remove( trackLengthColumn );
									trackTable.getColumns().add( newIndex, trackLengthColumn );
								} else if ( columnName.equals( "number" ) ) {
									trackTable.getColumns().remove( trackNumberColumn );
									trackTable.getColumns().add( newIndex, trackNumberColumn );
								} else if ( columnName.equals( "album" ) ) {
									trackTable.getColumns().remove( trackAlbumColumn );
									trackTable.getColumns().add( newIndex, trackAlbumColumn );
								} else if ( columnName.equals( "title" ) ) {
									trackTable.getColumns().remove( trackTitleColumn );
									trackTable.getColumns().add( newIndex, trackTitleColumn );
								} 
								newIndex++;
							} catch ( Exception e ) {
								LOGGER.log( Level.INFO, "Unable to set album table column order: '" + value + "'", e );
							}
							
						}
						settings.remove ( setting );
						break;
					}
					
					case PLAYLIST_COLUMN_ORDER: {
						String[] order = value.split( " " );
						int newIndex = 0;

						for ( String columnName : order ) {
							try {
								if ( columnName.equals( "name" ) ) {
									playlistTable.getColumns().remove( playlistNameColumn );
									playlistTable.getColumns().add( newIndex, playlistNameColumn );
								} else if ( columnName.equals( "tracks" ) ) {
									playlistTable.getColumns().remove( playlistTracksColumn );
									playlistTable.getColumns().add( newIndex, playlistTracksColumn );
								} else if ( columnName.equals( "length" ) ) {
									playlistTable.getColumns().remove( playlistLengthColumn );
									playlistTable.getColumns().add( newIndex, playlistLengthColumn );
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
										albumTable.getSortOrder().add( albumArtistColumn );
										albumArtistColumn.setSortType( sortType );
									} else if ( columnName.equals( "year" ) ) {
										albumTable.getSortOrder().add( albumYearColumn );
										albumYearColumn.setSortType( sortType );
									} else if ( columnName.equals( "album" ) ) {
										albumTable.getSortOrder().add( albumAlbumColumn );
										albumAlbumColumn.setSortType( sortType );
									} else if ( columnName.equals( "added" ) ) {
										albumTable.getSortOrder().add( albumAddedDateColumn );
										albumAddedDateColumn.setSortType( sortType );
									}
								} catch ( Exception e ) {
									LOGGER.log( Level.INFO, "Unable to set album table sort order: '" + value + "'", e );
								}
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
										trackTable.getSortOrder().add( trackArtistColumn );
										trackArtistColumn.setSortType( sortType );
									} else if ( columnName.equals( "length" ) ) {
										trackTable.getSortOrder().add( trackLengthColumn );
										trackLengthColumn.setSortType( sortType );
									} else if ( columnName.equals( "number" ) ) {
										trackTable.getSortOrder().add( trackNumberColumn );
										trackNumberColumn.setSortType( sortType );
									} else if ( columnName.equals( "album" ) ) {
										trackTable.getSortOrder().add( trackAlbumColumn );
										trackAlbumColumn.setSortType( sortType );
									} else if ( columnName.equals( "title" ) ) {
										trackTable.getSortOrder().add( trackTitleColumn );
										trackTitleColumn.setSortType( sortType );
									}
									
								} catch ( Exception e ) {
									LOGGER.log( Level.INFO, "Unable to set album table sort order: '" + value + "'", e );
								}
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
										playlistTable.getSortOrder().add( playlistNameColumn );
										playlistNameColumn.setSortType( sortType );
									} else if ( columnName.equals( "tracks" ) ) {
										playlistTable.getSortOrder().add( playlistTracksColumn );
										playlistTracksColumn.setSortType( sortType );
									} else if ( columnName.equals( "length" ) ) {
										playlistTable.getSortOrder().add( playlistLengthColumn );
										playlistLengthColumn.setSortType( sortType );
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

	@SuppressWarnings("incomplete-switch")
	public void applySettingsAfterWindowShown ( EnumMap <Setting, String> settings ) {
		settings.forEach( ( setting, value )-> {
			try {
				switch ( setting ) {
					case LIBRARY_TAB_ALBUMS_VISIBLE:
						setAlbumsVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case LIBRARY_TAB_TRACKS_VISIBLE:
						setTracksVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case LIBRARY_TAB_PLAYLISTS_VISIBLE:
						setPlaylistsVisible( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
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
			if ( column == albumArtistColumn ) {
				albumColumnOrderValue += "artist ";
			} else if ( column == albumYearColumn ) {
				albumColumnOrderValue += "year ";
			} else if ( column == albumAddedDateColumn ) {
				albumColumnOrderValue += "added ";
			} else if ( column == albumAlbumColumn ) {
				albumColumnOrderValue += "album ";
			}
		}
		retMe.put ( Setting.ALBUM_COLUMN_ORDER, albumColumnOrderValue );
		
		String albumSortValue = "";
		for ( TableColumn<Album, ?> column : albumTable.getSortOrder() ) {
			if ( column == albumArtistColumn ) {
				albumSortValue += "artist-" + albumArtistColumn.getSortType() + " ";
			} else if ( column == albumYearColumn ) {
				albumSortValue += "year-" + albumYearColumn.getSortType() + " ";
			} else if ( column == albumAlbumColumn ) {
				albumSortValue += "album-" + albumAlbumColumn.getSortType() + " ";
			} else if ( column == this.albumAddedDateColumn ) {
				albumSortValue += "added-" + albumAddedDateColumn.getSortType() + " ";
			}
		}
		retMe.put ( Setting.ALBUM_SORT_ORDER, albumSortValue );
		
		String trackColumnOrderValue = "";
		for ( TableColumn<Track, ?> column : trackTable.getColumns() ) {
			if ( column == trackArtistColumn ) {
				trackColumnOrderValue += "artist ";
			} else if ( column == trackLengthColumn ) {
				trackColumnOrderValue += "length ";
			} else if ( column == trackNumberColumn ) {
				trackColumnOrderValue += "number ";
			} else if ( column == trackAlbumColumn ) {
				trackColumnOrderValue += "album ";
			} else if ( column == trackTitleColumn ) {
				trackColumnOrderValue += "title ";
			} 
		}
		retMe.put ( Setting.TRACK_COLUMN_ORDER, trackColumnOrderValue );
		
		String trackSortValue = "";
		for ( TableColumn<Track, ?> column : trackTable.getSortOrder() ) {
			if ( column == trackArtistColumn ) {
				trackSortValue += "artist-" + trackArtistColumn.getSortType() + " ";
			} else if ( column == trackLengthColumn ) {
				trackSortValue += "length-" + trackLengthColumn.getSortType() + " ";
			} else if ( column == trackNumberColumn ) {
				trackSortValue += "number-" + trackNumberColumn.getSortType() + " ";
			} else if ( column == trackAlbumColumn ) {
				trackSortValue += "album-" + trackAlbumColumn.getSortType() + " ";
			} else if ( column == trackTitleColumn ) {
				trackSortValue += "title-" + trackTitleColumn.getSortType() + " ";
			} 
		}
		retMe.put ( Setting.TRACK_SORT_ORDER, trackSortValue );
		
		String playlistColumnOrderValue = "";
		for ( TableColumn<Playlist, ?> column : playlistTable.getColumns() ) {
			if ( column == this.playlistNameColumn ) {
				playlistColumnOrderValue += "name ";
			} else if ( column == this.playlistTracksColumn ) {
				playlistColumnOrderValue += "tracks ";
			} else if ( column == this.playlistLengthColumn ) {
				playlistColumnOrderValue += "length ";
			}
		}
		retMe.put ( Setting.PLAYLIST_COLUMN_ORDER, playlistColumnOrderValue );
		
		String playlistSortValue = "";
		for ( TableColumn<Playlist, ?> column : playlistTable.getSortOrder() ) {
			if ( column == this.playlistNameColumn ) {
				playlistSortValue += "name-" + playlistNameColumn.getSortType() + " ";
			} else if ( column == this.playlistTracksColumn ) {
				playlistSortValue += "tracks-" + playlistTracksColumn.getSortType() + " ";
			} else if ( column == this.playlistLengthColumn ) {
				playlistSortValue += "length-" + playlistLengthColumn.getSortType() + " ";
			}
		}
		retMe.put ( Setting.PLAYLIST_SORT_ORDER, playlistSortValue );
		
		retMe.put ( Setting.LIBRARY_TAB, getSelectionModel().getSelectedIndex() );
		retMe.put ( Setting.AL_TABLE_ARTIST_COLUMN_SHOW, albumArtistColumn.isVisible() );
		retMe.put ( Setting.AL_TABLE_YEAR_COLUMN_SHOW, albumYearColumn.isVisible() );
		retMe.put ( Setting.AL_TABLE_ALBUM_COLUMN_SHOW, albumAlbumColumn.isVisible() );
		retMe.put ( Setting.AL_TABLE_ADDED_COLUMN_SHOW, albumAddedDateColumn.isVisible() );
		retMe.put ( Setting.TR_TABLE_ARTIST_COLUMN_SHOW, trackArtistColumn.isVisible() );
		retMe.put ( Setting.TR_TABLE_NUMBER_COLUMN_SHOW, trackNumberColumn.isVisible() );
		retMe.put ( Setting.TR_TABLE_TITLE_COLUMN_SHOW, trackTitleColumn.isVisible() );
		retMe.put ( Setting.TR_TABLE_ALBUM_COLUMN_SHOW, trackAlbumColumn.isVisible() );
		retMe.put ( Setting.TR_TABLE_LENGTH_COLUMN_SHOW, trackLengthColumn.isVisible() );
		retMe.put ( Setting.PL_TABLE_PLAYLIST_COLUMN_SHOW, playlistNameColumn.isVisible() );
		retMe.put ( Setting.PL_TABLE_TRACKS_COLUMN_SHOW, playlistTracksColumn.isVisible() );
		retMe.put ( Setting.PL_TABLE_LENGTH_COLUMN_SHOW, playlistLengthColumn.isVisible() );
		
		retMe.put ( Setting.AL_TABLE_ARTIST_COLUMN_WIDTH, albumArtistColumn.getPrefWidth() );
		retMe.put ( Setting.AL_TABLE_YEAR_COLUMN_WIDTH, albumYearColumn.getPrefWidth() );
		retMe.put ( Setting.AL_TABLE_ADDED_COLUMN_WIDTH, albumAddedDateColumn.getPrefWidth() );
		retMe.put ( Setting.AL_TABLE_ALBUM_COLUMN_WIDTH, albumAlbumColumn.getPrefWidth() );
		retMe.put ( Setting.TR_TABLE_ARTIST_COLUMN_WIDTH, trackArtistColumn.getPrefWidth() );
		retMe.put ( Setting.TR_TABLE_NUMBER_COLUMN_WIDTH, trackNumberColumn.getPrefWidth() );
		retMe.put ( Setting.TR_TABLE_TITLE_COLUMN_WIDTH, trackTitleColumn.getPrefWidth() );
		retMe.put ( Setting.TR_TABLE_ALBUM_COLUMN_WIDTH, trackAlbumColumn.getPrefWidth() );
		retMe.put ( Setting.TR_TABLE_LENGTH_COLUMN_WIDTH, trackLengthColumn.getPrefWidth() );
		retMe.put ( Setting.PL_TABLE_PLAYLIST_COLUMN_WIDTH, playlistNameColumn.getPrefWidth() );
		retMe.put ( Setting.PL_TABLE_TRACKS_COLUMN_WIDTH, playlistTracksColumn.getPrefWidth() );
		retMe.put ( Setting.PL_TABLE_LENGTH_COLUMN_WIDTH, playlistLengthColumn.getPrefWidth() );
		
		retMe.put ( Setting.LIBRARY_TAB_ALBUMS_VISIBLE, getTabs().contains( albumTab ) );
		retMe.put ( Setting.LIBRARY_TAB_TRACKS_VISIBLE, getTabs().contains( trackTab ) );
		retMe.put ( Setting.LIBRARY_TAB_PLAYLISTS_VISIBLE, getTabs().contains( playlistTab ) );
		
		retMe.put ( Setting.HIDE_ALBUM_TRACKS, trackListCheckBox.isSelected() );
		
		return retMe;
	}

	public void setLabelsToLoading () {
		albumTable.setPlaceholder( loadingAlbumListLabel );
		trackTable.setPlaceholder( loadingTrackListLabel );
		playlistTable.setPlaceholder( loadingPlaylistListLabel );
	}
}
