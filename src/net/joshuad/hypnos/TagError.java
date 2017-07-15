package net.joshuad.hypnos;
import java.nio.file.Path;

public class TagError {

	public enum Severity {
		MINOR ( "*" ),
		MAJOR ( "**" ),
		FATAL ( "***" );
		
		private String display;
		
		Severity ( String display ) { this.display = display; }
		public String getDisplay () { return display; }
	}
	
	private Path path;
	private String message;
	private Severity severity;
	
	public TagError ( Path path, String message, Severity severity ) {
		this.path = path;
		this.message = message;
		this.severity = severity;
	}
	
	public Path getPath() {
		return path;
	}
	
	public String getPathDisplay() {
		return path.toString();
	}
	
	public String getMessage() {
		return message;
	}
	
	public String getSeverityDisplay () {
		return severity.getDisplay();
	}
}
