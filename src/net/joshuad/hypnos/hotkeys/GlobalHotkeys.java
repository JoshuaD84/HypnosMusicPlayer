package net.joshuad.hypnos.hotkeys;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
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

import net.joshuad.hypnos.Hypnos;

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
	
	private static boolean disableHotkeys = false;
	
	private static boolean disabled = false;
	
	private KeyState lastKeyState = null;
	
	private EnumMap <Hotkey, KeyState> hotkeyMap = new EnumMap <Hotkey, KeyState> ( Hotkey.class );
	
	transient private boolean hasUnsavedData = true;
	
	public boolean hasUnsavedData() {
		return hasUnsavedData;
	}
	
	public void setHasUnsavedData( boolean b ) {
		hasUnsavedData = b;
	}
	
	public static GlobalHotkeys start() {
		GlobalHotkeys hotkeys;
		if ( !disableHotkeys ) {
			PrintStream out = System.out;
			
			try {
				//This suppresses the lgpl banner from the hotkey library. 
				System.setOut( new PrintStream ( new OutputStream() {
				    @Override public void write(int b) throws IOException {}
				}));
			
				//LogManager.getLogManager().reset();
				Logger logger = Logger.getLogger( GlobalScreen.class.getPackage().getName() );
				logger.setLevel( Level.OFF );
				GlobalScreen.setEventDispatcher(new VoidDispatchService());
				GlobalScreen.registerNativeHook();
				
			} catch ( NativeHookException ex ) {
				LOGGER.warning( "There was a problem registering the global hotkey listeners. Global Hotkeys are disabled." );
				disabled = true;
			} finally {
				System.setOut( out );
			}
		}
		
		hotkeys = new GlobalHotkeys();
		
		if ( !disableHotkeys ) {
			GlobalScreen.addNativeKeyListener( hotkeys );
		} else {
			disabled = true;
		}
		
		return hotkeys;
	}
	
	public static void setDisableRequested ( boolean b ) {
		disableHotkeys = b;
	}
	
	public static boolean getDisableRequested ( ) {
		return disableHotkeys;
	}
	
	public GlobalHotkeys() {}
	
	public boolean registerLastCombination ( Hotkey hotkey, String display ) {
		if ( lastKeyState == null ) return false;
		if ( hotkey == null ) return false;
		
		for ( Hotkey key : hotkeyMap.keySet() ) {
			KeyState registeredHotkey = hotkeyMap.get( key );
			if ( registeredHotkey != null && registeredHotkey.equals( lastKeyState ) ) {
				hotkeyMap.remove( key );
			}
		}
		
		lastKeyState.setDisplay ( display );
		
		hotkeyMap.put( hotkey, lastKeyState );
		hasUnsavedData = true;
		
		return true;
	}
	
	public void clearHotkey ( Hotkey key ) {
		if ( key == null ) return;
		hotkeyMap.remove( key );
		hasUnsavedData = true;
	}
	
	public String getDisplay ( Hotkey hotkey ) {
		KeyState keyState = hotkeyMap.get( hotkey );
		
		if ( keyState == null ) return "";
		else return keyState.getDisplay();
	}
	
	public void disable() {
		disabled = true;
	}
	
	public void enable() {
		disabled = false;
	}
			
	public void nativeKeyPressed ( NativeKeyEvent e ) {	
		lastKeyState = new KeyState ( e.getKeyCode(), e.getModifiers() );
		
		if ( !Hypnos.hotkeysDisabledForConfig() ) {
			for ( Hotkey hotkey : hotkeyMap.keySet() ) {
				KeyState registeredHotkey = hotkeyMap.get( hotkey );
				if ( registeredHotkey != null && registeredHotkey.equals( lastKeyState ) ) {
					
					Hypnos.doHotkeyAction ( hotkey );
					
					try {
						//This is an attempt to consume the hotkey. Doesn't work in X11
						Field f = NativeInputEvent.class.getDeclaredField("reserved");
						f.setAccessible(true);
						f.setShort(e, (short) 0x01);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
	}

	public EnumMap <Hotkey, KeyState> getMap() {
		return hotkeyMap;
	}
	
	public void setMap ( EnumMap <Hotkey, KeyState> map ) {
		this.hotkeyMap = map;
	}
	
	public void nativeKeyReleased( NativeKeyEvent e ) {}
	
	public void nativeKeyTyped(NativeKeyEvent e) {}
	
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

