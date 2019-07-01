package net.joshuad.hypnos.library;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import net.joshuad.hypnos.Utils;
import net.joshuad.hypnos.fxui.FXUI;

class LibraryLoader {

	private static final Logger LOGGER = Logger.getLogger(LibraryLoader.class.getName());

	private final Vector<Path> musicRootsToAdd = new Vector<>();
	private final Vector<Path> pathsToUpdate = new Vector<>();

	private boolean clearOrphans = false;

	private Thread loaderThread;

	private Library library;

	private DiskReader diskReader;

	public LibraryLoader(Library library) {
		this.library = library;
		this.diskReader = new DiskReader(library);
		setupLoaderThread();
	}

	public void setUI(FXUI ui) {
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
		this.clearOrphans = true;
	}

	public void addMusicRoot(Path path) {
		if ( diskReader.isRescanning() ) {
				diskReader.interrupt();
		}
		
		if (Platform.isFxApplicationThread()) {
			library.getMusicRootData().add(new MusicRoot(path));
		} else {
			musicRootsToAdd.add(path);
		}
	}

	public void queueUpdatePath(Path path) {
		pathsToUpdate.add(path);
	}

	private long lastOrphanClearMS = 0;

	private void setupLoaderThread() {
		loaderThread = new Thread() {
			public void run() {
				while (true) {

					for (MusicRoot root : library.getMusicRootData()) {
						root.recheckValidity();
					}

					if (System.currentTimeMillis() - lastOrphanClearMS > 5000) {
						clearOrphans = true;
					}

					if (clearOrphans) {
						clearOrphans = false;
						lastOrphanClearMS = System.currentTimeMillis();
						clearOrphans();
					}

					if (!musicRootsToAdd.isEmpty()) {
						synchronized (musicRootsToAdd) {
							for (Path path : musicRootsToAdd) {
								library.getMusicRootData().add(new MusicRoot(path));
							}
							musicRootsToAdd.clear();
						}
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

							// remove any sub paths, because they got updated when we called
							// updateLibraryAtPath(
							// parent )
							List<Path> childPaths = new ArrayList<>();

							for (Path path : pathsToUpdate) {
								if (path.toAbsolutePath().startsWith(pathToUpdate)) {
									childPaths.add(path);
								}
							}

							pathsToUpdate.removeAll(childPaths);
						}
					}

					library.getDiskWatcher().processWatcherEvents();

					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
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
			
			for (Track track : library.getTrackDataCopy()) {
				if (track.getPath().toAbsolutePath().startsWith(path)) {
					library.getScanLogger().println("[LibraryLoader] Removing track data at: " + track.getPath());
					library.getTrackDataCopy().remove(track);
				}
			}

			for (Album album : library.getAlbumData()) {
				if (album.getPath().toAbsolutePath().startsWith(path)) {
					library.getScanLogger().println("[LibraryLoader] Removing album data at: " + path);
					//TODO: library.getMerger().removeAlbum(album);
					library.getDiskWatcher().stopWatching(album.getPath());
				}
			}

		} else if (Utils.isMusicFile(path)) {
			Track existingTrackAtPath = null;
			for (Track track : library.getTrackDataCopy()) {
				if (track.getPath().equals(path)) {
					existingTrackAtPath = track;
					break;
				}
			}

			if (existingTrackAtPath != null) {
				library.getScanLogger().println("[LibraryLoader] Updating track data at: " + path);
				existingTrackAtPath.refreshTagData();

				// This will make sure that any existing album gets updated, and if the
				// album has been destroyed on disk, it is removed from our library
				pathsToUpdate.add(existingTrackAtPath.getPath().getParent());

			} else {
				library.getScanLogger().println("[LibraryLoader] new track found at: " + path);
				Track newTrack = new Track(path);
				library.getTrackDataCopy().remove(newTrack);
			}

		} else if (Files.isDirectory(path)) {
			library.getScanLogger().println("[LibraryLoader] Doing directory rescan at: " + path);

			List<Path> childrenOfPath = new ArrayList<>();
			for (Path futureUpdate : pathsToUpdate) {
				if (Utils.isChildOf(futureUpdate, path)) {
					childrenOfPath.add(futureUpdate);
					library.getScanLogger().println("[LibraryLoader] - Removing future scan, its a child: " + path);
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

	private void clearOrphans() {
		
		if (library.getMusicRootData().size() == 0) {
			/*
			 * library.getAlbumData().clear();
			library.getTrackData().clear();
			library.getArtistData().clear();
			*/
		}
		
		List<Album> removeMe = new ArrayList<>();
		for (Album album : library.getAlbumData()) {
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
				library.getScanLogger().println( "[LibraryLoader] Orphan album pruned, no root: " + album.getPath() );
				removeMe.add(album);
			}
		}

		for (Album album : removeMe) {
			library.getAlbumData().remove(album);
			for (Track track : album.getTracks()) {
				library.getTrackDataCopy().remove(track);
			}
		}

		List<Track> removeMeTracks = new ArrayList<>();
		for (Track track : library.getTrackDataCopy()) {
			boolean hasRoot = false;
			for (MusicRoot root : library.getMusicRootData()) {
				if (Utils.isChildOf(track.getPath(), root.getPath())) {
					hasRoot = true;
					break;
				}
			}
			if (!hasRoot) {
				removeMeTracks.add(track);
				library.getScanLogger().println( "[LibraryLoader] Orphan track pruned, no root: " + track.getPath() );
			}
		}
		for (Track track : removeMeTracks) {
			library.getTrackDataCopy().remove(track);
		}
		library.getArtistData().setAll(library.generateArtists());
	}

	public void removeMusicRoot(MusicRoot musicRoot) {
		if (Platform.isFxApplicationThread()) {
			library.getMusicRootData().remove(musicRoot);
		}
		diskReader.interrupt();
		requestClearOrphans();
	}
}
