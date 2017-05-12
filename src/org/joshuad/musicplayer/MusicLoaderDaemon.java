package org.joshuad.musicplayer;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

import javafx.collections.ListChangeListener;

public class MusicLoaderDaemon implements ListChangeListener <Path> {
	
	Vector <Path> loadMe = new Vector <Path> ();
	Vector <Path> removeMe = new Vector <Path> ();
	
	Thread loaderThread;
	private WatchService watcher;
    private final HashMap<WatchKey,Path> keys;
	
	public MusicLoaderDaemon() {

		keys = new HashMap <WatchKey,Path> ();
		
		try {
			watcher = FileSystems.getDefault().newWatchService();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	
	public void start() {
		loaderThread = new Thread ( new Runnable() {
			public void run() {
				MusicPlayerUI.loadData();
				while ( true ) {
					
					if ( !loadMe.isEmpty() ) {
						Path selectedPath = loadMe.remove( 0 );
						
						Logger.getLogger( "org.jaudiotagger" ).setLevel( Level.OFF );
						long startTime = System.currentTimeMillis();
						
						try {
							Files.walkFileTree ( 
								selectedPath, 
								EnumSet.of(FileVisitOption.FOLLOW_LINKS), 
								Integer.MAX_VALUE,
								new MusicFileVisitor( MusicPlayerUI.albums, MusicPlayerUI.tracks ) 
							);
							
						} catch ( IOException e ) {
							System.out.println ("Unable to load files in path: " + selectedPath.toString() );
							e.printStackTrace();
						}
						
						long endTime = System.currentTimeMillis();
	
						System.out.println( "Time to load all tracks (path: " + selectedPath.toString() + "): " + (endTime - startTime) );
						
						watcherRegisterAll ( selectedPath );
						
					} else if ( !removeMe.isEmpty() ) {
						Path sourcePath = removeMe.remove( 0 ).toAbsolutePath();
						
						if ( Files.isDirectory( sourcePath ) ) {
							ArrayList <Album> albumsCopy = new ArrayList <Album> ( MusicPlayerUI.albums );
							for ( Album album : albumsCopy ) {
								if ( album.getPath().toAbsolutePath().startsWith( sourcePath ) ) {
									MusicPlayerUI.albums.remove ( album );
									ArrayList <Track> tracks = album.getTracks();
									if ( tracks != null ) {
										MusicPlayerUI.tracks.removeAll( tracks );
									}
								}
							}
						} else {
							ArrayList <Track> tracksCopy = new ArrayList <Track> ( MusicPlayerUI.tracks );
							for ( Track track : tracksCopy ) {
								if ( track.getPath().toAbsolutePath().equals( sourcePath ) ) {
									MusicPlayerUI.tracks.remove ( track );
								}
							}
						}
					}
					//TODO: this structure sucks
					processWatcherEvents();
				}
			}
		});
		
		loaderThread.setDaemon( true );
		loaderThread.start();
	}
	
	private void watcherRegisterAll ( final Path start ) {
		try {
			Files.walkFileTree( start, new SimpleFileVisitor <Path>() {
				@Override
				public FileVisitResult preVisitDirectory ( Path dir, BasicFileAttributes attrs ) throws IOException {
					watcherRegister( dir );
					return FileVisitResult.CONTINUE;
				}
			});
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void watcherRegister ( Path dir ) throws IOException {
		WatchKey key = dir.register( watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY );
		keys.put( key, dir );
	}
	
	@SuppressWarnings("rawtypes")
	void processWatcherEvents () {
		WatchKey key;
		try {
			key = watcher.poll( 250, TimeUnit.MILLISECONDS );
		} catch ( InterruptedException x ) {
			return;
		}

		Path dir = keys.get( key );
		if ( dir == null ) {
			return;
		}

		for ( WatchEvent <?> event : key.pollEvents() ) {
			WatchEvent.Kind kind = event.kind();

			if ( kind == OVERFLOW ) {
				// TODO: What's this?
				continue;
			}

			WatchEvent <Path> ev = cast( event );
			Path name = ev.context();
			Path child = dir.resolve( name );

			System.out.format( "%s: %s\n", event.kind().name(), child );

			if ( kind == ENTRY_CREATE ) {
				if ( Files.isDirectory( child ) ) {
					System.out.println ( "is directory" );
					watcherRegisterAll( child );
					loadMe.add( child );
				}
			} else if ( kind == ENTRY_DELETE ) {
				removeMe.add( child );
			}

			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if ( !valid ) {
				keys.remove( key );
			}
		}
	}
		
	@SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }
	


}
