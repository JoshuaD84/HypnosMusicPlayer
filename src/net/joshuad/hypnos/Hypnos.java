package net.joshuad.hypnos;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.stage.Stage;
import net.joshuad.hypnos.fxui.FXUI;

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
	}
	
	
	private static OS os = OS.UNKNOWN;
	private static boolean IS_STANDALONE = false;
	private static boolean IS_DEVELOPING = false;
	private static Path ROOT;
	private static Library library;
	
	private static Persister persister;
	private static SoundSystem player;
	private FXUI ui;
	private LibraryUpdater libraryUpdater;
	
	public static Library library() {
		return library;
	}
	
	public static OS getOS() {
		return os;
	}
	
	public static boolean isStandalone() {
		return IS_STANDALONE;
	}
	
	public static boolean isDeveloping() {
		return IS_DEVELOPING;
	}
	
	public static Path getRootDirectory() {
		return ROOT;
	}
	
	private void parseSystemProperties() {
				
		IS_STANDALONE = Boolean.getBoolean( "hypnos.standalone" );
		IS_DEVELOPING = Boolean.getBoolean( "hypnos.developing" );
		
		//TODO: Logging instead of print
		if ( IS_STANDALONE ) LOGGER.info ( "Running as standalone" );
		if ( IS_DEVELOPING ) LOGGER.info ( "Running on development port" );
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
	
	
	private void setupRootDirectory () {
		String path = FXUI.class.getProtectionDomain().getCodeSource().getLocation().getPath();
				
		try {
			String decodedPath = URLDecoder.decode(path, "UTF-8");
			decodedPath = decodedPath.replaceFirst("^/(.:/)", "$1");
			ROOT = Paths.get( decodedPath ).getParent();
			
		} catch ( UnsupportedEncodingException e ) {
			ROOT = Paths.get( path ).getParent();
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
			//TODO: Logging
			System.out.println ( "Unable to set java.library.path. A crash is likely imminent, but I'll try to continue running.");
		}
	}

	public static void exit ( ExitCode exitCode ) {
		persister.saveAllData();
		player.stopTrack();
		System.exit ( exitCode.ordinal() );
	}
	
	@Override
	public void stop () {
		exit ( ExitCode.NORMAL );
	}
	
	@Override
	public void start ( Stage stage ) {
		try {
			parseSystemProperties();
			determineOS();
			setupRootDirectory(); 
			setupJavaLibraryPath();
			
			CLIParser parser = new CLIParser( );
			String[] args = getParameters().getRaw().toArray(new String[0]);
			ArrayList <SocketCommand> commands = parser.parseCommands( args );
			
			SingleInstanceController singleInstanceController = new SingleInstanceController();
					
			if ( singleInstanceController.isFirstInstance() ) {
				library = new Library();
				player = new SoundSystem();
				ui = new FXUI ( stage, player );
				
				persister = new Persister( ui, player );
				
				persister.loadDataBeforeShowWindow();
				ui.showMainWindow();
				persister.loadDataAfterShowWindow();
				
				libraryUpdater = new LibraryUpdater ( ui );
				
				ui.takeRemoteCommand( commands ); //TODO: maybe pass these to player instead? 
				
				library.startLoader( persister );
				
			} else {
				singleInstanceController.sendCommandsThroughSocket( commands );
				System.out.println ( "Not first instance, sent commands, now exiting." ); //TODO: Logging instead of print
				System.exit ( 0 ); //TODO: Use exit ()
			}
			
		} catch ( Exception e ) {
			LOGGER.log( Level.SEVERE, "Exception caught at top level of Hypnos. Exiting.", e );
			exit ( ExitCode.UNKNOWN_ERROR );
		}
	}

	public static void main ( String[] args ) {
		launch( args ); //This calls start() above. 
	}
}
