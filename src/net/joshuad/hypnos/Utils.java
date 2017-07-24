package net.joshuad.hypnos;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.StandardArtwork;

import me.xdrop.fuzzywuzzy.FuzzySearch;

public class Utils {

	private static transient final Logger LOGGER = Logger.getLogger( Utils.class.getName() );
	
	private static String[] musicExtStrings = new String[] { "flac", "mp3", "ogg", "m4a", "m4b", "m4r", "aac", "wav" }; //TODO: use Track.Format instead
	private static String[] imageExtStrings = new String[] { "jpg", "png", "gif" };
	private static String[] playlistExtStrings = new String[] { "m3u" };
		
	public static final ArrayList <String> musicExtensions = new ArrayList <String> ( Arrays.asList ( musicExtStrings ) );  

	public static final DirectoryStream.Filter<Path> musicFileFilter = new DirectoryStream.Filter<Path>() {
		@Override
		public boolean accept ( Path entry ) throws IOException {
			return isMusicFile ( entry );			
		}
	};
	
	public static boolean isImageFile ( File testFile ) {
		return isImageFile ( testFile.toPath() );
	}
	  
	public static boolean isImageFile ( Path testFile ) {
		String fileName = testFile.getFileName().toString();
		
		if ( !Files.exists( testFile ) ) {
			return false;		
		
		} else if ( !Files.isRegularFile( testFile ) ) {
			return false;
		
		} else if ( fileName.lastIndexOf(".") == -1 || fileName.lastIndexOf(".") == 0 ) {
			return false;
			
		} 
		
		String testExtension = fileName.substring ( fileName.lastIndexOf( "." ) + 1 ).toLowerCase();
		
		for ( String imageExtension : imageExtStrings ) {
			if ( imageExtension.toLowerCase().equals( testExtension ) ) { 
				return true;
			}
		}
		
		return false;
				
	}
	  
	public static String toReleaseTitleCase ( String input ) {
		if ( input.equalsIgnoreCase( "EP" ) ) {
			return "EP";
		} else {
			
		    StringBuilder titleCase = new StringBuilder();
		    boolean nextTitleCase = true;
	
		    for (char c : input.toCharArray()) {
		        if (Character.isSpaceChar(c)) {
		            nextTitleCase = true;
		        } else if (nextTitleCase) {
		            c = Character.toTitleCase(c);
		            nextTitleCase = false;
		        }
	
		        titleCase.append(c);
		    }
	
		    return titleCase.toString();
		}
	}
	
	public static boolean isMusicFile ( String testFile ) {
		return isMusicFile ( Paths.get( testFile ) );
	}
	
