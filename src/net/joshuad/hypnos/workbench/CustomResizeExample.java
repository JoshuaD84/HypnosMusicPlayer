	package net.joshuad.hypnos.workbench;
	 
	import java.util.List;
	import java.util.Locale;
	import java.util.Set;
	
	import javafx.application.Application;
	import javafx.beans.property.SimpleObjectProperty;
	import javafx.collections.FXCollections;
	import javafx.geometry.Orientation;
	import javafx.scene.Node;
	import javafx.scene.Parent;
	import javafx.scene.Scene;
	import javafx.scene.control.ScrollBar;
	import javafx.scene.control.TableColumn;
	import javafx.scene.control.TableView;
	import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
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
		
		@SuppressWarnings("unchecked")
		@Override
		public Boolean call ( TableView.ResizeFeatures feature ) {
			
			TableView table = feature.getTable();
			List <TableColumn> columns = table.getVisibleLeafColumns();
			double widthAvailable = table.getWidth() - getScrollbarWidth ( table );
			
			Border border = table.getBorder();
			
			if ( border != null && border.getInsets() != null ) {
				widthAvailable = table.getBorder().getInsets().getLeft() - table.getBorder().getInsets().getRight();
			}
				
			
			Region region = null;
			Set<Node> nodes = table.lookupAll(".clipped-container");
			for (Node node : nodes) {
			    if (node instanceof Region) {
			        region = (Region) node;
			    }
			}
			for (TableColumn column : columns ) {
			    column.setPrefWidth(region.getWidth() / table.getColumns().size());
			}
			
			/*
			double forEachColumn = widthAvailable / columns.size();
			
			for ( TableColumn column : columns ) {
				// This is depreciated, but if you look at Java's internal
				// resize policies, they also use this to set width. 
				//column.impl_setWidth( forEachColumn );
				column.setMinWidth( forEachColumn );
				column.setMaxWidth( forEachColumn );
			}*/
			
			return true;
			
		}
		
		private double getScrollbarWidth ( TableView table ) {
			double scrollBarWidth = 0;
			Set <Node> nodes = table.lookupAll( ".scroll-bar" );
			for ( final Node node : nodes ) {
				if ( node instanceof ScrollBar ) {
					ScrollBar sb = (ScrollBar) node;
					if ( sb.getOrientation() == Orientation.VERTICAL ) {
						if ( sb.isVisible() ) {
							scrollBarWidth = sb.getWidth();
						}
					}
				}
			}
			
			return scrollBarWidth;
		}
	}
