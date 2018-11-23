package net.joshuad.hypnos.hotkeys;

public class HotkeyException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	private String message;
	
	public HotkeyException ( String message ) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
}