	public static boolean isMusicFile ( Path testFile ) {
		String fileName = testFile.getFileName().toString();
		
		if ( !Files.exists( testFile ) ) {
			return false;		
		
		} else if ( !Files.isRegularFile( testFile ) ) {
			return false;
		
		} else if ( fileName.lastIndexOf(".") == -1 || fileName.lastIndexOf(".") == 0 ) {
			return false;
			
		} 
		
		String testExtension = fileName.substring ( fileName.lastIndexOf( "." ) + 1 ).toLowerCase();
		
		for ( String musicExtension : musicExtensions ) {
			if ( musicExtension.toLowerCase().equals( testExtension ) ) { 
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean isPlaylistFile ( Path testFile ) {
		String fileName = testFile.getFileName().toString();
		
		if ( !Files.exists( testFile ) ) {
			return false;		
		
		} else if ( !Files.isRegularFile( testFile ) ) {
			return false;
		
		} else if ( fileName.lastIndexOf(".") == -1 || fileName.lastIndexOf(".") == 0 ) {
			return false;
			
		} 
		
		String testExtension = fileName.substring ( fileName.lastIndexOf( "." ) + 1 ).toLowerCase();
		
		for ( String playlistExtension : playlistExtStrings ) {
			if ( playlistExtension.toLowerCase().equals( testExtension ) ) { 
				return true;
			}
		}
		
		return false;
	}
	
	public static String getLengthDisplay ( int lengthSeconds ) {
		boolean negative = lengthSeconds < 0;
		lengthSeconds = Math.abs( lengthSeconds );
		int hours = lengthSeconds / 3600;
		int minutes = ( lengthSeconds % 3600 ) / 60;
		int seconds = lengthSeconds % 60;
		
		if ( hours > 0 ) {
			return String.format ( (negative ? "-" : "") + "%d:%02d:%02d", hours, minutes, seconds );
		} else {
			return String.format ( (negative ? "-" : "") + "%d:%02d", minutes, seconds );
		}
	}
	
	public static ArrayList <CurrentListTrack> convertTrackList ( List <Track> tracks ) {
		ArrayList <CurrentListTrack> retMe = new ArrayList <CurrentListTrack> ( tracks.size() );
		
		for ( Track track : tracks ) {
			if ( track instanceof CurrentListTrack ) {
				retMe.add ( (CurrentListTrack)track );
			} else {
				try {
					retMe.add ( new CurrentListTrack ( track ) );
				} catch ( FileNotFoundException e ) {
					LOGGER.log( Level.INFO, "Unable to load track: " + track.getPath().toString() );
				} catch ( IOException e ) {
					e.printStackTrace();
				}
			}
		}
		
		return retMe;
	}
	
	public static ArrayList <Track> convertCurrentTrackList ( List <CurrentListTrack> tracks ) {
		ArrayList <Track> retMe = new ArrayList <Track> ( tracks.size() );
		
		for ( CurrentListTrack track : tracks ) {
			retMe.add ( (Track)track );
		}
		
		return retMe;
	}
	
	public static boolean isArtistDirectory ( Path path ) {
		if ( !Files.isDirectory( path ) ) return false;
		
		String directoryName = prepareArtistForCompare ( path.getFileName().toString() );
		
		int entries = 0;
		
		try ( DirectoryStream <Path> stream = Files.newDirectoryStream( path ) ) {
			for ( Path child : stream ) {
				if ( isAlbumDirectory ( child ) ) {
					entries++;
					Album album = new Album ( child );
					int matchPercent = FuzzySearch.weightedRatio( directoryName, prepareArtistForCompare ( album.getAlbumArtist() ) );
					if ( matchPercent < 90 ) return false;
					
					
				} else if ( isMusicFile( child ) ) {
					entries++;
					Track track = new Track ( child );
					int matchPercent = FuzzySearch.weightedRatio( directoryName, prepareArtistForCompare ( track.getAlbumArtist() ) );
					if ( matchPercent < 90 ) return false;
				}
			}
		} catch ( IOException e ) {
			return false;
		}
		
		if ( entries > 0 ) {
			return true;
		} else {
			return false;
		}
	}
	
	private static String prepareArtistForCompare ( String string ) {
		return string
			.toLowerCase()
			.replaceAll( " & ", " and " )
			.replaceAll( "&", " and " )
			.replaceAll( " \\+ ", " and ")
			.replaceAll( "\\+", " and " )
			.replaceAll( "  ", " " )
			.replaceAll( " the ", "" )
			.replaceAll( "^the ", "" )
			.replaceAll( ", the$", "" )
			.replaceAll( "-", " " )
			.replaceAll( "\\.", "" )
			.replaceAll( "_", " ")
		;
	}
	
	private static String prepareAlbumForCompare ( String string ) {
		return string.toLowerCase();
	}
	
	public static boolean isAlbumDirectory ( Path path ) {
		if ( !Files.isDirectory( path ) ) return false;
		
		boolean hasChildTrack = false;
		
		String albumName = null;
		String artistName = null;
		
		try ( 
			DirectoryStream <Path> stream = Files.newDirectoryStream( path ); 
		) {
			for ( Path child : stream ) {
				if ( isAlbumDirectory ( child ) ) {
					return false;
				}
				
				if ( Utils.isMusicFile( child ) ) {
					Track track = new Track ( child );
					if ( albumName == null ) {
						albumName = prepareAlbumForCompare ( track.getSimpleAlbumTitle() );
						artistName = prepareArtistForCompare ( track.getAlbumArtist() );
						
					} else {
						int albumMatchPercent = FuzzySearch.weightedRatio( albumName, prepareAlbumForCompare ( track.getSimpleAlbumTitle() ) );
						if ( albumMatchPercent < 90 ) {
							return false;
						}
						
						int artistMatchPercent = FuzzySearch.weightedRatio( artistName, prepareArtistForCompare ( track.getAlbumArtist() ) );
						if ( artistMatchPercent < 90 ) {
							return false;
						}
					}
					hasChildTrack = true;
				}
			}
		} catch ( IOException e ) {
			return false;
		}
		
		if ( hasChildTrack ) return true;
		else return false;
	}
	
	public static ArrayList <Path> getAllTracksInDirectory ( Path startingDirectory ) {
		
		TrackFinder finder = new TrackFinder ();
		try {
			Files.walkFileTree( startingDirectory, finder );
	    	return finder.trackPaths;
	    	
		} catch (IOException e) {
			System.out.println ( "Read error while traversing directory, some files may not have been loaded: " + startingDirectory.toString() );
		}
		
		return new ArrayList <Path> ();
	}
	
	public static boolean saveImageToDisk ( Path location, byte[] buffer ) {
		
		if ( location == null ) {
			return false;
		}
		
		InputStream in = new ByteArrayInputStream ( buffer );
		
		try {
			BufferedImage bImage = ImageIO.read( in );
			ImageIO.write ( bImage, "png", location.toFile() );
			return true;
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	public static void saveArtistImageToID3 ( File file, byte[] buffer ) {
		saveImageToID3 ( file, buffer, 8 );
	}
	

	public static void saveAlbumImageToID3 ( File file, byte[] buffer ) {
		saveImageToID3 ( file, buffer, 3 );
	}
	
	private static void saveImageToID3 ( File file, byte[] buffer, int type ) {
		try {
			
			AudioFile audioFile = AudioFileIO.read( file );
			Tag tag = audioFile.getTag();
			
			if ( tag instanceof ID3v1Tag ) {
				tag = new ID3v23Tag ( (ID3v1Tag)tag );
			}
			
			Artwork artwork = new StandardArtwork();
			artwork.setBinaryData( buffer );
			artwork.setPictureType( type ); //See ID3 specifications for why 8 
			
			tag.addField( artwork );
			
			audioFile.setTag( tag );
			AudioFileIO.write( audioFile );
			
		} catch ( CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException | CannotWriteException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class TrackFinder extends SimpleFileVisitor <Path> {

	private static transient final Logger LOGGER = Logger.getLogger( TrackFinder.class.getName() );
	
	ArrayList <Path> trackPaths = new ArrayList <Path> ();
	
	@Override
	public FileVisitResult visitFile ( Path path, BasicFileAttributes attr ) {

		boolean isMusicFile = Utils.isMusicFile ( path );
		
		if ( isMusicFile ) {
			trackPaths.add ( path );
		} 
		
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult postVisitDirectory( Path dir, IOException exc ) {
		return FileVisitResult.CONTINUE;
	}
	
	public FileVisitResult visitFileFailed( Path file, IOException exc ) {
		return FileVisitResult.CONTINUE;
	}
}
