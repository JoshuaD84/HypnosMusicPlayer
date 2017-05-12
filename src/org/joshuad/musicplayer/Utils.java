package org.joshuad.musicplayer;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

public class Utils {
	
	private static String[] musicExtStrings = new String[] { "flac", "mp3" };
	private static String[] imageExtStrings = new String[] { "jpg", "png", "gif" };
	
		
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
			if ( imageExtension.toLowerCase().equals( testExtension ) ) { 
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
			if ( musicExtension.toLowerCase().equals( testExtension ) ) { 
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
		
		
		ArrayList <Path> possibleFiles = new ArrayList <Path> ();
		possibleFiles.add( Paths.get ( track.getPath().getParent().toString(), "front.jpg" ) );
		possibleFiles.add( Paths.get ( track.getPath().getParent().toString(), "front.png" ) );
		possibleFiles.add( Paths.get ( track.getPath().getParent().toString(), "cover.jpg" ) );
		possibleFiles.add( Paths.get ( track.getPath().getParent().toString(), "cover.png" ) );
		possibleFiles.add( Paths.get ( track.getPath().getParent().toString(), "album.jpg" ) );
		possibleFiles.add( Paths.get ( track.getPath().getParent().toString(), "album.png" ) );
		
		try {
			DirectoryStream <Path> albumDirectoryStream = Files.newDirectoryStream ( track.getPath().getParent(), imageFileFilter );
			for ( Path imagePath : albumDirectoryStream ) { possibleFiles.add( imagePath ); }
		
		} catch ( IOException e ) {
			//TODO: I think we can ignore this one. 
		}
		
		for ( Path test : possibleFiles ) {
			if ( Files.exists( test ) && Files.isRegularFile( test ) ) {
				return test;
			}
		}
		
		return null;
	}
	
	public static Path getAlbumArtistImagePath ( Track track ) {
		
		ArrayList <Path> possibleFiles = new ArrayList <Path> ();
		possibleFiles.add( Paths.get ( track.getPath().getParent().toString(), "artist.jpg" ) );
		possibleFiles.add( Paths.get ( track.getPath().getParent().toString(), "artist.png" ) );
		possibleFiles.add( Paths.get ( track.getPath().getParent().toString(), "artist.gif" ) );
		possibleFiles.add( Paths.get ( track.getPath().getParent().getParent().toString(), "artist.jpg" ) );
		possibleFiles.add( Paths.get ( track.getPath().getParent().getParent().toString(), "artist.png" ) );
		possibleFiles.add( Paths.get ( track.getPath().getParent().getParent().toString(), "artist.gif" ) );
		
		try {
			DirectoryStream <Path> artistDirectoryStream = Files.newDirectoryStream ( track.getPath().getParent().getParent(), imageFileFilter );
			for ( Path imagePath : artistDirectoryStream ) { possibleFiles.add( imagePath ); }
		
		} catch ( IOException e ) {
			//TODO: I think we can ignore this one. 
		}
		
		for ( Path test : possibleFiles ) {
			if ( Files.exists( test ) && Files.isRegularFile( test ) ) {
				return test;
			}
		}
		
		return null;
	}
	
	public static boolean isAlbumDirectory ( Path path ) {
		if ( !Files.isDirectory( path ) ) return false;
		
		boolean hasChildAlbumDirectory = false;
		boolean hasChildTrack = false;
		
		try ( 
			DirectoryStream <Path> stream = Files.newDirectoryStream( path ); 
		) {
			for ( Path child : stream ) {
				if ( Files.isDirectory( child ) ) {
					if ( isAlbumDirectory ( child ) ) {
						hasChildAlbumDirectory = true;
					}
				}
				
				if ( Utils.isMusicFile( child ) ) {
					hasChildTrack = true;
				}
			}
		} catch ( IOException e ) {
			return false;
		}
		
		if ( hasChildAlbumDirectory ) return false;
		
		if ( hasChildTrack ) return true;
		else return false;
	}
	
	public static ArrayList <Track> getAllTracksInDirectory ( Path startingDirectory ) {
		
		TrackFinder finder = new TrackFinder () ;
		try {
			Files.walkFileTree( startingDirectory, finder );
	    	return finder.tracks;
		} catch (IOException e) {
			// TODO 
			e.printStackTrace();
		}
		
		return new ArrayList <Track> ();
	}
}


class TrackFinder extends SimpleFileVisitor <Path> {
	
	ArrayList <Track> tracks = new ArrayList <Track> ();
	
	@Override
	public FileVisitResult visitFile ( Path file, BasicFileAttributes attr ) {

		boolean isMusicFile = Utils.isMusicFile ( file );
		
		if ( isMusicFile ) {
			try {
				Track track = new Track ( file );
				tracks.add ( track );
				
			} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
				//TODO: 				
			}
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
