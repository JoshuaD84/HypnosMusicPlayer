package net.joshuad.hypnos;

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
		String retMe = key.toString();
		
		if ( key == FieldKey.TRACK ) retMe = "TRACK NUMBER";
		else if ( key == FieldKey.DISC_NO ) retMe = "DISC NUMBER";
		else if ( key == FieldKey.MUSICBRAINZ_RELEASE_TYPE ) retMe = "RELEASE TYPE";
		
		return retMe.replaceAll( "_", " " );
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