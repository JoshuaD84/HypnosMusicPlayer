package net.joshuad.hypnos.trayicon;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Hypnos.ExitCode;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.audio.AudioSystem.RepeatMode;
import net.joshuad.hypnos.audio.AudioSystem.ShuffleMode;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;
import net.joshuad.hypnos.audio.PlayerListener;
import net.joshuad.hypnos.fxui.FXUI;
import net.joshuad.hypnos.library.Track;

public class WindowsTrayIcon extends NativeTrayIcon implements PlayerListener {
	private static final Logger LOGGER = Logger.getLogger( WindowsTrayIcon.class.getName() );
	
	private FXUI ui;
	
	private Display swtDisplay; 
	private TrayItem trayIcon;
	private Shell shell;
	private Menu menu;
	
	private MenuItem play;
	
	private Object lock = new Object();
	private boolean hideTrayIconRequested = false;
	private boolean showTrayIconRequested = false;
	private boolean exitRequested = false;
	private boolean setTextToPlay = false;
	private boolean setTextToPause = false;
	private boolean setTextToUnmute = false;
	private boolean setTextToMute = false;
	private boolean setTextToHide = false;
	private boolean setTextToShow =false;
	
	
	public WindowsTrayIcon ( FXUI ui, AudioSystem audioSystem ) {
		this.ui = ui;
		
		Thread t = new Thread (() -> {
			swtDisplay = new Display();
			
			shell = new Shell( swtDisplay );
			menu = new Menu( shell, SWT.POP_UP );
			
			MenuItem toggleShow = new MenuItem( menu, SWT.PUSH );
			toggleShow.setText( "Hide" );
			toggleShow.addListener( SWT.Selection, (Event e) -> {
				ui.toggleHidden();
			});
			
			ui.getMainStage().showingProperty().addListener( ( obs, wasShowing, isShowing ) -> {
				synchronized ( lock ) {
					if ( isShowing ) {
						setTextToHide = true;
						setTextToShow = false;
					} else {
						setTextToHide = false;
						setTextToShow = true;
					}
				}
			});
			
			new MenuItem( menu, SWT.SEPARATOR );
			 
			play = new MenuItem( menu, SWT.PUSH );
			play.setText( "Play" );
			play.addListener( SWT.Selection, (Event e) -> {
				audioSystem.togglePlayPause();
			});
						
			MenuItem next = new MenuItem( menu, SWT.PUSH );
			next.setText( "Next" );
			next.addListener( SWT.Selection, (Event e) -> {
				audioSystem.next();
			});
			
			MenuItem previous = new MenuItem( menu, SWT.PUSH );
			previous.setText( "Previous" );
			previous.addListener( SWT.Selection, (Event e) -> {
				audioSystem.previous();
			});
			
			MenuItem stop = new MenuItem( menu, SWT.PUSH );
			stop.setText( "Stop" );
			stop.addListener( SWT.Selection, (Event e) -> {
				audioSystem.stop( StopReason.USER_REQUESTED );
			});
			
			MenuItem toggleMute = new MenuItem( menu, SWT.PUSH );
			toggleMute.setText( audioSystem.getVolumePercent() == 0 ? "Unmute" : "Mute" );
			toggleMute.addListener( SWT.Selection, (Event e) -> {
				audioSystem.toggleMute();
			});
			
			new MenuItem( menu, SWT.SEPARATOR );
			
			MenuItem quit = new MenuItem( menu, SWT.PUSH );
			quit.setText( "Quit" );
			quit.addListener( SWT.Selection, (Event e) -> {
				if ( trayIcon != null ) {
					trayIcon.setVisible ( false );
				}
				swtDisplay.dispose();
				Hypnos.exit( ExitCode.NORMAL );
			});
			
			while ( !shell.isDisposed() ) {
				synchronized ( lock ) {
					if ( showTrayIconRequested ) {
						if ( trayIcon == null ) {
							initializeTrayIcon();
						}
						trayIcon.setVisible ( true );
						showTrayIconRequested = false;
					} 
					
					if ( hideTrayIconRequested ) {
						if ( trayIcon != null ) {
							trayIcon.setVisible ( false );
						}
						hideTrayIconRequested = false;
					}
				
					if ( setTextToPlay ) {
						play.setText( "Play" );
						setTextToPlay = false;
					}
					if ( setTextToPause ) {
						play.setText( "Pause" );
						setTextToPause = false;
					}
					if ( setTextToUnmute ) {
						toggleMute.setText( "Unmute" );
						setTextToUnmute = false;
					}
					if ( setTextToMute ) {
						toggleMute.setText( "Mute" );
						setTextToMute = false;
					}
					
					if ( setTextToShow ) {
						toggleShow.setText( "Show" );
						setTextToShow = false;
					}
					
					if ( setTextToHide ) {
						toggleShow.setText( "Hide" );
						setTextToHide = false;
					}
				}
				
				if ( !swtDisplay.readAndDispatch() ) {
					try {
						Thread.sleep( 25 );
					} catch (InterruptedException e1) {
						LOGGER.log ( Level.INFO, "Interrupted during swt sleep loop", e1 );
					}
				}
				
				if ( exitRequested ) {
					if ( trayIcon != null ) {
						trayIcon.setVisible ( false );
					}
					swtDisplay.dispose();
				}
			}
		});
		
		t.setDaemon( true );
		t.setName("SWT Tray Icon Thread");
		t.start();

		audioSystem.addPlayerListener( this );
	}
	
