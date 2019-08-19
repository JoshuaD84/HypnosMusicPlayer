package net.joshuad.hypnos.library;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.fxui.FXUI;

class LibraryLoader {
	private static final Logger LOGGER = Logger.getLogger(LibraryLoader.class.getName());
	private final ArrayList<Path> pathsToUpdate = new ArrayList<>();
	private boolean clearOrphansAndMissing = false;
	private boolean musicRootRemoved = false;
	private Thread loaderThread;
	private Library library;
	private DiskReader diskReader;
	private LibraryScanLogger scanLogger;
	private FXUI ui;
	
	public LibraryLoader(Library library, LibraryScanLogger scanLogger) {
		this.library = library;
		this.scanLogger = scanLogger;
		this.diskReader = new DiskReader(library, this, scanLogger);
		setupLoaderThread();
	}

	public void setUI(FXUI ui) {
		this.ui = ui;
		diskReader.setUI(ui);
	}

	public void start() {
		if (!loaderThread.isAlive()) {
			loaderThread.start(); 
		} else {
			LOGGER.log(Level.INFO, "Disk Scanner thread asked to start, but it's already running, request ignored.");
		}
	}

	void requestClearOrphans() {
		this.clearOrphansAndMissing = true;
	}

	public void queueUpdatePath(Path path) {
		if (!pathsToUpdate.contains(path)) {
			pathsToUpdate.add(path);
		}
	}

	private long lastOrphanClearMS = 0;

