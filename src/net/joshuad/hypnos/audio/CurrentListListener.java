package net.joshuad.hypnos.audio;

import java.util.List;

import net.joshuad.hypnos.Album;
import net.joshuad.hypnos.Playlist;

public interface CurrentListListener {

	public void albumsSet( List<Album> albums );
	public void playlistsSet ( List<Playlist> playlists );
	public void tracksSet( );
	public void tracksAdded ();
	public void tracksRemoved ();
	public void listCleared ();
	public void listReordered ();
}
