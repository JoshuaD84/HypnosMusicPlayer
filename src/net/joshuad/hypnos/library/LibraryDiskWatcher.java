package net.joshuad.hypnos.library;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.HypnosURLS;
import net.joshuad.hypnos.fxui.FXUI;
import net.joshuad.hypnos.Hypnos.OS;

class LibraryDiskWatcher {
	private static final Logger LOGGER = Logger.getLogger( LibraryDiskWatcher.class.getName() );

	private WatchService watcher;
	private final HashMap<WatchKey, Path> keys = new HashMap<WatchKey, Path>();

	private DelayedUpdateThread delayedUpdater;

	private FXUI ui;
	
	private Library library;

	LibraryDiskWatcher( Library library ) {
		
		this.library = library;
		
		delayedUpdater = new DelayedUpdateThread( library );
		
		try {
			watcher = FileSystems.getDefault().newWatchService();
			delayedUpdater.start();
		} catch ( IOException e ) {
			String message = "Unable to initialize file watcher, changes to file system while running won't be detected";
			LOGGER.log( Level.WARNING, message );
			ui.notifyUserError( message );
		}
	}
	
	void setUI ( FXUI ui ) {
		this.ui = ui;
	}

	void stopWatching( Path path ) {
		//TODO: Should we stop watching recursively? 
		for( WatchKey key : keys.keySet() ) {
			if( keys.get( key ).equals( path ) ) {
				library.getScanLogger().println( "[Watcher] stopping watch on: " + path.toString() );
				key.cancel();
			}
		}
	}
	
	void watchAll( final Path path ) {
		watchAll( path, null );
	}
	
	void watchAll( final MusicRoot musicRoot ) {
		watchAll( musicRoot.getPath(), null );
	}
	
	void watchAll( final Path path, final MusicRoot musicRoot ) {
		/* Note: in my testing, this takes less than a few seconds to run, depending on folder count */ 
		try {
			Files.walkFileTree( 
				path, 
				EnumSet.of( FileVisitOption.FOLLOW_LINKS ), 
				Integer.MAX_VALUE,
				new SimpleFileVisitor <Path>() {
					@Override
					public FileVisitResult preVisitDirectory ( Path dir, BasicFileAttributes attrs ) throws IOException {
						if ( !keys.containsValue( dir ) ) {
							WatchKey key = dir.register( 
								watcher, 
								StandardWatchEventKinds.ENTRY_CREATE, 
								StandardWatchEventKinds.ENTRY_DELETE, 
								StandardWatchEventKinds.ENTRY_MODIFY 
							);
							
							keys.put( key, dir );
						}
						return FileVisitResult.CONTINUE;
					}
				}
			);
		
		} catch ( IOException e ) {
			if ( Hypnos.getOS() == OS.NIX && e.getMessage().matches( ".*inotify.*" ) ) {

				if ( ui != null ) {
					ui.notifyUserLinuxInotifyIssue();
				}
				
				LOGGER.log( Level.INFO, e.getMessage() + "\nUnable to watch directory for changes: " + musicRoot.getPath().toString() +
					"\nSee here for how to fix this error on linux: " + HypnosURLS.HELP_INOTIFY 
				);
				
				if ( musicRoot != null ) {
					musicRoot.setHadInotifyError( true );
				}
			
			} else {
				LOGGER.log( Level.INFO, e.getMessage() + "\nUnable to watch directory for changes: " + musicRoot.getPath().toString(), e );
			}
		}
	}
	
	boolean processWatcherEvents () {
		WatchKey key;
		try {
			key = watcher.poll( 250, TimeUnit.MILLISECONDS );
		} catch ( InterruptedException e ) {
			return false;
		}
		
		Path directory = keys.get( key );
		if ( directory == null ) {
			return false;
		}
		
		for ( WatchEvent <?> event : key.pollEvents() ) {
			WatchEvent.Kind<?> eventKind = event.kind();

			WatchEvent <Path> watchEvent = (WatchEvent<Path>)event;
			Path child = directory.resolve( watchEvent.context() );
			
			if ( eventKind == StandardWatchEventKinds.ENTRY_CREATE ) {
				library.getScanLogger().println( "[Watcher] Heard create: " + child );
				delayedUpdater.addUpdateItem( child );
				
			} else if ( eventKind == StandardWatchEventKinds.ENTRY_DELETE ) {
				library.getScanLogger().println( "[Watcher] heard delete: " + child );
				delayedUpdater.addUpdateItem( child );
				
			} else if ( eventKind == StandardWatchEventKinds.ENTRY_MODIFY ) {
				library.getScanLogger().println( "[Watcher] heard modify: " + child );
				delayedUpdater.addUpdateItem( child );
			
			} else if ( eventKind == StandardWatchEventKinds.OVERFLOW ) {
				//TODO: Think about how to handle this
			}

			boolean valid = key.reset();
			if ( !valid ) {
				keys.remove( key );
			}
		}
		
		return true;
	}
}

class DelayedUpdateThread extends Thread {
	private final Logger LOGGER = Logger.getLogger( DelayedUpdateThread.class.getName() );
	public final int DELAY_LENGTH_MS = 3000; 
	public int counter = DELAY_LENGTH_MS;
	
	Vector <Path> updateItems = new Vector <Path> ();
	Library library;
	
	public DelayedUpdateThread ( Library library ) {
		this.library = library;
		setDaemon( true );
		setName ( "Library Update Delayer" );
	}
	
	public void run() {
		while ( true ) {
			long startSleepTime = System.currentTimeMillis();
			try { 
				Thread.sleep ( 50 ); 
			} catch ( InterruptedException e ) {
				LOGGER.log ( Level.FINE, "Sleep interupted during wait period." );
			}

			long sleepTime = System.currentTimeMillis() - startSleepTime;
			
			if ( counter > 0 ) {
				counter -= sleepTime; 
			} else {
				Vector <Path> copyUpdateItems = new Vector<Path> ( updateItems );
				for ( Path location : copyUpdateItems ) {
					library.getLoader().queueUpdatePath ( location );
					updateItems.remove( location );
				}
			}
		}
	}
	
	public void addUpdateItem ( Path location ) {
		counter = DELAY_LENGTH_MS;
		if ( !updateItems.contains( location ) ) {
			updateItems.add ( location );
		}
	}
};
