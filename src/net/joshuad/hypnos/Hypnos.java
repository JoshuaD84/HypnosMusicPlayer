package net.joshuad.hypnos;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.io.output.TeeOutputStream;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;
import net.joshuad.hypnos.fxui.FXUI;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys.Hotkey;

public class Hypnos extends Application {

	private static final Logger LOGGER = Logger.getLogger( Hypnos.class.getName() );
	
	private static String versionString = "Beta 1 - 2017/10/16";

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
	}
	
	private static OS os = OS.UNKNOWN;
	private static Path rootDirectory;
	private static Path configDirectory;
	private static Path logFile;
	private static boolean isStandalone = false;
	private static boolean isDeveloping = false;
	private static boolean disableHotkeys = false;
	
	private static Persister persister;
	private static AudioSystem player;
	private static FXUI ui;
	private LibraryUpdater libraryUpdater;
	private static Library library;
	private GlobalHotkeys hotkeys;
	
	private static PrintStream originalOut;
	private static PrintStream originalErr;
	
	private static ByteArrayOutputStream logBuffer; //Used to store log info until log file is initialized
	
	
	public static OS getOS() {
		return os;
	}
	
	public static String getVersionString () {
		return versionString;
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
	
	public static Persister getPersister() {
		return persister;
	}
	
	public static Library getLibrary() {
		return library;
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
		disableHotkeys = Boolean.getBoolean( "hypnos.disableglobalhotkeys" );
		
		if ( isStandalone ) LOGGER.info ( "Running as standalone" );
		if ( isDeveloping ) LOGGER.info ( "Running on development port" );
		if ( isDeveloping ) LOGGER.info ( "Global hotkeys disabled" );
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
		
		LOGGER.config ( "Operating System: " + os.getDisplayName() );
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
		// TODO: We might want to make a few fall-throughs if these locations
		// don't exist.
		// TODO: Test this on each OS. 
		String home = System.getProperty( "user.home" );

		if ( Hypnos.isStandalone() ) {
			configDirectory = getRootDirectory().resolve( "config" );

		} else {
			final String x = File.separator;
			switch ( getOS() ) {
				case NIX:
					configDirectory = Paths.get( home + x + ".hypnos" );
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
			boolean created = configDirectory.mkdirs();
			// TODO: check created
		}

		if ( !configDirectory.isDirectory() ) {
			// TODO:
		}
	}
	
	private void setupLogFile() {
		logFile = configDirectory.resolve( "hypnos.log" );
		try {
			
			logFile.toFile().createNewFile();
			PrintWriter logOut = new PrintWriter ( new FileOutputStream ( logFile.toFile(), false ) );
			logOut.print( logBuffer.toString() );
			logOut.close();
			
			System.setOut( originalOut );
			System.setErr( originalErr );
			
			FileHandler fileHandler = new FileHandler( logFile.toString(), true );     
			fileHandler.setFormatter( new Formatter() {
				SimpleDateFormat dateFormat = new SimpleDateFormat ( "MMM d, yyyy HH:mm:ss aaa" );
				public String format ( LogRecord record ) {
					
					String retMe = dateFormat.format( new Date ( record.getMillis() ) )
						  + " " + record.getLoggerName()
						  + " " + record.getSourceMethodName()
						  + "\n"
						  + record.getLevel() + ": " + record.getMessage()
						  + "\n";
					
					return retMe;
					
				}
			} );
			
	        Logger.getLogger("").addHandler( fileHandler );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		
		if ( !disableHotkeys ) {
			PrintStream out = System.out;
			
			try {
				//This suppresses the lgpl banner from the hotkey library. 
				System.setOut( new PrintStream ( new OutputStream() {
				    @Override public void write(int b) throws IOException {}
				}));
			
				LogManager.getLogManager().reset();
				Logger logger = Logger.getLogger( GlobalScreen.class.getPackage().getName() );
				logger.setLevel( Level.OFF );
	
				GlobalScreen.registerNativeHook();
				
			} catch ( NativeHookException ex ) {
				LOGGER.warning( "There was a problem registering the global hotkey listeners. Global Hotkeys are disabled." );
				//TODO: set a boolean and put an indicator in the UI somewhere? 
			} finally {
				System.setOut( out );
			}
		}
		
		hotkeys = new GlobalHotkeys();
		
		if ( !disableHotkeys ) {
			GlobalScreen.addNativeKeyListener( hotkeys );
		}
	}
	
	public static boolean hotkeysEnabled () {
		return ui.hotkeysEnabled();
	}

	public static void doHotkeyAction ( Hotkey hotkey ) {
		Platform.runLater( () -> {
			switch ( hotkey ) {
				case NEXT:
					player.next();
					break;
				case PLAY:
					player.play();
					break;
				case PREVIOUS:
					player.previous();
					break;
				case SHOW_HIDE_UI:
					ui.toggleMinimized();
					break;
				case SKIP_BACK:
					//TODO: 
					break;
				case SKIP_FORWARD:
					//TODO: 
					break;
				case STOP:
					player.stop( StopReason.USER_REQUESTED );
					break;
				case TOGGLE_MUTE:
					//TODO: 
					break;
				case TOGGLE_PAUSE:
					player.togglePause();
					break;
				case TOGGLE_REPEAT:
					player.toggleRepeatMode();
					break;
				case TOGGLE_SHUFFLE:
					player.toggleShuffleMode();
					break;
				case VOLUME_DOWN:
					player.decrementVolume();
					break;
				case VOLUME_UP:
					player.incrementVolume();
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
	
	@SuppressWarnings("unchecked")
	public void applyCLICommands ( ArrayList <SocketCommand> commands ) {
		Platform.runLater( () -> {
			for ( SocketCommand command : commands ) {
				if ( command.getType() == SocketCommand.CommandType.SET_TRACKS ) {
					ArrayList<Path> newList = new ArrayList<Path>();
					
					for ( File file : (List<File>) command.getObject() ) {
						if ( file.isDirectory() ) {
							newList.addAll( Utils.getAllTracksInDirectory( file.toPath() ) );
						} else {
							newList.add( file.toPath() );
						}
					}
					
					if ( newList.size() > 0 ) {
						player.getCurrentList().setTracksPathList( newList );
					}
				}
			}
	
			for ( SocketCommand command : commands ) {
				if ( command.getType() == SocketCommand.CommandType.CONTROL ) {
					int action = (Integer)command.getObject();
	
					switch ( action ) {
						case SocketCommand.NEXT: 
							player.next();
							break;
						case SocketCommand.PREVIOUS:
							player.previous();
							break;
						case SocketCommand.PAUSE:
							player.pause();
							break;
						case SocketCommand.PLAY:
							player.unpause();
							break;
						case SocketCommand.TOGGLE_PAUSE:
							player.togglePause();
							break;
						case SocketCommand.STOP:
							player.stop( StopReason.USER_REQUESTED );
							break;
						case SocketCommand.TOGGLE_MINIMIZED:
							ui.toggleMinimized();
							break;
						case SocketCommand.VOLUME_DOWN:
							player.decrementVolume();
							break;
						case SocketCommand.VOLUME_UP:
							player.incrementVolume();
							break;
					}
				} 
			}
		});
	}

	public static void exit ( ExitCode exitCode ) {
		persister.saveAllData();
		player.stop ( StopReason.USER_REQUESTED );
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
			parseSystemProperties();
			determineOS();
			setupRootDirectory(); 
			setupConfigDirectory();
			setupLogFile();
			
			setupJavaLibraryPath();
			
			String[] args = getParameters().getRaw().toArray ( new String[0] );
			CLIParser parser = new CLIParser( );
			ArrayList <SocketCommand> commands = parser.parseCommands( args );
			
			SingleInstanceController singleInstanceController = new SingleInstanceController();
					
			if ( singleInstanceController.isFirstInstance() ) {
				library = new Library();
				player = new AudioSystem();
				startGlobalHotkeyListener();
				
				ui = new FXUI ( stage, library, player, hotkeys );
				
				persister = new Persister ( ui, library, player, hotkeys );
				
				persister.loadDataBeforeShowWindow();
				ui.showMainWindow();
				persister.loadDataAfterShowWindow();
				
				libraryUpdater = new LibraryUpdater ( library, ui );
				
				applyCLICommands( commands );
				
				singleInstanceController.startCLICommandListener ( this );
				library.startLoader( persister );
				
				LOGGER.info( "Hypnos finished loading." );
				
								
			} else {
				singleInstanceController.sendCommandsThroughSocket( commands );
				if ( commands.size() > 0 ) {
					System.out.println ( "Commands sent to currently running Hypnos." );
				} else {
					System.out.println ( "Hypnos is already running." );
				}
				
				System.exit ( 0 ); //We don't use Hypnos.exit here intentionally. 
			}
			
		} catch ( Exception e ) {
			LOGGER.log( Level.SEVERE, "Exception caught at top level of Hypnos. Exiting.", e );
			exit ( ExitCode.UNKNOWN_ERROR );
		}
	}

	public static void main ( String[] args ) {
		launch( args ); //This calls start()
	}
}
