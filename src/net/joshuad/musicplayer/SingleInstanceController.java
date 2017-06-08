package net.joshuad.musicplayer;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import javafx.application.Platform;

public class SingleInstanceController {
	
	//TODO: Maybe change these to enums
	public static final int NEXT = 0;
	public static final int PREVIOUS = 1;
	public static final int PAUSE = 2;
	public static final int PLAY = 3;
	public static final int TOGGLE_PAUSE = 4;
	public static final int STOP = 5;
	
	static int port = 49485;
	
	// Return true if the socket is available.
	public static boolean startCLICommandListener() {
		
		if ( MusicPlayerUI.IS_DEVELOPING ) {
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
						System.out.println ( "Read object from socket." ); //TODO: DD
						
						if ( dataIn instanceof ArrayList ) {
							giveCommandToUI ( (ArrayList <SocketCommand>) dataIn );
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
	public static void giveCommandToUI ( final ArrayList <SocketCommand> commands ) {
		Platform.runLater( new Runnable() { public void run() {
			for ( SocketCommand command : commands ) {
				if ( command.getType() == SocketCommand.CommandType.LOAD_TRACKS ) {
					System.out.println ( "Load tracks heard." ); //TODO: DD
					ArrayList<CurrentListTrack> newList = new ArrayList<CurrentListTrack>();
					
					for ( File file : (List<File>) command.getObject() ) {
						try {
							newList.add( new CurrentListTrack ( file.toPath() ) );
							System.out.println ( "loading file: " + file.toString() );
						} catch ( CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e ) {
							System.out.println ( "Unable to load file: " + file.toString() );
						}
					}
					
					if ( newList.size() > 0 ) {
						MusicPlayerUI.currentListData.clear();
						MusicPlayerUI.currentListData.addAll( newList );
					}
				}
			}
	
			for ( SocketCommand command : commands ) {
				if ( command.getType() == SocketCommand.CommandType.CONTROL ) {
					int action = (Integer)command.getObject();
	
					System.out.println ( "Action being sent to UI: " + action ); //TODO: DD
					switch ( action ) {
						case NEXT: 
							MusicPlayerUI.playNextTrack();
							break;
						case PREVIOUS:
							MusicPlayerUI.playPreviousTrack();
							break;
						case PAUSE:
							MusicPlayerUI.pause();
							break;
						case PLAY:
							MusicPlayerUI.play();
							break;
						case TOGGLE_PAUSE:
							MusicPlayerUI.togglePause();
							break;
						case STOP:
							MusicPlayerUI.stopTrack();
							break;
					}
				} 
			}
		}});
	}
	
	public static void sendCommandsThroughSocket( ArrayList <SocketCommand> commands ) {
		try (
			Socket clientSocket = new Socket( InetAddress.getByName(null), port );
			ObjectOutputStream out = new ObjectOutputStream( clientSocket.getOutputStream() );
		){
			System.out.println ( "Sending command out over socket." ); //TODO: DD
			out.writeObject( commands );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

			
			
			
