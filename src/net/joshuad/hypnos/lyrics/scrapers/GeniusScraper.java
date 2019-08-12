package net.joshuad.hypnos.lyrics.scrapers;

import java.io.IOException;
import java.text.Normalizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import net.joshuad.hypnos.lyrics.Lyrics;
import net.joshuad.hypnos.lyrics.LyricsFetcher;

public class GeniusScraper extends AbstractScraper {
	@SuppressWarnings("unused")
	private static transient final Logger LOGGER = Logger.getLogger(GeniusScraper.class.getName());

	public GeniusScraper() {
		baseURL = "http://www.genius.com/";
	}

	@Override
	public Lyrics getLyrics(String artist, String song) {
		String artistBase = makeURLReady(artist);
		String songBase = makeURLReady(song);
		String url = baseURL + artistBase + "-" + songBase + "-lyrics";
		String lyrics = null;
		try {
			Document doc = Jsoup.connect(url).get();
			Elements verses = doc.getElementsByClass("lyrics");
			lyrics = cleanPreserveLineBreaks(verses.html()).replaceAll("\n +", "\n").replaceAll("^\\s*", "");
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Exception", e); //TODO: DD
			return new Lyrics("", LyricsFetcher.LyricSite.GENIUS, url, Lyrics.ScrapeError.NOT_FOUND);
		}
		return new Lyrics(StringEscapeUtils.unescapeHtml4(lyrics), LyricsFetcher.LyricSite.GENIUS, url);
	}

	private String makeURLReady(String string) {
		return Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "").replaceAll("&", "and")
				.replaceAll("@", "at").replaceAll("[#%]", "").replaceAll("[+] ?", "").replaceAll("[?]", "")
				.replaceAll("['\",.!]", "").replaceAll("[\\/= ]", "-").toLowerCase();
	}

	private static String cleanPreserveLineBreaks(String bodyHtml) {
		String prettyPrintedBodyFragment = Jsoup.clean(bodyHtml, "", Whitelist.none().addTags("br", "p"),
				new Document.OutputSettings().prettyPrint(true));
		prettyPrintedBodyFragment = prettyPrintedBodyFragment.replaceAll("(?i)<br */?>", "\n").replaceAll("(?i)< */? *p *>",
				"\n\n");
		return prettyPrintedBodyFragment;
	}

	public static void main(String[] args) {
		GeniusScraper parser = new GeniusScraper();
		Lyrics result = parser.getLyrics("Sufjan Stevens", "Impossible Soul");
		if (result.hadScrapeError()) {
			System.out.println("Error: " + result.getError());
		} else {
			System.out.println(result.getLyrics());
		}
	}
}
