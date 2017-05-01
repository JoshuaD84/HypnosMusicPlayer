import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Album {
	private final SimpleStringProperty artist;
	private final SimpleIntegerProperty year;
	private final SimpleStringProperty album;
	
	Album ( String artist, int year, String album ) {
		this.artist = new SimpleStringProperty ( artist );
		this.year = new SimpleIntegerProperty ( year );
		this.album = new SimpleStringProperty ( album );
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
}
