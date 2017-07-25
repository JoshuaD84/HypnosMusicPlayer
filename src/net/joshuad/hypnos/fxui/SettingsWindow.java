package net.joshuad.hypnos.fxui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumMap;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Library;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys.Hotkey;
import net.joshuad.hypnos.TagError;

public class SettingsWindow extends Stage {
	
	private FXUI ui;
	private Library library;
	private GlobalHotkeys hotkeys;
	
	private TabPane tabPane;
	private Tab globalHotkeysTab;
	
	private EnumMap <Hotkey, TextField> hotkeyFields = new EnumMap <Hotkey, TextField> ( Hotkey.class );
	
	SettingsWindow( FXUI ui, Library library, GlobalHotkeys hotkeys ) {
		super();
		
		this.ui = ui;
		this.library = library;
		this.hotkeys = hotkeys;

		initModality( Modality.NONE );
		initOwner( ui.getMainStage() );		
		setWidth ( 500 );
		setHeight ( 600 );
		setTitle( "Config and Info" );
		Pane root = new Pane();
		Scene scene = new Scene( root );
		
		tabPane = new TabPane();
		
		
		Tab settingsTab = setupSettingsTab( root );
		globalHotkeysTab = setupGlobalHotkeysTab ( root );
		Tab logTab = setupLogTab( root );
		Tab tagTab = setupTagTab ( root );
		Tab aboutTab = setupAboutTab( root ); 
		
		tabPane.getTabs().addAll( settingsTab, globalHotkeysTab, logTab, tagTab, aboutTab );
		
		tabPane.prefWidthProperty().bind( root.widthProperty() );
		tabPane.prefHeightProperty().bind( root.heightProperty() );

		
		VBox primaryPane = new VBox();
		primaryPane.getChildren().addAll( tabPane );
		root.getChildren().add( primaryPane );
		setScene( scene );
		refreshHotkeyFields();
	}	
	
	private Tab setupGlobalHotkeysTab ( Pane root ) {
		GridPane content = new GridPane();
		content.setAlignment( Pos.TOP_CENTER );
		content.setPadding( new Insets ( 10 ) );
		content.setVgap( 2 );
		
		Tab hotkeysTab = new Tab ( "Global Hotkeys" );
		hotkeysTab.setClosable( false );
		hotkeysTab.setContent( content );
		
		int row = 0;
	
		Label headerLabel = new Label ( "Global Hotkeys" );
		headerLabel.setPadding( new Insets ( 0, 0, 10, 0 ) );
		headerLabel.setWrapText( true );
		headerLabel.setTextAlignment( TextAlignment.CENTER );
		headerLabel.setStyle( "-fx-alignment: center; -fx-font-size: 20px; -fx-font-weight: bold" );
		content.add( headerLabel, 0, row, 2, 1 );
		GridPane.setHalignment( headerLabel, HPos.CENTER );
		row++;
		
		Label descriptionLabel = new Label ( "These hotkeys will also work when Hypnos is minimized or out of focus." );
		descriptionLabel.setPadding( new Insets ( 0, 0, 20, 0 ) );
		descriptionLabel.setWrapText( true );
		descriptionLabel.setTextAlignment( TextAlignment.CENTER );
		content.add( descriptionLabel, 0, row, 2, 1 );
		GridPane.setHalignment( descriptionLabel, HPos.CENTER );
		row++;
		
		for ( Hotkey key : Hotkey.values() ) {
			Label label = new Label ( key.getLabel() );
			label.setStyle( "-fx-alignment: center" );
			label.setPadding( new Insets ( 0, 20, 0, 0 ) );
			
			TextField field = new TextField ();
			field.setStyle( "-fx-alignment: center" );
			field.setPrefWidth( 200 );
			
			field.setOnKeyPressed( ( KeyEvent e ) -> { 
				
				String shortcut = "";
				if ( e.isControlDown() ) shortcut += "Ctrl + ";
				if ( e.isAltDown() ) shortcut += "Alt + ";
				if ( e.isShiftDown() ) shortcut += "Shift + ";
				if ( e.isMetaDown() ) shortcut += "Meta + ";
				
				boolean registered = false;
				if ( e.getCode().isModifierKey() ) {
					field.setText( shortcut );
					
				} else if ( e.getCode().equals( KeyCode.ESCAPE ) ) {
					field.setText( "" );
					hotkeys.clearHotkey ( key );
				
				} else {
					shortcut += e.getCode().getName();
					registered = hotkeys.registerLastCombination( key, shortcut );
					
					if ( registered ) {
						field.setText( shortcut );
						refreshHotkeyFields();
					}
				}

				field.positionCaret( shortcut.length() );
				
				e.consume();
			});
			
			field.setOnKeyTyped( ( KeyEvent e ) -> {
				e.consume();
			});
			
			field.setOnKeyReleased( ( KeyEvent e ) -> {
				refreshHotkeyFields();
			});
			
			content.add( label, 0, row );
			content.add( field, 1, row );
			
			hotkeyFields.put( key, field );
			
			row++;
		}
		
		Label clearLabel = new Label ( "(Use <ESC> to erase a hotkey)" );
		clearLabel.setPadding( new Insets ( 20, 0, 0, 0 ) );
		clearLabel.setWrapText( true );
		clearLabel.setTextAlignment( TextAlignment.CENTER );
		content.add( clearLabel, 0, row, 2, 1 );
		GridPane.setHalignment( clearLabel, HPos.CENTER );
		row++;
		
		return hotkeysTab;
	}
	
