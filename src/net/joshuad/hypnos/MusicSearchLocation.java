package net.joshuad.hypnos;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class MusicSearchLocation implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private File file;
	private transient boolean hadInotifyError;

	private transient BooleanProperty isValidSearchLocation = new SimpleBooleanProperty ( true );
	
	public MusicSearchLocation ( Path path ) {
		this.file = path.toFile();
	}
	
	public Path getPath() {
		return file.toPath();
	}
	
	public void setHadInotifyError( boolean hadError ) {
		this.hadInotifyError = hadError;
	}

	@Override
	public boolean equals ( Object other ) {
		if ( !( other instanceof MusicSearchLocation ) ) {
			return false;
		}
		
		MusicSearchLocation otherLocation = (MusicSearchLocation)other;
			
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
}