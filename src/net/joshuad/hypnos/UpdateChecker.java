package net.joshuad.hypnos;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.joshuad.hypnos.fxui.SettingsWindow;

public class UpdateChecker {
	private static final Logger LOGGER = Logger.getLogger( SettingsWindow.class.getName() );
	
	private DateFormat releaseDateFormat = new SimpleDateFormat ( "yyyy-MM-dd" );
	
	public UpdateChecker() {}
	
	public boolean updateAvailable () {
		
		if ( !UpdateChecker.class.getResource( "UpdateChecker.class" ).toString().startsWith( "jar" ) ) {
			if ( Hypnos.isDeveloping() ) {
				//This is what happens when I am working on the project and I don't want to see extraneous prints. 
				return false;
			} else {
				LOGGER.info( "Not running from a jar, so we don't have access to this version's release date. Not checking for updates. "
					+ "You may want to download a release binary or build a jar using the provided ant build file to be able to check for updates." 
				);
				return false;
			}
		}
		
		Date newestVersionDate = getNewestVersionDateFromWeb ();
		Date thisVersionDate = null;
		
		try {
			thisVersionDate = releaseDateFormat.parse( Hypnos.getBuildDate() );
		} catch ( Exception e ) {
			LOGGER.log ( Level.WARNING, "Unable to parse this release's date, assuming no update available.", e );
	        return false;
		}
		
		if ( newestVersionDate == null ) return false;
		return newestVersionDate.after( thisVersionDate );
	}
	
	private Date getNewestVersionDateFromWeb () {
		try {
			URL url = new URL( "http://hypnosplayer.org/update-info/current-version-date" );
	
	        String line;
	        URLConnection con = url.openConnection();
	        con.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
	        
			try (
				InputStream is = con.getInputStream();
				BufferedReader br = new BufferedReader( new InputStreamReader( is ) );	
			) {
				line = br.readLine();
				return releaseDateFormat.parse ( line );
				
			} catch ( Exception e ) {
				LOGGER.log ( Level.WARNING, "Unable to fetch current version date from web. Assuming no update available.", e );
		        return null;
			}
			
		} catch ( Exception e ) {
			LOGGER.log ( Level.WARNING, "Unable to fetch current version date from web. Assuming no updates available.", e );
	        return null;
		}
	}
}