	public void refreshHotkeyFields() {
		for ( Hotkey key : Hotkey.values() ) {
			TextField field = hotkeyFields.get( key );
			if ( field == null ) continue;
			
			field.setText( hotkeys.getDisplay( key ) );
		}
	}
	
	private Tab setupSettingsTab ( Pane root ) {
		Tab settingsTab = new Tab ( "Settings" );
		settingsTab.setClosable( false );
		VBox settingsPane = new VBox();
		settingsTab.setContent ( settingsPane );
		settingsPane.setAlignment( Pos.TOP_CENTER );		
		settingsPane.setPadding( new Insets ( 10 ) );
		
		Label headerLabel = new Label ( "Settings" );
		headerLabel.setPadding( new Insets ( 0, 0, 10, 0 ) );
		headerLabel.setWrapText( true );
		headerLabel.setTextAlignment( TextAlignment.CENTER );
		headerLabel.setStyle( "-fx-font-size: 20px; -fx-font-weight: bold" );
		settingsPane.getChildren().add( headerLabel );

		Insets labelInsets = new Insets ( 10, 10, 10, 10 );
		Insets checkBoxInsets = new Insets ( 10, 10, 10, 10 );
		
		Label warnLabel = new Label ( "Warn before erasing unsaved playlists" );
		warnLabel.setPadding( labelInsets );
		
		CheckBox warnCheckBox = new CheckBox ();
		warnCheckBox.setPadding( checkBoxInsets );
		
		warnCheckBox.selectedProperty().bindBidirectional( ui.promptBeforeOverwriteProperty() );
		
		warnCheckBox.selectedProperty().addListener( (obs, wasSelected, isNowSelected) -> {
	    	ui.setPromptBeforeOverwrite( isNowSelected );
		});
				
		HBox warnBox = new HBox();
		warnBox.setAlignment( Pos.CENTER );
		warnBox.getChildren().addAll( warnLabel, warnCheckBox );
		
		GridPane shuffleGrid = new GridPane();
		
		final ObservableList<String> shuffleOptions = FXCollections.observableArrayList( "No Change", "Sequential", "Shuffle" );
		int row = 0;
		Label albumsLabel = new Label ( "Default shuffle setting for albums:" );
		albumsLabel.setPadding( new Insets ( 0, 10, 0, 0 ) );
		shuffleGrid.add ( albumsLabel, 0, row );
		ChoiceBox <String> albumChoices = new ChoiceBox <String>( shuffleOptions );
		shuffleGrid.add ( albumChoices, 1, row );
		albumChoices.getSelectionModel().select( 1 );
		row++;
		
		Label trackLabel = new Label ( "Default shuffle setting for tracks:" );
		trackLabel.setPadding( new Insets ( 0, 10, 0, 0 ) );
		shuffleGrid.add ( trackLabel, 0, row );
		ChoiceBox <String> trackChoices = new ChoiceBox <String>( shuffleOptions );
		shuffleGrid.add ( trackChoices, 1, row );
		trackChoices.getSelectionModel().select( 0 );
		row++;
		
		Label playlistLabel = new Label ( "Default shuffle setting for playlists:" );
		playlistLabel.setPadding( new Insets ( 0, 10, 0, 0 ) );
		shuffleGrid.add ( playlistLabel, 0, row );
		ChoiceBox <String> playlistChoices = new ChoiceBox <String>( shuffleOptions );
		shuffleGrid.add ( playlistChoices, 1, row );
		playlistChoices.getSelectionModel().select( 2 );
		row++;

		settingsPane.getChildren().addAll( warnBox, shuffleGrid );
		
		return settingsTab;
	}
	
