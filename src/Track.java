import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Track {
	private final SimpleStringProperty artist;
	private final SimpleIntegerProperty year;
	private final SimpleStringProperty album;
	private final SimpleStringProperty title;
	private final SimpleIntegerProperty trackNumber;
	private final SimpleIntegerProperty length;
	
	
	Track ( String artist, int year, String album, String title, int trackNumber, int length ) {
		this.artist = new SimpleStringProperty ( artist );
		this.year = new SimpleIntegerProperty ( year );
		this.album = new SimpleStringProperty ( album );
		this.title = new SimpleStringProperty ( title );
		this.trackNumber = new SimpleIntegerProperty ( trackNumber );
		this.length = new SimpleIntegerProperty ( length );
	}
	
	public String getArtist () {
		return artist.get();
	}
	
	public int getYear () {
		return year.get();
	}
	
	public String getAlbum () {
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
}
