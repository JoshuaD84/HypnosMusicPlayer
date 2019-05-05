package net.joshuad.hypnos.fxui;

import java.text.Normalizer;
import java.util.ArrayList;

import javafx.collections.transformation.FilteredList;
import net.joshuad.hypnos.library.Album;

public class ThrottledAlbumFilter {
	private String requestedFilter = "";
	private long timeRequestMadeMS = 0;
	
	private Thread filterThread;
	private boolean interruptFiltering = false;
	
	private String currentAppliedFilter = "";
	
	private FilteredList <Album> filteredList;
	
	public ThrottledAlbumFilter ( FilteredList <Album> filteredList ) {
		this.filteredList = filteredList;
		
		try {
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
					
					try { Thread.sleep( 50 ); } catch ( InterruptedException e ) {}	
				}
			});

			filterThread.setName( "Throttled Album Filter" );
			filterThread.setDaemon( true );
			filterThread.start();
		} catch ( Exception e ) {
			//TODO: Logging, what is this exception? 
			System.out.println ( "Caught here" );
		}
	}
	
	public void setFilter ( String filter ) {
		if ( filter == null ) filter = "";
		timeRequestMadeMS = System.currentTimeMillis();
		this.requestedFilter = filter;
		interruptFiltering = true;
	}
	
	private void setPredicate ( String filterText ) {
		filteredList.setPredicate( album -> {
			if ( interruptFiltering ) return true;
			if ( filterText.isEmpty() ) return true;

			ArrayList <String> matchableText = new ArrayList <String>();
						
			matchableText.add( album.getAlbumArtist().toLowerCase() );
			matchableText.add( album.getYear().toLowerCase() );
			matchableText.add( album.getFullAlbumTitle().toLowerCase() );
			
			matchableText.add( Normalizer.normalize( album.getFullAlbumTitle(), Normalizer.Form.NFD )
				.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() 
			);
			
			matchableText.add( Normalizer.normalize( album.getYear(), Normalizer.Form.NFD )
				.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase()
			);
			
			matchableText.add( Normalizer.normalize( album.getAlbumArtist(), Normalizer.Form.NFD )
				.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() 
			);

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
