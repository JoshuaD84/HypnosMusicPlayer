/*  Copyright 2008  Edwin Stang (edwinstang@gmail.com),
 *
 *  This file is part of JXGrabKey.
 *
 *  JXGrabKey is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  JXGrabKey is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JXGrabKey.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Modified by Josh Hartwell to accomodate JavaFX, 2018. 
 */
package jxgrabkey;

import net.joshuad.hypnos.hotkeys.HotkeyState;

/**
 * This class holds definitions for X11 masks. It can also convert AWT masks into X11 masks.
 *
 * @author subes
 */
public final class X11MaskDefinitions {

    public static final int X11_SHIFT_MASK = 1 << 0;
    public static final int X11_LOCK_MASK = 1 << 1;
    public static final int X11_CONTROL_MASK = 1 << 2;
    public static final int X11_MOD1_MASK = 1 << 3;
    public static final int X11_MOD2_MASK = 1 << 4;
    public static final int X11_MOD3_MASK = 1 << 5;
    public static final int X11_MOD4_MASK = 1 << 6;
    public static final int X11_MOD5_MASK = 1 << 7;

    //TODO: It'd be nice if this didn't depend on my custom class, since this is kind of a library
    public static int fxKeyStateToX11Mask ( HotkeyState e ) {
		int mask = 0;
		
		if ( e.isShiftDown() ) {
			mask |= X11MaskDefinitions.X11_SHIFT_MASK;
		}
		
		if ( e.isControlDown() ) {
			mask |= X11MaskDefinitions.X11_CONTROL_MASK;
		}
		
		if ( e.isAltDown() ) {
			mask |= X11MaskDefinitions.X11_MOD1_MASK;
		}
		
		if ( e.isMetaDown() ) {
			mask |= X11MaskDefinitions.X11_MOD4_MASK;
		}
			
		return mask;
	}
}
