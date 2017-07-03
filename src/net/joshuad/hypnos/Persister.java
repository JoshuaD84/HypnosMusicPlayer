package net.joshuad.hypnos;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javafx.collections.ObservableList;
import net.joshuad.hypnos.audio.PlayerController;
import net.joshuad.hypnos.fxui.FXUI;

public class Persister {

	private static final Logger LOGGER = Logger.getLogger( Persister.class.getName() );
	
	public enum Setting {
		SHUFFLE,                                     
		REPEAT,                                           
		HIDE_ALBUM_TRACKS,                       
		WINDOW_MAXIMIZED,                        
		PRIMARY_SPLIT_PERCENT,           
		CURRENT_LIST_SPLIT_PERCENT,  
		ART_SPLIT_PERCENT,                   
		WINDOW_X_POSITION,                               
		WINDOW_Y_POSITION,                               
		WINDOW_WIDTH,                                
		WINDOW_HEIGHT,                              
		TRACK,                                      
		TRACK_POSITION,                     
		TRACK_NUMBER;  
	}
	
	File configDirectory;
	File sourcesFile;
	File playlistsDirectory;
	File currentFile;
	File queueFile;
	File historyFile;
	File dataFile;
	File settingsFile;
	
	FXUI ui;
	PlayerController player;
	
