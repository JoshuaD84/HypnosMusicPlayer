package net.joshuad.hypnos;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.io.output.TeeOutputStream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import net.joshuad.hypnos.LibraryUpdater.LoaderSpeed;
import net.joshuad.hypnos.Persister.Setting;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;
import net.joshuad.hypnos.fxui.FXUI;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys.Hotkey;

public class Hypnos extends Application {

	private static final Logger LOGGER = Logger.getLogger( Hypnos.class.getName() );
	
	public enum ExitCode {
		NORMAL,
		UNKNOWN_ERROR,
		AUDIO_ERROR
	}
	
	public enum OS {
		WIN_XP ( "Windows XP" ),
		WIN_VISTA ( "Windows Vista" ),
		WIN_7 ( "Windows 7" ),
		WIN_8 ( "Windows 8" ),
		WIN_10 ( "Windows 10" ),
		WIN_UNKNOWN ( "Windows Unknown" ),
		OSX ( "Mac OSX" ),
		NIX ( "Linux/Unix" ), 
		UNKNOWN ( "Unknown" );
		
		private String displayName;
		OS ( String displayName ) { this.displayName = displayName; }
		public String getDisplayName () { return displayName; }
		
		public boolean isWindows() {
			switch ( this ) {
				case WIN_10:
				case WIN_7:
				case WIN_8:
				case WIN_UNKNOWN:
				case WIN_VISTA:
				case WIN_XP:
					return true;
				case NIX:
				case OSX:
				case UNKNOWN:
				default:
					return false;
			}
		}
		
		public boolean isOSX() {
			switch ( this ) {
				case OSX:
					return true;
				case WIN_10:
				case WIN_7:
				case WIN_8:
				case WIN_UNKNOWN:
				case WIN_VISTA:
				case WIN_XP:
				case NIX:
				case UNKNOWN:
				default:
					return false;
			}
		}
		
		public boolean isLinux() {
			switch ( this ) {
				case NIX:
					return true;
				case WIN_10:
				case WIN_7:
				case WIN_8:
				case WIN_UNKNOWN:
				case WIN_VISTA:
				case WIN_XP:
				case UNKNOWN:
				case OSX:
				default:
					return false;
			}
		}
	}
	
	private static OS os = OS.UNKNOWN;
	private static String build;
	private static String version;
	private static String buildDate;
	private static Path rootDirectory;
	private static Path configDirectory;
	private static Path logFile, logFileBackup, logFileBackup2;
	private static boolean isStandalone = false;
	private static boolean isDeveloping = false;
	private static boolean globalHotkeysDisabled = false;
	
	private static Persister persister;
	private static AudioSystem audioSystem;
	private static FXUI ui;
	private static LibraryUpdater libraryUpdater;
	private static Library library;
	private static GlobalHotkeys hotkeys;
	
	private static PrintStream originalOut;
	private static PrintStream originalErr;
	
	private static LoaderSpeed loaderSpeed = LoaderSpeed.HIGH;
	
	private static ByteArrayOutputStream logBuffer; //Used to store log info until log file is initialized
	
	private static Handler consoleHandler;
	
	private static Formatter logFormat = new Formatter() {
		SimpleDateFormat dateFormat = new SimpleDateFormat ( "MMM d, yyyy HH:mm:ss aaa" );
		public String format ( LogRecord record ) {
			
			String exceptionMessage = "";
			
			if ( record.getThrown() != null ) {
				StringWriter sw = new StringWriter();
				record.getThrown().printStackTrace( new PrintWriter( sw ) );
				exceptionMessage = "\n" + sw.toString();
			}
			
			String retMe = dateFormat.format( new Date ( record.getMillis() ) )
				  + " " + record.getLoggerName()
				  + " " + record.getSourceMethodName()
				  + System.lineSeparator()
				  + record.getLevel() + ": " + record.getMessage()
				  + exceptionMessage
				  + System.lineSeparator()
				  + System.lineSeparator();
			
			return retMe;
		}
	};
	
