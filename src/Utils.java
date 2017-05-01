import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class Utils {
	
	private static String[] musicExtStrings = new String[] {
			"flac",
			"mp3"
		};
		
	static ArrayList <String> musicExtensions = new ArrayList <String> ( Arrays.asList ( musicExtStrings ) );  

	public static final DirectoryStream.Filter<Path> musicFileFilter = new DirectoryStream.Filter<Path>() {
		@Override
		public boolean accept ( Path entry ) throws IOException {
			return isMusicFile ( entry );			
		}
	};
	  
	public static boolean isMusicFile ( Path testFile ) {
		String fileName = testFile.getFileName().toString();
		
		if ( !Files.exists( testFile ) ) {
			System.out.println ( "Doesn't exist" );
			return false;		
		
		} else if ( !Files.isRegularFile( testFile ) ) {
			System.out.println ( "Not regular" );
			return false;
		
		} else if ( fileName.lastIndexOf(".") == -1 || fileName.lastIndexOf(".") == 0 ) {
			System.out.println ( "No extension" );
			return false;
			
		} 
		
		String testExtension = fileName.substring ( fileName.lastIndexOf( "." ) + 1 ).toLowerCase();
		
		for ( String musicExtension : musicExtensions ) {
			if ( musicExtension.equals( testExtension ) ) { 
				return true;
			}
		}
		
		System.out.println ( "Extension doesn't match" );
		return false;
				
	}

}
