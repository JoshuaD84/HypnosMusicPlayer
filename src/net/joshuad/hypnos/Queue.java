package net.joshuad.hypnos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import net.joshuad.hypnos.library.Album;
import net.joshuad.hypnos.library.Artist;
import net.joshuad.hypnos.library.Playlist;
import net.joshuad.hypnos.library.Track;

public class Queue {
	private static final Logger LOGGER = Logger.getLogger( Queue.class.getName() );

	final private ObservableList <Track> queue = FXCollections.observableArrayList ( new ArrayList <Track>() );
	
	transient private boolean hasUnsavedData = true;
	
	public Queue() {
		queue.addListener( (ListChangeListener.Change<? extends Track> change) -> {
			hasUnsavedData = true;			
		});
	}
	
	public boolean hasUnsavedData() {
		return hasUnsavedData;
	}
	
	public void setHasUnsavedData( boolean b ) {
		hasUnsavedData = b;
	}
	
	public synchronized void queueTrack ( Track track ) {
		queueTrack ( queue.size(), track );
	}
				
	public synchronized void queueTrack ( int index, Track track ) {
		if ( index < 0 ) {
			LOGGER.fine ( "Asked to add a track at index: " + index + ", adding at 0 instead." );
			index = 0;
		}
		
		if ( index > queue.size() ) { 
			LOGGER.fine ( "Asked to add a track at index: " + index + ", which is beyond the end of the queue. Adding at the end instead." );
			index = queue.size();
		}
			
		queue.add( index, track );
		updateQueueIndexes();
	}
	
	public synchronized void queueAllAlbums ( List<? extends Album> albums ) {
		for ( Album album : albums ) {
			queueAllTracks( album.getTracks() );
		}
	}
	
	public synchronized void queueAllAlbums ( List<? extends Album> albums, int index ) {
		List <Track> tracks = new ArrayList <Track> ();
		for ( Album album : albums ) {
			tracks.addAll ( album.getTracks() );
		}
		queueAllTracks( tracks, index );
	}

	public synchronized void queueAllPlaylists ( List<? extends Playlist> playlists ) {
		for ( Playlist playlist : playlists ) {
			queueAllTracks ( playlist.getTracks() );
		}
	}
	
	public synchronized void queueAllPlaylists ( List<? extends Playlist> playlists, int index ) {
		List <Track> tracks = new ArrayList <Track> ();
		for ( Playlist playlist : playlists ) {
			tracks.addAll ( playlist.getTracks() );
		}
		queueAllTracks( tracks, index );
	}
	
	public synchronized void queueAllTracks ( List<? extends Track> tracks ) {
		for ( Track track : tracks ) {
			queueTrack ( track );
		}
	}
	
	public synchronized void queueAllTracks ( List<? extends Track> tracks, int index ) {
		if ( index < 0 ) {
			LOGGER.fine ( "Asked to add a tracks at index: " + index + ", adding at 0 instead." );
			index = 0;
		}
		
		if ( index > queue.size() ) { 
			LOGGER.fine ( "Asked to add a tracks at index: " + index + ", which is beyond the end of the queue. Adding at the end instead." );
			index = queue.size();
		}
			
		for ( int k = 0; k < tracks.size(); k++ ) {
			queueTrack ( index + k, tracks.get ( k ) );
		}
	}
	
	public synchronized void updateQueueIndexes () {
		updateQueueIndexes ( new ArrayList<Track> () );
	}

	public synchronized void updateQueueIndexes( Track removedTrack ) {
		updateQueueIndexes ( Arrays.asList( removedTrack ) );
	}
	
	public synchronized void updateQueueIndexes( List<Track> removedTracks ) {
		for ( Track removedTrack : removedTracks ) {
			if ( removedTrack != null && removedTrack instanceof CurrentListTrack ) {
				((CurrentListTrack)removedTrack).clearQueueIndex();
			}
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
	
	public synchronized Track get ( int index ) {
		return queue.get( index );
	}
	
	public synchronized int size () {
		return queue.size();
	}
	
	public synchronized void remove ( int index ) {
		if ( index >= 0 && index < queue.size() ) {
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
			return null;
		}
		Track nextTrack = queue.remove ( 0 );
		
		updateQueueIndexes( nextTrack );
		
		return nextTrack;
	}
	
	public synchronized ObservableList<Track> getData() {
		return queue;
	}
	
	public void clear() {
		for ( Track removedTrack : queue ) {
			if ( removedTrack != null && removedTrack instanceof CurrentListTrack ) {
				((CurrentListTrack)removedTrack).clearQueueIndex();
			}
		}
		
		queue.clear();
	}

	public void queueAllArtists ( ObservableList <Artist> artists, int index ) {
		List<Track> tracks = new ArrayList<> ();
		for ( Artist artist : artists ) {
			tracks.addAll( artist.getAllTracks() );
		}
		queueAllTracks ( tracks, index );
	}

	public void queueAllArtists ( List <Artist> artists ) {
		List<Track> tracks = new ArrayList<> ();
		for ( Artist artist : artists ) {
			tracks.addAll( artist.getAllTracks() );
		}
		queueAllTracks ( tracks );
	}
}
