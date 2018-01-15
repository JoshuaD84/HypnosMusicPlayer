package net.joshuad.hypnos.workbench;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class NoDecorationStage extends Application {
	
	private double borderWidth = 3;
	
	private double beginDragXOffset = 0;
	private double beginDragYOffset = 0;
	
	
	
	@Override
	public void start( Stage stage ) throws Exception {
		stage.initStyle( StageStyle.TRANSPARENT );
		
		Pane topLeft = new Pane();
		Pane top = new Pane();
		Pane topRight = new Pane();
		Pane left = new Pane();
		Pane right = new Pane();
		Pane bottomLeft = new Pane();
		Pane bottom = new Pane();
		Pane bottomRight = new Pane();
		
		topLeft.setMaxWidth( borderWidth );
		topLeft.setMinWidth( borderWidth );
		topLeft.setMaxHeight( borderWidth );
		topLeft.setMinHeight ( borderWidth );
		
		topRight.setMaxWidth( borderWidth );
		topRight.setMaxHeight( borderWidth );
		topRight.setMinWidth( borderWidth );
		topRight.setMinHeight( borderWidth );
		
		bottomLeft.setMaxWidth( borderWidth );
		bottomLeft.setMaxHeight( borderWidth );
		bottomLeft.setMinWidth( borderWidth );
		bottomLeft.setMinHeight( borderWidth );
		
		bottomRight.setMaxWidth( borderWidth );
		bottomRight.setMaxHeight( borderWidth );
		bottomRight.setMinWidth( borderWidth );
		bottomRight.setMinHeight( borderWidth );

		top.setMaxHeight( borderWidth );
		top.setMinHeight( borderWidth );
		left.setMaxWidth( borderWidth );
		left.setMinWidth( borderWidth );
		right.setMaxWidth( borderWidth );
		right.setMinWidth( borderWidth );
		bottom.setMaxHeight( borderWidth );
		bottom.setMinHeight( borderWidth );
		
		
		BorderPane mainPane = new BorderPane();
		
		mainPane.setTop( new SimpleTitleBar( stage, "Hypnos" ) );
		mainPane.setStyle( "-fx-background-color: blue"  );
		
		GridPane grid = new GridPane();
		grid.add( topLeft, 		0, 	0 );
		grid.add( top, 			1, 	0 );
		grid.add( topRight,		2,	0 );
		grid.add( left, 		0,	1 );
		grid.add( mainPane,		1,	1 );
		grid.add( right, 		2,	1 );
		grid.add( bottomLeft, 	0,	2 );
		grid.add( bottom, 		1,	2 );
		grid.add( bottomRight,	2,	2 );
		
		grid.prefWidthProperty().bind( stage.widthProperty() );
		grid.prefHeightProperty().bind( stage.heightProperty() );
		mainPane.prefWidthProperty().bind( grid.widthProperty().subtract( borderWidth * 2 ) );
		mainPane.prefHeightProperty().bind( grid.widthProperty().subtract( borderWidth * 2 ) );
		
		Scene scene = new Scene ( grid, 800, 600 );
		
		topLeft.hoverProperty().addListener( ( observable, oldValue, newValue ) -> {
			if ( newValue ) scene.setCursor( Cursor.NW_RESIZE );
			else scene.setCursor( Cursor.DEFAULT );
		});
		
		top.hoverProperty().addListener( ( observable, oldValue, newValue ) -> {
			if ( newValue ) scene.setCursor( Cursor.N_RESIZE );
			else scene.setCursor( Cursor.DEFAULT );
		});
		
		topRight.hoverProperty().addListener( ( observable, oldValue, newValue ) -> {
			if ( newValue ) scene.setCursor( Cursor.NE_RESIZE );
			else scene.setCursor( Cursor.DEFAULT );
		});
		
		left.hoverProperty().addListener( ( observable, oldValue, newValue ) -> {
			if ( newValue ) scene.setCursor( Cursor.W_RESIZE );
			else scene.setCursor( Cursor.DEFAULT );
		});

		left.setOnMouseDragged( (MouseEvent e) -> {
			stage.setX( stage.getX() + e.getSceneX() );
			stage.setWidth( stage.getWidth() - e.getSceneX() );
		});
		
		right.hoverProperty().addListener( ( observable, oldValue, newValue ) -> {
			if ( newValue ) scene.setCursor( Cursor.E_RESIZE );
			else scene.setCursor( Cursor.DEFAULT );
		});
		
		bottomLeft.hoverProperty().addListener( ( observable, oldValue, newValue ) -> {
			if ( newValue ) scene.setCursor( Cursor.SW_RESIZE );
			else scene.setCursor( Cursor.DEFAULT );
		});
		
		bottom.hoverProperty().addListener( ( observable, oldValue, newValue ) -> {
			if ( newValue ) scene.setCursor( Cursor.S_RESIZE );
			else scene.setCursor( Cursor.DEFAULT );
		});
		
		bottomRight.hoverProperty().addListener( ( observable, oldValue, newValue ) -> {
			if ( newValue ) scene.setCursor( Cursor.SE_RESIZE );
			else scene.setCursor( Cursor.DEFAULT );
		});
		
		stage.setScene( scene );
		stage.show();
	}
	
	public static void main( String[] args ) {
		launch( args );
	}

}

class SimpleTitleBar extends BorderPane {
	
	Stage stage;
	
	private double beginDragXOffset = 0;
	private double beginDragYOffset = 0;
	
	public SimpleTitleBar( Stage stage, String name ) {
		this.stage = stage;
		
		Label nameLabel = new Label ( name );
		
		Button minimizeButton = new Button ( "-" );
		Button maximizeButton = new Button ( "□" );
		Button closeButton = new Button ( "X" );
		
		HBox rightPanel = new HBox();
		rightPanel.getChildren().addAll( minimizeButton, maximizeButton, closeButton );

		HBox leftPanel = new HBox();
		leftPanel.prefWidthProperty().bind( rightPanel.widthProperty() );

		setCenter( nameLabel );
		setRight( rightPanel );
		
		setOnMousePressed( (MouseEvent event)-> {
        	beginDragXOffset = event.getSceneX();
        	beginDragYOffset = event.getSceneY();
        });
		
		setOnMouseDragged( (MouseEvent event)-> {
			stage.setX( event.getScreenX() - beginDragXOffset );
			stage.setY( event.getScreenY() - beginDragYOffset );
		});
		
		minimizeButton.setOnAction( (ActionEvent e)-> {
			stage.setIconified( true );
		});
		
		maximizeButton.setOnAction( (ActionEvent e)-> {
			maximizeWindow();
		});
		
		closeButton.setOnAction( (ActionEvent e)-> {
			stage.close();
		});
	}
	
	private void maximizeWindow() {
		ObservableList<Screen> screens = Screen.getScreensForRectangle(new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()));

		System.out.println ( "Setot: " + !stage.isMaximized() );
		stage.setMaximized( !stage.isMaximized() );
		Rectangle2D bounds = screens.get(0).getVisualBounds();
		stage.setX(bounds.getMinX());
		stage.setY(bounds.getMinY());
		stage.setWidth(bounds.getWidth());
		stage.setHeight(bounds.getHeight());
	}
		
		
}
	