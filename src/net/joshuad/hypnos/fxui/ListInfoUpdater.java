package net.joshuad.hypnos.fxui;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.control.Label;
import net.joshuad.hypnos.Album;
import net.joshuad.hypnos.Playlist;
import net.joshuad.hypnos.audio.CurrentListListener;

public class ListInfoUpdater implements CurrentListListener {
	
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
	
	@Override
	public void albumsSet ( List<Album> albums ) {
		if ( albums == null ) {
			LOGGER.log( Level.FINE, "Recieved an null album list." );
			return;
		}
		
		if ( albums.size() == 0 ) {
			LOGGER.log( Level.FINE, "Recieved an empty album list." );
			return;
			
		} else if ( albums.size() == 1 ) {
			albumSet ( albums.get( 0 ) );
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
				tracksSet();
				
			} else if ( differentBaseAlbums ) {
				tracksSet();
				
			} else {
				baseString = "Album: " + artist + " - " + year + " - " + simpleTitle;
				setText( baseString );
				mode = Mode.ALBUM;
			}
		}
	}
	
	private void albumSet ( Album album ) {
		if ( album == null ) {
			LOGGER.log( Level.FINE, "Recieved a null album." );
			return;
		}
		
		baseString = "Album: " + album.getAlbumArtist() + " - " + album.getYear() + " - " + album.getFullTitle();
		setText( baseString );
		mode = Mode.ALBUM;
	}
	
	@Override
	public void tracksSet () { 
		baseString = NEW_PLAYLIST_NAME;
		setText( baseString + " *");
		mode = Mode.PLAYLIST;
	}
	
	@Override
	public void tracksAdded () {
		if ( mode == Mode.PLAYLIST ) {
			setText( baseString + " *" );
			
		} else {
			tracksSet();
		}
	}
	
	@Override
	public void tracksRemoved() {
		if ( mode == Mode.PLAYLIST ) {
			setText( baseString + " *" );
			
		} else {
			baseString = NEW_PLAYLIST_NAME;
			setText( baseString + " *");
			mode = Mode.PLAYLIST;
		}
	}

	private void playlistSet( Playlist playlist ) {
		if ( playlist == null )  {
			LOGGER.log( Level.FINE, "Recieved a null playlist." );
			return;
		}
		
		baseString = "Playlist: " + playlist.getName();
		setText( baseString );
		mode = Mode.PLAYLIST;
	}
	
	@Override
	public void playlistsSet ( List<Playlist> playlists ) {
		if ( playlists == null ) {
			LOGGER.log( Level.FINE, "Recieved an null playlist list." );
			return;
		}
		
		if ( playlists.size() == 0 ) {
			LOGGER.log( Level.FINE, "Recieved an empty playlists list." );
			return;
			
		} else if ( playlists.size() == 1 ) {
			playlistSet ( playlists.get( 0 ) );
			return;
			
		} else {
			tracksSet();
		}
	}
	
	@Override
	public void listCleared () {
		setText( EMPTY_LIST_NAME );
		mode = Mode.EMPTY;
	}	
	
	@Override
	public void listReordered() {
		setText( baseString + " *" );
	}
	
	private void setText ( String string ) {
		label.setText( string );
	}
}
