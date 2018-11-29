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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.fxui.FXUI;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys.Hotkey;
import net.joshuad.hypnos.hotkeys.HotkeyState;

public class Persister {

	private static final Logger LOGGER = Logger.getLogger( Persister.class.getName() );

	public enum Setting {
		SHUFFLE, REPEAT, HIDE_ALBUM_TRACKS, WINDOW_MAXIMIZED, PRIMARY_SPLIT_PERCENT, 
		ART_CURRENT_SPLIT_PERCENT, ART_SPLIT_PERCENT, WINDOW_X_POSITION, WINDOW_Y_POSITION, 
		WINDOW_WIDTH, WINDOW_HEIGHT, TRACK, TRACK_POSITION, TRACK_NUMBER, VOLUME, LIBRARY_TAB,
		PROMPT_BEFORE_OVERWRITE, SHOW_INOTIFY_ERROR_POPUP, SHOW_UPDATE_AVAILABLE_IN_MAIN_WINDOW, 
		SHOW_SYSTEM_TRAY_ICON, CLOSE_TO_SYSTEM_TRAY, MINIMIZE_TO_SYSTEM_TRAY, THEME, LOADER_SPEED,
		DEFAULT_SHUFFLE_ALBUMS, DEFAULT_SHUFFLE_TRACKS, DEFAULT_SHUFFLE_PLAYLISTS,
		DEFAULT_REPEAT_ALBUMS,  DEFAULT_REPEAT_TRACKS,  DEFAULT_REPEAT_PLAYLISTS,
		
		AR_TABLE_ARTIST_COLUMN_SHOW, AR_TABLE_ALBUMS_COLUMN_SHOW, AR_TABLE_TRACKS_COLUMN_SHOW, 
		AR_TABLE_LENGTH_COLUMN_SHOW,
		AL_TABLE_ARTIST_COLUMN_SHOW, AL_TABLE_YEAR_COLUMN_SHOW, AL_TABLE_ALBUM_COLUMN_SHOW, 
		AL_TABLE_ADDED_COLUMN_SHOW,
		TR_TABLE_ARTIST_COLUMN_SHOW, TR_TABLE_NUMBER_COLUMN_SHOW, TR_TABLE_TITLE_COLUMN_SHOW, 
		TR_TABLE_ALBUM_COLUMN_SHOW, TR_TABLE_LENGTH_COLUMN_SHOW, 
		PL_TABLE_PLAYLIST_COLUMN_SHOW, PL_TABLE_TRACKS_COLUMN_SHOW, PL_TABLE_LENGTH_COLUMN_SHOW,
		CL_TABLE_PLAYING_COLUMN_SHOW, CL_TABLE_NUMBER_COLUMN_SHOW, CL_TABLE_ARTIST_COLUMN_SHOW, 
		CL_TABLE_YEAR_COLUMN_SHOW, CL_TABLE_ALBUM_COLUMN_SHOW, CL_TABLE_TITLE_COLUMN_SHOW, 
		CL_TABLE_LENGTH_COLUMN_SHOW,
		
		AR_TABLE_ARTIST_COLUMN_WIDTH, AR_TABLE_ALBUMS_COLUMN_WIDTH, AR_TABLE_TRACKS_COLUMN_WIDTH, 
		AR_TABLE_LENGTH_COLUMN_WIDTH,
		AL_TABLE_ARTIST_COLUMN_WIDTH, AL_TABLE_YEAR_COLUMN_WIDTH, AL_TABLE_ALBUM_COLUMN_WIDTH, 
		AL_TABLE_ADDED_COLUMN_WIDTH,
		TR_TABLE_ARTIST_COLUMN_WIDTH, TR_TABLE_NUMBER_COLUMN_WIDTH, TR_TABLE_TITLE_COLUMN_WIDTH, 
		TR_TABLE_ALBUM_COLUMN_WIDTH, TR_TABLE_LENGTH_COLUMN_WIDTH, 
		PL_TABLE_PLAYLIST_COLUMN_WIDTH, PL_TABLE_TRACKS_COLUMN_WIDTH, PL_TABLE_LENGTH_COLUMN_WIDTH,
		CL_TABLE_PLAYING_COLUMN_WIDTH, CL_TABLE_NUMBER_COLUMN_WIDTH, CL_TABLE_ARTIST_COLUMN_WIDTH, 
		CL_TABLE_YEAR_COLUMN_WIDTH, CL_TABLE_ALBUM_COLUMN_WIDTH, CL_TABLE_TITLE_COLUMN_WIDTH, 
		CL_TABLE_LENGTH_COLUMN_WIDTH,
		
