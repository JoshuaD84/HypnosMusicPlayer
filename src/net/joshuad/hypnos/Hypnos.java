package net.joshuad.hypnos;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import net.joshuad.hypnos.audio.AudioSystem;
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
	private static Path rootDirectory;
	private static boolean isStandalone = false;
	private static boolean isDeveloping = false;
	
	private static Persister persister;
	private static AudioSystem player;
	private FXUI ui;
	private LibraryUpdater libraryUpdater;
	private Library library;
	
	public static OS getOS() {
		return os;
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
	
	private void parseSystemProperties() {
				
		isStandalone = Boolean.getBoolean( "hypnos.standalone" );
		isDeveloping = Boolean.getBoolean( "hypnos.developing" );
		
		if ( isStandalone ) LOGGER.config ( "Running as standalone" );
		if ( isDeveloping ) LOGGER.config ( "Running on development port" );
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
	
	@SuppressWarnings("unchecked")
	public void applyCommands ( ArrayList <SocketCommand> commands ) {
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
						player.setTracksPathList( newList );
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
							player.stop( true );
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
		player.stop( true );
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
			
			String[] args = getParameters().getRaw().toArray(new String[0]);

			CLIParser parser = new CLIParser( );
			ArrayList <SocketCommand> commands = parser.parseCommands( args );
			
			SingleInstanceController singleInstanceController = new SingleInstanceController();
					
			if ( singleInstanceController.isFirstInstance() ) {
				library = new Library();
				player = new AudioSystem();
				ui = new FXUI ( stage, library, player );
				
				persister = new Persister( ui, library, player );
				
				persister.loadDataBeforeShowWindow();
				ui.showMainWindow();
				persister.loadDataAfterShowWindow();
				
				libraryUpdater = new LibraryUpdater ( library, ui );
				
				applyCommands( commands );
				
				singleInstanceController.startCLICommandListener ( this );
				library.startLoader( persister );
				
			} else {
				singleInstanceController.sendCommandsThroughSocket( commands );
				System.out.println ( "Not first instance, sent commands, now exiting." ); //TODO: Logging instead of print
				System.exit ( 0 ); //We don't use exit here intentionally. 
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
