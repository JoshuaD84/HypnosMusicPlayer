package net.joshuad.musicplayer;

import java.io.Serializable;
import java.util.ArrayList;

public class Playlist implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private ArrayList <Track> tracks;
	
	private String name;
	
	public Playlist ( String name ) {
		this ( name, new ArrayList <Track> () );
	}
	
	public Playlist ( String name, ArrayList <Track> tracks ) {
		setTracks( tracks );
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName ( String newName ) {
		this.name = newName;
	}
	
	public int getLength() {
		int retMe = 0;
		for ( Track track : tracks ) {
			if ( track != null ) retMe += track.getLength ();
		}
		
		return retMe;
	}
	
	public String getLengthDisplay() {
		return Utils.getLengthDisplay ( getLength() );
	}
	
	public int getSongCount() {
		return tracks.size();
	}
	
	public ArrayList <Track> getTracks () {
		return tracks;
	}
	
	public void setTracks( ArrayList <Track> tracks ) {
		this.tracks = tracks;
	}
		
}
