package net.joshuad.hypnos;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Queue {

	final private ObservableList <Track> queue = FXCollections.observableArrayList ( new ArrayList <Track>() );
	
	public Queue() {}
	
	public synchronized void addTrack ( Track track ) {
		addTrack ( queue.size(), track );
	}
				
				
	public synchronized void addTrack ( int index, Track track ) {
		queue.add( index, track );
		
		if ( track instanceof CurrentListTrack ) {
			((CurrentListTrack)track).addQueueIndex( queue.size() );
		}
	}
	
	public synchronized void addAllAlbums ( List<? extends Album> albums ) {
		for ( Album album : albums ) {
			for ( Track track : album.getTracks() ) {
				addTrack ( track );
			}
		}
	}

	public synchronized void addAllPlaylists ( List<? extends Playlist> playlists ) {
		for ( Playlist playlist : playlists ) {
			for ( Track track : playlist.getTracks() ) {
				addTrack ( track );
			}
		}
	}
	
	public synchronized void addAllTracks ( List<? extends Track> tracks ) {
		for ( Track track : tracks ) {
			addTrack ( track );
		}
	}
	
	public synchronized void addAllTracks ( int index, List<? extends Track> tracks ) {
		for ( Track track : tracks ) {
			addTrack ( track );
		}
	}
	
	public synchronized void updateQueueIndexes( Track removedTrack ) {
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
	
	public synchronized int size () {
		return queue.size();
	}
	
	public synchronized void remove ( int index ) {
		if ( queue.size() > index ) {
			Track removedTrack = queue.remove( index );
			updateQueueIndexes( removedTrack );
		}
	}
	
	public synchronized boolean hasNext() {
		return ( !queue.isEmpty() );
	}
	
	public synchronized boolean isEmpty() {
		return queue.isEmpty();
	}
	
	public synchronized Track getNextTrack ( ) {
		if ( queue.isEmpty() ) {
			//TODO: throw new QueueException();
			return null;
		}
		Track nextTrack = queue.remove ( 0 );
		
		updateQueueIndexes( nextTrack );
		
		return nextTrack;
	}
	
	public synchronized ObservableList<Track> getData() {
		return FXCollections.unmodifiableObservableList( queue );
	}

}


