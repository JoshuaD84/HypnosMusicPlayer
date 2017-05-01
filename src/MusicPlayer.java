import java.io.File;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
 

@SuppressWarnings({ "rawtypes", "unchecked" }) //TODO: Maybe get rid of this when I understand things better
public class MusicPlayer extends Application {

	public static final String PROGRAM_NAME = "Music Player";
	
	final static ObservableList <Album> albumListData = FXCollections.observableArrayList(
    	new Album ("Air", 2009, "Pocket WhateveR" ),	
    	new Album ("Air", 2008, "Pocket WhateveR" ),
    	new Album ("Air", 2007, "Pocket WhateveR" ),
    	new Album ("Air", 2006, "Pocket WhateveR" ),
    	new Album ("Air", 2005, "Pocket WhateveR" ),
    	new Album ("Air", 2004, "Pocket WhateveR" ),
    	new Album ("Josh", 2008, "wefwef" ),
    	new Album ("EWFEW", 2005, "EEFEFE" ),
    	new Album ("Maria", 1949, "AAA" ),
    	new Album ("Thomas", 2000, "DDDDD" ),
    	new Album ("234234", 1999, "wefwef" ),
    	new Album ("wefawfasdfs", 2008, "AAAAA" ),
    	new Album ("qqqqqq", 2008, "VBBBBB" )
    );
	
	final static ObservableList <Track> playlistData = FXCollections.observableArrayList (
		new Track ( "Air", 2009, "Pocket Whatever", "Johs's Song", 5, 344 ),
		new Track ( "Air", 2009, "Pocket Whatever", "Song ABC", 1, 1344 ),
		new Track ( "Air", 2009, "Pocket Whatever", "GGG", 2, 443 ),
		new Track ( "Air", 2009, "Pocket Whatever", "HLIFJWE", 3, 992 ),
		new Track ( "Air", 2009, "Pocket Whatever", "Living Free", 4, 116 ),
		new Track ( "Air", 2009, "Pocket Whatever", "Absbolute", 6, 644 )
	);
	
	static TableView albumTable;
	static TableView playlistTable;
	
	static BorderPane albumImage;
	static BorderPane artistImage;
	static TextField albumSearchPane;
	
	static VBox transport;
	
	
	
    public static void main ( String[] args ) throws Exception {

        /*AudioFile audioFile = AudioFileIO.read( new File ( "song.flac" ) );
        Tag tag = audioFile.getTag();
        
        System.out.println ( tag.getFirst ( FieldKey.ARTIST ) );
        System.out.println ( tag.getFirst ( FieldKey.ALBUM ) );
        System.out.println ( tag.getFirst ( FieldKey.TITLE ) );
        System.out.println ( tag.getFirst ( FieldKey.TRACK ) );
		*/
             
     
        Application.launch ( args );
        
    }
    
	@Override
    public void start ( Stage stage ) {
    	Scene scene = new Scene ( new Group() );
    	 
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
        stage.setWidth ( 1024 );
        stage.setHeight ( 768 );
        ((Group) scene.getRoot()).getChildren().addAll( primaryContainer );
        
        stage.setScene ( scene );
        stage.show();
    }

	public void setupTransport() {

		Button playButton = new Button ( "▶" );
		Button pauseButton = new Button ( "▋▋" );
		Button stopButton = new Button ( "◼" );
		Button nextButton = new Button ( "⏩" );
		Button previousButton = new Button ( "⏪" );
		
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
        albumColumn.setCellValueFactory ( new PropertyValueFactory <Album, String> ( "Album" ) );
        
        artistColumn.setSortType (TableColumn.SortType.ASCENDING );
                
        albumTable = new TableView();
        albumTable.getColumns().addAll ( artistColumn, yearColumn, albumColumn );
        albumTable.setEditable ( false );
        albumTable.setItems( albumListData );
        albumTable.getSortOrder().add ( artistColumn );
        albumTable.setColumnResizePolicy ( TableView.CONSTRAINED_RESIZE_POLICY );
        
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
        lengthColumn.setCellValueFactory( new PropertyValueFactory <Track, String> ("Length") );
                
        playlistTable = new TableView();
        playlistTable.getColumns().addAll ( trackColumn, artistColumn, yearColumn, albumColumn, titleColumn, lengthColumn );
        playlistTable.setEditable ( false );
        playlistTable.setItems( playlistData );
        playlistTable.setColumnResizePolicy ( TableView.CONSTRAINED_RESIZE_POLICY );
        
    }
}


