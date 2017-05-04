package org.joshuad.musicplayer;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Track implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public enum Format {
		FLAC ( "flac" ),
		MP3 ( "mp3" ),
		UNKNOWN ( "" );
		
		final String extension;
		Format ( String extension ) {
			this.extension = extension;
		}
		
		public String getExtension () {
			return extension;
		}
	}
	
	
	//TODO: Do these need to be final? 
	private SimpleStringProperty artist;
	private SimpleStringProperty year;
	private SimpleStringProperty album;
	private SimpleStringProperty title;
	private SimpleIntegerProperty trackNumber;
	private SimpleIntegerProperty length;
	private SimpleStringProperty disc;
	private SimpleStringProperty discCount;
	private Path trackPath;
	
	private SimpleBooleanProperty isCurrentTrack = new SimpleBooleanProperty ( false );
	
	//TODO: Deal w/ these exceptions right. 
	Track ( Path trackPath ) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
		this.trackPath = trackPath;
		AudioFile audioFile = AudioFileIO.read( trackPath.toFile() );
		Tag tag = audioFile.getTag();
		//TODO: what to do if no tag present? 
		artist = new SimpleStringProperty ( tag.getFirst ( FieldKey.ARTIST ) );
		year = new SimpleStringProperty ( tag.getFirst ( FieldKey.YEAR ) );
		album = new SimpleStringProperty ( tag.getFirst ( FieldKey.ALBUM ) );
		title = new SimpleStringProperty ( tag.getFirst ( FieldKey.TITLE ) );
		discCount = new SimpleStringProperty ( tag.getFirst ( FieldKey.DISC_TOTAL ) );
		
		String rawTrackText = tag.getFirst ( FieldKey.TRACK );
		
		if ( rawTrackText.matches( "\\d+" ) ) { // 0, 01, 1010, 2134141, etc.
			trackNumber = new SimpleIntegerProperty ( Integer.parseInt( rawTrackText ) );
			
		} else if ( rawTrackText.matches("\\d+/.*") ) {
			//if matches 23/<whatever>
			trackNumber = new SimpleIntegerProperty ( Integer.parseInt( rawTrackText.split("/")[0] ) );
		} else {
			System.out.println ( "Invalid track number: " + rawTrackText );
			trackNumber = new SimpleIntegerProperty ( -1 );
			throw new TagException();
		}
					
		length = new SimpleIntegerProperty ( audioFile.getAudioHeader().getTrackLength() );		
	}
	
	public void setIsCurrentTrack ( boolean isCurrentTrack ) {
		this.isCurrentTrack.set( isCurrentTrack );
	}
	
	public boolean getIsCurrentTrack ( ) {
		return isCurrentTrack.get();
	}
	
	public String getArtist () {
		return artist.get();
	}
	
	public String getYear () {
		return year.get();
	}
	
	public String getAlbum () {
		
		try {
			if ( disc != null && discCount != null && disc.get().length() > 0 && discCount.get().length() > 0 && Integer.parseInt( discCount.get() ) > 1 ) {
				return album.get() + " (disc " + disc.get() + ")";
			} 
		} catch ( NumberFormatException e ) {
		}

		return album.get();
	}		
	
	public String getTitle () {
		return title.get();
	}
	
	public int getTrackNumber () {
		return trackNumber.get();
	}	
	
	public int getLength () {
		return length.get();
	}
	
	public Path getPath() {
		return trackPath;
	}
	
	public String getLengthDisplay () {
		return Utils.getLengthDisplay( getLength () );
	}
	
	public Format getFormat () {
		String fileName = getPath().getFileName().toString();
		
		String testExtension = fileName.substring ( fileName.lastIndexOf( "." ) + 1 ).toLowerCase();
		
		if ( testExtension.equals( Format.FLAC.getExtension() ) ) {
			return Format.FLAC;
		
		} else if ( testExtension.equals( Format.MP3.getExtension() ) ) {
			return Format.MP3;
		
		} else {
			return Format.UNKNOWN;
		}
	}
}
