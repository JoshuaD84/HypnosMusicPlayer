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
	public static final int NO_TRACK_NUMBER = -885533;
	
	private static transient final Logger LOGGER = Logger.getLogger( Track.class.getName() );

	public enum Format {
		FLAC ( "flac" ),
		MP3 ( "mp3" ),
		OGG ( "ogg" ),
		WAV ( "wav" ),
		AAC ( "m4a" ),
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
	private String date = "";
	private String originalDate = "";
	private int trackNumber = NO_TRACK_NUMBER;
	private String discSubtitle = null;
	private Integer discNumber = null;
	private Integer discCount =  null;
	private String releaseType = null;
	
	public Track ( Path trackPath ) throws IOException {
		this ( trackPath, false );
	}
	
	//TODO: Deal w/ these exceptions right, don't throw them. Catch them and fill in data as best you can. 
	public Track ( Path trackPath, boolean hasAlbum ) throws IOException {

		Logger.getLogger( "org.jaudiotagger" ).setLevel( Level.OFF );
		this.trackFile = trackPath.toFile();
		this.hasAlbum = hasAlbum;
		
		refreshTagData();
	}
	
	public void refreshTagData() throws IOException {
		Tag tag = null;
		AudioFile audioFile;
		try {
			int i = trackFile.toString().lastIndexOf('.');
			String extension = "";
			if( i > 0 ) {
			    extension = trackFile.toString().substring(i+1).toLowerCase();
			}
			
			if ( extension.matches( "aac" ) || extension.matches( "m4r" ) ) {
				audioFile = AudioFileIO.readAs( trackFile, "m4a" );
				
			} else {
				audioFile = AudioFileIO.read( trackFile );
			}
			
			length = audioFile.getAudioHeader().getTrackLength();
			tag = audioFile.getTag();
			
			parseArtist( tag );
			parseTitle( tag ); 
			parseAlbum( tag );
			parseDate( tag );
			parseTrackNumber( tag );
			parseDiscInfo( tag );
			parseReleaseType( tag );	
			
		} catch ( CannotReadException | TagException | ReadOnlyFileException | InvalidAudioFrameException e1 ) {
			LOGGER.log( Level.INFO, e1.getClass().toString() + " while read tags on file: " + trackFile.toString() + ", using file name." );
		}
		
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
		if ( date.equals( "" ) ) { date = fnYear; setByFileName = true; }
		if ( title.equals( "" ) ) { title = fnTitle; setByFileName = true; }
		
		if ( setByFileName ) {
			if ( Library.SHOW_SCAN_NOTES ) {
				System.out.println ( "Set by filename: " + trackFile );
			}
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
			
			try { 
				if ( title.equals( "" ) ) {
					title = tag.getFirst( FieldKey.TITLE_SORT );
				}
			} catch ( UnsupportedOperationException e ) {
				//No problem, it doesn't exist for this file format
			}
		}	
	}
	
	private void parseAlbum ( Tag tag ) {
		if ( tag != null ) {
			album = tag.getFirst ( FieldKey.ALBUM );
			try { 
				if ( album.equals( "" ) ) album = tag.getFirst( FieldKey.ALBUM_SORT );
			} catch ( UnsupportedOperationException e ) {}
		}
	}
	
	private void parseDate ( Tag tag ) {
		if ( tag != null ) {
			try { 
				originalDate = tag.getFirst ( FieldKey.ORIGINAL_YEAR );
			} catch ( UnsupportedOperationException e ) {}

			try { 
				date = tag.getFirst( FieldKey.YEAR );
			} catch ( UnsupportedOperationException e ) {}
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
					if ( Library.SHOW_SCAN_NOTES ) {
						System.out.println ( "Bad, but fixable, track numbering: " + rawText + " - " + trackFile.toString() );
					}
				
				} else if ( rawNoWhiteSpace.matches("^[0-9]+/.*") ) {
					trackNumber = Integer.parseInt( rawNoWhiteSpace.split("/")[0] );
					if ( Library.SHOW_SCAN_NOTES ) {
						System.out.println ( "Bad, but fixable, track numbering: " + rawText + " - " + trackFile.toString() );
					}
					
				} else {
					throw new NumberFormatException();
				}
				
			} catch ( NumberFormatException e ) {
				if ( ! rawNoWhiteSpace.equals( "" ) ) {
					if ( Library.SHOW_SCAN_NOTES ) {
						System.out.println ( "Invalid track number: " + rawText  + " - " + trackFile.toString() );
					}
				}
			}
		}
	}
	
	private void parseDiscInfo ( Tag tag ) {
		if ( tag != null ) {
			try {
				discSubtitle = tag.getFirst ( FieldKey.DISC_SUBTITLE );
			} catch ( UnsupportedOperationException e ) {
				//No problem, it doesn't exist for this file format
			}
			try {
				discCount = Integer.valueOf( tag.getFirst ( FieldKey.DISC_TOTAL ) );
			} catch ( NumberFormatException e ) {
				if ( ! tag.getFirst ( FieldKey.DISC_TOTAL ).equals( "" ) ) {
					if ( Library.SHOW_SCAN_NOTES ) {
						System.out.println ( "Invalid disc count: " + tag.getFirst ( FieldKey.DISC_TOTAL )  + " - " + trackFile.toString() );
					}
				}
			} catch ( UnsupportedOperationException e ) {
				//No problem, it doesn't exist for this file format
			}
			
			String rawText = "";
			String rawNoWhiteSpace = "";
			try {
				rawText = tag.getFirst ( FieldKey.DISC_NO );
				rawNoWhiteSpace = rawText.replaceAll("\\s+","");
			
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
						
					if ( Library.SHOW_SCAN_NOTES ) {	
						System.out.println ( "Bad, but fixable, disc numbering: " + rawText + " - " + trackFile.toString() );
					}
				
				} else if ( rawNoWhiteSpace.matches("^[0-9]+/.*") ) {
					//if matches 23/<whatever>
					discNumber = Integer.parseInt( rawNoWhiteSpace.split("/")[0] );
					
					if ( discCount == null || discCount.equals( "" ) ) {
						discCount = Integer.parseInt( rawNoWhiteSpace.split("/")[1] );
					}

					if ( Library.SHOW_SCAN_NOTES ) {	
						System.out.println ( "Bad, but fixable, disc numbering: " + rawText + " - " + trackFile.toString() );
					}
					
				} else {
					throw new NumberFormatException();
				}
				
			} catch ( NumberFormatException e ) {
				if ( ! rawNoWhiteSpace.equals( "" ) ) {
					if ( Library.SHOW_SCAN_NOTES ) {
						System.out.println ( "Invalid disc number: " + rawText  + " - " + trackFile.toString() );
					}
				}
			} catch ( UnsupportedOperationException e ) {
				//No problem, it doesn't exist for this file format
			}
		}
	}
	
	private void parseDiscCount ( Tag tag ) {
		
	}
	
	private void parseReleaseType ( Tag tag ) {
		if ( tag != null ) {
			try {
				releaseType = tag.getFirst ( FieldKey.MUSICBRAINZ_RELEASE_TYPE );
			} catch ( UnsupportedOperationException e ) {
				//No problem, it doesn't exist for this file format
			}
		}
	}
		
	public String getArtist () {
		return artist;
	}
	
	public String getAlbumArtist() {
		return albumArtist;
	}
	
	public String getYear () {
		if ( !originalDate.isEmpty() ) {
			return originalDate;
		} else {
			return date;
		}
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
			
		} else if ( testExtension.equals( Format.OGG.getExtension() ) ) {
			return Format.OGG;
			
		} else if ( testExtension.equals( Format.AAC.getExtension() ) ) {
			return Format.AAC;
			
		} else if ( testExtension.equals( Format.WAV.getExtension() ) ) {
			return Format.WAV;
			
		} else if ( testExtension.equals( "m4b" ) ) { //TODO: do this right
			return Format.AAC;
			
		} else if ( testExtension.equals( "m4r" ) ) { //TODO: do this right
			return Format.AAC;
			
		} else if ( testExtension.equals( "aac" ) ) { //TODO: do this right
			return Format.AAC;
		
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






