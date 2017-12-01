package net.joshuad.hypnos.lyrics;

import java.io.IOException;
import java.text.Normalizer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import net.joshuad.hypnos.Track;

public class MetroParser {
	
	private String root = "http://www.metrolyrics.com/";
	
	public MetroParser() {}
	
	public String getLyrics ( Track track ) {

		String lyrics = getLyrics ( track.getArtist(), track.getTitle() );
		
		if ( lyrics == null ) {
			lyrics = getLyrics ( track.getAlbumArtist(), track.getTitle() );
		}
		
		return lyrics;
	}
	
	public String getLyrics ( String artist, String song ) {
		
		String artistBase = makeURLReady ( artist );
		String songBase = makeURLReady ( song );
		
		String url = root + songBase + "-lyrics-" + artistBase + ".html";
		
		String lyrics = null;
		
		try {
			Document doc = Jsoup.connect( url ).get();
			Elements verses = doc.getElementsByClass( "verse" );
			lyrics = cleanPreserveLineBreaks ( verses.html() ).replaceAll( "\n ", "\n" );
			
		} catch ( IOException e ) {
			//TODO: logging? 
		}
		
		return lyrics;
	}
	
	private  String makeURLReady ( String string ) {
		return Normalizer.normalize( string, Normalizer.Form.NFD ).replaceAll( " ", "-" ).replaceAll( "['\",.]", "" ).toLowerCase();
	}
	
	public static String cleanPreserveLineBreaks ( String bodyHtml ) {
		String prettyPrintedBodyFragment = Jsoup.clean( bodyHtml, "", Whitelist.none().addTags( "br", "p" ), new Document.OutputSettings().prettyPrint( true ) );
		return Jsoup.clean( prettyPrintedBodyFragment, "", Whitelist.none(), new Document.OutputSettings().prettyPrint( false ) );
	}
	
	
	public static void main ( String [] args ) {
		MetroParser parser = new MetroParser();
		String result = parser.getLyrics( "keane", "time to go" );
		System.out.println ( result );
	}
}
