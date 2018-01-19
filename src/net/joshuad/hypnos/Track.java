package net.joshuad.hypnos;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.StandardArtwork;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.joshuad.hypnos.MultiFileImageTagPair.ImageFieldKey;
import net.joshuad.hypnos.TagError.TagErrorType;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;

public class Track implements Serializable, AlbumInfoSource {
	
	private static final long serialVersionUID = 1L;
	public static final int NO_TRACK_NUMBER = -885533;
	
	private static transient final Logger LOGGER = Logger.getLogger( Track.class.getName() );
	
	private transient Vector <TagError> tagErrors = new Vector <TagError> ();

	public enum Format {
		FLAC ( "flac" ),
		MP3 ( "mp3" ),
		OGG ( "ogg" ),
		WAV ( "wav" ),
		M4A ( "m4a" ),
		M4B ( "m4b" ),
		M4R ( "m4r" ),
		AAC ( "AAC" ),
		
		UNKNOWN ( "" );
		
		final String extension;
		Format ( String extension ) {
			this.extension = extension;
		}
		
		public String getExtension () {
			return extension;
		}
	}
	
	public enum ArtistTagImagePriority { 
		GENERAL ( 0 ), 
		ALBUM ( 1 ), 
		TRACK ( 2 );
		
		
		private int value;
		
		ArtistTagImagePriority ( int value ) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
	};

	private int length = 0;
	private File trackFile;
	private boolean hasAlbum = false;
	
	File albumDirectory = null;
	
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
	
	private boolean isLossless = false;
	private long bitRate = -1;
	private int sampleRate = -1;
	private boolean isVBR = false;
	private String encodingType = "";
	private String format = "";
	
	private long diskTimeStamp = 0;
	
	private static final DirectoryStream.Filter<Path> imageFileFilter = new DirectoryStream.Filter<Path>() {
		@Override
		public boolean accept ( Path entry ) throws IOException {
			return Utils.isImageFile ( entry );			
		}
	};
	
	public Track ( Track track ) {
		this.length = track.length;
		this.trackFile = track.trackFile;
		this.hasAlbum = track.hasAlbum;
		this.albumDirectory = track.albumDirectory;
		this.artist = track.artist;
		this.title = track.title;
		this.album = track.album;
		this.date = track.date;
		this.originalDate = track.originalDate;
		this.trackNumber = track.trackNumber;
		this.discSubtitle = track.discSubtitle;
		this.discNumber = track.discNumber;
		this.discCount = track.discCount;
		this.releaseType = track.releaseType;
		this.isLossless = track.isLossless;
		this.bitRate = track.bitRate;
		this.sampleRate = track.sampleRate;
		this.isVBR = track.isVBR;
		this.encodingType = track.encodingType;
		this.format = track.format;
		this.diskTimeStamp = track.diskTimeStamp;
		
		/* REFACTOR - Something like this, to avoid programmer mistakes. 
		   for ( Field field : Track.class.getFields() ) {
			if ( Modifier.isTransient( field.getModifiers() ) ) continue;
			if ( Modifier.isStatic( field.getModifiers() ) ) continue;
			
			try {
				Object thisValue = Track.class.getField( field.getName() ).get ( this );
				Track.class.getField( field.getName() ).set ( this, thisValue );
				
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (NoSuchFieldException e) {
			} catch (SecurityException e) {
			}
		}*/
	}
	
	public Track ( Path trackPath ) {
		this.trackFile = trackPath.toFile();
		refreshTagData();
	}
	
	public Track ( Path trackPath, Path albumPath ) {
		this ( trackPath );
		if ( albumPath != null ) {
			this.albumDirectory = albumPath.toFile();
		}
	}
	
	public Path getAlbumPath() {
		if ( albumDirectory == null ) {
			return null;
		} else {
			return albumDirectory.toPath();
		}
	}
		
	public List <TagError> getTagErrors () {
		return tagErrors;
	}
	