	private void setupLoaderThread() {
		loaderThread = new Thread() {
			public void run() {
				while (true) {
					try {
						for (MusicRoot root : library.getMusicRootData()) {
							root.recheckValidity();
						}
						if (System.currentTimeMillis() - lastOrphanClearMS > 5000) {
							clearOrphansAndMissing = true;
						}
						if (clearOrphansAndMissing || musicRootRemoved) {
							String message = musicRootRemoved ? "Removing Items..." : "";
							musicRootRemoved = false;
							clearOrphansAndMissing = false;
							lastOrphanClearMS = System.currentTimeMillis();
							clearOrphans(message);
							clearMissing();
							if (!message.isBlank()) {
								ui.setLibraryLoaderStatusToStandby(null);
							}
							musicRootRemoved = false;
						}
						List<MusicRoot> libraryRoots = new ArrayList<>(library.getMusicRootData());
						for (MusicRoot root : libraryRoots) {
							if (root.needsInitialScan()) {
								diskReader.scanMusicRoot(root, DiskReader.ScanMode.INITIAL_SCAN);
							}
						}
						libraryRoots = new ArrayList<>(library.getMusicRootData());
						for (MusicRoot root : libraryRoots) {
							if (root.needsRescan()) {
								diskReader.scanMusicRoot(root, DiskReader.ScanMode.RESCAN);
							}
						}
						if (!pathsToUpdate.isEmpty()) {
							synchronized (pathsToUpdate) {
								Path pathToUpdate = pathsToUpdate.remove(0).toAbsolutePath();
								updateLibraryAtPath(pathToUpdate);
							}
						}
						library.getDiskWatcher().processWatcherEvents();
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
						}
					} catch (Exception e) {
						LOGGER.log(Level.INFO, "Caught an unhandled exception in loader loop, continuing.", e);
					}
				}
			}
		};
		loaderThread.setName("Library Loader");
		loaderThread.setDaemon(true);
	}

	public void updateLibraryAtPath(Path path) {
		path = path.toAbsolutePath();
		if (!Files.exists(path)) {
			List<Track> tracksToRemove = new ArrayList<>();
			for (Track track : library.getTrackData()) {
				if (track.getPath().toAbsolutePath().startsWith(path)) {
					tracksToRemove.add(track);
				}
			}
			for (Track track : tracksToRemove) {
				scanLogger.println("[LibraryLoader] Removing track data from track list: " + track.getPath());
				library.removeTrack(track);
			}
			List<Album> albumsToRemove = new ArrayList<>();
			for (Album album : library.getAlbumData()) {
				if (album.getPath().toAbsolutePath().startsWith(path)) {
					albumsToRemove.add(album);
				}
			}
			for (Album album : albumsToRemove) {
				scanLogger.println("[LibraryLoader] Removing album data from album list: " + path);
				library.removeAlbum(album);
				library.getDiskWatcher().stopWatching(album.getPath());
			}
		} else if (Utils.isMusicFile(path)) {
			Track existingTrackAtPath = null;
			for (Track track : library.getTrackData()) {
				if (track.getPath().equals(path)) {
					existingTrackAtPath = track;
					break;
				}
			}
			if (existingTrackAtPath != null) {
				scanLogger.println("[LibraryLoader] Updating track data at: " + path);
				existingTrackAtPath.refreshTagData();
				// This will make sure that any existing album gets updated, and if the
				// album has been destroyed on disk, it is removed from our library
				pathsToUpdate.add(existingTrackAtPath.getPath().getParent());
				library.requestRegenerateArtists();
			} else {
				scanLogger.println("[LibraryLoader] new track found at: " + path);
				Track newTrack = new Track(path, true);
				library.getTrackData().remove(newTrack);
			}
		} else if (Files.isDirectory(path)) {
			scanLogger.println("[LibraryLoader] Doing directory rescan at: " + path);
			List<Path> childrenOfPath = new ArrayList<>();
			for (Path futureUpdate : pathsToUpdate) {
				if (Utils.isChildOf(futureUpdate, path)) {
					childrenOfPath.add(futureUpdate);
					scanLogger.println("[LibraryLoader] - Removing future scan, its a child: " + path);
				}
			}
			pathsToUpdate.removeAll(childrenOfPath);
			diskReader.updatePath(path);
		}
	}

	static long getDirectoryCount(Path dir) {
		long retMe = 5000;
		try {
			retMe = Files.walk(dir, FileVisitOption.FOLLOW_LINKS).parallel().filter(p -> p.toFile().isDirectory()).count();
		} catch (Exception e) {
			LOGGER.log(Level.INFO, "Unable to count subdirectories, loader status bar will not be accurate", e);
		}
		return retMe;
	}

	private void clearMissing() {
		List<Album> removeMeAlbums = new ArrayList<>();
		for (Album album : library.getAlbumData()) {
			if(!Files.isDirectory(album.getPath())) {
				removeMeAlbums.add(album);
			}
		}
		for (Album album : removeMeAlbums) {
			library.removeAlbum(album);
			scanLogger.println( "[LibraryLoader] Album pruned, directory missing from disk: " + album.getPath() );
			//No need to remove tracks from the album, they'll be removed below
		}
		List<Track> removeMeTracks = new ArrayList<>();
		for (Track track : library.getTrackData()) {
			if(!Files.isRegularFile(track.getPath())) {
				removeMeTracks.add(track);
			}
		}
		for (Track track : removeMeTracks) {
			library.removeTrack(track);
			scanLogger.println( "[LibraryLoader] Track pruned, file missing from disk: " + track.getPath() );
		}
	}
		
	private void clearOrphans(String message) {
		int totalCount = library.getAlbumData().size() + library.getTrackData().size() + 10; //10 for the removal time
		int currentCount = 0;
		List<Album> removeMeAlbums = new ArrayList<>();
		for (Album album : library.getAlbumData()) {
			currentCount++;
			if(!message.isBlank()) {
				ui.setLibraryLoaderStatus(message, currentCount/(double)totalCount, this);
			}
			boolean hasRoot = false;
			for (MusicRoot root : library.getMusicRootData()) {
				if (Utils.isChildOf(album.getPath(), root.getPath())) {
					hasRoot = true;
					break;
				}
				if(album.getPath().equals(root.getPath())) {
					hasRoot = true;
					break;
				}
			}
			if (!hasRoot) {
				removeMeAlbums.add(album);
			}
		}
		for (Album album : removeMeAlbums) {
			library.removeAlbum(album);
			//No need to remove tracks from the album, they'll be removed below
			scanLogger.println( "[LibraryLoader] Orphan album pruned, no root: " + album.getPath() );
		}

		List<Track> removeMeTracks = new ArrayList<>();
		for (Track track : library.getTrackData()) {
			currentCount++;
			if(!message.isBlank()) {
				ui.setLibraryLoaderStatus(message, currentCount/(double)totalCount, this);
			}
			boolean hasRoot = false;
			for (MusicRoot root : library.getMusicRootData()) {
				if (Utils.isChildOf(track.getPath(), root.getPath())) {
					hasRoot = true;
					break;
				}
			}
			if (!hasRoot) {
				removeMeTracks.add(track);
			}
		}
		for (Track track : removeMeTracks) {
			library.removeTrack(track);
			scanLogger.println( "[LibraryLoader] Orphan track pruned, no root: " + track.getPath() );
		}
	}

	public void removeMusicRoot(MusicRoot musicRoot) {
		diskReader.interrupt();
		requestClearOrphans();
	}
	
	void interruptDiskReader() {
		diskReader.interrupt();
	}

	public void setMusicRootRemoved(boolean b) {
		musicRootRemoved = b;
	}

	void pathUpdated(Path path) {
		pathsToUpdate.remove(path);
	}
}
