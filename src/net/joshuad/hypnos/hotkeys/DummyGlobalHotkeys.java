package net.joshuad.hypnos.hotkeys;

import net.joshuad.hypnos.hotkeys.GlobalHotkeys.Hotkey;

public class DummyGlobalHotkeys extends SystemHotkeys {
	
	public DummyGlobalHotkeys( GlobalHotkeys parent ) {} 
	
	@Override
	public boolean registerHotkey ( Hotkey hotkey, HotkeyState event ) {
		return false;
	}
	
	@Override
	void unregisterHotkey ( Hotkey key ) {}
}
