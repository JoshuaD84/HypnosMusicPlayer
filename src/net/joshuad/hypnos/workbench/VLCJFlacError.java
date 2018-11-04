package net.joshuad.hypnos.workbench;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.NativeLibrary;

import uk.co.caprica.vlcj.component.AudioMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaResourceLocator;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

public class VLCJFlacError {
	
	
	/*
		"D:\\test\\o.flac",   //works
		"D:\\test\\ö.flac",   //does not work
		"test\\o.flac",       //works
		"test\\ö.flac",       //does not work
		"test/ö.flac",        //works
		"ö.flac",             //works
		"../ö.flac",          //works
		"../test/ö.flac"      //works
		
	*/
	
	final static String vlcLibPath = "stage/lib/win/vlc";
	
	public static void main( String[] args ) throws Exception {
		
		String targetFile = "D:/test/ö.flac";
		
		String nativeVLCLibPath = Paths.get( vlcLibPath ).toAbsolutePath().toString();
		
		NativeLibrary.addSearchPath( RuntimeUtil.getLibVlcLibraryName(), nativeVLCLibPath );
		AudioMediaPlayerComponent vlcComponent = new AudioMediaPlayerComponent();
		MediaPlayer mediaPlayer = vlcComponent.getMediaPlayer();
								
		//Path root = Paths.get( System.getProperty("user.dir") );
		//String playMe = root.relativize( Paths.get( "D:\\test\\ö.flac" ) ).toString().replaceAll( "\\\\", "/" );
	
		String encoded = encode( targetFile );
		System.out.println ( targetFile + " -> " + encoded );
		mediaPlayer.playMedia( encoded );
		
		while ( true ) {
			Thread.sleep ( 50 );
		}
	}
	

}
