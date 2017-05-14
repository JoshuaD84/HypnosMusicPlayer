package org.joshuad.musicplayer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class Persister {

	@SuppressWarnings("unchecked")
	public static void loadData() {
		try (
				ObjectInputStream sourcesIn = new ObjectInputStream( new FileInputStream( "info.sources" ) );
		) {
			ArrayList<String> searchPaths = (ArrayList<String>) sourcesIn.readObject();
			for ( String pathString : searchPaths ) {
				MusicPlayerUI.musicSourcePaths.add( Paths.get( pathString ) );
			}
		} catch ( IOException | ClassNotFoundException e ) {
			e.printStackTrace(); //TODO: 
		}
		
		try (
				ObjectInputStream currentListIn = new ObjectInputStream( new FileInputStream( "info.current" ) );
		) {
			MusicPlayerUI.currentListData.addAll( (ArrayList<CurrentListTrack>) currentListIn.readObject() );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
		
		try (
				ObjectInputStream playlistsIn = new ObjectInputStream( new FileInputStream( "info.playlists" ) );
		) {
			MusicPlayerUI.playlists.addAll( (ArrayList<Playlist>) playlistsIn.readObject() );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
		
		try (
				ObjectInputStream dataIn = new ObjectInputStream( new FileInputStream( "info.data" ) );
		) {
			MusicPlayerUI.albums.addAll( (ArrayList<Album>) dataIn.readObject() );
			MusicPlayerUI.tracks.addAll( (ArrayList<Track>) dataIn.readObject() );
		} catch ( IOException | ClassNotFoundException e ) {
			//TODO: 
			e.printStackTrace();
		}
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
		
		try ( 
				ObjectOutputStream dataOut = new ObjectOutputStream ( new FileOutputStream ( "info.data" ) );
		) {
			dataOut.writeObject( new ArrayList <Album> ( Arrays.asList( MusicPlayerUI.albums.toArray( new Album[ MusicPlayerUI.albums.size() ] ) ) ) );
			dataOut.writeObject( new ArrayList <Track> ( Arrays.asList( MusicPlayerUI.tracks.toArray( new Track[ MusicPlayerUI.tracks.size() ] ) ) ) );
			dataOut.flush();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
