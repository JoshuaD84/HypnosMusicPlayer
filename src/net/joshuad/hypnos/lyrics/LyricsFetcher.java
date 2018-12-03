package net.joshuad.hypnos.lyrics;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.lyrics.scrapers.AZScraper;
import net.joshuad.hypnos.lyrics.scrapers.AbstractScraper;
import net.joshuad.hypnos.lyrics.scrapers.GeniusScraper;
import net.joshuad.hypnos.lyrics.scrapers.MetroScraper;
import net.joshuad.hypnos.lyrics.scrapers.MusixScraper;

public class LyricsFetcher {
	@SuppressWarnings("unused")
	private static transient final Logger LOGGER = Logger.getLogger( LyricsFetcher.class.getName() );
	
	public enum LyricSite {
		NONE ( "No Source" ),
		METRO ( "Metro Lyrics" ),
		GENIUS ( "Genius Lyrics" ),
		AZ ( "AZLyrics" ),
		MUSIX ( "MusixMatch" );
		
		private String name;
		
		LyricSite ( String name ) {
			this.name = name;
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
		setParseOrder ( LyricSite.GENIUS, LyricSite.AZ, LyricSite.MUSIX, LyricSite.METRO );
		parseOrder.add ( geniusParser );
		parseOrder.add ( azParser );
		parseOrder.add ( metroParser );
		parseOrder.add ( musixParser );
	}
	
	public Lyrics get ( Track track ) {
		
		Lyrics lyrics = new Lyrics ( "", LyricSite.NONE, "", Lyrics.ScrapeError.NOT_FOUND );
		
		for ( AbstractScraper parser : parseOrder ) {
			lyrics = parser.getLyrics( track );
			if ( !lyrics.hadScrapeError() )  break;
		}
		
		if ( lyrics.hadScrapeError() ) {
			String simplifiedTrackTitle = track.getTitle().replaceAll( " ?\\(.*\\)", "" );
			
			if ( !simplifiedTrackTitle.equals( track.getTitle() ) ) {
				for ( AbstractScraper parser : parseOrder ) {
					
					lyrics = parser.getLyrics ( track.getAlbumArtist(), simplifiedTrackTitle );
					if ( !lyrics.hadScrapeError() )  break;
					
					lyrics = parser.getLyrics ( track.getArtist(), simplifiedTrackTitle );
					if ( !lyrics.hadScrapeError() )  break;
					
					lyrics = parser.getLyrics ( track.getAlbumArtist().toLowerCase().replaceFirst( "the ", "" ), simplifiedTrackTitle );
					if ( !lyrics.hadScrapeError() )  break;
					
					lyrics = parser.getLyrics ( track.getArtist().toLowerCase().replaceFirst( "the ", "" ), simplifiedTrackTitle );
					if ( !lyrics.hadScrapeError() )  break;
				}
			}
		}
	
		return lyrics;			
	}
	
	public void setParseOrder ( LyricSite ... parsers ) {
		parseOrder.clear();
		for ( LyricSite type : parsers ) {
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
				case NONE:
					//Do nothing
					break;
			}
		}
	}
	
	
	/** Testing **/
	
	private static void testAll ( String artist, String song, boolean printLyrics ) {
		MetroScraper metroParser = new MetroScraper();
		GeniusScraper geniusParser = new GeniusScraper();
		AZScraper azParser = new AZScraper();
		MusixScraper musixParser = new MusixScraper();		
		
		Lyrics metro = metroParser.getLyrics( artist, song );
		Lyrics az = azParser.getLyrics( artist, song );
		Lyrics genius = geniusParser.getLyrics( artist, song );
		Lyrics musix = musixParser.getLyrics( artist, song );
		
		if ( printLyrics ) {
			printTestResults ( metro );
			printTestResults ( az );
			printTestResults ( genius );
			printTestResults ( musix );
		}
		
		System.out.println ( artist + " - " + song );
		System.out.println ( "------------" );
		System.out.println ( "Metro: " + ( metro.hadScrapeError() ? "Fail - " + metro.getError() + " - " + metro.getSourceURL(): "Success" ) );
		System.out.println ( "AZ: " + ( az.hadScrapeError() ? "Fail - " + az.getError() + " - " + az.getSourceURL() : "Success" ) );
		System.out.println ( "Genius: " + ( genius.hadScrapeError() ? "Fail - " + genius.getError() + " - " + genius.getSourceURL(): "Success" ) );
		System.out.println ( "Musix: " + ( musix.hadScrapeError() ? "Fail - " + musix.getError() + " - " + musix.getSourceURL(): "Success" ) );
		System.out.println ( "\n" );
	}
	
	private static void printTestResults ( Lyrics results ) {
		System.out.println ( "\n\n---------------------- Begin " + results.getSite().getName() + " ------------------------" );
		System.out.println ( results.hadScrapeError() ? "No lyrics found" : results.getLyrics() );
		System.out.println ( "----------------------- End " + results.getSite().getName() + " -------------------------\n\n" );
		
		
	}
	
	public static void main ( String[] args ) {
		/*
		testAll ( "Regina Spektor", "Apres Moi", true );
		testAll ( "Regina Spektor", "Après Moi", true );
		testAll ( "Andrew Bird", "Action/Adventure", true );
		testAll ( "Björk", "Aeroplane", true );
		testAll ( "Bjork", "Aeroplane", true );
		testAll ( "311", "Rollin'", true );
		testAll ( "John Lennon", "Oh Yoko!", true );
		testAll ( "Jenny Lewis", "Aloha & The Three Johns", true );
		testAll ( "John Frusciante", "Away & Anywhere", true );
		testAll ( "John Vanderslice", "C & O Canal", true );
		testAll ( "The Roots", "Thought @ Work", true );
		testAll ( "Elliott Smith", "Waltz #1", true );
		testAll ( "Ani DiFranco", "78% H2O", true );
		testAll ( "Radiohead", "2+2=5", true );
		testAll ( "Florence + the Machine", "Bird Song", true );
		testAll ( "M. Ward", "Lullaby + Exile", true );
		testAll ( "PJ Harvey", "Is This Desire?", true );
		*/
		
		testAll ( "Decemberists", "The Hazards of Love 2", true );
		
	}
}
