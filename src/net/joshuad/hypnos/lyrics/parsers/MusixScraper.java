package net.joshuad.hypnos.lyrics.parsers;

import java.io.IOException;
import java.text.Normalizer;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import net.joshuad.hypnos.Track;

public class MusixScraper extends AbstractScraper {

	private static transient final Logger LOGGER = Logger.getLogger( MusixScraper.class.getName() );
	
	public MusixScraper() {
		baseURL = "https://www.musixmatch.com/";
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
		
		String url = baseURL + "lyrics/" + artistBase + "/" + songBase;
		
		String lyrics = null;
		
		try {
			Document doc = Jsoup.connect( url ).get();
			Elements verses = doc.getElementsByClass( "mxm-lyrics" );
			lyrics = cleanPreserveLineBreaks ( verses.html() ).replaceAll( "\n ", "\n" );
			
		} catch ( IOException e ) {
			LOGGER.info( "Unable to find lyrics for: " + artist + " - " + song );
		}
		
		if ( lyrics.matches( "^Restricted Lyrics.*" ) ) {
			lyrics = null;
		}
		
		return lyrics;
	}
	
	private  String makeURLReady ( String string ) {
		return Normalizer.normalize( string, Normalizer.Form.NFD ).replaceAll( "['\",.]", "" ).replaceAll( "[\\/ ]", "-" ).toLowerCase();
	}
	
	public static String cleanPreserveLineBreaks ( String bodyHtml ) {
		String prettyPrintedBodyFragment = Jsoup.clean( bodyHtml, "", Whitelist.none().addTags( "br", "p" ), new Document.OutputSettings().prettyPrint( true ) );
		return Jsoup.clean( prettyPrintedBodyFragment, "", Whitelist.none(), new Document.OutputSettings().prettyPrint( false ) );
	}
	
	
	public static void main ( String [] args ) {
		MusixScraper parser = new MusixScraper();
		String result = parser.getLyrics( "Andrew Bird", "Action Adventure" );
		System.out.println ( result );
	}
}
