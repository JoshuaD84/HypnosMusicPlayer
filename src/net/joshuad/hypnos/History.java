package net.joshuad.hypnos;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class History {

	private static final int MAX_HISTORY_SIZE = 1000;

	private final ObservableList <Track> history = FXCollections.observableArrayList( new ArrayList <Track>(MAX_HISTORY_SIZE) );
	
	private transient boolean hasUnsavedData = false;
	
	public History() {
		history.addListener( (ListChangeListener.Change<? extends Track> change) -> {
			hasUnsavedData = true;			
		});
	}
	
	public boolean hasUnsavedData() {
		return hasUnsavedData;
	}
	
	public void setHasUnsavedData( boolean b ) {
		hasUnsavedData = b;
	}
	
	public void trackPlayed ( Track track ) {
		
		if ( track == null ) return;
		
		if ( history.size() == 0 || !track.equals( history.get( 0 ) ) ) {
			while ( history.size() >= MAX_HISTORY_SIZE ) {
				history.remove( history.size() - 1 );
			}
			
			history.add( 0, track );
		}
	}
	
	public ObservableList <Track> getItems() {
		return history;
	}

	public void setData ( List <Track> data ) {
		history.clear();
		history.addAll( data );
	}

	public Track getLastTrack () {
		if ( history.size() >= 0 ) {
			return history.get( 0 );
		} else {
			return null;
		}
	}
}
