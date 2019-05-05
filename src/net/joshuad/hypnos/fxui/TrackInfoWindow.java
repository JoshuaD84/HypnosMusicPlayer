package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.library.Track;

public class TrackInfoWindow extends Stage {
	private static final Logger LOGGER = Logger.getLogger( TrackInfoWindow.class.getName() );
	
	private Track track = null;
	
	private TableView <TrackFieldPair> table;

	Button browseButton;
	TextField locationField;
	
	public TrackInfoWindow ( FXUI ui ) {
		super();
		
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
		
		BorderPane root = new BorderPane();
		Scene scene = new Scene( root );
		
		VBox primaryPane = new VBox();
		primaryPane.prefWidthProperty().bind( root.widthProperty() );
		primaryPane.prefHeightProperty().bind( root.heightProperty() );
		
		Label label = new Label ( "Location: " );
		label.setAlignment( Pos.CENTER_RIGHT );
		
		locationField = new TextField();
		locationField.setEditable( false );
		locationField.setMaxWidth( Double.MAX_VALUE );
		
		HBox.setHgrow( locationField, Priority.ALWAYS );
		browseButton = new Button( "Browse" );
		browseButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				if ( track != null ) {
					ui.openFileBrowser( track.getPath() );
				}
			}
		});
		
		HBox locationBox = new HBox();
		locationBox.getChildren().addAll( label, locationField, browseButton );
		label.prefHeightProperty().bind( locationBox.heightProperty() );
		locationBox.prefWidthProperty().bind( primaryPane.widthProperty() );
		
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
		
		root.setTop ( locationBox );
		root.setCenter ( primaryPane );
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
		
		locationField.setText( track.getPath().toString() );

		table.getItems().add ( new TrackFieldPair ( "Title", track.getTitle() ) );
		table.getItems().add ( new TrackFieldPair ( "Artist", track.getArtist() ) );
		table.getItems().add ( new TrackFieldPair ( "Album", track.getFullAlbumTitle() ) );
		table.getItems().add ( new TrackFieldPair ( "Year", track.getYear() ) );
		table.getItems().add ( new TrackFieldPair ( "Length", track.getLengthDisplay() ) );
		table.getItems().add ( new TrackFieldPair ( "File Name", track.getPath().getFileName().toString() ) );
		table.getItems().add ( new TrackFieldPair ( "Encoding", track.getShortEncodingString() ) );
	}
}

