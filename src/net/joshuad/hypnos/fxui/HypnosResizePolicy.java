package net.joshuad.hypnos.fxui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/* Goals:
 * total width of columns is the width of the table. i.e. columns are full width of table, but not wider
 * 
 * Programmer can register columns, so that they do not change size except when the user directly resizes them
 * 
 * When the table's size is changed, the excess space is shared on a ratio basis between non-registered, resizable columns
 * 
 * If a column is manually resized, the delta is shared on a ratio basis between columns to that resized column's right only. 
 * 
 * There are some narrow-case exceptions to these goals to avoid broken behavior.
 */


@SuppressWarnings({ "rawtypes", "deprecation" })
public class HypnosResizePolicy implements Callback <TableView.ResizeFeatures, Boolean> {
	static final double DEFAULT_MIN_WIDTH = 10.0F, DEFAULT_MAX_WIDTH = 5000.0F;
	
	Vector <TableColumn<?, ?>> noResizeColumns = new Vector <TableColumn<?, ?>> ();
	
	public void registerColumns ( TableColumn <?, ?> ... addMe ) {
		noResizeColumns.addAll ( Arrays.asList ( addMe ) );
	}
	
	public void removeColumn ( TableColumn <?, ?> column ) {
		noResizeColumns.remove ( column );
	}
	
	private void distributeSpace ( List<TableColumn> columns, double space ) {
		
		double totalWidth = 0;
		for ( TableColumn column : columns ) totalWidth += column.getWidth();
		
		for ( TableColumn column : columns ) {
			column.impl_setWidth ( column.getWidth() + space * ( column.getWidth() / totalWidth ) );
		}
	}
	
	@SuppressWarnings({ "unchecked" })
	@Override
	public Boolean call ( TableView.ResizeFeatures feature ) {
		System.out.println ( "Here" );

		if ( feature != null ) {

			TableView table = feature.getTable();
			TableColumn columnResized = feature.getColumn();
			List <TableColumn> columns = table.getVisibleLeafColumns();
			
			if ( columns.indexOf( columnResized ) == columns.size() - 1 ) return true;
			
			if ( columnResized != null ) {
				columnResized.impl_setWidth( columnResized.getWidth() + feature.getDelta() );
			}
			
			
			//keep trying to get rid of excess space until there is none
			//"keep trying" means abandoning the least important promises, one at a time
			//until there is no excess space. We never 
			for ( int attempt = 0; attempt < 5; attempt++ ) {
				double excessSpace = table.getWidth() - getScrollbarWidth ( table );
				for ( TableColumn column : columns ) excessSpace -= column.getWidth();

				if ( excessSpace == 0 ) break;
				
				List<TableColumn> columnsToShareExcessSpace = new ArrayList<>();
				
				for ( TableColumn column : columns ) {
					if ( !column.isResizable() ) continue; //we never break this promise
					if ( attempt < 4 && column == columnResized ) continue;
					if ( attempt < 3 && noResizeColumns.contains( column ) ) continue;
					if ( attempt < 2 && column.getWidth() == column.getMinWidth() ) continue;
					if ( attempt < 1 && columns.indexOf( column ) < columns.indexOf( columnResized ) ) continue;
					
					columnsToShareExcessSpace.add( column );
				}
				
				if ( columnsToShareExcessSpace.size() == 0 ) {
					if ( columnResized == null ) {
						columnsToShareExcessSpace.add( columns.get( columns.size() - 1 ) );
					} else {
						columnsToShareExcessSpace.add( columns.get( columns.indexOf( columnResized ) + 1 ) );
					}
				}
				
				distributeSpace ( columnsToShareExcessSpace, excessSpace );
			}
			
			return true;
		}
		
		return false;
	}
	
	private double getScrollbarWidth ( TableView table ) {
		double scrollBarWidth = 0;
		Set <Node> nodes = table.lookupAll( ".scroll-bar" );
		for ( final Node node : nodes ) {
			if ( node instanceof ScrollBar ) {
				ScrollBar sb = (ScrollBar) node;
				if ( sb.getOrientation() == Orientation.VERTICAL ) {
					if ( sb.isVisible() ) {
						scrollBarWidth = sb.getWidth() + 4;
					}
				}
			}
		}
		
		return scrollBarWidth;
	}
}
