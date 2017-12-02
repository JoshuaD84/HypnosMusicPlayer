package net.joshuad.hypnos.lyrics.parsers;

import java.io.IOException;
import java.text.Normalizer;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import net.joshuad.hypnos.Track;

public class MetroScraper extends AbstractScraper {

	private static transient final Logger LOGGER = Logger.getLogger( MetroScraper.class.getName() );
	
	public MetroScraper() {
		baseURL = "http://www.metrolyrics.com/";
	}
	
	@Override
	public String getLyrics ( Track track ) {

		String lyrics = getLyrics ( track.getAlbumArtist(), track.getTitle() );
		
		if ( lyrics == null ) {
			lyrics = getLyrics ( track.getArtist(), track.getTitle() );
		}
		
		return lyrics;
	}
	
	public String getLyrics ( String artist, String song ) {
		
		String artistBase = makeURLReady ( artist );
		String songBase = makeURLReady ( song );
		
		String url = baseURL + songBase + "-lyrics-" + artistBase + ".html";
		
		String lyrics = null;
		
		try {
			Document doc = Jsoup.connect( url ).get();
			Elements verses = doc.getElementsByClass( "verse" );
		
			lyrics = verses.html().replaceAll("\n", "\n\n").replaceAll("<br> ", "\n");

		} catch ( IOException e ) {
			LOGGER.info( "Unable to find lyrics for: " + artist + " - " + song );
		}
		if ( lyrics.isEmpty() ) {
			lyrics = null;
			LOGGER.info( "Unable to find lyrics for: " + artist + " - " + song );
		}
		
		return lyrics;
	}
	
	private  String makeURLReady ( String string ) {
		//TODO: Unfortunately it doesn't appear that MetroScraper has a standard for handling "/".
		//Sometimes it replaces it with - and sometims with an empty string
		//See Bon Iver - Beth/Rest and Bright Eyes - Easy/Lucky/Free
		return Normalizer.normalize( string, Normalizer.Form.NFD ).replaceAll( "[\\/'\",.-]", "" ).replaceAll( "[ ]", "-" ).toLowerCase();
	}
	
	public static void main ( String [] args ) {
		MetroScraper parser = new MetroScraper();
		String result = parser.getLyrics( "Regina Spektor", "Après Moi" );
		System.out.println ( "result:\n" + result );
	}
}
