package net.joshuad.hypnos;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;

public class SingleInstanceController {
	
	//TODO: Maybe change these to enums
	public static final int NEXT = 0;
	public static final int PREVIOUS = 1;
	public static final int PAUSE = 2;
	public static final int PLAY = 3;
	public static final int TOGGLE_PAUSE = 4;
	public static final int STOP = 5;
	public static final int TOGGLE_MINIMIZED = 6;
	
	static int port = 49485;
	
	// Return true if the socket is available.
	public static boolean startCLICommandListener() {
		
		if ( Hypnos.IS_DEVELOPING ) {
			port = 49486;
		} 
		
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket ( port, 0, InetAddress.getByName(null) );
	
			@SuppressWarnings("unchecked")
			Thread t = new Thread ( () -> {
				while ( true ) {
					try {
						Socket clientSocket = serverSocket.accept(); //It blocks here while listening
						ObjectInputStream in = new ObjectInputStream( clientSocket.getInputStream() );
						
						Object dataIn = in.readObject();
						
						if ( dataIn instanceof ArrayList ) {
							sendCommandToUI ( (ArrayList <SocketCommand>) dataIn );
						}

					} catch ( IOException | ClassNotFoundException e ) {
						System.err.println ( "Read error at commandline parser" );
						e.printStackTrace();
					}
					
					try {
						Thread.sleep( 50 );
					} catch ( InterruptedException e ) {
						e.printStackTrace();
					}
				}
			});
			
			t.setDaemon( true );
			t.start();
			return true;
			
		} catch ( IOException e ) {
			return false;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void sendCommandToUI ( final ArrayList <SocketCommand> commands ) {
		Platform.runLater( new Runnable() { public void run() {
			for ( SocketCommand command : commands ) {
				if ( command.getType() == SocketCommand.CommandType.LOAD_TRACKS ) {
					ArrayList<Track> newList = new ArrayList<Track>();
					
					for ( File file : (List<File>) command.getObject() ) {
						if ( file.isDirectory() ) {
							newList.addAll( Utils.convertTrackList( Utils.getAllTracksInDirectory( file.toPath() ) ) );
						} else {
							try {
								newList.add( new CurrentListTrack ( file.toPath() ) );
							} catch ( IOException e ) {
								System.out.println ( "Unable to load file specified by user: " + file.toString() );
							}
						}
					}
					
					if ( newList.size() > 0 ) {
						Hypnos.ui.loadTracks ( newList );
					}
				}
			}
	
			for ( SocketCommand command : commands ) {
				if ( command.getType() == SocketCommand.CommandType.CONTROL ) {
					int action = (Integer)command.getObject();
	
					/* TODO 
					switch ( action ) {
						case NEXT: 
							Hypnos.ui.nextTrack();
							break;
						case PREVIOUS:
							Hypnos.ui.previousTrack();
							break;
						case PAUSE:
							Hypnos.ui.pause();
							break;
						case PLAY:
							Hypnos.ui.play();
							break;
						case TOGGLE_PAUSE:
							Hypnos.ui.togglePause();
							break;
						case STOP:
							Hypnos.ui.stopTrack();
							break;
						case TOGGLE_MINIMIZED:
							Hypnos.ui.toggleMinimized();
							break;
					}
					*/
				} 
				
				try {
					Thread.sleep( 5 ); //We have to do this because too many quick calls to any SourceDataLine.open() locks up
				} catch ( InterruptedException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		}});
	}
	
	public static void sendCommandsThroughSocket( ArrayList <SocketCommand> commands ) {
		try (
			Socket clientSocket = new Socket( InetAddress.getByName(null), port );
			ObjectOutputStream out = new ObjectOutputStream( clientSocket.getOutputStream() );
		){
			out.writeObject( commands );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

			
			
			
