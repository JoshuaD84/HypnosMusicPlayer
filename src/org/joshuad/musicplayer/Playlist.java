package org.joshuad.musicplayer;

import java.util.ArrayList;

public class Playlist {
	
	ArrayList <Track> tracks = new ArrayList <Track> ();
	
	String name;
	
	public Playlist ( String name ) {
		this ( name, new ArrayList <Track> () );
	}
	
	public Playlist ( String name, ArrayList <Track> tracks ) {
		this.tracks = tracks;
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public int getLength() {
		return 545;//TODO
	}
	
	public int getSongCount() {
		return tracks.size();
	}
	
	public ArrayList <Track> getTracks () {
		return tracks;
	}
	
	
}
