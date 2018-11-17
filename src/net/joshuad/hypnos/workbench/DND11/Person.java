package net.joshuad.hypnos.workbench.DND11;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Person implements Serializable {

	private static final long serialVersionUID = 1L;

	private String firstName, lastName;
	
	public Person ( String firstName, String lastName ) {
		this.firstName = firstName;
		this.lastName = lastName;
	}
	
	public String getFirstName() {
		return firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public static List <Person> generatePersons ( int number ) {
		List<Person> retMe = new ArrayList<Person> ( number );
		for ( int k = 0; k < number; k++ ) {
			retMe.add ( new Person ( randomFirstName(), randomLastName() ) );
		}
		return retMe;
	}
	

	private static Random rand = new Random();
	
	private static String randomFirstName() {
		return firstNames [ Math.abs( rand.nextInt() ) % firstNames.length ];
	}
	
	private static String randomLastName() {
		return lastNames [ Math.abs( rand.nextInt() ) % lastNames.length ];
	}
	
	private static String[] firstNames = new String[] {
		"ANTON","ANTONE","ANTONIA","Ä�NTONIO","ANTONY","ANTWAN","ARCHIE","ARDEN","ARIEL","ARLEN",
		"ARMAND","ARMANDO","ARNOLD","ARNOLDO","ARNULFÃ³","ARON","ARRON","ART","ARTHUR","ARTURO",
		"DARRICK","DARRIN","DARRON","DARRYL","DARWIN","DARYL","DAVE","DAVID","DAVIS","DEAN",

	};
	
	private static String[] lastNames = new String[] {
		"SMITH","JOHNSON","WILLIAMS","BROWN","JONES","MILLER","DAVIS","GARCIA","RODRIGUEZ",
		"WILSON","MARTINEZ","ANDERSON","TAYLOR","THOMAS","HERNANDEZ","MOORE","MARTIN","JACKSON"
	};
}