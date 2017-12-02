package net.joshuad.hypnos.lyrics.parsers;

import java.io.IOException;
import java.text.Normalizer;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import net.joshuad.hypnos.Track;

public class GeniusScraper extends AbstractScraper {
	private static transient final Logger LOGGER = Logger.getLogger( GeniusScraper.class.getName() );
	
	public GeniusScraper() {
		baseURL = "http://www.genius.com/";
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
		
		String url = baseURL + artistBase + "-" + songBase + "-lyrics";
		
		String lyrics = null;
		
		try {
			Document doc = Jsoup.connect( url ).get();
			Elements verses = doc.getElementsByClass( "lyrics" );
			lyrics = cleanPreserveLineBreaks ( verses.html() ).replaceAll( "\n +", "\n" ).replaceAll( "^\\s*", "" );
			
		} catch ( IOException e ) {
			LOGGER.info( "Unable to find lyrics for: " + artist + " - " + song );
		}
		
		return lyrics;
	}
	
	private  String makeURLReady ( String string ) {
		return Normalizer.normalize( string, Normalizer.Form.NFD ).replaceAll( "['\",.]", "" ).replaceAll( "[\\/ ]", "-" ).toLowerCase();
	}
	
	public static String cleanPreserveLineBreaks ( String bodyHtml ) {
		String prettyPrintedBodyFragment = Jsoup.clean( bodyHtml, "", Whitelist.none().addTags( "br", "p" ), new Document.OutputSettings().prettyPrint( true ) );
		prettyPrintedBodyFragment = prettyPrintedBodyFragment.replaceAll ( "(?i)<br */?>", "\n" ).replaceAll ( "(?i)< */? *p *>", "\n\n" );
		
		return prettyPrintedBodyFragment;
	}
	
	
	public static void main ( String [] args ) {
		GeniusScraper parser = new GeniusScraper();
		String result = parser.getLyrics( "Bright Eyes", "Easy/Lucky/Free" );
		System.out.println ( result );
	}
}
