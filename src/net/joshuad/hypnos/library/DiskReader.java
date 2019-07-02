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
	LibraryScanLogger scanLogger;
	
	DiskReader(Library library, LibraryScanLogger scanLogger) {
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
		scanLogger.println("[MusicRootLoader] " + scanMode.statusPrefix + " root: " + musicRoot.getPath().toString());
		try {
			directoriesToScan = LibraryLoader.getDirectoryCount(musicRoot.getPath());
			currentRootPath = musicRoot.getPath();
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

		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path filePath, BasicFileAttributes attr) {
		if (Utils.isMusicFile(filePath)) {
			Track track = new Track(filePath);
			currentDirectoryNode.addChild(new FileTreeNode(filePath, currentDirectoryNode, track));
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {

		directoriesVisited++;

		if (LibraryLoader.isAlbum(currentDirectoryNode, scanLogger)) {
			List<Track> tracks = new ArrayList<>();
			for (FileTreeNode child : currentDirectoryNode.getChildren()) {
				if (child.getTrack() != null) {
					tracks.add(child.getTrack());
				}
			}

			Album album = new Album(currentDirectoryNode.getPath(), tracks);

			scanLogger.println("[MusicRootLoader] Loading album: " + album.getAlbumArtist() + " - " + album.getAlbumTitle());
			currentDirectoryNode.setAlbum(album);
			library.addTrackData(tracks);
			library.addAlbumData(album);


		} else {
			for (FileTreeNode child : currentDirectoryNode.getChildren()) {
				if (child.getTrack() != null) {
					library.addTrackData(child.getTrack());
				}
			}
			
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
}