		LIBRARY_TAB_ARTISTS_VISIBLE, LIBRARY_TAB_ALBUMS_VISIBLE, LIBRARY_TAB_TRACKS_VISIBLE, 
		LIBRARY_TAB_PLAYLISTS_VISIBLE,
		
		//This is dumb, but column order has to come before sort order in this list
		//so column order is applied first, so it doesn't mess up sort order. 
		ARTIST_COLUMN_ORDER, ARTIST_SORT_ORDER,
		ALBUM_COLUMN_ORDER, ALBUM_SORT_ORDER, 
		TRACK_COLUMN_ORDER, TRACK_SORT_ORDER, 
		PLAYLIST_COLUMN_ORDER, PLAYLIST_SORT_ORDER, 
		CL_COLUMN_ORDER, CL_SORT_ORDER, 
		
		LASTFM_USERNAME, LASTFM_PASSWORD_MD5, LASTFM_SCROBBLE_ON_PLAY, LASTFM_SCROBBLE_TIME,  
		SHOW_LASTFM_IN_UI,
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
	private AudioSystem audioSystem;
	private Library library;
	private GlobalHotkeys hotkeys;
	
	public Persister ( FXUI ui, Library library, AudioSystem audioSystem, GlobalHotkeys hotkeys ) {

		this.ui = ui;
		this.audioSystem = audioSystem;
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
				ui.notifyUserError( "Playlist directory (" + playlistsDirectory + ") exists but is not a directory.\n\nPlaylist data will not be saved." );
				LOGGER.warning( "Playlists directory exists but is a normal file, and I can't remove it."
					+ " Playlist data may be lost after program is terminated."
					+ playlistsDirectory.toString()
				);
			}
		}
		
