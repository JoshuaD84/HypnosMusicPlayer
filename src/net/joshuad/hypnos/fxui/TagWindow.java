package net.joshuad.hypnos.fxui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import net.joshuad.hypnos.Album;
import net.joshuad.hypnos.MultiFileImageTagPair;
import net.joshuad.hypnos.MultiFileImageTagPair.ImageFieldKey;
import net.joshuad.hypnos.MultiFileTextTagPair;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;


@SuppressWarnings("rawtypes")
public class TagWindow extends Stage {
	private static transient final Logger LOGGER = Logger.getLogger( TagWindow.class.getName() );
	
	List<Track> tracks;
	List<Album> albums;
	 
	List <FieldKey> supportedTextTags = Arrays.asList (
		FieldKey.ARTIST, FieldKey.ALBUM_ARTIST, FieldKey.TITLE, FieldKey.ALBUM,
		FieldKey.YEAR, FieldKey.ORIGINAL_YEAR, FieldKey.TRACK, FieldKey.DISC_SUBTITLE, FieldKey.DISC_NO,
		FieldKey.DISC_TOTAL, FieldKey.MUSICBRAINZ_RELEASE_TYPE
		//TODO: Think about TITLE_SORT, ALBUM_SORT, ORIGINAL_YEAR -- we read them. 
	);
	
	List <FieldKey> hiddenTextTagsList = Arrays.asList( );
	
	final ObservableList <MultiFileTextTagPair> textTagPairs = FXCollections.observableArrayList();
	final ObservableList <MultiFileImageTagPair> imageTagPairs = FXCollections.observableArrayList();
	
	HBox controlPanel = new HBox();
	
	TableColumn textTagColumn;
	TableColumn textValueColumn;
	
	TableColumn imageTagColumn;
	TableColumn imageValueColumn;
	TableColumn imageDeleteColumn;
	
	FXUI ui;
	
