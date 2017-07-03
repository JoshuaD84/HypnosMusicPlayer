package net.joshuad.hypnos;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.stage.Stage;
import net.joshuad.hypnos.audio.PlayerController;
import net.joshuad.hypnos.fxui.FXUI;

public class Hypnos extends Application {
	
	public enum ExitCode {
		NORMAL,
		AUDIO_ERROR
	}
	
	public static boolean IS_STANDALONE = false;
	public static boolean IS_DEVELOPING = false;
	public static Path ROOT;
	
	private static Library library;
	private static Queue queue;
	
	private UIUpdater uiUpdater;
	private static Persister persister;
	private static PlayerController player;
	private FXUI ui;
	
	public static Library library() {
		return library;
	}
	
	public static Queue queue() {
		return queue; 
	}
	
	public void parseSystemProperties() {
				
		IS_STANDALONE = Boolean.getBoolean( "hypnos.standalone" );
		IS_DEVELOPING = Boolean.getBoolean( "hypnos.developing" );
		
		//TODO: Logging instead of print
		if ( IS_STANDALONE ) System.out.println ( "Running as standalone" );
		if ( IS_DEVELOPING ) System.out.println ( "Running on development port" );
	}
	
	public void setupRootDirectory () {
		String path = FXUI.class.getProtectionDomain().getCodeSource().getLocation().getPath();
				
		try {
			String decodedPath = URLDecoder.decode(path, "UTF-8");
			decodedPath = decodedPath.replaceFirst("^/(.:/)", "$1");
			ROOT = Paths.get( decodedPath ).getParent();
			
		} catch ( UnsupportedEncodingException e ) {
			ROOT = Paths.get( path ).getParent();
		}
	}
	
	public void setupJavaLibraryPath() {
		// This is a cool little hack that makes it so 
		// the user doensn't have to specify -Djava.libary.path="./lib" at the 
		// command line, making the .jar's double-clickable
		System.setProperty( "java.library.path", "./lib" );
		
		try {
			Field fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
			fieldSysPath.setAccessible( true );
			fieldSysPath.set( null, null );
			
		} catch (NoSuchFieldException|SecurityException|IllegalArgumentException|IllegalAccessException e1) {
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
			setupRootDirectory(); 
			setupJavaLibraryPath();
			
			CLIParser parser = new CLIParser( );
			String[] args = getParameters().getRaw().toArray(new String[0]);
			ArrayList <SocketCommand> commands = parser.parseCommands( args );
			
			SingleInstanceController singleInstanceController = new SingleInstanceController();
					
			boolean firstInstance = singleInstanceController.isFirstInstance();
			
			if ( firstInstance ) {
				library = new Library();
				queue = new Queue();
				player = new PlayerController();
				ui = new FXUI ( stage, player );
				
				persister = new Persister( ui, player );
				
				persister.loadDataBeforeShowWindow();
				ui.showMainWindow();
				persister.loadDataAfterShowWindow();
				
				uiUpdater = new UIUpdater ( ui );
				ui.takeRemoteCommand( commands );
				
				library.startLoader( persister );
				
			} else {
				singleInstanceController.sendCommandsThroughSocket( commands );
				System.out.println ( "Not first instance, sent commands, now exiting." ); //TODO: Logging instead of print
				System.exit ( 0 ); //TODO: Use exit ()
			}
			
		} catch ( Exception e ) {
			//TODO: 
			e.printStackTrace();
		}
	}

	public static void main ( String[] args ) {
		launch( args ); //This calls start() above. 
	}
}
