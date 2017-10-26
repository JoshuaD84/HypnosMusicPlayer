package net.joshuad.hypnos;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.Logger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

	public class CurrentListTrack extends Track {
		private static final Logger LOGGER = Logger.getLogger( CurrentListTrack.class.getName() );
		
		private static final long serialVersionUID = 1L;
		
		private boolean isCurrentTrack = false; 
		private boolean lastCurrentListTrack = false;
		
		private ArrayList <Integer> queueIndex = new ArrayList <Integer> ();
		
		private transient StringProperty display = new SimpleStringProperty ( "" );
		
		private transient BooleanProperty fileIsMissing = new SimpleBooleanProperty ( false );
		
		private boolean needsUpdate = false;
		
		public CurrentListTrack ( Path source ) throws Exception {
			super ( source );
			if ( Utils.isAlbumDirectory( source.getParent() ) ) {
				this.albumDirectory = source.getParent().toFile();
			}
			
			updateDisplayString();
		}
	
		public CurrentListTrack ( Track source ) {
			super ( source );		
			this.albumDirectory = getPath().getParent().toFile();
			needsUpdate = true;
			updateDisplayString();
		}
		
		public boolean needsUpdate () {
			return needsUpdate;
		}
		
		public void setNeedsUpdate ( boolean needsUpdate ) {
			this.needsUpdate = needsUpdate;
		}
		
		public void update() throws Exception {
			refreshTagData();
			
			if ( Utils.isAlbumDirectory( getPath().getParent() ) ) {
				this.albumDirectory = getPath().getParent().toFile();
			} else {
				this.albumDirectory = null;
			}

			needsUpdate = false;
		}
		
		
		public void setIsCurrentTrack ( boolean isCurrentTrack ) {
			this.isCurrentTrack = isCurrentTrack;
			updateDisplayString();
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
		
		public ArrayList <Integer> getQueueIndices() {
			return queueIndex;
		}
		
		public void clearQueueIndex () {
			queueIndex.clear();
			updateDisplayString();
		}

		public void addQueueIndex ( Integer index ) {
			queueIndex.add ( index );
			updateDisplayString();
		}
		
		public void updateDisplayString() {
			if ( isCurrentTrack ) {
				if ( queueIndex.size() == 1 && queueIndex.get( 0 ) == 1  ) {
					display.set( "🔁" );
				} else if ( queueIndex.size() > 1 && queueIndex.get( 0 ) == 1  ) {
					display.set( "🔁+" );
				} else if ( queueIndex.size() > 0 ) {
					display.set( "▶+" );
				} else {
					display.set( "▶" );
				}

			} else if ( queueIndex.size() == 1 ) {
				display.set( queueIndex.get( 0 ).toString() );
			} else if ( queueIndex.size() >= 1 ) {
				display.set( queueIndex.get( 0 ).toString() + "+" );
			} else {
				display.set( "" );
			}
		}
		
		public void setDisplay( String newDisplay ) {
			display.set( newDisplay );
		}
		
		public String getDisplay () {
			return display.get();
		}
		
		public StringProperty displayProperty () {
			return display;
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
		
		private void readObject ( ObjectInputStream in ) throws IOException, ClassNotFoundException {
			in.defaultReadObject();
			display = new SimpleStringProperty ( "" );
			fileIsMissing = new SimpleBooleanProperty ( false );
			queueIndex = new ArrayList <Integer> ();
			updateDisplayString();
		}
	}
