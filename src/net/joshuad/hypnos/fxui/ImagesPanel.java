package net.joshuad.hypnos.fxui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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
import javafx.collections.ListChangeListener;
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
import net.joshuad.library.Album;
import net.joshuad.hypnos.CurrentListTrack;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.HypnosURLS;
import net.joshuad.library.Track;
import net.joshuad.hypnos.Utils;
import net.joshuad.library.Track.ArtistTagImagePriority;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.AudioSystem.RepeatMode;
import net.joshuad.hypnos.audio.AudioSystem.ShuffleMode;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;
import net.joshuad.hypnos.audio.PlayerListener;

public class ImagesPanel extends SplitPane implements PlayerListener {

	static final DataFormat textContentFormat = DataFormat.lookupMimeType( "text/plain" );
	
	private static final Logger LOGGER = Logger.getLogger( ImagesPanel.class.getName() );

	ResizableImageView albumImage;
	ResizableImageView artistImage;

	BorderPane albumImagePane;
	BorderPane artistImagePane;
	
	Track currentImagesTrack = null;
	Album currentImagesAlbum = null;
	
	FXUI ui;
	AudioSystem audioSystem;
	
	public ImagesPanel( FXUI ui, AudioSystem audioSystem ) {
		this.ui = ui;
		this.audioSystem = audioSystem;
		setupAlbumImage();
		setupArtistImage();

		getItems().addAll( albumImagePane, artistImagePane );
		
		startImageLoaderThread();
		
		audioSystem.addPlayerListener ( this );
		
		audioSystem.getCurrentList().getItems().addListener ( 
			new ListChangeListener<CurrentListTrack> () {
				@Override
				public void onChanged ( Change <? extends CurrentListTrack> arg0 ) {
					if ( audioSystem.getCurrentList().getItems().size() == 0 ) {
						currentListCleared();
					}
				}
			}
		);
	}
	
