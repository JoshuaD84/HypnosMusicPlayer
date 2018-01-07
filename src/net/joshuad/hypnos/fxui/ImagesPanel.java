package net.joshuad.hypnos.fxui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import net.joshuad.hypnos.Album;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.Track.ArtistTagImagePriority;
import net.joshuad.hypnos.audio.AudioSystem;

public class ImagesPanel extends SplitPane {
	private static final Logger LOGGER = Logger.getLogger( ImagesPanel.class.getName() );

	ResizableImageView albumImage;
	ResizableImageView artistImage;

	BorderPane albumImagePane;
	BorderPane artistImagePane;
	
	FXUI ui;
	AudioSystem audioSystem;
	
	public ImagesPanel( FXUI ui, AudioSystem audioSystem ) {
		this.ui = ui;
		this.audioSystem = audioSystem;
		setupAlbumImage();
		setupArtistImage();

		getItems().addAll( albumImagePane, artistImagePane );
		
		startImageLoaderThread();
	}
	
	public void setupAlbumImage () {
		
		ContextMenu menu = new ContextMenu();
		
		MenuItem setImage = new MenuItem ( "Set Album Image" );
		MenuItem exportImage = new MenuItem ( "Export Image" );

		Label dragAndDropLabel = new Label ( "Drop Album Image Here" );
		dragAndDropLabel.getStyleClass().add( "dragAndDropLabel" );
		
		menu.getItems().addAll( setImage, exportImage );

		setImage.setOnAction( ( ActionEvent event ) -> {
				
			Track track = ui.currentImagesTrack;
			if ( track == null ) return;
			
			FileChooser fileChooser = new FileChooser(); 
			FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter( 
				"Image Files", Arrays.asList( "*.jpg", "*.jpeg", "*.png" ) );
			
			fileChooser.getExtensionFilters().add( fileExtensions );
			fileChooser.setTitle( "Set Album Image" );
			File targetFile = fileChooser.showOpenDialog( ui.getMainStage() );
			
			if ( targetFile == null ) return; 
			
			track.setAndSaveAlbumImage ( targetFile.toPath(), audioSystem );

			setImages ( ui.currentImagesTrack );
		});
			
		exportImage.setOnAction( ( ActionEvent event ) -> {
			Track track = ui.currentImagesTrack;
			if ( track == null ) return;
			
			FileChooser fileChooser = new FileChooser();
			FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter( 
				"Image Files", Arrays.asList( "*.png" ) );
			
			fileChooser.getExtensionFilters().add( fileExtensions );
			fileChooser.setTitle( "Export Album Image" );
			fileChooser.setInitialFileName( "album.png" );
			File targetFile = fileChooser.showSaveDialog( ui.getMainStage() );
			
			if ( targetFile == null ) return; 

			if ( !targetFile.toString().toLowerCase().endsWith(".png") ) {
				targetFile = targetFile.toPath().resolveSibling ( targetFile.getName() + ".png" ).toFile();
			}
			
			try {
				BufferedImage bImage = SwingFXUtils.fromFXImage( albumImage.getImage(), null );
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				ImageIO.write( bImage, "png", byteStream );
				byte[] imageBytes  = byteStream.toByteArray();
				byteStream.close();
				
				Utils.saveImageToDisk( targetFile.toPath(), imageBytes );
			} catch ( IOException ex ) {
				ui.notifyUserError ( "Unable to export image. See log for more information." );
				LOGGER.log( Level.WARNING, "Unable to export album image.", ex );
			}
		});
				
		albumImagePane = new BorderPane();
		albumImagePane.setMinWidth( 0 );

		albumImagePane.getStyleClass().add( "artpane" );
		
		albumImagePane.setOnContextMenuRequested( ( ContextMenuEvent e ) -> {
			boolean disableMenus = ui.currentImagesTrack == null;
			setImage.setDisable( disableMenus );
			exportImage.setDisable( disableMenus );
			
			menu.show( albumImagePane, e.getScreenX(), e.getScreenY() );
		});
		
		albumImagePane.addEventHandler( MouseEvent.MOUSE_PRESSED, ( MouseEvent e ) -> {
			menu.hide();
		});
		
		albumImagePane.setOnDragOver( event -> {
			//Dragboard db = event.getDragboard();
			//TODO: be discerning of what we accept
			event.acceptTransferModes( TransferMode.COPY );
			event.consume();
		});
		
		albumImagePane.setOnDragEntered ( event -> {
			albumImagePane.getChildren().clear( );
			albumImagePane.setCenter( dragAndDropLabel );
		});
		
		albumImagePane.setOnDragExited( event -> {
			albumImagePane.getChildren().clear( );
			albumImagePane.setCenter( albumImage );
		});
		
		albumImagePane.setOnDragDone( event -> {
			albumImagePane.getChildren().clear( );
			albumImagePane.setCenter( albumImage );
		});
		
		albumImagePane.setOnDragDropped( event -> {
			
			Track track = ui.currentImagesTrack;
			
			if ( track == null ) return;
			
			Dragboard db = event.getDragboard();
			
			if ( db.hasFiles() ) {
				List <File> files = db.getFiles();
				event.setDropCompleted( true );
				event.consume();
				
				for ( File file : files ) {
					if ( Utils.isImageFile( file ) ) {
						try {
							track.setAndSaveAlbumImage ( file.toPath(), audioSystem );
							break;
						} catch ( Exception e ) {
							LOGGER.log( Level.WARNING, "Unable to set album image from file: " + file.toString(), e );
						}
					}
				}
				
				setImages ( ui.currentImagesTrack );
		
			} else {
				for ( DataFormat contentType : db.getContentTypes() ) {

					try {
						if ( contentType == DataFormat.lookupMimeType("application/octet-stream" ) ) {
							ByteBuffer buffer = (ByteBuffer)db.getContent( contentType );
							track.setAndSaveAlbumImage( buffer.array(), audioSystem );
						}
					} catch ( Exception e ) {
						LOGGER.log( Level.WARNING, "Unable to set album image from drop source.", e );
					}
				}
				
				event.setDropCompleted( true );
				event.consume();
			}
		});
	}
	
