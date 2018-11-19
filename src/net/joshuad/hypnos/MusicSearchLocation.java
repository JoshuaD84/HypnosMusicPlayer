package net.joshuad.hypnos;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;

public class MusicSearchLocation implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private File file;
	private transient boolean hadInotifyError;
	
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
}
