package net.joshuad.hypnos;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.joshuad.hypnos.LibraryUpdater.LoaderSpeed;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.fxui.FXUI;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys;
import net.joshuad.hypnos.hotkeys.KeyState;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys.Hotkey;

public class Persister {

	private static final Logger LOGGER = Logger.getLogger( Persister.class.getName() );

	public enum Setting {
		SHUFFLE, REPEAT, HIDE_ALBUM_TRACKS, WINDOW_MAXIMIZED, PRIMARY_SPLIT_PERCENT, 
		CURRENT_LIST_SPLIT_PERCENT, ART_SPLIT_PERCENT, WINDOW_X_POSITION, WINDOW_Y_POSITION, 
		WINDOW_WIDTH, WINDOW_HEIGHT, TRACK, TRACK_POSITION, TRACK_NUMBER, VOLUME, LIBRARY_TAB,
		PROMPT_BEFORE_OVERWRITE, THEME, LOADER_SPEED,
		DEFAULT_SHUFFLE_TRACKS, DEFAULT_SHUFFLE_ALBUMS, DEFAULT_SHUFFLE_PLAYLISTS,
		DEFAULT_REPEAT_TRACKS, DEFAULT_REPEAT_ALBUMS, DEFAULT_REPEAT_PLAYLISTS,

		AL_TABLE_SHOW_ARTIST_COLUMN, AL_TABLE_SHOW_YEAR_COLUMN, AL_TABLE_SHOW_ALBUM_COLUMN, 
		TR_TABLE_SHOW_ARTIST_COLUMN, TR_TABLE_SHOW_NUMBER_COLUMN, TR_TABLE_SHOW_TITLE_COLUMN, 
		TR_TABLE_SHOW_ALBUM_COLUMN, TR_TABLE_SHOW_LENGTH_COLUMN, 
		PL_TABLE_SHOW_PLAYLIST_COLUMN, PL_TABLE_SHOW_TRACKS_COLUMN, PL_TABLE_SHOW_LENGTH_COLUMN,
		CL_TABLE_SHOW_PLAYING_COLUMN, CL_TABLE_SHOW_NUMBER_COLUMN, CL_TABLE_SHOW_ARTIST_COLUMN, 
		CL_TABLE_SHOW_YEAR_COLUMN, CL_TABLE_SHOW_ALBUM_COLUMN, CL_TABLE_SHOW_TITLE_COLUMN, 
		CL_TABLE_SHOW_LENGTH_COLUMN
		;
	}

	private File sourcesFile;
	private File playlistsDirectory;
	private File currentFile;
	private File queueFile;
	private File historyFile;
	private File dataFile;
	private File settingsFile;
	private File hotkeysFile;

	private FXUI ui;
	private AudioSystem player;
	private Library library;
	private GlobalHotkeys hotkeys;
	
	public Persister ( FXUI ui, Library library, AudioSystem player, GlobalHotkeys hotkeys ) {

		this.ui = ui;
		this.player = player;
		this.library = library;
		this.hotkeys = hotkeys;

		File configDirectory = Hypnos.getConfigDirectory().toFile();

		sourcesFile = new File( configDirectory + File.separator + "sources" );
		playlistsDirectory = new File( configDirectory + File.separator + "playlists" );
		currentFile = new File( configDirectory + File.separator + "current" );
		queueFile = new File( configDirectory + File.separator + "queue" );
		historyFile = new File( configDirectory + File.separator + "history" );
		dataFile = new File( configDirectory + File.separator + "data" );
		settingsFile = new File( configDirectory + File.separator + "settings" );
		hotkeysFile = new File( configDirectory + File.separator + "hotkeys" );
		
		createNecessaryFolders();
	}

	private void createNecessaryFolders () {
		if ( playlistsDirectory.exists() && !playlistsDirectory.isDirectory() ) {
			try {
				Files.delete( playlistsDirectory.toPath() );
				LOGGER.info( "Playlists directory location existed but was not a directory. Removed: " + playlistsDirectory.toString() ); 
			} catch ( IOException e ) {
				LOGGER.warning( e.getClass().getCanonicalName() 
					+ ": Playlists directory exists but is a normal file, and I can't remove it."
					+ " Playlist data may be lost after program is terminated."
					+ playlistsDirectory.toString()
				);
			}
		}
		
		if ( !playlistsDirectory.exists() ) {
			boolean playlistDirCreated = playlistsDirectory.mkdirs(); //TODO: boolean never read
			if ( playlistDirCreated ) {
				LOGGER.info( "Playlist directory did not exist. Created: " + playlistsDirectory.toString() ); 
			} else {
				LOGGER.warning( "Cannot create playlists directory. Playlist data may be lost after program is terminated." + playlistsDirectory.toString() );
			}
		}
	}