		if ( !playlistsDirectory.exists() ) {
			boolean playlistDirCreated = playlistsDirectory.mkdirs(); 
			if ( playlistDirCreated ) {
				LOGGER.info( "Playlist directory did not exist. Created: " + playlistsDirectory.toString() ); 
			} else {
				ui.notifyUserError( "Cannot create playlist directory (" + playlistsDirectory + ").\n\nPlaylist data will not be saved." );
				LOGGER.warning( "Cannot create playlists directory. Playlist data may be lost after program is terminated." + playlistsDirectory.toString() );
			}
		}
	}

	public void saveAllData( EnumMap <Setting, ? extends Object> fromAudioSystem, EnumMap <Setting, ? extends Object> fromUI ) {
		createNecessaryFolders();
		saveAlbumsAndTracks();
		saveSources();
		saveCurrentList();
		saveQueue();
		saveHistory();
		saveLibraryPlaylists();
		saveSettings( fromAudioSystem, fromUI );
		saveHotkeys();	
	}

	public void saveSettings () {
		EnumMap <Setting, ? extends Object> fromAudioSystem = audioSystem.getSettings();
		EnumMap <Setting, ? extends Object> fromUI = ui.getSettings();
		saveSettings ( fromAudioSystem, fromUI );
	}

	public boolean loadSources () {
		try ( ObjectInputStream sourcesIn = new ObjectInputStream( new FileInputStream( sourcesFile ) ); ) {
			ArrayList <MusicSearchLocation> searchLocations = (ArrayList <MusicSearchLocation>) sourcesIn.readObject();
			for ( MusicSearchLocation locations : searchLocations ) {
				library.requestUpdateSource( locations );
			}

			library.setSourcesHasUnsavedData( false );
			return true;
			
		} catch ( Exception e ) {
			LOGGER.warning( "Unable to read library source directory list from disk, continuing." );
		}
		
		return false;
	}

	public void loadCurrentList() {
		try ( ObjectInputStream dataIn = new ObjectInputStream( new FileInputStream( currentFile ) ) ) {
			audioSystem.getCurrentList().setState ( (CurrentListState)dataIn.readObject() );
			audioSystem.getCurrentList().setHasUnsavedData( false );
			
		} catch ( Exception e ) {
			try ( ObjectInputStream dataIn = new ObjectInputStream( new GZIPInputStream( new FileInputStream( currentFile ) ) ) ) {
				audioSystem.getCurrentList().setState ( (CurrentListState)dataIn.readObject() );
				audioSystem.getCurrentList().setHasUnsavedData( false );
				
			} catch ( Exception e2 ) {
				LOGGER.warning( "Unable to read library data from disk, continuing." );
			}
		}
	}

	public void loadQueue () {
		try ( ObjectInputStream queueIn = new ObjectInputStream( new FileInputStream( queueFile ) ); ) {
			audioSystem.getQueue().queueAllTracks( (ArrayList <Track>) queueIn.readObject() );
			audioSystem.getQueue().setHasUnsavedData( false );
			
		} catch ( Exception e ) {
			LOGGER.warning( "Unable to read queue data from disk, continuing." );
		}
	}

	public void loadHistory () {
		try ( ObjectInputStream historyIn = new ObjectInputStream( new FileInputStream( historyFile ) ); ) {
			audioSystem.getHistory().setData( (ArrayList <Track>) historyIn.readObject() );
			audioSystem.getHistory().setHasUnsavedData( false );
		
		} catch ( Exception e ) {
			LOGGER.warning( "Unable to read history from disk, continuing." );
		}
	}
	
	public void loadHotkeys () {
		try ( ObjectInputStream hotkeysIn = new ObjectInputStream( new FileInputStream( hotkeysFile ) ); ) {
			hotkeys.setMap( (EnumMap <Hotkey, HotkeyState>) hotkeysIn.readObject() );
			hotkeys.setHasUnsavedData( false );
		} catch ( Exception e ) {
			LOGGER.warning( "Unable to read hotkeys from disk, continuing." );
		}
	}

	public void loadAlbumsAndTracks () {
		try ( ObjectInputStream dataIn = new ObjectInputStream( new GZIPInputStream( new FileInputStream( dataFile ) ) ) ) {
			library.albums.addAll( (ArrayList <Album>) dataIn.readObject() );
			library.tracks.addAll( (ArrayList <Track>) dataIn.readObject() );
			library.regenerateArtists();
		} catch ( Exception e ) {
			LOGGER.warning( "Unable to read library data from disk, continuing." );
		}
	}

	public void loadPlaylists () {
		try ( DirectoryStream <Path> stream = Files.newDirectoryStream( playlistsDirectory.toPath() ); ) {
			for ( Path child : stream ) {
				Playlist playlist = Playlist.loadPlaylist( child );
				if ( playlist != null ) {
					library.addPlaylist( playlist );
				}
				playlist.setHasUnsavedData( false );
			}

		} catch ( IOException e ) {
			LOGGER.log( Level.WARNING, "Unable to load playlists from disk.", e );
		}
	}

	public void saveSources () {
		if ( !library.sourcesHasUnsavedData() ) return;
		File tempSourcesFile = new File ( sourcesFile.toString() + ".temp" );
		try ( ObjectOutputStream sourcesOut = new ObjectOutputStream( new FileOutputStream( tempSourcesFile ) ); ) {
			sourcesOut.writeObject( new ArrayList <MusicSearchLocation> ( library.musicSearchLocations ) );
			sourcesOut.flush();
			sourcesOut.close();

			Files.move( tempSourcesFile.toPath(), sourcesFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );
			
			library.setSourcesHasUnsavedData( false );
			
		} catch ( Exception e ) {
			LOGGER.warning( "Unable to save library source directory list to disk, continuing." );
		}
	}
	
	public void saveCurrentList () {
		if ( !audioSystem.getCurrentList().hasUnsavedData() ) return;
		File tempCurrentFile = new File ( currentFile.toString() + ".temp" );
		
		if ( audioSystem.getCurrentList().getState().getItems().size() < 500 ) {
			try ( ObjectOutputStream currentListOut = new ObjectOutputStream( new FileOutputStream( tempCurrentFile ) ) ) {
				currentListOut.writeObject( audioSystem.getCurrentList().getState() );
				currentListOut.flush();
				currentListOut.close();
				
				Files.move( tempCurrentFile.toPath(), currentFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );

				audioSystem.getCurrentList().setHasUnsavedData( false );
				
			} catch ( Exception e ) {
				LOGGER.log( Level.WARNING, "Unable to save current list data to disk, continuing.", e );
			}
		} else {
			try ( GZIPOutputStream currentListOut = new GZIPOutputStream( new BufferedOutputStream( new FileOutputStream( tempCurrentFile ) ) ); ) {
	
				ByteArrayOutputStream byteWriter = new ByteArrayOutputStream();
				ObjectOutputStream bytesOut = new ObjectOutputStream( byteWriter );
	
				bytesOut.writeObject( audioSystem.getCurrentList().getState() );
	
				currentListOut.write( byteWriter.toByteArray() );
				currentListOut.flush();
				currentListOut.close();
				
				Files.move( tempCurrentFile.toPath(), currentFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );

				audioSystem.getCurrentList().setHasUnsavedData( false );
				
			} catch ( Exception e ) {
				LOGGER.log( Level.WARNING, "Unable to save current list data to disk, continuing.", e );
			}
		}
	}

	public void saveQueue () {
		if ( !audioSystem.getQueue().hasUnsavedData() ) {
			return;
		}
		
		File tempQueueFile = new File ( queueFile.toString() + ".temp" );
		try ( ObjectOutputStream queueListOut = new ObjectOutputStream( new FileOutputStream( tempQueueFile ) ) ) {
			queueListOut.writeObject( new ArrayList <Track>( audioSystem.getQueue().getData() ) );
			queueListOut.flush();
			queueListOut.close();
			
			Files.move( tempQueueFile.toPath(), queueFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );
			
			audioSystem.getQueue().setHasUnsavedData( false );
			
		} catch ( Exception e ) {
			LOGGER.warning( "Unable to save queue to disk, continuing." );
		}
	}

	public void saveHistory () {
		if ( !audioSystem.getHistory().hasUnsavedData() ) return;

		File tempHistoryFile = new File ( historyFile.toString() + ".temp" );
		
		try ( ObjectOutputStream historyListOut = new ObjectOutputStream( new FileOutputStream( tempHistoryFile ) ) ) {
			
			historyListOut.writeObject( new ArrayList <Track>( audioSystem.getHistory().getItems() ) );
			historyListOut.flush();
			historyListOut.close();
			
			Files.move( tempHistoryFile.toPath(), historyFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );
			
			audioSystem.getHistory().setHasUnsavedData( false );
			
		} catch ( Exception e ) {
			LOGGER.warning( "Unable to save history to disk, continuing." );
		}
	}
	
	public void saveHotkeys () {
		if ( !hotkeys.hasUnsavedData() ) return;
		
		File tempHotkeysFile = new File ( hotkeysFile.toString() + ".temp" );

		try ( ObjectOutputStream hotkeysOut = new ObjectOutputStream( new FileOutputStream( tempHotkeysFile ) ) ) {
			hotkeysOut.writeObject( hotkeys.getMap() );
			hotkeysOut.flush();
			hotkeysOut.close();
			
			Files.move( tempHotkeysFile.toPath(), hotkeysFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE );

			hotkeys.setHasUnsavedData( false );
			
		} catch ( Exception e ) {
			LOGGER.warning( "Unable to save hotkeys to disk, continuing." );
		}
	}

	public void saveAlbumsAndTracks () {
		/*
		 * Some notes for future Josh (2017/05/14): 
		 * 1. For some reason, keepingthe ByteArrayOutputStream in the middle 
		 * makes things take ~2/3 the amount of time. 
		 * 2. I tried removing tracks that have albums (since they're being 
		 * written twice) but it didn't create any savings. I guess compression
		 * is handling that.
		 * 3. I didn't try regular zip. GZIP was easier.
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
			LOGGER.warning( "Unable to save library data to disk, continuing." );
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
			
			if ( !playlist.hasUnsavedData() ) continue;
			
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

		playlist.setHasUnsavedData( false );
	}
	
	public void saveSettings ( EnumMap <Setting, ? extends Object> fromAudioSystem, EnumMap <Setting, ? extends Object> fromUI ) {
		
		File tempSettingsFile = new File ( settingsFile.toString() + ".temp" );

		try ( FileWriter fileWriter = new FileWriter( tempSettingsFile ); ) {
			PrintWriter settingsOut = new PrintWriter( new BufferedWriter( fileWriter ) );

			fromAudioSystem.forEach( ( key, value ) -> {
				String valueOut = value == null ? "null" : value.toString();
				settingsOut.printf( "%s: %s%s", key, valueOut, System.lineSeparator() );
			} );

			fromUI.forEach( ( key, value ) -> {
				String valueOut = value == null ? "null" : value.toString();
				settingsOut.printf( "%s: %s%s", key, valueOut, System.lineSeparator() );
			} );
			
			settingsOut.printf( "%s: %s%s", Setting.LOADER_SPEED, Hypnos.getLoaderSpeed(), System.lineSeparator() );

			settingsOut.flush();
			settingsOut.close();
			
			Files.move( tempSettingsFile.toPath(), settingsFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );

		} catch ( Exception e ) {
			LOGGER.warning( "Unable to save settings to disk, continuing." );
		}
	}
	
	public EnumMap <Setting, String> loadSettingsFromDisk () {
		EnumMap <Setting, String> settings = new EnumMap <Setting, String>( Setting.class );

		try ( FileReader fileReader = new FileReader( settingsFile ) ) {
			BufferedReader settingsIn = new BufferedReader( fileReader );
			for ( String line; (line = settingsIn.readLine()) != null; ) {
				Setting setting;
				try {
					setting = Setting.valueOf( line.split( ":\\s+" )[0] );
				} catch ( IllegalArgumentException e ) {
					LOGGER.info( "Found invalid setting: " + line.split( ":\\s+" )[0] + ", ignoring." );
					continue;
				}
				
				String value = "";
				try {
					value = line.split( ":\\s+" )[1];
				} catch ( ArrayIndexOutOfBoundsException e ) {
					//Do nothing, some settings can be empty
				}
					
				settings.put ( setting, value );
			}

		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to read settings from disk, continuing." );
		}
		
		return settings;
	}

	public void logUnusedSettings ( EnumMap <Setting, String> pendingSettings ) {
		if ( pendingSettings.size() == 0 ) return;
		
		String message = "";
		for ( Setting setting : pendingSettings.keySet() ) {
			if ( message.length() > 0 ) message += "\n";
			message += setting.toString() + ": " + pendingSettings.get( setting );
		}
		
		LOGGER.info ( "Some settings were read from disk but not applied:\n" + message );
	}
}
