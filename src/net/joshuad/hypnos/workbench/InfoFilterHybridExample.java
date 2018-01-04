package net.joshuad.hypnos.workbench;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import net.joshuad.hypnos.fxui.InfoFilterHybrid;

public class InfoFilterHybridExample extends Application {
	
	@Override
	public void start( Stage mainStage ) {

		TableView <String> table = new TableView <> ();
		
		InfoFilterHybrid hybrid = new InfoFilterHybrid ( "Album: Regina Spektor - 2006 - Begin to Hope" );
		hybrid.setFilteredTable( table );

		BorderPane content = new BorderPane();
		content.setTop( hybrid );
		content.setCenter( table );
		
		content.setOnKeyPressed( ( KeyEvent e ) -> { 
			if ( e.getCode() == KeyCode.F 
			&& !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown() ) {
				hybrid.beginEditing();
				e.consume();
			}
		});
		
		Scene scene = new Scene ( content );
		
		mainStage.setScene( scene );
		mainStage.setWidth( 500 );
		mainStage.setHeight( 500 );
		mainStage.show();
	}
	
	public static void main ( String [] args ) {
		launch( args );
	}
}

