package net.joshuad.hypnos.lastfm;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Result;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;
import de.umass.util.StringUtilities;

public class LastFM {

	private String key = "accf97169875efd9a189520ee542c1f6";
	private String secret = "534851bcd3b6441dc91c9c970c766666";
	private String username = "";
	private String password = "";
	
	private Session session;
	
	private boolean triedConnectWithTheseCredentials = true;
	
	StringBuffer log = new StringBuffer();
	
	SimpleDateFormat timeStampFormat = new SimpleDateFormat( "yyyy/MM/dd hh:mm:ss aaa" );
	
	public LastFM() {
		Logger logger = Logger.getLogger( Caller.class.getPackage().getName() );
		logger.setLevel( Level.OFF );
		Caller.getInstance().setUserAgent( "hypnos-music-player" );
	}
	
	public void setPassword ( String password ) {
		if ( password == null ) {
			password = "";
		} else if ( !StringUtilities.isMD5 ( password ) ) {
			password = StringUtilities.md5 ( password );
		}
		
		this.password = password;
		triedConnectWithTheseCredentials = false;
	}
	
	public void setUsername ( String username ) {
		if ( username == null ) {
			username = "";
		}
		
		this.username = username;
		triedConnectWithTheseCredentials = false;
	}
	
	public void setCredentials( String username, String password ) {
		setUsername ( username );
		setPassword ( password );
	}
	
	public void connect () {
		session = Authenticator.getMobileSession( username, password, key, secret );
		
		if ( session == null ) {
			log.append ( "Unable to connect to lastfm. Check your username, password, and internet connection.\n" );
		} else {
			log.append ( "Successfully connected to lastfm.\n" );
		}
		
		triedConnectWithTheseCredentials = true;
	}

	public void scrobbleTrack( String artist, String title ) {
		
		if ( !triedConnectWithTheseCredentials ) connect();
		
		String timeStamp = timeStampFormat.format( new Date ( System.currentTimeMillis() ) );
		
		if ( session == null ) {
			log.append( "[" + timeStamp + "] Invalid session, unable to scrobble: " + artist + " - " + title + "\n" );
			return;
		} 
					
		log.append( "[" + timeStamp + "] Attempting to Scrobble: " + artist + " - " + title + " ... " );
		
		int now = (int) (System.currentTimeMillis() / 1000);
		ScrobbleResult result = Track.scrobble( artist, title, now, session );

		boolean success = result.isSuccessful() && !result.isIgnored();
		log.append ( success ? "success!" : "failed." );
		log.append ( "\n" );
		
		return;
	}
	
	public void loveTrack( String artist, String title ) {
		if ( !triedConnectWithTheseCredentials ) connect();
		String timeStamp = timeStampFormat.format( new Date ( System.currentTimeMillis() ) );
		
		if ( session == null ) {
			log.append( "[" + timeStamp + "] Invalid session, unable to love: " + artist + " - " + title + "\n" );
			return;
		} 
		
		log.append( "[" + timeStamp + "] Attempting to Love: " + artist + " - " + title + " ... " );
		
		Result result = Track.love( artist, title, session );

		boolean success = result.isSuccessful();
		
		log.append ( success ? "success!" : "failed - " + result.getErrorMessage() );
		log.append ( "\n" );
	}
	
	public void unloveTrack( String artist, String title ) {
		if ( !triedConnectWithTheseCredentials ) connect();
		String timeStamp = timeStampFormat.format( new Date ( System.currentTimeMillis() ) );
		
		if ( session == null ) {
			log.append( "[" + timeStamp + "] Invalid session, unable to unlove: " + artist + " - " + title + "\n" );
			return;
		} 
		
		log.append( "[" + timeStamp + "] Attempting to Unlove: " + artist + " - " + title + " ... " );
		
		Result result = Track.unlove( artist, title, session );

		boolean success = result.isSuccessful();
		
		log.append ( success ? "success!" : "failed - " + result.getErrorMessage() );
		log.append ( "\n" );
	}
	
	public void loveTrack ( net.joshuad.hypnos.Track track ) {
		String timeStamp = timeStampFormat.format( new Date ( System.currentTimeMillis() ) );
		if ( track == null ) {
			log.append ( "[" + timeStamp + "] Asked to love a null track, ignoring.\n" );
		} else {
			loveTrack( track.getArtist(), track.getTitle() );
		}
	}
	
	public void unloveTrack ( net.joshuad.hypnos.Track track ) {
		String timeStamp = timeStampFormat.format( new Date ( System.currentTimeMillis() ) );
		if ( track == null ) {
			log.append ( "[" + timeStamp + "] Asked to unlove a null track, ignoring.\n" );
		} else {
			unloveTrack( track.getArtist(), track.getTitle() );
		}
	}
	
	public void scrobbleTrack ( net.joshuad.hypnos.Track track ) {
		String timeStamp = timeStampFormat.format( new Date ( System.currentTimeMillis() ) );
		if ( track == null ) {
			log.append ( "[" + timeStamp + "] Asked to scrobble a null track, ignoring.\n" );
		} else {
			scrobbleTrack( track.getArtist(), track.getTitle() );
		}
	}	

	public StringBuffer getLog () {
		return log;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPasswordMD5() {
		return password;
	}
	
	
	public static void main( String[] args ) {
		LastFM lastFM = new LastFM();
		lastFM.setCredentials( "HypnosTest", "ADD ME WHEN TESTING" );
		lastFM.scrobbleTrack( "Bowerbirds", "Hooves" );
		lastFM.scrobbleTrack( "Sufjan Stevens", "All of Me Wants All Of You" );
		
		System.out.println( lastFM.log.toString() );
	}

}
