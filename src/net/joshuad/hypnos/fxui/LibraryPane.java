package net.joshuad.hypnos.fxui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import net.joshuad.hypnos.Library;
import net.joshuad.hypnos.Persister;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.Persister.Setting;

public class LibraryPane extends BorderPane {
	private static final Logger LOGGER = Logger.getLogger( LibraryPane.class.getName() );
	
	FXUI ui;
	AudioSystem audioSystem;
	Library library;

	LibraryArtistPane artistPane;
	LibraryAlbumPane albumPane;
	LibraryTrackPane trackPane;
	LibraryPlaylistPane playlistPane;

	ContextMenu tabMenu;
	CheckMenuItem showArtists, showAlbums, showTracks, showPlaylists;
	
	ToggleGroup buttonGroup;
	ToggleButton artistsButton, albumsButton, tracksButton, playlistsButton; 
	HBox buttonBox;
	
	public LibraryPane ( FXUI ui, AudioSystem audioSystem, Library library ) {
		this.ui = ui;
		this.audioSystem = audioSystem;
		this.library = library; 

		artistPane = new LibraryArtistPane ( ui, audioSystem, library );
		albumPane = new LibraryAlbumPane ( ui, audioSystem, library );
		trackPane = new LibraryTrackPane ( ui, audioSystem, library );
		playlistPane = new LibraryPlaylistPane ( ui, audioSystem, library );
		
		tabMenu = new ContextMenu();
		showArtists = new CheckMenuItem ( "Artists" );
		showAlbums = new CheckMenuItem ( "Albums" );
		showTracks = new CheckMenuItem ( "Tracks" );
		showPlaylists = new CheckMenuItem ( "Playlists" );
		tabMenu.getItems().addAll( showArtists, showAlbums, showTracks, showPlaylists );

		showArtists.setSelected( true );
		showAlbums.setSelected( true );
		showTracks.setSelected( true );
		showPlaylists.setSelected( true );
		
		showArtists.selectedProperty().addListener( ( observable, oldValue, newValue ) -> {
			setArtistsVisible ( newValue );
		});
		
		showAlbums.selectedProperty().addListener( ( observable, oldValue, newValue ) -> {
			setAlbumsVisible ( newValue );
		});
		
		showTracks.selectedProperty().addListener( ( observable, oldValue, newValue ) -> {
			setTracksVisible ( newValue );
		});
		
		showPlaylists.selectedProperty().addListener( ( observable, oldValue, newValue ) -> {
			setPlaylistsVisible ( newValue );
		});
		
    	artistsButton = new ToggleButton( "Artists" );
    	albumsButton = new ToggleButton( "Albums" );
    	tracksButton = new ToggleButton( "Tracks" );
    	playlistsButton = new ToggleButton( "Playlists" );

		artistsButton.setContextMenu( tabMenu );
		albumsButton.setContextMenu( tabMenu );
		tracksButton.setContextMenu( tabMenu );
		playlistsButton.setContextMenu( tabMenu );
    	
    	artistsButton.setOnAction( e -> { 
    		setCenter( artistPane ); 
    		artistsButton.setSelected ( true ); 
    	});
    	
    	albumsButton.setOnAction( e -> { 
    		setCenter( albumPane ); 
    		albumsButton.setSelected ( true ); 
    	} );
    	
    	tracksButton.setOnAction( e -> { 
    		setCenter( trackPane ); 
    		tracksButton.setSelected ( true ); 
    	} );
    	
    	playlistsButton.setOnAction( e -> { 
    		setCenter( playlistPane ); 
    		playlistsButton.setSelected ( true ); 
    	} );
    	
    	artistsButton.focusedProperty().addListener( event -> artistsButton.fire() );
    	albumsButton.focusedProperty().addListener( event -> albumsButton.fire() );
    	tracksButton.focusedProperty().addListener( event -> tracksButton.fire() );
    	playlistsButton.focusedProperty().addListener( event -> playlistsButton.fire() );
    	
    	buttonGroup = new ToggleGroup();
    	artistsButton.setToggleGroup( buttonGroup );
    	albumsButton.setToggleGroup( buttonGroup );
    	tracksButton.setToggleGroup( buttonGroup );
    	playlistsButton.setToggleGroup( buttonGroup );
    	
    	artistsButton.setPrefWidth ( 1000000 );
    	albumsButton.setPrefWidth ( 1000000 );
    	tracksButton.setPrefWidth ( 1000000 );
    	playlistsButton.setPrefWidth ( 1000000 );
    	
    	artistsButton.setMinWidth ( 0 );
    	albumsButton.setMinWidth ( 0 );
    	tracksButton.setMinWidth ( 0 );
    	playlistsButton.setMinWidth ( 0 );

    	setupTooltip ( artistsButton, library.getArtists(), "Artist Count: " );   
    	setupTooltip ( albumsButton, library.getAlbums(), "Album Count: " ); 
    	setupTooltip ( tracksButton, library.getTracks(), "Track Count: " ); 
    	setupTooltip ( playlistsButton, library.getPlaylists(), "Playlist Count: " );  	    	    	
    	
		buttonBox = new HBox();
		buttonBox.getChildren().addAll( artistsButton, albumsButton, tracksButton, playlistsButton );
		buttonBox.maxWidthProperty().bind( this.widthProperty().subtract( 1 ) );
		artistsButton.fire();
		setBottom ( buttonBox );
		setMinWidth( 0 );
	}
	
