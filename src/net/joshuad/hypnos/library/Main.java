package net.joshuad.hypnos.library;

import java.nio.file.Paths;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

	public static void main ( String[] args ) {
		launch( args );
	}

	@Override
	public void start( Stage stage ) throws Exception {
		
		stage.setTitle("Hello World!");
        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction(new EventHandler<ActionEvent>() {
 
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Hello World!");
            }
        });
        
        StackPane root = new StackPane();
        root.getChildren().add(btn);
        stage.setScene(new Scene(root, 300, 250));
        stage.show();
        
		Library library = new Library();
		library.loader.start();
		library.merger.start();
		library.loader.addMusicRoot( Paths.get ( "D:\\music" ) );

        
	}
}
