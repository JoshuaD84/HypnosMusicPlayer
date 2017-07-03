package net.joshuad.hypnos.fxui;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.control.Label;
import net.joshuad.hypnos.Album;
import net.joshuad.hypnos.Playlist;

public class ListInfoUpdater {
	
	private static final Logger LOGGER = Logger.getLogger( ListInfoUpdater.class.getName() );
	
	private static final String NEW_PLAYLIST_NAME = "Playlist: New";
	private static final String EMPTY_LIST_NAME = "";
	
	public enum ChangeType {
		REORDERED,
		REMOVED,
		ADDED,
		CHANGED,
		CLEARED
	}
	
	enum Mode {
		ALBUM,
		PLAYLIST,
		EMPTY;
	}
	
	String baseString = "";
	
	Label label;
	
	Mode mode = Mode.EMPTY;
	
	public ListInfoUpdater ( Label label ) {
		
		if ( label == null ) {
			throw new IllegalArgumentException ( "ListInfo Label is null." );
		}
		
		this.label = label;
	}
	
	public void albumsLoaded ( List<Album> albums ) {
		
		if ( albums == null ) {
			LOGGER.log( Level.FINE, "Recieved an null album list." );
			return;
		}
		
		if ( albums.size() == 0 ) {
			LOGGER.log( Level.FINE, "Recieved an empty album list." );
			return;
			
		} else if ( albums.size() == 1 ) {
			albumLoaded ( albums.get( 0 ) );
			return;
			
		} else {
			boolean initialized = false;
			String simpleTitle = "", year = "", artist = "";
			boolean differentBaseAlbums = false;
			
			for ( int k = 0; k < albums.size(); k++ ) {
				Album album = albums.get( k );
				
				if ( album == null ) {
					LOGGER.log( Level.FINE, "Recieved a null album." );
					continue;
				}
				
				if ( !initialized ) {
					simpleTitle = album.getSimpleTitle();
					year = album.getYear();
					artist = album.getAlbumArtist();
					initialized = true;
					
				} else {
					if ( !album.getSimpleTitle().equals( simpleTitle ) 
					||   !album.getAlbumArtist().equals( artist )
					||   !album.getYear().equals( year )
					){
						differentBaseAlbums = true;
					}
				}
			}
			
			if ( !initialized ) {
				tracksLoaded();
				
			} else if ( differentBaseAlbums ) {
				tracksLoaded();
				
			} else {
				baseString = "Album: " + artist + " - " + year + " - " + simpleTitle;
				label.setText( baseString );
				mode = Mode.ALBUM;
			}
		}
	}
	
	public void albumLoaded ( Album album ) {
		if ( album == null ) {
			LOGGER.log( Level.FINE, "Recieved a null album." );
			return;
		}
		
		baseString = "Album: " + album.getAlbumArtist() + " - " + album.getYear() + " - " + album.getFullTitle();
		label.setText( baseString );
		mode = Mode.ALBUM;
	}

	public void tracksLoaded ( ) { 
		baseString = NEW_PLAYLIST_NAME;
		label.setText( baseString + " *");
		mode = Mode.PLAYLIST;
	}
	
	public void tracksAdded () {
		if ( mode == Mode.PLAYLIST ) {
			label.setText( baseString + " *" );
			
		} else {
			tracksLoaded();
		}
	}
	
	public void tracksRemoved() {
		if ( mode == Mode.PLAYLIST ) {
			label.setText( baseString + " *" );
			
		} else {
			baseString = NEW_PLAYLIST_NAME;
			label.setText( baseString + " *");
			mode = Mode.PLAYLIST;
		}
	}

	public void playlistLoaded ( Playlist playlist ) {
		if ( playlist == null )  {
			LOGGER.log( Level.FINE, "Recieved a null playlist." );
			return;
		}
		
		baseString = "Playlist: " + playlist.getName();
		label.setText( baseString );
		mode = Mode.PLAYLIST;
	}
	
	public void playlistsLoaded ( List<Playlist> playlists ) {
		if ( playlists == null ) {
			LOGGER.log( Level.FINE, "Recieved an null playlist list." );
			return;
		}
		
		if ( playlists.size() == 0 ) {
			LOGGER.log( Level.FINE, "Recieved an empty playlists list." );
			return;
			
		} else if ( playlists.size() == 1 ) {
			playlistLoaded ( playlists.get( 0 ) );
			return;
			
		} else {
			tracksLoaded();
		}
	}
	
	public void listCleared () {
		label.setText( EMPTY_LIST_NAME );
		mode = Mode.EMPTY;
	}	
	
	public void listReordered() {
		label.setText( baseString + " *" );
	}
}
