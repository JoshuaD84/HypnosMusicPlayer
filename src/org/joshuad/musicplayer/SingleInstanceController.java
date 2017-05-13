package org.joshuad.musicplayer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class SingleInstanceController {
	
	public static final int NEXT = 0;
	public static final int PREVIOUS = 1;
	public static final int PAUSE = 2;
	public static final int PLAY = 3;
	public static final int TOGGLE_PAUSE = 4;
	public static final int STOP = 5;
	
	static int port = 49485;
	
	// Return true if the socket is available.
	public static boolean startCLICommandListener() {
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket ( port, 0, InetAddress.getByName(null) );
	
			Thread t = new Thread ( new Runnable() {
				public void run() {
					while ( true ) {
						try {
							Socket clientSocket = serverSocket.accept(); //It blocks here while listening
							BufferedReader in = new BufferedReader( new InputStreamReader( clientSocket.getInputStream() ) );
							int command;
							while ( (command = in.read()) != -1 ) {
								switch ( command ) {
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

						} catch ( IOException e ) {
							System.err.println ( "Read error at commandline parser" );
							e.printStackTrace();
						}
						
						try {
							Thread.sleep( 50 );
						} catch ( InterruptedException e ) {
							e.printStackTrace();
						}
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
	
	public static void sendCommands( ArrayList <Integer> commands ) {
		try (
			Socket clientSocket = new Socket( InetAddress.getByName(null), port );
			DataOutputStream out = new DataOutputStream( clientSocket.getOutputStream() );
		){
			for ( int command : commands ) {
				out.write( command );
			}
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
