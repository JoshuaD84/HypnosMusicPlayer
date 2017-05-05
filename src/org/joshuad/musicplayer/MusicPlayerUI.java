package org.joshuad.musicplayer;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.ListIterator;
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
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
	
	
	private static final DataFormat DRAGGED_TRACK = new DataFormat("application/x-java-track-index");
	private static final DataFormat DRAGGED_ALBUM = new DataFormat("application/x-java-album-index");
	
	public static final String PROGRAM_NAME = "Music Player";
		
	final static ObservableList <Path> musicSearchDirectories  = FXCollections.observableArrayList ();
	final static ObservableList <Track> playlistData = FXCollections.observableArrayList ();
	
	//These are all three representations of the same data. Add stuff to the Observable List, the other two can't accept add. 
	static ObservableList <Album> albumListData = FXCollections.observableArrayList( new ArrayList <Album> () );
	static SortedList <Album> albumListWrapped = new SortedList<Album>( new FilteredList<Album> ( albumListData, p -> true ) );
	static FilteredList <Album> albumListFiltered;
	
	static TableView <Album> albumTable;
	static TableView <Track> playlistTable;
	
	static BorderPane albumImage;
	static BorderPane artistImage;
	static HBox albumFilterPane;
	static Slider trackPositionSlider;
	static boolean sliderMouseHeld;
	
	static VBox transport;
	
	static AbstractPlayer currentPlayingTrack;
	
	static Label timeElapsedLabel = new Label ( "2:02" );
	static Label timeRemainingLabel = new Label ( "-1:12" );
	static Label trackInfo = new Label ( "" );
	
	static Stage mainWindow;
	static Stage albumListSettingsWindow;
		
    public static void main ( String[] args ) throws Exception {
    	
    	Logger.getLogger ("org.jaudiotagger").setLevel( Level.OFF );
    	
    	Path startingDirectory = Paths.get ( "/home/joshua/Desktop/music-test/" );
    	/*
    	musicSearchDirectories.add ( startingDirectory );
    	long startTime = System.currentTimeMillis();
    	
    	for ( Path searchDirectory : musicSearchDirectories ) {
	    	Files.walkFileTree( searchDirectory, new MusicFileVisitor ( albumListData ) );
    	}
    	
    	long endTime = System.currentTimeMillis();

    	System.out.println ( "Time to index all music: " + ( endTime - startTime ) );
    	*/
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
    		ListIterator <Track> iterator = playlistData.listIterator();
    		while ( iterator.hasNext() ) {
    			if ( iterator.next().getIsCurrentTrack() ) {
    				if ( iterator.hasNext() ) {
    					MusicPlayerUI.startTrack( iterator.next() );
    				} else {
    					MusicPlayerUI.stopTrack ();
    				}
    				break;
    			}
    		}
    	}
    }
    
    public static void startTrack ( Track track ) {
    	if ( currentPlayingTrack != null ) currentPlayingTrack.stop();
    	
    	switch ( track.getFormat() ) {
			case FLAC:
				currentPlayingTrack = new FlacPlayer( track, trackPositionSlider );
				break;
			case MP3:
				currentPlayingTrack = new MP3Player( track, trackPositionSlider );
				break;
			case UNKNOWN:
				break;
			default:
				break;
    	}
    	
		playlistTable.refresh();
		
		StackPane thumb = (StackPane)trackPositionSlider.lookup(".thumb");
		thumb.setVisible( true );
    	
    	trackInfo.setText( track.getArtist() + " - " + track.getYear() + " - " + track.getAlbum() + 
    		" - " + track.getTrackNumber() + " - " + track.getTitle() );
    	
    	setAlbumImage ( Utils.getAlbumCoverImagePath ( track ) );
    	setArtistImage ( Utils.getAlbumArtistImagePath ( track ) );
    }
    
    public static void playAlbum( Album album ) {
        playlistData.clear();
        playlistData.addAll( album.getTracks() );
        Track firstTrack = playlistData.get( 0 );
        if ( firstTrack != null ) {
        	MusicPlayerUI.startTrack( firstTrack );
        }
    }
    
    public static void appendAlbum( Album album ) {
        playlistData.addAll( album.getTracks() );
    }
    
    
    public static void stopTrack ( ) {
    	if ( currentPlayingTrack != null ) {
    		currentPlayingTrack.stop();
			currentPlayingTrack = null;
			playlistTable.refresh();
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
    	setupPlaylistTable();
    	setupAlbumImage();
    	setupArtistImage();
    	setupTransport();
    	setupAlbumListSettingsWindow();
    	
    	SplitPane artSplitPane = new SplitPane();
    	artSplitPane.getItems().addAll ( albumImage, artistImage );
            	
    	SplitPane playlistSplitPane = new SplitPane();
        playlistSplitPane.setOrientation ( Orientation.VERTICAL );
        playlistSplitPane.getItems().addAll ( playlistTable, artSplitPane );
        
        BorderPane albumListPane = new BorderPane();
        albumFilterPane.prefWidthProperty().bind ( albumListPane.widthProperty() );
        albumListPane.setTop( albumFilterPane );
        albumListPane.setCenter( albumTable );
        
        SplitPane primarySplitPane = new SplitPane();
        primarySplitPane.getItems().addAll ( albumListPane, playlistSplitPane );
        
        
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
        
		stopTrack();
        artSplitPane.setDividerPosition( 0, .5d );
        playlistSplitPane.setDividerPosition( 0, .8d );
        primarySplitPane.setDividerPositions( .35d );
    }

	public void setupTransport() {

		Button previousButton = new Button ( "⏪" );
		Button playButton = new Button ( "▶" );
		Button pauseButton = new Button ( "𝍪" );
		Button stopButton = new Button ( "◼" );
		Button nextButton = new Button ( "⏩" );
		

		previousButton.setOnAction ( new EventHandler<ActionEvent>() {
		    @Override public void handle ( ActionEvent e ) {
		    	if ( currentPlayingTrack != null ) {
		    		Track previousTrack = null;
		    		for ( Track track : playlistData ) {
		    			if ( track.getIsCurrentTrack() ) {
		    				if ( previousTrack != null ) {
		    					MusicPlayerUI.startTrack( previousTrack );
		    				} else {
		    					MusicPlayerUI.startTrack ( track );
		    				}
		    				break;
		    			} else {
		    				previousTrack = track;
		    			}
		    		}
		    	}
		    }
		});
		
		nextButton.setOnAction ( new EventHandler<ActionEvent>() {
		    @Override public void handle ( ActionEvent e ) {
		    	playNextTrack();
		    }
		});
		
		
		stopButton.setOnAction(new EventHandler<ActionEvent>() {
		    @Override public void handle(ActionEvent e) {
		    	if ( currentPlayingTrack != null ) {
		    		stopTrack();
		    	}
		    }
		});
		
		pauseButton.setOnAction(new EventHandler<ActionEvent>() {
		    @Override public void handle(ActionEvent e) {
		    	if ( currentPlayingTrack != null ) {
		    		currentPlayingTrack.pause();
		    	}
		    }
		});
		
		playButton.setOnAction(new EventHandler<ActionEvent>() {
		    @Override public void handle(ActionEvent e) {
		    	
		    	if ( currentPlayingTrack != null && currentPlayingTrack.isPaused() ) {
		    		currentPlayingTrack.play();
		    		
		    	} else {
                    Track selectedTrack = playlistTable.getSelectionModel().getSelectedItem();
                    
                    if ( selectedTrack != null ) {
                    	startTrack ( selectedTrack );
                    	
                    } else if ( ! playlistTable.getItems().isEmpty() ) {
                		selectedTrack = playlistTable.getItems().get( 0 );
                        startTrack ( selectedTrack );
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
		sliderPane.getChildren().add ( timeElapsedLabel );
		sliderPane.getChildren().add ( trackPositionSlider );
		sliderPane.getChildren().add ( timeRemainingLabel );
		sliderPane.setAlignment( Pos.CENTER );
		sliderPane.setSpacing( 5 );
		
		HBox buttons = new HBox();
		buttons.getChildren().add ( previousButton );
		buttons.getChildren().add ( stopButton );
		buttons.getChildren().add ( playButton );
		buttons.getChildren().add ( pauseButton );
		buttons.getChildren().add ( nextButton );
		buttons.setPadding ( new Insets ( 5 ) );
		buttons.setSpacing ( 5 );
		
		HBox controls = new HBox ();
		controls.getChildren().add ( buttons );
		controls.getChildren().add ( sliderPane );
		controls.setSpacing( 10 );
		controls.setAlignment( Pos.CENTER );
		
		HBox playingTrackInfo = new HBox();
		trackInfo = new Label ( "" );
		trackInfo.setStyle( "-fx-font-weight: bold" );
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
		    						Files.walkFileTree( selectedPath, new MusicFileVisitor ( albumListData ) );
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
			ImageView view = new ImageView( new Image ( imagePath.toUri().toString() ) );
			view.setPreserveRatio( true );
			view.fitWidthProperty().set( 250 ); 
			view.fitHeightProperty().set( 250 ); 
			albumImage.setCenter( view );
		} catch ( Exception e ) {
			albumImage.setCenter( null );
		}
	}
	
	public static void setArtistImage ( Path imagePath ) {
		try {
			ImageView view = new ImageView( new Image ( imagePath.toUri().toString() ) );
			view.setPreserveRatio( true );
			view.setFitHeight( 250 );
			view.setFitWidth( 250 );
			artistImage.setCenter( view );
		} catch ( Exception e ) {
			artistImage.setCenter( null );
		}
	}
	
	public void setupAlbumFilterPane () {
		albumFilterPane = new HBox();
		TextField albumFilterBox = new TextField();
		albumFilterBox.setPrefWidth( 500000 );
		albumFilterBox.textProperty().addListener((observable, oldValue, newValue) -> {
			albumListFiltered.setPredicate( album -> {
                // If filter text is empty, display all persons.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                // Compare first name and last name of every person with filter text.
                String lowerCaseFilter = newValue.toLowerCase();
                
                ArrayList <String> matchableText = new ArrayList <String> ();
                
                matchableText.add ( Normalizer.normalize( album.getArtist(), Normalizer.Form.NFD ).replaceAll("[^\\p{ASCII}]", "").toLowerCase() );
                matchableText.add ( album.getArtist().toLowerCase() );
                matchableText.add ( Normalizer.normalize( album.getTitle(), Normalizer.Form.NFD ).replaceAll("[^\\p{ASCII}]", "").toLowerCase() );
                matchableText.add ( album.getTitle().toLowerCase() );
                matchableText.add ( Normalizer.normalize( album.getYear(), Normalizer.Form.NFD ).replaceAll("[^\\p{ASCII}]", "").toLowerCase() );
                matchableText.add ( album.getYear().toLowerCase() );
                
                //TODO: Matching songs would be nice, like foobar
                
                for ( String test : matchableText ) {
                	if ( test.contains( lowerCaseFilter ) ) {
                		return true;
                	}
                }
                
                return false; // Does not match.
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
        
        artistColumn.setSortType (TableColumn.SortType.ASCENDING );
        
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
        albumTable.setColumnResizePolicy ( TableView.CONSTRAINED_RESIZE_POLICY );
        albumTable.setPlaceholder( new Label ( "No albums loaded." ) );
        
        ContextMenu albumListContextMenu = new ContextMenu();
        MenuItem playMenuItem = new MenuItem("Play");
        MenuItem addMenuItem = new MenuItem("Add to Playlist");
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
    
    public void setupPlaylistTable () {
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
                
        playlistTable = new TableView();
        playlistTable.getColumns().addAll ( playingColumn, trackColumn, artistColumn, yearColumn, albumColumn, titleColumn, lengthColumn );
        albumTable.getSortOrder().add ( trackColumn );
        playlistTable.setEditable ( false );
        playlistTable.setItems ( playlistData );
        playlistTable.setColumnResizePolicy ( TableView.CONSTRAINED_RESIZE_POLICY );
        playlistTable.setPlaceholder ( new Label ( "No songs in playlist." ) );
        playlistTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
        
        playingColumn.setMaxWidth( 20 );
        playingColumn.setMinWidth( 20 );
        
		playlistTable.setOnKeyPressed ( new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent keyEvent) {
				if ( keyEvent.getCode().equals ( KeyCode.DELETE ) ) {
					ObservableList <Integer> selectedIndexes = playlistTable.getSelectionModel().getSelectedIndices();
					ObservableList <Track> selectedItems = playlistTable.getSelectionModel().getSelectedItems();
					
					if ( ! selectedItems.isEmpty() ) {
						int selectAfterDelete = selectedIndexes.get(0) - 1;
						playlistData.removeAll( selectedItems );
						playlistTable.getSelectionModel().clearAndSelect( selectAfterDelete );
					}
				}
			}
		});
      
		
		playlistTable.setOnDragOver( event -> {
	         Dragboard db = event.getDragboard();
             
             if ( db.hasContent ( DRAGGED_ALBUM ) ) {
                 event.acceptTransferModes ( TransferMode.MOVE );
                 event.consume();
             } else if ( db.hasFiles() ) {
             	event.acceptTransferModes ( TransferMode.MOVE );
             }
		});
		
		playlistTable.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if ( db.hasContent( DRAGGED_ALBUM ) ) {
            	
            	if ( playlistTable.getItems().isEmpty() ) { //If the list is empty, we handle the drop, otherwise let the rows handle it
	            	int draggedIndex = (Integer) db.getContent( DRAGGED_ALBUM );
	                Album draggedAlbum = albumTable.getItems().get( draggedIndex );
	                
	                int dropIndex = 0;
	                playlistTable.getItems().addAll ( dropIndex, draggedAlbum.getTracks() );
	
	                event.setDropCompleted(true);
	                event.consume();
            	}
            } else if ( db.hasFiles() ) {
            	ArrayList <Track> tracksToAdd = new ArrayList ();
                for ( File file:db.getFiles() ) {
                	Path droppedPath = Paths.get( file.getAbsolutePath() );
                	if ( Utils.isMusicFile( droppedPath ) ) {
                		try {
							playlistTable.getItems().add( new Track ( droppedPath ) );
						} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
							e.printStackTrace();
						}
                	} else if ( Files.isDirectory( droppedPath ) ) {
                		playlistTable.getItems().addAll( Utils.getAllTracksInDirectory( droppedPath ) );
                	}
                }
                
                event.setDropCompleted(true);
                event.consume();
            }
            
        });
			
		
        playlistTable.setRowFactory( tv -> {
            TableRow<Track> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if ( event.getClickCount() == 2 && (! row.isEmpty() ) ) {
                	startTrack ( row.getItem() );
                }
            });
            
            row.setOnDragDetected(event -> {
                if (! row.isEmpty()) {
                    Integer index = row.getIndex();
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(row.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(DRAGGED_TRACK, index);
                    db.setContent(cc);
                    event.consume();
                }
            });

            row.setOnDragOver ( event -> {
            	
                Dragboard db = event.getDragboard();
                
                if ( db.hasContent(DRAGGED_TRACK ) ) {
                	if ( row.getIndex() != ((Integer)db.getContent(DRAGGED_TRACK)).intValue() ) {
                        event.acceptTransferModes ( TransferMode.MOVE );
                        event.consume();
                    }
                } else if ( db.hasContent ( DRAGGED_ALBUM ) ) {
                	if ( row.getIndex() != ((Integer)db.getContent ( DRAGGED_ALBUM) ).intValue() ) {
                        event.acceptTransferModes ( TransferMode.MOVE );
                        event.consume();
                    }
                } else if ( db.hasFiles() ) {
                	event.acceptTransferModes ( TransferMode.MOVE );
                }
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if ( db.hasContent ( DRAGGED_TRACK ) ) {
                    int draggedIndex = (Integer) db.getContent(DRAGGED_TRACK);
                    Track draggedPerson = playlistTable.getItems().remove(draggedIndex);
                    int dropIndex = row.isEmpty() ? dropIndex = playlistTable.getItems().size() :row.getIndex();
                    playlistTable.getItems().add(dropIndex, draggedPerson);

                    event.setDropCompleted(true);
                    playlistTable.getSelectionModel().clearAndSelect(dropIndex);
                    event.consume();
                } else if ( db.hasContent( DRAGGED_ALBUM ) ) {
                	int draggedIndex = (Integer) db.getContent( DRAGGED_ALBUM );
	                Album draggedAlbum = albumTable.getItems().get( draggedIndex );
	                int dropIndex = row.getIndex();
	                ArrayList <Track> droppedTracks = draggedAlbum.getTracks();
	                if ( droppedTracks != null ) playlistTable.getItems().addAll ( Math.min( dropIndex, playlistTable.getItems().size() ), droppedTracks );
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
                    	playlistTable.getItems().addAll ( Math.min( dropIndex, playlistTable.getItems().size() ), tracksToAdd );
                    }
                    
                    event.setDropCompleted(true);
                    event.consume();
                }
            });
            
            return row;
        });
    }
}


