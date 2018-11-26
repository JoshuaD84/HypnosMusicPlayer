package net.joshuad.hypnos.hotkeys;

import java.io.IOException;
import java.util.HashMap;

import com.melloware.jintellitype.JIntellitype;

import javafx.scene.input.KeyCode;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys.Hotkey;

public class WindowsGlobalHotkeys extends SystemHotkeys {
	
	GlobalHotkeys parent;
	
	public WindowsGlobalHotkeys( GlobalHotkeys parent ) throws HotkeyException {
		this.parent = parent;
		
		try {
			String location = Hypnos.getRootDirectory().resolve( "lib\\win\\jintellitype\\JIntellitype-64.dll" )
					.toFile().getCanonicalFile().toString();
			JIntellitype.setLibraryLocation( location );
			JIntellitype.getInstance().addHotKeyListener( ( int hotkeyID ) -> {
				if ( hotkeyID >= 0 && hotkeyID < Hotkey.values().length ) {
					Hotkey hotkey = Hotkey.values() [ hotkeyID ];
					parent.systemHotkeyEventHappened ( hotkey );
				}
			});
			JIntellitype.getInstance().addIntellitypeListener( ( int command ) -> {
				onIntellitype ( command );
			});
			
		} catch ( IOException | RuntimeException e ) {
			throw new HotkeyException( "Unable to load Hotkey Library for Windows: " + e );
		}
	}

