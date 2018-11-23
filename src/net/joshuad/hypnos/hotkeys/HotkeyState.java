package net.joshuad.hypnos.hotkeys;

import java.io.Serializable;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class HotkeyState implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private KeyCode keyCode; 
	
	private boolean isShiftDown;
	private boolean isControlDown;
	private boolean isAltDown;
	private boolean isMetaDown;

	public HotkeyState ( KeyCode keyCode, boolean shiftDown, boolean controlDown, boolean altDown, boolean metaDown ) {
		this.keyCode = keyCode;
		this.isShiftDown = shiftDown;
		this.isControlDown = controlDown;
		this.isAltDown = altDown;
		this.isMetaDown = metaDown;
	}
	
	public HotkeyState ( KeyEvent event ) {
		keyCode = event.getCode();
		isShiftDown = event.isShiftDown();
		isControlDown = event.isControlDown();
		isAltDown = event.isAltDown();
		isMetaDown = event.isMetaDown();
	}
	
	public static String getDisplayText ( KeyEvent event ) {
		return new HotkeyState ( event ).getDisplayText();
	}
	
	public KeyCode getCode() {
		return keyCode;
	}
	
	public boolean isShiftDown() {
		return isShiftDown;
	}
	
	public boolean isControlDown() {
		return isControlDown;
	}
	
	public boolean isAltDown() {
		return isAltDown;
	}
	
	public boolean isMetaDown() {
		return isMetaDown;
	}
	
	public String getDisplayText ( ) {
		String shortcut = "";
		if ( isControlDown() ) shortcut += "Ctrl + ";
		if ( isAltDown() ) shortcut += "Alt + ";
		if ( isShiftDown() ) shortcut += "Shift + ";
		if ( isMetaDown() ) shortcut += "Meta + ";
		
		if ( keyCode != KeyCode.CONTROL && keyCode != KeyCode.ALT
		&& keyCode != KeyCode.SHIFT && keyCode != KeyCode.META 
		&& keyCode != KeyCode.ALT_GRAPH && keyCode != KeyCode.WINDOWS
		&& keyCode != KeyCode.ESCAPE ) {
			shortcut += keyCode.getName();
		}
		
		return shortcut;
	}
	
	public boolean equals ( Object compareTo ) {
		if ( !(compareTo instanceof HotkeyState) ) return false;
		
		HotkeyState compareState = (HotkeyState)compareTo;
		
		if ( !getCode().equals( compareState.getCode() ) ) return false;
		if ( isShiftDown() != compareState.isShiftDown() ) return false;
		if ( isControlDown() != compareState.isControlDown() ) return false;
		if ( isAltDown() != compareState.isAltDown() ) return false;
		if ( isMetaDown() != compareState.isMetaDown() ) return false;

		return true;
	}
}
