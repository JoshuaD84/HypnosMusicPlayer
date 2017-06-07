package net.joshuad.musicplayer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CLIParser {
	
	private static final String HELP = "help";
	private static final String NEXT = "next";
	private static final String PREVIOUS = "previous";
	private static final String PAUSE = "pause";
	private static final String PLAY = "play";
	private static final String TOGGLE_PAUSE = "play-pause";
	private static final String STOP = "stop";
	
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
		
		parser = new DefaultParser();
		
	}
	
	public ArrayList <Path> parseFiles ( String [] args ) {
		
		ArrayList<Path> retMe = new ArrayList<Path> ();
		
		try {
			CommandLine line = parser.parse( options, args );
			
			for ( String leftOverArgument : line.getArgList() ) {
				retMe.add( Paths.get( leftOverArgument ) );
			}

		} catch ( ParseException e ) {
			System.out.println ( "Error parsing commandline options, continuing." );
		}

		return retMe;
	}
	
	public ArrayList <Integer> parseCommands ( String[] args ) {
		 
		ArrayList <Integer> retMe = new ArrayList <Integer> ();
		
		try {
			CommandLine line = parser.parse( options, args );
			
			if ( line.hasOption( HELP ) ) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "Hypnos Music Player <options>", options );
				System.exit( 0 );
			}
			
			if ( line.hasOption( NEXT ) ) {
				retMe.add( SingleInstanceController.NEXT );
			}
			
			if ( line.hasOption( PREVIOUS ) ) {
				retMe.add( SingleInstanceController.PREVIOUS );
			}
			
			if ( line.hasOption( PAUSE ) ) {
				retMe.add( SingleInstanceController.PAUSE );
			}
			
			if ( line.hasOption( PLAY ) ) {
				retMe.add( SingleInstanceController.PLAY );
			}
			
			if ( line.hasOption( TOGGLE_PAUSE ) ) {
				retMe.add( SingleInstanceController.TOGGLE_PAUSE );
			}
			
			if ( line.hasOption( STOP ) ) {
				retMe.add( SingleInstanceController.STOP );
			}
			
		} catch ( ParseException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return retMe;
	}
	
	

}