	private Track requestedTrack;
	private Album requestedAlbum;
	
	private void startImageLoaderThread() {
		Thread imageLoader = new Thread() {
			public void run() {
				while ( true ) {
					
					if ( requestedTrack != null ) {
						Track loadMeTrack = requestedTrack;
						Album loadMeAlbum = requestedAlbum;
						requestedTrack = null;
						requestedAlbum = null;
						
						Image albumImage = loadMeTrack.getAlbumCoverImage();
						Image artistImage = loadMeTrack.getAlbumArtistImage();
						
						SimpleBooleanProperty imagesDisplayed = new SimpleBooleanProperty ( false );
						Platform.runLater( () -> {
							setAlbumImage( albumImage );
							setArtistImage( artistImage );
							ui.currentImagesTrack = loadMeTrack;
							ui.currentImagesAlbum = loadMeAlbum;
							imagesDisplayed.setValue( true );
						});
						
						while ( !imagesDisplayed.getValue() ) {
							try { Thread.sleep ( 10 ); } catch ( InterruptedException e ) {}
						}
					}

					try { Thread.sleep ( 100 ); } catch ( InterruptedException e ) {}
				}
			}
		};
		
		imageLoader.setName ( "Image Panel Loader" );
		imageLoader.setDaemon( true );
		imageLoader.start();
	}
	
