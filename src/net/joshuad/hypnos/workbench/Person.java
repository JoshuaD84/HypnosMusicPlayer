package net.joshuad.hypnos.workbench;

public class Person {
	private String firstName, lastName, address;
	
	public Person ( String firstName, String lastName, String address ) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.address = address;
	}
	
	public String getFirstName() {
		return firstName;
	} 
	
	public String getLastName() {
		return lastName;
	}
	
	public String getAddress() {
		return address;
	}
}