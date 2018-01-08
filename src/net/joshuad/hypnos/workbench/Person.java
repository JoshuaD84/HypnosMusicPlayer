package net.joshuad.hypnos.workbench;

public class Person {
	private String firstName, middleName, lastName, address;
	
	public Person ( String firstName, String lastName, String address ) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.address = address;
	}
	
	public Person ( String firstName, String middleName, String lastName, String address ) {
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		this.address = address;
	}
	
	public String getFirstName() {
		return firstName;
	}
	
	public String getMiddleName() {
		return middleName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public String getAddress() {
		return address;
	}
}