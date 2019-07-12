package net.joshuad.hypnos.library;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.joshuad.hypnos.MultiFileImageTagPair;
import net.joshuad.hypnos.MultiFileImageTagPair.ImageFieldKey;
import net.joshuad.hypnos.MultiFileTextTagPair;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;
import net.joshuad.hypnos.lastfm.LastFM.LovedState;
import net.joshuad.hypnos.library.TagError.TagErrorType;

public class Track implements Serializable, AlbumInfoSource {
	private static transient final Logger LOGGER = Logger.getLogger( Track.class.getName() );
	
	private static final long serialVersionUID = 1L;
	public static final int NO_TRACK_NUMBER = -885533;

	public enum Format {
		FLAC ( "flac" ),
		MP3 ( "mp3" ),
		OGG ( "ogg" ),
		ALAC ( "alac" ),
		WAV ( "wav" ),
		M4A ( "m4a" ),
		M4B ( "m4b" ),
		M4R ( "m4r" ),
		AAC ( "AAC" ),
		AIFF ( "aiff" ),
		AC3 ( "ac3" ),
		AMR ( "amr" ),
		AU ( "au" ),
		MKA ( "mka" ),
		RA ( "ra" ),
		VOC ( "voc" ),
		WMA ( "wma" );
		
		final String extension;
		Format ( String extension ) {
			this.extension = extension;
		}
		
		public String getExtension () {
			return extension;
		}
		
