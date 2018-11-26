package net.joshuad.hypnos.trayicon;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.audio.AudioSystem;
import net.joshuad.hypnos.fxui.FXUI;

public class TrayIcon {
	private static final Logger LOGGER = Logger.getLogger( TrayIcon.class.getName() );
	
	private NativeTrayIcon nativeTrayIcon;
	private BooleanProperty systemTraySupported = new SimpleBooleanProperty ( true );
	
	public TrayIcon ( FXUI ui, AudioSystem audioSystem ) {
		
		switch ( Hypnos.getOS() ) {
			case NIX:
				try {
					nativeTrayIcon = new LinuxTrayIcon( ui, audioSystem );
					nativeTrayIcon.hide();
				} catch ( IOException e ) {
					LOGGER.log( Level.INFO, "Unable to initialize linux tray icon.", e );
					nativeTrayIcon = null;
				}
				break;
				
			case OSX:
				
			case WIN_10:
			case WIN_7:
			case WIN_8:
			case WIN_UNKNOWN:
			case WIN_VISTA:
			case WIN_XP:
				nativeTrayIcon = new WindowsTrayIcon( ui, audioSystem );
				break;

			case UNKNOWN:
			default:
		}
		
		systemTraySupported.set( nativeTrayIcon != null );
	}
	
	public boolean isSupported() {
		return systemTraySupported.get();
	}
	
	public BooleanProperty systemTraySupportedProperty() {
		return systemTraySupported;
	}
	
	public void show() {
		if ( nativeTrayIcon != null ) {
			nativeTrayIcon.show();
		}
	}
	
	public void hide() {
		if ( nativeTrayIcon != null ) {
			nativeTrayIcon.hide();
		}	
	}

	public void prepareToExit() {
		nativeTrayIcon.prepareToExit();
	}
}
