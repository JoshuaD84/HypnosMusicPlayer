package net.joshuad.hypnos.fxui;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import net.joshuad.hypnos.library.Track;

public class ThrottledTrackFilter {
	private static final Logger LOGGER = Logger.getLogger(ThrottledTrackFilter.class.getName());
	
	private String requestedFilter = "";
	private boolean requestedHideAlbumTracks = false;
	private long timeRequestMadeMS = 0;

	private Thread filterThread;
	private boolean interruptFiltering = false;
	
	private String currentAppliedFilter = "";
	private boolean currentAppliedHideAlbumTracks = false;
	
	private FilteredList <? extends Track> filteredList;
	
	public ThrottledTrackFilter ( FilteredList <? extends Track> filteredList ) {
		this.filteredList = filteredList;
		
		filterThread = new Thread ( () -> {
			while ( true ) {
				String filter = requestedFilter;
				boolean hide = requestedHideAlbumTracks;
				
				if ( !filter.equals( currentAppliedFilter ) || hide != currentAppliedHideAlbumTracks ) {
					if ( System.currentTimeMillis() >= timeRequestMadeMS + 100 ) {
						interruptFiltering = false;
						Platform.runLater(()->setPredicate( filter, hide ));
						currentAppliedFilter = filter;
						currentAppliedHideAlbumTracks = hide;
					}
				}
				
				try { Thread.sleep( 25 ); } catch ( InterruptedException e ) {}	
			}
		});

		filterThread.setName( "Throttled Track Filter" );
		filterThread.setDaemon( true );
		filterThread.start();
	}
	
	public void setFilter ( String filter, boolean hideAlbumTracks ) {
		if ( filter == null ) filter = "";
		timeRequestMadeMS = System.currentTimeMillis();
		this.requestedFilter = filter;
		this.requestedHideAlbumTracks = hideAlbumTracks;
		interruptFiltering = true;
	}
	
	private void setPredicate ( String filterText, boolean hideAlbumTracks ) {
			try {
				filteredList.setPredicate( ( Track track ) -> {
					try {
						if ( track.getAlbum() != null && hideAlbumTracks ) return false;
						if ( interruptFiltering ) return true;
						if ( filterText.isEmpty() ) return true;
				
						ArrayList <String> matchableText = new ArrayList <String>();
				
						matchableText.add( track.getArtist().toLowerCase() );
						matchableText.add( track.getTitle().toLowerCase() );
						matchableText.add( track.getFullAlbumTitle().toLowerCase() );
						
						if ( track.getArtist().matches( ".*[^\\p{ASCII}]+.*" ) ) {
							matchableText.add( Normalizer.normalize( track.getArtist(), Normalizer.Form.NFD )
								.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
						}
						
						if ( track.getTitle().matches( ".*[^\\p{ASCII}]+.*" ) ) {
							matchableText.add( Normalizer.normalize( track.getTitle(), Normalizer.Form.NFD )
								.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
						}
						
						if ( track.getFullAlbumTitle().matches( ".*[^\\p{ASCII}]+.*" ) ) {
							matchableText.add( Normalizer.normalize( track.getFullAlbumTitle(), Normalizer.Form.NFD )
								.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
						}
						
						String[] lowerCaseFilterTokens = filterText.toLowerCase().split( "\\s+" );
						for ( String token : lowerCaseFilterTokens ) {
							boolean tokenMatches = false;
							for ( String test : matchableText ) {
								if ( test.contains( token ) ) {
									tokenMatches = true;
								}
							}
				
							if ( !tokenMatches ) {
								return false;
							}
						}
					}catch(ArrayIndexOutOfBoundsException ae) {
						//This happens if you filter a list while scanning, and then clear the filter.  
						//It doesn't seem to matter at all, so I'm just ignoring it for now. 
					}catch(Exception e) {
						LOGGER.log(Level.INFO, "Exception caught, ignored.", e); 
					}
				
					return true;
				});
			} catch ( Exception e ) {
				LOGGER.log(Level.INFO, "Caught Exception: " + e );
			}

	}
}
