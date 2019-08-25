package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.library.Library;

public class LibraryLogWindow extends Stage {
	private static final Logger LOGGER = Logger.getLogger( LibraryLogWindow.class.getName() );
	ListView<String> logView;
	ObservableList<String> logData = FXCollections.observableArrayList();
	
	Library library;
	
	public LibraryLogWindow ( FXUI ui, Library library ) {
		super();
		
		this.library = library;
		
		initModality( Modality.NONE );
		initOwner( ui.getMainStage() );
		setTitle( "Library Log" );
		setWidth( 600 );
		setHeight( 600 );
		
		try {
			getIcons().add( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) ) );
		} catch ( FileNotFoundException e ) {
			LOGGER.log( Level.WARNING, "Unable to load program icon: resources/icon.png", e );
		}
		
		VBox root = new VBox();
		Scene scene = new Scene( root );
		logView = new ListView<>(logData);
		logView.setEditable( false );
		logView.prefHeightProperty().bind( root.heightProperty() );
		logView.getStyleClass().add( "monospaced" );
		logView.fixedCellSizeProperty().set(24);
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
		
		Thread logReader = new Thread( () -> {
			while(true) {
				String[] newData = library.getScanLogger().dumpBuffer().split("\n");
				Platform.runLater(() -> {
					for( String line : newData ) {
						if(!line.isBlank()) {
							logData.add(line);
						}
					}
				});
				try {
					Thread.sleep( 1000 );
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
				}
			}
		});
		
		logReader.setName( "Library Log UI Text Loader" );
		logReader.setDaemon( true );
		logReader.start();
	}
}