	public Persister ( FXUI ui, PlayerController player ) {
		
		this.ui = ui;
		this.player = player;
		
		//TODO: We might want to make a few fall-throughs if these locations don't exist. 
		//TODO: I'm sure this needs fine tuning. 
		String osString = System.getProperty( "os.name" ).toLowerCase();
		String home = System.getProperty( "user.home" );
		
		if ( Hypnos.IS_STANDALONE ) {
			configDirectory = Hypnos.ROOT.resolve( "config" ).toFile();
			
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
		
		sourcesFile = new File ( configDirectory + File.separator + "sources" );
		playlistsDirectory = new File ( configDirectory + File.separator + "playlists" );
		currentFile = new File ( configDirectory + File.separator + "current" );
		queueFile = new File ( configDirectory + File.separator + "queue" );
		historyFile = new File ( configDirectory + File.separator + "history" );
		dataFile = new File ( configDirectory + File.separator + "data" );
		settingsFile = new File ( configDirectory + File.separator + "settings" );
	}
	
	private void createNecessaryFolders() {	
		if ( !configDirectory.exists() ) {
			boolean created = configDirectory.mkdirs();
			//TODO: check created
		}
		
		if ( !configDirectory.isDirectory() ) {
			//TODO: 
		}
		
		if ( !playlistsDirectory.exists() ) {
			boolean playlistDir = playlistsDirectory.mkdirs();
		}
	}
		
	
	public void loadDataBeforeShowWindow() {
		loadPreWindowSettings();
	}
	
	public void loadDataAfterShowWindow() {
		loadAlbumsAndTracks();
		loadSources();
		loadCurrentList();
		loadQueue();
		loadHistory();
		loadPlaylists();
		loadPostWindowSettings( );
	}

	public void saveAllData() {
		createNecessaryFolders();
		saveAlbumsAndTracks();
		saveSources();
		saveCurrentList();
		saveQueue();
		saveHistory();
		savePlaylists();
		saveSettings();
	}
	
	@SuppressWarnings("unchecked")
	public void loadSources() {
		try (
				ObjectInputStream sourcesIn = new ObjectInputStream( new FileInputStream( sourcesFile ) );
		) {
			ArrayList<String> searchPaths = (ArrayList<String>) sourcesIn.readObject();
			for ( String pathString : searchPaths ) {
				Hypnos.library().requestUpdateSource( Paths.get( pathString ) );
			}
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: sources, unable to load library source location list, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			e.printStackTrace(); //TODO: 
		}
	}
	
	@SuppressWarnings("unchecked")
	public void loadCurrentList() {
		try (
				ObjectInputStream currentListIn = new ObjectInputStream( new FileInputStream( currentFile ) );
		) {
			player.loadTracks( (ArrayList<CurrentListTrack>) currentListIn.readObject(), true );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: current, unable to load current playlist, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void loadQueue() {
		try (
				ObjectInputStream queueIn = new ObjectInputStream( new FileInputStream( queueFile ) );
		) {
			Hypnos.queue().addAllTracks( (ArrayList<Track>) queueIn.readObject() );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: queue, unable to load queue, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void loadHistory() {
		try (
				ObjectInputStream historyIn = new ObjectInputStream( new FileInputStream( historyFile ) );
		) {
			player.addToHistory( (ArrayList<Track>) historyIn.readObject() );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: history, unable to load queue, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
	}
	
	public void saveSources() {
		try ( 
				ObjectOutputStream sourcesOut = new ObjectOutputStream ( new FileOutputStream ( sourcesFile ) );
		) {
			ArrayList <String> searchPaths = new ArrayList <String> ( Hypnos.library().musicSourcePaths.size() );
			for ( Path path : Hypnos.library().musicSourcePaths ) {
				searchPaths.add( path.toString() );
			}
			sourcesOut.writeObject( searchPaths );
			sourcesOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void saveCurrentList() {
		try ( 
				ObjectOutputStream currentListOut = new ObjectOutputStream ( new FileOutputStream ( currentFile ) );
		) {
			ObservableList<CurrentListTrack> saveMe = player.getCurrentList();
			List<Track> writeMe = new ArrayList <Track> ( Arrays.asList( saveMe.toArray( new Track[ saveMe.size() ] ) ) );
			currentListOut.writeObject( writeMe );
			currentListOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void saveQueue() {
		
		try ( 
				ObjectOutputStream queueListOut = new ObjectOutputStream ( new FileOutputStream ( queueFile ) );
		) {
			queueListOut.writeObject( new ArrayList <Track> ( Arrays.asList( Hypnos.queue().getData().toArray( new Track[ Hypnos.queue().getData().size() ] ) ) ) );
			queueListOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void saveHistory( ) {
		try ( 
				ObjectOutputStream historyListOut = new ObjectOutputStream ( new FileOutputStream ( historyFile ) );
		) {
			historyListOut.writeObject( new ArrayList <Track> ( Arrays.asList( player.getHistory().toArray( new Track[ player.getHistory().size() ] ) ) ) );
			historyListOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void loadAlbumsAndTracks() {
		try (
				ObjectInputStream dataIn = new ObjectInputStream( new GZIPInputStream ( new FileInputStream( dataFile ) ) );
		) {
			//TODO: Maybe do this more carefully, give Library more control over it? 
			Hypnos.library().albums.addAll( (ArrayList<Album>) dataIn.readObject() );
			Hypnos.library().tracks.addAll( (ArrayList<Track>) dataIn.readObject() );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: info.data, unable to load albuma and song lists, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
	}
	
	public void saveAlbumsAndTracks() {
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
			
			bytesOut.writeObject( new ArrayList <Album> ( Arrays.asList( Hypnos.library().albums.toArray( new Album[ Hypnos.library().albums.size() ] ) ) ) );
			bytesOut.writeObject( new ArrayList <Track> ( Arrays.asList( Hypnos.library().tracks.toArray( new Track[ Hypnos.library().tracks.size() ] ) ) ) );

			compressedOut.write( byteWriter.toByteArray() );
			compressedOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void loadPlaylists() {
		
		try ( 
				DirectoryStream <Path> stream = Files.newDirectoryStream( playlistsDirectory.toPath() ); 
		) {
			for ( Path child : stream ) {
				Playlist playlist = Playlist.loadPlaylist( child );
				if ( playlist != null ) {
					Hypnos.library().addPlaylist( playlist );
				}
			}
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void savePlaylists() {
		ArrayList <Playlist> playlists = new ArrayList <Playlist> ( Hypnos.library().playlists );
		
		int playlistIndex = 1;
		for ( Playlist playlist : playlists ) {
			try (
					FileWriter fileWriter = new FileWriter ( Paths.get( playlistsDirectory.toString(), playlistIndex + ".m3u" ).toFile() );
			) {
				PrintWriter playlistOut = new PrintWriter( new BufferedWriter( fileWriter ) );
				playlistOut.println ( "#EXTM3U" );
				playlistOut.printf ( "#Name: %s\n", playlist.getName() );
				playlistOut.println();
				
				for ( Track track : playlist.getTracks() ) {
					playlistOut.printf( "#EXTINF:%d,%s - %s\n", track.getLengthS(), track.getArtist(), track.getTitle() );
					playlistOut.println( track.getPath().toString() );
					playlistOut.println();
				}
				
				playlistOut.flush();
			} catch ( IOException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			playlistIndex++;
		}
	}
	
	public void saveSettings() {
		EnumMap <Setting, ? extends Object> fromPlayer = player.getSettings();
		EnumMap <Setting, ? extends Object> fromUI = ui.getSettings();

		try ( 
				FileWriter fileWriter = new FileWriter( settingsFile );
		) {
			PrintWriter settingsOut = new PrintWriter( new BufferedWriter( fileWriter ) );
			
			fromPlayer.forEach( ( key, value )-> {
				settingsOut.printf( "%s: %s\n", key, value.toString() );
			});
			
			fromUI.forEach( ( key, value )-> {
				settingsOut.printf( "%s: %s\n", key, value.toString() );
			});
			
			settingsOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("incomplete-switch")
	public void loadPreWindowSettings() {
		EnumMap <Setting, String> loadMe = new EnumMap<Setting, String> ( Setting.class );
		
		try (
				FileReader fileReader = new FileReader( settingsFile );
		) {

			BufferedReader settingsIn = new BufferedReader ( fileReader );
			
			for ( String line; (line = settingsIn.readLine()) != null; ) {
				Setting setting;
				try {
					setting = Setting.valueOf( line.split(":\\s+")[0] );
				} catch ( IllegalArgumentException e ) {
					LOGGER.info( "Found invalid setting: " + line.split(":\\s+")[0] + ", continuing." );
					continue;
				}
				
				String value = line.split(":\\s+")[1];
				
				switch ( setting ) {
					case SHUFFLE:
					case REPEAT:
					case HIDE_ALBUM_TRACKS:
					case WINDOW_X_POSITION:
					case WINDOW_Y_POSITION:
					case WINDOW_WIDTH:
					case WINDOW_HEIGHT:
					case WINDOW_MAXIMIZED:
					case PRIMARY_SPLIT_PERCENT:
					case CURRENT_LIST_SPLIT_PERCENT:
					case ART_SPLIT_PERCENT:
						loadMe.put( setting, value );
						break;
				}
			}
			
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: settings, unable to load user settings, using defaults. Continuing." );
		} catch ( IOException e ) {
			//TODO: 
			e.printStackTrace();
		}
		
		ui.applySettings( loadMe );
	}
	
	@SuppressWarnings("incomplete-switch")
	public void loadPostWindowSettings( ) {

		EnumMap <Setting, String> loadMe = new EnumMap<Setting, String> ( Setting.class );
		try (
				FileReader fileReader = new FileReader( settingsFile );
		) {

			BufferedReader settingsIn = new BufferedReader ( fileReader );
			
			for ( String line; (line = settingsIn.readLine()) != null; ) {
				Setting setting;
				try {
					setting = Setting.valueOf( line.split(":\\s+")[0] );
				} catch ( IllegalArgumentException e ) {
					LOGGER.info( "Found invalid setting: " + line.split(":\\s+")[0] + ", continuing." );
					continue;
				}
				
				String value = line.split(":\\s+")[1];
				
				switch ( setting ) {
				case TRACK:
				case TRACK_POSITION:
				case TRACK_NUMBER:
					loadMe.put( setting, value );
					break;
				}
			}
			
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: settings, unable to load user settings, using defaults. Continuing." );
		} catch ( IOException e ) {
			//TODO: 
			e.printStackTrace();
		}

		ui.applySettings( loadMe );
	}
}
