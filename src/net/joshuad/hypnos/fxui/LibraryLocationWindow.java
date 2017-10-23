package net.joshuad.hypnos.fxui;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import net.joshuad.hypnos.Library;

public class LibraryLocationWindow extends Stage {

	TableView <Path> musicSourceTable;
	
	Library library;

	Scene scene;
	
	public LibraryLocationWindow ( Stage mainStage, Library library ) {
		super();
		this.library = library;
		
		initModality( Modality.NONE );
		initOwner( mainStage );
		setTitle( "Music Search Locations" );
		setWidth( 350 );
		setHeight( 500 );
		Pane root = new Pane();
		scene = new Scene( root );
		VBox primaryPane = new VBox();

		musicSourceTable = new TableView<Path> ();
		Label emptyLabel = new Label( "No directories in your library. Either '+ Add' or drop directories here." );
		emptyLabel.setPadding( new Insets( 20, 10, 20, 10 ) );
		emptyLabel.setWrapText( true );
		emptyLabel.setTextAlignment( TextAlignment.CENTER );

		musicSourceTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		musicSourceTable.setPlaceholder( emptyLabel );
		musicSourceTable.setItems( library.getMusicSourcePaths() );
		musicSourceTable.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		musicSourceTable.widthProperty().addListener( new ChangeListener <Number>() {
			@Override
			public void changed ( ObservableValue <? extends Number> source, Number oldWidth, Number newWidth ) {
				Pane header = (Pane) musicSourceTable.lookup( "TableHeaderRow" );
				if ( header.isVisible() ) {
					header.setMaxHeight( 0 );
					header.setMinHeight( 0 );
					header.setPrefHeight( 0 );
					header.setVisible( false );
				}
			}
		} );

		TableColumn <Path, String> dirListColumn = new TableColumn<Path, String> ( "Location" );
		dirListColumn.setCellValueFactory( new Callback <TableColumn.CellDataFeatures <Path, String>, ObservableValue <String>>() {

			@Override
			public ObservableValue <String> call ( TableColumn.CellDataFeatures <Path, String> p ) {
				if ( p.getValue() != null ) {
					return new SimpleStringProperty( p.getValue().toAbsolutePath().toString() );
				} else {
					return new SimpleStringProperty( "<no name>" );
				}
			}
		} );
		
		musicSourceTable.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				List <File> files = db.getFiles();
				
				for ( File file : files ) {
					library.requestAddSource( file.toPath() );
				}

				event.setDropCompleted( true );
				event.consume();
			}
		});
			
		musicSourceTable.getColumns().add( dirListColumn );
		
		musicSourceTable.setOnDragOver( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				event.acceptTransferModes( TransferMode.COPY );
				event.consume();

			}
		});

		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle( "Music Folder" );
		File defaultDirectory = new File( System.getProperty( "user.home" ) ); // PENDING: start windows on desktop maybe.
		chooser.setInitialDirectory( defaultDirectory );

		Button addButton = new Button( "+ Add" );
		Button removeButton = new Button( "- Remove" );

		addButton.setPrefWidth( 100 );
		removeButton.setPrefWidth( 100 );
		addButton.setMinWidth( 100 );
		removeButton.setMinWidth( 100 );

		LibraryLocationWindow me = this;
		addButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				File selectedFile = chooser.showDialog( me );
				if ( selectedFile != null ) {
					library.requestAddSource( selectedFile.toPath() );
				}
			}
		});

		removeButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				library.requestRemoveSources( musicSourceTable.getSelectionModel().getSelectedItems() );
				musicSourceTable.getSelectionModel().clearSelection();	
			}
		});

		musicSourceTable.setOnKeyPressed( new EventHandler <KeyEvent>() {
			@Override
			public void handle ( final KeyEvent keyEvent ) {
				if ( keyEvent.getCode().equals( KeyCode.DELETE ) ) {
					library.requestRemoveSources( musicSourceTable.getSelectionModel().getSelectedItems() );
				}
			}
		});
		
		HBox controlBox = new HBox();
		controlBox.getChildren().addAll( addButton, removeButton);
		controlBox.setAlignment( Pos.CENTER );
		controlBox.prefWidthProperty().bind( widthProperty() );
		controlBox.setPadding( new Insets( 5 ) );
		
		primaryPane.prefWidthProperty().bind( root.widthProperty() );
		primaryPane.prefHeightProperty().bind( root.heightProperty() );
		musicSourceTable.prefHeightProperty().bind( root.heightProperty().subtract( controlBox.heightProperty() ) );

		primaryPane.getChildren().addAll( musicSourceTable, controlBox );
		root.getChildren().add( primaryPane );
		setScene( scene );
	}
}