	public static OS getOS() {
		return os;
	}
	
	public static String getVersion() {
		return version;
	}
	
	public static String getBuild() {
		return build;
	}
	
	public static String getBuildDate() {
		return buildDate;
	}
	
	public static boolean isStandalone() {
		return isStandalone;
	}
	
	public static boolean isDeveloping() {
		return isDeveloping;
	}
	
	public static Path getRootDirectory() {
		return rootDirectory;
	}
	
	public static Path getConfigDirectory() {
		return configDirectory;
	}
	
	public static Path getLogFile() {
		return logFile;
	}
	
	public static Path getLogFileBackup() {
		return logFileBackup;
	}
	
	public static Persister getPersister() {
		return persister;
	}
	
	public static Library getLibrary() {
		return library;
	}
	
	public static FXUI getUI() {
		return ui;
	}
	
	public static LoaderSpeed getLoaderSpeed ( ) {
		return loaderSpeed;
	}
	
	public static void setLoaderSpeed ( LoaderSpeed speed ) {
		/*Items that have background loading threads
		 * CurrentList -> updates missing tracks, relinks files, updates track data, etc. on current list.
		 * Library -> LoaderThread
		 * LibraryUpdater -> updaterThread
		 * MusicFileVisitor -> has a sleep increment
		 * AudioPlayer -> PlayerThread
		 */
		
		loaderSpeed = speed;
		
		switch ( speed ) {
			case LOW:
				InitialScanFileVisitor.setSleepTimeBetweenVisits( 150 );
				library.setLoaderSleepTimeMS( 250 );
				libraryUpdater.setMaxChangesPerUpdate ( 500 );
				libraryUpdater.setSleepTimeMS( 60 );
				break;
				
			case MED:
				InitialScanFileVisitor.setSleepTimeBetweenVisits( 50 );
				library.setLoaderSleepTimeMS( 50 );
				libraryUpdater.setMaxChangesPerUpdate ( 2000 );
				libraryUpdater.setSleepTimeMS( 15 );
				break;
				
			case HIGH:
				InitialScanFileVisitor.setSleepTimeBetweenVisits( 0 );
				library.setLoaderSleepTimeMS( 10 );
				libraryUpdater.setMaxChangesPerUpdate ( 20000 );
				libraryUpdater.setSleepTimeMS( 2 );
				break;
		}
		
		ui.setLoaderSpeedDisplay ( speed );
		
	}
	
	private static void setupInitialLoggerFormat () {
		consoleHandler = new ConsoleHandler();
		consoleHandler.setFormatter( logFormat );
		Logger.getLogger( "" ).getHandlers()[0].setFormatter( logFormat );
	}
	
	private static void startLogToBuffer() {
		originalOut = System.out;
		originalErr = System.err;
		
		logBuffer = new ByteArrayOutputStream();
		
		TeeOutputStream bufferOutTee = new TeeOutputStream ( originalOut, new PrintStream ( logBuffer ) );
		TeeOutputStream bufferErrTee = new TeeOutputStream ( originalErr, new PrintStream ( logBuffer ) );
		
		System.setOut( new PrintStream ( bufferOutTee ) );
		System.setErr( new PrintStream ( bufferErrTee ) );
	}
	
	private void parseSystemProperties() {
				
		isStandalone = Boolean.getBoolean( "hypnos.standalone" );
		isDeveloping = Boolean.getBoolean( "hypnos.developing" );
		GlobalHotkeys.setDisableRequested ( Boolean.getBoolean( "hypnos.disableglobalhotkeys" ) );
		
		if ( isStandalone ) LOGGER.info ( "Running as standalone - requested by system properties set at program launch" );
		if ( isDeveloping ) LOGGER.info ( "Running on development port - requested by system properties set at program launch" );
		if ( GlobalHotkeys.getDisableRequested() ) LOGGER.info ( "Global hotkeys disabled - requested by system properties set at program launch" );
	}
	
