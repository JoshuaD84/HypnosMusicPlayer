package net.joshuad.hypnos.hotkeys;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javafx.scene.input.KeyEvent;
import net.joshuad.hypnos.Hypnos.OS;

public class GlobalHotkeys {
	private static transient final Logger LOGGER = Logger.getLogger( GlobalHotkeys.class.getName() );
	
	public enum Hotkey {
		PREVIOUS ( "Previous" ), 
		NEXT ( "Next" ), 
		PLAY_PAUSE ( "Play / Pause" ),
		PLAY ( "Play" ), 
		STOP ( "Stop" ),
		
		VOLUME_UP ( "Volume Up" ),
		VOLUME_DOWN ( "Volume Down" ),
		TOGGLE_MUTE ( "Mute / Unmute" ),
		
		TOGGLE_SHUFFLE ( "Toggle Shuffle" ),
		TOGGLE_REPEAT ( "Toggle Repeat" ),
		
		SHOW_HIDE_UI ( "Show / Hide Main Window" ),
		
		SKIP_FORWARD ( "Forward 5 Seconds" ),
		SKIP_BACK ( "Back 5 Seconds" );
		
		private String label;
		Hotkey ( String label ) { this.label = label; }
		public String getLabel () { return label; }
	}

	List<GlobalHotkeyListener> listeners = new ArrayList<>();

	Map <Hotkey, HotkeyState> hotkeyMap = new EnumMap <> ( Hotkey.class );
	
	private boolean disabled = false;
	boolean hasUnsavedData = false;
	private boolean inEditMode = false;
	
	private SystemHotkeys system;
	
	public GlobalHotkeys( OS operatingSystem, boolean disabled ) {
		this.disabled = disabled;
		this.hasUnsavedData = true;
		try {
			switch ( operatingSystem ) {
				case NIX:
					system = new LinuxGlobalHotkeys( this );
					break;
				case OSX:
					throw new HotkeyException ( "OSX does not currently support hotkeys." );
				case WIN_10:
				case WIN_7:
				case WIN_8:
				case WIN_UNKNOWN:
				case WIN_VISTA:
					system = new WindowsGlobalHotkeys( this );
					break;
				case WIN_XP:
					throw new HotkeyException ( "Windows XP not supported by Hypnos." );
				case UNKNOWN:
				default:
					throw new HotkeyException ( "Cannot recognize operating system." );
				
			}
		} catch ( HotkeyException e ) {
			system = new DummyGlobalHotkeys ( this );
			disabled = true;
			LOGGER.info( "Unable to setup global hotkeys, they are disabled: " + e.getMessage() );
		}
	}
	
	public void addListener ( GlobalHotkeyListener listener ) {
		listeners.add ( listener );
	}
	
	public boolean hasUnsavedData () {
		return hasUnsavedData;
	}

	public void setHasUnsavedData ( boolean b ) {
		hasUnsavedData = b;
	}
	
	public boolean isDisabled() {
		return disabled || system instanceof DummyGlobalHotkeys;
	}
	
	public String getReasonDisabled() {
		if ( disabled ) {
			return "Disabled at launch";
		} else if ( system instanceof DummyGlobalHotkeys ) {
			return "Global Hotkeys not currently supported by Hypnos for your Operating System.";
		} else {
			return "";
		}
	}
	
	public boolean registerFXHotkey ( Hotkey hotkey, KeyEvent event ) {
		if ( isDisabled() ) return false;
		return registerHotkey ( hotkey, new HotkeyState ( event ) );
	}

	public boolean registerHotkey ( Hotkey hotkey, HotkeyState keystate ) {
		if ( isDisabled() ) return false;
		if ( keystate == null ) return false;
		
		for ( Hotkey existingKey : hotkeyMap.keySet() ) {
			if ( keystate.equals( hotkeyMap.get( existingKey ) ) ) {
				clearHotkey ( existingKey );
			}
		}
		
		boolean result = system.registerHotkey ( hotkey, keystate );
		if ( inEditMode ) {
			system.unregisterHotkey ( hotkey );
		}
		
		if ( result ) {
			hotkeyMap.put( hotkey, keystate );
			hasUnsavedData = true;
		} 
		
		return result;
	}
	
	public void beginEditMode() {
		if ( isDisabled() ) return;
		if ( !inEditMode ) {
			for ( Hotkey hotkey : hotkeyMap.keySet() ) {
				system.unregisterHotkey ( hotkey );
			}
			inEditMode = true;
		}
	}
	
	public void endEditMode() {
		if ( isDisabled() ) return;
		if ( inEditMode ) {
			for ( Hotkey hotkey : hotkeyMap.keySet() ) {
				system.registerHotkey ( hotkey, hotkeyMap.get( hotkey ) );
			}
			inEditMode = false;
		}
	}
	
	public void prepareToExit() {
		for ( Hotkey hotkey : hotkeyMap.keySet() ) {
			system.unregisterHotkey ( hotkey );
		}
	}
	
	public void clearAllHotkeys() {
		if ( isDisabled() ) return;
		for ( Hotkey hotkey : hotkeyMap.keySet() ) {
			clearHotkey ( hotkey );
		}
	}
	
	public void clearHotkey ( Hotkey key ) {
		if ( isDisabled() ) return;
		if ( key == null ) return;
		hotkeyMap.remove( key );
		hasUnsavedData = true;
		system.unregisterHotkey ( key );
	}

	public Map <Hotkey, HotkeyState> getMap() {
		return hotkeyMap;
	}
	
	public void setMap ( EnumMap <Hotkey, HotkeyState> map ) {
		this.hotkeyMap = map;

		if ( isDisabled() ) return;
		for ( Hotkey hotkey : map.keySet() ) {
			registerHotkey ( hotkey, map.get( hotkey ) );
		}
	}

	public String getDisplayText ( Hotkey hotkey ) {
		HotkeyState state = hotkeyMap.get( hotkey );
		
		if ( state == null ) {
			return "";
		} else {
			return hotkeyMap.get( hotkey ).getDisplayText( );
		}
	}

	void systemHotkeyEventHappened ( Hotkey hotkey ) {
		if ( isDisabled() || inEditMode ) return;
		for ( GlobalHotkeyListener listener : listeners ) {
			listener.hotkeyPressed ( hotkey );
		}
	}

	public HotkeyState createJustPressedState ( KeyEvent keyEvent ) {
		return system.createJustPressedState ( keyEvent );
	}
}
