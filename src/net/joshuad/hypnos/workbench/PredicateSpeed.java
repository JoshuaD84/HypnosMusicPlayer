package net.joshuad.hypnos.workbench;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Random;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class PredicateSpeed extends Application {

	ObservableList<Person> tableContent = FXCollections.observableArrayList( new ArrayList <Person>() );
	FilteredList<Person> filteredContent = new FilteredList <Person>( tableContent, p -> true );
	
	private BooleanProperty lastInterruptSignal = new SimpleBooleanProperty ( false );
	
	private long lastKeyPressMS = 0;
	private String mostRecentFilter = "";
	private String requestedFilter = "";
	private boolean interruptPredicate = false;

	Thread filterThread;
	TextField filterBox;

	@Override
	public void start ( Stage stage ) throws Exception {
		Random random = new Random();
		
		TableColumn <Person, String> firstNameColumn = new TableColumn <> ( "First Name" );
		TableColumn <Person, String> middleNameColumn = new TableColumn <> ( "Middle Name" );
		TableColumn <Person, String> LastNameColumn = new TableColumn <> ( "Last Name" );
		TableColumn <Person, String> addressColumn = new TableColumn <> ( "Address" );

		firstNameColumn.setCellValueFactory( new PropertyValueFactory <Person, String>( "firstName" ) );
		middleNameColumn.setCellValueFactory( new PropertyValueFactory <Person, String>( "middleName" ) );
		LastNameColumn.setCellValueFactory( new PropertyValueFactory <Person, String>( "lastName" ) );
		addressColumn.setCellValueFactory( new PropertyValueFactory <Person, String>( "address" ) );

		TableView <Person> tableView = new TableView <> ();
		tableView.getColumns().addAll( firstNameColumn, middleNameColumn, LastNameColumn, addressColumn );
		tableView.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		tableView.setItems( filteredContent );
		
		for ( int k = 0; k < 3000; k ++ ) {
			String firstName = firstNames [ random.nextInt( firstNames.length ) ];
			String middleName = firstNames [ random.nextInt( firstNames.length ) ];
			String lastName = lastNames [ random.nextInt( lastNames.length ) ];
			int streetNumber = random.nextInt( 1000 );
			String streetName = streetNames [ random.nextInt( streetNames.length ) ];
			String streetType = streetTypes [ random.nextInt( streetTypes.length ) ];
			tableContent.add ( 	new Person ( firstName, middleName, lastName, 
					streetNumber + " " + streetName + " " + streetType ) );
		}
		
		filterBox = new TextField();
		
		System.out.println ( "Text: " + Normalizer.normalize( "3335", Normalizer.Form.NFD )
				.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
		
		setupSmartFilter();
		
		BorderPane pane = new BorderPane( );
		pane.setCenter( tableView );
		pane.setTop( filterBox );
		stage.setScene( new Scene( pane, 800, 400 ) );
		stage.show();
		
	}
	
	void setupDumbFilter() {
		filterBox.textProperty().addListener( new ChangeListener <String> () {

			@Override
			public void changed ( ObservableValue <? extends String> observable, String oldValue, String newValue ) {
				System.out.println ( "New request heard: " + newValue );
				long startTimeMS = System.currentTimeMillis();
				setPredicate ( newValue );
				long execTime = ( System.currentTimeMillis() - startTimeMS );
				System.out.println ( "Time (ms) to set predicate '" + newValue + "': " + execTime ); 
			}
		});
	}
		
	
	void setupSmartFilter() {
		filterBox.textProperty().addListener( new ChangeListener <String> () {

			@Override
			public void changed ( ObservableValue <? extends String> observable, String oldValue, String newValue ) {
				lastKeyPressMS = System.currentTimeMillis();
				requestedFilter = newValue;
				interruptPredicate = true;
			}
		});
		
		filterThread = new Thread ( () -> {
			while ( true ) {
				String request = requestedFilter;

				if ( !request.equals ( mostRecentFilter ) ) {
					if ( System.currentTimeMillis() >= lastKeyPressMS + 100 ) {
						System.out.println ( "New request heard: " + request );
						long startTimeMS = System.currentTimeMillis();
						System.out.print ( "starting.." );
						
						interruptPredicate = false;
						setPredicate ( request );
						
						mostRecentFilter = request;
						long execTime = ( System.currentTimeMillis() - startTimeMS );
						System.out.println ( "Time (ms) to set predicate '" + request + "': " + execTime );
						System.out.println();
					}
				}
				
				try { Thread.sleep( 25 ); } catch ( InterruptedException e ) {}	
						
			}
		});
		
		filterThread.start();
	}
		
	void setPredicate ( String filterText ) {
		BooleanProperty hasPrinted = new SimpleBooleanProperty ( false );
		
		filteredContent.setPredicate( ( Person person ) -> {
			if ( interruptPredicate ) {
				if ( !hasPrinted.get() ) {
					System.out.print ("interrupted! .." );
					hasPrinted.set ( true );
				}
				
				return true;
			}
			
			if ( filterText == null || filterText.isEmpty() ) {
				return true;
			}
			
			String[] lowerCaseFilterTokens = filterText.toLowerCase().split( "\\s+" );

			ArrayList <String> matchableText = new ArrayList <String>();

			String addressNumber = person.getAddress().split( " " )[0];
			String addressStreet = person.getAddress().split( " " )[1];
			String addressType = person.getAddress().split( " " )[2];
			
			matchableText.add( person.getFirstName().toLowerCase() );
			matchableText.add( person.getMiddleName().toLowerCase() );
			matchableText.add( person.getLastName().toLowerCase() );
			matchableText.add( addressNumber.toLowerCase() );
			matchableText.add( addressStreet.toLowerCase() );
			matchableText.add( addressType.toLowerCase() );
			
			//So o finds ö and ó, a finds ā, etc.
			matchableText.add( 
				Normalizer.normalize( person.getFirstName(), Normalizer.Form.NFD )
				.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
			
			matchableText.add( 
					Normalizer.normalize( person.getMiddleName(), Normalizer.Form.NFD )
					.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
			
			matchableText.add( 
					Normalizer.normalize( person.getLastName(), Normalizer.Form.NFD )
					.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
	
			/*matchableText.add( 
					Normalizer.normalize( addressNumber, Normalizer.Form.NFD )
					.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
			*/
			
			matchableText.add( 
					Normalizer.normalize( addressStreet, Normalizer.Form.NFD )
					.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
			
			matchableText.add( 
					Normalizer.normalize( addressType, Normalizer.Form.NFD )
					.replaceAll( "[^\\p{ASCII}]", "" ).toLowerCase() );
			
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
	
	public static void main ( String[] args ) {
		launch ( args );
	}
	

	String[] firstNames = new String[] {
		"AARON","ABDUL","ABE","ABEL","ABRAHAM","ABRAM","ADALBERTO","ADAM","ADAN","ADOLFO","ADOLPH",
		"AGUSTIN","AHMAD","AHMED","AL","ALAN","ALBERT","ALBERTO","ALDEN","ALDO","ALEC","ALEJANDRO",
		"ALEXANDER","ALEXIS","ALFONSO","ALFONZO","ALFRED","ALFREDO","ALI","ALLAN","ALLEN","ALONSO",
		"ALPHONSE","ALPHONSO","ALTON","ALVA","ALVARO","ALVIN","AMADO","AMBRóSE","AMOS","ANDERSON",
		"ANDREA","ANDREAS","ANDRES","ANDREW","ANDY","ANGEL","ANGELO","ANIBAL","ANTHONY","ANTIONE",
		"ANTON","ANTONE","ANTONIA","āNTONIO","ANTONY","ANTWAN","ARCHIE","ARDEN","ARIEL","ARLEN",
		"ARMAND","ARMANDO","ARNOLD","ARNOLDO","ARNULFó","ARON","ARRON","ART","ARTHUR","ARTURO",
		"DARRICK","DARRIN","DARRON","DARRYL","DARWIN","DARYL","DAVE","DAVID","DAVIS","DEAN",
		"DEANGELO", "DEE","DEL","DELBERT","DELMAR","DELMER","DEMARCUS","DEMETRIUS","DENIS","DENES",
		"DEON","DEREK","DERICK","DERRICK","DESHAWN","DESMOND","DEVIN","DEVON","DEWAYNE","DEWEY",
		"DEXTER","DICK","DIEGO","DILLON","DINO","DION","DIRK","DOMENIC","DOMINGO","DOMINIC",
		"DOMINIQUE","DON","DONALD","DONG","DONN","DONNELL","DONNIE","DONNY","DONOVAN","DONTE",
		"DORSEY","DOUG","DOUGLAS","DOUGLASS","DOYLE","DREW","DUANE","DUDLEY","DUNCAN","DUSTIN",
		"DWAIN","DWAYNE","DWIGHT","DYLAN","EARL","EARLE","EARNEST","ED","EDDIE","EDDY","EDGAR",
		"EDISON","EDMOND","EDMUND","EDMUNDO","EDUARDO","EDWARD","EDWARDO","EDWIN","EFRAIN","EFREN",
		"ELBERT","ELDEN","ELDON","ELDRIDGE","ELI","ELIAS","ELIJAH","ELISEO","ELISHA","ELLIOT",
		"ELLIS","ELLSWORTH","ELMER","ELMO","ELOY","ELROY","ELTON","ELVIN","ELVIS","ELWOOD","EMA",
		"EMERSON","EMERY","EMIL","EMILE","EMILIO","EMMANUEL","EMMETT","EMMITT","EMORY","ENOCH",
		"ERASMO","ERIC","ERICH","ERICK","ERIK","ERIN","ERNEST","ERNESTO","ERNIE","ERROL","ERVIN",
	};
	
	String[] lastNames = new String[] {
		"SMITH","JOHNSON","WILLIAMS","BROWN","JONES","MILLER","DAVIS","GARCIA","RODRIGUEZ",
		"WILSON","MARTINEZ","ANDERSON","TAYLOR","THOMAS","HERNANDEZ","MOORE","MARTIN","JACKSON",
		"THOMPSON","WHITE","LOPEZ","LEE","GONZALEZ","HARRIS","CLARK","LEWIS","ROBINSON",
		"WALKER","PEREZ","HALL","YOUNG","ALLEN","SANCHEZ","WRIGHT","KING","SCOTT","GREEN","BAKER",
		"ADAMS","NELSON","HILL","RAMIREZ","CAMPBELL","MITCHELL","ROBERTS","CARTER","PHILLIPS",
		"EVANS","TURNER","TORRES","PARKER","CóLLINS","EDWARDS","STEWāRT","FLORES","MORRIS",
		"NGUYEN","MURPHY","RIVERA","COOK","ROGERS","MORGAN","PETERSON","COOPER","REED",
		"BAILEY","BELL","GOMEZ","KELLY","HOWARD","WARD","COX","DIAZ","RICHARDSON","WOOD","WATSON",
		"BROOKS","BENNETT","GRAY","JAMES","REYES","CRUZ","HUGHES","PRICE","MYERS","LONG",
		"FOSTER","SANDERS","ROSS","MORALES","POWELL","SULLIVAN","RUSSELL","ORTIZ","JENKINS",
		"GUTIERREZ","PERRY","BUTLER","BARNES","FISHER","HENDERSON","COLEMAN","SIMMONS","PATTERSON",
		"JORDAN","REYNOLDS","HAMILTON","GRAHAM","KIM","GONZALES","ALEXANDER","RAMOS",
		"WALLACE","GRIFFIN","WEST","COLE","HAYES","CHAVEZ","GIBSON","BRYANT","ELLIS","STEVENS",
		"MURRAY","FORD","MARSHALL","OWENS","MCDONALD","HāRRISON","RUIZ","KENNEDY","WELLS",
		"ALVAREZ","WOODS","MENDOZA","CASTILLO","OLSON","WEBB","WASHINGTON","TUCKER","FREEMAN",
		"BURNS","HENRY","VASQUEZ","SNYDER","SIMPSON","CRAWFORD","JIMENEZ","PORTER","MASON",
		"SHAW","GORDON","WAGNER","HUNTER","ROMERO","HICKS","DIXON","HUNT","PALMER","ROBERTSON",
		"BLACK","HOLMES","STONE","MEYER","BOYD","MILLS","WARREN","FOX","ROSE","RICE","MORENO",
		"SCHMIDT","PATEL","FERGUSON","NICHOLS","HERRERA","MEDINA","RYAN","FERNANDEZ","WEAVER",
		"DANIELS","STEPHENS","GARDNER","PAYNE","KELLEY","DUNN","PIERCE","ARNOLD","TRAN","SPENCER",
		"PETERS","HAWKINS","GRANT","HANSEN","CASTRó","HOFFMAN","HART","ELLIOTT","CUNNINGHAM",
		"KNIGHT","BRADLEY","CARROLL","HUDSON","DUNCAN","ARMSTRONG","BERRY","ANDREWS","JOHNSTON",
		"RAY","LANE","RILEY","CARPENTER","PERKINS","AGUILAR","SILVA","RICHARDS","WILLIS","MATHEWS",
		"CHAPMAN","LAWRENCE","GARZA","VARGAS","WATKINS","WHEELER","LARSON","CARLSON","HARPER",
		"GREENE","BURKE","GUZMAN","MORRISON","MUNOZ","JACOBS","óBRIEN","LAWSON","FRANKLIN","LYNCH",
		"BISHOP","CARR","SALAZAR","AUSTIN","MENDEZ","GILBERT","JENSEN","WILLIAMSON","MONTGOMERY",
		"HARVEY","OLIVER","HOWELL","DEAN","HANSON","WEBER","GARRETT","SIMS","BURTON","FULLER",
		"SOTO","MCCOY","WELCH","CHEN","SCHULTZ","WALTERS","REID","FIELDS","WALSH","LITTLE","FOWLR",
		"BOWMAN","DAVIDSON","MAY","DAY","SCHNEIDER","NEWMAN","BREWER","LUCAS","HOLLAND","WONG",
		"SANTOS","CURTIS","PEARSON","DELGADO","VALDEZ","PENA","RIOS","DOUGLAS","SANDOVAL","BāRETT",
		"HOPKINS","KELLER","GUERRERO","STANLEY","BATES","ALVARADO","BECK","ORTEGA","WADE","ESTRDA",
		"CONTRERAS","BARNETT","CALDWELL","SANTIAGO","LAMBERT","POWERS","CHAMBERS","NUNEZ","CRAIG",
		"LEONARD","LOWE","RHODES","BYRD","GREGORY","SHELTON","FRAZIER","BECKER","MALDONADO","FLEG",
	};
	
	String[] streetNames = new String[] {
		"VEGA","SUTTON","COHEN","JENNINGS","PARKS","MCDANIEL","WATTS","BARKER","NORRIS","VAUGHN",
		"VAZQUEZ","HOLT","SCHWARTZ","STEELE","BENSON","NEAL","DOMINGUEZ","HORTON","TERRY","WOLFE",
		"HALE","LYONS","GRAVES","HAYNES","MILES","PARK","WARNER","PāDILLA","BUSH","THORNTON",
		"MCCARTHY","MANN","ZIMMERMAN","ERICKSON","FLETCHER","MCKINNEY","PAGE","DAWSON","JOSEPH",
		"MARQUEZ","REEVES","KLEIN","ESPINóZA","BALDWIN","MORAN","LOVE","ROBBINS","HIGGINS","BALL",
		"CORTEZ","LE","GRIFFITH","BOWEN","SHARP","CUMMINGS","RAMSEY","HARDY","SWANSON","BARBER",
		"ACOSTA","LUNA","CHāNDLER","DANIEL","BLAIR","CROSS","SIMON","DENNIS","OCONNOR","QUINN",
		"GROSS","NAVARRO","MóSS","FITZGERALD","DOYLE","MCLAUGHLIN","ROJAS","RODGERS","STEVENSON",
		"SINGH","YANG","FIGUEROA","HARMON","NEWTON","PAUL","MANNING","GARNER","MCGEE","REESE",
		"FRANCIS","BURGESS","ADKINS","GOODMAN","CURRY","BRADY","CHRISTENSEN","POTTER","WāLTON",
		"GOODWIN","MULLINS","MOLINA","WEBSTER","FISCHER","CAMPOS","AVILA","SHERMAN","TODD","CHANG"
	};
	
	String[] streetTypes = new String[] {
		"STREET", "ROAD", "AVENUE", "LANE", "DRIVE", "BLVD"
	};
}


