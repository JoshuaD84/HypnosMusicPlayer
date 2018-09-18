package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.joshuad.hypnos.CurrentList;
import net.joshuad.hypnos.CurrentListTrack;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;

public class ExportPlaylistPopup extends Stage {
	
	private static final Logger LOGGER = Logger.getLogger( ExportPlaylistPopup.class.getName() );

	FXUI ui;
	
	private final ProgressBar progressBar;
	private final Label label;
	
	private boolean doingExport = false;
	private boolean stopRequested = false;
	
	public ExportPlaylistPopup ( FXUI ui ) {
		super();
		this.ui = ui;
		
		Pane root = new Pane();
		Scene scene = new Scene( root );
		VBox primaryPane = new VBox();
		
		initModality( Modality.NONE );
		initOwner( ui.getMainStage() );
		setTitle( "Export Tracks" );
		
		try {
			getIcons().add( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) ) );
		} catch ( FileNotFoundException e ) {
			LOGGER.warning( "Unable to load program icon: resources" + File.separator + "icon.png" );
		}
		
		progressBar = new ProgressBar();

		progressBar.setPrefWidth( 400 );
		progressBar.setPrefHeight( 40 );
		
		label = new Label ( "Hi Josh" );

		primaryPane.getChildren().addAll( progressBar, label );
		root.getChildren().add( primaryPane );
		setScene( scene );
		sizeToScene();
		setResizable( false );
		setOnCloseRequest( event -> {
			if ( doingExport ) {
				Optional<ButtonType> choice = confirmCancel();
				if ( choice.isPresent() && choice.get() == ButtonType.YES ) {
					stopRequested = true;
					hide();
				} 
			} else {
				hide();
			}
		});
	}
	

	public void export ( List <Track> tracks ) {
		
		if ( doingExport ) {
			Platform.runLater( ()-> {
				ui.notifyUserError( "Currently exporting, please cancel that task or wait for it to finish." );
			});
			return;
		}

		doingExport = true;
		stopRequested = false;
		progressBar.progressProperty().set( 0 );
		
		Path targetFolder = ui.promptUserForFolder().toPath();
		if ( targetFolder == null ) {
			return;
		}
		
		this.show();
		
		Runnable exportRunnable = ()-> {
			if ( !Files.isDirectory( targetFolder ) ) {
				Platform.runLater( ()-> {
					ui.alertUser( AlertType.WARNING, "Unable to Copy the following files:", "Unable to Copy Files", 
					"Destination is not a folder" );
				});
				return;
			}
			
			String error = "";
			
			int playlistIndex = 1;
			for ( Track track : tracks ) {
				if ( stopRequested ) break;
				String number = String.format( "%02d", playlistIndex );
				String extension = Utils.getFileExtension( track.getPath() );
				String name = track.getArtist() + " - " + track.getTitle();
				String fullName = number + " - " + name + "." + extension;
				Path targetOut = targetFolder.resolve( fullName );
				
				Platform.runLater( ()-> label.setText( "Exporting: " + fullName ) );
				
				try {
					Files.copy(	track.getPath(), targetOut );
				} catch ( FileAlreadyExistsException ex ) {
					if ( !error.equals( "" ) ) error += "\n\n";
					error += "File already exists, not overwritten: " + targetOut;
				} catch ( IOException ex ) {
					if ( !error.equals( "" ) ) error += "\n\n";
					error += "Unable to save file (" + ex.getMessage() + "): " + targetOut;
				}
				playlistIndex++;
				progressBar.progressProperty().setValue( playlistIndex / (double)tracks.size() );
			}
			
			if ( !error.equals( "" ) ) {
				final String finalError = error;
				Platform.runLater( ()-> {
					ui.alertUser( AlertType.WARNING, "Unable to Copy the following files:", "Unable to Copy Files", finalError );
				});
			}
			doingExport = false;
		};
		
		Thread t = new Thread ( exportRunnable );
		t.setDaemon( true );
		t.start();
	}
	
	private Optional <ButtonType> confirmCancel () {
		Alert alert = new Alert( AlertType.CONFIRMATION );
		
		double x = getX() + getWidth() / 2 - 220; //It'd be nice to use alert.getWidth() / 2, but it's NAN now. 
		double y = getY() + getHeight() / 2 - 50;
		
		alert.setX( x );
		alert.setY( y );
		
		alert.setDialogPane( new DialogPane() {
			protected Node createDetailsButton () {
				return new Pane();
			}
			
			@Override
			protected Node createButtonBar () {
				//This lets me specify my own button order below
				ButtonBar node = (ButtonBar) super.createButtonBar();
				node.setButtonOrder( ButtonBar.BUTTON_ORDER_NONE );
				return node;
			}
			
		});

		FXUI.setAlertWindowIcon ( alert );
		ui.applyCurrentTheme ( alert );

		alert.getDialogPane().getButtonTypes().addAll( ButtonType.YES, ButtonType.NO,  ButtonType.CANCEL );
		alert.getDialogPane().setContentText( "Cancel Export?" );
		alert.getDialogPane().setExpandableContent( new Group() );
		alert.getDialogPane().setExpanded( false );
		alert.getDialogPane().setGraphic( alert.getDialogPane().getGraphic() );
		alert.setTitle( "Cancel Export?" );
		alert.setHeaderText( null );
		
		return alert.showAndWait();
	}
}