	public void loadDataBeforeShowWindow () {
		loadCurrentList();
		loadPreWindowSettings();
	}

	public void loadDataAfterShowWindow () {
		loadAlbumsAndTracks();
		loadSources();
		loadQueue();
		player.linkQueueToCurrentList();
		loadHistory();
		loadPlaylists();
		loadHotkeys();
		ui.refreshHotkeyList();
	}
	
	//REFACTOR: I don't think this way of doing things is very great, two almost identical functions. W/e it works for now. 
	public void saveAllData( EnumMap <Setting, ? extends Object> fromPlayer, EnumMap <Setting, ? extends Object> fromUI ) {
		createNecessaryFolders();
		saveAlbumsAndTracks();
		saveSources();
		saveCurrentList();
		saveQueue();
		saveHistory();
		saveLibraryPlaylists();
		saveSettings( fromPlayer, fromUI );
		saveHotkeys();	
	}

	public void saveAllData () {
		createNecessaryFolders();
		saveAlbumsAndTracks();
		saveSources();
		saveCurrentList();
		saveQueue();
		saveHistory();
		saveLibraryPlaylists();
		saveSettings();
		saveHotkeys();
	}

	@SuppressWarnings("unchecked")
	public void loadSources () {
		try ( ObjectInputStream sourcesIn = new ObjectInputStream( new FileInputStream( sourcesFile ) ); ) {
			ArrayList <String> searchPaths = (ArrayList <String>) sourcesIn.readObject();
			for ( String pathString : searchPaths ) {
				library.requestUpdateSource( Paths.get( pathString ) );
			}
			
		} catch ( Exception e ) {
			LOGGER.warning( e.getClass().getCanonicalName() + ": Unable to read library source directory list from disk, continuing." );
		}
		
	}

	public void loadCurrentList () {
		try ( ObjectInputStream currentListIn = new ObjectInputStream( new FileInputStream( currentFile ) ); ) {
			player.getCurrentList().setState ( (CurrentListState)currentListIn.readObject() );
		
		} catch ( Exception e ) {
			LOGGER.warning( e.getClass().getCanonicalName() + ": Unable to read current list from disk, continuing." );
		}
		
	}

	@SuppressWarnings("unchecked")
	public void loadQueue () {
		try ( ObjectInputStream queueIn = new ObjectInputStream( new FileInputStream( queueFile ) ); ) {
			player.getQueue().queueAllTracks( (ArrayList <Track>) queueIn.readObject() );
			
		} catch ( Exception e ) {
			LOGGER.warning( e.getClass().getCanonicalName() + ": Unable to read queue data from disk, continuing." );
		}
	}

	@SuppressWarnings("unchecked")
	public void loadHistory () {
		try ( ObjectInputStream historyIn = new ObjectInputStream( new FileInputStream( historyFile ) ); ) {
			player.getHistory().setData( (ArrayList <Track>) historyIn.readObject() );
		
		} catch ( Exception e ) {
			LOGGER.warning( e.getClass().getCanonicalName() + ": Unable to read history from disk, continuing." );
		}
	}
	
	@SuppressWarnings("unchecked")
	public void loadHotkeys () {
		try ( ObjectInputStream hotkeysIn = new ObjectInputStream( new FileInputStream( hotkeysFile ) ); ) {
			hotkeys.setMap( (EnumMap <Hotkey, KeyState>) hotkeysIn.readObject() );
		} catch ( Exception e ) {
			LOGGER.warning( e.getClass().getCanonicalName() + ": Unable to read hotkeys from disk, continuing." );
		}
	}

	@SuppressWarnings("unchecked")
	public void loadAlbumsAndTracks () {
		try ( ObjectInputStream dataIn = new ObjectInputStream( new GZIPInputStream( new FileInputStream( dataFile ) ) ) ) {
			library.albums.addAll( (ArrayList <Album>) dataIn.readObject() );
			library.tracks.addAll( (ArrayList <Track>) dataIn.readObject() );
			
		} catch ( Exception e ) {
			LOGGER.warning( e.getClass().getCanonicalName() + ": Unable to read library data from disk, continuing." );
		}
	}

