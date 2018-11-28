package net.joshuad.hypnos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Artist implements Serializable {
	private static final long serialVersionUID = 1L;

	private ObservableList <Album> albums = FXCollections.observableArrayList();
	private ObservableList <Track> looseTracks = FXCollections.observableArrayList();
	String name;
	
	private IntegerProperty trackCount = new SimpleIntegerProperty(); 
	{	
		//This is a hack because Track and Album don't use Observable Values
		albums.addListener( ( Observable obs ) -> trackCount.set( getAllTracks().size() ) );
		looseTracks.addListener( ( Observable obs ) -> trackCount.set( getAllTracks().size() ) );
	};
	
	private IntegerProperty albumCount = new SimpleIntegerProperty(); 
	{
		albumCount.bind( Bindings.size( albums ) );
	}
	
	private IntegerProperty totalLength = new SimpleIntegerProperty ( 0 );
	{	
		//This is a hack because Track and Album don't use Observable Values
		InvalidationListener listener = ( Observable obs ) -> {
			int lengthS = 0;
			for ( Track track : getAllTracks() ) {
				lengthS += track.getLengthS();
			}
			totalLength.setValue( lengthS );
		};
		
		albums.addListener( listener );
		looseTracks.addListener( listener );
	};
	
	public Artist ( String name ) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public IntegerProperty lengthProperty() {
		return totalLength;
	}
	
	public IntegerProperty albumCountProperty() {
		return albumCount;
	}
		
	public IntegerProperty trackCountProperty() {
		return trackCount;
	}
	
	public int getTrackCount() {
		return trackCount.get();
	}
	
	public List<Track> getAllTracks() {
		List<Track> retMe = new ArrayList<>();
		for ( Album album : albums ) {
			retMe.addAll ( album.getTracks() );
		}
		retMe.addAll( looseTracks );
		return retMe;
	}
	
	public boolean equals ( Object object ) {
		if ( ! ( object instanceof Artist ) ) {
			return false;
		}
		
		return ( ((Artist)object).getName().equals ( getName() ) );
	}

	public void addAlbum ( Album album ) {
		if ( albums.contains( album ) ) {
			albums.remove( album );
		}
		
		//TODO: error checking maybe? 
		albums.add ( album );
	}

	public void addLooseTrack ( Track track ) {
		looseTracks.add ( track );
	}

	public void removeAlbum ( Album removeMe ) {
		albums.remove( removeMe );
	}

	public void removeTrack ( Track removeMe ) {
		looseTracks.remove ( removeMe );
	}

	public List <Album> getAlbums () {
		return new ArrayList<Album> ( albums );
	}
}
