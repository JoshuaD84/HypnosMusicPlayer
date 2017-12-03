package net.joshuad.hypnos.lyrics;

import java.util.logging.Logger;

import net.joshuad.hypnos.lyrics.LyricsFetcher.LyricSite;

public class Lyrics {
	private static transient final Logger LOGGER = Logger.getLogger( Lyrics.class.getName() );
	
	public enum ScrapeError {
		NONE,
		NOT_FOUND,
		NOT_AVAILABLE,
		RESTRICTED
	}

	private String lyrics;
	private ScrapeError error;
	private LyricSite site;
	private String sourceURL;
	
	public Lyrics ( String lyrics, LyricSite site, String sourceURL ) {
		this ( lyrics, site, sourceURL, ScrapeError.NONE );
	}

	public Lyrics ( String lyrics, LyricSite site, String sourceURL, ScrapeError error ) {
		if ( lyrics == null ) lyrics = "";
		this.lyrics = lyrics;
		this.error = error;
		this.site = site;
		this.sourceURL = sourceURL;
	}
	
	public String getLyrics() {
		if ( error != ScrapeError.NONE ) {
			LOGGER.warning ( "Asked for lyrics when there was a scrape error." );
		}
		return lyrics;
	}
	
	public ScrapeError getError() {
		return error;
	}
	
	public boolean hadScrapeError() {
		return error != ScrapeError.NONE;
	}
	
	public String getSourceURL() {
		return sourceURL;
	}
	
	public LyricSite getSite() {
		return site;
	}
}