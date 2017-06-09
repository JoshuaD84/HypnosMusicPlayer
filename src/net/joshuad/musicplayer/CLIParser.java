package net.joshuad.musicplayer;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CLIParser {
	
	//TODO: Combine these into an Enum with the things in SingleInstanceController and the strings in constructor below. 
	private static final String HELP = "help";
	private static final String NEXT = "next";
	private static final String PREVIOUS = "previous";
	private static final String PAUSE = "pause";
	private static final String PLAY = "play";
	private static final String TOGGLE_PAUSE = "play-pause";
	private static final String STOP = "stop";
	private static final String TOGGLE_MINIMIZED = "toggle-window";
	
	CommandLineParser parser;
	Options options;
	
	
	public CLIParser ( ) {

		options = new Options();
		options.addOption( null, HELP, false, "Print this message" );
		options.addOption( null, NEXT, false, "Jump to next track" );
		options.addOption( null, PREVIOUS, false, "Jump to previous track" );
		options.addOption( null, PAUSE, false, "Pause Playback" );
		options.addOption( null, PLAY, false, "Start Playback" );
		options.addOption( null, TOGGLE_PAUSE, false, "Toggle play/pause mode" );
		options.addOption( null, STOP, false, "Stop playback" );
		options.addOption( null, TOGGLE_MINIMIZED, false, "Toggle the minimized state" );
		
		parser = new DefaultParser();
		
	}
	
	public ArrayList <SocketCommand> parseCommands ( String[] args ) {
		 
		ArrayList <SocketCommand> retMe = new ArrayList <SocketCommand> ();
		
		try {
			CommandLine line = parser.parse( options, args );
			
			if ( line.hasOption( HELP ) ) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "Hypnos Music Player <options>", options );
				System.exit( 0 );
			}

			if ( line.hasOption( PLAY ) ) {
				retMe.add( new SocketCommand ( SocketCommand.CommandType.CONTROL, SingleInstanceController.PLAY ) );
			}
			
			if ( line.hasOption( NEXT ) ) {
				retMe.add( new SocketCommand ( SocketCommand.CommandType.CONTROL, SingleInstanceController.NEXT ) );
			}
			
			if ( line.hasOption( PREVIOUS ) ) {
				retMe.add( new SocketCommand ( SocketCommand.CommandType.CONTROL, SingleInstanceController.PREVIOUS ) );
			}
			
			if ( line.hasOption( TOGGLE_PAUSE ) ) {
				retMe.add( new SocketCommand ( SocketCommand.CommandType.CONTROL, SingleInstanceController.TOGGLE_PAUSE ) );
			}

			
			if ( line.hasOption( PAUSE ) ) {
				retMe.add( new SocketCommand ( SocketCommand.CommandType.CONTROL, SingleInstanceController.PAUSE ) );
			}
			
			if ( line.hasOption( STOP ) ) {
				retMe.add( new SocketCommand ( SocketCommand.CommandType.CONTROL, SingleInstanceController.STOP ) );
			}
			
			if ( line.hasOption( TOGGLE_MINIMIZED ) ) {
				retMe.add( new SocketCommand ( SocketCommand.CommandType.CONTROL, SingleInstanceController.TOGGLE_MINIMIZED ) );
			}
			
			ArrayList<File> filesToLoad = new ArrayList<File> ();
			for ( String leftOverArgument : line.getArgList() ) {
				filesToLoad.add( Paths.get( leftOverArgument ).toFile() );
			}
			
			if ( filesToLoad.size() > 0 ) {
				retMe.add( new SocketCommand ( SocketCommand.CommandType.LOAD_TRACKS, filesToLoad ) );
			}
		
		} catch ( ParseException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return retMe;
	}
	
	

}