	private void onIntellitype ( int aCommand ) {

		switch (aCommand) {
		case JIntellitype.APPCOMMAND_BROWSER_BACKWARD:
			System.out.println( "BROWSER_BACKWARD message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_BROWSER_FAVOURITES:
			System.out.println( "BROWSER_FAVOURITES message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_BROWSER_FORWARD:
			System.out.println( "BROWSER_FORWARD message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_BROWSER_HOME:
			System.out.println( "BROWSER_HOME message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_BROWSER_REFRESH:
			System.out.println( "BROWSER_REFRESH message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_BROWSER_SEARCH:
			System.out.println( "BROWSER_SEARCH message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_BROWSER_STOP:
			System.out.println( "BROWSER_STOP message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_LAUNCH_APP1:
			System.out.println( "LAUNCH_APP1 message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_LAUNCH_APP2:
			System.out.println( "LAUNCH_APP2 message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_LAUNCH_MAIL:
			System.out.println( "LAUNCH_MAIL message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_MEDIA_NEXTTRACK:
			System.out.println( "MEDIA_NEXTTRACK message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_MEDIA_PLAY_PAUSE:
			System.out.println( "MEDIA_PLAY_PAUSE message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_MEDIA_PREVIOUSTRACK:
			System.out.println( "MEDIA_PREVIOUSTRACK message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_MEDIA_STOP:
			System.out.println( "MEDIA_STOP message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_VOLUME_DOWN:
			System.out.println( "VOLUME_DOWN message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_VOLUME_UP:
			System.out.println( "VOLUME_UP message received " + Integer.toString( aCommand ) );
			break;
		case JIntellitype.APPCOMMAND_VOLUME_MUTE:
			System.out.println( "VOLUME_MUTE message received " + Integer.toString( aCommand ) );
			break;
		default:
			System.out.println( "Undefined INTELLITYPE message caught " + Integer.toString( aCommand ) );
			break;
		}
	}

	@Override
	boolean registerHotkey ( Hotkey hotkey, HotkeyState event ) {
		
		int mod = 0;
		if ( event.isMetaDown() ) mod += JIntellitype.MOD_WIN;
		if ( event.isAltDown() ) mod += JIntellitype.MOD_ALT;
		if ( event.isControlDown() ) mod += JIntellitype.MOD_CONTROL;
		if ( event.isShiftDown() ) mod += JIntellitype.MOD_SHIFT;
		
		int keyCode = keyCodeToAWTKeyEvent ( event.getCode() );
		
		JIntellitype.getInstance().registerHotKey( hotkey.ordinal(), mod, keyCode );
		return true;
	}

	@Override
	void unregisterHotkey ( Hotkey hotkey ) {
		JIntellitype.getInstance().unregisterHotKey( hotkey.ordinal() );
	}
	
	public static int keyCodeToAWTKeyEvent ( KeyCode code ) {
		return keycodetoAWTMap.get( code );
	}
	
	private static HashMap<KeyCode, Integer> keycodetoAWTMap = new HashMap<>();
	{
		keycodetoAWTMap.put( KeyCode.ENTER, 13 );
		keycodetoAWTMap.put( KeyCode.BACK_SPACE, java.awt.event.KeyEvent.VK_BACK_SPACE );
		keycodetoAWTMap.put( KeyCode.TAB, java.awt.event.KeyEvent.VK_TAB );
		keycodetoAWTMap.put( KeyCode.CANCEL, java.awt.event.KeyEvent.VK_CANCEL );
		keycodetoAWTMap.put( KeyCode.CLEAR, java.awt.event.KeyEvent.VK_CLEAR );
		keycodetoAWTMap.put( KeyCode.PAUSE, java.awt.event.KeyEvent.VK_PAUSE );
		keycodetoAWTMap.put( KeyCode.CAPS, java.awt.event.KeyEvent.VK_CAPS_LOCK );
		keycodetoAWTMap.put( KeyCode.ESCAPE, java.awt.event.KeyEvent.VK_ESCAPE );
		keycodetoAWTMap.put( KeyCode.SPACE, java.awt.event.KeyEvent.VK_SPACE );
		keycodetoAWTMap.put( KeyCode.PAGE_UP, java.awt.event.KeyEvent.VK_PAGE_UP );
		keycodetoAWTMap.put( KeyCode.PAGE_DOWN, java.awt.event.KeyEvent.VK_PAGE_DOWN );
		keycodetoAWTMap.put( KeyCode.END, java.awt.event.KeyEvent.VK_END );
		keycodetoAWTMap.put( KeyCode.HOME, java.awt.event.KeyEvent.VK_HOME );
		keycodetoAWTMap.put( KeyCode.LEFT, java.awt.event.KeyEvent.VK_LEFT );
		keycodetoAWTMap.put( KeyCode.UP, java.awt.event.KeyEvent.VK_UP );
		keycodetoAWTMap.put( KeyCode.RIGHT, java.awt.event.KeyEvent.VK_RIGHT );
		keycodetoAWTMap.put( KeyCode.DOWN, java.awt.event.KeyEvent.VK_DOWN );
		keycodetoAWTMap.put( KeyCode.COMMA, 188 );
		keycodetoAWTMap.put( KeyCode.MINUS, 109 );
		keycodetoAWTMap.put( KeyCode.PERIOD, 110 );
		keycodetoAWTMap.put( KeyCode.SLASH, 191 );
		keycodetoAWTMap.put( KeyCode.BACK_QUOTE, 192);
		keycodetoAWTMap.put( KeyCode.DIGIT0, java.awt.event.KeyEvent.VK_0 );
		keycodetoAWTMap.put( KeyCode.DIGIT1, java.awt.event.KeyEvent.VK_1 );
		keycodetoAWTMap.put( KeyCode.DIGIT2, java.awt.event.KeyEvent.VK_2 );
		keycodetoAWTMap.put( KeyCode.DIGIT3, java.awt.event.KeyEvent.VK_3 );
		keycodetoAWTMap.put( KeyCode.DIGIT4, java.awt.event.KeyEvent.VK_4 );
		keycodetoAWTMap.put( KeyCode.DIGIT5, java.awt.event.KeyEvent.VK_5 );
		keycodetoAWTMap.put( KeyCode.DIGIT6, java.awt.event.KeyEvent.VK_6 );
		keycodetoAWTMap.put( KeyCode.DIGIT7, java.awt.event.KeyEvent.VK_7 );
		keycodetoAWTMap.put( KeyCode.DIGIT8, java.awt.event.KeyEvent.VK_8 );
		keycodetoAWTMap.put( KeyCode.DIGIT9, java.awt.event.KeyEvent.VK_9 );
		keycodetoAWTMap.put( KeyCode.SEMICOLON, 186 );
		keycodetoAWTMap.put( KeyCode.EQUALS, 187 );
		keycodetoAWTMap.put( KeyCode.A, java.awt.event.KeyEvent.VK_A );
		keycodetoAWTMap.put( KeyCode.B, java.awt.event.KeyEvent.VK_B );
		keycodetoAWTMap.put( KeyCode.C, java.awt.event.KeyEvent.VK_C );
		keycodetoAWTMap.put( KeyCode.D, java.awt.event.KeyEvent.VK_D );
		keycodetoAWTMap.put( KeyCode.E, java.awt.event.KeyEvent.VK_E );
		keycodetoAWTMap.put( KeyCode.F, java.awt.event.KeyEvent.VK_F );
		keycodetoAWTMap.put( KeyCode.G, java.awt.event.KeyEvent.VK_G );
		keycodetoAWTMap.put( KeyCode.H, java.awt.event.KeyEvent.VK_H );
		keycodetoAWTMap.put( KeyCode.I, java.awt.event.KeyEvent.VK_I );
		keycodetoAWTMap.put( KeyCode.J, java.awt.event.KeyEvent.VK_J );
		keycodetoAWTMap.put( KeyCode.K, java.awt.event.KeyEvent.VK_K );
		keycodetoAWTMap.put( KeyCode.L, java.awt.event.KeyEvent.VK_L );
		keycodetoAWTMap.put( KeyCode.M, java.awt.event.KeyEvent.VK_M );
		keycodetoAWTMap.put( KeyCode.N, java.awt.event.KeyEvent.VK_N );
		keycodetoAWTMap.put( KeyCode.O, java.awt.event.KeyEvent.VK_O );
		keycodetoAWTMap.put( KeyCode.P, java.awt.event.KeyEvent.VK_P );
		keycodetoAWTMap.put( KeyCode.Q, java.awt.event.KeyEvent.VK_Q );
		keycodetoAWTMap.put( KeyCode.R, java.awt.event.KeyEvent.VK_R );
		keycodetoAWTMap.put( KeyCode.S, java.awt.event.KeyEvent.VK_S );
		keycodetoAWTMap.put( KeyCode.T, java.awt.event.KeyEvent.VK_T );
		keycodetoAWTMap.put( KeyCode.U, java.awt.event.KeyEvent.VK_U );
		keycodetoAWTMap.put( KeyCode.V, java.awt.event.KeyEvent.VK_V );
		keycodetoAWTMap.put( KeyCode.W, java.awt.event.KeyEvent.VK_W );
		keycodetoAWTMap.put( KeyCode.X, java.awt.event.KeyEvent.VK_X );
		keycodetoAWTMap.put( KeyCode.Y, java.awt.event.KeyEvent.VK_Y );
		keycodetoAWTMap.put( KeyCode.Z, java.awt.event.KeyEvent.VK_Z );
		keycodetoAWTMap.put( KeyCode.OPEN_BRACKET, 219 );
		keycodetoAWTMap.put( KeyCode.BACK_SLASH, 220 );
		keycodetoAWTMap.put( KeyCode.CLOSE_BRACKET, 221 );
		keycodetoAWTMap.put( KeyCode.NUMPAD0, java.awt.event.KeyEvent.VK_NUMPAD0 );
		keycodetoAWTMap.put( KeyCode.NUMPAD1, java.awt.event.KeyEvent.VK_NUMPAD1 );
		keycodetoAWTMap.put( KeyCode.NUMPAD2, java.awt.event.KeyEvent.VK_NUMPAD2 );
		keycodetoAWTMap.put( KeyCode.NUMPAD3, java.awt.event.KeyEvent.VK_NUMPAD3 );
		keycodetoAWTMap.put( KeyCode.NUMPAD4, java.awt.event.KeyEvent.VK_NUMPAD4 );
		keycodetoAWTMap.put( KeyCode.NUMPAD5, java.awt.event.KeyEvent.VK_NUMPAD5 );
		keycodetoAWTMap.put( KeyCode.NUMPAD6, java.awt.event.KeyEvent.VK_NUMPAD6 );
		keycodetoAWTMap.put( KeyCode.NUMPAD7, java.awt.event.KeyEvent.VK_NUMPAD7 );
		keycodetoAWTMap.put( KeyCode.NUMPAD8, java.awt.event.KeyEvent.VK_NUMPAD8 );
		keycodetoAWTMap.put( KeyCode.NUMPAD9, java.awt.event.KeyEvent.VK_NUMPAD9 );
		keycodetoAWTMap.put( KeyCode.MULTIPLY, java.awt.event.KeyEvent.VK_MULTIPLY );
		keycodetoAWTMap.put( KeyCode.ADD, java.awt.event.KeyEvent.VK_ADD );
		keycodetoAWTMap.put( KeyCode.SEPARATOR, java.awt.event.KeyEvent.VK_SEPARATOR );
		keycodetoAWTMap.put( KeyCode.SUBTRACT, java.awt.event.KeyEvent.VK_SUBTRACT );
		keycodetoAWTMap.put( KeyCode.DECIMAL, java.awt.event.KeyEvent.VK_DECIMAL );
		keycodetoAWTMap.put( KeyCode.DIVIDE, java.awt.event.KeyEvent.VK_DIVIDE );
		keycodetoAWTMap.put( KeyCode.DELETE, 46 );
		keycodetoAWTMap.put( KeyCode.NUM_LOCK, java.awt.event.KeyEvent.VK_NUM_LOCK );
		keycodetoAWTMap.put( KeyCode.SCROLL_LOCK, java.awt.event.KeyEvent.VK_SCROLL_LOCK );
		keycodetoAWTMap.put( KeyCode.F1, java.awt.event.KeyEvent.VK_F1 );
		keycodetoAWTMap.put( KeyCode.F2, java.awt.event.KeyEvent.VK_F2 );
		keycodetoAWTMap.put( KeyCode.F3, java.awt.event.KeyEvent.VK_F3 );
		keycodetoAWTMap.put( KeyCode.F4, java.awt.event.KeyEvent.VK_F4 );
		keycodetoAWTMap.put( KeyCode.F5, java.awt.event.KeyEvent.VK_F5 );
		keycodetoAWTMap.put( KeyCode.F6, java.awt.event.KeyEvent.VK_F6 );
		keycodetoAWTMap.put( KeyCode.F7, java.awt.event.KeyEvent.VK_F7 );
		keycodetoAWTMap.put( KeyCode.F8, java.awt.event.KeyEvent.VK_F8 );
		keycodetoAWTMap.put( KeyCode.F9, java.awt.event.KeyEvent.VK_F9 );
		keycodetoAWTMap.put( KeyCode.F10, java.awt.event.KeyEvent.VK_F10 );
		keycodetoAWTMap.put( KeyCode.F11, java.awt.event.KeyEvent.VK_F11 );
		keycodetoAWTMap.put( KeyCode.F12, java.awt.event.KeyEvent.VK_F12 );
		keycodetoAWTMap.put( KeyCode.F13, java.awt.event.KeyEvent.VK_F13 );
		keycodetoAWTMap.put( KeyCode.F14, java.awt.event.KeyEvent.VK_F14 );
		keycodetoAWTMap.put( KeyCode.F15, java.awt.event.KeyEvent.VK_F15 );
		keycodetoAWTMap.put( KeyCode.F16, java.awt.event.KeyEvent.VK_F16 );
		keycodetoAWTMap.put( KeyCode.F17, java.awt.event.KeyEvent.VK_F17 );
		keycodetoAWTMap.put( KeyCode.F18, java.awt.event.KeyEvent.VK_F18 );
		keycodetoAWTMap.put( KeyCode.F19, java.awt.event.KeyEvent.VK_F19 );
		keycodetoAWTMap.put( KeyCode.F20, java.awt.event.KeyEvent.VK_F20 );
		keycodetoAWTMap.put( KeyCode.F21, java.awt.event.KeyEvent.VK_F21 );
		keycodetoAWTMap.put( KeyCode.F22, java.awt.event.KeyEvent.VK_F22 );
		keycodetoAWTMap.put( KeyCode.F23, java.awt.event.KeyEvent.VK_F23 );
		keycodetoAWTMap.put( KeyCode.F24, java.awt.event.KeyEvent.VK_F24 );
		keycodetoAWTMap.put( KeyCode.PRINTSCREEN, 44 );
		keycodetoAWTMap.put( KeyCode.INSERT, 45 );
		keycodetoAWTMap.put( KeyCode.HELP, 47 );
		keycodetoAWTMap.put( KeyCode.META, java.awt.event.KeyEvent.VK_META );
		keycodetoAWTMap.put( KeyCode.BACK_QUOTE, java.awt.event.KeyEvent.VK_BACK_QUOTE );
		keycodetoAWTMap.put( KeyCode.QUOTE, java.awt.event.KeyEvent.VK_QUOTE );
		keycodetoAWTMap.put( KeyCode.KP_UP, java.awt.event.KeyEvent.VK_KP_UP );
		keycodetoAWTMap.put( KeyCode.KP_DOWN, java.awt.event.KeyEvent.VK_KP_DOWN );
		keycodetoAWTMap.put( KeyCode.KP_LEFT, java.awt.event.KeyEvent.VK_KP_LEFT );
		keycodetoAWTMap.put( KeyCode.KP_RIGHT, java.awt.event.KeyEvent.VK_KP_RIGHT );
		keycodetoAWTMap.put( KeyCode.DEAD_GRAVE, java.awt.event.KeyEvent.VK_DEAD_GRAVE );
		keycodetoAWTMap.put( KeyCode.DEAD_ACUTE, java.awt.event.KeyEvent.VK_DEAD_ACUTE );
		keycodetoAWTMap.put( KeyCode.DEAD_CIRCUMFLEX, java.awt.event.KeyEvent.VK_DEAD_CIRCUMFLEX );
		keycodetoAWTMap.put( KeyCode.DEAD_TILDE, java.awt.event.KeyEvent.VK_DEAD_TILDE );
		keycodetoAWTMap.put( KeyCode.DEAD_MACRON, java.awt.event.KeyEvent.VK_DEAD_MACRON );
		keycodetoAWTMap.put( KeyCode.DEAD_BREVE, java.awt.event.KeyEvent.VK_DEAD_BREVE );
		keycodetoAWTMap.put( KeyCode.DEAD_ABOVEDOT, java.awt.event.KeyEvent.VK_DEAD_ABOVEDOT );
		keycodetoAWTMap.put( KeyCode.DEAD_DIAERESIS, java.awt.event.KeyEvent.VK_DEAD_DIAERESIS );
		keycodetoAWTMap.put( KeyCode.DEAD_ABOVERING, java.awt.event.KeyEvent.VK_DEAD_ABOVERING );
		keycodetoAWTMap.put( KeyCode.DEAD_DOUBLEACUTE, java.awt.event.KeyEvent.VK_DEAD_DOUBLEACUTE );
		keycodetoAWTMap.put( KeyCode.DEAD_CARON, java.awt.event.KeyEvent.VK_DEAD_CARON );
		keycodetoAWTMap.put( KeyCode.DEAD_CEDILLA, java.awt.event.KeyEvent.VK_DEAD_CEDILLA );
		keycodetoAWTMap.put( KeyCode.DEAD_OGONEK, java.awt.event.KeyEvent.VK_DEAD_OGONEK );
		keycodetoAWTMap.put( KeyCode.DEAD_IOTA, java.awt.event.KeyEvent.VK_DEAD_IOTA );
		keycodetoAWTMap.put( KeyCode.DEAD_VOICED_SOUND, java.awt.event.KeyEvent.VK_DEAD_VOICED_SOUND );
		keycodetoAWTMap.put( KeyCode.DEAD_SEMIVOICED_SOUND, java.awt.event.KeyEvent.VK_DEAD_SEMIVOICED_SOUND );
		keycodetoAWTMap.put( KeyCode.AMPERSAND, java.awt.event.KeyEvent.VK_AMPERSAND );
		keycodetoAWTMap.put( KeyCode.ASTERISK, java.awt.event.KeyEvent.VK_ASTERISK );
		keycodetoAWTMap.put( KeyCode.QUOTEDBL, java.awt.event.KeyEvent.VK_QUOTEDBL );
		keycodetoAWTMap.put( KeyCode.LESS, java.awt.event.KeyEvent.VK_LESS );
		keycodetoAWTMap.put( KeyCode.GREATER, java.awt.event.KeyEvent.VK_GREATER );
		keycodetoAWTMap.put( KeyCode.BRACELEFT, java.awt.event.KeyEvent.VK_BRACELEFT );
		keycodetoAWTMap.put( KeyCode.BRACERIGHT, java.awt.event.KeyEvent.VK_BRACERIGHT );
		keycodetoAWTMap.put( KeyCode.AT, java.awt.event.KeyEvent.VK_AT );
		keycodetoAWTMap.put( KeyCode.COLON, java.awt.event.KeyEvent.VK_COLON );
		keycodetoAWTMap.put( KeyCode.CIRCUMFLEX, java.awt.event.KeyEvent.VK_CIRCUMFLEX );
		keycodetoAWTMap.put( KeyCode.DOLLAR, java.awt.event.KeyEvent.VK_DOLLAR );
		keycodetoAWTMap.put( KeyCode.EURO_SIGN, java.awt.event.KeyEvent.VK_EURO_SIGN );
		keycodetoAWTMap.put( KeyCode.EXCLAMATION_MARK, java.awt.event.KeyEvent.VK_EXCLAMATION_MARK );
		keycodetoAWTMap.put( KeyCode.INVERTED_EXCLAMATION_MARK, java.awt.event.KeyEvent.VK_INVERTED_EXCLAMATION_MARK );
		keycodetoAWTMap.put( KeyCode.LEFT_PARENTHESIS, java.awt.event.KeyEvent.VK_LEFT_PARENTHESIS );
		keycodetoAWTMap.put( KeyCode.NUMBER_SIGN, java.awt.event.KeyEvent.VK_NUMBER_SIGN );
		keycodetoAWTMap.put( KeyCode.PLUS, java.awt.event.KeyEvent.VK_PLUS );
		keycodetoAWTMap.put( KeyCode.RIGHT_PARENTHESIS, java.awt.event.KeyEvent.VK_RIGHT_PARENTHESIS );
		keycodetoAWTMap.put( KeyCode.UNDERSCORE, java.awt.event.KeyEvent.VK_UNDERSCORE );
		keycodetoAWTMap.put( KeyCode.CONTEXT_MENU, java.awt.event.KeyEvent.VK_CONTEXT_MENU );
		keycodetoAWTMap.put( KeyCode.FINAL, java.awt.event.KeyEvent.VK_FINAL );
		keycodetoAWTMap.put( KeyCode.CONVERT, java.awt.event.KeyEvent.VK_CONVERT );
		keycodetoAWTMap.put( KeyCode.NONCONVERT, java.awt.event.KeyEvent.VK_NONCONVERT );
		keycodetoAWTMap.put( KeyCode.ACCEPT, java.awt.event.KeyEvent.VK_ACCEPT );
		keycodetoAWTMap.put( KeyCode.MODECHANGE, java.awt.event.KeyEvent.VK_MODECHANGE );
		keycodetoAWTMap.put( KeyCode.KANA, java.awt.event.KeyEvent.VK_KANA );
		keycodetoAWTMap.put( KeyCode.KANJI, java.awt.event.KeyEvent.VK_KANJI );
		keycodetoAWTMap.put( KeyCode.ALPHANUMERIC, java.awt.event.KeyEvent.VK_ALPHANUMERIC );
		keycodetoAWTMap.put( KeyCode.KATAKANA, java.awt.event.KeyEvent.VK_KATAKANA );
		keycodetoAWTMap.put( KeyCode.HIRAGANA, java.awt.event.KeyEvent.VK_HIRAGANA );
		keycodetoAWTMap.put( KeyCode.FULL_WIDTH, java.awt.event.KeyEvent.VK_FULL_WIDTH );
		keycodetoAWTMap.put( KeyCode.HALF_WIDTH, java.awt.event.KeyEvent.VK_HALF_WIDTH );
		keycodetoAWTMap.put( KeyCode.ROMAN_CHARACTERS, java.awt.event.KeyEvent.VK_ROMAN_CHARACTERS );
		keycodetoAWTMap.put( KeyCode.ALL_CANDIDATES, java.awt.event.KeyEvent.VK_ALL_CANDIDATES );
		keycodetoAWTMap.put( KeyCode.PREVIOUS_CANDIDATE, java.awt.event.KeyEvent.VK_PREVIOUS_CANDIDATE );
		keycodetoAWTMap.put( KeyCode.CODE_INPUT, java.awt.event.KeyEvent.VK_CODE_INPUT );
		keycodetoAWTMap.put( KeyCode.JAPANESE_KATAKANA, java.awt.event.KeyEvent.VK_JAPANESE_KATAKANA );
		keycodetoAWTMap.put( KeyCode.JAPANESE_HIRAGANA, java.awt.event.KeyEvent.VK_JAPANESE_HIRAGANA );
		keycodetoAWTMap.put( KeyCode.JAPANESE_ROMAN, java.awt.event.KeyEvent.VK_JAPANESE_ROMAN );
		keycodetoAWTMap.put( KeyCode.KANA_LOCK, java.awt.event.KeyEvent.VK_KANA_LOCK );
		keycodetoAWTMap.put( KeyCode.INPUT_METHOD_ON_OFF, java.awt.event.KeyEvent.VK_INPUT_METHOD_ON_OFF );
		keycodetoAWTMap.put( KeyCode.CUT, java.awt.event.KeyEvent.VK_CUT );
		keycodetoAWTMap.put( KeyCode.COPY, java.awt.event.KeyEvent.VK_COPY );
		keycodetoAWTMap.put( KeyCode.PASTE, java.awt.event.KeyEvent.VK_PASTE );
		keycodetoAWTMap.put( KeyCode.UNDO, java.awt.event.KeyEvent.VK_UNDO );
		keycodetoAWTMap.put( KeyCode.AGAIN, java.awt.event.KeyEvent.VK_AGAIN );
		keycodetoAWTMap.put( KeyCode.FIND, java.awt.event.KeyEvent.VK_FIND );
		keycodetoAWTMap.put( KeyCode.PROPS, java.awt.event.KeyEvent.VK_PROPS );
		keycodetoAWTMap.put( KeyCode.STOP, java.awt.event.KeyEvent.VK_STOP );
		keycodetoAWTMap.put( KeyCode.COMPOSE, java.awt.event.KeyEvent.VK_COMPOSE );
		keycodetoAWTMap.put( KeyCode.ALT_GRAPH, java.awt.event.KeyEvent.VK_ALT_GRAPH );
		keycodetoAWTMap.put( KeyCode.BEGIN, java.awt.event.KeyEvent.VK_BEGIN );
	}
}
