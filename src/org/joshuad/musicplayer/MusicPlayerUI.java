package org.joshuad.musicplayer;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.joshuad.musicplayer.players.AbstractPlayer;
import org.joshuad.musicplayer.players.FlacPlayer;
import org.joshuad.musicplayer.players.MP3Player;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
 
@SuppressWarnings({ "rawtypes", "unchecked" }) //TODO: Maybe get rid of this when I understand things better
public class MusicPlayerUI extends Application {
	
	private static final int MAX_TRACK_HISTORY = 10000;
	
	//private static final String SYMBOL_REPEAT_ONE_TRACK = "🔂";
	
	private static final DataFormat DRAGGED_TRACKS = new DataFormat( "application/x-java-track" );
	private static final DataFormat DRAGGED_TRACK_INDEX = new DataFormat ( "application/x-java-track-index" );
	private static final DataFormat DRAGGED_ALBUM = new DataFormat( "application/x-java-album-index" );
	private static final DataFormat DRAGGED_PLAYLIST = new DataFormat( "application/x-java-playlist-index" );
	
	public static final String PROGRAM_NAME = "Music Player";
		
	final static ObservableList <Path> musicSearchDirectories  = FXCollections.observableArrayList ();
	final static ObservableList <Track> currentListData = FXCollections.observableArrayList ();
	
	//These are all three representations of the same data. Add stuff to the Observable List, the other two can't accept add. 
	static ObservableList <Album> albumListData = FXCollections.observableArrayList( new ArrayList <Album> () );
	static FilteredList <Album> albumListFiltered = new FilteredList<Album> ( albumListData, p -> true );
	static SortedList <Album> albumListWrapped = new SortedList<Album>( albumListFiltered );

	static ObservableList <Track> trackListData = FXCollections.observableArrayList( new ArrayList <Track> () );
	static FilteredList <Track> trackListFiltered = new FilteredList<Track> ( trackListData, p -> true );
	static SortedList <Track> trackListWrapped = new SortedList<Track>( trackListFiltered );
	
	static TableView <Album> albumTable;
	static TableView <Track> currentListTable;
	static TableView <Playlist> playlistTable;
	static TableView <Track> trackTable;
	
	static ArrayList <Track> recentlyPlayedTracks = new ArrayList ( MAX_TRACK_HISTORY );
	
	static BorderPane albumImage;
	static BorderPane artistImage;
	static HBox albumFilterPane;
	static HBox trackFilterPane;
	static HBox playlistFilterPane;
	static HBox playlistControls;
	static Slider trackPositionSlider;
	static boolean sliderMouseHeld;
	
	static VBox transport;
	
	static AbstractPlayer currentPlayingTrack;
	
	static Label timeElapsedLabel = new Label ( "" );
	static Label timeRemainingLabel = new Label ( "" );
	static Label trackInfo = new Label ( "" );
	
	static Stage mainWindow;
	static Stage albumListSettingsWindow;
	
	static Button togglePlayButton;
	static Button toggleRepeatButton;
	static Button toggleShuffleButton;
	
	static Label currentPlayingListInfo = new Label ( "" );
	
	static SplitPane artSplitPane;
	
	static Random randomGenerator = new Random();
	
	static ShuffleMode shuffleMode = ShuffleMode.SEQUENTIAL;
	enum ShuffleMode {
		SEQUENTIAL ( "⇉" ),
		SHUFFLE ( "🔀" );
		
		String symbol;
		ShuffleMode ( String symbol ) {
			this.symbol = symbol;
		}
		
		public String getSymbol () {
			return symbol;
		}
	}
	
	
	static RepeatMode repeatMode = RepeatMode.PLAY_ONCE;
	enum RepeatMode {
		PLAY_ONCE ( "⇥" ),
		REPEAT ( "🔁" );
		
		String symbol;
		RepeatMode ( String symbol ) {
			this.symbol = symbol;
		}
		
		public String getSymbol () {
			return symbol;
		}
	}
		
    public static void main ( String[] args ) throws Exception {
    	
    	Logger.getLogger ("org.jaudiotagger").setLevel( Level.OFF );
    	
    	Path startingDirectory = Paths.get ( "/home/joshua/Desktop/music-test/" );
        Application.launch ( args );
        
    }
        
    public static void updateTransport ( int timeElapsed, int timeRemaining, double percent ) {
    	Platform.runLater( new Runnable() {
    		public void run() {
    			if ( !trackPositionSlider.isValueChanging() && !sliderMouseHeld ) {
    				trackPositionSlider.setValue( ( trackPositionSlider.getMax() - trackPositionSlider.getMin() ) * percent + trackPositionSlider.getMin() );
    			}
		    	timeElapsedLabel.setText( Utils.getLengthDisplay( timeElapsed ) );
		    	timeRemainingLabel.setText( Utils.getLengthDisplay( timeRemaining ) );
    		}
    	});
    }
    
    public static void songFinishedPlaying ( boolean userRequested ) {
    	Platform.runLater( new Runnable() {
    		public void run() {
		    	if ( !userRequested ) {
		    		playNextTrack();
		    	} 
    		}
    	});   		
    }
    	
    public static void playNextTrack() {
    	if ( currentPlayingTrack != null ) {
    		
    		switch ( shuffleMode ) {
    			case SEQUENTIAL: {
		    		ListIterator <Track> iterator = currentListData.listIterator();
		    		while ( iterator.hasNext() ) {
		    			if ( iterator.next().getIsCurrentTrack() ) {
		    				if ( iterator.hasNext() ) {
		    					playTrack( iterator.next() );
		    				} else {
		    					
		    					
		    					if ( repeatMode == RepeatMode.PLAY_ONCE ) {
		    						stopTrack ();
		    						
		    					} else if ( repeatMode == RepeatMode.REPEAT && currentListData.size () > 0 ) {
		    						playTrack ( currentListData.get ( 0 ) );
		    						
		    					} else {
		    						stopTrack();
		    					}
		    							
		    				}
		    				break;
		    			}
		    		}
    			} break;
	    		
    			case SHUFFLE: {
    				//TODO: I think there may be issues with multithreading here. 
    				int currentListSize = currentListData.size ();
    				int collisionWindowSize = currentListSize / 2; //TODO: Fine tune this number? 
    				int permittedRetries = 2; //TODO: fine tune this number? 
    				
    				boolean foundMatch = false;
    				int retryCount = 0;
    				Track playMe;
    				
    				List <Track> collisionWindow;
    				
    				if ( recentlyPlayedTracks.size() >= collisionWindowSize ) {
    					collisionWindow = recentlyPlayedTracks.subList ( 0, collisionWindowSize );
    				} else {
    					collisionWindow = recentlyPlayedTracks;
    				}

					System.out.println ( "Size: " + currentListSize + ", windowsize: " + collisionWindowSize );
    				do { 
    					playMe = currentListData.get ( randomGenerator.nextInt ( currentListData.size() ) );
    					if ( !collisionWindow.contains ( playMe ) ) {
    						foundMatch = true;
    					} else {
    						System.out.println ( "Collision: " + playMe.getTitle () ); //TODO
    						++retryCount;
    					}
    				} while ( !foundMatch && retryCount < permittedRetries );
    				
    				playTrack ( playMe );
    				
    			} break;
    			
    		}
    	}
    }
    
