package net.joshuad.hypnos;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import net.joshuad.hypnos.library.Track;
import net.joshuad.hypnos.library.Track.Format;

public class Utils {

	private static transient final Logger LOGGER = Logger.getLogger(Utils.class.getName());

	private static String[] imageExtStrings = new String[] { "jpg", "jpeg", "png", "gif" };
	private static String[] playlistExtStrings = new String[] { "m3u" };

	public static final DirectoryStream.Filter<Path> musicFileFilter = new DirectoryStream.Filter<Path>() {
		@Override
		public boolean accept(Path entry) throws IOException {
			return isMusicFile(entry);
		}
	};

	public static boolean hasImageExtension(String url) {
		String testExtension = url.substring(url.lastIndexOf(".") + 1).toLowerCase();
		for (String imageExtension : imageExtStrings) {
			if (imageExtension.toLowerCase().equals(testExtension)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isChildOf(Path potentialChild, Path parent) {
		parent = parent.normalize().toAbsolutePath();
		Path test = potentialChild.getParent();
		while (test != null) {
			if (test.equals(parent)) {
				return true;
			}
			test = test.getParent();
		}
		return false;
	}

	public static boolean isImageFile(File testFile) {
		return isImageFile(testFile.toPath());
	}

	public static boolean isImageFile(Path testFile) {
		String fileName = testFile.getFileName().toString();
		if (!Files.exists(testFile)) {
			return false;
		} else if (!Files.isRegularFile(testFile)) {
			return false;
		} else if (fileName.lastIndexOf(".") == -1 || fileName.lastIndexOf(".") == 0) {
			return false;
		}
		return hasImageExtension(fileName);
	}

	public static String toReleaseTitleCase(String input) {
		if (input.equalsIgnoreCase("EP")) {
			return "EP";
		} else {
			StringBuilder titleCase = new StringBuilder();
			boolean nextTitleCase = true;
			for (char c : input.toCharArray()) {
				if (Character.isSpaceChar(c)) {
					nextTitleCase = true;
				} else if (nextTitleCase) {
					c = Character.toTitleCase(c);
					nextTitleCase = false;
				}
				titleCase.append(c);
			}
			return titleCase.toString();
		}
	}

	public static boolean isMusicFile(String testFile) {
		return isMusicFile(Paths.get(testFile));
	}

	public static boolean isMusicFile(Path testFile) {
		if (testFile == null) {
			LOGGER.log(Level.INFO, "Asked if a null path was a music file, returning false.", new NullPointerException());
			return false;
		}
		String fileName = testFile.getFileName().toString();
		if (!Files.exists(testFile)) {
			return false;
		} else if (!Files.isRegularFile(testFile)) {
			return false;
		} else if (fileName.lastIndexOf(".") == -1 || fileName.lastIndexOf(".") == 0) {
			return false;
		} else if (fileName.startsWith("._")) {
			return false;
		}
		String testExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
		Format format = Format.getFormat(testExtension);
		return format != null;
	}

	public static boolean isPlaylistFile(Path testFile) {
		String fileName = testFile.getFileName().toString();
		if (!Files.exists(testFile)) {
			return false;
		} else if (!Files.isRegularFile(testFile)) {
			return false;
		} else if (fileName.lastIndexOf(".") == -1 || fileName.lastIndexOf(".") == 0) {
			return false;
		}
		String testExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
		for (String playlistExtension : playlistExtStrings) {
			if (playlistExtension.toLowerCase().equals(testExtension)) {
				return true;
			}
		}
		return false;
	}

	public static String getFileExtension(Path path) {
		return getFileExtension(path.toFile());
	}

	public static String getFileExtension(File file) {
		String name = file.getName();
		try {
			return name.substring(name.lastIndexOf(".") + 1);
		} catch (Exception e) {
			return "";
		}
	}

	public static String getLengthDisplay(int lengthSeconds) {
		boolean negative = lengthSeconds < 0;
		lengthSeconds = Math.abs(lengthSeconds);
		int hours = lengthSeconds / 3600;
		int minutes = (lengthSeconds % 3600) / 60;
		int seconds = lengthSeconds % 60;
		if (hours > 0) {
			return String.format((negative ? "-" : "") + "%d:%02d:%02d", hours, minutes, seconds);
		} else {
			return String.format((negative ? "-" : "") + "%d:%02d", minutes, seconds);
		}
	}

	public static ArrayList<CurrentListTrack> convertTrackList(List<Track> tracks) {
		ArrayList<CurrentListTrack> retMe = new ArrayList<CurrentListTrack>(tracks.size());
		for (Track track : tracks) {
			if (track instanceof CurrentListTrack) {
				retMe.add((CurrentListTrack) track);
			} else {
				retMe.add(new CurrentListTrack(track));
			}
		}
		return retMe;
	}

	public static ArrayList<Track> convertCurrentTrackList(List<CurrentListTrack> tracks) {
		ArrayList<Track> retMe = new ArrayList<Track>(tracks.size());
		for (CurrentListTrack track : tracks) {
			retMe.add((Track) track);
		}
		return retMe;
	}

	public static String prepareArtistForCompare(String string) {
		return string.toLowerCase().replaceAll(" & ", " and ").replaceAll("&", " and ").replaceAll(" \\+ ", " and ")
				.replaceAll("\\+", " and ").replaceAll("  ", " ").replaceAll(" the ", "").replaceAll("^the ", "")
				.replaceAll(", the$", "").replaceAll("-", " ").replaceAll("\\.", "").replaceAll("_", " ");
	}

	public static String prepareAlbumForCompare(String string) {
		return string.toLowerCase();
	}

	public static ArrayList<Path> getAllTracksInDirectory(Path startingDirectory) {
		TrackFinder finder = new TrackFinder();
		try {
			Files.walkFileTree(startingDirectory, finder);
			return finder.trackPaths;

		} catch (Exception e) {
			LOGGER.log(Level.WARNING,
					"Read error while traversing directory, some files may not have been loaded: " + startingDirectory.toString(),
					e);
		}

		return new ArrayList<Path>();
	}

	public static boolean saveImageToDisk(Path location, byte[] buffer) {
		if (location == null) {
			return false;
		}
		InputStream in = new ByteArrayInputStream(buffer);
		try {
			BufferedImage bImage = ImageIO.read(in);
			ImageIO.write(bImage, "png", location.toFile());
			return true;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Unable to save image to location: " + location, e);
		}
		return false;
	}
}

class TrackFinder extends SimpleFileVisitor<Path> {
	ArrayList<Path> trackPaths = new ArrayList<Path>();
	ArrayList<Path> addMe = new ArrayList<Path>();

	@Override
	public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
		boolean isMusicFile = Utils.isMusicFile(path);
		if (isMusicFile) {
			addMe.add(path);
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
		Collections.sort(addMe);
		trackPaths.addAll(addMe);
		addMe.clear();
		return FileVisitResult.CONTINUE;
	}

	public FileVisitResult visitFileFailed(Path file, IOException exc) {
		return FileVisitResult.CONTINUE;
	}
}