		public static Format getFormat ( String extension ) {
			extension = extension.replaceAll( "^\\.", "" );
			for ( Format format : Format.values() ) {
				if ( format.getExtension().equalsIgnoreCase( extension ) ) {
					return format;
				}
			}
			return null;
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
	private boolean isLossless = false;
	private long bitRate = -1;
	private int sampleRate = -1;
	private boolean isVBR = false;
	private String encodingType = "";
	private String format = "";
	private Album album = null;
	private transient StringProperty artist;
	private transient StringProperty albumArtist;
	private transient StringProperty title;
	private transient StringProperty albumTitle;
	private transient StringProperty date;
	private transient StringProperty originalDate;
	private transient IntegerProperty trackNumber;
	private transient StringProperty discSubtitle;
	private transient IntegerProperty discNumber;
	private transient IntegerProperty discCount;
	private transient StringProperty releaseType;
	private transient Vector <TagError> tagErrors;
	private transient LovedState lovedState;
	
	private static final DirectoryStream.Filter<Path> imageFileFilter = new DirectoryStream.Filter<Path>() {
		@Override
		public boolean accept ( Path entry ) throws IOException {
			return Utils.isImageFile ( entry );			
		}
	};
	
	public Track ( Path trackPath ) {
		initializeTransientFields();
		this.trackFile = trackPath.toFile();
		refreshTagData();
	}
	
	public Track ( Track track ) {
		initializeTransientFields();
		setData ( track );
	}
	
	public void setData ( Track track ) {
		this.length = track.length;
		this.trackFile = track.trackFile;
		this.album = track.album;
		this.artist.set(track.artist.get());
		this.title.set(track.title.get());
		this.albumTitle.set(track.albumTitle.get());
		this.date.set(track.date.get());
		this.originalDate.set(track.originalDate.get());
		this.trackNumber.set(track.trackNumber.get());
		this.discSubtitle.set(track.discSubtitle.get());
		this.discNumber.set(track.discNumber.get());
		this.discCount.set(track.discCount.get());
		this.releaseType.set(track.releaseType.get());
		this.isLossless = track.isLossless;
		this.bitRate = track.bitRate;
		this.sampleRate = track.sampleRate;
		this.isVBR = track.isVBR;
		this.encodingType = track.encodingType;
		this.format = track.format;
	}

	public void setAlbum( Album album ) {
		this.album = album;
	}
	
	public Album getAlbum() {
		return album;
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
		//TODO: Look at parsing track number from file name too
		String filenameArtist = "";
		String filenameYear = "";
		String filenameAlbum = "";
		String filenameTitle = "";
		try {
			filenameArtist = trackFile.toPath().getParent().getParent().getFileName().toString();
		} catch ( Exception e ) { 
			//No need to log this
		}
		try {
			String parentName = trackFile.toPath().getParent().getFileName().toString();
			String[] parentParts = parentName.split( " - " );
			if ( parentParts.length == 1 ) {
				if ( album != null ) {
					filenameAlbum = parentParts [ 0 ];
				} else {
					filenameArtist = parentParts [ 0 ];
				}
			} else if ( parentParts.length == 2 ) { 
				if ( parentParts [ 0 ].matches( "^[0-9]{4}[a-zA-Z]{0,1}" ) ) {
					filenameYear = parentParts [ 0 ];
					filenameAlbum = parentParts [ 1 ];
				} else if ( parentParts [ 1 ].matches( "^[0-9]{4}[a-zA-Z]{0,1}" ) ) {
					filenameYear = parentParts [ 1 ];
					filenameAlbum = parentParts [ 0 ];
				} else {
					filenameAlbum = parentName;
				}
			}
			//TODO: parse track number
			filenameTitle = trackFile.toPath().getFileName().toString();
			filenameTitle = filenameTitle.replaceAll(" - ", "").replaceAll(filenameArtist, "").replaceAll(filenameAlbum, "")
					.replaceAll(filenameYear, "");
		} catch ( Exception e ) { 
			//PENDING: We get an exception with albums that have [] and maybe {} in their directory structure 
		}
		//TODO: Add some error checking, only do this if we're pretty sure it's good. 
		if ( artist.get().equals( "" ) && !filenameArtist.equals( "" ) ) { artist.set(filenameArtist); }
		if ( albumArtist.get().equals( "" ) && !filenameArtist.equals( "" ) ) { albumArtist.set(filenameArtist); }
		if ( albumTitle.get().equals( "" ) && !filenameAlbum.equals( "" ) ) { albumTitle.set(filenameAlbum); }
		if ( date.get().equals( "" ) && !filenameYear.equals( "" ) ) { date.set(filenameYear); }
		if ( title.get().equals( "" ) && !filenameTitle.equals( "" )  ) { title.set(filenameTitle); }
	}
	
	private void parseArtist( Tag tag ) {
		// Do we want to do antyhing with FieldKey.ARTISTS or .ALBUM_ARTISTS or .ALBUM_ARTIST_SORT?
		if ( tag != null ) {
			albumArtist.set(tag.getFirst ( FieldKey.ALBUM_ARTIST ));
			artist.set(tag.getFirst ( FieldKey.ARTIST ));
		}
		if ( albumArtist.get().equals( "" ) ) {
			albumArtist.set(artist.get());
		}
		if ( artist.get().equals( "" ) ) {
			tagErrors.add( new TagError ( TagErrorType.MISSING_ARTIST, this ) );
		}
	}
	
	private void parseTitle ( Tag tag ) {
		if ( tag != null ) {
			title.set(tag.getFirst ( FieldKey.TITLE ));
			try { 
				if ( title.get().equals( "" ) ) {
					tagErrors.add( new TagError ( TagErrorType.MISSING_TITLE, this ) );
					title.set(tag.getFirst( FieldKey.TITLE_SORT ));
				}
			} catch ( UnsupportedOperationException e ) {
				//No problem, it doesn't exist for this file format
			}
		}	
	}
	
	private void parseAlbum ( Tag tag ) {
		if ( tag != null ) {
			albumTitle.set(tag.getFirst ( FieldKey.ALBUM ));
			try { 
				if ( albumTitle.get().equals( "" ) ) {
					tagErrors.add( new TagError ( TagErrorType.MISSING_ALBUM, this ) );
					albumTitle.set(tag.getFirst( FieldKey.ALBUM_SORT ));
				}
			} catch ( UnsupportedOperationException e ) {}
		}
	}
	
	private void parseDate ( Tag tag ) {
		if ( tag != null ) {
			try { 
				originalDate.set(tag.getFirst ( FieldKey.ORIGINAL_YEAR ));
			} catch ( UnsupportedOperationException e ) {
				//Do nothing
			}
			try { 
				date.set(tag.getFirst( FieldKey.YEAR ));
			} catch ( UnsupportedOperationException e ) {
				//Do nothing
			}
		}
		if ( date.get().equals( "" ) ) {
			tagErrors.add( new TagError ( TagErrorType.MISSING_DATE, this ) );
		}
	}
	
	private void parseTrackNumber( Tag tag ) {
		if ( tag != null ) {
			String rawText = tag.getFirst ( FieldKey.TRACK );
			String rawNoWhiteSpace = rawText.replaceAll("\\s+","");
			try { 
				if ( rawText.matches( "^[0-9]+$" ) ) { // 0, 01, 1010, 2134141, etc.
					trackNumber.set(Integer.parseInt( rawText ));
				} else if ( rawNoWhiteSpace.matches( "^[0-9]+$" ) ) { 
					trackNumber.set(Integer.parseInt( rawNoWhiteSpace ));
					tagErrors.add( new TagError ( TagErrorType.TRACK_NUMBER_EXCESS_WHITESPACE, this ) );
				} else if ( rawText.matches("^[0-9]+/.*") ) {
					trackNumber.set(Integer.parseInt( rawText.split("/")[0] ));
					tagErrors.add( new TagError ( TagErrorType.TRACK_NUMBER_HAS_DISC, this ) );
				} else if ( rawNoWhiteSpace.matches("^[0-9]+/.*") ) {
					trackNumber.set(Integer.parseInt( rawNoWhiteSpace.split("/")[0] ));
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
				discSubtitle.set(tag.getFirst ( FieldKey.DISC_SUBTITLE ));
			} catch ( UnsupportedOperationException e ) {
				//No problem, it doesn't exist for this file format
			}
			try {
				discCount.set(Integer.valueOf( tag.getFirst ( FieldKey.DISC_TOTAL ) ));
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
					discNumber.set(Integer.parseInt( rawText ));
				} else if ( rawNoWhiteSpace.matches( "^[0-9]+$" ) ) { 
					discNumber.set(Integer.parseInt( rawNoWhiteSpace ));
					tagErrors.add( new TagError ( TagErrorType.DISC_NUMBER_EXCESS_WHITESPACE, this ) );
				} else if ( rawText.matches("^[0-9]+/.*") ) {//if matches 23/<whatever>
					discNumber.set(Integer.parseInt( rawText.split("/")[0] ));
					if ( discCount == null ) {
						discCount.set(Integer.parseInt( rawText.split("/")[1] ));
					}
					tagErrors.add( new TagError ( TagErrorType.DISC_NUMBER_HAS_TRACK, this ) );
				} else if ( rawNoWhiteSpace.matches("^[0-9]+/.*") ) {
					//if matches 23/<whatever>
					discNumber.set(Integer.parseInt( rawNoWhiteSpace.split("/")[0] ));
					if ( discCount == null ) {
						discCount.set(Integer.parseInt( rawNoWhiteSpace.split("/")[1] ));
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
				releaseType.set(tag.getFirst ( FieldKey.MUSICBRAINZ_RELEASE_TYPE ));
			} catch ( UnsupportedOperationException e ) {
				//No problem, it doesn't exist for this file format
			}
		}
	}
		
	public String getArtist () {
		return artist.get();
	}
	
	public String getAlbumArtist() {
		return albumArtist.get();
	}
	
	public String getYear () {
		if ( !originalDate.get().isEmpty() ) {
			return originalDate.get();
		} else {
			return date.get();
		}
	}
	
	public String getAlbumTitle () {
		return albumTitle.get();
	}
	
	public String getFullAlbumTitle () {
		String retMe = albumTitle.get();
		if ( discSubtitle != null && !discSubtitle.get().equals( "" ) ) {
			retMe += " (" + discSubtitle + ")";
		} else if ( discCount != null && discCount.get() > 1 ) {
			if ( discNumber != null ) retMe += " (Disc " + discNumber + ")";
		} else if ( discNumber != null && discNumber.get() > 1 ) { 
			retMe += " (Disc " + discNumber + ")";
		}
		if ( releaseType.get() != null && !releaseType.get().equals("") && !releaseType.get().matches( "(?i:album)" ) ) {
			retMe += " [" + Utils.toReleaseTitleCase( releaseType.get() ) + "]";
		}
		return retMe;
	}		
	
	public Integer getDiscNumber() {
		return discNumber.get();
	}
	
	public Integer getDiscCount() {
		return discCount.get();
	}
	
	public String getReleaseType () {
		if ( releaseType.get() != null && !releaseType.get().matches( "(?i:album)" ) ) {
			return Utils.toReleaseTitleCase( releaseType.get() );
		} else {
			return null;
		}
	}
	
	public String getDiscSubtitle () {
		return discSubtitle.get();
	}
	
	public StringProperty getTitleProperty() {
		return title;
	}
	
	public StringProperty getArtistProperty() {
		return artist;
	}
	
	public StringProperty getAlbumArtistProperty() {
		return albumArtist;
	}
	
	public StringProperty getAlbumTitleProperty() {
		return albumTitle;
	}
	
	public IntegerProperty getTrackNumberProperty() {
		return trackNumber;
	}
	
	public String getTitle () {
		return title.get();
	}
	
	public Integer getTrackNumber () {
		return trackNumber.get();
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
	
	public boolean equals( Object o ) {
		if ( !( o instanceof Track ) ) return false;
		Track compareTo = (Track) o;
		return ( compareTo.getPath().toAbsolutePath().equals( getPath().toAbsolutePath() ) );
	}
	
	public int hashCode() {
		return getPath().hashCode();
	}
	
	// TODO: It's not clear to me any more that these methods belong in this class. 
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
		if ( buffer.length < 8 ) {
			return; //png signature is length 8, so might as well use that as an absolute minimum
		}
		saveAlbumImageToTag ( getPath().toFile(), buffer, audioSystem );
		if ( album != null ) {
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
			Path copyTo = album.getPath().resolve( "front" + extension );
			try {
				Files.deleteIfExists( album.getPath().resolve( "front.png" ) );
			} catch ( Exception e ) {
				LOGGER.log( Level.WARNING, "Unable to delete previous album cover: " + album.getPath().resolve( "front.png" ), e );
			}
			try ( FileOutputStream fos = new FileOutputStream ( copyTo.toFile() ) ) {
				fos.write( buffer );
				fos.close();
			} catch ( IOException e ) {
				LOGGER.log( Level.WARNING, "Unable to write album image to disk: " + copyTo, e );
			}
			Thread updaterThread = new Thread ( () -> {
				List <Path> tracks = Utils.getAllTracksInDirectory ( album.getPath() );
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
		if ( album != null ) {
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
		if ( album != null ) {
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
		if ( this.getPath().getParent() != null ) {
			Path targetPath = this.getPath().toAbsolutePath();
			ArrayList <Path> possibleFiles = new ArrayList <Path> ();
			possibleFiles.add( Paths.get ( targetPath.getParent().toString(), "artist.png" ) );
			possibleFiles.add( Paths.get ( targetPath.getParent().toString(), "artist.jpg" ) );
			if ( album != null ) {
				if ( this.getPath().getParent().getParent() != null ) {
					possibleFiles.add( Paths.get ( targetPath.getParent().getParent().toString(), "artist.png" ) );
					possibleFiles.add( Paths.get ( targetPath.getParent().getParent().toString(), "artist.jpg" ) );
				}
			}
			for ( Path test : possibleFiles ) {
				if ( Files.exists( test ) && Files.isRegularFile( test ) ) {
					return new Image( test.toUri().toString() );
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
						if ( leadArtistImage != null ) {
							return leadArtistImage;
						}
					} else if ( artwork.getPictureType() == 8 ) {
						Image artistImage = SwingFXUtils.toFXImage((BufferedImage) artwork.getImage(), null);
						if ( artistImage != null ) {
							return artistImage;
						}
					} else if ( artwork.getPictureType() == 12 ) {
						Image writerImage = SwingFXUtils.toFXImage( (BufferedImage) artwork.getImage(), null );
						if ( writerImage != null ) {
							return writerImage;
						}
					} else if ( artwork.getPictureType() == 13 ) {
						Image logoImage = SwingFXUtils.toFXImage( (BufferedImage) artwork.getImage(), null );
						if ( logoImage != null ) {
							return logoImage;
						}
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
			if(getAlbum() != null) {
				getAlbum().updateData();
			}
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to save updated tag.", e );
		}
	}
	
	private void writeObject ( ObjectOutputStream out ) throws IOException {
		out.defaultWriteObject();
		out.writeObject(artist.get());
		out.writeObject(albumArtist.get());
		out.writeObject(title.get());
		out.writeObject(albumTitle.get());
		out.writeObject(date.get());
		out.writeObject(originalDate.get());
		out.writeInt(trackNumber.get());
		out.writeObject(discSubtitle.get());
		out.writeInt(discNumber.get());
		out.writeInt(discCount.get());
		out.writeObject(releaseType.get());
	}
	
	private void readObject ( ObjectInputStream in ) throws IOException, ClassNotFoundException {
		initializeTransientFields();
		in.defaultReadObject();
		artist.set((String)in.readObject());
		albumArtist.set((String)in.readObject());
		title.set((String)in.readObject());
		albumTitle.set((String)in.readObject());
		date.set((String)in.readObject());
		originalDate.set((String)in.readObject());
		trackNumber.set(in.readInt());
		discSubtitle.set((String)in.readObject());
		discNumber.set(in.readInt());
		discCount.set(in.readInt());
		releaseType.set((String)in.readObject());
	}

	public LovedState getLovedState () {
		return lovedState;
	}

	public void setLovedState ( LovedState state ) {
		this.lovedState = state;
	}
	
	private void initializeTransientFields() {
		artist = new SimpleStringProperty("");
		albumArtist = new SimpleStringProperty("");
		title = new SimpleStringProperty("");
		albumTitle = new SimpleStringProperty("");
		date = new SimpleStringProperty("");
		originalDate = new SimpleStringProperty("");
		trackNumber = new SimpleIntegerProperty(NO_TRACK_NUMBER);
		discSubtitle = new SimpleStringProperty("");
		discNumber = new SimpleIntegerProperty();
		discCount = new SimpleIntegerProperty();
		releaseType = new SimpleStringProperty("");
		tagErrors = new Vector <TagError> ();
		lovedState = LovedState.NOT_SET;
	}
}

