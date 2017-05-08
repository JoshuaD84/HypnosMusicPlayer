package org.joshuad.musicplayer;
import java.io.File;
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
	private String artist;
	private String year;
	private String album;
	private String title;
	private int trackNumber;
	private int length;
	private String disc;
	private String discCount;
	private File trackPath;
	
	private boolean isCurrentTrack = false;
	
	//TODO: Deal w/ these exceptions right. 
	public Track ( Path trackPath ) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
		this.trackPath = trackPath.toFile();
		AudioFile audioFile = AudioFileIO.read( trackPath.toFile() );
		Tag tag = audioFile.getTag();
		//TODO: what to do if no tag present? 
		try { 
			artist = tag.getFirst ( FieldKey.ARTIST );
		} catch ( NullPointerException e ) { throw new TagException( "Cannot read artist tag." ); }
		
		try { 
			year = tag.getFirst ( FieldKey.YEAR );
		} catch ( NullPointerException e ) { throw new TagException( "Cannot read year tag." ); }
		
		try { 	
			album = tag.getFirst ( FieldKey.ALBUM );
		} catch ( NullPointerException e ) { throw new TagException( "Cannot read album tag." ); }
		
		try { 
			title = tag.getFirst ( FieldKey.TITLE );
		} catch ( NullPointerException e ) { throw new TagException( "Cannot read title tag." ); }
		
		try { 		
			discCount = tag.getFirst ( FieldKey.DISC_TOTAL );
		} catch ( NullPointerException e ) { throw new TagException( "Cannot read disc count tag." ); }
	
		
		String rawTrackText = tag.getFirst ( FieldKey.TRACK );
		
		if ( rawTrackText.matches( "\\d+" ) ) { // 0, 01, 1010, 2134141, etc.
			trackNumber = Integer.parseInt( rawTrackText );
			
		} else if ( rawTrackText.matches("\\d+/.*") ) {
			//if matches 23/<whatever>
			trackNumber = Integer.parseInt( rawTrackText.split("/")[0] );
		} else {
			System.out.println ( "Invalid track number: '" + rawTrackText + "' - " + trackPath.toString() );
			trackNumber = -1;
			throw new TagException();
		}
					
		length = audioFile.getAudioHeader().getTrackLength();		
	}
	
	public void setIsCurrentTrack ( boolean isCurrentTrack ) {
		this.isCurrentTrack = isCurrentTrack;
	}
	
	public boolean getIsCurrentTrack ( ) {
		return isCurrentTrack;
	}
	
	public String getArtist () {
		return artist;
	}
	
	public String getYear () {
		return year;
	}
	
	public String getAlbum () {
		
		try {
			if ( disc != null && discCount != null && disc.length() > 0 && discCount.length() > 0 && Integer.parseInt( discCount ) > 1 ) {
				return album + " (disc " + disc + ")";
			} 
		} catch ( NumberFormatException e ) {
		}

		return album;
	}		
	
	public String getTitle () {
		return title;
	}
	
	public int getTrackNumber () {
		return trackNumber;
	}	
	
	public int getLength () {
		return length;
	}
	
	public Path getPath() {
		return trackPath.toPath();
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
