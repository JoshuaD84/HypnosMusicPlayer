package net.joshuad.hypnos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Playlist implements Serializable {
	private static final Logger LOGGER = Logger.getLogger( Playlist.class.getName() );
	
	private static final long serialVersionUID = 1L;

	private List <Track> tracks;
	
	private String name;
	
	public Playlist ( String name ) {
		this ( name, new ArrayList <Track> () );
	}
	
	public Playlist ( String name, List <Track> tracks ) {
		
		if ( name == null || name.length() == 0 ) {
			name = Hypnos.getLibrary().getNewPlaylistName();
		}
		
		setTracks( tracks );
		this.name = name;
	}
	
	public static List<Path> getTrackPaths ( Path playlistPath ) {
		List<Path> retMe = new ArrayList<Path> ();
		
		if ( playlistPath.toString().toLowerCase().endsWith( ".m3u" ) ) {
			try (
					FileReader fileReader = new FileReader( playlistPath.toFile() );
			) {
				BufferedReader m3uIn = new BufferedReader ( fileReader );
				for ( String line; (line = m3uIn.readLine()) != null; ) {
					if ( line.isEmpty() ) {
						//Do nothing
						
					} else if ( !line.startsWith( "#" ) ) {
						retMe.add( Paths.get ( line ) );
					}
				}
			} catch ( Exception e ) {
				LOGGER.log( Level.INFO, "Error reading playlist file: " + playlistPath, e );
			}
		} else {
			LOGGER.info( "Asked to load a playlist that doesn't have a playlist extension, ignoring." );
		}
			
		return retMe;
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
						String name = line.split( ":" )[1].trim(); //TODO: OOB error checking on index
						playlist.setName( name );
					} else if ( line.isEmpty() ) {
						//Do nothing
						
					} else if ( !line.startsWith( "#" ) ) {
						try {
							playlist.addTrack ( new Track ( Paths.get ( line ) ) );
						} catch ( Exception e ) {
							//TODO: 
							System.out.println ( "Error parsing line in playlist: " + path.toString() + ", continuing." );
							System.out.println ( "\tLine: " + line );
						}
					}
						
						
				}
			} catch ( Exception e ) {
				LOGGER.info( "Error reading playlist file: " + path );
				return null;
			}
			
			//TODO: If name isn't set, set it to the file name, so we have something. 
			
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
	
	public List <Track> getTracks () {
		return tracks;
	}
	
	public void setTracks( List <Track> tracks ) {
		if ( tracks == null ) this.tracks = new ArrayList <Track> ();
		else this.tracks = new ArrayList <Track> ( tracks );
	}

	public void addTrack ( Track track ) {
		tracks.add ( track );
	}

	public void addTracks ( ArrayList <Track> addMe ) {
		tracks.addAll ( addMe );
	}
	
	public void saveAs ( File file, boolean includeName ) throws IOException {
		if ( file == null ) {
			throw new IOException ( "Null file specified." );
		}
		
		try ( FileWriter fileWriter = new FileWriter( file ) ) {
			PrintWriter playlistOut = new PrintWriter( new BufferedWriter( fileWriter ) );
			playlistOut.println( "#EXTM3U" );
			
			if ( includeName ) {
				playlistOut.printf( "#Name: %s\n", getName() );
				playlistOut.println();
			}

			for ( Track track : getTracks() ) {
				playlistOut.printf( "#EXTINF:%d,%s - %s\n", track.getLengthS(), track.getArtist(), track.getTitle() );
				playlistOut.println( track.getPath().toString() );
				playlistOut.println();
			}

			playlistOut.flush();
		} 
	}
}