	private Tab setupLogTab( Pane root ) {
		Tab logTab = new Tab ( "Log" );
		logTab.setClosable( false );
		VBox logPane = new VBox();
		logTab.setContent( logPane );
		logPane.setAlignment( Pos.CENTER );
		logPane.setPadding( new Insets ( 10 ) );
		
		Label headerLabel = new Label ( "Error Log" );
		headerLabel.setPadding( new Insets ( 0, 0, 10, 0 ) );
		headerLabel.setWrapText( true );
		headerLabel.setTextAlignment( TextAlignment.CENTER );
		headerLabel.setStyle( "-fx-font-size: 20px; -fx-font-weight: bold" );
		logPane.getChildren().add( headerLabel );
		
		TextArea logView = new TextArea();
		logView.setEditable( false );
		logView.prefHeightProperty().bind( root.heightProperty() );
		logView.setWrapText( true );
		
		Thread logReader = new Thread( () -> {
			try ( 
				BufferedReader reader = new BufferedReader( new FileReader( Hypnos.getRootDirectory().resolve( "hypnos.log" ).toFile() ) ); 
			){
				while ( true ) {
					String line = reader.readLine();
					if ( line == null ) {
						try {
							Thread.sleep( 1000 );
						} catch ( InterruptedException ie ) {
							// TODO:
						}
					} else {
						Platform.runLater( () -> {
							logView.appendText( line + "\n" );

							if ( line.matches( "^[A-Z]+:.*" ) ) {
								logView.appendText( "\n" );
							}
						});
					}
				}
			} catch ( FileNotFoundException ex ) {
				System.err.println( ex );
				// TODO:
			} catch ( IOException e ) {
				e.printStackTrace();
				// TODO:
			}
		});
		
		logReader.setDaemon( true );
		logReader.start();
		 
		logPane.getChildren().add( logView );
		
		return logTab;
	}
	
	@SuppressWarnings("unchecked")
	private Tab setupTagTab ( Pane root ) {
		Tab tab = new Tab ( "Tags" );
		tab.setClosable( false );
		VBox pane = new VBox();
		tab.setContent ( pane );
		pane.setAlignment( Pos.TOP_CENTER );
		pane.setPadding( new Insets ( 10 ) );
		
		Label headerLabel = new Label ( "Tag Errors" );
		headerLabel.setPadding( new Insets ( 0, 0, 10, 0 ) );
		headerLabel.setWrapText( true );
		headerLabel.setTextAlignment( TextAlignment.CENTER );
		headerLabel.setStyle( "-fx-font-size: 20px; -fx-font-weight: bold" );
		pane.getChildren().add( headerLabel );

		
		
		TableColumn<TagError, String> pathColumn = new TableColumn<TagError, String> ( "Location" );
		TableColumn<TagError, String> messageColumn = new TableColumn<TagError, String> ( "Error Message" );
		TableColumn<TagError, String> severityColumn = new TableColumn<TagError, String> ( "Severity" );

		pathColumn.setCellValueFactory( new PropertyValueFactory <TagError, String>( "PathDisplay" ) );
		messageColumn.setCellValueFactory( new PropertyValueFactory <TagError, String>( "Message" ) );
		severityColumn.setCellValueFactory( new PropertyValueFactory <TagError, String>( "SeverityDisplay" ) );

		pathColumn.setMaxWidth( 45000 );
		messageColumn.setMaxWidth( 45000 );
		severityColumn.setMaxWidth( 10000 );


		TableView <TagError> table = new TableView <TagError> ();
		table.getColumns().addAll( pathColumn, messageColumn, severityColumn );

		library.getTagErrorsSorted().comparatorProperty().bind( table.comparatorProperty() );
		
		table.setEditable( false );
		table.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
		table.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		table.setItems( library.getTagErrorsSorted() );
		table.prefWidthProperty().bind( pane.widthProperty() );
		table.prefHeightProperty().bind( pane.heightProperty() );
		
		pane.getChildren().addAll( table );
		
		return tab;
	}
	
