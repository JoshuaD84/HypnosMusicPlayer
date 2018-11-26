package net.joshuad.hypnos.hotkeys;

import java.io.IOException;

import javafx.scene.input.KeyEvent;
import jxgrabkey.HotkeyConflictException;
import jxgrabkey.JXGrabKey;
import jxgrabkey.X11KeysymDefinitions;
import jxgrabkey.X11MaskDefinitions;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys.Hotkey;

public class LinuxGlobalHotkeys extends SystemHotkeys {

	public LinuxGlobalHotkeys ( GlobalHotkeys parent ) throws HotkeyException {
		try {
			System.load( Hypnos.getRootDirectory().resolve( "lib/nix/libJXGrabKey.so" ).toFile().getCanonicalPath() );
		} catch ( IOException e ) {
			throw new HotkeyException ( "Unable to load Hotkey Library for Linux: " + e );
		}

		JXGrabKey.getInstance().addHotkeyListener( ( int hotkeyID ) -> {
			if ( hotkeyID >= 0 && hotkeyID < Hotkey.values().length ) {
				Hotkey hotkey = Hotkey.values() [ hotkeyID ];
				parent.systemHotkeyEventHappened ( hotkey );
			}
		});
	}
	
	@Override
	void unregisterHotkey( Hotkey hotkey ) {
		JXGrabKey.getInstance().unregisterHotKey ( hotkey.ordinal() );	
	}
	
	@Override
	boolean registerHotkey ( Hotkey hotkey, HotkeyState keystate ) {
		int hotkeyID = hotkey.ordinal();
		
		int key = X11KeysymDefinitions.fxKeyToX11Keysym ( keystate.getCode() );
		int mask = X11MaskDefinitions.fxKeyStateToX11Mask ( keystate );
		
		if ( key == X11KeysymDefinitions.ERROR ) return false; 
		
		try {
			JXGrabKey.getInstance().registerX11Hotkey( hotkeyID, mask, key );
		} catch ( HotkeyConflictException e ) {
			return false;
		}
		
		return true;
	}

	@Override
	protected HotkeyState createJustPressedState( KeyEvent keyEvent ) {
		return null;
	}
}
