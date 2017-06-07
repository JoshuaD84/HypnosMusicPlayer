package net.joshuad.musicplayer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Persister {
	
	static File configDirectory;
	
	static { 
		//TODO: We might want to make a few fall-throughs if these locations don't exist. 
		//TODO: I'm sure this needs fine tuning. I don't love putting it in a static block, either 
		String osString = System.getProperty( "os.name" ).toLowerCase();
		String home = System.getProperty( "user.home" );
		
		if ( MusicPlayerUI.IS_STANDALONE ) {
			configDirectory = MusicPlayerUI.ROOT.resolve( "config" ).toFile();
			
		} else if ( osString.indexOf( "win" ) >= 0 ) {
			if ( osString.indexOf( "xp" ) >= 0 ) {
				configDirectory = new File( 
					home + File.separator + 
					"Local Settings" + File.separator + 
					"Application Data" + File.separator + 
					"Hypnos"
				);
				
			} else if ( osString.indexOf( "vista" ) >= 0 ) {
				configDirectory = new File( 
					home + File.separator + 
					"AppData" + File.separator + 
					"Local" + File.separator + 
					"Hypnos"
				);
				
			} else if ( osString.indexOf( "7" ) >= 0 ) {
				configDirectory = new File( 
					home + File.separator + 
					"AppData" + File.separator + 
					"Local" + File.separator + 
					"Hypnos"
				);
				
			} else if ( osString.indexOf( "8" ) >= 0 ) {
				configDirectory = new File( 
					home + File.separator + 
					"AppData" + File.separator + 
					"Local" + File.separator + 
					"Hypnos"
				);
				
			} else if ( osString.indexOf( "10" ) >= 0 ) {
				configDirectory = new File( 
					home + File.separator + 
					"AppData" + File.separator + 
					"Local" + File.separator + 
					"Hypnos"
				);
			} else {
				configDirectory = new File( 
					home + File.separator + 
					"AppData" + File.separator + 
					"Local" + File.separator + 
					"Hypnos"
				);
			}
			
		} else if ( osString.indexOf( "nix" ) >= 0 || osString.indexOf( "linux" ) >= 0 ) {
			configDirectory = new File( home + File.separator + ".hypnos" );

		} else if ( osString.indexOf( "mac" ) >= 0 ) {
			configDirectory = new File( home + File.separator + "Preferences" + File.separator + "Hypnos" );
			
		} else {
			configDirectory = new File( home + File.separator + ".hypnos" );
		}
	}
	
	static File sourcesFile = new File ( configDirectory + File.separator + "sources" );
	static File playlistsFile = new File ( configDirectory + File.separator + "playlists" );
	static File currentFile = new File ( configDirectory + File.separator + "current" );
	static File dataFile = new File ( configDirectory + File.separator + "data" );
	
	
	private static void createNecessaryFolders() {	
		if ( !configDirectory.exists() ) {
			boolean created = configDirectory.mkdirs();
			//TODO: check created
		}
		
		if ( !configDirectory.isDirectory() ) {
			//TODO: 
		}
	}
		
	
	@SuppressWarnings("unchecked")
	public static void loadData() {
		createNecessaryFolders();
		readDataCompressed();

		try (
				ObjectInputStream sourcesIn = new ObjectInputStream( new FileInputStream( sourcesFile ) );
		) {
			ArrayList<String> searchPaths = (ArrayList<String>) sourcesIn.readObject();
			for ( String pathString : searchPaths ) {
				Library.requestUpdateSource( Paths.get( pathString ) );
			}
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: info.sources, unable to load library source location list, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			e.printStackTrace(); //TODO: 
		}
		
		try (
				ObjectInputStream currentListIn = new ObjectInputStream( new FileInputStream( currentFile ) );
		) {
			MusicPlayerUI.currentListData.addAll( (ArrayList<CurrentListTrack>) currentListIn.readObject() );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: info.current, unable to load current playlist, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
		
		try (
				ObjectInputStream playlistsIn = new ObjectInputStream( new FileInputStream( playlistsFile ) );
		) {
			Library.addPlaylists ( (ArrayList<Playlist>) playlistsIn.readObject() );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: info.playlists, unable to load custom playlist data, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
	}

	static void saveData() {
		createNecessaryFolders();
		
		if ( !configDirectory.exists() ) {
			boolean created = configDirectory.mkdirs();
			//TODO: check created
		}
		
		if ( !configDirectory.isDirectory() ) {
			//TODO: 
		}
		
		writeDataCompressed();
		try ( 
				ObjectOutputStream sourcesOut = new ObjectOutputStream ( new FileOutputStream ( sourcesFile ) );
		) {
			ArrayList <String> searchPaths = new ArrayList <String> ( Library.musicSourcePaths.size() );
			for ( Path path : Library.musicSourcePaths ) {
				searchPaths.add( path.toString() );
			}
			sourcesOut.writeObject( searchPaths );
			sourcesOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try ( 
				ObjectOutputStream currentListOut = new ObjectOutputStream ( new FileOutputStream ( currentFile ) );
		) {
			currentListOut.writeObject( new ArrayList <CurrentListTrack> ( Arrays.asList( MusicPlayerUI.currentListData.toArray( new CurrentListTrack[ MusicPlayerUI.currentListData.size() ] ) ) ) );
			currentListOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		try ( 
				ObjectOutputStream playlistsOut = new ObjectOutputStream ( new FileOutputStream ( playlistsFile ) );
		) {
			playlistsOut.writeObject( new ArrayList <Playlist> ( Arrays.asList( Library.playlists.toArray( new Playlist[ Library.playlists.size() ] ) ) ) );
			playlistsOut.flush();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void readDataCompressed() {
		try (
				ObjectInputStream dataIn = new ObjectInputStream( new GZIPInputStream ( new FileInputStream( dataFile ) ) );
		) {
			//TODO: Maybe do this more carefully, give Library more control over it? 
			Library.albums.addAll( (ArrayList<Album>) dataIn.readObject() );
			Library.tracks.addAll( (ArrayList<Track>) dataIn.readObject() );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: info.data, unable to load albuma and song lists, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
	}
	
	private static void writeDataCompressed() {
		/* Some notes for future Josh (2017/05/14):
		 * 1. For some reason, keeping the ByteArrayOutputStream in the middle makes things take ~2/3 the amount of time.
		 * 2. I tried removing tracks that have albums (since they're being written twice) but it didn't create any savings. I guess compression is handling that
		 * 3. I didn't trip regular zip. GZIP was easier.
		 */
		try ( 
				GZIPOutputStream compressedOut = new GZIPOutputStream ( new BufferedOutputStream ( new FileOutputStream ( dataFile ) ) );
		) {
			
			ByteArrayOutputStream byteWriter = new ByteArrayOutputStream();
			ObjectOutputStream bytesOut = new ObjectOutputStream ( byteWriter );
			
			bytesOut.writeObject( new ArrayList <Album> ( Arrays.asList( Library.albums.toArray( new Album[ Library.albums.size() ] ) ) ) );
			bytesOut.writeObject( new ArrayList <Track> ( Arrays.asList( Library.tracks.toArray( new Track[ Library.tracks.size() ] ) ) ) );

			compressedOut.write( byteWriter.toByteArray() );
			compressedOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
