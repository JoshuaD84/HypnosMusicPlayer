package net.joshuad.hypnos.audio;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.sun.jna.NativeLibrary;

import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;
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
	private static final String NATIVE_LIBRARY_SEARCH_PATH = "stage\\lib\\vlc";
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private Runnable notifyFinished = new Runnable () {
		public void run() {
			controller.playerStopped( currentTrack, StopReason.TRACK_FINISHED );
		}
	};
	
	public VLCAudioPlayer( AudioSystem controller ) {
		this.controller = controller;

		NativeLibrary.addSearchPath( RuntimeUtil.getLibVlcLibraryName(), NATIVE_LIBRARY_SEARCH_PATH );
		vlcComponent = new AudioMediaPlayerComponent();
		mediaPlayer = vlcComponent.getMediaPlayer();
		
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
		mediaPlayer.playMedia( track.getPath().toString() );
		currentTrack = track;
		
		if ( startPaused ) {
			mediaPlayer.setPause( true );
		}
	}
	
	//VLC accepts 0 ... 200 as range for volume
	public void requestVolumePercent ( double volumePercent ) { //: allow up to 200% volume, maybe. VLC supports it. 
		if ( volumePercent < 0 ) {
			LOGGER.info( "Volume requested to be turned down below 0. Setting to 0 instead." );
			volumePercent = 0;
		} 
		
		if ( volumePercent > 1 ) {
			LOGGER.info( "Volume requested to be more than 1 (i.e. 100%). Setting to 1 instead." );
			volumePercent = 1;
		}
		
		int vlcValue = (int)(volumePercent * 100f); 
		
		mediaPlayer.setVolume(  vlcValue );
		controller.volumeChanged ( mediaPlayer.getVolume()/100f );
	}
	
	public Track getTrack() {
		return currentTrack;
	}
		
	public double getVolumePercent () {
		int vlcVolume = mediaPlayer.getVolume();
		double hypnosVolume = ((double)vlcVolume)/100;
		return hypnosVolume;
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
	
	public boolean volumeChangeSupported() {
		return true; //TODO: Test
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