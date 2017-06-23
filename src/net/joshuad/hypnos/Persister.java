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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Persister {
	
	static File configDirectory;
	static File sourcesFile;
	static File playlistsDirectory;
	static File currentFile;
	static File queueFile;
	static File historyFile;
	static File dataFile;
	static File settingsFile;
	
	
	public static void init() {
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
		
		sourcesFile = new File ( configDirectory + File.separator + "sources" );
		playlistsDirectory = new File ( configDirectory + File.separator + "playlists" );
		currentFile = new File ( configDirectory + File.separator + "current" );
		queueFile = new File ( configDirectory + File.separator + "queue" );
		historyFile = new File ( configDirectory + File.separator + "history" );
		dataFile = new File ( configDirectory + File.separator + "data" );
		settingsFile = new File ( configDirectory + File.separator + "settings" );
	}
	
	private static void createNecessaryFolders() {	
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
		
	
	public static void loadDataBeforeShowWindow() {
		loadPreWindowSettings();
	}
	
	public static void loadDataAfterShowWindow() {
		loadAlbumsAndTracks();
		loadSources();
		loadCurrentList();
		loadQueue();
		loadHistory();
		loadPlaylists();
		loadPostWindowSettings();
	}

	public static void saveAllData() {
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
	public static void loadSources() {
		try (
				ObjectInputStream sourcesIn = new ObjectInputStream( new FileInputStream( sourcesFile ) );
		) {
			ArrayList<String> searchPaths = (ArrayList<String>) sourcesIn.readObject();
			for ( String pathString : searchPaths ) {
				Library.requestUpdateSource( Paths.get( pathString ) );
			}
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: sources, unable to load library source location list, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			e.printStackTrace(); //TODO: 
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void loadCurrentList() {
		try (
				ObjectInputStream currentListIn = new ObjectInputStream( new FileInputStream( currentFile ) );
		) {
			MusicPlayerUI.currentListData.addAll( (ArrayList<CurrentListTrack>) currentListIn.readObject() );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: current, unable to load current playlist, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void loadQueue() {
		
		try (
				ObjectInputStream queueIn = new ObjectInputStream( new FileInputStream( queueFile ) );
		) {
			Queue.addAllTracks( (ArrayList<Track>) queueIn.readObject() );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: queue, unable to load queue, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static void loadHistory() {
		try (
				ObjectInputStream historyIn = new ObjectInputStream( new FileInputStream( historyFile ) );
		) {
			MusicPlayerUI.history.addAll( (ArrayList<Track>) historyIn.readObject() );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: history, unable to load queue, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
	}
	
	public static void saveSources() {
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
	}
	
	public static void saveCurrentList() {
		try ( 
				ObjectOutputStream currentListOut = new ObjectOutputStream ( new FileOutputStream ( currentFile ) );
		) {
			currentListOut.writeObject( new ArrayList <CurrentListTrack> ( Arrays.asList( MusicPlayerUI.currentListData.toArray( new CurrentListTrack[ MusicPlayerUI.currentListData.size() ] ) ) ) );
			currentListOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void saveQueue() {
		
		try ( 
				ObjectOutputStream queueListOut = new ObjectOutputStream ( new FileOutputStream ( queueFile ) );
		) {
			queueListOut.writeObject( new ArrayList <Track> ( Arrays.asList( Queue.getData().toArray( new Track[ Queue.getData().size() ] ) ) ) );
			queueListOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void saveHistory() {
		try ( 
				ObjectOutputStream historyListOut = new ObjectOutputStream ( new FileOutputStream ( historyFile ) );
		) {
			historyListOut.writeObject( new ArrayList <Track> ( Arrays.asList( MusicPlayerUI.history.toArray( new Track[ MusicPlayerUI.history.size() ] ) ) ) );
			historyListOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void loadAlbumsAndTracks() {
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
	
	public static void saveAlbumsAndTracks() {
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
	
	public static void loadPlaylists() {
		
		try ( 
				DirectoryStream <Path> stream = Files.newDirectoryStream( playlistsDirectory.toPath() ); 
		) {
			for ( Path child : stream ) {
				Playlist playlist = Playlist.loadPlaylist( child );
				if ( playlist != null ) {
					Library.addPlaylist( playlist );
				}
			}
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void savePlaylists() {
		ArrayList <Playlist> playlists = new ArrayList <Playlist> ( Library.playlists );
		
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
	
	
	
	//TODO: Move these up top
	private static final String SETTING_TAG_SHUFFLE = "Shuffle";
	private static final String SETTING_TAG_REPEAT = "Repeat";
	private static final String SETTING_TAG_HIDE_ALBUM_TRACKS = "HideAlbumTracks";
	private static final String SETTING_TAG_WINDOW_MAXIMIZED = "WindowMaximized";
	private static final String SETTING_TAG_PRIMARY_SPLIT_PERCENT = "PrimaryPaneSplitPercent";
	private static final String SETTING_TAG_CURRENT_LIST_SPLIT_PERCENT = "CurrentListPaneSplitPercent";
	private static final String SETTING_TAG_ART_SPLIT_PERCENT = "ArtPaneSplitPercent";
	private static final String SETTING_TAG_WINDOW_X_POSITION = "WindowX";
	private static final String SETTING_TAG_WINDOW_Y_POSITION = "WindowY";
	private static final String SETTING_TAG_WINDOW_WIDTH = "WindowWidth";
	private static final String SETTING_TAG_WINDOW_HEIGHT = "WindowHeight";
	private static final String SETTING_TAG_TRACK = "CurrentTrack";
	private static final String SETTING_TAG_TRACK_POSITION = "CurrentTrackPosition";
	private static final String SETTING_TAG_TRACK_NUMBER = "CurrentTrackNumber";
	
	
	public static void saveSettings() {
		try ( 
				FileWriter fileWriter = new FileWriter( settingsFile );
		) {
			PrintWriter settingsOut = new PrintWriter( new BufferedWriter( fileWriter ) );
			
			if ( MusicPlayerUI.currentPlayer != null ) {
				settingsOut.printf( "%s: %s\n", SETTING_TAG_TRACK, MusicPlayerUI.currentPlayer.getTrack().getPath().toString() );
				settingsOut.printf( "%s: %s\n", SETTING_TAG_TRACK_POSITION, MusicPlayerUI.currentPlayer.getPositionMS() );
				settingsOut.printf( "%s: %d\n", SETTING_TAG_TRACK_NUMBER, MusicPlayerUI.getCurrentTrackNumber() );
			} 
			
			settingsOut.printf( "%s: %s\n", SETTING_TAG_SHUFFLE, MusicPlayerUI.shuffleMode.toString() );
			settingsOut.printf( "%s: %s\n", SETTING_TAG_REPEAT, MusicPlayerUI.repeatMode.toString() );
			settingsOut.printf( "%s: %b\n", SETTING_TAG_HIDE_ALBUM_TRACKS, MusicPlayerUI.trackListCheckBox.isSelected() );
			settingsOut.printf( "%s: %b\n", SETTING_TAG_WINDOW_MAXIMIZED, MusicPlayerUI.isMaximized() );
			settingsOut.printf( "%s: %f\n", SETTING_TAG_WINDOW_X_POSITION, MusicPlayerUI.mainStage.getX() );
			settingsOut.printf( "%s: %f\n", SETTING_TAG_WINDOW_Y_POSITION, MusicPlayerUI.mainStage.getY() );
			settingsOut.printf( "%s: %f\n", SETTING_TAG_WINDOW_WIDTH, MusicPlayerUI.mainStage.getWidth() );
			settingsOut.printf( "%s: %f\n", SETTING_TAG_WINDOW_HEIGHT, MusicPlayerUI.mainStage.getHeight() );
			settingsOut.printf( "%s: %f\n", SETTING_TAG_PRIMARY_SPLIT_PERCENT, MusicPlayerUI.getPrimarySplitPercent() );
			settingsOut.printf( "%s: %f\n", SETTING_TAG_CURRENT_LIST_SPLIT_PERCENT, MusicPlayerUI.getCurrentListSplitPercent() );
			settingsOut.printf( "%s: %f\n", SETTING_TAG_ART_SPLIT_PERCENT, MusicPlayerUI.getArtSplitPercent() );
			
			settingsOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void loadPreWindowSettings() {
		try (
				FileReader fileReader = new FileReader( settingsFile );
		) {

			BufferedReader settingsIn = new BufferedReader ( fileReader );
			
			for ( String line; (line = settingsIn.readLine()) != null; ) {
				String tag = line.split(":\\s+")[0];
				String value = line.split(":\\s+")[1];
				
				try {
					switch ( tag ) {
						case SETTING_TAG_SHUFFLE:
							MusicPlayerUI.setShuffleMode ( MusicPlayerUI.ShuffleMode.valueOf( value ) );
							break;
							
						case SETTING_TAG_REPEAT:
							MusicPlayerUI.setRepeatMode ( MusicPlayerUI.RepeatMode.valueOf( value ) );
							break;
							
						case SETTING_TAG_HIDE_ALBUM_TRACKS:
							MusicPlayerUI.setShowAlbumTracks ( Boolean.valueOf( value ) );
							break;		
							
						case SETTING_TAG_WINDOW_X_POSITION:
							MusicPlayerUI.mainStage.setX( Double.valueOf( value ) );
							break;
							
						case SETTING_TAG_WINDOW_Y_POSITION:
							MusicPlayerUI.mainStage.setY( Double.valueOf( value ) );
							break;
							
						case SETTING_TAG_WINDOW_WIDTH:
							MusicPlayerUI.mainStage.setWidth( Double.valueOf( value ) );
							break;
							
						case SETTING_TAG_WINDOW_HEIGHT:
							MusicPlayerUI.mainStage.setHeight( Double.valueOf( value ) );
							break;
							
						case SETTING_TAG_WINDOW_MAXIMIZED:
							MusicPlayerUI.setMaximized ( Boolean.valueOf( value ) );
							break;
							
						case SETTING_TAG_PRIMARY_SPLIT_PERCENT:
							MusicPlayerUI.setPrimarySplitPercent ( Double.valueOf( value ) );
							break;
							
						case SETTING_TAG_CURRENT_LIST_SPLIT_PERCENT:
							MusicPlayerUI.setCurrentListSplitPercent ( Double.valueOf( value ) );
							break;
							
						case SETTING_TAG_ART_SPLIT_PERCENT:
							MusicPlayerUI.setArtSplitPercent ( Double.valueOf( value ) );
							break;
							
					}
				} catch ( Exception e ) {
					e.printStackTrace( System.out );
					System.out.println ( "Unable to parse settings tag: " + tag + ", continuing." );
				}
			}
			
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: settings, unable to load user settings, using defaults. Continuing." );
		} catch ( IOException e ) {
			//TODO: 
			e.printStackTrace();
		}
	}
	
	public static void loadPostWindowSettings() {
		try (
				FileReader fileReader = new FileReader( settingsFile );
		) {

			BufferedReader settingsIn = new BufferedReader ( fileReader );
			
			for ( String line; (line = settingsIn.readLine()) != null; ) {
				String tag = line.split(":\\s+")[0];
				String value = line.split(":\\s+")[1];
				
				try {
					switch ( tag ) {
						case SETTING_TAG_TRACK:
							Track track = new Track ( Paths.get( value ), false );
							MusicPlayerUI.playTrack( track, true );
							break;
							
						case SETTING_TAG_TRACK_POSITION:
							if ( MusicPlayerUI.currentPlayer != null ) {
								MusicPlayerUI.currentPlayer.seekMS( Long.parseLong( value ) );
							}
							break;
							
						case SETTING_TAG_TRACK_NUMBER:
							try {
								int tracklistNumber = Integer.parseInt( value );
								if ( tracklistNumber != -1 ) {
									MusicPlayerUI.currentListData.get( tracklistNumber ).setIsCurrentTrack( true );
								}
							} catch ( Exception e ) {
								System.out.println ( "Error loading current list track number: " + e.getMessage() );
							}
							
							break;
					}
				} catch ( Exception e ) {
					e.printStackTrace( System.out );
					System.out.println ( "Unable to parse settings tag: " + tag + ", continuing." );
				}
			}
			
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: settings, unable to load user settings, using defaults. Continuing." );
		} catch ( IOException e ) {
			//TODO: 
			e.printStackTrace();
		}
	}
}