	private void determineOS() {
		String osString = System.getProperty( "os.name" ).toLowerCase();
		
		if ( osString.indexOf( "win" ) >= 0 ) {
			if ( osString.indexOf( "xp" ) >= 0 ) {
				os = OS.WIN_XP;

			} else if ( osString.indexOf( "vista" ) >= 0 ) {
				os = OS.WIN_VISTA;
			
			} else if ( osString.indexOf( "7" ) >= 0 ) {
				os = OS.WIN_7;
				
			} else if ( osString.indexOf( "8" ) >= 0 ) {
				os = OS.WIN_8;
				
			} else if ( osString.indexOf( "10" ) >= 0 ) {
				os = OS.WIN_10;

			} else {
				os = OS.WIN_UNKNOWN;
			}
			
		} else if ( osString.indexOf( "nix" ) >= 0 || osString.indexOf( "linux" ) >= 0 ) {
			os = OS.NIX;

		} else if ( osString.indexOf( "mac" ) >= 0 ) {
			os = OS.OSX;
			
		} else {
			os = OS.UNKNOWN;
		}
		
		LOGGER.info ( "Operating System: " + os.getDisplayName() );
	}
	
	public String determineVersionInfo () {
		@SuppressWarnings("rawtypes")
		Enumeration resEnum;
		try {
			resEnum = Thread.currentThread().getContextClassLoader().getResources( JarFile.MANIFEST_NAME );
			while ( resEnum.hasMoreElements() ) {
				try {
					URL url = (URL) resEnum.nextElement();
					
					if ( url.getFile().toLowerCase().contains( "hypnos" ) ) {
						try ( 
							InputStream is = url.openStream();
						) {
							if ( is != null ) {
								Manifest manifest = new Manifest( is );
								Attributes mainAttribs = manifest.getMainAttributes();
								if ( mainAttribs.getValue( "Hypnos" ) != null ) {
									version = mainAttribs.getValue( "Implementation-Version" );
									build = mainAttribs.getValue ( "Build-Number" );
									buildDate = mainAttribs.getValue ( "Build-Date" );
									LOGGER.info ( "Version: " + version + ", Build: " + build + ", Build Date: " + buildDate );
								}
							}
						}
					}
				} catch ( Exception e ) {
					// Silently ignore wrong manifests on classpath?
				}
			}
		} catch ( Exception e1 ) {
			// Silently ignore wrong manifests on classpath?
		}
		return null;
	}
	
	
	private void setupRootDirectory () {
		String path = FXUI.class.getProtectionDomain().getCodeSource().getLocation().getPath();
				
		try {
			String decodedPath = URLDecoder.decode(path, "UTF-8");
			decodedPath = decodedPath.replaceFirst("^/(.:/)", "$1");
			rootDirectory = Paths.get( decodedPath ).getParent();
			
		} catch ( UnsupportedEncodingException e ) {
			rootDirectory = Paths.get( path ).getParent();
		}
	}
	
