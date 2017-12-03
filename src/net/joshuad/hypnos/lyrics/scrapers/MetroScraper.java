package net.joshuad.hypnos.lyrics.scrapers;

import java.io.IOException;
import java.text.Normalizer;
import java.util.logging.Logger;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import net.joshuad.hypnos.lyrics.Lyrics;
import net.joshuad.hypnos.lyrics.LyricsFetcher;

public class MetroScraper extends AbstractScraper {

	private static transient final Logger LOGGER = Logger.getLogger( MetroScraper.class.getName() );
	
	public MetroScraper() {
		baseURL = "http://www.metrolyrics.com/";
	}
	
	@Override
	public Lyrics getLyrics ( String artist, String song ) {
		
		String artistBase = makeURLReady ( artist );
		String songBase = makeURLReady ( song );
		
		String url = baseURL + songBase + "-lyrics-" + artistBase + ".html";
		
		String lyrics = null;
		
		try {
			Document doc = Jsoup.connect( url ).get();

			Elements message = doc.getElementsByClass( "lyric-message" );
			if ( message.html().matches( "^Unfortunately.*" ) ) {
				return new Lyrics ( "", LyricsFetcher.LyricSite.METRO, url, Lyrics.ScrapeError.RESTRICTED );
			}
			
			Elements verses = doc.getElementsByClass( "verse" );
			lyrics = verses.html().replaceAll("\n", "\n\n").replaceAll("<br> ?", "\n");

		} catch ( IOException e ) {
			return new Lyrics ( "", LyricsFetcher.LyricSite.METRO, url, Lyrics.ScrapeError.NOT_FOUND );
		}
		
		if ( lyrics != null && lyrics.isEmpty() ) {
			lyrics = null;
			return new Lyrics ( "", LyricsFetcher.LyricSite.METRO, url, Lyrics.ScrapeError.NOT_AVAILABLE );
		}
		
		return new Lyrics ( StringEscapeUtils.unescapeHtml4 ( lyrics ), LyricsFetcher.LyricSite.METRO, url );
	}
	
	private  String makeURLReady ( String string ) {
		//TODO: Unfortunately it doesn't appear that MetroScraper has a standard for handling "/".
		//Sometimes it replaces it with - and sometims with an empty string
		//See Bon Iver - Beth/Rest and Bright Eyes - Easy/Lucky/Free
		return Normalizer.normalize( string, Normalizer.Form.NFD ).replaceAll( "[^\\p{ASCII}]", "" )
			.replaceAll ( "& ?", "" )
			.replaceAll ( "@ ?", "" )
			.replaceAll ( "[#%]", "" )
			.replaceAll ( "[+=] ?", "" )
			.replaceAll ( "[?]", "" )
			.replaceAll( "['\",.!-]", "" ).replaceAll( "[\\/ ]", "-" ).toLowerCase();
	}
	
	private static void test ( String artist, String song ) {
		MetroScraper parser = new MetroScraper();
		
		Lyrics result = parser.getLyrics( artist, song );
		
		if ( result.hadScrapeError() ) {
			System.out.println ( "Error - " + result.getError() );
			System.out.println ( "---------------" );
			System.out.println ( "url: " + result.getSourceURL() );
			System.out.println ( "\n" );
		} else {
			System.out.println ( "Success" );
			System.out.println ( "---------------" );
			System.out.println ( result.getLyrics() );
			System.out.println ( "\n" );
		}
	}
	
	public static void main ( String [] args ) {
		//test ( "Björk", "Aeroplane" );
		test ( "Andrew Bird", "Action/Adventure" );
		//test ( "Bon Iver", "Beth/Rest" );
		//test ( "Bright Eyes", "Easy/Lucky/Free" );
		//test ( "Bjork", "Aeroplane" );	
		//System.out.println ( Normalizer.normalize( "Björk", Normalizer.Form.NFD ) );
	}
}
