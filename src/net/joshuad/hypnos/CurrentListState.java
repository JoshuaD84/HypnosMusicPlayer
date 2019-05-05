package net.joshuad.hypnos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.joshuad.hypnos.CurrentList.Mode;
import net.joshuad.hypnos.Hypnos.OS;
import net.joshuad.hypnos.library.Album;
import net.joshuad.hypnos.library.Artist;
import net.joshuad.hypnos.library.Playlist;

public class CurrentListState implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Mode mode;
	private Artist artist;
	private Playlist playlist;
	private List<Album> albums;
	private List<CurrentListTrack> tracks;
	
	public CurrentListState ( List<CurrentListTrack> tracks, Artist artist, List<Album> albums, Playlist playlist, Mode mode ) {
		if ( tracks == null ) {
			this.tracks = null;
		} else {
			this.tracks = new ArrayList<CurrentListTrack> ( tracks );
		}
		
		if ( albums == null ) {
			this.albums = null;
		} else {
			this.albums = new ArrayList<Album> ( albums );
		}
		
		this.artist = artist;
		this.playlist = playlist;
		this.mode = mode;
	}
	
	public Mode getMode() {
		return mode;
	}
	
	public Playlist getPlaylist() {
		return playlist;
	}
	
	public List<Album> getAlbums() {
		return Collections.unmodifiableList( albums );
	}
	
	public Artist getArtist() {
		return artist;
	}
	
	public List<CurrentListTrack> getItems() {
		return Collections.unmodifiableList( tracks );
	}
	
	public String getDisplayString() {

		String retMe = "";
		
		if ( mode == Mode.ARTIST ) {
			retMe = "Artist - " + artist.getName();
		
		} else if ( mode == Mode.ALBUM || mode == Mode.ALBUM_REORDERED ) {
		
			if ( albums.size() == 0 ) {
				
			} else if ( albums.size() == 1 ) {
				Album album = albums.get( 0 );
				retMe = "Album: " + album.getAlbumArtist() + " - " + album.getYear() + " - " + album.getFullAlbumTitle();
				
			} else {
				Album album = albums.get( 0 );
				retMe ="Album: " + album.getAlbumArtist() + " - " + album.getYear() + " - " + album.getAlbumTitle();
			}
			
			if ( mode == Mode.ALBUM_REORDERED ) {
				if ( Hypnos.getOS() == OS.WIN_XP || Hypnos.getOS() == OS.WIN_VISTA || Hypnos.getOS() == OS.WIN_7 ) {
					retMe += " *";
				} else {
					retMe += " 🔀";
				}
			}

		} else if ( mode == Mode.PLAYLIST || mode == Mode.PLAYLIST_UNSAVED ) {
			
			if ( playlist != null ) {
				retMe = "Playlist: " + playlist.getName();
			} else {
				retMe = "Playlist: New";
			}
			
				
			if ( mode == Mode.PLAYLIST_UNSAVED ) {
				retMe += " *";
			}
		} 
		
		return retMe;
	}
}
