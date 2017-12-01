package net.joshuad.hypnos.lyrics;

import net.joshuad.hypnos.Track;

public class Lyrics {
	public static String get ( Track track ) {
		return new MetroParser().getLyrics( track );
	}
}
