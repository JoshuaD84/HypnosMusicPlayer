package net.joshuad.hypnos.hotkeys;

import javafx.scene.input.KeyEvent;

public class IntellitypeHotkeyState extends HotkeyState {

	private static final long serialVersionUID = 1L;
	
	private int intellitypeCommand = -1;
	private String name;
	
	
	public IntellitypeHotkeyState ( KeyEvent event, int intellitypeCommand, String name ) {
		super ( null, event.isShiftDown(), event.isControlDown(), event.isAltDown(), event.isMetaDown() );
		this.intellitypeCommand = intellitypeCommand;
		this.name = name;
	}
	
	public int getIntellitypeCommand () {
		return intellitypeCommand;
	}
	
	@Override
	public String getDisplayText ( ) {
		String shortcut = "";
		if ( isControlDown() ) shortcut += "Ctrl + ";
		if ( isAltDown() ) shortcut += "Alt + ";
		if ( isShiftDown() ) shortcut += "Shift + ";
		if ( isMetaDown() ) shortcut += "Meta + ";
		
		shortcut += name;
		
		return shortcut;
	}
	
	public boolean equals ( Object compareTo ) {
		if ( !(compareTo instanceof IntellitypeHotkeyState) ) return false;
		
		IntellitypeHotkeyState compareState = (IntellitypeHotkeyState)compareTo;
		
		if ( getIntellitypeCommand() != compareState.getIntellitypeCommand() ) return false;
		if ( isShiftDown() != compareState.isShiftDown() ) return false;
		if ( isControlDown() != compareState.isControlDown() ) return false;
		if ( isAltDown() != compareState.isAltDown() ) return false;
		if ( isMetaDown() != compareState.isMetaDown() ) return false;

		return true;
	}
}
