package gui;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class Utils {
	
	private static String[] musicExtStrings = new String[] { "flac", "mp3" };
	private static String[] imageExtStrings = new String[] { "flac", "mp3" };
	
	
		
	static ArrayList <String> musicExtensions = new ArrayList <String> ( Arrays.asList ( musicExtStrings ) );  

	public static final DirectoryStream.Filter<Path> musicFileFilter = new DirectoryStream.Filter<Path>() {
		@Override
		public boolean accept ( Path entry ) throws IOException {
			return isMusicFile ( entry );			
		}
	};
	

	public static final DirectoryStream.Filter<Path> imageFileFilter = new DirectoryStream.Filter<Path>() {
		@Override
		public boolean accept ( Path entry ) throws IOException {
			return isImageFile ( entry );			
		}
	};
	  
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
			if ( imageExtension.equals( testExtension ) ) { 
				return true;
			}
		}
		
		return false;
				
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
			if ( musicExtension.equals( testExtension ) ) { 
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
	
	public static Path getAlbumCoverImagePath ( Track track ) {
		
		Path cover = Paths.get ( track.getPath().getParent().toString(), "front.jpg" );
		
		System.out.println ( cover );
		
		if ( Files.exists( cover ) && Files.isRegularFile( cover ) ) {
			return cover;
		} else {
			return null;
		}		
	}
	
	public static Path getAlbumArtistImagePath ( Track track ) {
		
		Path cover = Paths.get ( track.getPath().getParent().getParent().toString(), "artist.jpg" );
		
		System.out.println ( cover );
		
		if ( Files.exists( cover ) && Files.isRegularFile( cover ) ) {
			return cover;
		} else {
			return null;
		}		
	}
}