	public void saveSources () {
		File tempSourcesFile = new File ( sourcesFile.toString() + ".temp" );
		try ( ObjectOutputStream sourcesOut = new ObjectOutputStream( new FileOutputStream( tempSourcesFile ) ); ) {
			ArrayList <String> searchPaths = new ArrayList <String>( library.musicSourcePaths.size() );
			for ( Path path : library.musicSourcePaths ) {
				searchPaths.add( path.toString() );
			}
			sourcesOut.writeObject( searchPaths );
			sourcesOut.flush();
			sourcesOut.close();

			Files.move( tempSourcesFile.toPath(), sourcesFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );
			
		} catch ( Exception e ) {
			LOGGER.warning( e.getClass().getCanonicalName() + ": Unable to save library source directory list to disk, continuing." );
		}
	}

	public void saveCurrentList () {
		File tempCurrentFile = new File ( currentFile.toString() + ".temp" );
		try ( ObjectOutputStream currentListOut = new ObjectOutputStream( new FileOutputStream( tempCurrentFile ) ) ) {
			currentListOut.writeObject( player.getCurrentList().getState() );
			currentListOut.flush();
			currentListOut.close();

			Files.move( tempCurrentFile.toPath(), currentFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE );

		} catch ( Exception e ) {
			LOGGER.warning( e.getClass().getCanonicalName() + ": Unable to save current list to disk, continuing." );
		}
	}

	public void saveQueue () {

		File tempQueueFile = new File ( queueFile.toString() + ".temp" );
		try ( ObjectOutputStream queueListOut = new ObjectOutputStream( new FileOutputStream( tempQueueFile ) ) ) {
			queueListOut.writeObject( new ArrayList <Track>( player.getQueue().getData() ) );
			queueListOut.flush();
			queueListOut.close();
			
			Files.move( tempQueueFile.toPath(), queueFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );

		} catch ( Exception e ) {
			LOGGER.warning( e.getClass().getCanonicalName() + ": Unable to save queue to disk, continuing." );
		}
	}

	public void saveHistory () {

		File tempHistoryFile = new File ( historyFile.toString() + ".temp" );
		
		try ( ObjectOutputStream historyListOut = new ObjectOutputStream( new FileOutputStream( tempHistoryFile ) ) ) {
			
			historyListOut.writeObject( new ArrayList <Track>( player.getHistory().getItems() ) );
			historyListOut.flush();
			historyListOut.close();
			
			Files.move( tempHistoryFile.toPath(), historyFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );

		} catch ( Exception e ) {
			LOGGER.warning( e.getClass().getCanonicalName() + ": Unable to save history to disk, continuing." );
		}
	}
	
	public void saveHotkeys () {
		
		File tempHotkeysFile = new File ( hotkeysFile.toString() + ".temp" );

		try ( ObjectOutputStream hotkeysOut = new ObjectOutputStream( new FileOutputStream( tempHotkeysFile ) ) ) {
			hotkeysOut.writeObject( hotkeys.getMap() );
			hotkeysOut.flush();
			hotkeysOut.close();
			
			Files.move( tempHotkeysFile.toPath(), hotkeysFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );

		} catch ( Exception e ) {
			LOGGER.warning( e.getClass().getCanonicalName() + ": Unable to save hotkeys to disk, continuing." );
		}
	}

	public void saveAlbumsAndTracks () {
		/*
		 * Some notes for future Josh (2017/05/14): 1. For some reason, keeping
		 * the ByteArrayOutputStream in the middle makes things take ~2/3 the
		 * amount of time. 2. I tried removing tracks that have albums (since
		 * they're being written twice) but it didn't create any savings. I
		 * guess compression is handling that 3. I didn't try regular zip. GZIP
		 * was easier.
		 */

		File tempDataFile = new File ( dataFile.toString() + ".temp" );
		
		try ( GZIPOutputStream compressedOut = new GZIPOutputStream( new BufferedOutputStream( new FileOutputStream( tempDataFile ) ) ); ) {

			ByteArrayOutputStream byteWriter = new ByteArrayOutputStream();
			ObjectOutputStream bytesOut = new ObjectOutputStream( byteWriter );

			bytesOut.writeObject( new ArrayList <Album>( Arrays.asList( library.albums.toArray( new Album [ library.albums.size() ] ) ) ) );
			bytesOut.writeObject( new ArrayList <Track>( Arrays.asList( library.tracks.toArray( new Track [ library.tracks.size() ] ) ) ) );

			compressedOut.write( byteWriter.toByteArray() );
			compressedOut.flush();
			compressedOut.close();
			
			Files.move( tempDataFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );

		} catch ( Exception e ) {
			LOGGER.warning( e.getClass().getCanonicalName() + ": Unable to save library data to disk, continuing." );
		}
	}