    public static void playTrack ( Track track ) {
    	if ( currentPlayingTrack != null ) {
    		currentPlayingTrack.stop();
			togglePlayButton.setText ( "▶" );
    	}
    	
    	switch ( track.getFormat() ) {
			case FLAC:
				currentPlayingTrack = new FlacPlayer( track, trackPositionSlider );
				togglePlayButton.setText ( "𝍪" );
				break;
			case MP3:
				currentPlayingTrack = new MP3Player( track, trackPositionSlider );
				togglePlayButton.setText ( "𝍪" );
				break;
			case UNKNOWN:
				break;
			default:
				break;
    	}

    	while ( recentlyPlayedTracks.size () >= MAX_TRACK_HISTORY ) {
    		recentlyPlayedTracks.remove ( recentlyPlayedTracks.size() - 1 );
    	}
    	
    	recentlyPlayedTracks.add ( 0, track );
    	
    	
		currentListTable.refresh();
		
		StackPane thumb = (StackPane)trackPositionSlider.lookup(".thumb");
		thumb.setVisible( true );
    	
    	trackInfo.setText( track.getArtist() + " - " + track.getYear() + " - " + track.getAlbum() + 
    		" - " + track.getTrackNumber() + " - " + track.getTitle() );
    	
    	setAlbumImage ( Utils.getAlbumCoverImagePath ( track ) );
    	setArtistImage ( Utils.getAlbumArtistImagePath ( track ) );
    }
    
    public static void playAlbum( Album album ) {
        currentListData.clear();
        currentListData.addAll( album.getTracks() );
        Track firstTrack = currentListData.get( 0 );
        if ( firstTrack != null ) {
        	playTrack( firstTrack );
        }
        
        currentPlayingListInfo.setText ( "Album: " + album.getArtist () + " - " + album.getYear () + " - " +  album.getTitle () );
    }
    
    public static void appendAlbum( Album album ) {
        currentListData.addAll( album.getTracks() );
    }
    
    
    public static void stopTrack ( ) {
    	if ( currentPlayingTrack != null ) {
    		currentPlayingTrack.stop();
			currentPlayingTrack = null;
			currentListTable.refresh();
			togglePlayButton.setText ( "▶" );
    	}
    	
		trackPositionSlider.setValue( 0 );
		timeElapsedLabel.setText( "" );
		timeRemainingLabel.setText( "" );
		trackInfo.setText( "" );
		
		StackPane thumb = (StackPane)trackPositionSlider.lookup(".thumb");
		thumb.setVisible( false );
		
    }
    
	@Override
    public void start ( Stage stage ) {
		mainWindow = stage;
    	Scene scene = new Scene ( new Group(), 1024, 768 );
    	 
    	setupAlbumTable();
    	setupAlbumFilterPane();
    	setupTrackFilterPane();
    	setupPlaylistFilterPane();
    	setupCurrentListTable();
    	setupPlaylistTable();
    	setupCurrentListControlPane();
    	setupTrackTable();
    	setupAlbumImage();
    	setupArtistImage();
    	setupTransport();
    	setupAlbumListSettingsWindow();
    	
    	artSplitPane = new SplitPane();
    	artSplitPane.getItems().addAll ( albumImage, artistImage );
    	
        BorderPane currentPlayingPane = new BorderPane();
        playlistControls.prefWidthProperty().bind ( currentPlayingPane.widthProperty() );
        currentPlayingPane.setTop ( playlistControls );
        currentPlayingPane.setCenter ( currentListTable );
            	
    	SplitPane playingArtSplitPane = new SplitPane();
        playingArtSplitPane.setOrientation ( Orientation.VERTICAL );
        playingArtSplitPane.getItems().addAll ( currentPlayingPane, artSplitPane );
        
        BorderPane albumListPane = new BorderPane();
        albumFilterPane.prefWidthProperty().bind ( albumListPane.widthProperty() );
        albumListPane.setTop( albumFilterPane );
        albumListPane.setCenter( albumTable );
        
        BorderPane trackListPane = new BorderPane();
        trackFilterPane.prefWidthProperty().bind ( trackListPane.widthProperty() );
        trackListPane.setTop( trackFilterPane );
        trackListPane.setCenter( trackTable );
        
        BorderPane playlistPane = new BorderPane();
        playlistFilterPane.prefWidthProperty().bind ( playlistPane.widthProperty() );
        playlistPane.setTop( playlistFilterPane );
        playlistPane.setCenter( playlistTable );
        
        StretchedTabPane leftTabPane = new StretchedTabPane(); //TODO: I can probably name this better. 
        
        Tab albumListTab = new Tab("Albums");
        albumListTab.setContent( albumListPane );
        albumListTab.setClosable( false );
        
        Tab playlistTab = new Tab("Playlists");
        playlistTab.setContent( playlistPane );
        playlistTab.setClosable( false );   
        
        Tab songListTab = new Tab("Songs");
        songListTab.setContent( trackListPane );
        songListTab.setClosable( false );
        
        leftTabPane.getTabs().addAll ( albumListTab, songListTab, playlistTab );
        leftTabPane.setSide ( Side.BOTTOM );
          
        SplitPane primarySplitPane = new SplitPane();
        primarySplitPane.getItems().addAll ( leftTabPane, playingArtSplitPane );
        
        final BorderPane primaryContainer = new BorderPane();

        primaryContainer.prefWidthProperty().bind( scene.widthProperty() );
        primaryContainer.prefHeightProperty().bind( scene.heightProperty() );
        primaryContainer.setPadding( new Insets(0) ); //TODO: 
        primaryContainer.setCenter( primarySplitPane );
        primaryContainer.setTop( transport );
            	
    	
        stage.setTitle ( PROGRAM_NAME );
        ((Group) scene.getRoot()).getChildren().addAll( primaryContainer );
        stage.setScene ( scene );
        stage.show();
        
        //This stuff has to be done after setScene
		StackPane thumb = (StackPane)trackPositionSlider.lookup(".thumb");
		thumb.setVisible( false );
        
        primarySplitPane.setDividerPositions (.35d );
        playingArtSplitPane.setDividerPositions ( .7d );
        artSplitPane.setDividerPosition( 0, .51d ); //For some reason .5 doesn't work...
        
		double width = togglePlayButton.getWidth();
		double height = togglePlayButton.getHeight();
		
		togglePlayButton.setMaxWidth( width );
		togglePlayButton.setMinWidth( width );
		togglePlayButton.setMaxHeight( height );
		togglePlayButton.setMinHeight( height );
		
		//TODO: This is such a crappy hack 
		final ChangeListener<Number> listener = new ChangeListener<Number>() {
			final Timer timer = new Timer(); 
			TimerTask task = null; 
			final long delayTime = 500; 

			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, final Number newValue) {
				if (task != null) { // there was already a task scheduled from
									// the previous operation ...
					task.cancel(); // cancel it, we have a new size to consider
				}

				task = new TimerTask() // create new task that calls your resize
										// operation
				{
					@Override
					public void run() {

						SplitPane.setResizableWithParent(artSplitPane, false);
					}
				};
				// schedule new task
				timer.schedule(task, delayTime);
			}
		};
		
