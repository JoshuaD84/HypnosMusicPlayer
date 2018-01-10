package net.joshuad.hypnos.workbench.volume;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class VolumeWindowsDelay extends Application {

	private static final Logger LOGGER = Logger.getLogger( VolumeWindowsDelay.class.getName() );
	
	private final int BUFFER_SIZE = 128000;

	private File soundFile;

	private AudioInputStream audioInput;

	private AudioFormat audioFormat;

	private SourceDataLine audioOutput;
	
	private String targetFile = "/home/joshua/workbench/hypnos-test/sample.wav";

	@Override
	public void start ( Stage stage ) throws Exception {
		
		Thread playerThread = new Thread ( () -> playSound ( targetFile ) );
		playerThread.setDaemon ( true );
		playerThread.start();
		
		BorderPane mainPane = new BorderPane();
		Slider volumeSlider = new Slider();
		volumeSlider.setMin( 0 );
		volumeSlider.setMax( 100 );
		volumeSlider.setValue( 100 );
		volumeSlider.setPrefWidth( 100 );
		volumeSlider.setMinWidth( 80 );
		volumeSlider.setTooltip( new Tooltip ( "Control Volume" ) );
		volumeSlider.setPadding( new Insets ( 0, 10, 0, 0 ) );
		
		EventHandler<MouseEvent> volumeSliderHandler = new EventHandler<MouseEvent> () {
			@Override
			public void handle ( MouseEvent event ) {
				double min = volumeSlider.getMin();
				double max = volumeSlider.getMax();
				double percent = (volumeSlider.getValue() - min) / (max - min);
				setVolumePercent( percent );
			}
		};
		
		volumeSlider.setOnMouseDragged ( volumeSliderHandler );
		volumeSlider.setOnMouseClicked ( volumeSliderHandler );
		
		mainPane.setCenter( volumeSlider );
		
		Scene scene = new Scene ( mainPane );
		stage.setScene( scene );
		stage.setWidth( 400 );
		stage.setHeight( 100 );
		stage.show();
	}

	public static void main ( String[] args ) {
		
		launch( args );
	}
	
	void playSound ( String filename ) {

		String strFilename = filename;

		try {
			soundFile = new File( strFilename );
		} catch ( Exception e ) {
			e.printStackTrace();
			System.exit( 1 );
		}

		try {
			audioInput = AudioSystem.getAudioInputStream( soundFile );
		} catch ( Exception e ) {
			e.printStackTrace();
			System.exit( 1 );
		}

		audioFormat = audioInput.getFormat();

		DataLine.Info info = new DataLine.Info( SourceDataLine.class, audioFormat );
		try {
			audioOutput = (SourceDataLine) AudioSystem.getLine( info );
			audioOutput.open( audioFormat );
		} catch ( LineUnavailableException e ) {
			e.printStackTrace();
			System.exit( 1 );
		} catch ( Exception e ) {
			e.printStackTrace();
			System.exit( 1 );
		}

		audioOutput.start();

		int nBytesRead = 0;
		byte[] abData = new byte [ BUFFER_SIZE ];
		while ( nBytesRead != -1 ) {
			try {
				nBytesRead = audioInput.read( abData, 0, abData.length );
			} catch ( IOException e ) {
				e.printStackTrace();
			}
			if ( nBytesRead >= 0 ) {
				audioOutput.write( abData, 0, nBytesRead );
			}
		}

		audioOutput.drain();
		audioOutput.close();
	}
	
	public void setVolumePercent ( double percent ) throws IllegalArgumentException {
		System.out.println ( "Percent requested: " + percent );
		
		if ( audioOutput == null ) {
			LOGGER.info( "Cannot set volume, audioOutput is null" );
			return;
		}
			
		if ( audioOutput.isControlSupported( FloatControl.Type.VOLUME ) ) {
			FloatControl volume = (FloatControl)audioOutput.getControl( FloatControl.Type.VOLUME );
			setVolume ( volume, percent );
			
		} else if ( audioOutput.isControlSupported( FloatControl.Type.MASTER_GAIN ) ) {
			FloatControl masterGain = (FloatControl)audioOutput.getControl( FloatControl.Type.MASTER_GAIN );
			setVolume ( masterGain, percent );
			
		} else {
			LOGGER.info( "Cannot set volume, volume control is not supported by system for this audio format." );
		}
	}
	
	private void setVolume ( FloatControl control, double percent ) {
		double min = control.getMinimum();
		double max = control.getMaximum();			
		double value = (max - min) * percent + min;
		control.setValue( (float)value );
			
	}
}
