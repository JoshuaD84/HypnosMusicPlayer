package net.joshuad.hypnos.test;

import java.util.Locale;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class TableFormattedCell extends Application {

	public static class MyCell extends TableCell <Locale, Locale> {

		private TextFlow flow;
		private Label displayName;
		private Label displayLanguage;

		public MyCell () {
			displayName = new Label();
			displayName.setStyle( "-fx-font-weight: bold" );
			displayLanguage = new Label();
			displayLanguage.setStyle( "-fx-font-style: italic; -fx-text-fill: darkviolet" );
			flow = new TextFlow( displayName, displayLanguage ) {

				@Override
				protected double computePrefHeight ( double width ) {
					// quick hack to force into single line ...
					// there must be something better ..
					return super.computePrefHeight( -1 );
				}

			};

			setContentDisplay( ContentDisplay.GRAPHIC_ONLY );
			setGraphic( flow );
			flow.setMinWidth( Double.MAX_VALUE );
		}

		@Override
		protected void updateItem ( Locale item, boolean empty ) {
			super.updateItem( item, empty );
			if ( empty || item == null ) {
				displayName.setText( "" );
				displayLanguage.setText( "" );
			} else {
				displayName.setText( item.getDisplayName() + " " );
				displayLanguage.setText( item.getDisplayLanguage() );
			}
		}
	}

	private Parent getContent () {
		TableView <Locale> table = new TableView <>( FXCollections.observableArrayList( Locale.getAvailableLocales() ) );
		table.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		
		TableColumn <Locale, String> countryCode = new TableColumn <>( "CountryCode" );
		countryCode.setCellValueFactory( new PropertyValueFactory <>( "country" ) );
		
		TableColumn <Locale, String> language = new TableColumn <>( "Language" );
		language.setCellValueFactory( new PropertyValueFactory <>( "language" ) );
		table.getColumns().addAll( countryCode, language );

		
		
		TableColumn <Locale, Locale> local = new TableColumn <>( "Locale" );
		local.setCellValueFactory( c -> new SimpleObjectProperty <>( c.getValue() ) );
		local.setCellFactory( e -> new MyCell() );
		table.getColumns().addAll( local );


		
		
		
		BorderPane pane = new BorderPane( table );
		return pane;
	}

	@Override
	public void start ( Stage primaryStage ) throws Exception {
		primaryStage.setScene( new Scene( getContent(), 800, 400 ) );
		primaryStage.show();
	}

	public static void main ( String[] args ) {
		launch( args );
	}

	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.getLogger( TableFormattedCell.class.getName() );
}