	public void setupArtistImage () {
		
		ContextMenu menu = new ContextMenu();
		
		MenuItem setArtistImage = new MenuItem ( "Set Artist Image" );
		MenuItem setAlbumArtistImage = new MenuItem ( "Set Artist Image for this Album" );
		MenuItem setTrackArtistImage = new MenuItem ( "Set Artist Image for this Track" );
		MenuItem exportImage = new MenuItem ( "Export Image" );
		
		menu.getItems().addAll( setArtistImage, setAlbumArtistImage, setTrackArtistImage, exportImage );
		
		artistImagePane = new BorderPane();	
		artistImagePane.setMinWidth( 0 );
		
		artistImagePane.getStyleClass().add( "artpane" );
		Label dragAndDropLabel = new Label ( "Drop Artist Image Here" );
		dragAndDropLabel.getStyleClass().add( "dragAndDropLabel" );
		
		artistImagePane.setOnContextMenuRequested( ( ContextMenuEvent e ) -> {
			boolean disableAllMenus = false;
			boolean disableAlbum = true;
			boolean disableArtist = true;
			
			if ( ui.currentImagesTrack == null ) {
				disableAllMenus = true;
				
			} else if ( ui.currentImagesTrack.hasAlbumDirectory() ) {
				disableAlbum = false;
				
				if ( Utils.isArtistDirectory( ui.currentImagesTrack.getAlbumPath().getParent() ) ) {
					disableArtist = false;
				}
			}
			
			setTrackArtistImage.setDisable( disableAllMenus );
			setAlbumArtistImage.setDisable( disableAllMenus || disableAlbum );
			setArtistImage.setDisable( disableAllMenus || disableArtist );
			exportImage.setDisable( disableAllMenus );
			
			menu.show( artistImagePane, e.getScreenX(), e.getScreenY() );
		});

		exportImage.setOnAction( ( ActionEvent event ) -> {
			Track track = ui.currentImagesTrack;
			if ( track == null ) return;
			
			FileChooser fileChooser = new FileChooser();
			FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter( 
				"Image Files", Arrays.asList( "*.png" ) );
			
			fileChooser.getExtensionFilters().add( fileExtensions );
			fileChooser.setTitle( "Export Artist Image" );
			fileChooser.setInitialFileName( "artist.png" );
			File targetFile = fileChooser.showSaveDialog( ui.getMainStage() );
			
			if ( targetFile == null ) return; 

			if ( !targetFile.toString().toLowerCase().endsWith(".png") ) {
				targetFile = targetFile.toPath().resolveSibling ( targetFile.getName() + ".png" ).toFile();
			}
			
			try {
				BufferedImage bImage = SwingFXUtils.fromFXImage( artistImage.getImage(), null );
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				ImageIO.write( bImage, "png", byteStream );
				byte[] imageBytes  = byteStream.toByteArray();
				byteStream.close();
				
				Utils.saveImageToDisk( targetFile.toPath(), imageBytes );
			} catch ( IOException ex ) {
				ui.notifyUserError ( ex.getClass().getCanonicalName() + ": Unable to export image. See log for more information." );
				LOGGER.log( Level.WARNING, "Unable to export artist image.", ex );
			}
		});
		
		FileChooser fileChooser = new FileChooser();
		FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter( 
			"Image Files", Arrays.asList( "*.jpg", "*.jpeg", "*.png" ) );
		
		fileChooser.getExtensionFilters().add( fileExtensions );
		fileChooser.setTitle( "Set Artist Image" );
		
		setTrackArtistImage.setOnAction( ( ActionEvent e ) -> {
			Track track = ui.currentImagesTrack;
			if ( track == null ) return;
			
			File imageFile = fileChooser.showOpenDialog( ui.getMainStage() );
			if ( imageFile == null ) return; 
			
			Track.saveArtistImageToTag ( track.getPath().toFile(), imageFile.toPath(), ArtistTagImagePriority.TRACK, false, audioSystem );
			setImages ( ui.currentImagesTrack );
		});
		
		setAlbumArtistImage.setOnAction( ( ActionEvent e ) -> {
			Track track = ui.currentImagesTrack;
			if ( track == null ) return;
			
			File imageFile = fileChooser.showOpenDialog( ui.getMainStage() );
			if ( imageFile == null ) return; 
			
			try {
				byte[] buffer = Files.readAllBytes( imageFile.toPath() );
				
				//REFACTOR: put this code in a function, it's duplciated below. 
				
				if ( !track.hasAlbumDirectory() ) return;
				
				Path albumPath = track.getAlbumPath();
			
				Utils.saveImageToDisk( albumPath.resolve( "artist.png" ), buffer );
				setImages ( ui.currentImagesTrack );
				Thread workerThread = new Thread ( () -> {
					try ( DirectoryStream <Path> stream = Files.newDirectoryStream( albumPath ) ) {
						for ( Path child : stream ) {
							if ( Utils.isMusicFile( child ) ) {
								Track.saveArtistImageToTag ( child.toFile(), buffer, ArtistTagImagePriority.ALBUM, false, audioSystem );
							}
						}
					} catch ( IOException e3 ) {
						LOGGER.log( Level.WARNING, "Unable to get directory listing, artist tags not updated for album: " + albumPath, e3 );
					}
	
					Platform.runLater( () -> setImages ( ui.currentImagesTrack ) );
				});
				workerThread.setName ( "Album Artist Image Tag Saver" );
				workerThread.setDaemon( false );
				workerThread.start();
			} catch ( Exception e2 ) {
				LOGGER.log( Level.WARNING, "Unable to load image data from file: " + imageFile, e2 );
			}
			
		});
		
		setArtistImage.setOnAction( ( ActionEvent e ) -> {
			Track track = ui.currentImagesTrack;
			if ( track == null ) return;
			
			File imageFile = fileChooser.showOpenDialog( ui.getMainStage() );
			if ( imageFile == null ) return; 
			
			try {
				byte[] buffer = Files.readAllBytes( imageFile.toPath() );
				
				//REFACTOR: put this code in a function, it's duplicated below. 
				
				if ( !Utils.isArtistDirectory( ui.currentImagesTrack.getAlbumPath().getParent() ) ) return;
				
				Path artistPath = track.getAlbumPath().getParent();
			
				Utils.saveImageToDisk( artistPath.resolve( "artist.png" ), buffer );
				setImages ( ui.currentImagesTrack );
				Thread workerThread = new Thread ( () -> {
					try ( DirectoryStream <Path> stream = Files.newDirectoryStream( artistPath ) ) {
						for ( Path child : stream ) {
							if ( Utils.isMusicFile( child ) ) {
								Track.saveArtistImageToTag ( child.toFile(), buffer, ArtistTagImagePriority.GENERAL, false, audioSystem );
							}
						}
					} catch ( IOException e3 ) {
						LOGGER.log( Level.WARNING, "Unable to get directory listing, artist tags not updated for album: " + artistPath, e3 );
					}
	
					Platform.runLater( () -> setImages ( ui.currentImagesTrack ) );
				});
				workerThread.setName ( "Album Artist Image Tag Saver" );
				workerThread.setDaemon( false );
				workerThread.start();
			} catch ( Exception e2 ) {
				LOGGER.log( Level.WARNING, "Unable to load image data from file: " + imageFile, e2 );
			}
			
		});
		
		artistImagePane.addEventHandler( MouseEvent.MOUSE_PRESSED, ( MouseEvent e ) -> {
			menu.hide();
		});
		
		artistImagePane.setOnDragOver( event -> {
			//Dragboard db = event.getDragboard();
			//TODO: be discerning of what we accept
			event.acceptTransferModes( TransferMode.COPY );
			event.consume();
		});
		
		artistImagePane.setOnDragEntered ( event -> {
			artistImagePane.getChildren().clear( );
			artistImagePane.setCenter( dragAndDropLabel );
		});
		
		artistImagePane.setOnDragExited( event -> {
			artistImagePane.getChildren().clear( );
			artistImagePane.setCenter( artistImage );
		});
		
		artistImagePane.setOnDragDone( event -> {
			artistImagePane.getChildren().clear( );
			artistImagePane.setCenter( artistImage );
		});
		
		artistImagePane.setOnDragDropped( event -> {
			
			Track track = ui.currentImagesTrack;
			
			if ( track == null ) return;
			
			Dragboard db = event.getDragboard();
			
			if ( db.hasFiles() ) {
				List <File> files = db.getFiles();
				
				for ( File file : files ) {
					if ( Utils.isImageFile( file ) ) {
						try {
							byte[] buffer = Files.readAllBytes( file.toPath() );
							promptAndSaveArtistImage ( buffer );
							break;
						} catch ( Exception e ) {
							LOGGER.log( Level.WARNING, "Unable to set artist image from file: " + file.toString(), e );
						}
					}
				}
		
			} else {
				for ( DataFormat contentType : db.getContentTypes() ) {
					try {
						if ( contentType == DataFormat.lookupMimeType("application/octet-stream" ) ) {
							
							ByteBuffer buffer = (ByteBuffer)db.getContent( contentType );
							promptAndSaveArtistImage ( buffer.array() );
						} 
					} catch ( Exception e ) {
						LOGGER.log( Level.WARNING, "Unable to set artist image from drop source.", e );
					}
				}
			}
			
			event.setDropCompleted( true );
			event.consume();		
		});
	}
	

	
	private void promptAndSaveArtistImage ( byte[] buffer ) {
		Track targetTrack = ui.currentImagesTrack;
		
		if ( targetTrack != null ) {
			
			Path albumPath = targetTrack.getAlbumPath();
			Path artistPath = null;
			
			if ( albumPath != null && Utils.isArtistDirectory( albumPath.getParent() ) ) {
				artistPath = albumPath.getParent();
			}
			
			List <ArtistImageSaveDialog.Choice> choices = new ArrayList<>();
			
			if ( artistPath != null ) {
				choices.add ( ArtistImageSaveDialog.Choice.ALL );
			}
			
			if ( albumPath != null ) {
				choices.add ( ArtistImageSaveDialog.Choice.ALBUM );
			} 
			
			if ( ui.currentImagesAlbum == null ) {
				choices.add ( ArtistImageSaveDialog.Choice.TRACK );
			} 
			
			ArtistImageSaveDialog prompt = new ArtistImageSaveDialog ( ui.getMainStage(), choices );
			
			//TODO: Make this part of ArtistImageSaveDialog
			String darkSheet = ui.fileToStylesheetString( ui.darkStylesheet );
			if ( darkSheet == null ) {
				LOGGER.log( Level.INFO, "Unable to load dark style sheet, alert will not look right." + 
						ui.darkStylesheet.toString()
				);
			} else {
				if ( ui.isDarkTheme() ) {
					prompt.getScene().getStylesheets().add( darkSheet );
				} else {
					prompt.getScene().getStylesheets().remove( darkSheet );
				}
			}
			
			prompt.showAndWait();
			
			ArtistImageSaveDialog.Choice choice = prompt.getSelectedChoice();
			boolean overwriteAll = prompt.getOverwriteAllSelected();
	
			switch ( choice ) {
				
				case CANCEL:
					break;
				
				case ALL:
					Utils.saveImageToDisk( artistPath.resolve( "artist.png" ), buffer );
					setImages ( ui.currentImagesTrack );
					//PENDING: What about ID3 tags? Set them only if they're not already set?  
					break;
					
				case ALBUM:
					
					//REFACTOR: put this code in a function, it's duplicated above. 
					Utils.saveImageToDisk( albumPath.resolve( "artist.png" ), buffer );
					setImages ( ui.currentImagesTrack );
					Thread workerThread = new Thread ( () -> {
						try ( DirectoryStream <Path> stream = Files.newDirectoryStream( albumPath ) ) {
							for ( Path child : stream ) {
								if ( Utils.isMusicFile( child ) ) {
									Track.saveArtistImageToTag ( child.toFile(), buffer, ArtistTagImagePriority.ALBUM, overwriteAll, audioSystem );
								}
							}
						} catch ( IOException e ) {
							LOGGER.log( Level.WARNING, "Unable to list files in directory, artist tags not updated for album: " + albumPath, e );
						}

						Platform.runLater( () -> setImages ( ui.currentImagesTrack ) );
					});

					workerThread.setName ( "Artist Image Tag Saver" );
					workerThread.setDaemon( false );
					workerThread.start();
					break;
					
				case TRACK:
					Track.saveArtistImageToTag ( targetTrack.getPath().toFile(), buffer, ArtistTagImagePriority.TRACK, overwriteAll, audioSystem );
					setImages ( ui.currentImagesTrack );
					break;
					
			}
		}
	}
	