	private void setupTooltip ( ToggleButton button, ObservableList<? extends Object> list, String text ) {
		Tooltip tooltip = new Tooltip ( text + list.size() );
		button.setTooltip( tooltip );
		list.addListener( new ListChangeListener<Object> () {
			@Override
			public void onChanged ( Change <? extends Object> changed ) {
				tooltip.setText( text + list.size() );
			}
		});
	}
	
	private void fixButtonOrder() {
		List<Node> buttons = new ArrayList<Node> ( buttonBox.getChildren() );
		List<Node> reorderedButtons = new ArrayList<> ( buttons.size() );
		
		if ( buttons.contains( artistsButton ) ) {
			reorderedButtons.add( artistsButton );
		}
		
		if ( buttons.contains( albumsButton ) ) {
			reorderedButtons.add( albumsButton );
		}

		if ( buttons.contains( tracksButton ) ) {
			reorderedButtons.add( tracksButton );
		}
		
		if ( buttons.contains( playlistsButton ) ) {
			reorderedButtons.add( playlistsButton );
		}

		buttonBox.getChildren().remove( artistsButton );
		buttonBox.getChildren().remove( albumsButton );
		buttonBox.getChildren().remove( tracksButton );
		buttonBox.getChildren().remove( playlistsButton );
		buttonBox.getChildren().addAll( reorderedButtons );
	}
	
	public void setArtistsVisible ( boolean makeVisible ) {
		if ( makeVisible ) {
			if ( !buttonBox.getChildren().contains( artistsButton ) ) {
				buttonBox.getChildren().add( artistsButton );
				showArtists.setSelected( true );
				fixButtonOrder();
				artistsButton.fire();
				artistsButton.requestFocus();
			}
		} else {
			if ( buttonBox.getChildren().size() >= 2 ) {
				buttonBox.getChildren().remove( artistsButton );
				fixButtonOrder();
				showArtists.setSelected( false );
				//TODO: I kind of hate this solution, but it is what I could find. 
				Thread delayedAction = new Thread ( () -> {
					try {
						Thread.sleep ( 50 );
					} catch (InterruptedException e) {}
					Platform.runLater( ()-> ((ToggleButton)buttonBox.getChildren().get( 0 )).fire() );
				});
				
				delayedAction.start();
			} else {
				showArtists.setSelected( true );
			}
		}
	}
	
	public void setAlbumsVisible ( boolean visible ) {
		if ( visible ) {
			if ( !buttonBox.getChildren().contains( albumsButton ) ) {
				buttonBox.getChildren().add( albumsButton );
				albumsButton.fire();
				showAlbums.setSelected( true );
				fixButtonOrder();
				albumsButton.fire();
				albumsButton.requestFocus();
			}
		} else {
			if ( buttonBox.getChildren().size() >= 2 ) {
				buttonBox.getChildren().remove( albumsButton );
				showAlbums.setSelected( false );
				fixButtonOrder();
				//TODO: I kind of hate this solution, but it is what I could find. 
				Thread delayedAction = new Thread ( () -> {
					try {
						Thread.sleep ( 50 );
					} catch (InterruptedException e) {}
					Platform.runLater( ()-> ((ToggleButton)buttonBox.getChildren().get( 0 )).fire() );
				});
				
				delayedAction.start();
			}
		}
	}
	
