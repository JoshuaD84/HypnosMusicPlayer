package net.joshuad.hypnos;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.stage.Stage;

public class Hypnos extends Application {
	
	enum ExitCode {
		NORMAL ( 0 );
		
		private int value;
		ExitCode ( int value ) { this.value = value; }
		public int getValue() { return value; }
	}
	
	public static boolean IS_STANDALONE = false;
	public static boolean IS_DEVELOPING = false;
	public static Path ROOT;
	
	//TODO: Make these not static and not public
	public static PlayerController player;
	public static FXUI ui;
	public static Library library;
	public static Queue queue;
	public static Persister persister;
	public static UIUpdater uiUpdater;
	
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
	
	/* TODO
	public static void setupJavaLibraryPath() {
		// This is a cool little hack that makes it so 
		// the user doensn't have to specify -Djava.libary.path="./lib" at the 
		// command line, making the .jar's double-clickable
		System.setProperty( "java.library.path", "./" + ConfigManager.libraryPath );
		
		try {
			Field fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
			fieldSysPath.setAccessible( true );
			fieldSysPath.set( null, null );
			
		} catch (NoSuchFieldException|SecurityException|IllegalArgumentException|IllegalAccessException e1) {
			System.out.println ( "Unable to set java.library.path. A crash is likely imminent, but I'll try to continue running.");
		}
	}
	*/
	
	public void exit ( ExitCode exitCode ) {
		player.stopTrack();
		ui.stoppedPlaying();
		persister.saveAllData( player, ui );
		System.exit ( exitCode.getValue() );
	}
	
	@Override
	public void stop () {
		exit ( ExitCode.NORMAL );
	}
	
	@Override
	public void start ( Stage stage ) {
		parseSystemProperties();
		setupRootDirectory(); 
		
		CLIParser parser = new CLIParser( );
		String[] args = getParameters().getRaw().toArray(new String[0]);
		ArrayList <SocketCommand> commands = parser.parseCommands( args );
		
		boolean firstInstance = SingleInstanceController.startCLICommandListener();
		
		if ( firstInstance ) {
			persister = new Persister();
			library = new Library();
			queue = new Queue();
			player = new PlayerController();
			ui = new FXUI ( stage, player );
			
			persister.loadDataBeforeShowWindow( player, ui );
			ui.showMainWindow();
			persister.loadDataAfterShowWindow( player, ui );
			
			uiUpdater = new UIUpdater ( ui );
			SingleInstanceController.sendCommandToUI( commands );
			
			library.startLoader();
			
		} else {
			SingleInstanceController.sendCommandsThroughSocket( commands );
			System.out.println ( "Not first instance, sent commands, now exiting." ); //TODO: Logging instead of print
			System.exit ( 0 ); //TODO: Use exit ()
		}
	}

	public static void main ( String[] args ) {
		Application.launch( args ); //This calls start() above. 
	}
}
