package net.joshuad.hypnos.library;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class MusicRoot implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private File file;
	
	private boolean needsInitialScan = true;
	private transient boolean needsRescan = false;

	@SuppressWarnings("unused")
	private boolean failedScan = false; //TODO: Show this in the UI
	
	@SuppressWarnings("unused")
	private transient boolean hadInotifyError = false; //TODO: Show this in the UI

	private transient BooleanProperty isValidSearchLocation = new SimpleBooleanProperty ( true );
	
	public MusicRoot ( Path path ) {
		this.file = path.toFile();
	}
	
	public Path getPath() {
		return file.toPath();
	}
	
	public void setNeedsInitialScan( boolean needsInitialScan ) {
		this.needsInitialScan = needsInitialScan;
	}

	public boolean needsInitialScan() {
		return needsInitialScan;
	}
	
	public void setNeedsRescan( boolean needsRescan ) {
		this.needsRescan = needsRescan;
	}
	
	public boolean needsRescan() {
		return needsRescan;
	}
	
	public void setHadInotifyError( boolean hadError ) {
		this.hadInotifyError = hadError;
	}

	@Override
	public boolean equals ( Object other ) {
		if ( !( other instanceof MusicRoot ) ) {
			return false;
		}
		
		MusicRoot otherLocation = (MusicRoot)other;
			
		return file.toPath().equals( otherLocation.getPath() );
	}
	
	public BooleanProperty validSearchLocationProperty() {
		return isValidSearchLocation;
	}
	
	public void recheckValidity() {
		boolean newValue = true;
		if ( !Files.exists( getPath() ) || !Files.isDirectory( getPath() ) || !Files.isReadable( getPath() ) ) {
			newValue = false;
		}
		
		if ( isValidSearchLocation.get() != newValue ) {
			isValidSearchLocation.setValue( newValue );
		}
	}
	
	private void readObject ( ObjectInputStream in ) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		isValidSearchLocation = new SimpleBooleanProperty ( true );
	}

	public void setFailedScan(boolean failed) {
		this.failedScan = failed;
	}
}