	public void setTracksVisible ( boolean visible ) {
		if ( visible ) {
			if ( !buttonBox.getChildren().contains( tracksButton ) ) {
				buttonBox.getChildren().add( tracksButton );
				showTracks.setSelected( true );
				fixButtonOrder();
				tracksButton.fire();
				tracksButton.requestFocus();
			}
		} else {
			if ( buttonBox.getChildren().size() >= 2 ) {
				buttonBox.getChildren().remove( tracksButton );
				showTracks.setSelected( false );
				fixButtonOrder();
				//TODO: I kind of hate this solution, but it is what I could find. 
				Thread delayedAction = new Thread ( () -> {
					try {
						Thread.sleep ( 50 );
					} catch (InterruptedException e) {}
					Platform.runLater( ()-> ((ToggleButton)buttonBox.getChildren().get( 0 )).fire() );
				});
				delayedAction.start();
			} 
		}
	}
	
	public void setPlaylistsVisible ( boolean visible ) {
		if ( visible ) {
			if ( !buttonBox.getChildren().contains( playlistsButton ) ) {
				buttonBox.getChildren().add( playlistsButton );
				showPlaylists.setSelected( true );
				fixButtonOrder();
				playlistsButton.fire();
				playlistsButton.requestFocus();
			}
		} else {
			if ( buttonBox.getChildren().size() >= 2 ) {
				buttonBox.getChildren().remove( playlistsButton );
				showPlaylists.setSelected( false );
				fixButtonOrder();
				//TODO: I kind of hate this solution, but it is what I could find. 
				Thread delayedAction = new Thread ( () -> {
					try {
						Thread.sleep ( 50 );
					} catch (InterruptedException e) {}
					Platform.runLater( ()-> ((ToggleButton)buttonBox.getChildren().get( 0 )).fire() );
				});
				delayedAction.start();
			} 
		}
	}
	
	/* Begin: These three methods go together */
	
		private boolean attachEmptySelectorMenu( TableView<?> table, ContextMenu menu ) {
			Node blankHeader = table.lookup(".column-header-background");
			if ( blankHeader != null ) {
				blankHeader.setOnContextMenuRequested ( event -> menu.show( blankHeader, event.getScreenX(), event.getScreenY() ));
				return true;
			} else {
				return false;
			}
		}
		
		private void attachEmptySelectorMenuLater(  Pane pane, TableView<?> table, ContextMenu menu ) {
			final ChangeListener<Node> listener = new ChangeListener<Node>() {
				@Override
				public void changed ( ObservableValue< ? extends Node> obs, Node oldCenter, Node newCenter ) {
					if ( newCenter == pane ) {
						Thread runMe = new Thread( () -> {
							try {
								Thread.sleep( 50 );
							} catch (InterruptedException e) {}

							Platform.runLater( () -> {
								attachEmptySelectorMenu ( table, menu );
								centerProperty().removeListener( this );
							});
						});
						
						runMe.start();
					}
				}
			};
			
			this.centerProperty().addListener( listener );
		}
		
		private void setupEmptyTableHeaderConextMenu ( Pane pane, TableView<?> table, ContextMenu menu ) {
			boolean artistAttached = attachEmptySelectorMenu ( table, menu );
			
			if ( !artistAttached ) {
				attachEmptySelectorMenuLater ( pane, table, menu );
			}
				
		}
	/* End: These three methods go together */
	
	public void doAfterShowProcessing () {
		setupEmptyTableHeaderConextMenu ( artistPane, artistPane.artistTable, artistPane.columnSelectorMenu );
		setupEmptyTableHeaderConextMenu ( albumPane, albumPane.albumTable, albumPane.columnSelectorMenu );
		setupEmptyTableHeaderConextMenu ( trackPane, trackPane.trackTable, trackPane.columnSelectorMenu );
		setupEmptyTableHeaderConextMenu ( playlistPane, playlistPane.playlistTable, playlistPane.columnSelectorMenu );
	}

	public void applyDarkTheme ( ColorAdjust buttonColor ) {
		artistPane.applyDarkTheme ( buttonColor );
		albumPane.applyDarkTheme( buttonColor );
		trackPane.applyDarkTheme( buttonColor );
		playlistPane.applyDarkTheme( buttonColor );
	}

	public void applyLightTheme () {
		artistPane.applyLightTheme();
		albumPane.applyLightTheme();
		trackPane.applyLightTheme();
		playlistPane.applyLightTheme();
	}

	public void focusFilterOfCurrentTab () {
		if( buttonGroup.getSelectedToggle() == artistsButton ) {
			artistPane.focusFilter();
			
		} else if( buttonGroup.getSelectedToggle() == albumsButton ) {
			albumPane.focusFilter();
			
		} else if( buttonGroup.getSelectedToggle() == tracksButton ) {
			trackPane.focusFilter();
			
		} else if( buttonGroup.getSelectedToggle() == playlistsButton ) {
			playlistPane.focusFilter();
		}
	}

