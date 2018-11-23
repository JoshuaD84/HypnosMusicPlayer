package net.joshuad.hypnos.hotkeys;

import net.joshuad.hypnos.hotkeys.GlobalHotkeys.Hotkey;

public abstract class SystemHotkeys {
	abstract void unregisterHotkey ( Hotkey key );
	abstract boolean registerHotkey ( Hotkey hotkey, HotkeyState event );
}
