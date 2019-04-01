package net.joshuad.hypnos.lyrics.scrapers;

import net.joshuad.library.Track;
import net.joshuad.hypnos.lyrics.Lyrics;

public abstract class AbstractScraper {
	
	String baseURL = "undefined";
	
	public Lyrics getLyrics ( Track track ) {
		Lyrics lyrics = null;

		lyrics = getLyrics ( track.getAlbumArtist(), track.getTitle() );
		
		if ( lyrics.hadScrapeError() ) {
			lyrics = getLyrics ( track.getArtist(), track.getTitle() );
		}
		
		return lyrics;
	}
	
	public abstract Lyrics getLyrics ( String artist, String title );
	
	public String getURL() {
		return baseURL;
	}
}
