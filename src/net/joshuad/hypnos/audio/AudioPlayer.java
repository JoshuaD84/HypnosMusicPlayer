package net.joshuad.hypnos.audio;

import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;
import net.joshuad.hypnos.audio.decoders.*;

public class AudioPlayer {

	public enum PlayState {
		STOPPED, PAUSED, PLAYING;
	}
	
	private static final Logger LOGGER = Logger.getLogger( AudioPlayer.class.getName() );

	public static final int NO_REQUEST = -1;
	
	boolean unpauseRequested = false;
	boolean pauseRequested = false;
	boolean stopRequested = false;
	boolean volumeErrorRequested = false;
	Track trackRequested = null;
	double seekPercentRequested = NO_REQUEST;
	double seekMSRequested = NO_REQUEST;
	double volumePercentRequested = NO_REQUEST;
	
	PlayState state = PlayState.STOPPED;
	
	AbstractDecoder decoder;
	AudioSystem controller;
	Track track;

	private double volumePercent = 1;
	
	public AudioPlayer( AudioSystem controller ) {
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
				if ( state != PlayState.STOPPED && stopRequested ) {
					state = PlayState.STOPPED;
					decoder.closeAllResources();
					controller.playerStopped( StopReason.USER_REQUESTED );
					decoder = null;
					stopRequested = false;
				}	

				if ( trackRequested != null ) {
					
					synchronized ( trackRequested ) {
						
						if ( decoder != null ) {
							decoder.closeAllResources();
						}
						
						try {
							decoder = getDecoder ( trackRequested );
							
						} catch ( IllegalStateException e ) {
							LOGGER.info( "Unable to play file: " + track.getFilename() );
							trackRequested = null;
						}

						if ( decoder != null ) {
							track = trackRequested;
							setDecoderVolume( volumePercent );
							controller.playerStarted( trackRequested );
							updateTrackPosition();
							state = PlayState.PLAYING;
							
						} else {
							LOGGER.info( "Unable to initialize decoder for: " + trackRequested.getFilename() );
							state = PlayState.STOPPED;
						}

						trackRequested = null;
						stopRequested = false;
					}
				}

				if ( volumeErrorRequested ) {
					state = PlayState.STOPPED;
					decoder.closeAllResources();
					controller.playerStopped( StopReason.ERROR );
					decoder = null;					
					volumeErrorRequested = false;
					Hypnos.warnUserVolumeNotSet();
				}
				

				if ( state != PlayState.STOPPED ) {
						
					if ( pauseRequested ) {
						pauseRequested = false;
						state = PlayState.PAUSED;
						controller.playerPaused();
						decoder.pause();
					}
					
					if ( unpauseRequested ) {
						unpauseRequested = false;
						state = PlayState.PLAYING;
						controller.playerUnpaused();
						decoder.unpause();
					}
					
					if ( seekPercentRequested != NO_REQUEST ) {
						decoder.seekTo ( seekPercentRequested );
						updateTrackPosition();
						seekPercentRequested = NO_REQUEST;
					}
					
					if ( seekMSRequested != NO_REQUEST ) {
						decoder.seekTo ( seekMSRequested / (double)( track.getLengthS() * 1000 ) );
						updateTrackPosition();
						seekMSRequested = NO_REQUEST;
					}
					
					if ( volumePercentRequested != NO_REQUEST ) {
						setDecoderVolume ( volumePercentRequested );
						controller.volumeChanged ( volumePercentRequested );
						volumePercent = decoder.getVolumePercent();
						volumePercentRequested = NO_REQUEST;
					}
					
					if ( state == PlayState.PLAYING ) {
						
						boolean finishedPlaying = decoder.playSingleFrame();
						updateTrackPosition();
						
						if ( finishedPlaying ) {
							decoder.closeAllResources();
							decoder = null;
							state = PlayState.STOPPED;
							controller.playerStopped( StopReason.TRACK_FINISHED );
						} 				
						
					} else {
						try {
							Thread.sleep( 10 );
						} catch ( InterruptedException e ) {
							LOGGER.fine( "Interrupted while paused." );
						}
					}
					
				} else { 
					try {
						Thread.sleep( 20 );
					} catch ( InterruptedException e ) {
						LOGGER.fine ( "Interrupted while stopped" );
					}
				}
				
			} catch ( Exception e ) {
				//Note: We catch everything here because this loop has to keep running no matter what, or the program can't play music. 
				LOGGER.log( Level.WARNING, "Exception in AudioPlayer Loop.", e );
			}
		}
	}
	
	public void requestUnpause() {
		unpauseRequested = true;
	}
	
	public void requestPause() {
		pauseRequested = true;
	}
	
	public void requestTogglePause() {
		switch ( state ) {
			case PAUSED:
				requestUnpause();
				break;
				
			case PLAYING:
				requestPause();
				break;
				
			case STOPPED: //Fallthrough. 
			default:
				//Do nothing. 
				break;
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

	public void requestIncrementMS ( int diffMS ) {
		if ( seekMSRequested == NO_REQUEST ) {
			seekMSRequested = this.getPositionMS() + diffMS;
		} else {
			seekMSRequested += diffMS;
		}
		
	}

	public void requestSeekMS ( long seekMS ) {
		if ( seekMS < 0 ) {
			LOGGER.info( "Requested a seek to a negative location. Seeking to 0 instead." );
			seekMS = 0;
		}

		this.seekMSRequested = seekMS;
	}
	
	public void requestVolumePercent ( double volumePercent ) {
		if ( volumePercent < 0 ) {
			LOGGER.info( "Volume requested to be turned down below 0. Setting to 0 instead." );
			volumePercent = 0;
		}
		this.volumePercentRequested = volumePercent;
		this.volumePercent = volumePercent;
		
		if ( this.decoder == null ) {
			controller.volumeChanged ( volumePercent );
		}
	}
		
	public Track getTrack() {
		if ( isStopped() ) {
			return null;
		} else {
			return track;
		}
	}
	
	public boolean isStopped () {
		if ( trackRequested != null ) return false;
		else if ( state == PlayState.STOPPED ) return true;
		else if ( stopRequested ) return true;
		else return false;
	}
	
	public boolean isPaused () {
		if ( trackRequested != null ) return false;
		else if ( stopRequested ) return false;
		else if ( unpauseRequested ) return false;
		else if ( state == PlayState.PAUSED ) return true;
		else if ( pauseRequested ) return true;
		else return false; 
	}
	
	public boolean isPlaying() {
		return ( !isStopped() && !isPaused() );
	}
	
	public long getPositionMS () {
		if ( decoder == null ) {
			return 0;
		} else { 
			return decoder.getPositionMS();
		}
	}
		
	private AbstractDecoder getDecoder ( Track track ) {
		
		if ( track == null ) {
			LOGGER.info ( "Asked to play null track, ignoring." );
			return null;
		}
		
		if ( !Files.exists( track.getPath() ) ) {
			LOGGER.info ( "Track file does not exist: " + track.getPath().toString() );
			return null;
		}
		
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
				LOGGER.info( "Unrecognized file format. Unable to initialize decoder." );
				break;
		}
		
		
		return decoder;
	}
	
	private void setDecoderVolume ( double volumePercent ) { 
		
		try {
			decoder.setVolumePercent( volumePercent );
			
		} catch ( IllegalArgumentException e ) {
			
			if ( volumePercent < 1 ) {
				volumeErrorRequested = true;
			}
		}
	}
	
	public boolean volumeChangeSupported() {
		if ( decoder != null ) {
			return decoder.volumeChangeSupported();
			
		} else {
			return true;
		}
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
		
		controller.playerTrackPositionChanged ( timeElapsedMS, lengthMS );
	}

	public double getVolumePercent () {
		if ( decoder != null ) {
			return decoder.getVolumePercent();
		} else {
			return volumePercent;
		}
	}
}
