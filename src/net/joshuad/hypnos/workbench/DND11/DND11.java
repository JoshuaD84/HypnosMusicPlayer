package net.joshuad.hypnos.workbench.DND11;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;

public class DND11 extends Application {
	
	public TableView<Person> getTable () {
		DataFormat DRAGGED_PERSON = new DataFormat ( "application/example-person" );

		TableColumn <Person, String> firstNameColumn = new TableColumn <> ( "First Name" );
		TableColumn <Person, String> LastNameColumn = new TableColumn <> ( "Last Name" );

		firstNameColumn.setCellValueFactory( new PropertyValueFactory <Person, String>( "firstName" ) );
		LastNameColumn.setCellValueFactory( new PropertyValueFactory <Person, String>( "lastName" ) );

		TableView <Person> tableView = new TableView <> ();
		tableView.getColumns().addAll( firstNameColumn, LastNameColumn );
		tableView.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		
		tableView.setEditable( false );
		tableView.setItems( FXCollections.observableArrayList( Person.generatePersons ( 10 ) ) );
		
		tableView.getSelectionModel().setSelectionMode( SelectionMode.SINGLE
				);
		
		tableView.setRowFactory( tv -> {
			TableRow <Person> row = new TableRow <>();

			row.setOnDragDetected( event -> {
				if ( !row.isEmpty() ) {
					Dragboard db = row.startDragAndDrop( TransferMode.COPY );
					ClipboardContent cc = new ClipboardContent();
					db.setDragView( row.snapshot( null, null ) );
					cc.put( DRAGGED_PERSON, row.getItem() );
					tableView.getItems().remove( row.getItem() );
					db.setContent( cc );
				}
			});

			row.setOnDragOver( event -> {
				Dragboard db = event.getDragboard();
				event.acceptTransferModes( TransferMode.COPY );
			});

			row.setOnDragDropped( event -> {
				Dragboard db = event.getDragboard();
				
				Person person = (Person)event.getDragboard().getContent( DRAGGED_PERSON );
				
				if ( person != null ) {
					tableView.getItems().remove( person );
					int dropIndex = row.getIndex();
					tableView.getItems().add( dropIndex, person );
				}

				event.setDropCompleted( true );
				event.consume();
			});

			return row;
		});
		
		return tableView;
	}

	@Override
	public void start ( Stage stage ) throws Exception {
		stage.setScene( new Scene( getTable(), 800, 400 ) );
		stage.show();
	
	}
	
	public static void main ( String[] args ) {
		launch( args );
	}
}
