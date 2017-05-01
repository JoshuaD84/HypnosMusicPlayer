import java.io.IOException;
import java.nio.file.Path;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Track {
	//TODO: Do these need to be final? 
	private SimpleStringProperty artist;
	private SimpleStringProperty year;
	private SimpleStringProperty album;
	private SimpleStringProperty title;
	private SimpleStringProperty trackNumber;
	private SimpleIntegerProperty length;
	private Path trackPath;
	
	Track ( Path trackPath ) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
		
		this.trackPath = trackPath;
		AudioFile audioFile = AudioFileIO.read( trackPath.toFile() );
		Tag tag = audioFile.getTag();
		//TODO: what to do if no tag present? 
		artist = new SimpleStringProperty ( tag.getFirst ( FieldKey.ARTIST ) );
		year = new SimpleStringProperty ( tag.getFirst ( FieldKey.YEAR ) );
		album = new SimpleStringProperty ( tag.getFirst ( FieldKey.ALBUM ) );
		title = new SimpleStringProperty ( tag.getFirst ( FieldKey.TITLE ) );
		trackNumber = new SimpleStringProperty ( tag.getFirst ( FieldKey.TRACK ) );
		length = new SimpleIntegerProperty ( audioFile.getAudioHeader().getTrackLength() );			
	 
	}
	
	public String getArtist () {
		return artist.get();
	}
	
	public String getYear () {
		return year.get();
	}
	
	public String getAlbum () {
		return album.get();
	}		
	
	public String getTitle () {
		return title.get();
	}
	
	public String getTrackNumber () {
		return trackNumber.get();
	}	
	
	public int getLength () {
		return length.get();
	}
	
	public Path getPath() {
		return trackPath;
	}
	
	public String getLengthDisplay () {
		int hours = getLength() / 3600;
		int minutes = ( getLength() % 3600 ) / 60;
		int seconds = getLength() % 60;
		
		if ( hours > 0 ) {
			return String.format ( "%d:%02d:%02d", hours, minutes, seconds );
		} else {
			return String.format ( "%d:%02d", minutes, seconds );
		}
	}
}
