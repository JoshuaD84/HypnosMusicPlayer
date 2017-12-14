package net.joshuad.hypnos.fxui;

import java.util.List;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ArtistImageSaveDialog extends Stage {
	
	public enum Choice {
		ALL,
		ALBUM,
		TRACK,
		CANCEL
	}
	
	private Choice selectedChoice = Choice.CANCEL;
	CheckBox override;
	
	public ArtistImageSaveDialog ( Stage mainStage, List <Choice> choices ) {
		super( );
		
		Group root = new Group();
		Scene scene = new Scene( root );
		VBox primaryPane = new VBox();
		
		this.setResizable( false );
		
		primaryPane.setPadding( new Insets ( 10, 10, 10, 10 ) );
		primaryPane.setSpacing( 20 );

		setTitle( "Set Artist Image" );
		initModality ( Modality.APPLICATION_MODAL );
		
		double x = mainStage.getX() + mainStage.getWidth() / 2 - 200;  
		double y = mainStage.getY() + mainStage.getHeight() / 2 - 100;
		setX ( x );
		setY ( y );

		HBox info = new HBox();
		info.setPadding( new Insets ( 20, 0, 0, 0 ) );
		Label infoLabel = new Label ( "How do you want to set the artist image? More specific fields have higher priority." );
		info.getChildren().addAll( infoLabel );
			
		HBox controls = new HBox();
		override = new CheckBox ( "Overwrite all existing artist tag images" );
		override.setPadding( new Insets ( 0, 0, 0, 0 ) );
		controls.getChildren().addAll( override );
		
		HBox buttons = new HBox();
		buttons.setPadding( new Insets ( 10, 10, 10, 10 ) );
		buttons.setSpacing( 10 );
		Button all = new Button ( "All albums by this artist" );
		Button album = new Button ( "This album only" );
		Button track = new Button ( "This track only" );
		Button cancel = new Button ( "Cancel" );
		
		buttons.getChildren().addAll( all, album, track, cancel );
		
		if ( !choices.contains( Choice.ALL ) ) {
			all.setDisable( true );
		}
		
		if ( !choices.contains( Choice.ALBUM ) ) {
			album.setDisable( true );
		}
		
		if ( !choices.contains( Choice.TRACK ) ) {
			track.setDisable( true );
		}
		
		primaryPane.getChildren().addAll( info, controls, buttons );
		
		root.getChildren().add( primaryPane );
		setScene( scene );
		
		all.setOnAction( ( ActionEvent e ) -> {
			selectedChoice = Choice.ALL;
			close();
		});
		
		album.setOnAction( ( ActionEvent e ) -> {
			selectedChoice = Choice.ALBUM;
			close();
		});
		
		track.setOnAction( ( ActionEvent e ) -> {
			selectedChoice = Choice.TRACK;
			close();
		});
		
		cancel.setOnAction( ( ActionEvent e ) -> {
			selectedChoice = Choice.CANCEL;
			close();
		});
	}
	
	public Choice getSelectedChoice () {
		return selectedChoice;
	}
	
	public boolean getOverwriteAllSelected() {
		return override.isSelected();
	}
}
