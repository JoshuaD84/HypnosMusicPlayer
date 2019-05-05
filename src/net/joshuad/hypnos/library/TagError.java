package net.joshuad.hypnos.library;
import java.nio.file.Path;

public class TagError {
	
	public enum TagErrorType {
		MISSING_ARTIST ( "No artist name." ),
		MISSING_TITLE ( "No track title." ),
		MISSING_ALBUM ( "No album title." ),
		MISSING_DATE ( "No date." ),
		
		TRACK_NUMBER_EXCESS_WHITESPACE ( "Track # has excess whitespace." ),
		TRACK_NUMBER_HAS_DISC ( "Track # in N/N format." ),
		TRACK_NUMBER_INVALID_FORMAT ( "Track # format invalid." ),
		
		DISC_COUNT_INVALID_FORMAT ( "Disc count format invalid." ),
		
		DISC_NUMBER_EXCESS_WHITESPACE ( "Disc # has excess whitespace." ),
		DISC_NUMBER_HAS_TRACK ( "Disc # in N/N format." ),
		DISC_NUMBER_INVALID_FORMAT ( "Disc # format invalid." ),
		
		CANNOT_READ_TAG ( "Unable to read tag." ); 
		
		
		private String message;
	
		private TagErrorType ( String message ) {
			this.message = message;
		}
		
		public String getMessage () {
			return message;
		}
	}
	
	private TagErrorType type;
	private Track track;
	private String moreInfo;
	
	
	public TagError ( TagErrorType type, Track track ) {
		this.type = type;
		this.track = track;
	}
	
	public TagError ( TagErrorType type, Track track, String moreInfo ) {
		this ( type, track );
		this.moreInfo = moreInfo;
	}
	
	public Path getPath() {
		return track.getPath();
	}
	
	public String getFolderDisplay() {
		return getPath().getParent().toString();
	}
	
	public String getFilenameDisplay() {
		return getPath().getFileName().toString();
	}
	
	public String getMessage() {
		if ( moreInfo == null ) {
			return type.getMessage();
		} else {
			return type.getMessage() + ": " + moreInfo ;
		}
	}
	
	public Track getTrack() {
		return track;
	}
}
