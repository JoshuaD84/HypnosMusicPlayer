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
import net.joshuad.hypnos.Hypnos;

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
	
	Vector <TableColumn<?, ?>> noResizeColumns = new Vector <TableColumn<?, ?>> ();
	
	public void registerFixedWidthColumns ( TableColumn <?, ?> ... addMe ) {
		noResizeColumns.addAll ( Arrays.asList ( addMe ) );
	}
	
	public void removeColumn ( TableColumn <?, ?> column ) {
		noResizeColumns.remove ( column );
	}
	
	private double distributeSpaceRatio ( List<TableColumn> columns, double space ) {
		
		double distributed = 0;
		
		double totalWidth = 0;
		for ( TableColumn column : columns ) totalWidth += column.getWidth();
		
		for ( TableColumn column : columns ) {
			double startWidth = column.getWidth(); 
			column.impl_setWidth ( column.getWidth() + space * ( column.getWidth() / totalWidth ) );
			double endWidth = column.getWidth();
			
			distributed += endWidth - startWidth;
		}
		
		return distributed;
	}
	
	@SuppressWarnings({ "unchecked" })
	@Override
	public Boolean call ( TableView.ResizeFeatures feature ) {
		
		if ( feature != null ) {
			
			if ( Hypnos.getUI() == null ) return true;
			if ( Hypnos.getUI().mainStage == null ) return true;
			if ( !Hypnos.getUI().mainStage.isShowing() ) return true;
			
			TableView table = feature.getTable();
			TableColumn columnResized = feature.getColumn();
			List <TableColumn> columns = table.getVisibleLeafColumns();
			
			
			if ( columns.indexOf( columnResized ) == columns.size() - 1 ) return true;
			
			if ( columnResized != null ) {
				columnResized.impl_setWidth( columnResized.getWidth() + feature.getDelta() );
			}
			
			
			//keep trying to get rid of excess space until there is none
			//"keep trying" means abandoning the least important promises, one at a time
			//until there is no excess space.
			double totalToDistribute = table.getWidth() - getScrollbarWidth ( table );
			for ( TableColumn column : columns ) totalToDistribute -= column.getWidth();
			
			int increment = 10;
			while ( Math.abs ( totalToDistribute ) >= 1 ) {

				double excessSpace = totalToDistribute < increment ? totalToDistribute : increment;
				for ( int attempt = 0; attempt < 5; attempt++ ) {
					
					if ( excessSpace != excessSpace || Math.abs( excessSpace ) < 1 ) break;
					
					List<TableColumn> columnsToShareExcessSpace = new ArrayList<>();
					
					for ( TableColumn column : columns ) {
						if ( !column.isResizable() ) continue; //we never break this promise
						if ( attempt < 4 && column == columnResized ) continue;
						
						if ( excessSpace > 0 && column.getWidth() >= column.getPrefWidth() 
						||   excessSpace < 0 && column.getWidth() <= column.getPrefWidth()
						){
							if ( attempt < 3 && noResizeColumns.contains( column ) ) continue;
						}
	
						if ( attempt < 2 && excessSpace < 0 && column.getWidth() == column.getMinWidth() ) continue;
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
					
					double distributed = distributeSpaceRatio ( columnsToShareExcessSpace, excessSpace );
					totalToDistribute -= distributed;
					excessSpace -= distributed;
				}
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
