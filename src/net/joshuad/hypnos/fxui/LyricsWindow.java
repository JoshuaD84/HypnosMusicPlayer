package net.joshuad.hypnos.fxui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.joshuad.hypnos.HypnosURLS;
import net.joshuad.hypnos.library.Track;
import net.joshuad.hypnos.lyrics.Lyrics;
import net.joshuad.hypnos.lyrics.LyricsFetcher;

public class LyricsWindow extends Stage {
	
	private LyricsFetcher lyricsParser = new LyricsFetcher();
	
	private TextArea lyricsArea = new TextArea();
	private Label headerLabel = new Label ( "" );
	private Hyperlink sourceHyperlink = new Hyperlink ( "" );
	private Tooltip sourceTooltip = new Tooltip ( "" );
	private Label sourceLabel = new Label ( "Source:" );
	private String sourceURL = null;
	private Button searchWebButton = new Button( "Search on Web" );
	private Track track;
	
	public LyricsWindow ( FXUI ui ) {
		super();
		
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
		
		lyricsArea.setEditable( false );
		lyricsArea.setWrapText( true );
		lyricsArea.getStyleClass().add( "lyricsTextArea" );
		lyricsArea.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent");
		lyricsArea.getStyleClass().add( "lyrics" );
		lyricsPane.getChildren().add( lyricsArea );
		
		sourceHyperlink.setVisited( true );
		sourceHyperlink.setTooltip( sourceTooltip );
		sourceHyperlink.setVisible( false );
		sourceHyperlink.setOnAction( ( ActionEvent e ) -> {
			if ( sourceURL != null && !sourceURL.isEmpty() ) {
				ui.openWebBrowser( sourceURL );
			}
		});
		
		searchWebButton.setOnAction( ( ActionEvent e ) -> {
			String searchSlug = null;
			if ( track != null ) {
				searchSlug = "lyrics " + track.getAlbumArtist() + " " + track.getAlbumTitle();
				ui.openWebBrowser(HypnosURLS.getDDGSearchURL(searchSlug));
			}
		});

		sourceLabel.setVisible( false );
		
		Region spring = new Region();
		HBox.setHgrow(spring, Priority.ALWAYS);
		HBox sourceBox = new HBox();
		sourceBox.getChildren().addAll ( sourceLabel, sourceHyperlink, spring, searchWebButton );
		sourceBox.setAlignment( Pos.CENTER );
		
		lyricsPane.getChildren().add( sourceBox );

		lyricsArea.prefHeightProperty().bind( lyricsPane.heightProperty().subtract( headerLabel.heightProperty() ) );
		lyricsArea.prefWidthProperty().bind( lyricsPane.widthProperty() );
		
		lyricsPane.prefWidthProperty().bind( root.widthProperty() );
		lyricsPane.prefHeightProperty().bind( root.heightProperty() );
		
		root.getChildren().add( lyricsPane );
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
		if ( track == null ) {
			return;
		}

		headerLabel.setText( track.getArtist() + " - " + track.getTitle() );
		lyricsArea.setText( "loading..." );
		sourceHyperlink.setVisible( false );
		sourceLabel.setVisible( false );
		
		Thread lyricThread = new Thread () {
			public void run() {
				Lyrics lyrics = lyricsParser.get( track );
				
				Platform.runLater( () -> {
					if ( !lyrics.hadScrapeError() ) {
						lyricsArea.setText( lyrics.getLyrics() );
						sourceHyperlink.setText( lyrics.getSite().getName() );
						sourceURL = lyrics.getSourceURL();
						sourceTooltip.setText( lyrics.getSourceURL() );
						sourceHyperlink.setVisible( true );
						sourceLabel.setVisible( true );
					} else {
						lyricsArea.setText( "Unable to load lyrics for this song." );
						sourceURL = "";
					}
				});
			}
		};
		
		lyricThread.setDaemon( true );
		lyricThread.setName( "Lyric Fetcher" );
		lyricThread.start();
	}
}

