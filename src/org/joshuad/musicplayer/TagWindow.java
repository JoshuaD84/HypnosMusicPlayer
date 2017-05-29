package org.joshuad.musicplayer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;


@SuppressWarnings("rawtypes")
public class TagWindow extends Stage {
	
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
	
	@SuppressWarnings({ "unchecked" })
	public TagWindow( Stage owner ) {
		super();
		this.initModality( Modality.NONE );
		this.initOwner( owner );
		this.setTitle( "Tag Editor" );
		this.setWidth( 600 );
		//this.setHeight ( 400 );
		Group root = new Group();
		Scene scene = new Scene( root );
		VBox primaryPane = new VBox();
		
		tagColumn = new TableColumn( "Tag" );
		valueColumn = new TableColumn( "Value" );
		
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
		
		for ( Track track : tracks ) {
			try {
				AudioFile audioFile = AudioFileIO.read( track.getPath().toFile() );
				Tag tag = audioFile.getTag();
				
				//TODO: read value thing instead of cell value
				for ( int k = 0; k < tagPairs.size(); k++ ) {
					FieldKey key = FieldKey.valueOf( (String)tagColumn.getCellData( k ) );
					String newValue = (String)valueColumn.getCellData( k );

					if ( hiddenTagsList.contains( key ) ) continue;
					
					if ( !tagPairs.get( k ).isMultiValue() ) {
						tag.setField( key, newValue );
					}
				}
				
				//TODO: Maybe reduce to one runLater
				Platform.runLater( new Runnable() {
					public void run() {
						try {
							audioFile.setTag( tag );
							AudioFileIO.write( audioFile );
							try {
								track.refreshTagData();
							} catch ( IOException e ) {
								//TODO: I don't think we need to do anything here?
							}
							MusicPlayerUI.currentListTable.refresh();
						} catch ( CannotWriteException e ) {
							e.printStackTrace(); //TODO: 
						}
					}
				});
				
			} catch ( CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e ) {
				e.printStackTrace();
			}
		}
		
		Platform.runLater( new Runnable() {
			public void run() {
				if ( albums != null ) {
					for ( Album album : albums ) {
						album.refreshTagData();
						MusicPlayerUI.albumTable.refresh();
					}
				}
			}
		});
	}
	
	public void setTracks ( List <Track> tracks, List <Album> albumsToRefresh, FieldKey ... hiddenTags ) { 
		tagPairs.clear();
		this.hiddenTagsList = Arrays.asList( hiddenTags );
		this.tracks = tracks;
		this.albums = albumsToRefresh;

		if ( tracks.size() <= 0 ) return;
		
		try {
			
			Track firstTrack = tracks.get( 0 );
			
			Tag firstTag = AudioFileIO.read( firstTrack.getPath().toFile() ).getTag();
			
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
				Tag tag = AudioFileIO.read( track.getPath().toFile() ).getTag();
				
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
		
		} catch ( CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e ) {
			e.printStackTrace();
		}
	}
}

