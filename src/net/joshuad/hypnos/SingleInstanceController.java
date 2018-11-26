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

import net.joshuad.hypnos.SocketCommand.CommandType;

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
	
	public boolean startCLICommandListener ( Hypnos hypnos ) {

		if ( !isFirstInstance ) {
			throw new IllegalStateException( "Cannot start a command line listener if we are not the first instance." );
		}

		Thread t = new Thread( () -> {
			while ( true ) {
				try {
					Socket clientSocket = serverSocket.accept(); // It blocks here indefinitely while listening
					ObjectInputStream in = new ObjectInputStream( clientSocket.getInputStream() );
					ObjectOutputStream out = new ObjectOutputStream( clientSocket.getOutputStream() );
					Object dataIn = in.readObject();

					if ( dataIn instanceof List ) {
						List <SocketCommand> items = (List <SocketCommand>) dataIn;
						hypnos.applyCLICommands( items );
					}
					
					out.writeObject( new SocketCommand ( CommandType.CONTROL, SocketCommand.RECEIPT_ACKNOWLEDGED ) );

				} catch ( Exception e ) {
					LOGGER.log( Level.INFO, e.getClass() + ": Read error at commandline parser", e );
				}
			}
		} );
		
		t.setName( "CLI Listener" );
		t.setDaemon( true );
		t.start();
		return true;
	}
	
	public boolean sendCommandsThroughSocket( List <SocketCommand> commands ) {
		try (
			Socket clientSocket = new Socket( InetAddress.getByName(null), port );
			ObjectOutputStream out = new ObjectOutputStream( clientSocket.getOutputStream() );
			ObjectInputStream in = new ObjectInputStream( clientSocket.getInputStream() );
		){
			//TODO: This timeout isn't working on windows
			clientSocket.setSoTimeout( 100 );
			out.writeObject( commands );
			Object dataIn = in.readObject();
			if ( ((SocketCommand)dataIn).getObject().equals( SocketCommand.RECEIPT_ACKNOWLEDGED ) ) {
				return true;
			}
			
		} catch ( Exception e ) {
			LOGGER.log( Level.INFO, "Error sending commands through socket, UI may not accept commands." );
		}
		return false;
	}
}

			
			
			
