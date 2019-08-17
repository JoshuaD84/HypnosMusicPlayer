package net.joshuad.hypnos.library;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.exception.ExceptionUtils;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.fxui.FXUI;

class DiskReader implements FileVisitor<Path> {

	private static final Logger LOGGER = Logger.getLogger(DiskReader.class.getName());

	public enum ScanMode {
		INITIAL_SCAN("Scanning "), RESCAN("Rescanning ");

		private final String statusPrefix;

		ScanMode(String statusPrefix) {
			this.statusPrefix = statusPrefix;
		}

		public String getStatusPrefix() {
			return statusPrefix;
		}
	}

	private FileTreeNode currentDirectoryNode = null;
	private boolean interrupted = false;
	private boolean interruptRequested = false;
	private long directoriesVisited = 0;
	private long directoriesToScan = 1;
	private Path currentRootPath = null;
	private ScanMode scanMode = ScanMode.INITIAL_SCAN;

	private Library library;
	private FXUI ui;
	LibraryLoader loader;
	LibraryScanLogger scanLogger;
	
	DiskReader(Library library, LibraryLoader loader, LibraryScanLogger scanLogger) {
		this.loader = loader;
		this.library = library;
		this.scanLogger = scanLogger;
	}

	void setUI(FXUI ui) {
		this.ui = ui;
	}

	void interrupt() {
		interruptRequested = true;
	}

	private void resetState() {
		currentDirectoryNode = null;
		interrupted = false;
		interruptRequested = false;
		directoriesVisited = 0;
		directoriesToScan = 1;
		currentRootPath = null;
		scanMode = ScanMode.INITIAL_SCAN;
	}

	// TODO: make sure we're not already scanning, if so throw an error
	void scanMusicRoot(MusicRoot musicRoot, ScanMode scanMode) {
		resetState();
		this.scanMode = scanMode;
		scanLogger.println("[DiskReader] " + scanMode.statusPrefix + " root: " + musicRoot.getPath().toString());
		try {
			directoriesToScan = LibraryLoader.getDirectoryCount(musicRoot.getPath());
			currentRootPath = musicRoot.getPath();
			ui.setLibraryLoaderStatus(scanMode.getStatusPrefix() + " " + currentRootPath.toString() + "...", 0, this);
			library.getDiskWatcher().watchAll(musicRoot.getPath());
	
			if (scanMode == ScanMode.INITIAL_SCAN) {
				musicRoot.setNeedsRescan(false);
			}
		
			Files.walkFileTree(musicRoot.getPath(), EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, this);
			switch (scanMode) {
			case INITIAL_SCAN:
				musicRoot.setNeedsInitialScan(interrupted);
				break;
			case RESCAN:
				musicRoot.setNeedsRescan(interrupted);
				break;
			}

		} catch (Exception e) {
			scanLogger.println("[DiskReader] Scan failed or incomplete for path, giving up: " + musicRoot.getPath());
			scanLogger.println(ExceptionUtils.getStackTrace(e));
			musicRoot.setNeedsRescan(false);
			musicRoot.setFailedScan(true);
		}

		if (ui != null) {
			ui.setLibraryLoaderStatusToStandby(this);
		}
	}

	// TODO: make sure we're not already scanning, if so throw an error
	void updatePath(Path path) {
		resetState();
		this.scanMode = ScanMode.RESCAN;
		directoriesToScan = LibraryLoader.getDirectoryCount(path);
		currentRootPath = path;
		library.getDiskWatcher().watchAll(path);
		try {
			Files.walkFileTree(path, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, this);
		} catch (Exception e) {
			scanLogger.println("[DiskReader] Scan failed or incomplete for path, giving up: " + path);
			scanLogger.println(ExceptionUtils.getStackTrace(e));
			List<MusicRoot> roots = new ArrayList<>(library.getMusicRootData());
			for (MusicRoot root : roots) {
				if (Utils.isChildOf(path, root.getPath())) {
					root.setFailedScan(true);
				}
			}
		}
		if (ui != null) {
			ui.setLibraryLoaderStatusToStandby(this);
		}
	}
	