	private Tab setupAboutTab ( Pane root ) {
		Tab aboutTab = new Tab ( "About" );
		aboutTab.setClosable( false );
		VBox aboutPane = new VBox();
		aboutTab.setContent( aboutPane );
		aboutPane.setStyle( "-fx-background-color: wheat" );
		
		aboutPane.setAlignment( Pos.CENTER );
		Label name = new Label ( "Hypnos Music Player" );
		name.setStyle( "-fx-font-size: 36px" );
		
		Hyperlink website = new Hyperlink ( "http://www.hypnosplayer.org" );
		website.setOnAction( ( ActionEvent e ) -> {
			try {
				new ProcessBuilder("x-www-browser", "http://hypnosplayer.org" ).start();
			} catch ( IOException e1 ) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		website.setStyle( "-fx-font-size: 20px" );

		Label versionNumber = new Label ( " Alpha 2Pre Nightly 2017-07-15" );
		versionNumber.setStyle( "-fx-font-size: 16px" );
		versionNumber.setPadding( new Insets ( 0, 0, 20, 0 ) );
		
		Image image = null;
		try {
			image = new Image( new FileInputStream ( Hypnos.getRootDirectory().resolve( "resources" + File.separator + "icon.png" ).toFile() ) );
		} catch ( FileNotFoundException e1 ) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ImageView logo = new ImageView( image );
		logo.setFitWidth( 200 );
		logo.setPreserveRatio( true );
		logo.setSmooth( true );
		logo.setCache( true );
        
		HBox authorBox = new HBox();
		authorBox.setAlignment( Pos.CENTER );
		authorBox.setPadding ( new Insets ( 20, 0, 0, 0 ) );
		authorBox.setStyle( "-fx-font-size: 16px" );
		Label authorLabel = new Label ( "Author:" );
		Hyperlink authorLink = new Hyperlink ( "Joshua Hartwell" );
		authorLink.setOnAction( ( ActionEvent e ) -> {
			try {
				new ProcessBuilder("x-www-browser", "http://joshuad.net" ).start();
			} catch ( IOException e1 ) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		
		authorBox.getChildren().addAll( authorLabel, authorLink );
		
		
		HBox sourceBox = new HBox();
		sourceBox.setAlignment( Pos.CENTER );
		Label sourceLabel = new Label ( "Source Code:" );
		Hyperlink sourceLink = new Hyperlink ( "GitHub" );
		sourceLink.setOnAction( ( ActionEvent e ) -> {
			try {
				new ProcessBuilder("x-www-browser", "https://github.com/JoshuaD84/HypnosMusicPlayer" ).start();
			} catch ( IOException e1 ) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		

		sourceBox.getChildren().addAll( sourceLabel, sourceLink );
		
		HBox licenseBox = new HBox();
		licenseBox.setAlignment( Pos.CENTER );
		Label licenseLabel = new Label ( "License:" );
		Hyperlink licenseLink = new Hyperlink ( "GNU GPLv3" );
		licenseLink.setOnAction( ( ActionEvent e ) -> {
			try {
				new ProcessBuilder("x-www-browser", "https://www.gnu.org/licenses/gpl-3.0-standalone.html" ).start();
			} catch ( IOException e1 ) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		
		licenseBox.getChildren().addAll( licenseLabel, licenseLink );
		

		aboutPane.getChildren().addAll ( name, website, versionNumber, logo, authorBox, sourceBox, licenseBox);
		
		return aboutTab;
	}

	public boolean hotkeysEnabled () {
		if ( tabPane == null ) return true;
		if ( globalHotkeysTab == null ) return true;
		if ( !this.isShowing() ) return true;
		
		if ( tabPane.getSelectionModel().getSelectedItem().equals( this.globalHotkeysTab ) )  {
			return false;
		}
		
		return true;
	}

}
