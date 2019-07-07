package net.joshuad.hypnos.fxui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import net.joshuad.hypnos.CurrentList;
import net.joshuad.hypnos.CurrentListTrack;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.MultiFileImageTagPair;
import net.joshuad.hypnos.MultiFileTextTagPair;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.MultiFileImageTagPair.ImageFieldKey;
import net.joshuad.hypnos.library.Album;
import net.joshuad.hypnos.library.Track;


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
	
	BorderPane controlPanel = new BorderPane();
	
	TableColumn<MultiFileTextTagPair, String> textTagColumn;
	TableColumn<MultiFileTextTagPair, String> textValueColumn;
	
	TableColumn imageTagColumn;
	TableColumn imageValueColumn;
	TableColumn imageDeleteColumn;
	
	Button previousButton;
	Button nextButton;
	
	private TextField locationField;
	private HBox locationBox;
	
	private FXUI ui;
	AudioSystem audioSystem;
	
	public TagWindow( FXUI ui, AudioSystem audioSystem ) {
		super();
		this.ui = ui;
		this.audioSystem = audioSystem;
		this.initModality( Modality.NONE );
		this.initOwner( ui.getMainStage() );
		this.setTitle( "Tag Editor" );
		this.setWidth( 600 );
		this.setHeight ( 500 );
		Pane root = new Pane();
		Scene scene = new Scene( root );
		
		try {
			getIcons().add( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) ) );
		} catch ( FileNotFoundException e ) {
			LOGGER.warning( "Unable to load program icon: resources/icon.png" );
		}
		
		VBox textTagPane = new VBox();
		
		textTagColumn = new TableColumn<>( "Tag" );
		textValueColumn = new TableColumn<>( "Value" );
		
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
		
		textTagColumn.setReorderable( false );
		textValueColumn.setReorderable( false );
		
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
		
		imageTagColumn.setReorderable( false );
		imageValueColumn.setReorderable( false );
		imageDeleteColumn.setReorderable( false );
		
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
							imageview.setFitWidth( 220 );
							imageview.setSmooth( true );
							imageview.setCache( true );
							imageview.setPreserveRatio( true );
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
		
		Callback <TableColumn <MultiFileImageTagPair, Void>, TableCell <MultiFileImageTagPair, Void>> deleteCellFactory =
			new Callback <TableColumn <MultiFileImageTagPair, Void>, TableCell <MultiFileImageTagPair, Void>>() {
				@Override
				public TableCell call ( final TableColumn <MultiFileImageTagPair, Void> param ) {
					final TableCell <MultiFileImageTagPair, Void> cell = new TableCell <MultiFileImageTagPair, Void>() {

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
									LOGGER.warning( "Unable to read image data from file: " + targetFile );
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
										ui.notifyUserError ( ex.getClass().getCanonicalName() + ": Unable to export image. See log for more information." );
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
		
		Label label = new Label ( "Location: " );
		label.setAlignment( Pos.CENTER_RIGHT );
		
		locationField = new TextField();
		locationField.setEditable( false );
		locationField.setMaxWidth( Double.MAX_VALUE );
		
		HBox.setHgrow( locationField, Priority.ALWAYS );
		Button browseButton = new Button( "Browse" );
		browseButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent event ) {
				if ( tracks.size() == 1 ) {
					ui.openFileBrowser( tracks.get( 0 ).getPath() );
				}
			}
		});
		
		locationBox = new HBox();
		locationBox.getChildren().addAll( label, locationField, browseButton );
		label.prefHeightProperty().bind( locationBox.heightProperty() );
		
		VBox primaryPane = new VBox();
		primaryPane.getChildren().addAll( tabPane, locationBox, controlPanel );
		locationBox.prefWidthProperty().bind( primaryPane.widthProperty() );
		tabPane.prefWidthProperty().bind( primaryPane.widthProperty() );
		tabPane.prefHeightProperty().bind( 
			primaryPane.heightProperty()
			.subtract( controlPanel.heightProperty() )
			.subtract( locationBox.heightProperty() ) 
		);
		
		root.getChildren().add ( primaryPane );
		primaryPane.prefWidthProperty().bind( root.widthProperty() );
		primaryPane.prefHeightProperty().bind ( root.heightProperty() );
		
		scene.addEventFilter( KeyEvent.KEY_PRESSED, new EventHandler <KeyEvent>() {
			@Override
			public void handle ( KeyEvent e ) {
				if ( e.getCode() == KeyCode.RIGHT && e.isControlDown() ) {
					nextButton.fire();
					e.consume();
					
				} else if ( e.getCode() == KeyCode.LEFT && e.isControlDown() ) {
					previousButton.fire();
					e.consume();
					
				} else if ( e.getCode() == KeyCode.ESCAPE ) {
					if ( imageTagTable.getEditingCell() == null && textTagTable.getEditingCell() == null ) {
						close();
						e.consume();
					}
				}
			}
		});

		setScene( scene );
	}
	
	public void setupControlPanel() {
		Button saveButton = new Button ( "Save" );
		Button revertButton = new Button ( "Revert" );
		Button cancelButton = new Button ( "Cancel" );
		
		saveButton.setPrefWidth( 80 );
		revertButton.setMinWidth( 80 );
		cancelButton.setMinWidth( 80 );
		
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
		
		previousButton = new Button ( "< Save & Prev" );
		previousButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				saveCurrentTags();
				loadAtOffset ( -1 );
			}
		});
		
		nextButton = new Button ( "Save & Next >" );
		nextButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				saveCurrentTags();
				loadAtOffset ( 1 );
			}
		});
		
		
		
		HBox centerPanel = new HBox();
		centerPanel.getChildren().addAll ( cancelButton, revertButton, saveButton );
		centerPanel.setAlignment( Pos.CENTER );
		
		controlPanel.setCenter ( centerPanel );
		controlPanel.setLeft ( previousButton );
		controlPanel.setRight ( nextButton );
		controlPanel.prefWidthProperty().bind( this.widthProperty() );
		controlPanel.setPadding( new Insets( 5 ) );
	}
	
	private void loadAtOffset ( int offset ) {
		if ( albums != null ) {
			List <Album> albumList = ui.library.getAlbumsSorted();
			if ( albumList == null || albumList.size() == 0 ) return;
			int thisIndex = albumList.indexOf( albums.get ( 0 ) );
			int targetIndex = 0;
			if ( thisIndex != -1 ) targetIndex = ( thisIndex + albumList.size() + offset ) % albumList.size();
			FieldKey[] hidden = hiddenTextTagsList == null ? null : hiddenTextTagsList.toArray( new FieldKey[hiddenTextTagsList.size()] );
			Album nextAlbum = albumList.get( targetIndex );
			if ( nextAlbum != null ) setTracks ( nextAlbum.getTracks(), Arrays.asList( nextAlbum ), hidden ); 
			
		} else if ( tracks != null && tracks.get( 0 ) instanceof CurrentListTrack ) { 
			List <CurrentListTrack> currentList = ui.audioSystem.getCurrentList().getItems();
			if ( currentList == null || currentList.size() == 0 ) return;
			int thisIndex = currentList.indexOf( tracks.get ( 0 ) );
			int targetIndex = 0;
			if ( thisIndex != -1 ) targetIndex = ( thisIndex + currentList.size() + offset ) % currentList.size();
			FieldKey[] hidden = hiddenTextTagsList == null ? null : hiddenTextTagsList.toArray( new FieldKey[hiddenTextTagsList.size()] );
			Track nextTrack = currentList.get( targetIndex );
			if ( nextTrack != null ) setTracks ( Arrays.asList( nextTrack ), albums, hidden ); 
			
		} else { 
			List <Track> trackList = ui.library.getTracksSorted();
			if ( trackList == null || trackList.size() == 0 ) return;
			int thisIndex = trackList.indexOf( tracks.get ( 0 ) );
			int targetIndex = 0;
			if ( thisIndex != -1 ) targetIndex = ( thisIndex + trackList.size() + offset ) % trackList.size();
			FieldKey[] hidden = hiddenTextTagsList == null ? null : hiddenTextTagsList.toArray( new FieldKey[hiddenTextTagsList.size()] );
			Track nextTrack = trackList.get( targetIndex );
			if ( nextTrack != null ) setTracks ( Arrays.asList( nextTrack ), albums, hidden ); 
		}
	}

	private void saveCurrentTags() {
		List <Track> saveMe = tracks;
		List <MultiFileTextTagPair> saveMeTextPairs = new ArrayList <>( textTagPairs );
		List <MultiFileImageTagPair> saveMeImagePairs = new ArrayList <>( imageTagPairs );
		
		Thread saverThread = new Thread( () -> {
			if ( saveMe != null ) {
				for ( Track track : saveMe ) {
					track.updateTagsAndSave( saveMeTextPairs, saveMeImagePairs, ui.audioSystem );
				}
			}
			audioSystem.getCurrentList().setHasUnsavedData(true);
			ui.refreshCurrentList();
			ui.refreshQueueList();
			ui.refreshImages();
		});
		saverThread.setName ( "Tag Window Track Updater" );
		saverThread.setDaemon( true );
		saverThread.start();
	}
	
	public void setTracks ( List <Track> tracks, List <Album> albumsToRefresh, FieldKey ... hiddenTags ) {
		setTracks ( tracks, albumsToRefresh, false, hiddenTags );
	}
	
	public void setTracks ( List <Track> tracks, List <Album> albumsToRefresh, boolean hideCoverArt, FieldKey ... hiddenTags ) { 
		
		List <Track> nonMissing = new ArrayList <Track> ();
		for ( Track track : tracks ) if ( Utils.isMusicFile( track.getPath() ) ) nonMissing.add( track );
		tracks = nonMissing;
		
		textTagPairs.clear();
		imageTagPairs.clear();
		this.hiddenTextTagsList = Arrays.asList( hiddenTags );
		this.tracks = tracks;
		this.albums = albumsToRefresh;
		
		if ( tracks.size() <= 0 ) return;
		
		if ( tracks.size() == 1 ) {
			previousButton.setDisable( false );
			nextButton.setDisable ( false );
			previousButton.setVisible( true );
			nextButton.setVisible( true );
			locationField.setText( tracks.get( 0 ).getPath().toString() );
			locationBox.setVisible( true );
			
		} else if ( albums != null && albums.size() == 1 ) {
			previousButton.setDisable( false );
			nextButton.setDisable ( false );
			previousButton.setVisible( true );
			nextButton.setVisible( true );
			locationField.setText( albums.get( 0 ).getPath().toString() );
			locationBox.setVisible( true );
			
		} else {
			previousButton.setDisable( true );
			nextButton.setDisable ( true );
			previousButton.setVisible( false );
			nextButton.setVisible( false );
			locationBox.setVisible( false );
		}
		
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
				
				if ( tagPair.getKey() == ImageFieldKey.ALBUM_FRONT && hideCoverArt ) {
					hideMe.add ( tagPair );
				}
						
				if ( tagPair.getKey() == ImageFieldKey.ALBUM_FRONT || tagPair.getKey() == ImageFieldKey.ARTIST ) {
					continue;
				}
				
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