	private final List<Track> tracksInCurrentDirectory = new ArrayList<>();
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if (interruptRequested) {
			interrupted = true;
			return FileVisitResult.TERMINATE;
		}
		FileTreeNode directoryNode = new FileTreeNode(dir, currentDirectoryNode);
		if (currentDirectoryNode != null) {
			currentDirectoryNode.addChild(directoryNode);
		}
		currentDirectoryNode = directoryNode;
		if (ui != null) {
			ui.setLibraryLoaderStatus(scanMode.getStatusPrefix() + " " + currentRootPath.toString() + "...",
					directoriesVisited / (double) directoriesToScan, this);
		}
		tracksInCurrentDirectory.clear();
		for ( Track libraryTrack : library.getTrackData() ) {
			if ( libraryTrack.getPath().startsWith(dir) ) {
				tracksInCurrentDirectory.add(libraryTrack);
			}
		}	
		switch(Hypnos.getLoaderSpeed()) {
			case HIGH:
			default:
				break;
			case MED:
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					LOGGER.info("Interrupted during sleep in disk watcher, if this persists the loader speed may be ignored.");
				}
				break;
			case LOW:
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					LOGGER.info("Interrupted during sleep in disk watcher, if this persists the loader speed may be ignored.");
				}
				break;
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path filePath, BasicFileAttributes attr) {
		loader.pathUpdated(filePath);
		if (Utils.isMusicFile(filePath)) {
			Track track = null;
			for (Track libraryTrack : tracksInCurrentDirectory) {
				if (libraryTrack.getPath().equals(filePath)) {
					track = libraryTrack;
					break;
				}
			}
			if (track == null) {
				track = new Track(filePath);
				library.addTrack(track);
			} else {
				track.refreshTagData();
			}
			currentDirectoryNode.addChild(new FileTreeNode(filePath, currentDirectoryNode, track));
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
		directoriesVisited++;
		loader.pathUpdated(dir);
		if (isAlbum(currentDirectoryNode, scanLogger)) {
			List<Track> tracks = new ArrayList<>();
			for (FileTreeNode child : currentDirectoryNode.getChildren()) {
				if (child.getTrack() != null) {
					tracks.add(child.getTrack());
				}
			}
			Album album = null;
			for(Album libraryAlbum : library.getAlbumData()) {
				if(libraryAlbum.getPath().equals(currentDirectoryNode.getPath())) {
					album = libraryAlbum;
				}
			}
			if(album == null ) {
				album = new Album(currentDirectoryNode.getPath(), tracks);
				scanLogger.println("[DiskReader] Loading new album: " + album.getAlbumArtist() + " - " + album.getAlbumTitle());
				library.addAlbum(album);
			} else {
				album.setTracks(tracks);
				scanLogger.println("[DiskReader] Updating album: " + album.getAlbumArtist() + " - " + album.getAlbumTitle());
			}
			currentDirectoryNode.setAlbum(album);
		} else {
			library.notAnAlbum(currentDirectoryNode.getPath());
		}
		if (currentDirectoryNode.getParent() != null) {
			currentDirectoryNode = currentDirectoryNode.getParent();
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException {
		LOGGER.log(Level.INFO, "Unable to scan" + file, exception);
		return FileVisitResult.CONTINUE;
	}

	public boolean isRescanning() {
		return scanMode == ScanMode.RESCAN;
	}
	
	static boolean isAlbum(FileTreeNode node, LibraryScanLogger libraryLog) {

		if (!Files.isDirectory(node.getPath())) {
			libraryLog.println( "[LibraryLoader] Album rejected, not a directory: " + node.getPath() );
			return false;
		}

		String albumName = null;
		String artistName = null;

		int childTrackCount = 0;

		for (FileTreeNode child : node.getChildren()) {
			if (child.getAlbum() != null) {
				libraryLog.println( "[LibraryLoader] Album rejected, subdirectory is an album: " + node.getPath() );
				return false;
			}

			if (child.getArtist() != null) {
				libraryLog.println( "[LibraryLoader] Album rejected, no artist specified: " + node.getPath() );
				return false;
			}

			if (child.getTrack() != null) {
				childTrackCount++;

				if (albumName == null) {
					albumName = Utils.prepareAlbumForCompare(child.getTrack().getAlbumTitle());
					artistName = Utils.prepareArtistForCompare(child.getTrack().getAlbumArtist());

				} else {
					// We usually use weighted ratio, but that can return 0 for album names like ()
					// even if the strings are identical.
					// In that case, we switch to straight ratio, which doesn't have this problem

					int albumMatchPercent = FuzzySearch.weightedRatio(albumName,
							Utils.prepareAlbumForCompare(child.getTrack().getAlbumTitle()));
					if (albumMatchPercent == 0)
						albumMatchPercent = FuzzySearch.ratio(albumName,
								Utils.prepareAlbumForCompare(child.getTrack().getAlbumTitle()));
					if (albumMatchPercent < 90) {
						libraryLog.println( "[LibraryLoader] Album rejected, album name in tags too different: " + node.getPath() );
						return false;
					}

					int artistMatchPercent = FuzzySearch.weightedRatio(artistName,
							Utils.prepareArtistForCompare(child.getTrack().getAlbumArtist()));
					if (artistMatchPercent == 0)
						albumMatchPercent = FuzzySearch.ratio(artistName,
								Utils.prepareAlbumForCompare(child.getTrack().getAlbumArtist()));
					if (artistMatchPercent < 90) {

						libraryLog.println( "[LibraryLoader] Album rejected, artist name in tags too different: " + node.getPath() );
						return false;
					}
				}
			}
		}

		if (childTrackCount == 0) {
			libraryLog.println( "[LibraryLoader] Album rejected, no tracks: " + node.getPath() );
			return false;
		}

		return true;
	}
}