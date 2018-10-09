package net.joshuad.hypnos.workbench;

import com.sun.jna.NativeLibrary;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

public class VLCJLinuxTest {
	
	private static final String NATIVE_LIBRARY_SEARCH_PATH = "/d/programming/workspace/MusicPlayer/stage/lib/nix/lib/";
	
	public static void main( String[] args ) {
		
		NativeLibrary.addSearchPath( RuntimeUtil.getLibVlcLibraryName(), NATIVE_LIBRARY_SEARCH_PATH );
        System.out.println( LibVlc.INSTANCE.libvlc_get_version() );
    }
}