	private void setupConfigDirectory () {
		// PENDING: We might want to make a few fall-throughs if these locations don't exist.
		String home = System.getProperty( "user.home" );

		if ( Hypnos.isStandalone() ) {
			configDirectory = getRootDirectory().resolve( "config" );

		} else {
			final String x = File.separator;
			switch ( getOS() ) {
				case NIX:
					configDirectory = Paths.get( home + x + ".config/hypnos" );
					break;
				case OSX:
					configDirectory = Paths.get( home + x + "Preferences" + x + "Hypnos" );
					break;
				case WIN_10:
					configDirectory = Paths.get( home + x + "AppData" + x + "Local" + x + "Hypnos" );
					break;
				case WIN_7:
					configDirectory = Paths.get( home + x + "AppData" + x + "Local" + x + "Hypnos" );
					break;
				case WIN_8:
					configDirectory = Paths.get( home + x + "AppData" + x + "Local" + x + "Hypnos" );
					break;
				case WIN_UNKNOWN:
					configDirectory = Paths.get( home + x + "AppData" + x + "Local" + x + "Hypnos" );
					break;
				case WIN_VISTA:
					configDirectory = Paths.get( home + x + "AppData" + x + "Local" + x + "Hypnos" );
					break;
				case WIN_XP:
					configDirectory = Paths.get( home + x + "Local Settings" + x + "Application Data" + x + "Hypnos" );
					break;
				case UNKNOWN: //Fall through
				default:
					configDirectory = Paths.get( home + x + ".hypnos" );
					break;
			}
		}
		
		File configDirectory = Hypnos.getConfigDirectory().toFile();
		
		if ( !configDirectory.exists() ) {
			LOGGER.info( "Config directory doesn't exist, creating: " + Hypnos.getConfigDirectory() );
			try {
				configDirectory.mkdirs();
			} catch ( Exception e ) {
				String message = "Unable to create config directory, data will not be saved.\n" + Hypnos.getConfigDirectory();
				LOGGER.info( message );
				///TODO: Some version of a deferred ui.notifyUserError( message );
			}
		} else if ( !configDirectory.isDirectory() ) {
			String message = "There is a file where the config directory should be, data will not be saved.\n" + Hypnos.getConfigDirectory();
			LOGGER.info( message );
			///TODO: Some version of a deferred ui.notifyUserError( message );
			
		} else if ( !configDirectory.canWrite() ) {
			String message = "Cannot write to config directory, data will not be saved.\n" + Hypnos.getConfigDirectory();
			LOGGER.info( message );
			///TODO: Some version of a deferred ui.notifyUserError( message );
		}
	}
	
	private void setupLogFile() {
		logFile = configDirectory.resolve( "hypnos.log" );
		logFileBackup = configDirectory.resolve( "hypnos.log.1" );
		logFileBackup2 = configDirectory.resolve( "hypnos.log.2" );
		
		if ( Files.exists( logFileBackup ) ) {
			try {
				Files.move( logFileBackup, logFileBackup2, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );
			} catch ( Exception e ) {
				LOGGER.log ( Level.WARNING, "Unable to create 2nd backup logfile" );
			}
		}
		
		if ( Files.exists( logFile ) ) {
			try {
				Files.move( logFile, logFileBackup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE  );
			} catch ( Exception e ) {
				LOGGER.log ( Level.WARNING, "Unable to create 1st backup logfile" );
			}
		}
				
		try {
			logFile.toFile().createNewFile();
		} catch ( Exception e ) {
			LOGGER.log ( Level.WARNING, "Unable to create logfile", e );
		}
		
		try {
			PrintWriter logOut = new PrintWriter ( new FileOutputStream ( logFile.toFile(), false ) );
			logOut.print( logBuffer.toString() );
			logOut.close();
			
		} catch ( Exception e ) {
			LOGGER.log ( Level.WARNING, "Unable to write initial log entries to log file", e );
		}
		
		try {
			FileHandler fileHandler = new FileHandler( logFile.toString(), true );     
			fileHandler.setFormatter( logFormat );
			
			Logger.getLogger( "" ).removeHandler( consoleHandler );
	        Logger.getLogger("").addHandler( fileHandler );
	        
		} catch ( IOException e ) {
			LOGGER.log( Level.WARNING, "Unable to setup file handler for logger.", e );
		}
	}
	
	private void setupJavaLibraryPath() {
		// This makes it so the user doensn't have to specify -Djava.libary.path="./lib" at the 
		// command line, making the .jar's double-clickable
		System.setProperty( "java.library.path", getRootDirectory().resolve ("lib").toString() );
		
		try {
			Field fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
			fieldSysPath.setAccessible( true );
			fieldSysPath.set( null, null );
			
		} catch (NoSuchFieldException|SecurityException|IllegalArgumentException|IllegalAccessException e1) {
			LOGGER.warning( "Unable to set java.library.path. A crash is likely imminent, but I'll try to continue running.");
		}
	}
	
	private void startGlobalHotkeyListener() {
		hotkeys = GlobalHotkeys.start();
	}
	
