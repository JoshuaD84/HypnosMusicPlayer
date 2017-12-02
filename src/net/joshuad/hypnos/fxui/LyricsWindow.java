package net.joshuad.hypnos.fxui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.lyrics.LyricsFetcher;

public class LyricsWindow extends Stage {
	
	private FXUI ui;
	
	private Track track = null;
	
	private LyricsFetcher lyricsParser = new LyricsFetcher();
	
	private TextFlow titleFlow;
	private Text titleText;
	
	TextArea lyricsArea;
	Label headerLabel = new Label ( "" );
	
	public LyricsWindow ( FXUI ui ) {
		super();
		
		this.ui = ui;
		
		initModality( Modality.NONE );
		initOwner( ui.getMainStage() );
		setTitle( "Lyrics" );
		setWidth( 600 );
		setHeight( 700 );
		
		Pane root = new Pane();
		Scene scene = new Scene( root );
		VBox lyricsPane = new VBox();
		lyricsPane.setAlignment( Pos.CENTER );
		lyricsPane.setPadding( new Insets ( 10 ) );
		
		headerLabel.setPadding( new Insets ( 0, 0, 10, 0 ) );
		headerLabel.setWrapText( true );
		headerLabel.setTextAlignment( TextAlignment.CENTER );
		headerLabel.setStyle( "-fx-font-size: 16px; -fx-font-weight: bold" );
		lyricsPane.getChildren().add( headerLabel );
		
		lyricsArea = new TextArea();
		lyricsArea.setEditable( false );
		lyricsArea.setWrapText( true );
		lyricsArea.getStyleClass().add( "lyricsTextArea" );
		
		lyricsPane.getChildren().addAll( lyricsArea );

		lyricsArea.prefHeightProperty().bind( lyricsPane.heightProperty().subtract( headerLabel.heightProperty() ) );
		lyricsArea.prefWidthProperty().bind( lyricsPane.widthProperty() );
		
		lyricsPane.prefWidthProperty().bind( root.widthProperty() );
		lyricsPane.prefHeightProperty().bind( root.heightProperty() );
		
		root.getChildren().add( lyricsPane );
		setScene( scene );
		
	}
	
	public void setTrack ( Track track ) {
		this.track = track;
		if ( track == null ) return;

		headerLabel.setText( track.getArtist() + " - " + track.getTitle() );
		lyricsArea.setText( "loading..." );
		
		Thread lyricThread = new Thread () {
			public void run() {
				String lyricsString = lyricsParser.get( track );
				
				Platform.runLater( () -> {
					if ( lyricsString != null ) {
						lyricsArea.setText( lyricsString );
					} else {
						lyricsArea.setText( "Unable to load lyrics for this song." );
					}
				});
			}
		};
		
		lyricThread.setDaemon( true );
		lyricThread.setName( "lyric-fetcher" );
		lyricThread.start();
	}
}

