package net.joshuad.hypnos.lyrics.scrapers;

import java.io.IOException;
import java.text.Normalizer;
import java.util.logging.Logger;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import net.joshuad.hypnos.lyrics.Lyrics;
import net.joshuad.hypnos.lyrics.LyricsFetcher;

public class MusixScraper extends AbstractScraper {

	private static transient final Logger LOGGER = Logger.getLogger( MusixScraper.class.getName() );
	
	public MusixScraper() {
		baseURL = "https://www.musixmatch.com/";
	}
	
	@Override
	public Lyrics getLyrics ( String artist, String song ) {
		
		String artistBase = makeURLReady ( artist );
		String songBase = makeURLReady ( song );
		
		String url = baseURL + "lyrics/" + artistBase + "/" + songBase;
		
		String lyrics = null;
		
		try {
			Document doc = Jsoup.connect( url ).get();
			doc.outputSettings().prettyPrint( false );
			lyrics = doc.getElementsByClass( "mxm-lyrics__content" ).html();
			
		} catch ( IOException e ) {
			return new Lyrics ( "", LyricsFetcher.LyricSite.MUSIX, url, Lyrics.ScrapeError.NOT_FOUND );
		}
		
		if ( lyrics != null && lyrics.matches( "^Restricted Lyrics.*" ) ) {
			lyrics = null;
			return new Lyrics ( "", LyricsFetcher.LyricSite.MUSIX, url, Lyrics.ScrapeError.RESTRICTED );
		}
		
		return new Lyrics ( StringEscapeUtils.unescapeHtml4 ( lyrics ), LyricsFetcher.LyricSite.MUSIX, url );
	}
	
	private  String makeURLReady ( String string ) {
		return Normalizer.normalize( string, Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" )
			.replaceAll ( "& ", "" ).replaceAll( "&", "" )
			.replaceAll ( "@ ", "" ).replaceAll( "@", "" )
			.replaceAll ( "[#%]", "" )
			.replaceAll ( "[+] ?", "" )
			.replaceAll ( "[?]", "" )
			.replaceAll( "['\",.!]", "" ).replaceAll( "[\\/+= ]", "-" ).toLowerCase();
	}
	
	public static void main ( String [] args ) {
		MusixScraper parser = new MusixScraper();
		Lyrics result = parser.getLyrics( "Sufjan Stevens", "Impossible Soul" );
		
		if ( result.hadScrapeError() ) {
			System.out.println ( "Error: " + result.getError() );
		} else {
			System.out.println ( result.getLyrics() );
		}
	}
}
