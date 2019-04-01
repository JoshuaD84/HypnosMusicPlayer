package net.joshuad.hypnos;

import java.util.ArrayList;
import java.util.List;
import net.joshuad.library.Track;

/* 
 * Guarantees: 
 * - No null tracks in the stack
 * - It's impossible for the same track to appear twice or more consecutively. 
 */

public class PreviousStack {

	private static final int MAX_PREVIOUS_NEXT_STACK_SIZE = 1000;

	private final ArrayList <Track> stack = new ArrayList <Track>( MAX_PREVIOUS_NEXT_STACK_SIZE );

	public synchronized void addToStack ( Track track ) {
		synchronized ( stack ) {
			if ( track == null ) return;
			if ( stack.size() > 0 && track.equals( stack.get( 0 ) ) ) return;
							
			while ( stack.size() >= MAX_PREVIOUS_NEXT_STACK_SIZE ) {
				stack.remove( stack.size() - 1 );
			}
			
			stack.add( 0, track );
		}
	}
	
	public synchronized Track removePreviousTrack ( Track currentTrack ) {  
		
		if ( stack.isEmpty() ) return null;
		
		Track retMe = stack.remove( 0 );
		
		if ( retMe.equals( currentTrack ) ) {
			if ( stack.isEmpty() ) {
				return null;
			} else {
				retMe = stack.remove( 0 );
			}
		}
		return retMe;
	}
	
	public int size() { 
		return stack.size();
	}
	
	public boolean isEmpty () {
		return stack.isEmpty();
	}
	
	public List<Track> subList ( int fromIndex, int toIndex ) {
		return stack.subList( fromIndex, toIndex );
	}	        
	
	public List<Track> getData () {
		return stack;
	}
}
