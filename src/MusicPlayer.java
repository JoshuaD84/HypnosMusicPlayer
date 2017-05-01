import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import players.DummyPlayer;
import players.FLACPlayer;
import players.AudioPlayer;
 

@SuppressWarnings({ "rawtypes", "unchecked" }) //TODO: Maybe get rid of this when I understand things better
public class MusicPlayer extends Application {

	public static final String PROGRAM_NAME = "Music Player";
	
	static ObservableList <Album> albumListData;
	
	final static ObservableList <Track> playlistData = FXCollections.observableArrayList ();
	
	static TableView albumTable;
	static TableView playlistTable;
	
	static BorderPane albumImage;
	static BorderPane artistImage;
	static TextField albumSearchPane;
	
	static VBox transport;
	
	AudioPlayer currentPlayer = new DummyPlayer();
		
    public static void main ( String[] args ) throws Exception {
    	
    	Path startingDirectory = Paths.get ( "/home/joshua/Desktop/music-test/" );
    	
    	long startTime = System.currentTimeMillis();
    	Files.walkFileTree( startingDirectory, new MusicFileVisitor () );
    	long endTime = System.currentTimeMillis();
    	
    	System.out.println ( "To read all music: " + ( endTime - startTime ) );
     
        Application.launch ( args );
        
    }
    
	@Override
    public void start ( Stage stage ) {
    	Scene scene = new Scene ( new Group(), 1024, 768 );
    	 
    	setupAlbumTable();
    	setupAlbumSearchPane();
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
        albumSearchPane.prefWidthProperty().bind ( albumListPane.widthProperty() );
        albumListPane.setTop( albumSearchPane );
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
        primarySplitPane.setDividerPositions( .5d );
        
    }

	public void setupTransport() {

		Button playButton = new Button ( "▶" );
		Button pauseButton = new Button ( "▋▋" );
		Button stopButton = new Button ( "◼" );
		Button nextButton = new Button ( "⏩" );
		Button previousButton = new Button ( "⏪" );
		
		stopButton.setOnAction(new EventHandler<ActionEvent>() {
		    @Override public void handle(ActionEvent e) {
		        currentPlayer.stop();
		    }
		});
		
		pauseButton.setOnAction(new EventHandler<ActionEvent>() {
		    @Override public void handle(ActionEvent e) {
		        currentPlayer.togglePause();
		    }
		});
		
		Label timeElapsed = new Label ( "2:02" );
		Label timeRemaining = new Label ( "-1:12" );
		//Label transportTitle = 
		
		Slider trackPosition = new Slider();
		trackPosition.setMin( 0 );
		trackPosition.setMax( 100 );
		trackPosition.setValue ( 40 ); //TODO: Delete
		trackPosition.setMaxWidth ( 600 );
		trackPosition.setMinWidth( 200 );
		trackPosition.setPrefWidth( 400 );

		HBox sliderPane = new HBox();
		sliderPane.getChildren().add ( timeElapsed );
		sliderPane.getChildren().add ( trackPosition );
		sliderPane.getChildren().add ( timeRemaining );
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
		Label trackInfo = new Label ( "Air - 1998 - Moon Safari - 01 - La Femme D'Argent" );
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
		ImageView view = new ImageView( new Image ( "file:front.jpg" ) );
		view.fitWidthProperty().set( 250 ); 
		view.fitHeightProperty().set( 250 ); 
		albumImage.setCenter( view );
	}
	
	public void setupArtistImage() {
		artistImage = new BorderPane();
		ImageView view = new ImageView( new Image ( "file:artist.jpg" ) );
		view.fitWidthProperty().set( 250 ); 
		view.fitHeightProperty().set( 250 ); 
		artistImage.setCenter( view );
	}
			
	public void setupAlbumSearchPane () {
		albumSearchPane = new TextField();
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
        albumTable.setItems( FXCollections.observableArrayList( new ArrayList <Album> ( MusicFileVisitor.albums ) ) );
        albumTable.getSortOrder().add ( artistColumn );
        albumTable.getSortOrder().add ( yearColumn );
        albumTable.setColumnResizePolicy ( TableView.CONSTRAINED_RESIZE_POLICY );
        
        albumTable.setRowFactory( tv -> {
            TableRow<Album> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    Album selectedAlbum = row.getItem();
                    playlistData.clear();
                    playlistData.addAll( selectedAlbum.getTracks() );
                }
            });
            return row;
        });
        
    }
    
    public void setupPlaylistTable () {
        TableColumn artistColumn = new TableColumn("Artist");
        TableColumn yearColumn =  new TableColumn("Year");
        TableColumn albumColumn = new TableColumn("Album");
        TableColumn titleColumn = new TableColumn("Title");
        TableColumn trackColumn = new TableColumn("#");
        TableColumn lengthColumn = new TableColumn("Length");
        
        artistColumn.setCellValueFactory( new PropertyValueFactory <Track, String> ("Artist") );
        yearColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer> ("Year") );
        albumColumn.setCellValueFactory( new PropertyValueFactory <Track, String> ("Album") );
        titleColumn.setCellValueFactory( new PropertyValueFactory <Track, String> ("Title") );
        trackColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer> ("trackNumber") );
        lengthColumn.setCellValueFactory( new PropertyValueFactory <Track, String> ("LengthDisplay") );
                
        playlistTable = new TableView();
        playlistTable.getColumns().addAll ( trackColumn, artistColumn, yearColumn, albumColumn, titleColumn, lengthColumn );
        playlistTable.setEditable ( false );
        playlistTable.setItems( playlistData );
        playlistTable.setColumnResizePolicy ( TableView.CONSTRAINED_RESIZE_POLICY );
      
        playlistTable.setRowFactory( tv -> {
            TableRow<Track> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    Track selectedTrack = row.getItem();
                    currentPlayer = new FLACPlayer( selectedTrack.getPath().toAbsolutePath().toString() );
                    currentPlayer.play();
                }
            });
            return row;
        });
    }
}


