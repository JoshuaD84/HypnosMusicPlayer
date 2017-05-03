package org.joshuad.musicplayer;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joshuad.musicplayer.players.AbstractPlayer;
import org.joshuad.musicplayer.players.FlacPlayer;
import org.joshuad.musicplayer.players.MP3Player;

import javafx.application.Application;
import javafx.application.Platform;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
 

@SuppressWarnings({ "rawtypes", "unchecked" }) //TODO: Maybe get rid of this when I understand things better
public class MusicPlayerUI extends Application {
	
	private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
	
	public static final String PROGRAM_NAME = "Music Player";
		
	final static ObservableList <Track> playlistData = FXCollections.observableArrayList ();
	static SortedList <Album> albumListData;
	static FilteredList <Album> albumListDataFiltered;
	
	static TableView <Album> albumTable;
	static TableView <Track> playlistTable;
	
	static BorderPane albumImage;
	static BorderPane artistImage;
	static TextField albumFilterPane;
	static Slider trackPositionSlider;
	static boolean sliderMouseHeld;
	
	static VBox transport;
	
	static AbstractPlayer currentPlayingTrack;
	
	static Label timeElapsedLabel = new Label ( "2:02" );
	static Label timeRemainingLabel = new Label ( "-1:12" );
	static Label trackInfo = new Label ( "" );
		
		
    public static void main ( String[] args ) throws Exception {
    	
    	Logger.getLogger ("org.jaudiotagger").setLevel( Level.OFF );
    	
    	//Path startingDirectory = Paths.get ( "/home/joshua/Desktop/music-test/" );
    	Path startingDirectory = Paths.get ( "/d/music/" );
    	
    	long startTime = System.currentTimeMillis();
    	Files.walkFileTree( startingDirectory, new MusicFileVisitor () );
    	albumListDataFiltered = new FilteredList<> ( FXCollections.observableArrayList( new ArrayList <Album> ( MusicFileVisitor.albums ) ), p -> true );
    	albumListData = new SortedList<>( albumListDataFiltered );
    	
    	long endTime = System.currentTimeMillis();
    	
    	System.out.println ( "To read all music: " + ( endTime - startTime ) );
     
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
    	currentPlayingTrack.stop();
		currentPlayingTrack = null;
		playlistTable.refresh();
		trackPositionSlider.setValue( 0 );
		timeElapsedLabel.setText( "" );
		timeRemainingLabel.setText( "" );
		trackInfo.setText( "" );
    }
    
	@Override
    public void start ( Stage stage ) {
    	Scene scene = new Scene ( new Group(), 1024, 768 );
    	 
    	setupAlbumTable();
    	setupAlbumFilterPane();
    	setupPlaylistTable();
    	setupAlbumImage();
    	setupArtistImage();
    	setupTransport();
    	
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

        artSplitPane.setDividerPosition( 0, .5d );
        playlistSplitPane.setDividerPosition( 0, .8d );
        primarySplitPane.setDividerPositions( .4d );
    }

	public void setupTransport() {

		Button playButton = new Button ( "▶" );
		Button pauseButton = new Button ( "𝍪" );
		Button stopButton = new Button ( "◼" );
		Button nextButton = new Button ( "⏩" );
		Button previousButton = new Button ( "⏪" );
		

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
                    
                    if ( selectedTrack == null && ! playlistTable.getItems().isEmpty() ) {
                		selectedTrack = playlistTable.getItems().get( 0 );
                        startTrack ( selectedTrack );
                    }
		    	} 
		    }
		});
		
		
		timeElapsedLabel = new Label ( "" );
		timeRemainingLabel = new Label ( "" );
		
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
		
		HBox buttons = new HBox();
		buttons.getChildren().add ( stopButton );
		buttons.getChildren().add ( playButton );
		buttons.getChildren().add ( pauseButton );
		buttons.getChildren().add ( previousButton );
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
		albumFilterPane = new TextField();
		albumFilterPane.textProperty().addListener((observable, oldValue, newValue) -> {
			albumListDataFiltered.setPredicate( album -> {
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
	}
	
    public void setupAlbumTable () {
        TableColumn artistColumn = new TableColumn ( "Artist" );
        TableColumn yearColumn =  new TableColumn ( "Year" );
        TableColumn albumColumn = new TableColumn ( "Album" );
        
        artistColumn.setCellValueFactory ( new PropertyValueFactory <Album, String> ( "Artist" ) );
        yearColumn.setCellValueFactory ( new PropertyValueFactory <Album, Integer> ( "Year" ) );
        albumColumn.setCellValueFactory ( new PropertyValueFactory <Album, String> ( "Title" ) );
        
        artistColumn.setSortType (TableColumn.SortType.ASCENDING );
                
        
        albumTable = new TableView();
        albumTable.getColumns().addAll ( artistColumn, yearColumn, albumColumn );
        albumTable.setEditable ( false );
        albumTable.setItems( albumListData );

    	albumListData.comparatorProperty().bind(albumTable.comparatorProperty());
    	
        albumTable.getSortOrder().add ( artistColumn );
        albumTable.getSortOrder().add ( yearColumn );
        albumTable.setColumnResizePolicy ( TableView.CONSTRAINED_RESIZE_POLICY );
        albumTable.setPlaceholder( new Label ( "No albums loaded." ) );
        
        albumTable.setRowFactory( tv -> {
            TableRow<Album> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    playAlbum( row.getItem() );
                }
            });
            return row;
        });
        
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
            @Override
            public void handle(ActionEvent event) {
                
            	Platform.runLater( new Runnable() {
            		public void run() {
            			try {
            				System.out.println ( Desktop.isDesktopSupported() );
            				//Desktop.getDesktop().open( albumTable.getSelectionModel().getSelectedItem().getPath().toFile() );
        				} catch (Exception e) {
        					// TODO Auto-generated catch block
        					e.printStackTrace();
        				}
            		}
            	});
            }
        });
        
        
        albumTable.setContextMenu( albumListContextMenu );
        
        
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
                    cc.put(SERIALIZED_MIME_TYPE, index);
                    db.setContent(cc);
                    event.consume();
                }
            });

            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    if (row.getIndex() != ((Integer)db.getContent(SERIALIZED_MIME_TYPE)).intValue()) {
                        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                        event.consume();
                    }
                }
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                    Track draggedPerson = playlistTable.getItems().remove(draggedIndex);

                    int dropIndex ; 

                    if (row.isEmpty()) {
                        dropIndex = playlistTable.getItems().size() ;
                    } else {
                        dropIndex = row.getIndex();
                    }

                    playlistTable.getItems().add(dropIndex, draggedPerson);

                    event.setDropCompleted(true);
                    playlistTable.getSelectionModel().select(dropIndex);
                    event.consume();
                }
            });
            return row;
        });
    }
}