	public static boolean globalHotkeysDisabled() {
		return globalHotkeysDisabled;
	}
	
	public static boolean hotkeysDisabledForConfig () {
		return ui.hotkeysDisabledForConfig();
	}

	public static void doHotkeyAction ( Hotkey hotkey ) {
		Platform.runLater( () -> {
			switch ( hotkey ) {
				case NEXT:
					audioSystem.next();
					break;
				case PLAY:
					audioSystem.play();
					break;
				case PREVIOUS:
					audioSystem.previous();
					break;
				case SHOW_HIDE_UI:
					ui.toggleMinimized();
					break;
				case SKIP_BACK:
					long target = audioSystem.getPositionMS() - 5000 ;
					if ( target < 0 ) target = 0;
					audioSystem.seekMS( target ); 
					break;
				case SKIP_FORWARD:
					audioSystem.seekMS( audioSystem.getPositionMS() + 5000 ); 
					break;
				case STOP:
					audioSystem.stop( StopReason.USER_REQUESTED );
					break;
				case TOGGLE_MUTE:
					audioSystem.toggleMute();
					break;
				case TOGGLE_PAUSE:
					audioSystem.togglePause();
					break;
				case TOGGLE_REPEAT:
					audioSystem.toggleRepeatMode();
					break;
				case TOGGLE_SHUFFLE:
					audioSystem.toggleShuffleMode();
					break;
				case VOLUME_DOWN:
					audioSystem.decrementVolume();
					break;
				case VOLUME_UP:
					audioSystem.incrementVolume();
					break;
				default:
					break;
			}
		});
	}
	
	public static void warnUserVolumeNotSet() {
		ui.warnUserVolumeNotSet();
	}
	
	public static void warnUserPlaylistsNotSaved ( ArrayList <Playlist> errors ) {
		ui.warnUserPlaylistsNotSaved ( errors );
	}

	public static void warnUserAlbumsMissing ( List <Album> missing ) {
		ui.warnUserAlbumsMissing ( missing );
	}
	
	
	private long setTracksLastTime = 0;
	
	@SuppressWarnings("unchecked")
	public void applyCLICommands ( List <SocketCommand> commands ) {
		
		ArrayList<Path> newList = new ArrayList<Path>();
		for ( SocketCommand command : commands ) {
			if ( command.getType() == SocketCommand.CommandType.SET_TRACKS ) {
				
				for ( File file : (List<File>) command.getObject() ) {
					if ( file.isDirectory() ) {
						newList.addAll( Utils.getAllTracksInDirectory( file.toPath() ) );
						
					} else if ( Utils.isPlaylistFile( file.toPath() ) ) {
						//TODO: It's kind of lame to load the tracks here just to discard them and load them again a few seconds later
						//Maybe modify loadPlaylist so i can just ask for the specified paths, without loading tag data
						Playlist playlist = Playlist.loadPlaylist( file.toPath() );
						for ( Track track : playlist.getTracks() ) {
							newList.add( track.getPath() );
						}
						
					} else if ( Utils.isMusicFile( file.toPath() ) ) {
						newList.add( file.toPath() );
						
					} else {
						LOGGER.info( "Recived non-music, non-playlist file, ignoring: " + file );
					}
				}
			}
		}
		
		if ( newList.size() > 0 ) {
			if ( System.currentTimeMillis() - setTracksLastTime > 5000 ) {
				Platform.runLater( () -> {
					audioSystem.getCurrentList().setTracksPathList( newList,
						new Runnable() {
							@Override
							public void run() {
								audioSystem.next( false );
							}
						}
					);
				});
			} else {
				Platform.runLater( () -> {
					audioSystem.getCurrentList().appendTracksPathList ( newList );
				});
			}
			setTracksLastTime = System.currentTimeMillis();
		}

		for ( SocketCommand command : commands ) {
			if ( command.getType() == SocketCommand.CommandType.CONTROL ) {
				int action = (Integer)command.getObject();
				Platform.runLater( () -> {
					switch ( action ) {
						case SocketCommand.NEXT: 
							audioSystem.next();
							break;
						case SocketCommand.PREVIOUS:
							audioSystem.previous();
							break;
						case SocketCommand.PAUSE:
							audioSystem.pause();
							break;
						case SocketCommand.PLAY:
							audioSystem.unpause();
							break;
						case SocketCommand.TOGGLE_PAUSE:
							audioSystem.togglePause();
							break;
						case SocketCommand.STOP:
							audioSystem.stop( StopReason.USER_REQUESTED );
							break;
						case SocketCommand.TOGGLE_MINIMIZED:
							ui.toggleMinimized();
							break;
						case SocketCommand.VOLUME_DOWN:
							audioSystem.decrementVolume();
							break;
						case SocketCommand.VOLUME_UP:
							audioSystem.incrementVolume();
							break;
						case SocketCommand.SEEK_BACK:
							long target = audioSystem.getPositionMS() - 5000 ;
							if ( target < 0 ) target = 0;
							audioSystem.seekMS( target ); 
							break;
						case SocketCommand.SEEK_FORWARD:
							audioSystem.seekMS( audioSystem.getPositionMS() + 5000 ); 
							break;
						case SocketCommand.SHOW:
							ui.restoreWindow();
							break;
					}
				});
			} 
		}
	}

