package net.joshuad.hypnos.fxui;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.text.TextFlow;
import net.joshuad.hypnos.AlbumInfoSource;

public class FormattedAlbumCell extends TableCell <AlbumInfoSource, String> {

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
		setGraphic( flow );
		flow.setMinWidth( Double.MAX_VALUE );
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void updateItem ( String text, boolean empty ) {
		super.updateItem( text, empty );
		
		TableRow <AlbumInfoSource> row = this.getTableRow();
		AlbumInfoSource item = null;
		
		if ( row != null ) item = row.getItem();
		
		if ( empty || text == null || row == null || item == null ) {
			albumName.setText ( "" );
			albumType.setText ( "" );
			albumDisc.setText ( "" );
			this.setText ( "" );
		} else {
			String title = item.getAlbumTitle();
			String releaseType = item.getReleaseType();
			String discSubtitle = item.getDiscSubtitle();
			Integer discCount = item.getDiscCount();
			Integer discNumber = item.getDiscNumber();

			this.setText ( text );
			
			albumName.setText( title );
			
			if ( !releaseType.equals( "" ) ) {
				albumType.setText ( " [" + releaseType + "]" );
			} else {
				albumType.setText( "" );
			}
			
			if ( !discSubtitle.equals( "" ) ) {
				albumDisc.setText(  " (" + discSubtitle + ")" );
				
			} else if ( ( discCount == null || discCount > 1 ) && discNumber != null ) {
				albumDisc.setText(  " (Disc " + discNumber + ")" );
			} else {
				albumDisc.setText( "" );
			}
		}
	}
}