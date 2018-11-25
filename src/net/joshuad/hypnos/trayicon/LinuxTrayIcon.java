package net.joshuad.hypnos.trayicon;

import java.io.File;
import java.io.IOException;

import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Hypnos.ExitCode;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;
import net.joshuad.hypnos.fxui.FXUI;

public class LinuxTrayIcon extends NativeTrayIcon {
	private native void initTrayIcon ( String iconLocation );
	private native void showTrayIcon ();
	private native void hideTrayIcon ();

	private boolean doneInit = true;
	
	private boolean exitRequested = false;
	private boolean toggleUIRequested = false;
	private boolean playRequested = false;
	private boolean pauseRequested = false;
	private boolean previousRequested = false;
	private boolean nextRequested = false;
	private boolean stopRequested = false;
	private boolean muteRequested = false;

	private FXUI ui;
	private AudioSystem audioSystem;
	
	public LinuxTrayIcon ( FXUI ui, AudioSystem audioSystem ) throws IOException {
		this.ui = ui;
		this.audioSystem = audioSystem;
		
		System.load( new File( "stage/lib/nix/tray_icon_jni.so" ).getCanonicalPath() );
		// System.load ( Hypnos.getRootDirectory().resolve(
		// "lib/nix/tray_icon_jni.so" ).toFile().getCanonicalPath() );
		Thread gtkThread = new Thread( () -> {
			initTrayIcon( "/d/programming/workspace/MusicPlayer/stage/resources/icon.png" );
		} );

		gtkThread.setName( "GTK Tray Icon Thread" );
		gtkThread.setDaemon( true );

		doneInit = false;
		gtkThread.start();
		while ( !doneInit ) {
			try {
				Thread.sleep( 5 );
			} catch ( InterruptedException e ) {
			}
		}
		
		Thread enacterThread = new Thread( () -> {
			while ( true ) {
					if ( exitRequested ) Hypnos.exit( ExitCode.NORMAL );
					if ( toggleUIRequested ) ui.toggleMinimized();
					if ( playRequested ) audioSystem.play();
					if ( pauseRequested ) audioSystem.pause();
					if ( previousRequested ) audioSystem.previous();
					if ( nextRequested ) audioSystem.next();
					if ( stopRequested ) audioSystem.stop( StopReason.USER_REQUESTED );
					if ( muteRequested ) audioSystem.toggleMute();
					
					exitRequested = false;    
					toggleUIRequested = false;
					playRequested = false;    
					pauseRequested = false;   
					previousRequested = false;
					nextRequested = false;    
					stopRequested = false;    
					muteRequested = false;    
				
				try {
					Thread.sleep( 25 );
				} catch ( InterruptedException e ) {
				}
			}
		} );
		
		enacterThread.setName( "Enacter thread for GTK requests" );
		enacterThread.setDaemon( true );
		enacterThread.start();
	}

	@Override
	public void show () {
		showTrayIcon();
	}
	
	@Override
	public void hide () {
		hideTrayIcon();
	}

	void jni_doneInit () {
		doneInit = true;
	}

	void jni_requestExit () {
			exitRequested = true;
	}

	void jni_requestToggleUI () {
			toggleUIRequested = true;
	}

	void jni_requestPlay () {
			playRequested = true;
	}

	void jni_requestPause () {
			pauseRequested = true;
	}

	void jni_requestPrevious () {
			previousRequested = true;
	}

	void jni_requestNext () {
			nextRequested = true;
	}

	void jni_requestStop () {
			stopRequested = true;
	}

	void jni_requestMute () {
			muteRequested = true;
	}
}
