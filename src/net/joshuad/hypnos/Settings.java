package net.joshuad.hypnos;

public class Settings {
	
	public enum Hotkey {
		PREVIOUS ( "Previous" ), 
		NEXT ( "Next" ), 
		PLAY ( "Play" ), 
		TOGGLE_PAUSE ( "Pause / Unpause" ), 
		STOP ( "Stop" );
		
		private String label;
		Hotkey ( String label ) { this.label = label; }
		public String getLabel () { return label; }
	}

}
