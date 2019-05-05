package net.joshuad.hypnos.fxui;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.text.TextFlow;
import net.joshuad.hypnos.library.AlbumInfoSource;

public class FormattedAlbumCell<T extends AlbumInfoSource> extends TableCell<T, String> {

	private TextFlow flow;
	private Label albumName, albumType, albumDisc;

	public FormattedAlbumCell () {
		albumName = new Label();
		albumType = new Label();
		albumDisc = new Label();

		albumName.getStyleClass().add( "tableAlbumName" );
		albumDisc.getStyleClass().add( "tableAlbumDisc" );
		albumType.getStyleClass().add( "tableAlbumType" );

		flow = new TextFlow( albumName, albumDisc, albumType ) {

			@Override
			protected double computePrefHeight ( double width ) {
				// quick hack to force into single line
				return super.computePrefHeight( -1 );
			}
		};

		setContentDisplay( ContentDisplay.GRAPHIC_ONLY );
		flow.setMinWidth( Double.MAX_VALUE );
		setGraphic ( flow );
	}
	
	@Override
	protected void updateItem ( String text, boolean empty ) {
		super.updateItem ( text, empty );
		
		TableRow <T> row = this.getTableRow();
		AlbumInfoSource item = null;
		
		if ( row != null ) item = row.getItem();
		
		if ( empty || text == null || row == null || item == null ) {
			albumName.setText ( "" );
			albumType.setText ( "" );
			albumDisc.setText ( "" );
			this.setText ( null );
			
		} else {
			String title = item.getAlbumTitle();
			String releaseType = item.getReleaseType();
			String discSubtitle = item.getDiscSubtitle();
			Integer discCount = item.getDiscCount();
			Integer discNumber = item.getDiscNumber();
			
			this.setText ( text );
			
			albumName.setText( title );
			
			if ( releaseType != null && !releaseType.equals( "" ) ) {
				albumType.setText ( " [" + releaseType + "]" );
			} else {
				albumType.setText( "" );
			}
			
			if ( discSubtitle != null && !discSubtitle.equals( "" ) ) {
				albumDisc.setText(  " (" + discSubtitle + ")" );
				
			} else if ( discCount == null && discNumber != null && discNumber > 1 ) {
				albumDisc.setText(  " (Disc " + discNumber + ")" );
				
			} else if ( discCount != null && discCount > 1 && discNumber != null ) {
				albumDisc.setText(  " (Disc " + discNumber + ")" );
				
			} else {
				albumDisc.setText( "" );
			}
		}
	}
}