package net.joshuad.hypnos.lyrics;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.lyrics.parsers.AZScraper;
import net.joshuad.hypnos.lyrics.parsers.AbstractScraper;
import net.joshuad.hypnos.lyrics.parsers.GeniusScraper;
import net.joshuad.hypnos.lyrics.parsers.MetroScraper;
import net.joshuad.hypnos.lyrics.parsers.MusixScraper;

public class LyricsFetcher {
	private static transient final Logger LOGGER = Logger.getLogger( LyricsFetcher.class.getName() );
	
	public enum ScraperType {
		METRO ( "Metro Lyrics", MetroScraper.getURL() ),
		GENIUS ( "Genius Lyrics", GeniusScraper.getURL() ),
		AZ ( "AZLyrics", AZScraper.getURL() ),
		MUSIX ( "MusixMatch", MusixScraper.getURL() );
		
		private String baseURL;
		private String name;
		
		ScraperType ( String name, String baseURL ) {
			this.name = name;
			this.baseURL = baseURL;
		}
		
		public String getURL() {
			return baseURL;
		}
		
		public String getName() {
			return name;
		}
	}
	
	private MetroScraper metroParser = new MetroScraper();
	private GeniusScraper geniusParser = new GeniusScraper();
	private AZScraper azParser = new AZScraper();
	private MusixScraper musixParser = new MusixScraper();
	
	private List <AbstractScraper> parseOrder = new ArrayList <AbstractScraper> ();
	
	public LyricsFetcher () {
		parseOrder.add ( geniusParser );
		parseOrder.add ( metroParser );
		parseOrder.add ( azParser );
		parseOrder.add ( musixParser );
	}
	
	public String get ( Track track ) {
		
		String lyrics = null;
		
		for ( AbstractScraper parser : parseOrder ) {
			lyrics = parser.getLyrics( track );
			if ( lyrics != null )  break;
		}
		
		return lyrics;			
	}
	
	public void setParseOrder ( ScraperType ... parsers ) {
		parseOrder.clear();
		for ( ScraperType type : parsers ) {
			switch ( type ) {
				case AZ:
					parseOrder.add ( azParser );
					break;
				case GENIUS:
					parseOrder.add ( geniusParser );
					break;
				case METRO:
					parseOrder.add ( metroParser );
					break;
				case MUSIX:
					parseOrder.add ( musixParser );
					break;
			}
		}
	}
}
