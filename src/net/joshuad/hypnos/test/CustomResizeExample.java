	package net.joshuad.hypnos.test;
	
	import java.util.Locale;
	import javafx.application.Application;
	import javafx.beans.property.SimpleObjectProperty;
	import javafx.collections.FXCollections;
	import javafx.scene.Parent;
	import javafx.scene.Scene;
	import javafx.scene.control.TableColumn;
	import javafx.scene.control.TableView;
	import javafx.scene.control.cell.PropertyValueFactory;
	import javafx.scene.layout.BorderPane;
	import javafx.stage.Stage;
	import javafx.util.Callback;
	
	public class CustomResizeExample extends Application {
	
		@SuppressWarnings("unchecked")
		private Parent getContent () {
			TableView <Locale> table = new TableView <>( FXCollections.observableArrayList( Locale.getAvailableLocales() ) );
			TableColumn <Locale, String> countryCode = new TableColumn <>( "CountryCode" );
			countryCode.setCellValueFactory( new PropertyValueFactory <>( "country" ) );
			TableColumn <Locale, String> language = new TableColumn <>( "Language" );
			language.setCellValueFactory( new PropertyValueFactory <>( "language" ) );
			table.getColumns().addAll( countryCode, language );
	
			TableColumn <Locale, Locale> local = new TableColumn <>( "Locale" );
			local.setCellValueFactory( c -> new SimpleObjectProperty <>( c.getValue() ) );
	
			table.getColumns().addAll( local );
			table.setColumnResizePolicy( new CustomResizePolicy() );
	
			BorderPane pane = new BorderPane( table );
			return pane;
		}
		@Override
		public void start ( Stage stage ) throws Exception {
			stage.setScene( new Scene( getContent(), 800, 400 ) );
			stage.show();
		}
		
		public static void main ( String[] args ) {
			launch ( args );
		}
	
	}
	
	@SuppressWarnings ( "rawtypes" ) 
	class CustomResizePolicy implements Callback <TableView.ResizeFeatures, Boolean> {
		@Override
		public Boolean call ( TableView.ResizeFeatures feature ) {
			
			System.out.println ( "Called" ); 
			
			return TableView.CONSTRAINED_RESIZE_POLICY.call( feature );
		}
	}
