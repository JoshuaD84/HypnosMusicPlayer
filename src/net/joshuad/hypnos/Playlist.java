package net.joshuad.hypnos;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
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

	public static Playlist loadPlaylist ( Path path ) {
		if ( path.toString().toLowerCase().endsWith( ".m3u" ) ) {
			
			Playlist playlist = new Playlist( "NoName" );
			
			try (
					FileReader fileReader = new FileReader( path.toFile() );
			) {
				BufferedReader m3uIn = new BufferedReader ( fileReader );
				for ( String line; (line = m3uIn.readLine()) != null; ) {
					if ( line.startsWith( "#Name:" ) ) {
						String name = line.split( ":" )[1]; //TODO: OOB error checking on index
						playlist.setName( name );
					} else if ( line.isEmpty() ) {
						//Do nothing
						
					} else if ( !line.startsWith( "#" ) ) {
						try {
							playlist.addTrack ( new Track ( Paths.get ( line ) ) );
						} catch ( Exception e ) {
							System.out.println ( "Error parsing line in playlist: " + path.toString() + ", continuing." );
							System.out.println ( "\tLine: " + line );
						}
					}
						
						
				}
			} catch ( Exception e ) {
				System.out.println ( "Error loading: " + path.toString() );
				e.printStackTrace();
				return null;
			}
			return playlist;
		}
		return null;
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
			if ( track != null ) retMe += track.getLengthS ();
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

	public void addTrack ( Track track ) {
		if ( tracks == null ) tracks = new ArrayList <Track> ();
		if ( track != null ) tracks.add ( track );
	}
}
