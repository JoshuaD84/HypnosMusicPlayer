package net.joshuad.hypnos.workbench;

import java.util.ArrayList;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MemoryLeak extends Application {

	@Override
	public void start( Stage stage ) throws Exception {
		
		ObservableList<Person> tableContent = FXCollections.observableArrayList( new ArrayList <Person>() );
		
		TableColumn <Person, String> firstNameColumn = new TableColumn <> ( "First Name" );
		TableColumn <Person, String> LastNameColumn = new TableColumn <> ( "Last Name" );
		TableColumn <Person, String> addressColumn = new TableColumn <> ( "Address" );

		firstNameColumn.setCellValueFactory( new PropertyValueFactory <Person, String>( "firstName" ) );
		LastNameColumn.setCellValueFactory( new PropertyValueFactory <Person, String>( "lastName" ) );
		addressColumn.setCellValueFactory( new PropertyValueFactory <Person, String>( "address" ) );

		TableView <Person> tableView = new TableView <> ();
		tableView.getColumns().addAll( firstNameColumn, LastNameColumn, addressColumn );
		tableView.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		tableView.setItems( tableContent );
		
		for ( int k = 0; k < 4000; k ++ ) {
			tableContent.add ( new Person ( "Dummy", "Person", k + " Sesame Street" ) );
		}
		
		tableView.scrollTo( 200 );
		
		BorderPane pane = new BorderPane( tableView );
		stage.setScene( new Scene( pane, 800, 400 ) );
		stage.show();
		
	}
	
	public static void main ( String[] args ) {
		launch ( args );
	}
}

