package net.joshuad.hypnos.hotkeys;

import javafx.scene.input.KeyEvent;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys.Hotkey;

public abstract class SystemHotkeys {
	abstract void unregisterHotkey ( Hotkey key );
	abstract boolean registerHotkey ( Hotkey hotkey, HotkeyState event );
	protected abstract HotkeyState createJustPressedState( KeyEvent keyEvent );
}
