package net.joshuad.hypnos.workbench;
 
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
import net.joshuad.hypnos.fxui.HypnosResizePolicy;
		
public class CustomResizeExample extends Application {
	
	private Parent getContent () {
		TableView <Locale> table = new TableView <>( FXCollections.observableArrayList( Locale.getAvailableLocales() ) );
		TableColumn <Locale, String> countryCode = new TableColumn <>( "CountryCode" );
		countryCode.setCellValueFactory( new PropertyValueFactory <>( "country" ) );
		TableColumn <Locale, String> language = new TableColumn <>( "Language" );
		language.setCellValueFactory( new PropertyValueFactory <>( "language" ) );

		TableColumn <Locale, Locale> local = new TableColumn <>( "Locale" );
		local.setCellValueFactory( c -> new SimpleObjectProperty <>( c.getValue() ) );
		
		TableColumn <Locale, String> language2 = new TableColumn <>( "Language2" );
		language.setCellValueFactory( new PropertyValueFactory <>( "language" ) );

		HypnosResizePolicy resizePolicy = new HypnosResizePolicy();
		table.setColumnResizePolicy( resizePolicy );
		
		//resizePolicy.registerFixedWidthColumns( countryCode );
		//resizePolicy.registerFixedWidthColumns( language );
		resizePolicy.registerFixedWidthColumns( local );
		//resizePolicy.registerFixedWidthColumns( language2 );
		
		countryCode.setPrefWidth( 80 );
		language.setPrefWidth( 180 );
		local.setPrefWidth( 280 );
		language2.setPrefWidth( 80 );
		
		table.getColumns().add( countryCode );
		table.getColumns().add( language );
		table.getColumns().add( local );
		table.getColumns().add( language2 );
		
		for ( TableColumn column : table.getColumns() ) {
			System.out.println ( "Width: " + column.getWidth() + ", Pref: " + column.getPrefWidth() + ", Min: " + column.getMinWidth() + ", Max: " + column.getMaxWidth() ) ;
		}

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
	
