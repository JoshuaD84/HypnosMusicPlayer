package net.joshuad.hypnos.hotkeys;

import javafx.scene.input.KeyEvent;
import net.joshuad.hypnos.hotkeys.GlobalHotkeys.Hotkey;

public class DummyGlobalHotkeys extends SystemHotkeys {
	
	public DummyGlobalHotkeys( GlobalHotkeys parent ) {} 
	
	@Override
	public boolean registerHotkey ( Hotkey hotkey, HotkeyState event ) {
		return false;
	}
	
	@Override
	void unregisterHotkey ( Hotkey key ) {}

	@Override
	protected HotkeyState createJustPressedState(KeyEvent keyEvent) {
		return null;
	}
}
