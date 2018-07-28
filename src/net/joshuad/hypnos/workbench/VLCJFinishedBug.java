package net.joshuad.hypnos.workbench;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.sun.jna.NativeLibrary;

import uk.co.caprica.vlcj.component.AudioMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

public class VLCJFinishedBug {

	String[] trackList = new String[] {
		"D:\\test\\1.mp3",
		"D:\\test\\2.mp3",
		"D:\\test\\3.mp3",
		"D:\\test\\4.mp3",
		"D:\\test\\5.mp3",
		"D:\\test\\6.mp3",
		"D:\\test\\7.mp3",
		"D:\\test\\8.mp3",
		"D:\\test\\9.mp3",
	};
	
	final String[] VLC_ARGS = {
		"--intf", "dummy", 				//no interface
		"--vout", "dummy",			 	// no video
		"--no-disable-screensaver", 	// no disabling screensaver
		"--quiet-synchro"
	};
	
	int trackIndex = 0;
	
	private AudioMediaPlayerComponent vlcComponent;
	private MediaPlayer mediaPlayer;
	private static final String NATIVE_LIBRARY_SEARCH_PATH = "stage\\lib\\vlc";
	
	public VLCJFinishedBug( ) {
		NativeLibrary.addSearchPath( RuntimeUtil.getLibVlcLibraryName(), NATIVE_LIBRARY_SEARCH_PATH );
		vlcComponent = new AudioMediaPlayerComponent();
		mediaPlayer = vlcComponent.getMediaPlayer();
		
		mediaPlayer.addMediaPlayerEventListener( new MediaPlayerEventAdapter() {
			public void finished( MediaPlayer player ) {
				Thread t = new Thread() {
					public  void run() {
						try {
							sleep ( 2000 );
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						next();
					}
				};

				t.start();
			}
		});
	}
	
	private void playTrack( String trackLocation ) {
		System.out.println ( "Playing track: " + trackLocation );
		mediaPlayer.playMedia( trackLocation, VLC_ARGS );
	}
	
	private String nextTrackString() {
		trackIndex++;
		if ( trackIndex >= trackList.length ) {
			System.out.println( "Reach end of track list." );
			System.exit( 0 );
		}

		return ( trackList [ trackIndex ] );
	}
	
	public void start() {
		trackIndex = 0;
		playTrack ( trackList[ 0 ] );
	}
	
	public void next() {
		playTrack ( nextTrackString() );
	}
	
	public void goToNearEnd() {
		mediaPlayer.setPosition( .98f );
	}
	
	public static void main ( String[] args ) {
		VLCJFinishedBug player = new VLCJFinishedBug();
		
		JButton start = new JButton ( "Start" );
		start.addActionListener( e -> player.start() );
		
		JButton next = new JButton ( "Next" );
		next.addActionListener( e -> player.next() );
		
		JButton goToNearEnd = new JButton ( "Go To Near End" );
		goToNearEnd.addActionListener( e -> player.goToNearEnd() );
		
		JPanel content = new JPanel();
		content.add( start );
		content.add( next );
		content.add( goToNearEnd );
		
		JFrame frame = new JFrame( "VLC Play Test" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setContentPane( content );
		frame.pack();
		frame.setVisible( true );
	}
}