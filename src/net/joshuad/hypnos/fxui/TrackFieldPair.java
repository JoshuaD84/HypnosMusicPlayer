package net.joshuad.hypnos.fxui;

public class TrackFieldPair {
	
	String label;
	Object value;
	
	public TrackFieldPair ( String label, Object value ) {
		this.label = label;
		this.value = value;
	}
	
	public String getLabel() {
		return label;
	}
	
	public Object getValue() {
		return value;
	}
}