	public void loadPlaylists () {

		try ( DirectoryStream <Path> stream = Files.newDirectoryStream( playlistsDirectory.toPath() ); ) {
			for ( Path child : stream ) {
				Playlist playlist = Playlist.loadPlaylist( child );
				if ( playlist != null ) {
					library.addPlaylist( playlist );
				}
			}

		} catch ( IOException e ) {
			LOGGER.log( Level.WARNING, "Unable to load playlists from disk.", e );
		}
	}

	public void saveLibraryPlaylists () {
		
		ArrayList <Playlist> playlists = new ArrayList <> ( library.getPlaylists() );
		
		ArrayList <Playlist> errors = new ArrayList <> ();

		for ( Playlist playlist : playlists ) {
			if ( playlist == null ) {
				LOGGER.info( "Found a null playlist in library.playlists, ignoring." );
				continue;
			}
			
			try {
				saveLibaryPlaylist ( playlist );
			} catch ( IOException e ) {
				LOGGER.warning ( "Unable to save library playlist " + playlist.getName() + ": " + e.getMessage() );
				errors.add( playlist );
			}
		}
		
		if ( errors.size() > 0 ) {
			Hypnos.warnUserPlaylistsNotSaved ( errors ); 
		}
	}
	
	public void deletePlaylistFile ( Playlist playlist ) {
		deletePlaylistFile ( playlist.getBaseFilename() );
	}
	
	public void deletePlaylistFile ( String basename ) {
		Path targetFile = playlistsDirectory.toPath().resolve (  basename + ".m3u" );
		
		try {
			Files.deleteIfExists( targetFile );
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to delete playlist file: " + targetFile, e );
		}
	}
	
	
	//Assumptions: playlist != null, playlist.name is not null or empty, and no playlists in library have the same name. 
	private void saveLibaryPlaylist ( Playlist playlist ) throws IOException {
		
		String baseFileName = playlist.getBaseFilename();
		Path targetFile = playlistsDirectory.toPath().resolve (  baseFileName + ".m3u" );
		Path backupFile = playlistsDirectory.toPath().resolve ( baseFileName + ".m3u.backup" );
		Path tempFile = playlistsDirectory.toPath().resolve ( baseFileName + ".m3u.temp" );
		
		boolean savedToTemp = false;
		
		try {
			playlist.saveAs( tempFile.toFile() );
			savedToTemp = true;
		} catch ( IOException e ) {
			savedToTemp = false;
			LOGGER.info( "Unable to write to a temp file, so I will try writing directly to the playlist file." +
				"Your data will be saved in a backup file first. If this process is interrupted, you may need to manually " +
				"recover the data from the backup file.\n" + e.getMessage() );
			
			if ( Files.exists( targetFile ) ) {
				try {
					Files.move( targetFile, backupFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );
				} catch ( IOException e2 ) {
					LOGGER.info( "Unable to move existing playlist file to backup location (" + backupFile.toString() +
						") will continue trying to save current playlist, overwriting the existing file." + 
						"\n" + e2.getMessage() );
				}
			}
		}
		
		try {
			
			boolean movedFromTemp = false;
			
			if ( savedToTemp ) {
				try {
					Files.move( tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );
					movedFromTemp = true;
				} catch ( IOException e ) {
					movedFromTemp = false;
				}
			}
			
			if ( !movedFromTemp ) {
				playlist.saveAs( targetFile.toFile() );
			}
			
		} catch ( IOException e ) {
			LOGGER.info( "Unable to save playlist to file: " + targetFile.toString() + "." );
			throw e;
			
		} finally {
			Files.deleteIfExists( tempFile );
			
		}
	}
	
