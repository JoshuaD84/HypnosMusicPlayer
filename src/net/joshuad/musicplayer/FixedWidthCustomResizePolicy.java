package net.joshuad.musicplayer;

import java.util.Arrays;
import java.util.Vector;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

//TODO: this doesn't work perfectly for last columns. 

@SuppressWarnings ( "rawtypes" ) //TODO: I'm not sure why this one is firing or how to fix it yet.
public class FixedWidthCustomResizePolicy implements Callback <TableView.ResizeFeatures, Boolean> {
	static final double DEFAULT_MIN_WIDTH = 10.0F, DEFAULT_MAX_WIDTH = 5000.0F;
	
	Vector <TableColumn<?, ?>> columns = new Vector <TableColumn<?, ?>> ();
	
	public void registerColumns ( TableColumn <?, ?> ... addMe ) {
		columns.addAll ( Arrays.asList ( addMe ) );
		
		for ( TableColumn <?, ?> column : addMe ) {
			column.setMaxWidth ( column.getWidth () );
			column.setMinWidth ( column.getWidth () );
		}
	}
	
	public void removeColumn ( TableColumn <?, ?> column ) {
		columns.remove ( column );
	}

	@Override
	public Boolean call ( TableView.ResizeFeatures feature ) {
		
		final TableColumn <?, ?> c = feature.getColumn ();
		
		if ( columns.contains ( c ) ) {
			c.setMinWidth ( DEFAULT_MIN_WIDTH );
			c.setMaxWidth ( DEFAULT_MAX_WIDTH );
		}
		boolean result = TableView.CONSTRAINED_RESIZE_POLICY.call ( feature );
		if ( columns.contains ( c ) ) {
			c.setMinWidth ( c.getWidth () );
			c.setMaxWidth ( c.getWidth () );
		}
		return result;
	}
}
