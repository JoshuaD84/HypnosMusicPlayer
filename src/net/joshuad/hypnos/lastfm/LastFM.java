package net.joshuad.hypnos.lastfm;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Result;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.User;
import de.umass.lastfm.scrobble.ScrobbleResult;
import de.umass.util.StringUtilities;
import net.joshuad.hypnos.Hypnos;

public class LastFM {
	
	public enum LovedState { TRUE, FALSE, NOT_SET, CANNOT_GET_DATA };
	
	private String key = "accf97169875efd9a189520ee542c1f6";
	private String secret = "534851bcd3b6441dc91c9c970c766666";
	private String username = "";
	private String password = "";
	
	private Session session;
	
	private boolean triedConnectWithTheseCredentials = true;
	
	private boolean notifiedUserOfFailedAttempt = false;
	
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
	
	public void disconnectAndForgetCredentials() {

		setCredentials ( "", "" );
		if ( session == null ) {
			log.append ( "Not connected. Credentials forgotten.\n" );
		} else {
			session = null;
			log.append ( "Disconnected and credentials forgotten.\n" );
		}
	}
	
	//This should be called after any attempted write to lastfm
	public void notifyUserIfNeeded( boolean success ) {
		if ( success ) {
			notifiedUserOfFailedAttempt = false;
			
		} else if ( !success && !notifiedUserOfFailedAttempt ) {
			Hypnos.getUI().notifyUserError( 
				"Unable to connect to lastfm.\n\n" +
				"Check your internet connection, username, and password.\n\n" + 
				"(This popup won't appear again this session.)" );
			notifiedUserOfFailedAttempt  = true;
		}
	}

	public void scrobbleTrack ( net.joshuad.library.Track track ) {
		String timeStamp = timeStampFormat.format( new Date ( System.currentTimeMillis() ) );
		
		if ( track == null ) {
			log.append ( "[" + timeStamp + "] Asked to scrobble a null track, ignoring.\n" );
			return;
		}
		
		String artist = track.getArtist();
		String title = track.getTitle();
		
		if ( !triedConnectWithTheseCredentials ) connect();
		
		if ( session == null ) {
			log.append( "[" + timeStamp + "] Invalid session, unable to scrobble: " + artist + " - " + title + "\n" );
			notifyUserIfNeeded( false );
			return;
		} 
					
		log.append( "[" + timeStamp + "] Attempting to Scrobble: " + artist + " - " + title + " ... " );
		
		int now = (int) (System.currentTimeMillis() / 1000);
		ScrobbleResult result = Track.scrobble( artist, title, now, session );

		boolean success = result.isSuccessful() && !result.isIgnored();
		log.append ( success ? "success!" : "failed." );
		log.append ( "\n" );
		
		notifyUserIfNeeded( success );
		
		return;
	}	
	
	public void loveTrack ( net.joshuad.library.Track track ) {
		String timeStamp = timeStampFormat.format( new Date ( System.currentTimeMillis() ) );
		
		if ( track == null ) {
			log.append ( "[" + timeStamp + "] Asked to love a null track, ignoring.\n" );
			return;
		}
		
		String artist = track.getArtist();
		String title = track.getTitle();
		
		if ( !triedConnectWithTheseCredentials ) connect();
		
		if ( session == null ) {
			log.append( "[" + timeStamp + "] Invalid session, unable to love: " + artist + " - " + title + "\n" );
			notifyUserIfNeeded( false );
			return;
		} 
		
		log.append( "[" + timeStamp + "] Attempting to Love: " + artist + " - " + title + " ... " );
		
		Result result = Track.love( artist, title, session );

		boolean success = result.isSuccessful();
		if ( success ) track.setLovedState ( LovedState.TRUE );
		
		log.append ( success ? "success!" : "failed - " + result.getErrorMessage() );
		log.append ( "\n" );
		
		notifyUserIfNeeded( success );
	}
	
	public void unloveTrack ( net.joshuad.library.Track track ) {
		String timeStamp = timeStampFormat.format( new Date ( System.currentTimeMillis() ) );
		if ( track == null ) {
			log.append ( "[" + timeStamp + "] Asked to unlove a null track, ignoring.\n" );
			return;
		}
		
		String artist = track.getArtist();
		String title = track.getTitle();
		
		if ( !triedConnectWithTheseCredentials ) connect();
		
		if ( session == null ) {
			log.append( "[" + timeStamp + "] Invalid session, unable to unlove: " + artist + " - " + title + "\n" );
			notifyUserIfNeeded( false );
			return;
		} 
		
		log.append( "[" + timeStamp + "] Attempting to Unlove: " + artist + " - " + title + " ... " );
		
		Result result = Track.unlove( artist, title, session );

		boolean success = result.isSuccessful();
		if ( success ) track.setLovedState ( LovedState.FALSE );
		
		log.append ( success ? "success!" : "failed - " + result.getErrorMessage() );
		log.append ( "\n" );

		if ( success ) notifiedUserOfFailedAttempt = false;
	}
	
	public LovedState isLoved ( net.joshuad.library.Track track, boolean fromCache ) {
		if ( track == null ) return LovedState.FALSE;
		if ( fromCache && track.getLovedState() != LovedState.NOT_SET ) return track.getLovedState();
		
		String artist = track.getArtist();
		String title = track.getTitle();
		
		if ( artist == null || title == null ) {
			return LovedState.FALSE;
		}
		
		if ( !triedConnectWithTheseCredentials ) connect();
		String timeStamp = timeStampFormat.format( new Date ( System.currentTimeMillis() ) );
		
		if ( session == null ) {
			log.append( "[" + timeStamp + "] Invalid session, cannot check if loved: " + artist + " - " + title + "\n" );
			track.setLovedState( LovedState.CANNOT_GET_DATA );
 			return LovedState.CANNOT_GET_DATA;
		} 
		
		Collection<Track> lovedTracks = User.getLovedTracks( session.getUsername(), key ).getPageResults();
		
		for ( Track test : lovedTracks ) {
			if ( test.getArtist().toLowerCase().equals( artist.toLowerCase() )
			&& test.getName().toLowerCase().equals( title.toLowerCase() ) ) {
				track.setLovedState( LovedState.TRUE );
				return LovedState.TRUE;
			}
		}
		track.setLovedState( LovedState.FALSE );
		return LovedState.FALSE;
	}

	public void toggleLoveTrack ( net.joshuad.library.Track track ) {
		switch ( isLoved ( track, false ) ) {
			case FALSE:
				loveTrack( track );
				break;
			case TRUE:
				unloveTrack( track );
				break;
			case CANNOT_GET_DATA:
				notifyUserIfNeeded( false );
				break;
			case NOT_SET:
				//This should be impossible. 
				break;
				
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


}