	public static void exit ( ExitCode exitCode ) {
		LOGGER.info( "Exit requested: " + exitCode.toString() );
		EnumMap <Setting, ? extends Object> fromAudioSystem = audioSystem.getSettings();
		EnumMap <Setting, ? extends Object> fromUI = ui.getSettings();
		audioSystem.stop ( StopReason.USER_REQUESTED );
		persister.saveAllData( fromAudioSystem, fromUI );
		System.exit ( exitCode.ordinal() );
	}
	
	@Override
	public void stop () {
		exit ( ExitCode.NORMAL );
	}
	
	@Override
	public void start ( Stage stage ) {
		try {
			startLogToBuffer();
			setupInitialLoggerFormat();
			parseSystemProperties();
			determineOS();
			setupRootDirectory(); 
			setupConfigDirectory();
			setupJavaLibraryPath();
			determineVersionInfo();

			String[] args = getParameters().getRaw().toArray ( new String[0] );
			CLIParser parser = new CLIParser( );
			ArrayList <SocketCommand> commands = parser.parseCommands( args );
			
			SingleInstanceController singleInstanceController = new SingleInstanceController();

			if ( singleInstanceController.isFirstInstance() ) {
				setupLogFile();
				library = new Library();
				audioSystem = new AudioSystem();
				startGlobalHotkeyListener();
				
				ui = new FXUI ( stage, library, audioSystem, hotkeys );
				audioSystem.setUI ( ui );
				libraryUpdater = new LibraryUpdater ( library, audioSystem, ui );
				
				persister = new Persister ( ui, library, audioSystem, hotkeys );
				
				switch ( getOS() ) {
					case NIX:
					case OSX: {
						EnumMap <Setting, String> pendingSettings = persister.loadSettingsFromDisk();
						persister.loadCurrentList();
						ui.applySettingsBeforeWindowShown( pendingSettings );
						audioSystem.applySettings ( pendingSettings );
						
						//TODO: This def doesn't belong here. 
						if ( pendingSettings.containsKey( Setting.LOADER_SPEED ) ) {
							Hypnos.setLoaderSpeed( LoaderSpeed.valueOf( pendingSettings.get( Setting.LOADER_SPEED ) ) );
							pendingSettings.remove( Setting.LOADER_SPEED );
						}

						ui.setLibraryLabelsToLoading();
						ui.showMainWindow();
						
						Thread finishLoadingThread = new Thread ( () -> {
							Platform.runLater( () -> {
								ui.applySettingsAfterWindowShown( pendingSettings );
								persister.logUnusedSettings ( pendingSettings );
							});

							boolean sourcesLoaded = persister.loadSources();
							if ( sourcesLoaded ) {
								persister.loadAlbumsAndTracks();
							}
							
							persister.loadQueue();
							audioSystem.linkQueueToCurrentList();
							persister.loadHistory();
							persister.loadPlaylists();
							persister.loadHotkeys();
							
							Platform.runLater( () -> ui.libraryPane.updateLibraryListPlaceholder() );
							
							ui.refreshHotkeyList();
							
							audioSystem.start();
							
							applyCLICommands( commands );
							singleInstanceController.startCLICommandListener ( this );
			
							libraryUpdater.start();
							library.startLoader( persister );
							
							LOGGER.info( "Hypnos finished loading." );
							
							UpdateChecker updater = new UpdateChecker();
							boolean updateAvailable = updater.updateAvailable();
							if ( updateAvailable ) LOGGER.info( "Updates available" );
							ui.setUpdateAvailable ( updateAvailable );
							
							try { Thread.sleep ( 2000 ); } catch ( InterruptedException e ) {}
							
							ui.fixTables();
						} );
						
						finishLoadingThread.setName ( "Hypnos Load Finisher for Nix" );
						finishLoadingThread.setDaemon( false );
						finishLoadingThread.start();
					} 
					break;
					
					case UNKNOWN:
					case WIN_10:
					case WIN_7:
					case WIN_8:
					case WIN_UNKNOWN:
					case WIN_VISTA:
					case WIN_XP:
					default: {
						EnumMap <Setting, String> pendingSettings = persister.loadSettingsFromDisk();
						persister.loadCurrentList();
						audioSystem.applySettings ( pendingSettings );
						
						//TODO: This def doesn't belong here. 
						if ( pendingSettings.containsKey( Setting.LOADER_SPEED ) ) {
							Hypnos.setLoaderSpeed( LoaderSpeed.valueOf( pendingSettings.get( Setting.LOADER_SPEED ) ) );
							pendingSettings.remove( Setting.LOADER_SPEED );
						}
						
						ui.applySettingsBeforeWindowShown( pendingSettings );
						ui.applySettingsAfterWindowShown( pendingSettings );
						
						persister.logUnusedSettings ( pendingSettings );
						
						boolean sourcesLoaded = persister.loadSources();
						if ( sourcesLoaded ) {
							persister.loadAlbumsAndTracks();
						}
						
						persister.loadQueue();
						audioSystem.linkQueueToCurrentList();
						persister.loadHistory();
						persister.loadPlaylists();
						persister.loadHotkeys();
						
						audioSystem.start();
						
						applyCLICommands( commands );
						singleInstanceController.startCLICommandListener ( this );
		
						libraryUpdater.start();
						library.startLoader( persister );
						ui.showMainWindow();
						ui.libraryPane.updateLibraryListPlaceholder();
						ui.fixTables();
						ui.settingsWindow.refreshHotkeyFields();
						
						LOGGER.info( "Hypnos finished loading." );

						UpdateChecker updater = new UpdateChecker();
						boolean updateAvailable = updater.updateAvailable();
						if ( updateAvailable ) LOGGER.info( "Updates available" );
					} 
					break;
				}

			} else {
				singleInstanceController.sendCommandsThroughSocket( commands );
				if ( commands.size() > 0 ) {
					System.out.println ( "Commands sent to currently running Hypnos." );
				} else {
					singleInstanceController.sendCommandsThroughSocket( Arrays.asList(
							new SocketCommand ( SocketCommand.CommandType.CONTROL, SocketCommand.SHOW )
					) );
					String message = "Hypnos is already running.";
					System.out.println ( message );
					FXUI.notifyUserHypnosRunning();
				}
				
				System.exit ( 0 ); //We don't use Hypnos.exit here intentionally. 
			}
			
		} catch ( Exception e ) {
			LOGGER.log( Level.SEVERE, e.getClass() + ":  Exception caught at top level of Hypnos. Exiting.", e );
			exit ( ExitCode.UNKNOWN_ERROR );
		}
	}

	public static void main ( String[] args ) {
		launch( args ); //This calls start()
	}
}

