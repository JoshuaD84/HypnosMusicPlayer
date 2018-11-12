package net.joshuad.hypnos.workbench;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.fxui.ThrottledTrackFilter;

public class FilterTester extends Application {
	
	final ObservableList <Track> tracks = FXCollections.observableArrayList( new ArrayList <Track>() );
	final FilteredList <Track> tracksFiltered = new FilteredList <Track>( tracks, p -> true );
	final SortedList <Track> tracksSorted = new SortedList <Track>( tracksFiltered );
	
	Path[] trackPaths = new Path[] {
		Paths.get( "/home/joshua/workbench/hypnos-test/music-test/aa/1.flac" ),
		Paths.get( "/home/joshua/workbench/hypnos-test/music-test/aa/2.flac" ),
		Paths.get( "/home/joshua/workbench/hypnos-test/music-test/aa/3.flac" ),
		Paths.get( "/home/joshua/workbench/hypnos-test/music-test/aa/4.flac" ),
		Paths.get( "/home/joshua/workbench/hypnos-test/music-test/aa/5.flac" ),
		Paths.get( "/home/joshua/workbench/hypnos-test/music-test/aa/6.flac" ),
		Paths.get( "/home/joshua/workbench/hypnos-test/music-test/aa/7.flac" ),
		Paths.get( "/home/joshua/workbench/hypnos-test/music-test/aa/8.flac" ),
	};

	public void start ( Stage stage ) throws Exception {
		
		for ( Path path : trackPaths ) {
			tracks.add( new Track ( path ) );
		}
			
		TableColumn<Track, String> trackArtistColumn = new TableColumn<Track, String>( "Artist" );
		TableColumn<Track, Integer> trackLengthColumn = new TableColumn<Track, Integer>( "Length" );
		TableColumn<Track, Integer> trackNumberColumn = new TableColumn<Track, Integer>( "#" );
		TableColumn<Track, String>  trackAlbumColumn = new TableColumn<Track, String>( "Album" );
		TableColumn<Track, String>  trackTitleColumn = new TableColumn<Track, String>( "Title" );
		
		trackArtistColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Artist" ) );
		trackTitleColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "Title" ) );
		trackLengthColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "LengthDisplay" ) );
		trackNumberColumn.setCellValueFactory( new PropertyValueFactory <Track, Integer>( "TrackNumber" ) );
		trackAlbumColumn.setCellValueFactory( new PropertyValueFactory <Track, String>( "albumTitle" ) );
		
		trackArtistColumn.setSortType( TableColumn.SortType.ASCENDING );

		TableView <Track> trackTable = new TableView<Track>();
		trackTable.getColumns().addAll( 
			trackArtistColumn, trackAlbumColumn, trackNumberColumn, trackTitleColumn, trackLengthColumn );
		
		trackTable.setEditable( false );
		trackTable.setItems( tracksSorted );
		trackTable.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
		tracksSorted.comparatorProperty().bind( trackTable.comparatorProperty() );
		
		ThrottledTrackFilter filter = new ThrottledTrackFilter ( tracksFiltered );
		
		TextField filterBox = new TextField ();
		filterBox.textProperty().addListener( new ChangeListener <String> () {
			@Override
			public void changed ( ObservableValue <? extends String> observable, String oldValue, String newValue ) {
				filter.setFilter( newValue, false );
			}
		});
		
		BorderPane pane = new BorderPane( );
		pane.setTop( filterBox );
		pane.setCenter( trackTable );
		stage.setScene( new Scene( pane, 800, 400 ) );
		stage.show();
		
	}
	
	public static void main ( String[] args ) {
		launch ( args );
	}
}
