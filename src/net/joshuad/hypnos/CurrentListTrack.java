package net.joshuad.hypnos;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public class CurrentListTrack extends Track {
	
	private static final Logger LOGGER = Logger.getLogger( CurrentListTrack.class.getName() );
	
	private static final long serialVersionUID = 1L;
	
	private boolean isCurrentTrack = false; 
	private boolean lastCurrentListTrack = false;
	
	private List <Integer> queueIndex = new ArrayList <Integer> ();
	
	private transient BooleanProperty fileIsMissing = new SimpleBooleanProperty ( false );
	
	private transient ObjectProperty <CurrentListTrackState> displayState = new SimpleObjectProperty <CurrentListTrackState> ( new CurrentListTrackState ( isCurrentTrack, queueIndex ) );
	
	private boolean needsUpdateFromDisk = false;
	
	public CurrentListTrack ( Path source ) {
		super ( source );
		if ( Utils.isAlbumDirectory( source.getParent() ) ) {
			this.albumDirectory = source.getParent().toFile();
		}
	}
	
	public CurrentListTrack ( Track source ) {
		super ( source );		
		this.albumDirectory = getPath().getParent().toFile();
		needsUpdateFromDisk = true;
	}
	
	public boolean needsUpdateFromDisk () {
		return needsUpdateFromDisk;
	}
	
	public void setNeedsUpdateFromDisk ( boolean needsUpdate ) {
		this.needsUpdateFromDisk = needsUpdate;
	}
	
	public void update() {
		refreshTagData();
		
		if ( Utils.isAlbumDirectory( getPath().getParent() ) ) {
			this.albumDirectory = getPath().getParent().toFile();
		} else {
			this.albumDirectory = null;
		}

		needsUpdateFromDisk = false;
	}
	
	public void setIsCurrentTrack ( boolean isCurrentTrack ) {
		this.isCurrentTrack = isCurrentTrack;
		updateDisplayState ();
	}
	
	public boolean getIsCurrentTrack ( ) {
		return isCurrentTrack;
	}
	
	public void setIsLastCurrentListTrack ( boolean last ) {
		this.lastCurrentListTrack = last;
	}
	
	public boolean isLastCurrentListTrack ()  {
		return lastCurrentListTrack;
	}
	
	public List <Integer> getQueueIndices() {
		return queueIndex;
	}
	
	public void clearQueueIndex () {
		queueIndex.clear();
		updateDisplayState ();
	}

	public void addQueueIndex ( Integer index ) {
		queueIndex.add ( index );
		updateDisplayState ();
	}

	public BooleanProperty fileIsMissingProperty() {
		return fileIsMissing;
	}
	
	public boolean isMissingFile () {
		return fileIsMissing.getValue();
	}
	
	public void setIsMissingFile ( boolean missing ) {
		fileIsMissing.set( missing );
		if ( missing ) {
			setIsCurrentTrack( false );
		}
	}
	
	private void updateDisplayState () {
		displayState.setValue( new CurrentListTrackState ( isCurrentTrack, queueIndex ) );
	}
	
	public ObjectProperty<CurrentListTrackState> displayStateProperty () {
		return displayState;
	}
	
	private void readObject ( ObjectInputStream in ) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		fileIsMissing = new SimpleBooleanProperty ( false );
		queueIndex = new ArrayList <Integer> ();
		displayState = new SimpleObjectProperty <CurrentListTrackState> ( new CurrentListTrackState ( isCurrentTrack, queueIndex ) );
	}
}
	
