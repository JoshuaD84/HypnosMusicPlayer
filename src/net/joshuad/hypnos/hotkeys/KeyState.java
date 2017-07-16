package net.joshuad.hypnos.hotkeys;

import java.io.Serializable;

public class KeyState implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private int keyCode;
	private int modifiers;
	private String display = "";
	
	public KeyState ( int keyCode, int modifiers ) {
		this.keyCode = keyCode;
		this.modifiers = modifiers;
	}
		
	public int getKeyCode() {
		return keyCode;
	}
	
	public int getModifers() {
		return modifiers;
	}
	
	public void setDisplay ( String display ) {
		this.display = display;
	}
	
	public String getDisplay () {
		return display;
	}
	
	public boolean equals ( Object object ) {
		if ( object == null ) return false;
		if ( !( object instanceof KeyState ) ) return false;
		
		KeyState compareMe = (KeyState)object;
		
		return ( keyCode == compareMe.getKeyCode() && modifiers == compareMe.getModifers() );
	}
}