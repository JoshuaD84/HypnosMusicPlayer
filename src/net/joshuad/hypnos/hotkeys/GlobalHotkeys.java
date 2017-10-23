package net.joshuad.hypnos.hotkeys;

import java.util.EnumMap;

import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import net.joshuad.hypnos.Hypnos;

public class GlobalHotkeys implements NativeKeyListener {

	public enum Hotkey {
		PREVIOUS ( "Previous" ), 
		NEXT ( "Next" ), 
		PLAY ( "Play" ), 
		TOGGLE_PAUSE ( "Pause / Unpause" ),
		STOP ( "Stop" ),
		
		VOLUME_UP ( "Volume Up" ),
		VOLUME_DOWN ( "Volume Down" ),
		TOGGLE_MUTE ( "Mute / Unmute" ),
		
		TOGGLE_SHUFFLE ( "Toggle Shuffle" ),
		TOGGLE_REPEAT ( "Toggle Repeat" ),
		
		SHOW_HIDE_UI ( "Show/Hide Main Window" ),
		
		SKIP_FORWARD ( "Forward 5 Seconds" ),
		SKIP_BACK ( "Back 5 Seconds" );
		
				
		private String label;
		Hotkey ( String label ) { this.label = label; }
		public String getLabel () { return label; }
	}
	
	private boolean disabled = false;
	
	private KeyState lastKeyState = null;
	
	private EnumMap <Hotkey, KeyState> hotkeyMap = new EnumMap <Hotkey, KeyState> ( Hotkey.class );
	
	public GlobalHotkeys() {}
	
	public boolean registerLastCombination ( Hotkey hotkey, String display ) {
		if ( lastKeyState == null ) return false;
		if ( hotkey == null ) return false;
		
		for ( Hotkey key : hotkeyMap.keySet() ) {
			KeyState registeredHotkey = hotkeyMap.get( key );
			if ( registeredHotkey != null && registeredHotkey.equals( lastKeyState ) ) {
				hotkeyMap.remove( key );
			}
		}
		
		lastKeyState.setDisplay ( display );
		
		hotkeyMap.put( hotkey, lastKeyState );
		
		return true;
	}
	
	public void clearHotkey ( Hotkey key ) {
		if ( key == null ) return;
		hotkeyMap.remove( key );
	}
	
	public String getDisplay ( Hotkey hotkey ) {
		KeyState keyState = hotkeyMap.get( hotkey );
		
		if ( keyState == null ) return "";
		else return keyState.getDisplay();
	}
	
	public void disable() {
		this.disabled = true;
	}
	
	public void enable() {
		this.disabled = false;
	}
	
	public void nativeKeyPressed ( NativeKeyEvent e ) {
		lastKeyState = new KeyState ( e.getKeyCode(), e.getModifiers() );
		
		if ( !Hypnos.hotkeysDisabledForConfig() ) {
			for ( Hotkey hotkey : hotkeyMap.keySet() ) {
				KeyState registeredHotkey = hotkeyMap.get( hotkey );
				if ( registeredHotkey != null && registeredHotkey.equals( lastKeyState ) ) {
					Hypnos.doHotkeyAction ( hotkey );
					
					/*
					try {
						//This is an attempt to consume the hotkey. Doesn't work in X11
						Field f = NativeInputEvent.class.getDeclaredField("reserved");
						f.setAccessible(true);
						f.setShort(e, (short) 0x01);
					} catch (Exception ex) {
						ex.printStackTrace();
					}*/
				}
			}
		}
	}

	public EnumMap <Hotkey, KeyState> getMap() {
		return hotkeyMap;
	}
	
	public void setMap ( EnumMap <Hotkey, KeyState> map ) {
		this.hotkeyMap = map;
	}
	
	public void nativeKeyReleased(NativeKeyEvent e) {}
	public void nativeKeyTyped(NativeKeyEvent e) {}

	
}

