package net.joshuad.hypnos;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class SingleInstanceController {
	
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
			//TODO: throw the error, or log it? 
			throw new IllegalStateException( "Cannot start a command line listener if we are not the first instance." );
		}
			
		@SuppressWarnings("unchecked")
		Thread t = new Thread ( () -> {
			while ( true ) {
				try {
					Socket clientSocket = serverSocket.accept(); //It blocks here while listening
					ObjectInputStream in = new ObjectInputStream( clientSocket.getInputStream() );
					
					Object dataIn = in.readObject();
					
					if ( dataIn instanceof ArrayList ) {
						hypnos.applyCommands ( (ArrayList <SocketCommand>) dataIn );
					}

				} catch ( IOException | ClassNotFoundException e ) {
					System.err.println ( "Read error at commandline parser" );
					e.printStackTrace();
				}
			}
		});
		
		t.setDaemon( true );
		t.start();
		return true;
	}
	
	public void sendCommandsThroughSocket( ArrayList <SocketCommand> commands ) {
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

			
			
			
