package net.joshuad.hypnos.workbench;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.NativeInputEvent;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

public class GlobalHotkeys implements NativeKeyListener {
	private static final Logger LOGGER = Logger.getLogger( GlobalHotkeys.class.getName() );

	public enum Hotkey {
		PREVIOUS ( "Previous" ), 
		NEXT ( "Next" ), 
		PLAY ( "Play" ), 
		TOGGLE_PAUSE ( "Pause / Unpause" ),
		STOP ( "Stop" ),
		
		VOLUME_UP ( "Volume Up" ),
		VOLUME_DOWN ( "Volume Down" ),
		TOGGLE_MUTE ( "Mute / Unmute" ),
		
		TOGGLE_SHUFFLE ( "Toggle Shuffle" ),
		TOGGLE_REPEAT ( "Toggle Repeat" ),
		
		SHOW_HIDE_UI ( "Show/Hide Main Window" ),
		
		SKIP_FORWARD ( "Forward 5 Seconds" ),
		SKIP_BACK ( "Back 5 Seconds" );
		
				
		private String label;
		Hotkey ( String label ) { this.label = label; }
		public String getLabel () { return label; }
	}
	
	public static void main ( String[] args ) {
		GlobalHotkeys hotkeys = start();
	}
	
	public static GlobalHotkeys start() {
		GlobalHotkeys hotkeys;
		PrintStream out = System.out;
		
		try {
			//This suppresses the lgpl banner from the hotkey library. 
			System.setOut( new PrintStream ( new OutputStream() {
			    @Override public void write(int b) throws IOException {}
			}));
		
			Logger logger = Logger.getLogger( GlobalScreen.class.getPackage().getName() );
			logger.setLevel( Level.OFF );
			GlobalScreen.setEventDispatcher(new VoidDispatchService());
			GlobalScreen.registerNativeHook();
			
		} catch ( NativeHookException ex ) {
			LOGGER.warning( "There was a problem registering the global hotkey listeners. Global Hotkeys are disabled." );
			
		} finally {
			System.setOut( out );
		}
		
		hotkeys = new GlobalHotkeys();
		GlobalScreen.addNativeKeyListener( hotkeys );
		
		return hotkeys;
	}
	
	public GlobalHotkeys() {}
	
	public void nativeKeyPressed ( NativeKeyEvent e ) {	
		if ( e.getKeyCode() == 47 && e.getModifiers() == 4 ) {
			System.out.println( "Detected" );
			consume ( e );
		}
	}

	public void nativeKeyReleased( NativeKeyEvent e ) {}

	private void consume ( NativeKeyEvent e ) {
		try {
			//This is an attempt to consume the hotkey. Doesn't work in X11
			Field f = NativeInputEvent.class.getDeclaredField("reserved");
			f.setAccessible(true);
			f.setShort(e, (short) 0x01);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void nativeKeyTyped(NativeKeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
}

class VoidDispatchService extends AbstractExecutorService {
	private boolean running = false;

	public VoidDispatchService() {
		running = true;
	}

	public void shutdown() {
		running = false;
	}

	public List<Runnable> shutdownNow() {
		running = false;
		return new ArrayList<Runnable>(0);
	}

	public boolean isShutdown() {
		return !running;
	}

	public boolean isTerminated() {
		return !running;
	}

	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return true;
	}

	public void execute(Runnable r) {
		r.run();
	}
}

