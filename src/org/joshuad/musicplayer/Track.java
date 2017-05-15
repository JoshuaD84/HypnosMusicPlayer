package org.joshuad.musicplayer;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	private int length = 0;
	private File trackFile;
	private boolean hasAlbum = false;
	
	private String artist = "";
	private String albumArtist = "";
	private String title = "";
	private String album = "";
	private String year = "";
	private int trackNumber = -1;
	private String discSubtitle = null;
	private Integer discNumber = null;
	private Integer discCount =  null;
	private String releaseType = null;
	
	public Track ( Path trackPath ) {
		this ( trackPath, false );
	}
	
	//TODO: Deal w/ these exceptions right, don't throw them. Catch them and fill in data as best you can. 
	public Track ( Path trackPath, boolean hasAlbum ) {

		Logger.getLogger( "org.jaudiotagger" ).setLevel( Level.OFF );
		this.trackFile = trackPath.toFile();
		this.hasAlbum = hasAlbum;
		
		Tag tag = null;
		AudioFile audioFile;
		try {
			audioFile = AudioFileIO.read( trackPath.toFile() );
			length = audioFile.getAudioHeader().getTrackLength();
			tag = audioFile.getTag();
		} catch ( CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e1 ) {
			e1.printStackTrace ( System.out );
		}
		
		parseArtist( tag );
		parseTitle( tag ); 
		parseAlbum( tag );
		parseYear( tag );
		parseTrackNumber( tag );
		parseDiscInfo( tag );
		parseReleaseType( tag );
		
		parseFileName();
	}
	
	private void parseFileName () {
		
		String fnArtist = "";
		String fnYear = "";
		String fnAlbum = "";
		String fnTrackNumber = "";
		String fnTitle = "";
		
		fnArtist = trackFile.toPath().getParent().getParent().getFileName().toString();
	
		String parentName = trackFile.toPath().getParent().getFileName().toString();
		String[] parentParts = parentName.split( " - " );
		
		if ( parentParts.length == 1 ) {
			fnAlbum = parentParts [ 0 ];
			
		} else if ( parentParts.length == 2 ) { 
			if ( parentParts [ 0 ].matches( "^[0-9]{4}[a-zA-Z]{0,1}" ) ) {
				fnYear = parentParts [ 0 ];
				fnAlbum = parentParts [ 1 ];
			} else if ( parentParts [ 1 ].matches( "^[0-9]{4}[a-zA-Z]{0,1}" ) ) {
				fnYear = parentParts [ 1 ];
				fnAlbum = parentParts [ 0 ];
			} else {
				fnAlbum = parentName;
			}
		}
		
		fnTitle = trackFile.toPath().getFileName().toString();
		
		try {
			fnTitle = fnTitle.replaceAll( " - ", "" ).replaceAll( fnArtist, "" )
									.replaceAll( fnAlbum, "" ).replaceAll( fnYear, "" );
		} catch ( Exception e ) {}
		
		boolean setByFileName = false;
		
		//TODO: Some error checking, only do this if we're pretty sure it's good. 
		if ( artist.equals( "" ) ) { artist = fnArtist; setByFileName = true; }
		if ( albumArtist.equals( "" ) ) { albumArtist = fnArtist; setByFileName = true; }
		if ( album.equals( "" ) ) { album = fnAlbum; setByFileName = true; }
		if ( year.equals( "" ) ) { year = fnYear; setByFileName = true; }
		if ( title.equals( "" ) ) { title = fnTitle; setByFileName = true; }
		
		if ( setByFileName ) {
			System.out.println ( "Set by filename: " + trackFile );
		}
			
	}
	
	private void parseArtist( Tag tag ) {
		//TODO: Do we want to do antyhing with FieldKey.ARTISTS or .ALBUM_ARTISTS or .ALBUM_ARTIST_SORT?
		if ( tag != null ) {
			albumArtist = tag.getFirst ( FieldKey.ALBUM_ARTIST );
			artist = tag.getFirst ( FieldKey.ARTIST );
		}
		
		if ( albumArtist.equals( "" ) ) {
			albumArtist = artist;
		}
	}
	
	private void parseTitle ( Tag tag ) {
		if ( tag != null ) {
			title = tag.getFirst ( FieldKey.TITLE );
			
			if ( title.equals( "" ) ) {
				title = tag.getFirst( FieldKey.TITLE_SORT );
			}
		}	
	}
	
	private void parseAlbum ( Tag tag ) {
		if ( tag != null ) {
			album = tag.getFirst ( FieldKey.ALBUM );
			if ( album.equals( "" ) ) album = tag.getFirst( FieldKey.ALBUM_SORT );
		}
	}
	
	private void parseYear ( Tag tag ) {
		if ( tag != null ) {
			year = tag.getFirst ( FieldKey.ORIGINAL_YEAR );
			if ( year.equals( "" ) ) year = tag.getFirst( FieldKey.YEAR );
		}
	}
	
	private void parseTrackNumber( Tag tag ) {
		if ( tag != null ) {
			
			String rawText = tag.getFirst ( FieldKey.TRACK );
			String rawNoWhiteSpace = rawText.replaceAll("\\s+","");
			
			try { 
				if ( rawText.matches( "^[0-9]+$" ) ) { // 0, 01, 1010, 2134141, etc.
					trackNumber = Integer.parseInt( rawText );
					
				} else if ( rawNoWhiteSpace.matches( "^[0-9]+$" ) ) { 
					trackNumber = Integer.parseInt( rawNoWhiteSpace );
					
				} else if ( rawText.matches("^[0-9]+/.*") ) {
					trackNumber = Integer.parseInt( rawText.split("/")[0] );
					System.out.println ( "Bad, but fixable, track numbering: " + rawText + " - " + trackFile.toString() );
				
				} else if ( rawNoWhiteSpace.matches("^[0-9]+/.*") ) {
					trackNumber = Integer.parseInt( rawNoWhiteSpace.split("/")[0] );
					System.out.println ( "Bad, but fixable, track numbering: " + rawText + " - " + trackFile.toString() );
					
				} else {
					throw new NumberFormatException();
				}
				
			} catch ( NumberFormatException e ) {
				if ( ! rawNoWhiteSpace.equals( "" ) ) {
					System.out.println ( "Invalid track number: " + rawText  + " - " + trackFile.toString() );
				}
			}
		}
	}
	
	private void parseDiscInfo ( Tag tag ) {
		if ( tag != null ) {

			discSubtitle = tag.getFirst ( FieldKey.DISC_SUBTITLE );
			
			try {
				discCount = Integer.valueOf( tag.getFirst ( FieldKey.DISC_TOTAL ) );
			} catch ( NumberFormatException e ) {
				if ( ! tag.getFirst ( FieldKey.DISC_TOTAL ).equals( "" ) ) {
					System.out.println ( "Invalid disc count: " + tag.getFirst ( FieldKey.DISC_TOTAL )  + " - " + trackFile.toString() );
				}
			}
			
			String rawText = tag.getFirst ( FieldKey.DISC_NO );
			String rawNoWhiteSpace = rawText.replaceAll("\\s+","");
			
			try { 
				if ( rawText.matches( "^[0-9]+$" ) ) { // 0, 01, 1010, 2134141, etc.
					discNumber = Integer.parseInt( rawText );
					
				} else if ( rawNoWhiteSpace.matches( "^[0-9]+$" ) ) { 
						discNumber = Integer.parseInt( rawNoWhiteSpace );
					
				} else if ( rawText.matches("^[0-9]+/.*") ) {
					//if matches 23/<whatever>
					discNumber = Integer.parseInt( rawText.split("/")[0] );
					
					if ( discCount == null || discCount.equals( "" ) ) {
						discCount = Integer.parseInt( rawText.split("/")[1] );
					}
						
					System.out.println ( "Bad, but fixable, disc numbering: " + rawText + " - " + trackFile.toString() );
				
				} else if ( rawNoWhiteSpace.matches("^[0-9]+/.*") ) {
					//if matches 23/<whatever>
					discNumber = Integer.parseInt( rawNoWhiteSpace.split("/")[0] );
					
					if ( discCount == null || discCount.equals( "" ) ) {
						discCount = Integer.parseInt( rawNoWhiteSpace.split("/")[1] );
					}
						
					System.out.println ( "Bad, but fixable, disc numbering: " + rawText + " - " + trackFile.toString() );
					
				} else {
					throw new NumberFormatException();
				}
				
			} catch ( NumberFormatException e ) {
				if ( ! rawNoWhiteSpace.equals( "" ) ) {
					System.out.println ( "Invalid disc number: " + rawText  + " - " + trackFile.toString() );
				}
			}
		}
	}
	
	private void parseDiscCount ( Tag tag ) {
		
	}
	
	private void parseReleaseType ( Tag tag ) {
		if ( tag != null ) {
			releaseType = tag.getFirst ( FieldKey.MUSICBRAINZ_RELEASE_TYPE );
		}
	}
		
	public String getArtist () {
		return artist;
	}
	
	public String getAlbumArtist() {
		return albumArtist;
	}
	
	public String getYear () {
		return year;
	}
	
	public String getAlbum () {
		String retMe = album;
		
		if ( releaseType != null && !releaseType.equals("") && !releaseType.matches( "(?i:album)" ) ) {
			retMe += " [" + Utils.toReleaseTitleCase( releaseType ) + "]";
		}
		
		
		if ( discSubtitle != null && !discSubtitle.equals( "" ) ) {
			retMe += " (" + discSubtitle + ")";
			
		} else if ( discCount != null && discCount > 1 ) {
			if ( discNumber != null ) retMe += " (Disc " + discNumber + ")";
			
		} else if ( discNumber != null && discNumber > 1 ) { 
			retMe += " (Disc " + discNumber + ")";
		}
		
		return retMe;
	}		
	
	public String getTitle () {
		return title;
	}
	
	public Integer getTrackNumber () {
		return trackNumber;
	}	
	
	public int getLength () {
		return length;
	}
	
	public Path getPath() {
		return trackFile.toPath();
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
	
	public boolean hasAlbum() {
		return hasAlbum;
	}
	
	public boolean equals( Object o ) {
		if ( !( o instanceof Track ) ) return false;
		
		Track compareTo = (Track) o;
		
		return ( compareTo.getPath().toAbsolutePath().equals( getPath().toAbsolutePath() ) );
	}
}