	public void refreshTagData() {
		Tag tag = null;
		Logger.getLogger( "org.jaudiotagger" ).setLevel( Level.OFF ); 
		
		try {
			AudioFile audioFile = getAudioFile();
			AudioHeader audioHeader = audioFile.getAudioHeader();
			length = audioHeader.getTrackLength();
			tag = audioFile.getTag();
			
			isLossless = audioHeader.isLossless();
			bitRate = audioHeader.getBitRateAsNumber();
			sampleRate = audioHeader.getSampleRateAsNumber();
			isVBR = audioHeader.isVariableBitRate();
			encodingType = audioHeader.getEncodingType();
			format = audioHeader.getFormat();
			
			tagErrors.clear();

			parseArtist( tag );
			parseTitle( tag ); 
			parseAlbum( tag );
			parseDate( tag );
			parseTrackNumber( tag );
			parseDiscInfo( tag );
			parseReleaseType( tag );	
		} catch ( Exception e ) {
			tagErrors.add( new TagError ( TagErrorType.CANNOT_READ_TAG, this ) );
		}

		parseFileName();
		
		this.diskTimeStamp = trackFile.lastModified();
	}
	
	
	//TODO: i made this and then stopped coding for the day. Pick up from here or remove it. 
	public boolean needsRefreshFromDisk ( ) {
		return diskTimeStamp != trackFile.lastModified();
	}
	
	private AudioFile getAudioFile() throws IOException, CannotReadException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
		
		int i = trackFile.toString().lastIndexOf('.');
		String extension = "";
		if( i > 0 ) {
		    extension = trackFile.toString().substring(i+1).toLowerCase();
		}
		
