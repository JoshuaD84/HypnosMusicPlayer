package org.joshuad.musicplayer;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import javafx.beans.property.SimpleStringProperty;

public class Album {
	private final SimpleStringProperty artist;
	private final SimpleStringProperty year;
	private final SimpleStringProperty title;
	private Path directoryPath;
	
	
	
	Album ( String artist, String year, String title, Path directoryPath ) {
		this.artist = new SimpleStringProperty ( artist );
		this.year = new SimpleStringProperty ( year );
		this.title = new SimpleStringProperty ( title );
		this.directoryPath = directoryPath;
	}
	
	public String getArtist () {
		return artist.get();
	}
	
	public String getYear () {
		return year.get();
	}
	
	public String getTitle () {
		return title.get();
	}		
	
	public Path getPath () {
		return directoryPath;
	}
	
	public ArrayList <Track> getTracks() {
		try {
			ArrayList <Track> retMe = new ArrayList <Track> ();
		
			DirectoryStream <Path> albumDirectoryStream = Files.newDirectoryStream ( directoryPath, Utils.musicFileFilter );
					
			for ( Path trackPath : albumDirectoryStream ) {
				try {
					retMe.add( new Track ( trackPath ) );
				} catch ( TagException e) {
					System.out.println ( "Unable to read tags on: " + trackPath.toString() +", skipping." );
				} catch (CannotReadException e) {
					System.out.println ( trackPath.toString() );
					e.printStackTrace();
				} catch (ReadOnlyFileException e) {
					System.out.println ( trackPath.toString() +", is read only, unable to edit, skipping." );
				} catch (InvalidAudioFrameException e) {
					System.out.println ( trackPath.toString() +", has bad audio fram data, skipping." );
				}
			}
			
			retMe.sort ( Comparator.comparing( Track::getTrackNumber ) );
			return retMe;
		} catch ( IOException e) {
			System.out.println ( "Unable to get list of files in directory: " + directoryPath.toString() + ", skipping album." );
			return null;

		} 
	}
}

	
