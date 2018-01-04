package net.joshuad.hypnos.fxui;

import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;

public class InfoFilterHybrid extends BorderPane {
	private Label info;
	private TextField filter;
	
	private TableView <?> table;
	
	private boolean hasHover = false;
	private boolean editing = false;
	private boolean hasFocus = false;
	
	private boolean switchOnHover = false; 
	
	private boolean registeredSceneFilter = false;
	
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
	
	public InfoFilterHybrid( String text ) {
		super();
		
		info = new Label ( text );
		filter = new TextField ();

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
	
	public String getText () {
		return filter.getText();
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
			setCenter( filter );
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