	private void setupAlbumImage () {
		
		ContextMenu menu = new ContextMenu();
		
		MenuItem setImage = new MenuItem ( "Set Album Image" );
		MenuItem exportImage = new MenuItem ( "Export Album Image" );
		MenuItem searchForAlbumImage = new MenuItem ( "Search for Album Image" );

		Label dragAndDropLabel = new Label ( "Drop Album Image Here" );
		dragAndDropLabel.getStyleClass().add( "dragAndDropLabel" );
		
		menu.getItems().addAll( setImage, exportImage, searchForAlbumImage );

		setImage.setOnAction( ( ActionEvent event ) -> {
				
			Track track = currentImagesTrack;
			if ( track == null ) return;
			
			FileChooser fileChooser = new FileChooser(); 
			FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter( 
				"Image Files", Arrays.asList( "*.jpg", "*.jpeg", "*.png" ) );
			
			fileChooser.getExtensionFilters().add( fileExtensions );
			fileChooser.setTitle( "Set Album Image" );
			File targetFile = fileChooser.showOpenDialog( ui.getMainStage() );
			
			if ( targetFile == null ) return; 
			
			track.setAndSaveAlbumImage ( targetFile.toPath(), audioSystem );

			setImages ( currentImagesTrack ); //We set it to current because it might've changed since we assigned Track track. 
		});
		
		searchForAlbumImage.setOnAction( ( ActionEvent event ) -> {
			String artist = null;
			String album = null;
			if ( currentImagesTrack != null ) {
				artist = currentImagesTrack.getArtist();
				album = currentImagesTrack.getAlbumTitle();
			} else if ( currentImagesAlbum != null ) {
				artist = currentImagesAlbum.getAlbumArtist();
				album = currentImagesTrack.getAlbumTitle();
			}
			if ( artist != null && album != null ) {
				artist = artist.replaceAll( "&", "and" );
				album = album.replaceAll( "&", "and" );
				ui.openWebBrowser( HypnosURLS.DDG_IMAGE_SEARCH + artist + "+" + album );
			}
		});
			
		exportImage.setOnAction( ( ActionEvent event ) -> {
			Track track = currentImagesTrack;
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
			boolean disableMenus = ( currentImagesTrack == null );
			setImage.setDisable( disableMenus );
			exportImage.setDisable( disableMenus );
			searchForAlbumImage.setDisable( disableMenus );
			
			menu.show( albumImagePane, e.getScreenX(), e.getScreenY() );
		});
		
		albumImagePane.addEventHandler( MouseEvent.MOUSE_PRESSED, ( MouseEvent e ) -> {
			menu.hide();
		});
		
		albumImagePane.setOnDragOver( event -> {
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

			event.setDropCompleted( true );
			event.consume();
			
			Track track = currentImagesTrack;
			
			if ( track == null ) return;
			
			Dragboard db = event.getDragboard();
			
			if ( db.hasFiles() ) {
				List <File> files = db.getFiles();
				
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
				
				setImages ( currentImagesTrack );
		
			} else {
				byte[] buffer = getImageBytesFromDragboard( db );
				
				if ( buffer != null ) {
					track.setAndSaveAlbumImage( buffer, audioSystem );
				
				} else {
					String url = "";
					if ( db.getContentTypes().contains( textContentFormat ) ) {
						url = (String)db.getContent( textContentFormat ) + "\n\n";
					}
					
					String message = 
						"Cannot save image from dropped source.\n\n" +
						url + 
						"Try saving the image to disk and dragging from disk rather than web.";
					LOGGER.warning( message.replaceAll( "\n\n", " " ) );
					ui.notifyUserError( message );
				}

				setImages ( currentImagesTrack );
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
						Image artistImage = loadMeTrack.getArtistImage();
						
						SimpleBooleanProperty imagesDisplayed = new SimpleBooleanProperty ( false );
						Platform.runLater( () -> {
							setAlbumImage( albumImage );
							setArtistImage( artistImage );
							currentImagesTrack = loadMeTrack;
							currentImagesAlbum = loadMeAlbum;
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
	
	private void setupArtistImage () {
		
		ContextMenu menu = new ContextMenu();
		
		MenuItem setArtistImage = new MenuItem ( "Set Artist Image" );
		MenuItem setAlbumArtistImage = new MenuItem ( "Set Artist Image for this Album" );
		MenuItem setTrackArtistImage = new MenuItem ( "Set Artist Image for this Track" );
		MenuItem exportImage = new MenuItem ( "Export Image" );
		MenuItem searchForArtistImage = new MenuItem ( "Search for Artist Image" );
		
		menu.getItems().addAll( setArtistImage, setAlbumArtistImage, setTrackArtistImage,
			exportImage, searchForArtistImage );
		
		artistImagePane = new BorderPane();	
		artistImagePane.setMinWidth( 0 );
		
		artistImagePane.getStyleClass().add( "artpane" );
		Label dragAndDropLabel = new Label ( "Drop Artist Image Here" );
		dragAndDropLabel.getStyleClass().add( "dragAndDropLabel" );
		
		searchForArtistImage.setOnAction( ( ActionEvent event ) -> {
			String artist = null;
			String year = null;
			if ( currentImagesTrack != null ) {
				artist = currentImagesTrack.getArtist();
				year = currentImagesTrack.getYear();
			} else if ( currentImagesAlbum != null ) {
				artist = currentImagesAlbum.getAlbumArtist();
				year = currentImagesTrack.getYear();
			}
			if ( artist != null && year != null ) {
				artist = artist.replaceAll( "&", "and" );
				year = year.replaceAll( "&", "and" );
				ui.openWebBrowser( HypnosURLS.DDG_IMAGE_SEARCH + artist + "+" + year );
			}
		});
		
		artistImagePane.setOnContextMenuRequested( ( ContextMenuEvent e ) -> {
			boolean disableAllMenus = false;
			boolean disableAlbum = true;
			boolean disableArtist = true;
			
			if ( currentImagesTrack == null ) {
				disableAllMenus = true;
				
			} else if ( currentImagesTrack.getAlbum() != null ) {
				disableAlbum = false;
				
				if ( Utils.isArtistDirectory( currentImagesTrack.getAlbum().getPath().getParent() ) ) {
					disableArtist = false;
				}
			}
			
			setTrackArtistImage.setDisable( disableAllMenus );
			searchForArtistImage.setDisable ( disableAllMenus );
			setAlbumArtistImage.setDisable( disableAllMenus || disableAlbum );
			setArtistImage.setDisable( disableAllMenus || disableArtist );
			exportImage.setDisable( disableAllMenus );
			
			
			menu.show( artistImagePane, e.getScreenX(), e.getScreenY() );
		});

		exportImage.setOnAction( ( ActionEvent event ) -> {
			Track track = currentImagesTrack;
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
			Track track = currentImagesTrack;
			if ( track == null ) return;
			
			File imageFile = fileChooser.showOpenDialog( ui.getMainStage() );
			if ( imageFile == null ) return; 
			
			Track.saveArtistImageToTag ( track.getPath().toFile(), imageFile.toPath(), ArtistTagImagePriority.TRACK, false, audioSystem );
			setImages ( currentImagesTrack );
		});
		
		setAlbumArtistImage.setOnAction( ( ActionEvent e ) -> {
			Track track = currentImagesTrack;
			if ( track == null ) return;
			
			File imageFile = fileChooser.showOpenDialog( ui.getMainStage() );
			if ( imageFile == null ) return; 
			
			try {
				byte[] buffer = Files.readAllBytes( imageFile.toPath() );
				
				//REFACTOR: put this code in a function, it's duplciated below. 
				
				if ( track.getAlbum() == null ) return;
				
				Path albumPath = track.getAlbum().getPath();
			
				Utils.saveImageToDisk( albumPath.resolve( "artist.png" ), buffer );
				setImages ( currentImagesTrack );
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
	
					Platform.runLater( () -> setImages ( currentImagesTrack ) );
				});
				workerThread.setName ( "Album Artist Image Tag Saver" );
				workerThread.setDaemon( true );
				workerThread.start();
			} catch ( Exception e2 ) {
				LOGGER.log( Level.WARNING, "Unable to load image data from file: " + imageFile, e2 );
			}
			
		});
		
		setArtistImage.setOnAction( ( ActionEvent e ) -> {
			Track track = currentImagesTrack;
			if ( track == null ) return;
			
			File imageFile = fileChooser.showOpenDialog( ui.getMainStage() );
			if ( imageFile == null ) return; 
			
			try {
				byte[] buffer = Files.readAllBytes( imageFile.toPath() );
				
				//REFACTOR: put this code in a function, it's duplicated below. 
				
				if ( !Utils.isArtistDirectory( currentImagesTrack.getAlbum().getPath().getParent() ) ) return;
				
				Path artistPath = track.getAlbum().getPath().getParent();
			
				Utils.saveImageToDisk( artistPath.resolve( "artist.png" ), buffer );
				setImages ( currentImagesTrack );
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
	
					Platform.runLater( () -> setImages ( currentImagesTrack ) );
				});
				workerThread.setName ( "Album Artist Image Tag Saver" );
				workerThread.setDaemon( true );
				workerThread.start();
			} catch ( Exception e2 ) {
				LOGGER.log( Level.WARNING, "Unable to load image data from file: " + imageFile, e2 );
			}
			
		});
		
		artistImagePane.addEventHandler( MouseEvent.MOUSE_PRESSED, ( MouseEvent e ) -> {
			menu.hide();
		});
		
		artistImagePane.setOnDragOver( event -> {
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
			
			Track track = currentImagesTrack;
			
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
				
				byte[] buffer = getImageBytesFromDragboard( db );
				
				if ( buffer != null ) {
					promptAndSaveArtistImage ( buffer );
				} else {
					String url = "";
					if ( db.getContentTypes().contains( textContentFormat ) ) {
						url = (String)db.getContent( textContentFormat ) + "\n\n";
					}
					
					String message = 
						"Cannot pull image from dropped source.\n\n" +
						url + 
						"Try saving the image to disk and dragging from disk rather than web.";
					LOGGER.warning( message.replaceAll( "\n\n", " " ) );
					ui.notifyUserError( message );
				}
			}
			event.setDropCompleted( true );
			event.consume();		
		});
	}
	
	private void promptAndSaveArtistImage ( byte[] buffer ) {
		Track targetTrack = currentImagesTrack;
		
		if ( targetTrack != null ) {
			
			Path albumPath = targetTrack.getAlbum().getPath();
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
			
			if ( currentImagesAlbum == null ) {
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
					setImages ( currentImagesTrack );
					//PENDING: What about ID3 tags? Set them only if they're not already set?  
					break;
					
				case ALBUM:
					
					//REFACTOR: put this code in a function, it's duplicated above. 
					Utils.saveImageToDisk( albumPath.resolve( "artist.png" ), buffer );
					setImages ( currentImagesTrack );
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

						Platform.runLater( () -> setImages ( currentImagesTrack ) );
					});

					workerThread.setName ( "Artist Image Tag Saver" );
					workerThread.setDaemon( true );
					workerThread.start();
					break;
					
				case TRACK:
					Track.saveArtistImageToTag ( targetTrack.getPath().toFile(), buffer, ArtistTagImagePriority.TRACK, overwriteAll, audioSystem );
					setImages ( currentImagesTrack );
					break;
					
			}
		}
	}
	
	private void setImages ( Track track ) {
		setImages ( track, null );
	}
	
	private void setImages ( Album album ) {
		if ( album.getTracks() != null && album.getTracks().size() > 0 ) {
			setImages ( album.getTracks().get( 0 ), album );
		}
	}
	
	private void clearImages() {
		requestedTrack = null;
		requestedAlbum = null;
		currentImagesTrack = null;
		currentImagesAlbum = null;
		Platform.runLater( () -> {
			setAlbumImage ( null );
			setArtistImage ( null );
		});
	}
	
	private void setImages ( Track track, Album album ) {
		
		if ( track != null && Files.exists( track.getPath() ) ) {
			requestedTrack = track;
			requestedAlbum = album;
			
		} else if ( track == null && currentImagesTrack != null ) {
			setImages ( currentImagesTrack );
			
		} else {	
			requestedTrack = null;
			requestedAlbum = null;
			currentImagesTrack = null;
			currentImagesAlbum = null;
			setAlbumImage ( null );
			setArtistImage ( null );
		}
	}

	private void setAlbumImage ( Image image ) {
		if ( image == null ) {
			albumImagePane.setCenter( null );
		} else {
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
	}

	private void setArtistImage ( Image image ) {
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
	
	private byte[] getImageBytesFromDragboard ( Dragboard db ) {
		byte[] retMe = null;
		
		if ( db.getContentTypes().contains( textContentFormat ) ) {
			URL url;
			try {
				String file = (String) db.getContent( textContentFormat );
				file = file.replaceAll( "\\?.*$",  "" );//Get rid of everything after ? in a url, because who cares about arguments
				
				if ( Utils.hasImageExtension( file ) ) {
					url = new URL( file );
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					try ( InputStream is = url.openStream () ) {
						byte[] byteChunk = new byte [ 4096 ]; 
						int n;
	
						while ( (n = is.read( byteChunk )) > 0 ) {
							baos.write( byteChunk, 0, n );
						}
	
						retMe = baos.toByteArray();
						
					} catch ( IOException e ) {
						LOGGER.log( Level.WARNING, "Unable to pull image from internet (" + db.getContent( textContentFormat ) + ")", e );
					}
				} else {
					LOGGER.info( "Received drop of a non-image file, ignored: " + db.getContent( textContentFormat ) );
				}
				
			} catch ( MalformedURLException e1 ) {
				LOGGER.info( "Unable to parse url: " + db.getContent( textContentFormat ) );
			}
		} 
		
		if ( retMe == null ) {
			DataFormat byteBufferFormat = null;
			for ( DataFormat format : db.getContentTypes() ) { 
				//Stupidly, DataFormat.lookupMimeType( "application/octet-stream" ) does not work.
				//But this does. 
				if ( "application/octet-stream".equals( format.getIdentifiers().toArray()[0] ) ) {
					byteBufferFormat = format;
				}
			}
			
			if ( byteBufferFormat != null ) {
				retMe = ((ByteBuffer)db.getContent( byteBufferFormat )).array();
				if ( retMe.length < 100 ) retMe = null; //This is a hack. 
			}

			retMe = ((ByteBuffer)db.getContent( byteBufferFormat )).array();
		}

		return retMe;
	}

	public void libraryCleared () {
		Platform.runLater(() -> {
			if ( audioSystem.getCurrentList().getItems().isEmpty() ) {
				if ( audioSystem.isPlaying() ) {
					setImages ( audioSystem.getCurrentTrack() );
				} else {
					clearImages();
				}
			}
		});
	}
	
	public void currentListCleared() {
		if ( Hypnos.getLibrary().getAlbums().isEmpty() && Hypnos.getLibrary().getTracks().isEmpty() ) {
			if ( audioSystem.isPlaying() ) {
				setImages ( audioSystem.getCurrentTrack() );
			} else {
				clearImages();
			}
		}
	}
	
	public void trackSelected ( Track selected ) {
		setImages ( selected );
	}
	
	public void albumSelected ( Album selected ) {
		setImages ( selected );
	}

	@Override
	public void playerStopped ( Track track, StopReason reason ) {
		if ( audioSystem.getCurrentList().getItems().isEmpty()
		&& Hypnos.getLibrary().getAlbums().isEmpty()
		&& Hypnos.getLibrary().getTracks().isEmpty() ) {
			clearImages();
		}
	}

	@Override
	public void playerStarted ( Track track ) {
		setImages( track );
	}
	
	@Override
	public void playerUnpaused () {
		setImages( audioSystem.getCurrentTrack() );
	}

	@Override
	public void playerPositionChanged ( int positionMS, int lengthMS ) {}
	
	@Override 
	public void playerPaused () {}
	
	@Override
	public void playerVolumeChanged ( double newVolumePercent ) {}
	
	@Override
	public void playerShuffleModeChanged ( ShuffleMode newMode ) {}
	
	@Override
	public void playerRepeatModeChanged ( RepeatMode newMode ) {}

	public void refreshImages () {
		setImages ( currentImagesTrack, currentImagesAlbum );
	}

	public Track getCurrentImagesTrack () {
		return currentImagesTrack;
	}

}