	@SuppressWarnings({ "unchecked" })
	public TagWindow( FXUI ui ) {
		super();
		this.ui = ui;
		this.initModality( Modality.NONE );
		this.initOwner( ui.getMainStage() );
		this.setTitle( "Tag Editor" );
		this.setWidth( 600 );
		this.setHeight ( 500 );
		Pane root = new Pane();
		Scene scene = new Scene( root );
		
		VBox textTagPane = new VBox();
		
		textTagColumn = new TableColumn( "Tag" );
		textValueColumn = new TableColumn( "Value" );
		
		textTagColumn.setStyle( "-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-padding: 0 10 0 0; ");
		
		textTagColumn.setMaxWidth( 350000 );
		textValueColumn.setMaxWidth( 650000 );
		textValueColumn.setEditable( true );
		
		textTagColumn.setCellValueFactory( new PropertyValueFactory <MultiFileTextTagPair, String>( "TagName" ) );
		textValueColumn.setCellValueFactory( new PropertyValueFactory <MultiFileTextTagPair, String>( "Value" ) );
		
		textValueColumn.setCellFactory( TextFieldTableCell.forTableColumn() );
		textValueColumn.setOnEditCommit( new EventHandler <CellEditEvent <MultiFileTextTagPair, String>>() {
			@Override
			public void handle ( CellEditEvent <MultiFileTextTagPair, String> t ) {
				((MultiFileTextTagPair) t.getTableView().getItems().get( t.getTablePosition().getRow() )).setValue( t.getNewValue() );
			}
		} );
		
		textTagColumn.setSortable( false );
		textValueColumn.setSortable( false );
		
		TableView <MultiFileTextTagPair> textTagTable = new TableView<MultiFileTextTagPair> ();
		textTagTable.setItems ( textTagPairs );
		textTagTable.getColumns().addAll( textTagColumn, textValueColumn );
		textTagTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		textTagTable.setEditable( true );
		
		textTagPane.getChildren().addAll( textTagTable );
		textTagTable.prefHeightProperty().bind( textTagPane.heightProperty() );
		textTagTable.prefWidthProperty().bind( textTagPane.widthProperty() );
		
		
		
		
		
		VBox imageTagPane = new VBox();
		
		imageTagColumn = new TableColumn( "Tag" );
		imageValueColumn = new TableColumn( "Image" );
		imageDeleteColumn = new TableColumn ( "Delete" );
		
		imageTagColumn.setStyle( "-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-padding: 0 10 0 0; ");
		imageValueColumn.setStyle( "-fx-alignment: CENTER;");
		
		imageTagColumn.setMaxWidth( 300000 );
		imageValueColumn.setMaxWidth( 450000 );
		imageDeleteColumn.setMaxWidth( 250000 );
		
		imageTagColumn.setCellValueFactory( new PropertyValueFactory <MultiFileTextTagPair, String>( "TagName" ) );
		imageValueColumn.setCellValueFactory( new PropertyValueFactory <MultiFileTextTagPair, byte[]>( "ImageData" ) );
		
		imageTagColumn.setSortable( false );
		imageValueColumn.setSortable( false );
		imageDeleteColumn.setSortable ( false );
		
		imageValueColumn.setCellFactory( new Callback <TableColumn <MultiFileTextTagPair, byte[]>, TableCell <MultiFileTextTagPair, byte[]>> () {
			@Override
			public TableCell <MultiFileTextTagPair, byte[]> call ( TableColumn <MultiFileTextTagPair, byte[]> param ) {
				TableCell <MultiFileTextTagPair, byte[]> cell = new TableCell <MultiFileTextTagPair, byte[]>() {
					@Override
					public void updateItem ( byte[] imageData, boolean empty ) {
						this.setWrapText( true );
						this.setTextAlignment( TextAlignment.CENTER );
						if ( imageData == MultiFileImageTagPair.MULTI_VALUE ) {
							setGraphic( null );
							setText ( "Multiple Values" );
							
						} else if ( imageData != null ) {
							ImageView imageview = new ImageView( new Image( new ByteArrayInputStream( imageData ) ) );
							imageview.setFitHeight( 80 );
							imageview.setFitWidth( 80 );
							setGraphic( imageview );
							setText ( null );
						} else if ( !empty ) {
							setGraphic ( null );
							setText ( "No images of this type are embedded in this tag. Hypnos may be reading images from the album or artist folder." );
						} else {
							setGraphic ( null );
							setText ( null );
						}
							
					}
				};
				return cell;
			}
		});
		
		Callback <TableColumn <MultiFileTextTagPair, Void>, TableCell <MultiFileTextTagPair, Void>> deleteCellFactory =
			new Callback <TableColumn <MultiFileTextTagPair, Void>, TableCell <MultiFileTextTagPair, Void>>() {
				@Override
				public TableCell call ( final TableColumn <MultiFileTextTagPair, Void> param ) {
					final TableCell <MultiFileTextTagPair, Void> cell = new TableCell <MultiFileTextTagPair, Void>() {

						final Button changeButton = new Button( "Set" );
						final Button exportButton = new Button( "Export" );
						final Button deleteButton = new Button( "Delete" );
						final VBox box = new VBox();
						
						{
							box.getChildren().addAll( changeButton, exportButton, deleteButton );
							setGraphic( box );
							box.setAlignment( Pos.CENTER );
							box.setStyle("-fx-background-color: rgba(0, 0, 0, 0);");
							box.setPadding( new Insets ( 10, 0, 10, 0 ) );

							changeButton.setMinWidth ( 100 );
							deleteButton.setMinWidth ( 100 );
							exportButton.setMinWidth ( 100 );
							
							changeButton.setOnAction ( event -> {
								FileChooser fileChooser = new FileChooser(); 
								FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter( 
									"Image Files", Arrays.asList( "*.jpg", "*.jpeg", "*.png" ) );
								
								fileChooser.getExtensionFilters().add( fileExtensions );
								fileChooser.setTitle( "Set Image" );
								File targetFile = fileChooser.showOpenDialog( ui.mainStage );
								
								if ( targetFile == null ) return; 
								
								try {
									byte[] imageBuffer = Files.readAllBytes( targetFile.toPath() );
									((MultiFileImageTagPair)this.getTableRow().getItem()).setImageData( imageBuffer );
									this.getTableRow().getTableView().refresh();
								} catch ( IOException e ) {
									//TODO: LOGGING
								}
							} );
							
							exportButton.setOnAction ( event -> {
								byte[] data = ((MultiFileImageTagPair)this.getTableRow().getItem()).getImageData();
								
								if ( data != null ) {
									FileChooser fileChooser = new FileChooser();
									FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter( 
										"Image Files", Arrays.asList( "*.png" ) );
									
									fileChooser.getExtensionFilters().add( fileExtensions );
									fileChooser.setTitle( "Export Image" );
									fileChooser.setInitialFileName( "image.png" );
									File targetFile = fileChooser.showSaveDialog( ui.mainStage );
									
									if ( targetFile == null ) return; 
	
									if ( !targetFile.toString().toLowerCase().endsWith(".png") ) {
										targetFile = targetFile.toPath().resolveSibling ( targetFile.getName() + ".png" ).toFile();
									}
									
									try {
										BufferedImage bImage = ImageIO.read( new ByteArrayInputStream ( data ) );
										ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
										ImageIO.write( bImage, "png", byteStream );
										byte[] imageBytes  = byteStream.toByteArray();
										byteStream.close();
										
										Utils.saveImageToDisk( targetFile.toPath(), imageBytes );
									} catch ( IOException ex ) {
										FXUI.notifyUserError ( ex.getClass().getCanonicalName() + ": Unable to export image. See log for more information." );
										LOGGER.log( Level.WARNING, "Unable to export image.", ex );
									}
								}
								
							} );

							deleteButton.setOnAction( event -> {
								((MultiFileImageTagPair)this.getTableRow().getItem()).setImageData( null );
								this.getTableRow().getTableView().refresh();
							} );
						}
						
						@Override
						public void updateItem ( Void item, boolean empty ) {
							super.updateItem( item, empty );
							if ( empty ) {
								setGraphic( null );
								setText( null );
								
							} else {
								TableRow row = this.getTableRow();
								if ( row != null ) {
									MultiFileImageTagPair pair = (MultiFileImageTagPair)this.getTableRow().getItem();
									if ( pair != null ) {
										deleteButton.setDisable( pair.getImageData() == null );
										exportButton.setDisable( pair.getImageData() == null );
										

										if ( pair.getImageData() == MultiFileImageTagPair.MULTI_VALUE ) {
											changeButton.setText ( "Change" );
											exportButton.setDisable( true );
											
										} else if ( pair.getImageData() == null ) {
											changeButton.setText ( "Set" );
										} else {
											changeButton.setText ( "Change" );
										}
									}
								}
								setGraphic( box );
								setText( null );
							}
						}
					};
					
					return cell;
				}
			};

		imageDeleteColumn.setCellFactory( deleteCellFactory );
		
		
		TableView <MultiFileImageTagPair> imageTagTable = new TableView<MultiFileImageTagPair> ();
		imageTagTable.setItems ( imageTagPairs );
		imageTagTable.getColumns().addAll( imageTagColumn, imageValueColumn, imageDeleteColumn );
		imageTagTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		
		imageTagTable.prefWidthProperty().bind( imageTagPane.widthProperty() );
		
		imageTagPane.getChildren().add( imageTagTable );
		imageTagTable.prefHeightProperty().bind( imageTagPane.heightProperty() );
		imageTagTable.prefWidthProperty().bind( imageTagPane.widthProperty() );
		
		
		
		setupControlPanel();

		
		TabPane tabPane = new TabPane();
		Tab textTab = new Tab( "Tags" );
		Tab imagesTab = new Tab( "Images" );
		tabPane.getTabs().addAll( textTab, imagesTab );
		tabPane.setTabMinWidth( 100 );
		
		textTab.setClosable( false );
		imagesTab.setClosable ( false );
		
		textTab.setContent( textTagPane );
		textTagPane.prefWidthProperty().bind( tabPane.widthProperty() );
		textTagPane.prefHeightProperty().bind( tabPane.heightProperty() );
		
		imagesTab.setContent( imageTagPane );
		imageTagPane.prefWidthProperty().bind( tabPane.widthProperty() );
		imageTagPane.prefHeightProperty().bind( tabPane.heightProperty() );
		
		VBox primaryPane = new VBox();
		
		primaryPane.getChildren().addAll( tabPane, controlPanel );
		tabPane.prefWidthProperty().bind( primaryPane.widthProperty() );
		tabPane.prefHeightProperty().bind( primaryPane.heightProperty().subtract( controlPanel.heightProperty() ) );
		
		root.getChildren().add ( primaryPane );
		primaryPane.prefWidthProperty().bind( root.widthProperty() );
		primaryPane.prefHeightProperty().bind ( root.heightProperty() );
		
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
				setTracks ( tracks, albums, (FieldKey[])hiddenTextTagsList.toArray() );
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
					track.updateTagsAndSave( textTagPairs, imageTagPairs, ui.player );
					ui.library.addTrack( track ); //TODO: This just causes the track to be updated, probably should rename the function
				}
			}
			
			//TODO: it would be nice to get rid of these functions by using observable items. 
			ui.refreshAlbumTable();
			ui.refreshTrackTable();
			ui.refreshCurrentList();
			ui.refreshQueueList();
			ui.refreshHistory();
			ui.refreshImages();
			
		});
		saverThread.setDaemon( true );
		saverThread.start();
	}
	
	public void setTracks ( List <Track> tracks, List <Album> albumsToRefresh, FieldKey ... hiddenTags ) { 
		textTagPairs.clear();
		imageTagPairs.clear();
		this.hiddenTextTagsList = Arrays.asList( hiddenTags );
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
			
			for ( FieldKey key : supportedTextTags ) {
				
				if ( hiddenTextTagsList.contains( key ) ) continue;
				
				try {
					String value = firstTag.getFirst( key );
					textTagPairs.add( new MultiFileTextTagPair ( key, value ) );
				} catch ( KeyNotFoundException e ) {
					textTagPairs.add( new MultiFileTextTagPair ( key, "" ) );
				}
			}
			
			List<Artwork> firstArtworkList = firstTag.getArtworkList();
			
			for ( ImageFieldKey key : ImageFieldKey.values() ) {
				boolean added = false;
				for ( Artwork artwork : firstArtworkList ) {
					ImageFieldKey artworkKey = ImageFieldKey.getKeyFromTagIndex( artwork.getPictureType() );
					
					if ( artworkKey == key ) {
						imageTagPairs.add ( new MultiFileImageTagPair ( key, artwork.getBinaryData() ) );
						added = true;
					}
				}
				
				if ( !added ) {
					imageTagPairs.add ( new MultiFileImageTagPair ( key, null ) );
				}
			}
			
			if ( tracks.size() > 1 ) {
				for ( int k = 1 ; k < tracks.size() ; k++ ) {
					
					Track track = tracks.get ( k );
					
					AudioFile audioFile = AudioFileIO.read( track.getPath().toFile() );
					
					Tag tag = audioFile.getTag();
					
					if ( tag == null ) {
						tag = audioFile.createDefaultTag();
					}
					
					for ( MultiFileTextTagPair tagPair : textTagPairs ) {
						FieldKey key = tagPair.getKey();
	
						if ( hiddenTextTagsList.contains( key ) ) continue;
						
						try {
							String value = tag.getFirst( key );
							tagPair.anotherFileValue( value );
						} catch ( KeyNotFoundException e ) {
							tagPair.anotherFileValue( null );
						}
					}
					
					List<Artwork> artworkList = tag.getArtworkList();
					
					for ( MultiFileImageTagPair tagPair : imageTagPairs ) {
						boolean added = false;
						for ( Artwork artwork : artworkList ) {
							ImageFieldKey artworkKey = ImageFieldKey.getKeyFromTagIndex( artwork.getPictureType() );
							
							if ( artworkKey == tagPair.getKey() ) {
								tagPair.anotherFileValue( artwork.getBinaryData() );
							}
						}
					}
				}
			}
			
			List <MultiFileImageTagPair> hideMe = new ArrayList <MultiFileImageTagPair> ();
			
			for ( MultiFileImageTagPair tagPair : imageTagPairs ) {
				
				if ( tagPair.getKey() == ImageFieldKey.ALBUM_FRONT || tagPair.getKey() == ImageFieldKey.ARTIST ) continue;
				
				
				if ( tagPair.getImageData() == null ) {
					hideMe.add ( tagPair );
				}
			}
			
			imageTagPairs.removeAll( hideMe );
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to set data for tag window.", e );
		}
	}
}

