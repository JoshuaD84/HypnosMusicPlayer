package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.joshuad.hypnos.Hypnos;

public class LibraryLogWindow extends Stage {
	private static final Logger LOGGER = Logger.getLogger( LibraryLogWindow.class.getName() );
	TextArea logView;
	
	public LibraryLogWindow ( FXUI ui ) {
		super();
		
		initModality( Modality.NONE );
		initOwner( ui.getMainStage() );
		setTitle( "Library Log" );
		setWidth( 600 );
		setHeight( 600 );
		
		try {
			getIcons().add( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) ) );
		} catch ( FileNotFoundException e ) {
			LOGGER.warning( "Unable to load program icon: resources/icon.png" );
		}
		
		VBox root = new VBox();
		Scene scene = new Scene( root );
		logView = new TextArea();
		logView.setEditable( false );
		logView.prefHeightProperty().bind( root.heightProperty() );
		logView.getStyleClass().add( "monospaced" );
		logView.setWrapText( true );
		root.getChildren().add( logView );
		
		
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
}

