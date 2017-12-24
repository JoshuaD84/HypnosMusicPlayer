package net.joshuad.hypnos;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SingleInstanceController {

	private static final Logger LOGGER = Logger.getLogger( SingleInstanceController.class.getName() );
	
	int port = 49485;
	
	boolean isFirstInstance = true;
	ServerSocket serverSocket;
	
	public SingleInstanceController() {
		if ( Hypnos.isDeveloping() ) {
			port = 49486;
		} 
		
		try {
			serverSocket = new ServerSocket ( port, 0, InetAddress.getByName(null) );
			
		} catch ( IOException e ) {
			isFirstInstance = false;
		}
	}
	
	public boolean isFirstInstance () {
		return isFirstInstance;
	}
	
	public boolean startCLICommandListener( Hypnos hypnos ) {
		
		if ( !isFirstInstance ) {
			throw new IllegalStateException( "Cannot start a command line listener if we are not the first instance." );
		}
			
		@SuppressWarnings("unchecked")
		Thread t = new Thread ( () -> {
			while ( true ) {
				try {
					Socket clientSocket = serverSocket.accept(); //It blocks here while listening
					ObjectInputStream in = new ObjectInputStream( clientSocket.getInputStream() );
					
					Object dataIn = in.readObject();
					
					System.out.println ( dataIn.getClass().getCanonicalName() );
					
					if ( dataIn instanceof List ) {
						hypnos.applyCLICommands ( (List <SocketCommand>) dataIn );
					}
					
				} catch ( Exception e ) {
					LOGGER.log( Level.INFO, e.getClass() + ": Read error at commandline parser", e );
				}
			}
		});
		
		t.setName( "CLI Listener" );
		t.setDaemon( true );
		t.start();
		return true;
	}
	
	public void sendCommandsThroughSocket( List <SocketCommand> commands ) {
		try (
			Socket clientSocket = new Socket( InetAddress.getByName(null), port );
			ObjectOutputStream out = new ObjectOutputStream( clientSocket.getOutputStream() );
		){
			out.writeObject( commands );
		} catch ( Exception e ) {
			LOGGER.log( Level.INFO, "Difficulty sending commands through socket, UI may not accept commands." );
		}
	}
}

			
			
			