	public void saveSettings() {
		EnumMap <Setting, ? extends Object> fromPlayer = player.getSettings();
		EnumMap <Setting, ? extends Object> fromUI = ui.getSettings();
		saveSettings ( fromPlayer, fromUI );
	}

	public void saveSettings ( EnumMap <Setting, ? extends Object> fromPlayer, EnumMap <Setting, ? extends Object> fromUI ) {
		
		File tempSettingsFile = new File ( settingsFile.toString() + ".temp" );

		try ( FileWriter fileWriter = new FileWriter( tempSettingsFile ); ) {
			PrintWriter settingsOut = new PrintWriter( new BufferedWriter( fileWriter ) );

			fromPlayer.forEach( ( key, value ) -> {
				String valueOut = value == null ? "null" : value.toString();
				settingsOut.printf( "%s: %s\n", key, valueOut );
			} );

			fromUI.forEach( ( key, value ) -> {
				String valueOut = value == null ? "null" : value.toString();
				settingsOut.printf( "%s: %s\n", key, valueOut );
			} );
			
			settingsOut.printf( "%s: %s\n", Setting.LOADER_SPEED, Hypnos.getLoaderSpeed() );

			settingsOut.flush();
			settingsOut.close();
			
			Files.move( tempSettingsFile.toPath(), settingsFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );

		} catch ( Exception e ) {
			LOGGER.warning( e.getClass().getCanonicalName() + ": Unable to save settings to disk, continuing." );
		}
	}

	public void loadPreWindowSettings () {
		EnumMap <Setting, String> loadMe = new EnumMap <Setting, String>( Setting.class );

		try ( FileReader fileReader = new FileReader( settingsFile ); ) {

			BufferedReader settingsIn = new BufferedReader( fileReader );

			for ( String line; (line = settingsIn.readLine()) != null; ) {
				Setting setting;
				try {
					setting = Setting.valueOf( line.split( ":\\s+" )[0] );
				} catch ( IllegalArgumentException e ) {
					LOGGER.info( "Found invalid setting: " + line.split( ":\\s+" )[0] + ", continuing." );
					continue;
				}

				String value = line.split( ":\\s+" )[1];

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
					case VOLUME: 
					case LIBRARY_TAB:
					case TRACK:
					case TRACK_POSITION:
					case TRACK_NUMBER:
					case ART_SPLIT_PERCENT:
					case PROMPT_BEFORE_OVERWRITE:
					case DEFAULT_REPEAT_ALBUMS:
					case DEFAULT_REPEAT_PLAYLISTS:
					case DEFAULT_REPEAT_TRACKS:
					case DEFAULT_SHUFFLE_ALBUMS:
					case DEFAULT_SHUFFLE_PLAYLISTS:
					case DEFAULT_SHUFFLE_TRACKS:
					case THEME:
					case AL_TABLE_SHOW_ARTIST_COLUMN:
					case AL_TABLE_SHOW_YEAR_COLUMN:
					case AL_TABLE_SHOW_ALBUM_COLUMN:
					case TR_TABLE_SHOW_ARTIST_COLUMN:
					case TR_TABLE_SHOW_NUMBER_COLUMN:
					case TR_TABLE_SHOW_TITLE_COLUMN:
					case TR_TABLE_SHOW_ALBUM_COLUMN:
					case TR_TABLE_SHOW_LENGTH_COLUMN:
					case PL_TABLE_SHOW_PLAYLIST_COLUMN:
					case PL_TABLE_SHOW_TRACKS_COLUMN: 
					case PL_TABLE_SHOW_LENGTH_COLUMN:
					case CL_TABLE_SHOW_PLAYING_COLUMN:
					case CL_TABLE_SHOW_NUMBER_COLUMN:
					case CL_TABLE_SHOW_ARTIST_COLUMN:
					case CL_TABLE_SHOW_YEAR_COLUMN:
					case CL_TABLE_SHOW_ALBUM_COLUMN:
					case CL_TABLE_SHOW_TITLE_COLUMN:
					case CL_TABLE_SHOW_LENGTH_COLUMN:
						loadMe.put( setting, value );
						break;
					case LOADER_SPEED:
						Hypnos.setLoaderSpeed( LoaderSpeed.valueOf( value ) );
					default:
						break;
				}
			}

		} catch ( Exception e ) {
			LOGGER.warning( e.getClass().getCanonicalName() + ": Unable to read settings from disk, continuing." );
		}

		ui.applySettings( loadMe );
	}
}
