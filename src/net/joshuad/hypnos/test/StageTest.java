package net.joshuad.hypnos.test;

	import javafx.application.Application;
	import javafx.application.Platform;
	import javafx.stage.Stage;
	
	public class StageTest extends Application {
		
		Stage stage;
		
		public static void main ( String[] args ) {
			launch ( args );
		}
	
		@Override
		public void start ( Stage stage ) throws Exception {
			this.stage = stage;
			stage.setResizable( true );
			stage.show( );
			
			Thread thread = new Thread ( () -> {
				while ( true ) {
					Platform.runLater( () -> {
						toggleMinimized();
					} );
					try {
						Thread.sleep ( 1000 );
					} catch ( InterruptedException e ) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			
			thread.setDaemon( true );
			thread.start();
			
		}
		
		public void toggleMinimized() {
	
			System.out.println ( "Before call" );
			System.out.println ( "\tisIconified(): " + stage.isIconified() );
			System.out.println ( "\tisMaximized(): " + stage.isMaximized() );
			System.out.println ();
			
			if ( stage.isIconified() ) {
				System.out.println ( "Setting iconified to false" );
				System.out.println ();
				stage.setIconified( false );
			} else {
				System.out.println ( "Setting iconified to true" );
				System.out.println ();
				stage.setIconified( true );
			}
			
	
			System.out.println ( "After call" );
			System.out.println ( "\tisIconified(): " + stage.isIconified() );
			System.out.println ( "\tisMaximized(): " + stage.isMaximized() );
			System.out.println ();
			System.out.println ();
		}
	}
