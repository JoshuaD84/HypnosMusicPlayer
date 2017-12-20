package net.joshuad.hypnos.workbench;

public class VolumeTest {
	
	public static void main ( String[] args ) {
		for ( double i = 0; i <= 1.01; i+= .01 ) {
			System.out.println ( i + ":\t\t" + volumeCurve ( i ) + "\t\t-> " + inverseVolumeCurve ( volumeCurve ( i ) ) );
		}
		
	}
	
	private static double volumeCurve ( double input ) {
		if ( input <= 0 ) return 0;
		if ( input >= 1 ) return 1;
		
		double value = Math.log( 9 * input + 1 ) / 2.3d;
		if ( value < 0 ) value = 0;
		if ( value > 1 ) value = 1;
		
		return value;
	}
	
	private static double inverseVolumeCurve ( double input ) {
		if ( input <= 0 ) return 0;
		if ( input >= 1 ) return 1;
		
		double value = ( Math.exp( 2.3d * input ) - 1 ) / 9;
		if ( value < 0 ) value = 0;
		if ( value > 1 ) value = 1;
		
		return value;
	}
}


/* y = log ( 9 * x + 1 ) / 2.3
 * 2.3y = log ( 9x + 1 )
 * e^2.3y = 9 * x + 1
 * e^2.3y - 1 = 9x;
 * (e^2.3y - 1 ) / 9 = x
 * 


/*
y = e^6.9x / 1000

1000 y = e^6.9x

ln ( 1000y ) = 6.9x

ln ( 1000y ) / 6.9 = x
*/