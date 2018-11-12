package net.joshuad.hypnos.workbench.changescanner;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;


public class ChangeScanner extends SimpleFileVisitor <Path> {
	private static final Logger LOGGER = Logger.getLogger( ChangeScanner.class.getName() );

	List<PathAndTime> paths = new ArrayList<> ();
	List<PathAndTime> updateMe = new ArrayList<> ();
	
	@Override
	public FileVisitResult visitFile( Path file, BasicFileAttributes attr ) {
		
		PathAndTime thisOne = new PathAndTime ( file, attr.lastModifiedTime() );
		
		int indexOfSamePath = paths.indexOf( thisOne );
		
		if ( indexOfSamePath == -1 ) {
			paths.add ( thisOne );		
			
		} else {
			if ( paths.get( indexOfSamePath ).getTimeMS() != thisOne.getTimeMS() ) {
				updateMe.add( thisOne );
			}
		}
		
		return FileVisitResult.CONTINUE;
	}
	
	@Override
	public FileVisitResult postVisitDirectory( Path dir, IOException exc ) {
		return FileVisitResult.CONTINUE;
	}
	
	@Override	
	public FileVisitResult visitFileFailed( Path file, IOException exc ) {
		return FileVisitResult.CONTINUE;
	}
	
	public static void main( String[] args ) {

		ChangeScanner scanner = new ChangeScanner();
		
		/*File pathPersistFile = new File ( "paths.persist.test" );
		try ( ObjectInputStream itemsIn = new ObjectInputStream( new FileInputStream( pathPersistFile ) ) ) {
			scanner.paths.addAll( (ArrayList <PathAndTime>) itemsIn.readObject() );
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to read path data from disk, continuing.", e );
		}
		*/
		
		
		long startTime = System.currentTimeMillis();
		
		System.out.println ( "starting initial scan" );
		
		try {
			Files.walkFileTree ( 
				Paths.get( "/d/music" ), 
				EnumSet.of( FileVisitOption.FOLLOW_LINKS ), 
				Integer.MAX_VALUE,
				scanner
			);
		} catch ( IOException e ) {
			e.printStackTrace();
		}

		System.out.println ( "Time to generate content: " + ( System.currentTimeMillis() - startTime ) );
		System.out.println ( "Files indexed: " + scanner.paths.size() );
		
		
		startTime = System.currentTimeMillis();
		
		System.out.println ( "starting update scan" );
		
		try {
			Files.walkFileTree ( 
				Paths.get( "/d/music" ), 
				EnumSet.of( FileVisitOption.FOLLOW_LINKS ), 
				Integer.MAX_VALUE,
				scanner
			);
		} catch ( IOException e ) {
			e.printStackTrace();
		}

		System.out.println ( "Time to check for updates: " + ( System.currentTimeMillis() - startTime ) );
		System.out.println ( "Files indexed: " + scanner.paths.size() );
		
		/*
		try ( ObjectOutputStream itemsOut = new ObjectOutputStream( new FileOutputStream( pathPersistFile ) ) ) {
			itemsOut.writeObject( scanner.paths );
			itemsOut.flush();
			itemsOut.close();
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to save path data to disk, continuing.", e );
		}	
		*/	
	}
}


