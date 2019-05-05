package net.joshuad.hypnos.fxui;

import java.io.Serializable;
import java.util.List;

import net.joshuad.hypnos.library.Album;
import net.joshuad.hypnos.library.Artist;
import net.joshuad.hypnos.library.Playlist;
import net.joshuad.hypnos.library.Track;

public class DraggedTrackContainer implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public enum DragSource {
		CURRENT_LIST,
		ARTIST_LIST,
		ALBUM_LIST,
		TRACK_LIST,
		PLAYLIST_LIST,
		TAG_ERROR_LIST,
		PLAYLIST_INFO,
		ALBUM_INFO,
		QUEUE,
		HISTORY,
		CURRENT_TRACK
	}
	
	private List<Track> tracks;
	private List<Integer> indices;
	private List<Album> albums;
	private List<Artist> artists;
	private List<Playlist> playlists;
	
	private DragSource source;
	
	public DraggedTrackContainer( List<Integer> indices, List<Track> tracks, List<Album> albums, 
		List<Playlist> playlists, List<Artist> artists, DragSource source ) {
		this.indices = indices;
		this.source = source;
		this.tracks = tracks;
		this.albums = albums;
		this.artists = artists;
		this.playlists = playlists;
	}
	
	public List<Integer> getIndices() {
		return indices;
	}
	
	public DragSource getSource() {
		return source;
	}
	
	public List<Artist> getArtists() {
		return artists;
	}
	
	public List<Track> getTracks() {
		return tracks;
	}
	
	public List<Album> getAlbums() {
		return albums;
	}
	
	public List<Playlist> getPlaylists() {
		return playlists;
	}
}
