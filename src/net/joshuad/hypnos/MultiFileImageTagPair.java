package net.joshuad.hypnos;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MultiFileImageTagPair {
	private static transient final Logger LOGGER = Logger.getLogger( MultiFileImageTagPair.class.getName() );
	
	public static final byte[] MULTI_VALUE = new byte[] { 1, 3, 5, 8, 13, 21, 34, 55, 89 };
	
	public enum ImageFieldKey {
		ALBUM_FRONT, ARTIST, 
		ALBUM_OTHER, ALBUM_MEDIA, ARTIST_LEAD, ARTIST_ILLUSTRATION, ARTIST_LOGO;
		
		public static ImageFieldKey getKeyFromTagIndex ( int index ) {
			if ( index == 3 ) return ALBUM_FRONT;
			if ( index == 0 ) return ALBUM_OTHER;
			if ( index == 6 ) return ALBUM_MEDIA;
			if ( index == 8 ) return ARTIST;
			if ( index == 7 ) return ARTIST_LEAD;
			if ( index == 12 ) return ARTIST_ILLUSTRATION;
			if ( index == 13 ) return ARTIST_LOGO;
			
			return null;
		}
		
		public static int getIndexFromKey ( ImageFieldKey key ) {
			switch ( key ) {
				case ALBUM_FRONT: return 3;
				case ALBUM_OTHER: return 0;
				case ALBUM_MEDIA: return 6;
				case ARTIST: return 8;
				case ARTIST_LEAD: return 7;
				case ARTIST_ILLUSTRATION: return 12;
				case ARTIST_LOGO: return 13;
				default: return -1; //Tis can never happen
			}
		}
	}
	
	private ImageFieldKey key;
	private byte[] imageData;
	
	boolean multiValue = false;
	boolean imageDataChanged = false;

	public MultiFileImageTagPair ( ImageFieldKey key, byte[] imageData ) {
		this.key = key;
		this.imageData = imageData;
	}
	
	public void setImageData ( byte[] imageData ) {
		imageDataChanged = true;
		this.imageData = imageData;
		multiValue = false;
	}
	
	public void anotherFileValue ( byte[] newImageData ) {
		if ( !Arrays.equals( imageData, newImageData ) ) {
			multiValue = true;
		}
	}
	
	public boolean isMultiValue () {
		return multiValue;
	}
	
	public boolean imageDataChanged() {
		return imageDataChanged;
	}
	
	public String getTagName () {
		switch ( key ) {
			case ALBUM_FRONT:
				return "COVER";
			case ALBUM_MEDIA:
				return "MEDIA";
			case ALBUM_OTHER:
				return "ALBUM OTHER";
			case ARTIST:
				return "ARTIST";
			case ARTIST_ILLUSTRATION:
				return "ARTIST ILLUSTRATION";
			case ARTIST_LEAD:
				return "ARTIST LEAD";
			case ARTIST_LOGO:
				return "ARTIST LOGO";
			default:
				LOGGER.log ( Level.WARNING, "This should never happen. Key type: " + key.name(), new Exception() );
				return "NO NAME";
		}
	}
	public ImageFieldKey getKey() {
		return key;
	}
	
	public byte[] getImageData () {
		if ( multiValue ) {
			return MULTI_VALUE;
		} else {
			return imageData;
		}
	}
}
