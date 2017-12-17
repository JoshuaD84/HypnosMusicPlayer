package net.joshuad.hypnos.test;

public class VolumeTest {
	
	public static void main ( String[] args ) {
		for ( double i = 0; i <= 1.01; i+= .01 ) {
			System.out.println ( i + ": " + math ( i ) + " -> " + inverse ( math ( i ) ) );
		}
		
	}
	
	private static double math ( double input ) {
		return Math.exp( 6.908d * input ) / 1000d;
	}
	
	private static double inverse ( double input ) {
		return Math.log( 1000 * input ) / 6.908d;
	}
}


/*
y = e^6.9x / 1000

1000 y = e^6.9x

ln ( 1000y ) = 6.9x

ln ( 1000y ) / 6.9 = x
*/