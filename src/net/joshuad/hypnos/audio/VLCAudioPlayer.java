package net.joshuad.hypnos.audio;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.jna.NativeLibrary;

import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.Hypnos.ExitCode;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;
import net.joshuad.hypnos.fxui.FXUI;
import uk.co.caprica.vlcj.binding.internal.libvlc_state_t;
import uk.co.caprica.vlcj.component.AudioMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

public class VLCAudioPlayer {

	private static final Logger LOGGER = Logger.getLogger( VLCAudioPlayer.class.getName() );

	public enum PlayState {
		STOPPED, PAUSED, PLAYING;
	}

	PlayState state = PlayState.STOPPED;
	AudioSystem controller;
	
	Track currentTrack = null;

	private AudioMediaPlayerComponent vlcComponent;
	private MediaPlayer mediaPlayer;
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private Runnable notifyFinished = new Runnable () {
		public void run() {
			controller.playerStopped( currentTrack, StopReason.TRACK_FINISHED );
		}
	};
	
	private int currentVolume = 100; 
	
	public VLCAudioPlayer( AudioSystem controller ) {
		this.controller = controller;
		
		String nativeVLCLibPath = "";
		
		switch ( Hypnos.getOS() ) {
			case NIX:
				nativeVLCLibPath = Hypnos.getRootDirectory().resolve( "lib/nix/vlc/" ).toAbsolutePath().toString();
				break;
			case OSX:
				nativeVLCLibPath = Hypnos.getRootDirectory().resolve( "lib/osx/vlc" ).toAbsolutePath().toString();
				break;
			case UNKNOWN:
				break;
			case WIN_10:
			case WIN_7:
			case WIN_8:
			case WIN_UNKNOWN:
			case WIN_VISTA:
			case WIN_XP:
				nativeVLCLibPath = Hypnos.getRootDirectory().resolve( "lib/win/vlc" ).toAbsolutePath().toString();
				break;
			default:
				LOGGER.severe( "Cannot determine OS, unable to load native VLC libraries. Exiting." );
				Hypnos.exit( ExitCode.UNSUPPORTED_OS );
				break;
		}
		
		try {
			NativeLibrary.addSearchPath( RuntimeUtil.getLibVlcLibraryName(), nativeVLCLibPath );
			vlcComponent = new AudioMediaPlayerComponent();
			mediaPlayer = vlcComponent.getMediaPlayer();
		} catch ( Exception e ) {
			LOGGER.log( Level.SEVERE, "Unable to load VLC libaries.", e );
			FXUI.notifyUserVLCLibraryError();
			Hypnos.exit( ExitCode.AUDIO_ERROR );
		}
		
		mediaPlayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
			
			public void finished ( MediaPlayer player ) {
				scheduler.schedule( notifyFinished, 50, TimeUnit.MILLISECONDS );
			}
			
			//Can do either timeChanged or positionChanged, don't do both (duplicative)
			public void timeChanged ( MediaPlayer player, long newTime ) {
				updateControllerTrackPosition();
			}
						
			public void backward ( MediaPlayer player ) {
				updateControllerTrackPosition();
			}
			
			public void forward ( MediaPlayer player ) {
				updateControllerTrackPosition();
			}
			
			public void paused ( MediaPlayer player ) {
				controller.playerPaused();
			}
				
			public void playing ( MediaPlayer player ) {
				controller.playerStarted( currentTrack );
			}
		});
	}
	
	private void updateControllerTrackPosition() {
		if ( currentTrack != null ) {
			
			int lengthMS = currentTrack.getLengthS() * 1000;
			double positionPercent = mediaPlayer.getPosition();
			int timeElapsedMS = (int)( lengthMS * positionPercent );
			
			controller.playerTrackPositionChanged ( currentTrack, timeElapsedMS, lengthMS );
		}
	}

	public void requestTogglePause() {
		mediaPlayer.setPause( vlcComponent.getMediaPlayer().isPlaying() );
	}
	
	public void requestUnpause() {
		mediaPlayer.setPause( false );
	}
	
	public void requestPause() {
		mediaPlayer.setPause( true );
	}
	
	public void requestStop() {
		mediaPlayer.stop();
		currentTrack = null;
	}
	
	public void requestPlayTrack( Track track, boolean startPaused ) {
		
		String targetFile = track.getPath().toString();
		
		//Address this bug: https://github.com/caprica/vlcj/issues/645
		switch ( Hypnos.getOS() ) {
			case NIX:
			case OSX:
			case UNKNOWN:
			default:
				break;
			case WIN_10:
			case WIN_7:
			case WIN_8:
			case WIN_UNKNOWN:
			case WIN_VISTA:
			case WIN_XP:
				//Assumes filename is an absolute file location. 
				targetFile = new File( targetFile ).toURI().toASCIIString().replaceFirst( "file:/", "file:///" );
				break;
		}
		
		mediaPlayer.playMedia( targetFile );
		
		final int target = currentVolume;
		scheduler.schedule( new Runnable() {
			public void run() {
				mediaPlayer.setVolume( target );
			}
		}, 50, TimeUnit.MILLISECONDS );
		
		currentTrack = track;
		
		if ( startPaused ) {
			mediaPlayer.setPause( true );
		}
	}
	
	//VLC accepts 0 ... 200 as range for volume
	public void requestVolumePercent ( double volumePercent ) { //TODO: allow up to 200% volume, maybe. VLC supports it. 
		if ( volumePercent < 0 ) {
			LOGGER.info( "Volume requested to be turned down below 0. Setting to 0 instead." );
			volumePercent = 0;
		} 
		
		if ( volumePercent > 1 ) {
			LOGGER.info( "Volume requested to be more than 1 (i.e. 100%). Setting to 1 instead." );
			volumePercent = 1;
		}
		
		int vlcValue = (int)(volumePercent * 100f); 
		
		if ( vlcValue != currentVolume ) {
			currentVolume = vlcValue;
			if ( !isStopped() ) {
				mediaPlayer.setVolume( vlcValue );
			}
			controller.volumeChanged ( currentVolume/100f ); 
		}
	}
		
	public double getVolumePercent () {
		int vlcVolume = currentVolume;
		double hypnosVolume = ((double)vlcVolume)/100;
		return hypnosVolume;
	}	
	
	public Track getTrack() {
		return currentTrack;
	}
	
	public void requestSeekPercent ( double seekPercent ) {
		mediaPlayer.setPosition( (float)seekPercent );
		updateControllerTrackPosition();
	}
	
	public long getPositionMS() {
		int lengthMS = currentTrack.getLengthS() * 1000;
		double positionPercent = mediaPlayer.getPosition();
		long timeElapsedMS = (long)(lengthMS * positionPercent);
		return timeElapsedMS;
	}
	
	public void requestIncrementMS ( int diffMS ) {
		mediaPlayer.setTime( getPositionMS() + diffMS );
		updateControllerTrackPosition();
	}

	public void requestSeekMS ( long seekMS ) {
		mediaPlayer.setTime( seekMS );
		updateControllerTrackPosition();
	}
	
	public PlayState getState() {
		if ( isStopped() ) return PlayState.STOPPED;
		else if ( isPaused() ) return PlayState.PAUSED;
		else return PlayState.PLAYING;
	}
	
	public boolean isStopped () {
		return currentTrack == null;
	}
	
	public boolean isPaused () { 
		return mediaPlayer.getMediaState() == libvlc_state_t.libvlc_Paused;
	}
	
	public boolean isPlaying() {
		return mediaPlayer.getMediaState() == libvlc_state_t.libvlc_Playing;
	}
	
	public void releaseResources() {
		mediaPlayer.release();
	}
}