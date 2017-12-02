package net.joshuad.hypnos.lyrics.parsers;

import java.io.IOException;
import java.text.Normalizer;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;

import net.joshuad.hypnos.Track;

public class AZScraper extends AbstractScraper {
	private static transient final Logger LOGGER = Logger.getLogger( AZScraper.class.getName() );
	
	public AZScraper() {
		baseURL = "https://www.azlyrics.com/";
	}
	
	@Override
	public String getLyrics ( Track track ) {
		String lyrics = null;

		lyrics = getLyrics ( track.getAlbumArtist(), track.getTitle() );
		
		if ( lyrics == null ) {
			lyrics = getLyrics ( track.getArtist(), track.getTitle() );
		}
		
		return lyrics;
	}
	
	public String getLyrics ( String artist, String song ) {
		
		String artistBase = makeURLReady ( artist );
		String songBase = makeURLReady ( song );
		
		String url = baseURL + "lyrics/" + artistBase + "/" + songBase + ".html";
		
		String lyrics = null;
		
		try {
			Document doc = Jsoup.connect( url ).get();
			
			//We can't select the lyrics div directly since it has no id or class.
			//But it's the first div after the "div class=ringtone"
			//Thankfully, jsoup can do exactly that selection
			Element verses = doc.select( ".ringtone ~ div" ).get( 0 );
			
			lyrics = cleanPreserveLineBreaks ( verses.html() ).replaceAll( "\n +", "\n" ).replaceAll( "^\\s*", "" );
			
		} catch ( IOException e ) {
			LOGGER.info( "Unable to find lyrics for: " + artist + " - " + song );
		}
		
		return lyrics;
	}
	
	private  String makeURLReady ( String string ) {
		return Normalizer.normalize( string, Normalizer.Form.NFD ).replaceAll( "['\"\\/,. ]", "" ).toLowerCase();
	}
	
	public static String cleanPreserveLineBreaks ( String bodyHtml ) {
		String prettyPrintedBodyFragment = Jsoup.clean( bodyHtml, "", Whitelist.none().addTags( "br", "p" ), new Document.OutputSettings().prettyPrint( true ) );
		prettyPrintedBodyFragment = prettyPrintedBodyFragment.replaceAll ( "(?i)<br */?>", "" ).replaceAll ( "(?i)< */? *p *>", "\n\n" );
		
		return prettyPrintedBodyFragment;
	}
	
	public static void main ( String [] args ) {
		AZScraper parser = new AZScraper();
		String result = parser.getLyrics( "Regina Spektor", "Apres Moi" );
		System.out.println ( result );
	}
}