		stage.widthProperty().addListener( listener );
		stage.heightProperty().addListener( listener );
	
    }

	public void setupTransport() {

		Button previousButton = new Button ( "⏪" );
		togglePlayButton = new Button ( "▶" );
		Button stopButton = new Button ( "◼" );
		Button nextButton = new Button ( "⏩" );
		
		int fontSize = 22;
		
		previousButton.setStyle ( "-fx-font-size: " + fontSize + "px" );
		togglePlayButton.setStyle ( "-fx-font-size: " + fontSize + "px" );
		stopButton.setStyle ( "-fx-font-size: " + fontSize + "px" );
		nextButton.setStyle ( "-fx-font-size: " + fontSize + "px" );

		Insets buttonInsets = new Insets ( 3, 12, 6, 12 );
		previousButton.setPadding ( buttonInsets );
		togglePlayButton.setPadding ( buttonInsets );
		stopButton.setPadding ( buttonInsets );
		nextButton.setPadding ( buttonInsets );

		previousButton.setOnAction ( new EventHandler <ActionEvent> () {
			@Override 
			public void handle ( ActionEvent e ) {
				if ( currentPlayingTrack != null ) {
					Track previousTrack = null;
					for ( Track track : currentListData ) {
						if ( track.getIsCurrentTrack () ) {
							if ( previousTrack != null ) {
								MusicPlayerUI.playTrack ( previousTrack );
							} else {
								MusicPlayerUI.playTrack ( track );
							}
							break;
						} else {
							previousTrack = track;
						}
					}
				}
			}
		});

		nextButton.setOnAction ( new EventHandler <ActionEvent> () {
			@Override
			public void handle ( ActionEvent e ) {
				playNextTrack ();
			}
		} );

		stopButton.setOnAction ( new EventHandler <ActionEvent> () {
			@Override
			public void handle ( ActionEvent e ) {
				if ( currentPlayingTrack != null ) {
					stopTrack ();
				}
			}
		} );
		
		togglePlayButton.setOnAction(new EventHandler<ActionEvent>() {
		    @Override public void handle(ActionEvent e) {
		    	
		    	if ( currentPlayingTrack != null && currentPlayingTrack.isPaused() ) {
		    		currentPlayingTrack.play();
		    		togglePlayButton.setText ( "𝍪" );
		    		
		    	} else if ( currentPlayingTrack != null && ! currentPlayingTrack.isPaused() ) {
		    		currentPlayingTrack.pause();
		    		togglePlayButton.setText ( "▶" );
		    		
		    	} else {
                    Track selectedTrack = currentListTable.getSelectionModel().getSelectedItem();
                    
                    if ( selectedTrack != null ) {
                    	playTrack ( selectedTrack );
                    	
                    } else if ( ! currentListTable.getItems().isEmpty() ) {
                		selectedTrack = currentListTable.getItems().get( 0 );
                        playTrack ( selectedTrack );
                    }
		    	} 
		    }
		});
		
		
		timeElapsedLabel = new Label ( "" );
		timeRemainingLabel = new Label ( "" );

		timeElapsedLabel.setMinWidth( 40 );
		timeElapsedLabel.setStyle( "" );
		timeElapsedLabel.setAlignment( Pos.CENTER_RIGHT );
		
		timeRemainingLabel.setMinWidth( 40 );
		
		trackPositionSlider = new Slider();
		trackPositionSlider.setMin( 0 );
		trackPositionSlider.setMax( 1000 );
		trackPositionSlider.setValue ( 0 ); 
		trackPositionSlider.setMaxWidth ( 600 );
		trackPositionSlider.setMinWidth( 200 );
		trackPositionSlider.setPrefWidth( 400 );
		
		trackPositionSlider.valueChangingProperty().addListener ( new ChangeListener<Boolean>() {
            public void changed ( ObservableValue<? extends Boolean> obs, Boolean wasChanging, Boolean isNowChanging ) {
            	if ( !isNowChanging ) {
            		if ( currentPlayingTrack != null ) {
            			currentPlayingTrack.seek ( trackPositionSlider.getValue() / trackPositionSlider.getMax() );
            		}
            	} 
            }
        });
		
		trackPositionSlider.setOnMousePressed ( ( MouseEvent e ) -> {
			sliderMouseHeld = true;
		});
		
		trackPositionSlider.setOnMouseReleased ( ( MouseEvent e ) -> {
			sliderMouseHeld = false;
			if ( currentPlayingTrack != null ) {
				currentPlayingTrack.seek ( trackPositionSlider.getValue() / trackPositionSlider.getMax() );
			}
		});

		HBox sliderPane = new HBox();
		sliderPane.getChildren().addAll ( timeElapsedLabel, trackPositionSlider, timeRemainingLabel );
		sliderPane.setAlignment( Pos.CENTER );
		sliderPane.setSpacing( 5 );
		
		HBox trackControls = new HBox();
		trackControls.getChildren().addAll ( previousButton, togglePlayButton, stopButton, nextButton );
		trackControls.setPadding ( new Insets ( 5 ) );
		trackControls.setSpacing ( 5 );
		
		HBox controls = new HBox ();
		controls.getChildren().addAll ( trackControls, sliderPane );
		controls.setSpacing( 10 );
		controls.setAlignment( Pos.CENTER );
		
		HBox playingTrackInfo = new HBox();
		trackInfo = new Label ( "" );
		trackInfo.setStyle( "-fx-font-weight: bold; -fx-font-size: 16" );
		playingTrackInfo.getChildren().add ( trackInfo );
		playingTrackInfo.setAlignment( Pos.CENTER );
		
		transport = new VBox();
		transport.getChildren().add ( playingTrackInfo );
		transport.getChildren().add ( controls );
		transport.setPadding( new Insets ( 10, 0, 10, 0 ) );
		transport.setSpacing ( 5 );
	}
	
	public void setupAlbumListSettingsWindow() {
		albumListSettingsWindow = new Stage();
		albumListSettingsWindow.initModality ( Modality.NONE );
		albumListSettingsWindow.initOwner ( mainWindow );
		albumListSettingsWindow.setTitle( "Music Search Locations" );
		albumListSettingsWindow.setWidth( 350 );
		Group root = new Group();
	    Scene scene = new Scene( root );
		VBox primaryPane = new VBox();
		
		TableView <Path> musicSourceList = new TableView();
		Label emptyLabel = new Label ( "No directories selected, click 'add' to add your music to the library." ) ;
		emptyLabel.setPadding( new Insets ( 20, 10, 20, 10 ) );
		musicSourceList.setColumnResizePolicy ( TableView.CONSTRAINED_RESIZE_POLICY );
		musicSourceList.setPlaceholder ( emptyLabel );
		musicSourceList.setItems ( musicSearchDirectories );
		musicSourceList.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		
		musicSourceList.widthProperty().addListener(new ChangeListener<Number>() {
		    @Override
		    public void changed ( ObservableValue<? extends Number> source, Number oldWidth, Number newWidth ) {
		        Pane header = (Pane) musicSourceList.lookup("TableHeaderRow");
		        if (header.isVisible()){
		            header.setMaxHeight(0);
		            header.setMinHeight(0);
		            header.setPrefHeight(0);
		            header.setVisible(false);
		        }
		    }
		});
		
		emptyLabel.setWrapText( true );
		emptyLabel.setTextAlignment( TextAlignment.CENTER );
		
		TableColumn <Path, String> dirListColumn = new TableColumn( "Location" );
		dirListColumn.setCellValueFactory( new Callback<TableColumn.CellDataFeatures<Path, String>, ObservableValue<String>>() {

		    @Override
		    public ObservableValue<String> call( TableColumn.CellDataFeatures<Path, String> p ) {
		        if (p.getValue() != null) {
		            return new SimpleStringProperty( p.getValue().toAbsolutePath().toString() );
		        } else {
		            return new SimpleStringProperty("<no name>");
		        }
		    }
		});
		musicSourceList.getColumns().add ( dirListColumn );

		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Music Folder");
		File defaultDirectory = new File( System.getProperty( "user.home" ) ); //TODO: put windows on desktop maybe. 
		chooser.setInitialDirectory ( defaultDirectory );
		
		Button addButton = new Button( "+ Add" );
		Button removeButton = new Button( "- Remove" );
		
		addButton.setPrefWidth ( 100 );
		removeButton.setPrefWidth ( 100 );
		addButton.setMinWidth ( 100 );
		removeButton.setMinWidth ( 100 );
		
		addButton.setOnAction ( new EventHandler<ActionEvent>() {
		    @Override public void handle(ActionEvent e) {
		    	File selectedFile = chooser.showDialog ( albumListSettingsWindow ) ;
		    	if ( selectedFile != null ) {
		    		Path selectedPath = selectedFile.toPath().toAbsolutePath();
		    		boolean addSelectedPathToList = true;
		    		for ( Path alreadyAddedPath : musicSearchDirectories ) {
		    			try {
							if ( Files.isSameFile( selectedPath, alreadyAddedPath ) ) {
								addSelectedPathToList = false;
							}
						} catch (IOException e1) {} //Do nothing, assume they don't match. 
		    		}
		    		
		    		if ( addSelectedPathToList ) {
		    			musicSourceList.getItems().add( selectedPath );
		    			
		    			Thread t = new Thread ( new Runnable() {
		    				public void run() {
		    					try {
		    				    	Logger.getLogger ("org.jaudiotagger").setLevel( Level.OFF );
		    				    	long startTime = System.currentTimeMillis();
		    						Files.walkFileTree( selectedPath, new MusicFileVisitor ( albumListData, trackListData ) );
		    						long endTime = System.currentTimeMillis();
		    						
		    						System.out.println ( "Time to load all songs: " + ( endTime - startTime ) );
		    					} catch ( IOException e ) {
		    						//TODO
		    						e.printStackTrace();
		    					}
		    				}
		    			});
		    			
		    			t.setDaemon( true );
		    			t.start();
		    			
		    		}
		    	}
		    }
		});

		removeButton.setOnAction ( new EventHandler<ActionEvent>() {
		    @Override public void handle(ActionEvent e) {
		    	musicSourceList.getItems().removeAll( musicSourceList.getSelectionModel().getSelectedItems() );
		    	musicSourceList.getSelectionModel().clearSelection();
		    }
		});
		
		musicSourceList.setOnKeyPressed ( new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent keyEvent) {
				if ( keyEvent.getCode().equals ( KeyCode.DELETE ) ) {
					musicSourceList.getItems().removeAll( musicSourceList.getSelectionModel().getSelectedItems() );
			    	musicSourceList.getSelectionModel().clearSelection();
				}
			}
		});
		
		HBox controlBox = new HBox();
		controlBox.getChildren().addAll( addButton, removeButton );
		controlBox.setAlignment( Pos.CENTER );
		controlBox.prefWidthProperty().bind( albumListSettingsWindow.widthProperty() );
		controlBox.setPadding( new Insets ( 5 ) );

		
		

		primaryPane.getChildren().addAll( musicSourceList, controlBox );
		root.getChildren().add( primaryPane );
		albumListSettingsWindow.setScene( scene );
	}
	
	public void setupAlbumImage() {
		albumImage = new BorderPane();
	}
	
	public void setupArtistImage() {
		artistImage = new BorderPane();
	}
	
	public static void setAlbumImage ( Path imagePath ) {
		try {
			ResizableImageView view = new ResizableImageView( new Image ( imagePath.toUri().toString() ) );
			view.setPreserveRatio( true );
			albumImage.setCenter( view );
		} catch ( Exception e ) {
			albumImage.setCenter( null );
		}
	}
	
	public static void setArtistImage ( Path imagePath ) {
		try {
			ResizableImageView view = new ResizableImageView( new Image ( imagePath.toUri().toString() ) );
			view.setPreserveRatio( true );
			artistImage.setCenter( view );
		} catch ( Exception e ) {
			artistImage.setCenter( null );
		}
	}
	
	public void setupCurrentListControlPane() {

		toggleRepeatButton = new Button ( repeatMode.getSymbol () );
		toggleShuffleButton = new Button ( shuffleMode.getSymbol () );
		MenuButton savePlaylistButton = new MenuButton("💾");
		
		toggleRepeatButton.setOnAction(new EventHandler<ActionEvent>() {
		    @Override public void handle(ActionEvent e) {
		    	if ( repeatMode == RepeatMode.PLAY_ONCE ) {
		    		repeatMode = RepeatMode.REPEAT;
		    	} else {
		    		repeatMode = RepeatMode.PLAY_ONCE;
		    	}
		    	
		    	toggleRepeatButton.setText ( repeatMode.getSymbol () );
		    	
		    }
		});
		
		toggleShuffleButton.setOnAction(new EventHandler<ActionEvent>() {
		    @Override public void handle(ActionEvent e) {
		    	if ( shuffleMode == ShuffleMode.SEQUENTIAL ) {
		    		shuffleMode = ShuffleMode.SHUFFLE;
		    	} else {
		    		shuffleMode = ShuffleMode.SEQUENTIAL;
		    	}
		    	
		    	toggleShuffleButton.setText ( shuffleMode.getSymbol () );
		    	
		    }
		});
		
		Button loadTracksButton = new Button ( "⏏" );
		
		
		savePlaylistButton.getItems ().addAll ( new MenuItem ( "<New Playlist>" ), new MenuItem ( "Favorites" ) );

		currentPlayingListInfo.setAlignment ( Pos.CENTER );
		
		playlistControls = new HBox();
		playlistControls.setAlignment ( Pos.CENTER_RIGHT );

		currentPlayingListInfo.prefWidthProperty().bind ( playlistControls.widthProperty() );
		
		playlistControls.getChildren().addAll ( loadTracksButton, currentPlayingListInfo, toggleRepeatButton, toggleShuffleButton, savePlaylistButton );
	}
	
	public void setupPlaylistFilterPane () {
		playlistFilterPane = new HBox();
		TextField filterBox = new TextField();
		filterBox.setPrefWidth( 500000 );
				
		Button settingsButton = new Button ( "≡" );
		settingsButton.setOnAction(new EventHandler<ActionEvent>() {
		    @Override public void handle(ActionEvent e) {
		    	if ( albumListSettingsWindow.isShowing() ) {
		    		albumListSettingsWindow.hide();
		    	} else {
		    		albumListSettingsWindow.show();
		    	}
		    }
		});
		
		playlistFilterPane.getChildren().add( filterBox );
		playlistFilterPane.getChildren().add( settingsButton );
	}
	
	public void setupTrackFilterPane () {
		trackFilterPane = new HBox();
		TextField trackFilterBox = new TextField();
		trackFilterBox.setPrefWidth( 500000 );
		trackFilterBox.textProperty().addListener((observable, oldValue, newValue) -> {
			trackListFiltered.setPredicate( track -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String[] lowerCaseFilterTokens = newValue.toLowerCase().split( "\\s+" );
                           
                ArrayList <String> matchableText = new ArrayList <String> ();
                
                matchableText.add ( Normalizer.normalize( track.getArtist(), Normalizer.Form.NFD ).replaceAll("[^\\p{ASCII}]", "").toLowerCase() );
                matchableText.add ( track.getArtist().toLowerCase() );
                matchableText.add ( Normalizer.normalize( track.getTitle(), Normalizer.Form.NFD ).replaceAll("[^\\p{ASCII}]", "").toLowerCase() );
                matchableText.add ( track.getTitle().toLowerCase() );
                
                for ( String token : lowerCaseFilterTokens ) {
            		boolean tokenMatches = false;
                	for ( String test : matchableText ) {
	                	if ( test.contains( token ) ) {
	                		tokenMatches = true;
	                	}
                	}
                	
                	if ( !tokenMatches ) return false;
                }
                
                return true; 
            });
        });
		
		Button settingsButton = new Button ( "≡" );
		settingsButton.setOnAction(new EventHandler<ActionEvent>() {
		    @Override public void handle(ActionEvent e) {
		    	if ( albumListSettingsWindow.isShowing() ) {
		    		albumListSettingsWindow.hide();
		    	} else {
		    		albumListSettingsWindow.show();
		    	}
		    }
		});
		
		trackFilterPane.getChildren().add( trackFilterBox );
		trackFilterPane.getChildren().add( settingsButton );
	}
	
	public void setupAlbumFilterPane () {
		albumFilterPane = new HBox();
		TextField albumFilterBox = new TextField();
		albumFilterBox.setPrefWidth( 500000 );
		albumFilterBox.textProperty().addListener((observable, oldValue, newValue) -> {
			albumListFiltered.setPredicate( album -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String[] lowerCaseFilterTokens = newValue.toLowerCase().split( "\\s+" );
                
                ArrayList <String> matchableText = new ArrayList <String> ();
                
                matchableText.add ( Normalizer.normalize( album.getArtist(), Normalizer.Form.NFD ).replaceAll("[^\\p{ASCII}]", "").toLowerCase() );
                matchableText.add ( album.getArtist().toLowerCase() );
                matchableText.add ( Normalizer.normalize( album.getTitle(), Normalizer.Form.NFD ).replaceAll("[^\\p{ASCII}]", "").toLowerCase() );
                matchableText.add ( album.getTitle().toLowerCase() );
                matchableText.add ( Normalizer.normalize( album.getYear(), Normalizer.Form.NFD ).replaceAll("[^\\p{ASCII}]", "").toLowerCase() );
                matchableText.add ( album.getYear().toLowerCase() );
                
                for ( String token : lowerCaseFilterTokens ) {
            		boolean tokenMatches = false;
                	for ( String test : matchableText ) {
	                	if ( test.contains( token ) ) {
	                		tokenMatches = true;
	                	}
                	}
                	
                	if ( !tokenMatches ) return false;
                }
                
                return true;
			});
        });
		
		Button settingsButton = new Button ( "≡" );
		settingsButton.setOnAction(new EventHandler<ActionEvent>() {
		    @Override public void handle(ActionEvent e) {
		    	if ( albumListSettingsWindow.isShowing() ) {
		    		albumListSettingsWindow.hide();
		    	} else {
		    		albumListSettingsWindow.show();
		    	}
		    }
		});
		
		albumFilterPane.getChildren().add( albumFilterBox );
		albumFilterPane.getChildren().add( settingsButton );
	}
	
    public void setupAlbumTable () {
        TableColumn artistColumn = new TableColumn ( "Artist" );
        TableColumn yearColumn =  new TableColumn ( "Year" );
        TableColumn albumColumn = new TableColumn ( "Album" );
        
        artistColumn.setCellValueFactory ( new PropertyValueFactory <Album, String> ( "Artist" ) );
        yearColumn.setCellValueFactory ( new PropertyValueFactory <Album, Integer> ( "Year" ) );
        albumColumn.setCellValueFactory ( new PropertyValueFactory <Album, String> ( "Title" ) );
        
        //artistColumn.setSortType (TableColumn.SortType.ASCENDING );
        
        artistColumn.setMaxWidth( 45000 );
        yearColumn.setMaxWidth( 10000 );
        albumColumn.setMaxWidth( 45000 );
        
        albumTable = new TableView();
        albumTable.getColumns().addAll ( artistColumn, yearColumn, albumColumn );
        albumTable.setEditable ( false );
        albumTable.setItems( albumListWrapped );

    	albumListWrapped.comparatorProperty().bind(albumTable.comparatorProperty());
    	
        albumTable.getSortOrder().add ( artistColumn );
        albumTable.getSortOrder().add ( yearColumn );
        albumTable.getSortOrder().add ( albumColumn );
        
        albumTable.setColumnResizePolicy ( TableView.CONSTRAINED_RESIZE_POLICY );
        albumTable.setPlaceholder( new Label ( "No albums loaded." ) );
        
        ContextMenu albumListContextMenu = new ContextMenu();
        MenuItem playMenuItem = new MenuItem("Play");
        MenuItem addMenuItem = new MenuItem("Add to Current Playlist");
        MenuItem browseMenuItem = new MenuItem("Browse Folder");
        albumListContextMenu.getItems().addAll( playMenuItem, addMenuItem, browseMenuItem );
        
        playMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                playAlbum( albumTable.getSelectionModel().getSelectedItem() );
            }
        });
        
        addMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                appendAlbum( albumTable.getSelectionModel().getSelectedItem() );
            }
        });
        
        browseMenuItem.setOnAction(new EventHandler<ActionEvent>() {
        	//TODO: This is the better way, once openjdk and openjfx supports it: getHostServices().showDocument(file.toURI().toString());
            @Override
            public void handle(ActionEvent event) {
				SwingUtilities.invokeLater(new Runnable() {
				    public void run() {
				    	try {
							Desktop.getDesktop().open( albumTable.getSelectionModel().getSelectedItem().getPath().toFile() );
						} catch (IOException e) {
							e.printStackTrace();
						}
				    }
				});
            }
        });
        
        albumTable.setContextMenu( albumListContextMenu );
        
        albumTable.setRowFactory( tv -> {
            TableRow<Album> row = new TableRow<>();
            
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && ( ! row.isEmpty() ) ) {
                    playAlbum( row.getItem() );
                }
            });
                        
            row.setOnDragDetected(event -> {
                if ( ! row.isEmpty() ) {
                    Integer index = row.getIndex();
                    Dragboard db = row.startDragAndDrop ( TransferMode.MOVE );
                    db.setDragView ( row.snapshot ( null, null ) );
                    ClipboardContent cc = new ClipboardContent();
                    cc.put ( DRAGGED_ALBUM, index );
                    db.setContent ( cc );
                    event.consume();

                }
            });
            
            return row;
        });
    }
    
    public void setupTrackTable () {
        TableColumn artistColumn = new TableColumn ( "Artist" );
        TableColumn lengthColumn =  new TableColumn ( "Length" );
        TableColumn titleColumn = new TableColumn ( "Title" );
        
        artistColumn.setCellValueFactory ( new PropertyValueFactory <Track, String> ( "Artist" ) );
        titleColumn.setCellValueFactory ( new PropertyValueFactory <Track, String> ( "Title" ) );
        lengthColumn.setCellValueFactory ( new PropertyValueFactory <Track, Integer> ( "LengthDisplay" ) );
        
        artistColumn.setSortType (TableColumn.SortType.ASCENDING );
        
        artistColumn.setMaxWidth( 45000 );
        titleColumn.setMaxWidth( 45000 );
        lengthColumn.setMaxWidth( 10000 );
        
        trackTable = new TableView();
        trackTable.getColumns().addAll ( artistColumn, titleColumn, lengthColumn );
        trackTable.setEditable ( false );
        trackTable.setItems( trackListWrapped ); 

    	trackListWrapped.comparatorProperty().bind( trackTable.comparatorProperty() );
    	
        trackTable.getSortOrder().add ( artistColumn );
        trackTable.getSortOrder().add ( titleColumn );
        trackTable.setColumnResizePolicy ( TableView.CONSTRAINED_RESIZE_POLICY );
        trackTable.setPlaceholder( new Label ( "No songs loaded." ) );
        trackTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
        
        ContextMenu contextMenu = new ContextMenu();
        MenuItem playMenuItem = new MenuItem("Play");
        MenuItem addMenuItem = new MenuItem("Add to Current Playlist");
        MenuItem browseMenuItem = new MenuItem("Browse Folder");
        contextMenu.getItems().addAll( playMenuItem, addMenuItem, browseMenuItem );
        
        playMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            	currentListTable.getItems().clear();
	        	currentListTable.getItems().addAll( trackTable.getSelectionModel().getSelectedItems() );
	        	playTrack ( trackTable.getSelectionModel().getSelectedItem() );
	        	currentPlayingListInfo.setText ( "Playlist: New" );
            }
        });
        
        addMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            	currentListTable.getItems().addAll( trackTable.getSelectionModel().getSelectedItems() );
            }
        });
        
        browseMenuItem.setOnAction(new EventHandler<ActionEvent>() {
        	//TODO: This is the better way, once openjdk and openjfx supports it: getHostServices().showDocument(file.toURI().toString());
            @Override
            public void handle(ActionEvent event) {
				SwingUtilities.invokeLater(new Runnable() {
				    public void run() {
				    	try {
							Desktop.getDesktop().open( trackTable.getSelectionModel().getSelectedItem().getPath().getParent().toFile() );
						} catch (IOException e) {
							e.printStackTrace();
						}
				    }
				});
            }
        });
        
        trackTable.setContextMenu( contextMenu );
        
        trackTable.setRowFactory( tv -> {
            TableRow<Track> row = new TableRow<>();
            
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && ( ! row.isEmpty() ) ) {
                	currentListTable.getItems().clear();
                	currentListTable.getItems().add( trackTable.getSelectionModel().getSelectedItem() );
                	playTrack ( trackTable.getSelectionModel().getSelectedItem() );
    	        	currentPlayingListInfo.setText ( "Playlist: New Playlist" );
                }
            });
                        
            row.setOnDragDetected(event -> {
                if ( ! row.isEmpty() ) {
                	ArrayList <Track> tracks = new ArrayList <Track> ( trackTable.getSelectionModel().getSelectedItems() );
                    Dragboard db = row.startDragAndDrop ( TransferMode.MOVE );
                    db.setDragView ( row.snapshot ( null, null ) );
                    ClipboardContent cc = new ClipboardContent();
                    cc.put ( DRAGGED_TRACKS, tracks );
                    db.setContent ( cc );
                    event.consume();

                }
            });
            
            return row;
        });
    }
    
    public void setupPlaylistTable() {
    	TableColumn nameColumn = new TableColumn ( "Playlist" );
        TableColumn lengthColumn =  new TableColumn ( "Length" );
        TableColumn songsColumn = new TableColumn ( "Songs" );
        
        nameColumn.setCellValueFactory ( new PropertyValueFactory <Album, String> ( "Name" ) );
        lengthColumn.setCellValueFactory ( new PropertyValueFactory <Album, Integer> ( "Length" ) );
        songsColumn.setCellValueFactory ( new PropertyValueFactory <Album, String> ( "SongCount" ) );
        
        nameColumn.setSortType (TableColumn.SortType.ASCENDING );
        
        nameColumn.setMaxWidth( 70000 );
        lengthColumn.setMaxWidth( 15000 );
        songsColumn.setMaxWidth( 15000 );
        
        playlistTable = new TableView();
        playlistTable.getColumns().addAll ( nameColumn, lengthColumn, songsColumn );
        playlistTable.setEditable ( false );
        //playlistTable.setItems( albumListWrapped );

    	//albumListWrapped.comparatorProperty().bind(albumTable.comparatorProperty());
    	
    	playlistTable.getSortOrder().add ( nameColumn );
    	playlistTable.setColumnResizePolicy ( TableView.CONSTRAINED_RESIZE_POLICY );
    	Label emptyLabel = new Label ( "You haven't created any playlists, make a playlist on the right and save it." );
		emptyLabel.setWrapText( true );
		emptyLabel.setTextAlignment ( TextAlignment.CENTER );
		emptyLabel.setPadding( new Insets ( 20, 10, 20, 10 ) );
    	playlistTable.setPlaceholder( emptyLabel );
        
        ContextMenu albumListContextMenu = new ContextMenu();
        MenuItem playMenuItem = new MenuItem("Play");
        MenuItem enqueueMenuItem = new MenuItem("Enqueue");
        MenuItem deleteMenuItem = new MenuItem("Delete");
        albumListContextMenu.getItems().addAll( playMenuItem, enqueueMenuItem, deleteMenuItem );
        
        playMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            	//TODO                
            }
        });
        
        enqueueMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            	//TODO: 
            }
        });
        
        deleteMenuItem.setOnAction(new EventHandler<ActionEvent>() {
        	//TODO: This is the better way, once openjdk and openjfx supports it: getHostServices().showDocument(file.toURI().toString());
            @Override
            public void handle(ActionEvent event) {
				//TODO: 
            }
        });
        
        playlistTable.setContextMenu( albumListContextMenu );
        
        playlistTable.setRowFactory( tv -> {
            TableRow<Playlist> row = new TableRow<>();
            
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && ( ! row.isEmpty() ) ) {
                    //TODO: 
                }
            });
                        
            row.setOnDragDetected(event -> {
                if ( ! row.isEmpty() ) {
                    Integer index = row.getIndex();
                    Dragboard db = row.startDragAndDrop ( TransferMode.MOVE );
                    db.setDragView ( row.snapshot ( null, null ) );
                    ClipboardContent cc = new ClipboardContent();
                    cc.put ( DRAGGED_PLAYLIST, index );
                    db.setContent ( cc );
                    event.consume();
                }
            });
            
            return row;
        });
    	
    }
    
    public void setupCurrentListTable () {
    	TableColumn playingColumn = new TableColumn ( "" );
        TableColumn artistColumn = new TableColumn ( "Artist" );
        TableColumn yearColumn =  new TableColumn ( "Year" );
        TableColumn albumColumn = new TableColumn ( "Album" );
        TableColumn titleColumn = new TableColumn ( "Title" );
        TableColumn trackColumn = new TableColumn ( "#" );
        TableColumn lengthColumn = new TableColumn ( "Length" );
        
        playingColumn.setCellFactory ( column -> {
            return new TableCell <Track, Boolean> () {
                @Override protected void updateItem ( Boolean trackPlaying, boolean empty ) {
                    super.updateItem (trackPlaying, empty);
                    if ( empty || trackPlaying == null || trackPlaying == false ) {
                        setText(null);
                    } else {
                        setText( "▶" );
                    }
                }
            };
        });
        
        playingColumn.setCellValueFactory ( new PropertyValueFactory <Track, Boolean> ( "IsCurrentTrack" ) );
        artistColumn.setCellValueFactory ( new PropertyValueFactory <Track, String> ( "Artist" ) );
        yearColumn.setCellValueFactory ( new PropertyValueFactory <Track, Integer> ( "Year" ) );
        albumColumn.setCellValueFactory ( new PropertyValueFactory <Track, String> ( "Album" ) );
        titleColumn.setCellValueFactory ( new PropertyValueFactory <Track, String> ( "Title" ) );
        trackColumn.setCellValueFactory ( new PropertyValueFactory <Track, Integer> ( "TrackNumber" ) );
        lengthColumn.setCellValueFactory ( new PropertyValueFactory <Track, String> ( "LengthDisplay" ) );
        
        artistColumn.setMaxWidth( 22000 );
        trackColumn.setMaxWidth( 4000 );
        yearColumn.setMaxWidth( 8000 );
        albumColumn.setMaxWidth( 25000 );
        titleColumn.setMaxWidth( 25000 );
        lengthColumn.setMaxWidth( 8000 );
                
        currentListTable = new TableView();
        currentListTable.getColumns().addAll ( playingColumn, trackColumn, artistColumn, yearColumn, albumColumn, titleColumn, lengthColumn );
        albumTable.getSortOrder().add ( trackColumn );
        currentListTable.setEditable ( false );
        currentListTable.setItems ( currentListData );
        currentListTable.setColumnResizePolicy ( TableView.CONSTRAINED_RESIZE_POLICY );
        currentListTable.setPlaceholder ( new Label ( "No songs in playlist." ) );
        currentListTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
        
        playingColumn.setMaxWidth( 20 );
        playingColumn.setMinWidth( 20 );
        
        ContextMenu contextMenu = new ContextMenu();
        MenuItem playMenuItem = new MenuItem("Play");
        MenuItem deleteMenuItem = new MenuItem("Delete");
        MenuItem browseMenuItem = new MenuItem("Browse Folder");
        contextMenu.getItems().addAll( playMenuItem, deleteMenuItem, browseMenuItem );
        
        playMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
	        	playTrack ( currentListTable.getSelectionModel().getSelectedItem() );
            }
        });
        
        browseMenuItem.setOnAction(new EventHandler<ActionEvent>() {
        	//TODO: This is the better way, once openjdk and openjfx supports it: getHostServices().showDocument(file.toURI().toString());
            @Override
            public void handle(ActionEvent event) {
				SwingUtilities.invokeLater(new Runnable() {
				    public void run() {
				    	try {
							Desktop.getDesktop().open( currentListTable.getSelectionModel().getSelectedItem().getPath().getParent().toFile() );
						} catch (IOException e) {
							e.printStackTrace();
						}
				    }
				});
            }
        });
        
        //TODO: right click delete and key delete are same code....
        deleteMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            	
            	ObservableList <Integer> selectedIndexes = currentListTable.getSelectionModel().getSelectedIndices();
				ObservableList <Track> selectedItems = currentListTable.getSelectionModel().getSelectedItems();
				
				if ( ! selectedItems.isEmpty() ) {
					int selectAfterDelete = selectedIndexes.get(0) - 1;
					currentListData.removeAll( selectedItems );
					currentListTable.getSelectionModel().clearAndSelect( selectAfterDelete );
				}
            }
        });
        
        currentListTable.setContextMenu( contextMenu );
        
		currentListTable.setOnKeyPressed ( new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent keyEvent) {
				if ( keyEvent.getCode().equals ( KeyCode.DELETE ) ) {
					ObservableList <Integer> selectedIndexes = currentListTable.getSelectionModel().getSelectedIndices();
					ObservableList <Track> selectedItems = currentListTable.getSelectionModel().getSelectedItems();
					
					if ( ! selectedItems.isEmpty() ) {
						int selectAfterDelete = selectedIndexes.get(0) - 1;
						currentListData.removeAll( selectedItems );
						currentListTable.getSelectionModel().clearAndSelect( selectAfterDelete );
					}
				}
			}
		});
      
		
		currentListTable.setOnDragOver( event -> {
	         Dragboard db = event.getDragboard();
             
	         if ( db.hasContent ( DRAGGED_TRACKS )
             ||   db.hasContent ( DRAGGED_ALBUM ) 
             ||   db.hasFiles() ) {
	        	 
                 event.acceptTransferModes ( TransferMode.MOVE );
                 event.consume();
                 
             }
		});
		
		currentListTable.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            
            if ( db.hasContent ( DRAGGED_TRACKS ) ) {
                ArrayList <Track> draggedTracks = (ArrayList <Track>)db.getContent ( DRAGGED_TRACKS );
                currentListTable.getItems().addAll ( draggedTracks );

                event.setDropCompleted(true);
                currentListTable.getSelectionModel().clearSelection();
                event.consume();
            
            } else if ( db.hasContent( DRAGGED_ALBUM ) ) {
                        	
            	if ( currentListTable.getItems().isEmpty() ) { //If the list is empty, we handle the drop, otherwise let the rows handle it
	            	int draggedIndex = (Integer) db.getContent( DRAGGED_ALBUM );
	                Album draggedAlbum = albumTable.getItems().get( draggedIndex );
	                
	                int dropIndex = 0;
	                currentListTable.getItems().addAll ( dropIndex, draggedAlbum.getTracks() );
	
	                event.setDropCompleted(true);
	                event.consume();
            	}
            } else if ( db.hasFiles() ) {
            	ArrayList <Track> tracksToAdd = new ArrayList ();
                for ( File file:db.getFiles() ) {
                	Path droppedPath = Paths.get( file.getAbsolutePath() );
                	if ( Utils.isMusicFile( droppedPath ) ) {
                		try {
							currentListTable.getItems().add( new Track ( droppedPath ) );
						} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
							e.printStackTrace();
						}
                	} else if ( Files.isDirectory( droppedPath ) ) {
                		currentListTable.getItems().addAll( Utils.getAllTracksInDirectory( droppedPath ) );
                	}
                }
                
                event.setDropCompleted(true);
                event.consume();
            }
            
        });
			
		
        currentListTable.setRowFactory( tv -> {
            TableRow<Track> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if ( event.getClickCount() == 2 && (! row.isEmpty() ) ) {
                	playTrack ( row.getItem() );
                }
            });
            
            row.setOnDragDetected(event -> {
                if (! row.isEmpty()) {
                    Integer index = row.getIndex();
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(row.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(DRAGGED_TRACK_INDEX, index);
                    db.setContent(cc);
                    event.consume();
                }
            });

            row.setOnDragOver ( event -> {
            	
                Dragboard db = event.getDragboard();
                if ( db.hasContent( DRAGGED_TRACK_INDEX ) ) {
                    event.acceptTransferModes ( TransferMode.MOVE );
                    event.consume();
                    
                } else if ( db.hasContent( DRAGGED_TRACKS ) ) {
                    event.acceptTransferModes ( TransferMode.MOVE );
                    event.consume();
                    
                } else if ( db.hasContent ( DRAGGED_ALBUM ) ) {
                        event.acceptTransferModes ( TransferMode.MOVE );
                        event.consume();

                } else if ( db.hasFiles() ) {
                	event.acceptTransferModes ( TransferMode.MOVE );
                }
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if ( db.hasContent ( DRAGGED_TRACK_INDEX ) ) {
	                int draggedIndex = (Integer) db.getContent ( DRAGGED_TRACK_INDEX );
	                Track draggedPerson = currentListTable.getItems().remove(draggedIndex);
	                int dropIndex = row.isEmpty() ? dropIndex = currentListTable.getItems().size() :row.getIndex();
	                currentListTable.getItems().add(dropIndex, draggedPerson);
	
	                event.setDropCompleted(true);
	                currentListTable.getSelectionModel().clearAndSelect(dropIndex);
	                event.consume();
            	} else if ( db.hasContent ( DRAGGED_TRACKS ) ) {
            		
            		ArrayList <Track> draggedTracks = (ArrayList <Track>)db.getContent ( DRAGGED_TRACKS );
                    int dropIndex = row.isEmpty() ? dropIndex = currentListTable.getItems().size() :row.getIndex();
                    currentListTable.getItems().addAll ( dropIndex, draggedTracks );

                    event.setDropCompleted(true);
                    event.consume();
                    
                } else if ( db.hasContent( DRAGGED_ALBUM ) ) {
                	int draggedIndex = (Integer) db.getContent( DRAGGED_ALBUM );
	                Album draggedAlbum = albumTable.getItems().get( draggedIndex );
	                int dropIndex = row.getIndex();
	                ArrayList <Track> droppedTracks = draggedAlbum.getTracks();
	                if ( droppedTracks != null ) currentListTable.getItems().addAll ( Math.min( dropIndex, currentListTable.getItems().size() ), droppedTracks );
	                event.setDropCompleted(true);
	                event.consume();
                } else if ( db.hasFiles() ) {
                	ArrayList <Track> tracksToAdd = new ArrayList ();
                    for ( File file:db.getFiles() ) {
                    	Path droppedPath = Paths.get( file.getAbsolutePath() );
                    	if ( Utils.isMusicFile( droppedPath ) ) {
                    		try {
								tracksToAdd.add( new Track ( droppedPath ) );
							} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
								e.printStackTrace();
							}
                    	} else if ( Files.isDirectory( droppedPath ) ) {
                    		tracksToAdd.addAll( Utils.getAllTracksInDirectory( droppedPath ) );
                    	}
                    }
                    if ( !tracksToAdd.isEmpty() ) {
                        int dropIndex = row.getIndex();
                    	currentListTable.getItems().addAll ( Math.min( dropIndex, currentListTable.getItems().size() ), tracksToAdd );
                    }
                    
                    event.setDropCompleted(true);
                    event.consume();
                }
            });
            
            return row;
        });
    }
}