	public void setImages ( Track track ) {
		setImages ( track, null );
	}
	
	
	public void setImages ( Album album ) {
		setImages ( album.getTracks().get( 0 ), album );
	}
	
	public void setImages ( Track track, Album album ) {
		
		if ( track != null && Files.exists( track.getPath() ) ) {
			requestedTrack = track;
			requestedAlbum = album;
			
		} else if ( track == null && ui.currentImagesTrack != null ) {
			setImages ( ui.currentImagesTrack );
			
		} else {
			ui.currentImagesTrack = null;
			ui.currentImagesAlbum = null;
			setAlbumImage ( null );
			setArtistImage ( null );
		}
	}

	public void setAlbumImage ( Image image ) {
		try {
			albumImage = new ResizableImageView( image );
			albumImage.setSmooth(true);
			albumImage.setCache(true);
			albumImage.setPreserveRatio( true );
			albumImagePane.setCenter( albumImage );
		} catch ( Exception e ) {
			albumImagePane.setCenter( null );
		}
	}

	public void setArtistImage ( Image image ) {
		try {
			artistImage = new ResizableImageView( image );
			artistImage.setSmooth(true);
			artistImage.setCache(true);
			artistImage.setPreserveRatio( true );
			artistImagePane.setCenter( artistImage );
		} catch ( Exception e ) {
			artistImagePane.setCenter( null );
		}
	}
	
}
