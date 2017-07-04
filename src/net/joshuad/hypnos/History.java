package net.joshuad.hypnos;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class History {

	private static final int MAX_HISTORY_SIZE = 100;

	private final ObservableList <Track> history = FXCollections.observableArrayList( new ArrayList <Track>(MAX_HISTORY_SIZE) );
	
	public History() {}
	
	public void trackPlayed ( Track track ) {
		if ( history.size() == 0 || !history.get( 0 ).equals( track ) ) {
			while ( history.size() >= MAX_HISTORY_SIZE ) {
				history.remove( history.size() - 1 );
			}
			
			history.add( 0, track );
		}
	}
	
	public ObservableList <Track> getItems() {
		return history;
	}

	public void setData ( List <Track> history ) {
		history.clear();
		history.addAll( history );
	}
}
