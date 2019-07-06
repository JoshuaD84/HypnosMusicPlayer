package net.joshuad.hypnos.library;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

	//This values are only used and accurate during serialization. Don't read and write to them. 
	private List <Album> albumsForSerialization = new ArrayList<>();
	private List <Track> looseTracksForSerialization = new ArrayList<>();
	
	private transient ObservableList <Album> albums = FXCollections.observableArrayList();
	private transient ObservableList <Track> looseTracks = FXCollections.observableArrayList();
	String name;
	
	private transient IntegerProperty trackCount = new SimpleIntegerProperty(); 
	{	
		//This is a hack because Track and Album don't use Observable Values
		albums.addListener( ( Observable obs ) -> trackCount.set( getAllTracks().size() ) );
		looseTracks.addListener( ( Observable obs ) -> trackCount.set( getAllTracks().size() ) );
	};
	
	private transient IntegerProperty albumCount = new SimpleIntegerProperty(); 
	{
		albumCount.bind( Bindings.size( albums ) );
	}
	
	private transient IntegerProperty totalLength = new SimpleIntegerProperty ( 0 );
	{	
		//This is a hack because Track and Album don't use Observable Values
		InvalidationListener listener = ( Observable obs ) -> recalculateTotalLength();
		albums.addListener( listener );
		looseTracks.addListener( listener );
	};
	
	private void recalculateTotalLength () {
		int lengthS = 0;
		for ( Track track : getAllTracks() ) {
			lengthS += track.getLengthS();
		}
		totalLength.setValue( lengthS );
	}
	
	public Artist ( String name ) {
		this.name = name;
	}
	
	public Artist ( String name, List<Album> albums, List<Track> looseTracks ) {
		this.name = name;
		albums.addAll( albums );
		looseTracks.addAll( looseTracks );
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
	
	private void writeObject ( ObjectOutputStream out ) throws IOException {
		albumsForSerialization.clear();
		albumsForSerialization.addAll( albums );
		looseTracksForSerialization.clear();
		looseTracksForSerialization.addAll( looseTracks );
		out.defaultWriteObject();
	}
	
	private void readObject ( ObjectInputStream in ) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		
		trackCount = new SimpleIntegerProperty(); 
		albumCount = new SimpleIntegerProperty(); 
		totalLength = new SimpleIntegerProperty();

		albums = FXCollections.observableArrayList( albumsForSerialization );
		looseTracks = FXCollections.observableArrayList( looseTracksForSerialization );
		
		albumCount.set( albums.size() );
		trackCount.set( getAllTracks().size() );
		recalculateTotalLength();
	}
}