	@SuppressWarnings("incomplete-switch")
	public void applySettingsBeforeWindowShown ( EnumMap<Persister.Setting, String> settings ) {

		
		settings.forEach( ( setting, value )-> {
			try {
				switch ( setting ) {
				}
			} catch ( Exception e ) {
				LOGGER.log( Level.INFO, "Unable to apply setting: " + setting + " to UI.", e );
			}
		});
		
		artistPane.applySettingsBeforeWindowShown ( settings );
		albumPane.applySettingsBeforeWindowShown ( settings );
		trackPane.applySettingsBeforeWindowShown ( settings );
		playlistPane.applySettingsBeforeWindowShown ( settings );
	}

	@SuppressWarnings("incomplete-switch")
	public void applySettingsAfterWindowShown ( EnumMap <Setting, String> settings ) {
		settings.forEach( ( setting, value )-> {
			try {
				switch ( setting ) {
					case LIBRARY_TAB_ARTISTS_VISIBLE:
						showArtists.setSelected ( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case LIBRARY_TAB_ALBUMS_VISIBLE:
						showAlbums.setSelected ( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case LIBRARY_TAB_TRACKS_VISIBLE:
						showTracks.setSelected ( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case LIBRARY_TAB_PLAYLISTS_VISIBLE:
						showPlaylists.setSelected ( Boolean.valueOf ( value ) );
						settings.remove ( setting );
						break;
					case LIBRARY_TAB:
						ToggleButton selectMe = (ToggleButton)buttonBox.getChildren().get ( Integer.valueOf ( value ) );
						selectMe.fire();
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
		
		retMe.put ( Setting.LIBRARY_TAB, buttonBox.getChildren().indexOf( (ToggleButton)buttonGroup.getSelectedToggle() ) );

		retMe.put ( Setting.LIBRARY_TAB_ARTISTS_VISIBLE, buttonBox.getChildren().contains( artistsButton ) );
		retMe.put ( Setting.LIBRARY_TAB_ALBUMS_VISIBLE, buttonBox.getChildren().contains( albumsButton ) );
		retMe.put ( Setting.LIBRARY_TAB_TRACKS_VISIBLE, buttonBox.getChildren().contains( tracksButton ) );
		retMe.put ( Setting.LIBRARY_TAB_PLAYLISTS_VISIBLE, buttonBox.getChildren().contains( playlistsButton ) );
		
		EnumMap <Persister.Setting, ? extends Object> artistTabSettings = artistPane.getSettings();
		for ( Persister.Setting setting : artistTabSettings.keySet() ) {
			retMe.put( setting, artistTabSettings.get( setting ) );
		}
		
		EnumMap <Persister.Setting, ? extends Object> albumTabSettings = albumPane.getSettings();
		for ( Persister.Setting setting : albumTabSettings.keySet() ) {
			retMe.put( setting, albumTabSettings.get( setting ) );
		}
		
		EnumMap <Persister.Setting, ? extends Object> trackTabSettings = trackPane.getSettings();
		for ( Persister.Setting setting : trackTabSettings.keySet() ) {
			retMe.put( setting, trackTabSettings.get( setting ) );
		}
		
		EnumMap <Persister.Setting, ? extends Object> playlistTabSettings = playlistPane.getSettings();
		for ( Persister.Setting setting : playlistTabSettings.keySet() ) {
			retMe.put( setting, playlistTabSettings.get( setting ) );
		}
		
		return retMe;
	}

	public void setLabelsToLoading () {
		//TODO: Clean this up it's all twisted
		artistPane.artistTable.setPlaceholder( artistPane.loadingListLabel );
		albumPane.albumTable.setPlaceholder( albumPane.loadingListLabel );
		trackPane.trackTable.setPlaceholder( trackPane.loadingListLabel );
		playlistPane.playlistTable.setPlaceholder( playlistPane.loadingLabel );
	}

	public void clearAlbumFilter () {
		//TODO: clean this up
		albumPane.filterBox.setText( "" );
	}

	public void selectPane(int i) {
		List<Node> children = buttonBox.getChildren();
		if ( i >= 0 && i < children.size() ) {
			ToggleButton button = (ToggleButton)children.get( i );
			if ( button != null ) {
				button.fire();
			}
		}
	}

	public void showAndSelectAlbumTab() {
		albumsButton.fire();
	}
}
