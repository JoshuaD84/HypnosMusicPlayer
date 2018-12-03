package net.joshuad.hypnos.lyrics.scrapers;

import java.io.IOException;
import java.text.Normalizer;
import java.util.logging.Logger;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;

import net.joshuad.hypnos.lyrics.Lyrics;
import net.joshuad.hypnos.lyrics.LyricsFetcher;

public class AZScraper extends AbstractScraper {
	@SuppressWarnings("unused")
	private static transient final Logger LOGGER = Logger.getLogger( AZScraper.class.getName() );
	
	public AZScraper() {
		baseURL = "https://www.azlyrics.com/";
	}
	
	@Override
	public Lyrics getLyrics ( String artist, String song ) {
		
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
			return new Lyrics ( "", LyricsFetcher.LyricSite.AZ, url, Lyrics.ScrapeError.NOT_FOUND );
		}

		return new Lyrics ( StringEscapeUtils.unescapeHtml4 ( lyrics ), LyricsFetcher.LyricSite.AZ, url );
	}
	
	private  String makeURLReady ( String string ) {
		return Normalizer.normalize( string, Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" )
			.replaceAll ( "& ", "" ).replaceAll( "&", "" )
			.replaceAll ( "@ ", "" ).replaceAll( "@", "" )
			.replaceAll ( "# ", "" ).replaceAll( "#", "" )
			.replaceAll( "[%+=]", "" )
			.replaceAll ( "[?]", "" )
			.replaceAll( "['\"\\/,.! ]", "" ).toLowerCase();
	}
	
	public static String cleanPreserveLineBreaks ( String bodyHtml ) {
		String prettyPrintedBodyFragment = Jsoup.clean( bodyHtml, "", Whitelist.none().addTags( "br", "p" ), new Document.OutputSettings().prettyPrint( true ) );
		prettyPrintedBodyFragment = prettyPrintedBodyFragment.replaceAll ( "(?i)<br */?>", "" ).replaceAll ( "(?i)< */? *p *>", "\n\n" );
		
		return prettyPrintedBodyFragment;
	}
	
	public static void main ( String [] args ) {
		AZScraper parser = new AZScraper();
		Lyrics result = parser.getLyrics( "Rilo Kiley", "Silver Lining" );
		
		if ( result.hadScrapeError() ) {
			System.out.println ( "Error: " + result.getError() );
		} else {
			System.out.println ( result.getLyrics() );
		}
	}
}
