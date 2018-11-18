package net.joshuad.hypnos.fxui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;

class ProgressIndicatorBar extends StackPane {

	final private ProgressBar bar = new ProgressBar();
	final private Label text = new Label();
	final private static int DEFAULT_LABEL_PADDING = 5;

	ProgressIndicatorBar ( ) {

		bar.maxWidthProperty().bind( this.prefWidthProperty() );
		text.maxWidthProperty().bind( this.prefWidthProperty() );
		text.setAlignment( Pos.CENTER );
		StackPane.setAlignment( text, Pos.CENTER );
		bar.setMinHeight( 30 );
		bar.setMaxHeight( 30 );
		bar.setPrefHeight( 30 );
		bar.setProgress( 0 );
		text.setText ( "" );
		text.getStyleClass().add( "progress-indicator-text" );
		

		getChildren().setAll( bar, text );
	}

	public void setStatus ( String message, double percent ) {
		text.setText( message );
		bar.setProgress( percent );
	
	}
}