package net.joshuad.hypnos.fxui;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.joshuad.hypnos.Album;
import net.joshuad.hypnos.MultiFileTagPair;
import net.joshuad.hypnos.Track;


@SuppressWarnings("rawtypes")
public class TagWindow extends Stage {
	private static transient final Logger LOGGER = Logger.getLogger( TagWindow.class.getName() );
	
	List<Track> tracks;
	List<Album> albums;
	 
	List <FieldKey> supportedTags = Arrays.asList (
		FieldKey.ARTIST, FieldKey.ALBUM_ARTIST, FieldKey.TITLE, FieldKey.ALBUM,
		FieldKey.YEAR, FieldKey.ORIGINAL_YEAR, FieldKey.TRACK, FieldKey.DISC_SUBTITLE, FieldKey.DISC_NO,
		FieldKey.DISC_TOTAL, FieldKey.MUSICBRAINZ_RELEASE_TYPE
		//TODO: Think about TITLE_SORT, ALBUM_SORT, ORIGINAL_YEAR -- we read them. 
	);
	
	List <FieldKey> hiddenTagsList = Arrays.asList( );
	
	final ObservableList <MultiFileTagPair> tagPairs = FXCollections.observableArrayList();
	
	HBox controlPanel = new HBox();
	
	TableColumn tagColumn;
	TableColumn valueColumn;
	
	FXUI ui;
	
	@SuppressWarnings({ "unchecked" })
	public TagWindow( FXUI ui ) {
		super();
		this.ui = ui;
		this.initModality( Modality.NONE );
		this.initOwner( ui.getMainStage() );
		this.setTitle( "Tag Editor" );
		this.setWidth( 600 );
		this.setHeight ( 700 );
		Pane root = new Pane();
		Scene scene = new Scene( root );
		VBox primaryPane = new VBox();
		
		tagColumn = new TableColumn( "Tag" );
		valueColumn = new TableColumn( "Value" );
		
		tagColumn.setStyle( "-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-padding: 0 10 0 0; ");
		
		tagColumn.setMaxWidth( 350000 );
		valueColumn.setMaxWidth( 650000 );
		valueColumn.setEditable( true );
		
		tagColumn.setCellValueFactory( new PropertyValueFactory <MultiFileTagPair, String>( "TagName" ) );
		valueColumn.setCellValueFactory( new PropertyValueFactory <MultiFileTagPair, String>( "Value" ) );
		
		valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		valueColumn.setOnEditCommit( new EventHandler <CellEditEvent <MultiFileTagPair, String>>() {
			@Override
			public void handle ( CellEditEvent <MultiFileTagPair, String> t ) {
				((MultiFileTagPair) t.getTableView().getItems().get( t.getTablePosition().getRow() )).setValue( t.getNewValue() );
			}
		} );
		
		tagColumn.setSortable( false );
		valueColumn.setSortable( false );
		
		TableView <MultiFileTagPair> tagTable = new TableView<MultiFileTagPair> ();
		tagTable.setItems ( tagPairs );
		tagTable.getColumns().addAll( tagColumn, valueColumn );
		tagTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		tagTable.setEditable( true );
		
		tagTable.prefWidthProperty().bind( this.widthProperty() );
		
		setupControlPanel();

		primaryPane.getChildren().addAll( tagTable, controlPanel );
		root.getChildren().add( primaryPane );
		
		tagTable.prefHeightProperty().bind( primaryPane.heightProperty().subtract( controlPanel.heightProperty() ) );
		tagTable.prefWidthProperty().bind( primaryPane.widthProperty() );
		
		primaryPane.prefWidthProperty().bind( root.widthProperty() );
		primaryPane.prefHeightProperty().bind( root.heightProperty() );
		
		setScene( scene );
	}
	
	public void setupControlPanel() {
		Button saveButton = new Button ( "Save" );
		Button revertButton = new Button ( "Revert" );
		Button cancelButton = new Button ( "Cancel" );
		
		saveButton.setPrefWidth( 100 );
		revertButton.setMinWidth( 100 );
		cancelButton.setMinWidth( 100 );
		
		saveButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				saveCurrentTags();
				close();
			}
		});
		
		revertButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				setTracks ( tracks, albums, (FieldKey[])hiddenTagsList.toArray() );
			}
		});
		
		Stage stage = this;
		cancelButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				stage.hide();
			}
		});

		controlPanel.setAlignment( Pos.CENTER );
		controlPanel.getChildren().addAll ( cancelButton, revertButton, saveButton );
		controlPanel.prefWidthProperty().bind( this.widthProperty() );
		controlPanel.setPadding( new Insets( 5 ) );
	}

	private void saveCurrentTags() {
		Thread saverThread = new Thread( () -> {
			if ( tracks != null ) {
				for ( Track track : tracks ) {
					track.updateTagsAndSave( tagPairs );
					ui.library.addTrack( track ); //TODO: This just causes the track to be updated, probably should rename the function
				}
			}
			
			//TODO: it would be nice to get rid of these functions by using observable items. 
			ui.refreshAlbumTable();
			ui.refreshTrackTable();
			ui.refreshCurrentList();
			ui.refreshQueueList();
			ui.refreshHistory();
			
		});
		saverThread.setDaemon( true );
		saverThread.start();
	}
	
	public void setTracks ( List <Track> tracks, List <Album> albumsToRefresh, FieldKey ... hiddenTags ) { 
		tagPairs.clear();
		this.hiddenTagsList = Arrays.asList( hiddenTags );
		this.tracks = tracks;
		this.albums = albumsToRefresh;

		if ( tracks.size() <= 0 ) return;
		
		try {
			
			Track firstTrack = tracks.get( 0 );
			
			AudioFile firstAudioFile = AudioFileIO.read( firstTrack.getPath().toFile() );
			
			Tag firstTag = firstAudioFile.getTag();
			
			if ( firstTag == null ) {
				firstTag = firstAudioFile.createDefaultTag();
			}
			
			for ( FieldKey key : supportedTags ) {
				
				if ( hiddenTagsList.contains( key ) ) continue;
				
				try {
					String value = firstTag.getFirst( key );
					tagPairs.add( new MultiFileTagPair ( key, value ) );
				} catch ( KeyNotFoundException e ) {
					tagPairs.add( new MultiFileTagPair ( key, "" ) );
				}
			}
		
			if ( tracks.size() == 1 ) return;
			
			for ( int k = 1 ; k < tracks.size() ; k++ ) {
				
				Track track = tracks.get ( k );
				
				AudioFile audioFile = AudioFileIO.read( track.getPath().toFile() );
				
				Tag tag = audioFile.getTag();
				
				if ( tag == null ) {
					tag = audioFile.createDefaultTag();
				}
				
				for ( MultiFileTagPair tagPair : tagPairs ) {
					FieldKey key = tagPair.getKey();

					if ( hiddenTagsList.contains( key ) ) continue;
					
					try {
						String value = tag.getFirst( key );
						tagPair.anotherFileValue( value );
					} catch ( KeyNotFoundException e ) {
						tagPair.anotherFileValue( "" );
					}
				}
			}
		
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to set data for tag window.", e );
		}
	}
}

