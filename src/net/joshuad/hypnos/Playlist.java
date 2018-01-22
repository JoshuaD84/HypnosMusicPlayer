package net.joshuad.hypnos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class Playlist implements Serializable {
	private static final Logger LOGGER = Logger.getLogger( Playlist.class.getName() );
	
	private static final long serialVersionUID = 1L;

	//This is only used and accurate during serialization
	private List <Track> trackDataForSerialization = new ArrayList<Track>(); 
	
	private transient ObservableList <Track> tracks = FXCollections.observableArrayList( trackDataForSerialization );
	
	private String name;
	
	private transient boolean hasUnsavedData = true;
	
	public Playlist ( String name ) {
		this ( name, new ArrayList <Track> () );
		tracks.addListener( (ListChangeListener.Change<? extends Track> change) -> {
			hasUnsavedData = true;
		});
	}
	
	public boolean hasUnsavedData() {
		return hasUnsavedData;
	}
	
	public void setHasUnsavedData( boolean b ) {
		hasUnsavedData = b;
	}
	
	public Playlist ( String name, List <Track> tracks ) {
		
		if ( name == null || name.length() == 0 ) {
			name = Hypnos.getLibrary().getUniquePlaylistName();
		}
		
		setTracks( tracks );
		this.name = name;
	}
	
	public static List<Path> getTrackPaths ( Path playlistPath ) {
		List<Path> retMe = new ArrayList<Path> ();
		
		if ( playlistPath.toString().toLowerCase().endsWith( ".m3u" ) ) {
			try (
				FileInputStream fileInput = new FileInputStream( playlistPath.toFile() );
			) {
				BufferedReader m3uIn = new BufferedReader ( new InputStreamReader ( fileInput, "UTF8" ) );
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
			
			Playlist playlist = new Playlist( path.getFileName().toString() );
			
			try (
				FileInputStream fileInput = new FileInputStream( path.toFile() );
			) {
				BufferedReader m3uIn = new BufferedReader ( new InputStreamReader ( fileInput, "UTF8" ) );
				for ( String line; (line = m3uIn.readLine()) != null; ) {

					try {
						if ( line.startsWith( "#Name:" ) ) {
							String name = line.split( ":" )[1].trim(); 
							playlist.setName( name );
						} else if ( line.isEmpty() ) {
							//Do nothing
							
						} else if ( !line.startsWith( "#" ) ) {
							playlist.addTrack ( new Track ( Paths.get ( line ) ) );
						}
					} catch ( Exception e ) {
						LOGGER.info( "Error parsing line in playlist: " + path.toString() + "\n\tLine: " + line );
					}
				}
			} catch ( Exception e ) {
				LOGGER.info( "Error reading playlist file: " + path );
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
	
	public ObservableList <Track> getTracks () {
		return tracks;
	}
	
	public void setTracks( List <Track> newTracks ) {
		if ( tracks == null ) {
			tracks.clear();
		} else {
			tracks.clear();
			tracks.addAll( newTracks );
		}
	}

	public void addTrack ( Track track ) {
		tracks.add ( track );
	}

	public void addTracks ( ArrayList <Track> addMe ) {
		tracks.addAll ( addMe );
	}
	
	public void saveAs ( File file ) throws IOException {
		if ( file == null ) {
			throw new IOException ( "Null file specified." );
		}
		
		try ( FileOutputStream fileOut = new FileOutputStream( file ) ) {
			PrintWriter playlistOut = new PrintWriter( new BufferedWriter( new OutputStreamWriter ( fileOut, "UTF8" ) ) );
			playlistOut.println( "#EXTM3U" );
			
			playlistOut.printf( "#Name: %s\n\n", getName() );

			for ( Track track : getTracks() ) {
				playlistOut.printf( "#EXTINF:%d,%s - %s\n", track.getLengthS(), track.getArtist(), track.getTitle() );
				playlistOut.println( track.getPath().toString() );
				playlistOut.println();
			}

			playlistOut.flush();
		} 
	}
	
	public String getBaseFilename() {
		String fileSafeName = getName().replaceAll("\\W+", "");
		
		if ( fileSafeName.length() > 12 ) {
			fileSafeName = fileSafeName.substring( 0, 12 );
		}
		
		String baseFileName =  fileSafeName + getName().hashCode();
		
		return baseFileName;
	}
	
	
	private void writeObject ( ObjectOutputStream out ) throws IOException {
		trackDataForSerialization.clear();
		trackDataForSerialization.addAll( tracks );
		out.defaultWriteObject();
	}
	
	private void readObject ( ObjectInputStream in ) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		tracks = FXCollections.observableArrayList( trackDataForSerialization );
	}
}
