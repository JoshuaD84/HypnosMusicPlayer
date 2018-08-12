package net.joshuad.hypnos.workbench;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.runtime.x.LibXUtil;

import com.sun.jna.NativeLibrary;

/**
 * Base class for tests.
 * <p>
 * This makes it a lot easier to switch vlc versions or vlc install directories without having to
 * change system properties on a lot of IDE application run-configurations.
 * <p>
 * Explicitly setting a search path forces JNA to search that path <em>first</em>.
 * <p>
 * The search path should be the directory that contains libvlc.so and libvlccore.so.
 * <p>
 * If you do not explicitly set the search path, the system search path will be used.
 * <p>
 * You can also set the log level here.
 */
public abstract class VlcjTest {

    /**
     * Log.
     */
    private static final Logger logger = LoggerFactory.getLogger(VlcjTest.class);

    /**
     * Change this to point to your own vlc installation, or comment out the code if you want to use
     * your system default installation.
     * <p>
     * This is a bit more explicit than using the -Djna.library.path= system property.
     */
    private static final String NATIVE_LIBRARY_SEARCH_PATH = "stage/lib/nix/vlc";

    /**
     * Set to true to dump out native JNA memory structures.
     */
    private static final String DUMP_NATIVE_MEMORY = "false";

    /**
     * Static initialisation.
     */
    static {
    	
        // Initialise Log4J (this is good enough for testing, vlcj depends on log4j only for testing here)

        // Safely try to initialise LibX11 to reduce the opportunity for native
        // crashes - this will silently throw an Error on Windows (and maybe MacOS)
        // that can safely be ignored
        LibXUtil.initialise();
        

        if(null != NATIVE_LIBRARY_SEARCH_PATH) {
            logger.info("Explicitly adding JNA native library search path: '{}'", NATIVE_LIBRARY_SEARCH_PATH);
            NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), NATIVE_LIBRARY_SEARCH_PATH);
        }

        System.setProperty("jna.dump_memory", DUMP_NATIVE_MEMORY);
    }

    /**
     * Set the standard look and feel.
     */
    protected static final void setLookAndFeel() {
        String lookAndFeelClassName = null;
        LookAndFeelInfo[] lookAndFeelInfos = UIManager.getInstalledLookAndFeels();
        for(LookAndFeelInfo lookAndFeel : lookAndFeelInfos) {
            if("Nimbus".equals(lookAndFeel.getName())) {
                lookAndFeelClassName = lookAndFeel.getClassName();
            }
        }
        if(lookAndFeelClassName == null) {
            lookAndFeelClassName = UIManager.getSystemLookAndFeelClassName();
        }
        try {
            UIManager.setLookAndFeel(lookAndFeelClassName);
        }
        catch(Exception e) {
            // Silently fail, it doesn't matter
        }
    }
    
    
}