	public void initializeTrayIcon () {
		Tray tray = shell.getDisplay().getSystemTray();	
		trayIcon = new TrayItem( tray, SWT.NONE );
		trayIcon.setImage( new Image( swtDisplay, Hypnos.getRootDirectory().resolve( "resources/icon.png" ).toString() ) );
		
		trayIcon.addListener( SWT.Selection, (Event event) -> {
			ui.toggleHidden();
		});
		
		trayIcon.addListener( SWT.MenuDetect, (Event event) -> {
			menu.setVisible( true );
		});
	}
	

	@Override
	public void show() {
		showTrayIconRequested = true;
	}

	@Override
	public void hide() {
		hideTrayIconRequested = true;
	}


	@Override
	protected void prepareToExit() {
		exitRequested = true;
	}


	@Override
	public void playerStopped(Track track, StopReason reason) {
		synchronized ( lock ) {
			setTextToPlay = true;
			setTextToPause = false;
		}
	}

	@Override
	public void playerStarted(Track track) {
		synchronized ( lock ) {
			setTextToPlay = false;
			setTextToPause = true;
		}
	}

	@Override
	public void playerPaused() {
		synchronized ( lock ) {
			setTextToPlay = true;
			setTextToPause = false;
		}
	}

	@Override
	public void playerUnpaused() {
		synchronized ( lock ) {
			setTextToPlay = false;
			setTextToPause = true;
		}
	}

	@Override
	public void playerVolumeChanged(double newVolumePercent) {
		synchronized ( lock ) {
			if ( newVolumePercent == 0 ) {
				setTextToUnmute = true;
				setTextToMute = false;
			} else {
				setTextToUnmute = false;
				setTextToMute = true;
			}
		}
	}

	@Override
	public void playerShuffleModeChanged(ShuffleMode newMode) {}
	@Override
	public void playerRepeatModeChanged(RepeatMode newMode) {}
	@Override
	public void playerPositionChanged(int positionMS, int lengthMS) {}
	
	/*
	private Image toIcon ( Image image, int size ) {
		
		ImageData data = image.getImageData().scaledTo( size, size );

		for (int x = 0; x < data.width; x++ ) {
			for ( int y = 0; y < data.height; y++ ) {
				data.setPixel( x, y, 0x333333 );
			}
		}
        
		return new Image ( swtDisplay, data );
	}
	
	Image previousImage = new Image( swtDisplay, Hypnos.getRootDirectory().resolve( "resources/previous.png" ).toString() );
	previousImage = toIcon ( previousImage, 14 );
	
	Image stopImage = new Image( swtDisplay, Hypnos.getRootDirectory().resolve( "resources/stop.png" ).toString() );
	stopImage = toIcon ( stopImage, 14 );
	
	Image nextImage = new Image( swtDisplay, Hypnos.getRootDirectory().resolve( "resources/next.png" ).toString() );
	nextImage = toIcon ( nextImage, 14 );
	
	Image pauseImage = new Image( swtDisplay, Hypnos.getRootDirectory().resolve( "resources/pause.png" ).toString() );
	pauseImage = toIcon ( pauseImage, 14 );
	
	Image playImage = new Image( swtDisplay, Hypnos.getRootDirectory().resolve( "resources/play.png" ).toString() );
	playImage = toIcon ( playImage, 14 );
	
	Image muteImage = new Image( swtDisplay, Hypnos.getRootDirectory().resolve( "resources/vol-0.png" ).toString() );
	muteImage = toIcon ( muteImage, 14 );
	
	Image unuteImage = new Image( swtDisplay, Hypnos.getRootDirectory().resolve( "resources/vol-3.png" ).toString() );
	unuteImage = toIcon ( unuteImage, 14 );
	
	Image quitImage = new Image( swtDisplay, Hypnos.getRootDirectory().resolve( "resources/clear.png" ).toString() );
	quitImage = toIcon ( quitImage, 14 );
	*/
}
