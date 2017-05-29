package org.joshuad.musicplayer;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Queue {

	final private static ObservableList <Track> queue = FXCollections.observableArrayList ( new ArrayList <Track>() );
	
	public synchronized static void addTrack ( Track track ) {
		queue.add( track );
		
		if ( track instanceof CurrentListTrack ) {
			((CurrentListTrack)track).addQueueIndex( queue.size() );
		}
	}
	
	public synchronized static void addAllAlbums ( List<? extends Album> albums ) {
		for ( Album album : albums ) {
			for ( Track track : album.getTracks() ) {
				addTrack ( track );
			}
		}
	}

	public synchronized static void addAllPlaylists ( List<? extends Playlist> playlists ) {
		for ( Playlist playlist : playlists ) {
			for ( Track track : playlist.getTracks() ) {
				addTrack ( track );
			}
		}
	}

	
	public synchronized static void addAllTracks ( List<? extends Track> tracks ) {
		
		for ( Track track : tracks ) {
			addTrack ( track );
		}
	}
	
	public synchronized static void updateQueueIndexes( Track removedTrack ) {
		if ( removedTrack != null && removedTrack instanceof CurrentListTrack ) {
			((CurrentListTrack)removedTrack).clearQueueIndex();
		}
		
		for ( int k = 0; k < queue.size(); k++ ) {
			if ( queue.get( k ) instanceof CurrentListTrack ) {
				CurrentListTrack track = (CurrentListTrack)queue.get( k );
				track.clearQueueIndex();
			}
		}
		
		for ( int k = 0; k < queue.size(); k++ ) {
			if ( queue.get( k ) instanceof CurrentListTrack ) {
				CurrentListTrack track = (CurrentListTrack)queue.get( k );
				track.addQueueIndex( k + 1 );
			} 
		}
	}
	
	public synchronized static void remove ( int index ) {
		if ( queue.size() > index ) {
			Track removedTrack = queue.remove( index );
			updateQueueIndexes( removedTrack );
		}
	}
	
	public synchronized static boolean hasNext() {
		return ( !queue.isEmpty() );
	}
	
	public synchronized static Track getNextTrack ( ) {
		if ( queue.isEmpty() ) {
			//TODO: throw new QueueException();
			return null;
		}
		Track nextTrack = queue.remove ( 0 );
		
		updateQueueIndexes( nextTrack );
		
		return nextTrack;
	}
	
	//You shouldn't use this except when setting up tables. 
	public synchronized static ObservableList<Track> getData() {
		return queue;
	}

}