		if ( extension.matches( Format.AAC.getExtension() ) || extension.matches( Format.M4R.getExtension() ) ) {
			return AudioFileIO.readAs( trackFile, "m4a" );
			
		} else {
			return AudioFileIO.read( trackFile );
		}

		
	}
	
	private void parseFileName () {
		
		String fnArtist = "";
		String fnYear = "";
		String fnAlbum = "";
		String fnTrackNumber = "";
		String fnTitle = "";
		
		try {
			fnArtist = trackFile.toPath().getParent().getParent().getFileName().toString();
		} catch ( Exception e ) { 
			//No need to log this
		}
		
		try {
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
			//TODO: parse track number
			
			fnTitle = trackFile.toPath().getFileName().toString();

			fnTitle = fnTitle.replaceAll( " - ", "" ).replaceAll( fnArtist, "" ).replaceAll( fnAlbum, "" ).replaceAll( fnYear, "" );

		} catch ( Exception e ) { 
			//PENDING: We get an exception with albums that have [] and maybe {} in their directory structure 
		}

		boolean setByFileName = false;
		//TODO: Some error checking, only do this if we're pretty sure it's good. 
		if ( artist.equals( "" ) && !fnArtist.equals( "" ) ) { artist = fnArtist; setByFileName = true; }
		if ( albumArtist.equals( "" ) && !fnArtist.equals( "" ) ) { albumArtist = fnArtist; setByFileName = true; }
		if ( album.equals( "" ) && !fnAlbum.equals( "" ) ) { album = fnAlbum; setByFileName = true; }
		if ( date.equals( "" ) && !fnYear.equals( "" ) ) { date = fnYear; setByFileName = true; }
		if ( title.equals( "" ) && !fnTitle.equals( "" )  ) { title = fnTitle; setByFileName = true; }
	}
	
	private void parseArtist( Tag tag ) {
		// Do we want to do antyhing with FieldKey.ARTISTS or .ALBUM_ARTISTS or .ALBUM_ARTIST_SORT?
		if ( tag != null ) {
			albumArtist = tag.getFirst ( FieldKey.ALBUM_ARTIST );
			artist = tag.getFirst ( FieldKey.ARTIST );
		}
		
		if ( albumArtist.equals( "" ) ) {
			albumArtist = artist;
		}
		
		if ( artist.equals( "" ) ) {
			tagErrors.add( new TagError ( TagErrorType.MISSING_ARTIST, this ) );
		}
	}
	
	private void parseTitle ( Tag tag ) {
		if ( tag != null ) {
			title = tag.getFirst ( FieldKey.TITLE );
			
			try { 
				if ( title.equals( "" ) ) {
					tagErrors.add( new TagError ( TagErrorType.MISSING_TITLE, this ) );
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
				if ( album.equals( "" ) ) {
					tagErrors.add( new TagError ( TagErrorType.MISSING_ALBUM, this ) );
					
					album = tag.getFirst( FieldKey.ALBUM_SORT );
				}
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
		
		if ( date.equals( "" ) ) {
			tagErrors.add( new TagError ( TagErrorType.MISSING_DATE, this ) );
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
					tagErrors.add( new TagError ( TagErrorType.TRACK_NUMBER_EXCESS_WHITESPACE, this ) );
					
				} else if ( rawText.matches("^[0-9]+/.*") ) {
					trackNumber = Integer.parseInt( rawText.split("/")[0] );
					tagErrors.add( new TagError ( TagErrorType.TRACK_NUMBER_HAS_DISC, this ) );
				
				} else if ( rawNoWhiteSpace.matches("^[0-9]+/.*") ) {
					trackNumber = Integer.parseInt( rawNoWhiteSpace.split("/")[0] );
					tagErrors.add( new TagError ( TagErrorType.TRACK_NUMBER_HAS_DISC, this ) );
					
				} else {
					throw new NumberFormatException();
				}
				
			} catch ( NumberFormatException e ) {
				if ( ! rawNoWhiteSpace.equals( "" ) ) {
					tagErrors.add( new TagError ( TagErrorType.TRACK_NUMBER_INVALID_FORMAT, this, rawText ) );
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
					tagErrors.add( new TagError ( TagErrorType.DISC_COUNT_INVALID_FORMAT, this , tag.getFirst ( FieldKey.DISC_TOTAL ) ) );
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
					tagErrors.add( new TagError ( TagErrorType.DISC_NUMBER_EXCESS_WHITESPACE, this ) );
					
				} else if ( rawText.matches("^[0-9]+/.*") ) {//if matches 23/<whatever>
					discNumber = Integer.parseInt( rawText.split("/")[0] );
					
					if ( discCount == null || discCount.equals( "" ) ) {
						discCount = Integer.parseInt( rawText.split("/")[1] );
					}

					tagErrors.add( new TagError ( TagErrorType.DISC_NUMBER_HAS_TRACK, this ) );
				
				} else if ( rawNoWhiteSpace.matches("^[0-9]+/.*") ) {
					//if matches 23/<whatever>
					discNumber = Integer.parseInt( rawNoWhiteSpace.split("/")[0] );
					
					if ( discCount == null || discCount.equals( "" ) ) {
						discCount = Integer.parseInt( rawNoWhiteSpace.split("/")[1] );
					}

					tagErrors.add( new TagError ( TagErrorType.DISC_NUMBER_HAS_TRACK, this ) );
					
				} else {
					throw new NumberFormatException();
				}
				
			} catch ( NumberFormatException e ) {
				if ( ! rawNoWhiteSpace.equals( "" ) ) {
					tagErrors.add( new TagError ( TagErrorType.DISC_NUMBER_INVALID_FORMAT, this, rawText ) );
				}
			} catch ( UnsupportedOperationException e ) {
				//No problem, it doesn't exist for this file format
			}
		}
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
	
	public String getAlbumTitle () {
		return album;
	}
	
	public String getFullAlbumTitle () {
		String retMe = album;
		
		if ( discSubtitle != null && !discSubtitle.equals( "" ) ) {
			retMe += " (" + discSubtitle + ")";
			
		} else if ( discCount != null && discCount > 1 ) {
			if ( discNumber != null ) retMe += " (Disc " + discNumber + ")";
			
		} else if ( discNumber != null && discNumber > 1 ) { 
			retMe += " (Disc " + discNumber + ")";
		}
		
		if ( releaseType != null && !releaseType.equals("") && !releaseType.matches( "(?i:album)" ) ) {
			retMe += " [" + Utils.toReleaseTitleCase( releaseType ) + "]";
		}
		
		return retMe;
	}		
	
	public Integer getDiscNumber() {
		return discNumber;
	}
	
	public Integer getDiscCount() {
		return discCount;
	}
	
	public String getReleaseType () {
		if ( releaseType != null && !releaseType.matches( "(?i:album)" ) ) {
			return Utils.toReleaseTitleCase( releaseType );
		} else {
			return null;
		}
	}
	
	public String getDiscSubtitle () {
		return discSubtitle;
	}
	
	public String getTitle () {
		return title;
	}
	
	public Integer getTrackNumber () {
		return trackNumber;
	}	
	
	public int getLengthS () {
		return length;
	}
	
	public Path getPath() {
		return trackFile.toPath();
	}
	
	public String getFilename() {
		return trackFile.getName();
	}
	
	public String getLengthDisplay () {
		return Utils.getLengthDisplay( getLengthS () );
	}
	
	public String getShortEncodingString() {
		if ( encodingType.toLowerCase().matches( "mp3" ) ) {
			if ( isVBR ) {
				return "MP3 VBR";
			} else {
				return "MP3 " + bitRate;
			}
		} else if ( encodingType.toLowerCase().matches( "flac" ) ) {
			return encodingType;
		} else if ( encodingType.toLowerCase().matches( "aac" ) ) {			
			if ( isVBR ) {
				return "AAC VBR";
			} else {
				return "AAC " + bitRate;
			}
		} else {
			return encodingType;
		}
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
			
		} else if ( testExtension.equals( Format.M4A.getExtension() ) ) {
			return Format.M4A;
			
		} else if ( testExtension.equals( Format.WAV.getExtension() ) ) {
			return Format.WAV;
			
		} else if ( testExtension.equals( Format.M4B.getExtension() ) ) {
			return Format.M4B;
			
		} else if ( testExtension.equals( Format.M4R.getExtension() ) ) {
			return Format.M4R;
			
		} else if ( testExtension.equals( Format.AAC.getExtension() ) ) {
			return Format.AAC;
		
		} else {
			return Format.UNKNOWN;
		}
	}
	
	public boolean hasAlbumDirectory() {
		return albumDirectory != null;
	}
		
	public boolean equals( Object o ) {
		if ( !( o instanceof Track ) ) return false;
		
		Track compareTo = (Track) o;
		
		return ( compareTo.getPath().toAbsolutePath().equals( getPath().toAbsolutePath() ) );
	}

	public static void saveArtistImageToTag ( File file, Path imagePath, ArtistTagImagePriority priority, boolean overwriteAll, AudioSystem audioSystem ) {
		try {
			byte[] imageBuffer = Files.readAllBytes( imagePath );
			saveImageToID3 ( file, imageBuffer, 8, priority, overwriteAll, audioSystem );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to read image data from file" + imagePath, e );
		}
	}
	
	public static void saveArtistImageToTag ( File file, byte[] buffer, ArtistTagImagePriority priority, boolean overwriteAll, AudioSystem audioSystem ) {
		saveImageToID3 ( file, buffer, 8, priority, overwriteAll, audioSystem );
	}
	

	public static void saveAlbumImageToTag ( File file, byte[] buffer, AudioSystem audioSystem ) {
		saveImageToID3 ( file, buffer, 3, null, true, audioSystem );
	}
		
	private static void deleteImageFromID3 ( File file, int type, AudioSystem audioSystem ) { 
		try {
			AudioFile audioFile = AudioFileIO.read( file );
			Tag tag = audioFile.getTag();

			if ( tag instanceof ID3v1Tag ) {
				tag = new ID3v23Tag ( (ID3v1Tag)tag );
			}

			tag.deleteField( FieldKey.CUSTOM4 );

			List <Artwork> artworkList = tag.getArtworkList();

			tag.deleteArtworkField();
			
			for ( Artwork artwork : artworkList ) {
				if ( artwork.getPictureType() != type ) {
					tag.addField( artwork );
				}
			}
			
			boolean pausedPlayer = false;
			long currentPositionMS = 0;
			Track currentTrack = null;
			
			if ( audioSystem.getCurrentTrack() != null && audioSystem.getCurrentTrack().getPath().equals( file.toPath() ) && audioSystem.isPlaying() ) {

				currentPositionMS = audioSystem.getPositionMS();
				currentTrack = audioSystem.getCurrentTrack();
				audioSystem.stop( StopReason.WRITING_TO_TAG );
				pausedPlayer = true;
			}
				
			audioFile.setTag( tag );
			AudioFileIO.write( audioFile );
			
			if ( pausedPlayer ) {
				audioSystem.playTrack( currentTrack, true );
				audioSystem.seekMS( currentPositionMS );
				audioSystem.unpause();
			}
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to delete image from tag: " + file, e );
		}
		
	}
	
	private static void saveImageToID3 ( File file, byte[] buffer, int type, ArtistTagImagePriority priority, boolean overwriteAll, AudioSystem audioSystem ) {

		try {
			AudioFile audioFile = AudioFileIO.read( file );
			Tag tag = audioFile.getTag();

			if ( tag instanceof ID3v1Tag ) {
				tag = new ID3v23Tag ( (ID3v1Tag)tag );
			}
			
			if ( priority != null && !overwriteAll ) {
				Integer currentPriority = null;
				
				try {
					currentPriority = ArtistTagImagePriority.valueOf( tag.getFirst( FieldKey.CUSTOM4 ) ).getValue();
				} catch ( Exception e ) {
					currentPriority = null;
				}
				
				if ( currentPriority != null && currentPriority > priority.getValue() ) {
					LOGGER.info( file.getName() + ": Not overwriting tag. Selected priority (" 
						+ priority.getValue() + ") " + "is less than current priority (" + currentPriority + ")." );

					return;
				}
			}

			List <Artwork> artworkList = tag.getArtworkList();

			tag.deleteArtworkField();
			
			for ( Artwork artwork : artworkList ) {
				if ( artwork.getPictureType() != type ) {
					tag.addField( artwork );
				}
			}

			Artwork artwork = new StandardArtwork();
			artwork.setBinaryData( buffer );
			artwork.setPictureType( type ); 
			
			tag.addField( artwork );
			
			if ( priority != null ) {
				tag.deleteField( FieldKey.CUSTOM4 );
				tag.addField( FieldKey.CUSTOM4, priority.toString() );
			}
			
			boolean pausedPlayer = false;
			long currentPositionMS = 0;
			Track currentTrack = null;
			
			if ( audioSystem.getCurrentTrack() != null && audioSystem.getCurrentTrack().getPath().equals( file.toPath() ) && audioSystem.isPlaying() ) {

				currentPositionMS = audioSystem.getPositionMS();
				currentTrack = audioSystem.getCurrentTrack();
				audioSystem.stop( StopReason.WRITING_TO_TAG );
				pausedPlayer = true;
			}
				
			audioFile.setTag( tag );
			AudioFileIO.write( audioFile );
			
			if ( pausedPlayer ) {
				audioSystem.playTrack( currentTrack, true );
				audioSystem.seekMS( currentPositionMS );
				audioSystem.unpause();
			}
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to write image to tag: " + file, e );
		}
	}
	
	public void setAndSaveAlbumImage ( Path imagePath, AudioSystem audioSystem ) {
		try {
			byte[] imageBuffer = Files.readAllBytes( imagePath );
			setAndSaveAlbumImage ( imageBuffer, audioSystem );
		} catch ( IOException e ) {
			LOGGER.log( Level.WARNING, "Unable to read image data from image file (" + imagePath + ") can't set album image for track: " + getPath(), e );
		}
	}
	
	public void setAndSaveAlbumImage ( byte[] buffer, AudioSystem audioSystem ) {
		
		if ( buffer.length < 8 ) return; //png signature is length 8, so might as well use that as an absolute minimum

		saveAlbumImageToTag ( getPath().toFile(), buffer, audioSystem );
		
		if ( hasAlbumDirectory() ) {
			
			String extension = null;
			
			if ( buffer[0] == (byte)0xFF && buffer[1] == (byte)0xD8 ) {
				extension = ".jpg";
				
			} else if ( buffer[0] == (byte)0x89 && buffer[1] == (byte)0x50 && buffer[2] == (byte)0x4e && buffer[3] == (byte)0x47 
			&& buffer[4] == (byte)0x0d && buffer[5] == (byte)0x0A && buffer[6] == (byte)0x1A && buffer[7] == (byte)0x0A ) {
			
				extension = ".png";

			}		
			
			if ( extension == null ) {
				LOGGER.info( "Invalid image file type, not saving." );
			}
			
			Path copyTo = getAlbumPath().resolve( "front" + extension );

			try {
				Files.deleteIfExists( getAlbumPath().resolve( "front.png" ) );
			} catch ( Exception e ) {
				LOGGER.log( Level.WARNING, "Unable to delete previous album cover: " + getAlbumPath().resolve( "front.png" ), e );
			}
			
			try ( FileOutputStream fos = new FileOutputStream ( copyTo.toFile() ) ) {
				
				fos.write( buffer );
				fos.close();

			} catch ( IOException e ) {
				LOGGER.log( Level.WARNING, "Unable to write album image to disk: " + copyTo, e );
			}
			
			Thread updaterThread = new Thread ( () -> {
				
				List <Path> tracks = Utils.getAllTracksInDirectory ( this.getAlbumPath() );
				for ( Path track : tracks ) {
					if ( !track.toAbsolutePath().equals( getPath().toAbsolutePath() ) ) {
						saveAlbumImageToTag ( track.toFile(), buffer, audioSystem );
					}
				}
			});
			updaterThread.setName ( "Album Image Tag Writer" );
			updaterThread.setDaemon( true );
			updaterThread.start();
		}
	}
	
	private Path getPreferredAlbumCoverPath () {
		if ( hasAlbumDirectory() ) {
			
			if ( this.getPath().getParent() != null ) {
				
				ArrayList <Path> preferredFiles = new ArrayList <Path> ();

				preferredFiles.add( Paths.get ( this.getPath().getParent().toString(), "front.png" ) );
				preferredFiles.add( Paths.get ( this.getPath().getParent().toString(), "front.jpg" ) );
				preferredFiles.add( Paths.get ( this.getPath().getParent().toString(), "cover.png" ) );
				preferredFiles.add( Paths.get ( this.getPath().getParent().toString(), "cover.jpg" ) );
				preferredFiles.add( Paths.get ( this.getPath().getParent().toString(), "album.png" ) );
				preferredFiles.add( Paths.get ( this.getPath().getParent().toString(), "album.jpg" ) );
				
				for ( Path test : preferredFiles ) {
					if ( Files.exists( test ) && Files.isRegularFile( test ) ) {
						return test;
					}
				}
			}
		}
		return null;
	}
	
	private Path getSecondaryAlbumCoverPath () {
		if ( hasAlbumDirectory() ) {
			
			if ( this.getPath().getParent() != null ) {
				ArrayList<Path> otherFiles = new ArrayList<Path>();
				try {
					DirectoryStream <Path> albumDirectoryStream = Files.newDirectoryStream ( this.getPath().getParent(), imageFileFilter );
					for ( Path imagePath : albumDirectoryStream ) { 
						if ( !imagePath.toString().toLowerCase().matches( ".*artist\\.\\w{3,6}$" ) ) {
							otherFiles.add( imagePath ); 
						}
					}
				
				} catch ( Exception e ) {
					LOGGER.log( Level.INFO, "Unable to get directory listing: " + this.getPath().getParent(), e ); 
				}
				
				for ( Path test : otherFiles ) {
					if ( Files.exists( test ) && Files.isRegularFile( test ) ) {
						return test;
					}
				}
			}
		}
		return null;
	}

	public Image getAlbumCoverImage ( ) {
		
		//Get the tag cover image
		//then look to folder for key files
		//then look at tag for any other suitable images
		//then take any image file from the album folder. 
		
		if ( !Files.exists( getPath() ) ) {
			LOGGER.info( "Track file does not exist."  );
			return null;
		}
		
		List<Artwork> artworkList = null;
		try {
			artworkList = getAudioFile().getTag().getArtworkList(); //This line can throw a NPE
		} catch ( Exception e ) {
			//Do nothing, there is no artwork in this file, no big deal. 
		}
		if ( artworkList != null ) {
			try {
				artworkList = getAudioFile().getTag().getArtworkList();
				if ( artworkList != null ) {
					for ( Artwork artwork : artworkList ) {
						if ( artwork.getPictureType() == 3 ) {	//see ID3 specification for why 3
							Image coverImage = SwingFXUtils.toFXImage((BufferedImage) artwork.getImage(), null);
							if ( coverImage != null ) return coverImage;
						} 
					}
				}			
			} catch ( ClosedByInterruptException e ) {
				return null;
				
			} catch ( Exception e ) {
				LOGGER.log( Level.INFO, "Unable to load album image from tag for file: " + getFilename(), e );
			}
		}
		
		Path bestPath = getPreferredAlbumCoverPath ();
		
		if ( bestPath != null ) {
			return new Image( bestPath.toUri().toString() );
		}
		
		artworkList = null;
		try {
			artworkList = getAudioFile().getTag().getArtworkList(); //This line can throw a NPE
		} catch ( Exception e ) {
			//Do nothing, there is no artwork in this file, no big deal. 
		}
		
		try {
			if ( artworkList != null ) {
				for ( Artwork artwork : artworkList ) {
					if ( artwork.getPictureType() == 0 ) {
						Image otherImage = SwingFXUtils.toFXImage((BufferedImage) artwork.getImage(), null);
						if ( otherImage != null ) return otherImage;
					} else if ( artwork.getPictureType() == 6 ) {
						Image mediaImage = SwingFXUtils.toFXImage((BufferedImage) artwork.getImage(), null);
						if ( mediaImage != null ) return mediaImage;
					}
				}
			}			
			
		} catch ( ClosedByInterruptException e ) {
			return null;
			
		} catch ( Exception e ) {
			LOGGER.log( Level.INFO, "Unable to load album image from tag for file: " + getFilename(), e );
		}

		Path otherPaths = getSecondaryAlbumCoverPath ();
		
		if ( otherPaths != null ) {
			return new Image( otherPaths.toUri().toString() );
		}
		
		return null;
	}

	private ArtistTagImagePriority tagImagePriority() {
		try {
			TagField tagImagePriority = getAudioFile().getTag().getFirstField( FieldKey.CUSTOM4 );
			
			return ArtistTagImagePriority.valueOf( tagImagePriority.toString() );
			
		} catch ( Exception e ) {
			return null;
		}
	}
	
	private Image getTagArtistImage () {
		try {
			List<Artwork> artworkList = getAudioFile().getTag().getArtworkList(); 
			if ( artworkList != null ) {
				for ( Artwork artwork : artworkList ) {
					
					if ( artwork.getPictureType() == 8 ) {
						Image artistImage = SwingFXUtils.toFXImage((BufferedImage) artwork.getImage(), null);
						if ( artistImage != null ) return artistImage;
					}
				}
			}			
	
		} catch ( Exception e ) {
			LOGGER.log( Level.INFO, "Unable to load artist image from tag for file: " + getFilename(), e );
		}
		
		return null;
	}
	
	public Image getArtistImage ( ) {
				
		if ( tagImagePriority() == ArtistTagImagePriority.TRACK ) {
			Image tagArtistImage = getTagArtistImage();
			if ( tagArtistImage != null ) return tagArtistImage;
		}
		
		if ( hasAlbumDirectory() ) {
	
			if ( this.getPath().getParent() != null ) {
			
				Path targetPath = this.getPath().toAbsolutePath();
				
				ArrayList <Path> possibleFiles = new ArrayList <Path> ();
				possibleFiles.add( Paths.get ( targetPath.getParent().toString(), "artist.png" ) );
				possibleFiles.add( Paths.get ( targetPath.getParent().toString(), "artist.jpg" ) );
				//possibleFiles.add( Paths.get ( targetPath.getParent().toString(), "artist.gif" ) );
				
				if ( this.getPath().getParent().getParent() != null ) {
					possibleFiles.add( Paths.get ( targetPath.getParent().getParent().toString(), "artist.png" ) );
					possibleFiles.add( Paths.get ( targetPath.getParent().getParent().toString(), "artist.jpg" ) );
					//possibleFiles.add( Paths.get ( targetPath.getParent().getParent().toString(), "artist.gif" ) );
				}
				
				for ( Path test : possibleFiles ) {
					if ( Files.exists( test ) && Files.isRegularFile( test ) ) {
						return new Image( test.toUri().toString() );
					}
				}
			}
		}
		
		if ( tagImagePriority() == ArtistTagImagePriority.ALBUM ) {
			Image tagArtistImage = getTagArtistImage();
			if ( tagArtistImage != null ) return tagArtistImage;
		}
		
		List<Artwork> artworkList = null;
		try {
			artworkList = getAudioFile().getTag().getArtworkList(); //This line can throw a NPE
		} catch ( Exception e ) {
			//Do nothing, there is no artwork in this file, no big deal. 
		}
		
		try {
			if ( artworkList != null ) {
				for ( Artwork artwork : artworkList ) {
					if ( artwork.getPictureType() == 7 ) {
						Image leadArtistImage = SwingFXUtils.toFXImage( (BufferedImage) artwork.getImage(), null );
						if ( leadArtistImage != null ) return leadArtistImage;
					
					} else if ( artwork.getPictureType() == 8 ) {
						Image artistImage = SwingFXUtils.toFXImage((BufferedImage) artwork.getImage(), null);
						if ( artistImage != null ) return artistImage;
						
					} else if ( artwork.getPictureType() == 12 ) {
						Image writerImage = SwingFXUtils.toFXImage( (BufferedImage) artwork.getImage(), null );
						if ( writerImage != null ) return writerImage;
						
					} else if ( artwork.getPictureType() == 13 ) {
						Image logoImage = SwingFXUtils.toFXImage( (BufferedImage) artwork.getImage(), null );
						if ( logoImage != null ) return logoImage;
					}
				}
			}			
			
		} catch ( ClosedByInterruptException e ) {
			return null;
			
		} catch ( Exception e ) {
			LOGGER.log( Level.INFO, "Error when trying to load tag images for file" + getPath(), e );
		}
		return null;
	}
	
	public void updateTagsAndSave ( List<MultiFileTextTagPair> textTagPairs, List<MultiFileImageTagPair> imageTagPairs, AudioSystem audioSystem ) {
		try {
			AudioFile audioFile = AudioFileIO.read( getPath().toFile() );
			Tag tag = audioFile.getTag();
			
			if ( tag == null ) {
				tag = audioFile.createDefaultTag();
				
			} else if ( tag instanceof ID3v1Tag ) {
				tag = new ID3v23Tag ( (ID3v1Tag)tag );
			}
			
			for ( MultiFileTextTagPair tagPair : textTagPairs ) { 
				if ( !tagPair.isMultiValue() ) {
					if ( tagPair.getValue().equals( "" ) ) {
						tag.deleteField( tagPair.getKey() );
					} else {
						tag.setField( tagPair.getKey(), tagPair.getValue() );
					}
				}
			}
			
			audioFile.setTag( tag );
			AudioFileIO.write( audioFile );
			
			for ( MultiFileImageTagPair tagPair : imageTagPairs ) {
				if ( !tagPair.isMultiValue() && tagPair.imageDataChanged() ) {
					
					if ( tagPair.getImageData() == null ) {
						deleteImageFromID3 ( getPath().toFile(), ImageFieldKey.getIndexFromKey( tagPair.getKey() ), audioSystem );
					} else {
						saveImageToID3 ( getPath().toFile(), tagPair.getImageData(), ImageFieldKey.getIndexFromKey( tagPair.getKey() ), 
							ArtistTagImagePriority.TRACK, true, audioSystem );
					}
				}
			}
			
			refreshTagData();
	
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to save updated tag.", e );
		}
	}
	
	private void readObject ( ObjectInputStream in ) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		tagErrors = new Vector <TagError> ();
	}
}






