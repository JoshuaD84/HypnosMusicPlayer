package org.joshuad.musicplayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.collections.ListChangeListener;

public class MusicLoaderDaemon implements ListChangeListener <Path> {
	
	Vector <Path> loadMe = new Vector <Path> ();
	Thread loaderThread;
	
	public MusicLoaderDaemon() {}
	
	public void start() {
		
		loaderThread = new Thread ( new Runnable() {
			
			public void run() {
				while ( true ) {
					if ( !loadMe.isEmpty() ) {
						Path selectedPath = loadMe.remove( 0 );
						
						Logger.getLogger( "org.jaudiotagger" ).setLevel( Level.OFF );
						long startTime = System.currentTimeMillis();
						
						try {
							Files.walkFileTree( selectedPath, new MusicFileVisitor( MusicPlayerUI.albums, MusicPlayerUI.tracks ) );
						} catch ( IOException e ) {
							System.out.println ("Unable to load files in path: " + selectedPath.toString() );
							e.printStackTrace();
						}
						
						long endTime = System.currentTimeMillis();
	
						System.out.println( "Time to load all tracks (path: " + selectedPath.toString() + "): " + (endTime - startTime) );
						
					} else {
						try { Thread.sleep( 1000 ); } catch ( InterruptedException e ) { e.printStackTrace(); }
					}
				}
				
			}
		});
		
		loaderThread.setDaemon( true );
		loaderThread.start();
	}
	
	public void addPath ( Path path ) {
		loadMe.add( path );
	}

	@Override
	public void onChanged ( javafx.collections.ListChangeListener.Change <? extends Path> changes ) {
		
		while ( changes.next() ) {
			if ( changes.wasAdded() ) {
				List <? extends Path> addedPaths = changes.getAddedSubList();
				
				for ( Path path : addedPaths ) {
					loadMe.add( path );
				}
			}
		}
	}

}
