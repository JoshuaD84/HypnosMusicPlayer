package net.joshuad.hypnos.test;

import java.nio.file.Path;
import java.nio.file.Paths;

import net.joshuad.hypnos.Utils;

public class FuzzyTest {
	
	public static void main ( String [] args ) {
		evaluate ( "/d/music/Air/" );
		evaluate ( "/d/download/" );
		evaluate ( "/d/music/Air/1998 - Moon Safari/" );
		evaluate ( "/home/joshua/Desktop/almost/" );
		evaluate ( "/d/music-hard-to-sort/misc/" );
		evaluate ( "/d/music-hard-to-sort/Jon Rizzo/" );
		evaluate ( "/d/music/John Frusciante/2009 - The Empyrean" );
		evaluate ( "/d/music/John Frusciante/" );
		evaluate ( "/d/music/Sigur Rós/2002 - ( )/" );
		
		//System.out.println ( "() == () ? " + FuzzySearch.weightedRatio( "()", "()" ) );
	}
	
	public static void evaluate ( String location ) {
		Path path = Paths.get ( location );
		
		long startTime = System.currentTimeMillis();
		//boolean isArtistDirectory = Utils.isArtistDirectory( path );
		long artistCalcTime = System.currentTimeMillis() - startTime;
		
		startTime = System.currentTimeMillis();
		boolean isAlbumDirectory = Utils.isAlbumDirectory( path );
		long albumCalcTime = System.currentTimeMillis() - startTime;

		System.out.println ( location );
		//System.out.println ( "\tArtist Directory: " + isArtistDirectory + " (" + artistCalcTime + ")" );
		System.out.println ( "\tAlbum Directory: " + isAlbumDirectory + " (" + albumCalcTime + ")" );
		
		System.out.println ();
	}

}
