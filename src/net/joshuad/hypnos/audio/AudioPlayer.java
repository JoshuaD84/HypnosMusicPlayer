package net.joshuad.hypnos.audio;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.audio.decoders.*;

public class AudioPlayer {

	private static final Logger LOGGER = Logger.getLogger( AudioPlayer.class.getName() );

	public static final int NO_REQUEST = -1;
	
	boolean unpauseRequested = false;
	boolean pauseRequested = false;
	boolean stopRequested = false;
	Track trackRequested = null;
	double seekPercentRequested = NO_REQUEST;
	double volumePercentRequested = NO_REQUEST;
	
	boolean isPaused = false;
	
	AbstractDecoder decoder;
	PlayerController controller;
	Track track;
	
	public AudioPlayer( PlayerController controller ) {
		this.controller = controller;
		
		Thread playerThread = new Thread ( () -> {
			runPlayerThread();
		});
		
		playerThread.setDaemon( true );
		playerThread.start();
	}
	
	public void runPlayerThread() {
		while ( true ) {
			try {
				if ( stopRequested ) {
					if ( decoder != null ) {
						decoder.closeAllResources();
						controller.playerStopped( true );
					}
					decoder = null;
					stopRequested = false;
				}	
				
				if ( trackRequested != null ) {
					synchronized ( trackRequested ) {
						if ( decoder != null ) {
							decoder.closeAllResources();
						}
						
						decoder = getPlayer ( trackRequested );
						
						if ( decoder != null ) {
							controller.playerStarted();
						} else {
							LOGGER.info( "Unable to initialize decoder for: " + track.getFilename() );
						}
		
						track = trackRequested;
						trackRequested = null;
					}
				}
				
				if ( decoder != null ) {
						
					if ( pauseRequested ) {
						pauseRequested = false;
						isPaused = true;
						controller.playerPaused();
					}
					
					if ( unpauseRequested ) {
						unpauseRequested = false;
						isPaused = false;
						controller.playerUnpaused();
					}
					
					if ( seekPercentRequested != NO_REQUEST ) {
						decoder.seekTo ( seekPercentRequested );
						controller.playerSeekedToPercent ( seekPercentRequested );
						seekPercentRequested = NO_REQUEST;
						updateTrackPosition();
					}
					
					if ( volumePercentRequested != NO_REQUEST ) {
						decoder.setVolumePercent( volumePercentRequested );
						controller.volumeChanged ( volumePercentRequested );
						volumePercentRequested = NO_REQUEST;
					}
					
					if ( !isPaused ) {
						
						boolean finishedPlaying = decoder.playSingleFrame();
						if ( finishedPlaying ) {
							decoder.closeAllResources();
							decoder = null;
							controller.playerStopped ( false );
						}
						
						updateTrackPosition();
						
					} else {
						try {
							Thread.sleep( 10 );
						} catch ( InterruptedException e ) {
							LOGGER.warning ( "Couldn't sleep while paused, may be over-utilizing CPU while paused." );
						}
					}
					
				} else { 
					try {
						Thread.sleep( 20 );
					} catch ( InterruptedException e ) {
						LOGGER.warning ( "Couldn't sleep while stopped, may be over-utilizing CPU while stopped." );
					}
				}
				
			} catch ( Exception e ) {
				//Note: We catch everything here because this loop has to keep running no matter what, or the program can't play music. 
				LOGGER.log( Level.WARNING, "Exception in AudioPlayer Loop.", e );
			}
		}
	}
	
	public boolean isStopped() {
		return decoder == null;
	}
	
	public void requestUnpause() {
		unpauseRequested = true;
	}
	
	public void requestPause() {
		pauseRequested = true;
	}
	
	public void requestTogglePause() {
		if ( isPaused ) {
			requestUnpause();
		} else {
			requestPause();
		}
	}
	
	public void requestStop () {
		stopRequested = true;
	}
	
	public void requestPlayTrack ( Track track, boolean startPaused ) {
		trackRequested = track;
		pauseRequested = startPaused;
	}
	
	public void requestSeekPercent ( double seekPercent ) {
		this.seekPercentRequested = seekPercent;
	}
	
	public void requestVolumePercent ( double volumePercent ) {
		this.volumePercentRequested = volumePercent;
	}
	
	public boolean isPaused() {
		return isPaused;
	}
	
	public Track getTrack() {
		return track;
	}

	public long getPositionMS () {
		if ( decoder == null ) {
			return 0;
		} else { 
			return decoder.getPositionMS();
		}
	}

		
	private AbstractDecoder getPlayer ( Track track ) {
		
		AbstractDecoder decoder = null;
		
		switch ( track.getFormat() ) {
			case FLAC:
				try {
					decoder = new FlacDecoder ( track );
				} catch ( Exception e ) {
					LOGGER.info("Using backup flac decoder for: " + track.getPath() );
					decoder = new BackupFlacDecoder ( track );
				}
				break;
				
			case MP3:
				decoder = new MP3Decoder ( track );
				break;
				
			case AAC:
				decoder = new MP4Decoder ( track );
				break;
				
			case OGG:
				decoder = new OggDecoder ( track );
				break;
				
			case WAV:
				decoder = new WavDecoder ( track );
				break;
			
			case UNKNOWN:
			default:
				//TODO: 
				System.out.println ( "Unknown music file type" );
				break;
		}
		
		return decoder;
	}
	
	
	void updateTrackPosition() {
		int timeElapsedMS;
		int lengthMS = track.getLengthS() * 1000;
		
		if ( seekPercentRequested == NO_REQUEST ) {
			double positionPercent = (double) getPositionMS() / ( (double) track.getLengthS() * 1000 );
			timeElapsedMS = (int)( lengthMS * positionPercent );
		} else {
			timeElapsedMS = (int)( lengthMS * seekPercentRequested );
		}
		
		controller.playTrackPositionChanged ( timeElapsedMS, lengthMS );
	}
}
