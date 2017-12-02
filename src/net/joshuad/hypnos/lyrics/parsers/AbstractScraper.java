package net.joshuad.hypnos.lyrics.parsers;

import net.joshuad.hypnos.Track;

public abstract class AbstractScraper {
	
	static String baseURL = "undefined";
	
	public abstract String getLyrics ( Track track );
	
	public static String getURL() {
		return baseURL;
	}

}
