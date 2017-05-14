package org.joshuad.musicplayer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Persister {

	@SuppressWarnings("unchecked")
	public static void loadData() {

		long startTime = System.currentTimeMillis();
		try (
				ObjectInputStream sourcesIn = new ObjectInputStream( new FileInputStream( "info.sources" ) );
		) {
			ArrayList<String> searchPaths = (ArrayList<String>) sourcesIn.readObject();
			for ( String pathString : searchPaths ) {
				MusicPlayerUI.musicSourcePaths.add( Paths.get( pathString ) );
			}
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: info.sources, unable to load library source location list, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			e.printStackTrace(); //TODO: 
		}
		
		try (
				ObjectInputStream currentListIn = new ObjectInputStream( new FileInputStream( "info.current" ) );
		) {
			MusicPlayerUI.currentListData.addAll( (ArrayList<CurrentListTrack>) currentListIn.readObject() );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: info.current, unable to load current playlist, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
		
		try (
				ObjectInputStream playlistsIn = new ObjectInputStream( new FileInputStream( "info.playlists" ) );
		) {
			MusicPlayerUI.playlists.addAll( (ArrayList<Playlist>) playlistsIn.readObject() );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: info.playlists, unable to load custom playlist data, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
		
		readDataCompressed();
		System.out.println ( "Read time: " + (System.currentTimeMillis() - startTime ) );
	}

	static void saveData() {
		try ( 
				ObjectOutputStream sourcesOut = new ObjectOutputStream ( new FileOutputStream ( "info.sources" ) );
		) {
			ArrayList <String> searchPaths = new ArrayList <String> ( MusicPlayerUI.musicSourcePaths.size() );
			for ( Path path : MusicPlayerUI.musicSourcePaths ) {
				searchPaths.add( path.toString() );
			}
			sourcesOut.writeObject( searchPaths );
			sourcesOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try ( 
				ObjectOutputStream currentListOut = new ObjectOutputStream ( new FileOutputStream ( "info.current" ) );
		) {
			currentListOut.writeObject( new ArrayList <CurrentListTrack> ( Arrays.asList( MusicPlayerUI.currentListData.toArray( new CurrentListTrack[ MusicPlayerUI.currentListData.size() ] ) ) ) );
			currentListOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		try ( 
				ObjectOutputStream playlistsOut = new ObjectOutputStream ( new FileOutputStream ( "info.playlists" ) );
		) {
			playlistsOut.writeObject( new ArrayList <Playlist> ( Arrays.asList( MusicPlayerUI.playlists.toArray( new Playlist[ MusicPlayerUI.playlists.size() ] ) ) ) );
			playlistsOut.flush();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		long startTime = System.currentTimeMillis();
		writeDataCompressed();
		System.out.println ( "Write Compressed: " + (System.currentTimeMillis() - startTime ) );
	}
	
	@SuppressWarnings("unchecked")
	private static void readDataCompressed() {
		try (
				ObjectInputStream dataIn = new ObjectInputStream( new GZIPInputStream ( new FileInputStream( "info.data" ) ) );
		) {
			MusicPlayerUI.albums.addAll( (ArrayList<Album>) dataIn.readObject() );
			MusicPlayerUI.tracks.addAll( (ArrayList<Track>) dataIn.readObject() );
		} catch ( FileNotFoundException e ) {
			System.out.println ( "File not found: info.data, unable to load albuma and song lists, continuing." );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
	}
	
	private static void writeDataCompressed() {
		/* Some notes for future Josh (2017/05/14):
		 * 1. For some reason, keeping the ByteArrayOutputStream in the middle makes things take ~2/3 the amount of time.
		 * 2. I tried removing tracks that have albums (since they're being written twice) but it didn't create any savings. I guess compression is handling that
		 * 3. I didn't trip regular zip. GZIP was easier.
		 */
		try ( 
				GZIPOutputStream compressedOut = new GZIPOutputStream ( new BufferedOutputStream ( new FileOutputStream ( "info.data" ) ) );
		) {
			
			ByteArrayOutputStream byteWriter = new ByteArrayOutputStream();
			ObjectOutputStream bytesOut = new ObjectOutputStream ( byteWriter );
			
			bytesOut.writeObject( new ArrayList <Album> ( Arrays.asList( MusicPlayerUI.albums.toArray( new Album[ MusicPlayerUI.albums.size() ] ) ) ) );
			bytesOut.writeObject( new ArrayList <Track> ( Arrays.asList( MusicPlayerUI.tracks.toArray( new Track[ MusicPlayerUI.tracks.size() ] ) ) ) );

			compressedOut.write( byteWriter.toByteArray() );
			compressedOut.flush();
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
