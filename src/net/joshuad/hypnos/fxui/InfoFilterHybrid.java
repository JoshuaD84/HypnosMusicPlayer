package net.joshuad.hypnos.fxui;

import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import net.joshuad.hypnos.Hypnos;

public class InfoFilterHybrid extends BorderPane {
	private static final Logger LOGGER = Logger.getLogger( LibraryArtistTab.class.getName() );
	private Label info;
	private TextField filter;
	
	private HBox filterBox;
	private Button clearButton;
	
	private TableView <?> table;
	
	private boolean hasHover = false;
	private boolean editing = false;
	private boolean hasFocus = false;
	
	private boolean switchOnHover = false; 
	
	private boolean registeredSceneFilter = false;
	
	private ImageView filterClearImage;
	
	private FXUI ui;
	
	//Used for the text box and for the associated filtered tableview
	private EventHandler <? super KeyEvent> textBoxKeyFilter = ( KeyEvent e ) -> { 
		if ( e.getCode() == KeyCode.ESCAPE ) {
			if ( getText().length() > 0 ) {
				e.consume();
				clearFilterText();
			} else {
				e.consume();
				stopEditing();
			}
		}
	};
	
	private EventHandler <KeyEvent> tableKeyFilter = ( KeyEvent e ) -> { 
		if ( e.getCode() == KeyCode.ESCAPE ) {
			stopEditing();
			e.consume();
		}
	};
	
	public InfoFilterHybrid( FXUI ui ) {
		super();
		
		this.ui = ui;
		
		info = new Label ( );
		filter = new TextField ();
		
		clearButton = new Button ();		
		try {	
			Image clearImage = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources/clear.png" ).toFile() ) );
			filterClearImage = new ImageView ( clearImage );
			filterClearImage.setFitWidth( 12 );
			filterClearImage.setFitHeight( 12 );
			clearButton.setGraphic( filterClearImage );
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to load clear icon: resources/clear.png", e );
			clearButton.setText( "X" ); //TODO: Do this in other places that have a clear button or an add button, why not? 
		}
		
		clearButton.setOnAction( ( ActionEvent e ) -> {
			stopEditing();
		});

		filterBox = new HBox();
		filterBox.getChildren().addAll( filter, clearButton );
		filter.prefWidthProperty().bind( filterBox.widthProperty().subtract( clearButton.widthProperty() ) );

		//info.setPrefHeight( 30 );
		info.setAlignment( Pos.CENTER );
		//filter.setPrefHeight( 30 );
		
		filter.addEventFilter( KeyEvent.KEY_PRESSED, textBoxKeyFilter );
		
		setCenter ( info );
		
		hoverProperty().addListener( ( observable, hadHover, hasHover ) -> {
			this.hasHover = hasHover;
			updateDisplay();
		});
		
		focusedProperty().addListener( ( observable, hadFocus, hasFocus ) -> {
			this.hasFocus = hasFocus;
			updateDisplay();
		});
		
		layoutBoundsProperty().addListener( ( observable, wasVisible, isVisible ) -> {
			//We have to do this now instead of in the constructor because the scene is null 
			//during construction. This may not be the best place, but it works for me. 
			if ( !registeredSceneFilter ) {
				registeredSceneFilter = true;
				this.getScene().addEventFilter( MouseEvent.MOUSE_CLICKED, evt -> {
			        if ( !inHierarchy( evt.getPickResult().getIntersectedNode(), this ) ) {
			        	mouseClickedSomewhereElse();
			        }
			    });
			}
		});
		
		setOnMouseClicked ( ( MouseEvent e ) -> beginEditing() );
		filter.setOnMouseClicked ( ( MouseEvent e ) -> beginEditing() );
		info.setOnMouseClicked ( ( MouseEvent e ) -> beginEditing() );
	}
	
	public void applyButtonColorAdjust ( ColorAdjust buttonColor ) {
		if ( filterClearImage != null ) filterClearImage.setEffect( buttonColor );
	}

	
	public String getText () {
		return filter.getText();
	}
	
	public void setText ( String string ) {
		if ( string == null ) string = "";
		filter.setText( string );
		editing = false;
		updateDisplay();
	}
	
	public void clearFilterText() {
		filter.clear();
		updateDisplay();
	}
	
	public void setInfoText ( String text ) {
		info.setText( text );
	}
	
	public boolean isFilterMode() {
		if ( getCenter() == filter ) return true;
		else return false;
	}
	
	public void beginEditing() {
		editing = true;
		updateDisplay();
		this.requestFocus();
		filter.requestFocus();
	}
	
	public void stopEditing() {
		editing = false;
		hasHover = false;
		hasFocus = false;
		filter.clear();
		updateDisplay();
		info.requestFocus();
	}
	
	public StringProperty textProperty() {
		return filter.textProperty();
	}
		
	private void updateDisplay() {
		if ( ( hasHover && switchOnHover ) || editing || hasFocus || filter.getText().length() > 0 ) {
			setCenter( filterBox );
		} else {
			setCenter( info );
		}
	}
	
	public TextField getFilter() {
		return filter;
	}
	
	public void mouseClickedSomewhereElse() {
		if ( filter.getText().length() == 0 ) editing = false;
		hasFocus = false;
		updateDisplay();
	}
	
	public boolean inHierarchy( Node node, Node potentialHierarchyElement ) {
	    if (potentialHierarchyElement == null) {
	        return true;
	    }
	    while (node != null) {
	        if (node == potentialHierarchyElement) {
	            return true;
	        }
	        node = node.getParent();
	    }
	    return false;
	}
	
	public void setFilteredTable ( TableView <?> table ) {
		if ( table != null ) {
			table.removeEventFilter( KeyEvent.KEY_PRESSED, tableKeyFilter );
		}

		this.table = table;
		
		table.addEventFilter( KeyEvent.KEY_PRESSED, tableKeyFilter );
	}

}