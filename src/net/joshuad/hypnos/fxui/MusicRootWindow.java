package net.joshuad.hypnos.fxui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
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
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.library.Library;
import net.joshuad.hypnos.library.MusicRoot;
import net.joshuad.hypnos.library.Library.LoaderSpeed;

public class MusicRootWindow extends Stage {
	private static final Logger LOGGER = Logger.getLogger( MusicRootWindow.class.getName() );

	TableView <MusicRoot> musicSourceTable;
	
	Library library;

	Scene scene;
	
	Slider prioritySlider;

	private final ProgressIndicatorBar progressBar;
	
	FXUI ui;
	public MusicRootWindow ( FXUI ui, Stage mainStage, Library library ) {
		super();
		this.ui = ui;
		this.library = library;
		
		initModality( Modality.NONE );
		initOwner( mainStage );
		setTitle( "Music Search Locations" );
		setWidth( 400 );
		setHeight( 500 );
		Pane root = new Pane();
		scene = new Scene( root );
		
		try {
			getIcons().add( new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) ) );
		} catch ( FileNotFoundException e ) {
			LOGGER.warning( "Unable to load program icon: resources/icon.png" );
		}
		
		VBox primaryPane = new VBox();

		musicSourceTable = new TableView<MusicRoot> ();
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

		TableColumn <MusicRoot, String> dirListColumn = new TableColumn<> ( "Location" );
		dirListColumn.setCellValueFactory( 
			new Callback <TableColumn.CellDataFeatures <MusicRoot, String>, ObservableValue <String>>() {

			@Override
			public ObservableValue <String> call ( TableColumn.CellDataFeatures <MusicRoot, String> p ) {
				if ( p.getValue() != null ) {
					return new SimpleStringProperty( p.getValue().getPath().toAbsolutePath().toString() );
				} else {
					return new SimpleStringProperty( "<no name>" );
				}
			}
		} );
		
		musicSourceTable.setRowFactory( tv -> {
			TableRow <MusicRoot> row = new TableRow <>();

			row.itemProperty().addListener( (obs, oldValue, newValue ) -> {
				if ( newValue != null && row != null ) {
			        if ( !newValue.validSearchLocationProperty().get() ) {
			            row.getStyleClass().add( "file-missing" );
			        } else {
			            row.getStyleClass().remove( "file-missing" );
			        }
				}
		    });
			
			row.itemProperty().addListener( ( obs, oldValue, newTrackValue ) -> {
				if ( newTrackValue != null  && row != null ) {
					newTrackValue.validSearchLocationProperty().addListener( ( o, old, newValue ) -> {
						if ( !newValue ) {
							row.getStyleClass().add( "file-missing" );
						} else {
							row.getStyleClass().remove( "file-missing" );
						}
					});
				}
			});
			return row;
		});

		musicSourceTable.setOnDragDropped( event -> {
			Dragboard db = event.getDragboard();
			if ( db.hasFiles() ) {
				List <File> files = db.getFiles();
				
				for ( File file : files ) {
					library.addMusicRoot( file.toPath() );
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

		MusicRootWindow me = this;
		addButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
				File selectedFile = chooser.showDialog( me );
				if ( selectedFile != null ) {
					library.addMusicRoot( selectedFile.toPath() );
				}
			}
		});

		removeButton.setOnAction( new EventHandler <ActionEvent>() {
			@Override
			public void handle ( ActionEvent e ) {
					List <MusicRoot> removeMe = new ArrayList<>(musicSourceTable.getSelectionModel().getSelectedItems());
			    for (MusicRoot musicRoot : removeMe ) {
			    	library.removeMusicRoot( musicRoot );
			    }
			    musicSourceTable.getSelectionModel().clearSelection();	
			}
		});

		musicSourceTable.setOnKeyPressed( new EventHandler <KeyEvent>() {
			@Override
			public void handle ( final KeyEvent keyEvent ) {
				if ( keyEvent.getCode().equals( KeyCode.DELETE ) ) {
					for (MusicRoot musicRoot : musicSourceTable.getSelectionModel().getSelectedItems() ) {
			    	library.removeMusicRoot( musicRoot );
			    }
				}
			}
		});
		
		progressBar = new ProgressIndicatorBar();
		progressBar.prefWidthProperty().bind( widthProperty() );
		progressBar.setPrefHeight( 30 );
		
		HBox controlBox = new HBox();
		controlBox.getChildren().addAll( addButton, removeButton);
		controlBox.setAlignment( Pos.CENTER );
		controlBox.prefWidthProperty().bind( widthProperty() );
		controlBox.setPadding( new Insets( 5 ) );
		
		Label priorityLabel = new Label ( "Scan Speed:" );
		priorityLabel.setTooltip ( new Tooltip ( "How much resources to consume while loading and updating the library.\n(If your computer or hypnos is choppy while loading, try turning this down.)" ) );
		priorityLabel.setPadding( new Insets ( 0, 10, 0, 0 ) );
		
		prioritySlider = new Slider ( 1, 3, 1 );
		prioritySlider.prefWidthProperty().bind( widthProperty().subtract( 150 ) );
		prioritySlider.setShowTickMarks( true );
		prioritySlider.setMinorTickCount( 0 );
		prioritySlider.setMajorTickUnit( 1 );
		prioritySlider.setShowTickLabels( true );
		prioritySlider.setSnapToTicks( true );
		
		prioritySlider.valueProperty().addListener( new ChangeListener <Number>() {
			public void changed ( ObservableValue <? extends Number> oldValueObs, Number oldValue, Number newValue ) {
				switch ( newValue.intValue() ) {
					case 1:
						Hypnos.setLoaderSpeed( LoaderSpeed.LOW );
						break;
					case 2:
						Hypnos.setLoaderSpeed( LoaderSpeed.MED );
						break;
					case 3:
						Hypnos.setLoaderSpeed( LoaderSpeed.HIGH );
						break;
				}
			}
		} );
		
		
		Button libraryLogButton = new Button ( "?" );
		libraryLogButton.setOnAction( e -> ui.libraryLogWindow.show() );
		
		HBox progressBox = new HBox();
		progressBox.getChildren().addAll ( progressBar, libraryLogButton );
		
		HBox priorityBox = new HBox();
		priorityBox.getChildren().addAll( priorityLabel, prioritySlider);
		priorityBox.setAlignment( Pos.CENTER );
		priorityBox.prefWidthProperty().bind( widthProperty() );
		priorityBox.setPadding( new Insets( 5 ) );
		
		primaryPane.prefWidthProperty().bind( root.widthProperty() );
		primaryPane.prefHeightProperty().bind( root.heightProperty() );
		musicSourceTable.prefHeightProperty().bind( 
			root.heightProperty()
			.subtract( controlBox.heightProperty() )
			.subtract( progressBox.heightProperty() )
			.subtract( priorityBox.heightProperty() ) );

		primaryPane.getChildren().addAll( musicSourceTable, progressBox, priorityBox, controlBox );
		root.getChildren().add( primaryPane );
		setScene( scene );
		
		scene.addEventFilter( KeyEvent.KEY_PRESSED, new EventHandler <KeyEvent>() {
			@Override
			public void handle ( KeyEvent e ) {
				if ( e.getCode() == KeyCode.ESCAPE
				&& !e.isControlDown() && !e.isShiftDown() && !e.isMetaDown() && !e.isAltDown() ) {
					hide();
					e.consume();
				}
			}
		});
	}
	
	public void setLoaderSpeedDisplay ( LoaderSpeed speed ) {
		switch ( speed ) {
			case LOW:
				prioritySlider.setValue( 1 );
				break;
			case MED:
				prioritySlider.setValue( 2 );
				break;
			case HIGH:
				prioritySlider.setValue( 3 );
				break;
		}
	}

	public void setLoaderStatus ( String message, double percentDone ) {
		progressBar.setStatus( message, percentDone );
	}

	public void setLibraryLoaderStatusToStandby () {
		setLoaderStatus ( "", 0 );
	}
}
