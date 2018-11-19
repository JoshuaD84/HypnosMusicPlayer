package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Track;

public class TrackInfoWindow extends Stage {
	private static final Logger LOGGER = Logger.getLogger( TrackInfoWindow.class.getName() );
	
	private FXUI ui;
	
	private Track track = null;
	
	private TableView <TrackFieldPair> table;
	
	public TrackInfoWindow ( FXUI ui ) {
		super();
		
		this.ui = ui;
		
		initModality( Modality.NONE );
		initOwner( ui.getMainStage() );
		setTitle( "Track Info" );
		setWidth( 600 );
		setHeight( 400 );
		
		try {
			getIcons().add( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) ) );
		} catch ( FileNotFoundException e ) {
			LOGGER.warning( "Unable to load program icon: resources/icon.png" );
		}
		
		Pane root = new Pane();
		Scene scene = new Scene( root );
		
		VBox primaryPane = new VBox();
		primaryPane.prefWidthProperty().bind( root.widthProperty() );
		primaryPane.prefHeightProperty().bind( root.heightProperty() );
		
		table = new TableView <> ();
		table.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		table.setEditable( false );
		table.prefWidthProperty().bind( primaryPane.widthProperty() );
		table.prefHeightProperty().bind( primaryPane.heightProperty() );
		
		TableColumn <TrackFieldPair, String> labelColumn = new TableColumn<> ();
		TableColumn <TrackFieldPair, Object> valueColumn = new TableColumn<> ();
		
		labelColumn.setMaxWidth ( 25000 );
		valueColumn.setMaxWidth ( 75000 );

		labelColumn.setReorderable ( false );
		valueColumn.setReorderable ( false );
		
		labelColumn.setCellValueFactory( new PropertyValueFactory <TrackFieldPair, String> ( "Label" ) );
		valueColumn.setCellValueFactory( new PropertyValueFactory <TrackFieldPair, Object> ( "Value" ) );
		
		labelColumn.setText( "Field" );
		valueColumn.setText( "Value" );
		
		table.getColumns().addAll( labelColumn, valueColumn );
		
		primaryPane.getChildren().add( table );
		
		root.getChildren().add( primaryPane );
		setScene( scene );
		
		scene.addEventFilter( KeyEvent.KEY_PRESSED, new EventHandler <KeyEvent>() {
			@Override
			public void handle ( KeyEvent e ) {
				if ( e.getCode() == KeyCode.ESCAPE
				&& !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() && !e.isAltDown() ) {
					hide();
					e.consume();
				}
			}
		});
		
	}
	
	public void setTrack ( Track track ) {
		this.track = track;
		table.getItems().clear();
		
		if ( track == null ) return;

		table.getItems().add ( new TrackFieldPair ( "Title", track.getTitle() ) );
		table.getItems().add ( new TrackFieldPair ( "Artist", track.getArtist() ) );
		table.getItems().add ( new TrackFieldPair ( "Album", track.getFullAlbumTitle() ) );
		table.getItems().add ( new TrackFieldPair ( "Year", track.getYear() ) );
		table.getItems().add ( new TrackFieldPair ( "Length", track.getLengthDisplay() ) );
		table.getItems().add ( new TrackFieldPair ( "File Name", track.getPath().getFileName().toString() ) );
		table.getItems().add ( new TrackFieldPair ( "File Location", track.getPath().getParent().toString() ) );
		table.getItems().add ( new TrackFieldPair ( "Encoding", track.getShortEncodingString() ) );
	}
}

