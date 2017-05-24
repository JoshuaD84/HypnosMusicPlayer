package org.joshuad.musicplayer;

import org.jaudiotagger.tag.FieldKey;

public class MultiFileTagPair {
		
	private FieldKey key;
	private String value;
	private boolean multiValue = false;
	
	public MultiFileTagPair ( FieldKey key, String value ) {
		this.key = key;
		this.value = value;
	}
	
	public void setValue ( String value ) {
		this.value = value;
		multiValue = false;
	}
	
	public void anotherFileValue ( String newValue ) {
		if ( ! newValue.equals( value ) ) {
			multiValue = true;
		}
	}
	
	public boolean isMultiValue () {
		return multiValue;
	}
	
	public String getTagName () {
		return key.toString();
	}
	
	public FieldKey getKey() {
		return key;
	}
	
	public String getValue () {
		if ( multiValue ) {
			return "<Multiple Values>";
		} else {
			return value;
		}
	}
}