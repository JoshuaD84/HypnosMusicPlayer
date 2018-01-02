	package net.joshuad.hypnos.workbench;
	
	import java.util.Locale;
	
	import javafx.application.Application;
	import javafx.beans.property.SimpleObjectProperty;
	import javafx.collections.FXCollections;
	import javafx.scene.Scene;
	import javafx.scene.control.TableColumn;
	import javafx.scene.control.TableView;
	import javafx.scene.control.TextField;
	import javafx.scene.control.cell.PropertyValueFactory;
	import javafx.scene.input.KeyCode;
	import javafx.scene.input.KeyEvent;
	import javafx.scene.layout.BorderPane;
	import javafx.stage.Stage;
	
	public class KeyEventIssue extends Application {
	
		@Override
		public void start ( Stage stage ) throws Exception {
			
			TextField textInput = new TextField();
			TableView <Locale> table = getFilledTable();
			
			BorderPane primaryContainer = new BorderPane();
			
			primaryContainer.setTop( textInput );
			primaryContainer.setCenter( table );
			
			primaryContainer.setOnKeyPressed( ( KeyEvent e ) -> { 
				if ( e.getCode() == KeyCode.S 
				&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
					System.out.println ( "Heard S" );
					e.consume();
					
				} else if ( e.getCode() == KeyCode.S && e.isShiftDown() 
				&& !e.isControlDown() && !e.isAltDown() && !e.isMetaDown() ) {
					System.out.println ( "Heard Shift + S" );
					e.consume();
				} 
			});
			
			Scene scene = new Scene( primaryContainer );
			stage.setScene( scene );
			stage.setWidth( 700 );
			stage.show();
		}
		
		private TableView <Locale> getFilledTable () {
			TableView <Locale> table = new TableView <>( FXCollections.observableArrayList( Locale.getAvailableLocales() ) );
			TableColumn <Locale, String> countryCode = new TableColumn <>( "CountryCode" );
			countryCode.setCellValueFactory( new PropertyValueFactory <>( "country" ) );
			TableColumn <Locale, String> language = new TableColumn <>( "Language" );
			language.setCellValueFactory( new PropertyValueFactory <>( "language" ) );
	
			TableColumn <Locale, Locale> local = new TableColumn <>( "Locale" );
			local.setCellValueFactory( c -> new SimpleObjectProperty <>( c.getValue() ) );
			
			language.setCellValueFactory( new PropertyValueFactory <>( "language" ) );
	
			table.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
			
			table.getColumns().addAll( countryCode, language, local );
			
			return table;
		}
		
		public static void main ( String[] args ) {
			Application.launch ( args );
		}
	}
