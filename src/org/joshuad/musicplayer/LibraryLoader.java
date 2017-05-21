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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;

//TODO: look for, and remove, oprhaned albums/tracks, which can happen when the program is closed abruptly. 

public class LibraryLoader {
	
	public static final boolean SHOW_SCAN_NOTES = false;
	
	static Vector <Path> loadMe = new Vector <Path> ();
	static Vector <Path> updateMe = new Vector <Path> ();
	static Vector <Path> removeMe = new Vector <Path> ();
	
	static Thread loaderThread;
	private static WatchService watcher;
    private static final HashMap<WatchKey,Path> keys = new HashMap <WatchKey,Path> ();
    
    private static MusicFileVisitor updateWalker = null;
	
	public static void init() {
		if ( watcher == null ) {
			try {
				watcher = FileSystems.getDefault().newWatchService();
			} catch ( IOException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void updatePaths ( List<Path> paths ) {
		updateMe.addAll( paths );
		for ( Path path : paths ) {
			MusicPlayerUI.musicSourcePaths.add( path );
		}
	}
	
	public static void addPaths ( List<Path> paths ) {
		for ( Path path : paths ) {
			if ( path != null ) {
				path = path.toAbsolutePath();
				
				if ( path.toFile().exists() && path.toFile().isDirectory() ) {

					boolean addSelectedPathToList = true;
					for ( Path alreadyAddedPath : MusicPlayerUI.musicSourcePaths ) {
						try {
							if ( Files.isSameFile( path, alreadyAddedPath ) ) {
								addSelectedPathToList = false;
							}
						} catch ( IOException e1 ) {} // Do nothing, assume they don't match.
					}
					
					if ( addSelectedPathToList ) {
						loadMe.add ( path );
						MusicPlayerUI.musicSourcePaths.add( path );
						if ( updateWalker != null ) {
							updateWalker.interrupt();
						}
					}
				}
			}
		}
	}	
	
	public static void removePaths ( List<Path> paths ) {
		removeMe.addAll ( paths );
		for ( Path path : paths ) {
			MusicPlayerUI.musicSourcePaths.remove( path );
			updateMe.remove( path );
			loadMe.remove( path );
		}
		if ( updateWalker != null ) {
			updateWalker.interrupt();
		}
	}
	
	public static void updatePath ( Path path ) {
		updatePaths( Arrays.asList( path ) );
	}

	public static void addPath ( Path path ) {
		addPaths( Arrays.asList( path ) );
	}
	
	public static void removePath ( Path path ) {
		removePaths ( Arrays.asList( path ) );
	}
	
	public static void start() {
		loaderThread = new Thread ( new Runnable() {
			public void run() {
				while ( true ) {
					if ( !removeMe.isEmpty() ) {
						Path sourcePath = removeMe.remove( 0 ).toAbsolutePath();
						
						System.out.println ("Delete Started: " + sourcePath.toString() ) ; //TODO: DD
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
							
							ArrayList <Track> tracksCopy = new ArrayList <Track> ( MusicPlayerUI.tracks );
							for ( Track track : tracksCopy ) {
								if ( track.getPath().toAbsolutePath().equals( sourcePath ) ) {
									MusicPlayerUI.tracks.remove ( track );
								}
							}
							System.out.println ("Delete Ended: " + sourcePath.toString() ) ; //TODO: DD
						}
					} else if ( !loadMe.isEmpty() ) {
						Path selectedPath = loadMe.remove( 0 );
						try {

							System.out.println ("Load started: " + selectedPath.toString() ) ; //TODO: DD
							Files.walkFileTree ( 
								selectedPath, 
								EnumSet.of( FileVisitOption.FOLLOW_LINKS ), 
								Integer.MAX_VALUE,
								new MusicFileVisitor( MusicPlayerUI.albums, MusicPlayerUI.tracks )
							);

							System.out.println ("Load finished: " + selectedPath.toString() ) ; //TODO: DD
							
						} catch ( IOException e ) {
							System.out.println ("Unable to load some files in path: " + selectedPath.toString() );
							e.printStackTrace();
						}
						
						watcherRegisterAll ( selectedPath );
						
					} else if ( !updateMe.isEmpty() ) {
						Path selectedPath = updateMe.get( 0 );
						updateWalker = new MusicFileVisitor( MusicPlayerUI.albums, MusicPlayerUI.tracks );
						try {
							System.out.println ( "Updat walker started: " + selectedPath.toString() ); //TODO: DD
							Files.walkFileTree ( 
								selectedPath, 
								EnumSet.of( FileVisitOption.FOLLOW_LINKS ), 
								Integer.MAX_VALUE,
								updateWalker
							);
							
							if ( !updateWalker.getWalkInterrupted() ) {
								updateMe.remove( selectedPath );
								System.out.println ( "Update walker finished: "+ selectedPath.toString() ); //TODO: DD
							} else {
								System.out.println ( "Update walker interrupted: "+ selectedPath.toString() ); //TODO: DD
							}
							
							
						} catch ( IOException e ) {
							System.out.println ("Unable to load some files in path: " + selectedPath.toString() );
							e.printStackTrace();
						}
						
						updateWalker = null;
						
						watcherRegisterAll ( selectedPath );
					} else {
						//TODO: this structure sucks
						processWatcherEvents();
					}
				}
			}
		});
		
		loaderThread.setDaemon( true );
		loaderThread.start();
	}
	
	private static void watcherRegisterAll ( final Path start ) {
		try {
			Files.walkFileTree( start, new SimpleFileVisitor <Path>() {
				@Override
				public FileVisitResult preVisitDirectory ( Path dir, BasicFileAttributes attrs ) throws IOException {
					WatchKey key = dir.register( watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY );
					keys.put( key, dir );
					return FileVisitResult.CONTINUE;
				}
			});
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static void processWatcherEvents () {
		WatchKey key;
		try {
			key = watcher.poll( 50, TimeUnit.MILLISECONDS );
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

			if ( kind == ENTRY_CREATE ) {
				if ( Files.isDirectory( child ) ) {
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
