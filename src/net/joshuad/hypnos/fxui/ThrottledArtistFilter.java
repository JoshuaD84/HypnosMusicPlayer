package net.joshuad.hypnos.fxui;

import java.text.Normalizer;
import java.util.ArrayList;
import javafx.collections.transformation.FilteredList;
import net.joshuad.hypnos.library.Artist;

public class ThrottledArtistFilter {
	
	private String requestedFilter = "";
	private long timeRequestMadeMS = 0;

	private Thread filterThread;
	private boolean interruptFiltering = false;
	
	private String currentAppliedFilter = "";
	
	private FilteredList <? extends Artist> filteredList;
	
	public ThrottledArtistFilter ( FilteredList <? extends Artist> filteredList ) {
		this.filteredList = filteredList;
		
		filterThread = new Thread ( () -> {
			while ( true ) {
				String filter = requestedFilter;
				
				if ( !filter.equals( currentAppliedFilter ) ) {
					if ( System.currentTimeMillis() >= timeRequestMadeMS + 100 ) {
						interruptFiltering = false;
						setPredicate( filter );
						currentAppliedFilter = filter;
					}
				}
				
				try { Thread.sleep( 25 ); } catch ( InterruptedException e ) {}	
			}
		});

		filterThread.setName( "Throttled Track Filter" );
		filterThread.setDaemon( true );
		filterThread.start();
	}
	
	public void setFilter ( String filter ) {
		if ( filter == null ) filter = "";
		timeRequestMadeMS = System.currentTimeMillis();
		this.requestedFilter = filter;
		interruptFiltering = true;
	}
	
	private void setPredicate ( String filterText ) {
		filteredList.setPredicate( ( Artist artist ) -> {
			if ( interruptFiltering ) return true;
			if ( filterText.isEmpty() ) return true;
	
			ArrayList <String> matchableText = new ArrayList <String>();
	
			matchableText.add( artist.getName().toLowerCase() );
			
			if ( artist.getName().matches( ".*[^\\p{ASCII}]+.*" ) ) {
				matchableText.add( Normalizer.normalize( artist.getName(), Normalizer.Form.NFD )
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
	
			return true;
		});
	}
}
