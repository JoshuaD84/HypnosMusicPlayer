package net.joshuad.hypnos.library;

import java.io.PrintStream;
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
		if (Platform.isFxApplicationThread()) {
			library.musicRoots.add(new MusicRoot(path));
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

					for (MusicRoot root : library.musicRoots) {
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
								library.musicRoots.add(new MusicRoot(path));
							}
							musicRootsToAdd.clear();
						}
					}
					
					List<MusicRoot> libraryRoots = new ArrayList<>(library.musicRoots);
					for (MusicRoot root : libraryRoots) {
						if (root.needsRescan()) {
							diskReader.scanMusicRoot(root);
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

					library.diskWatcher.processWatcherEvents();

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
			for (Track track : library.getTracksCopy()) {
				if (track.getPath().toAbsolutePath().startsWith(path)) {
					library.getLog().println("[LibraryLoader] Removing track data at: " + track.getPath());
					library.merger.removeTrack(track);
				}
			}

			for (Album album : library.albums) {
				if (album.getPath().toAbsolutePath().startsWith(path)) {
					library.getLog().println("[LibraryLoader] Removing album data at: " + path);
					library.merger.removeAlbum(album);
					library.diskWatcher.stopWatching(album.getPath());
				}
			}

		} else if (Utils.isMusicFile(path)) {
			Track existingTrackAtPath = null;
			for (Track track : library.getTracksCopy()) {
				if (track.getPath().equals(path)) {
					existingTrackAtPath = track;
					break;
				}
			}

			if (existingTrackAtPath != null) {
				library.getLog().println("[LibraryLoader] Updating track data at: " + path);
				existingTrackAtPath.refreshTagData();

				// This will make sure that any existing album gets updated, and if the
				// album has been destroyed on disk, it is removed from our library
				pathsToUpdate.add(existingTrackAtPath.getPath().getParent());

			} else {
				library.getLog().println("[LibraryLoader] new track found at: " + path);
				Track newTrack = new Track(path);
				library.merger.removeTrack(newTrack);
			}

		} else if (Files.isDirectory(path)) {
			library.getLog().println("[LibraryLoader] Doing directory rescan at: " + path);

			List<Path> childrenOfPath = new ArrayList<>();
			for (Path futureUpdate : pathsToUpdate) {
				if (Utils.isChildOf(futureUpdate, path)) {
					childrenOfPath.add(futureUpdate);
					library.getLog().println("[LibraryLoader] - Removing future scan, its a child: " + path);
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

	static boolean isAlbum(FileTreeNode node, PrintStream out) {

		if (!Files.isDirectory(node.getPath())) {
			out.println( "[LibraryLoader] Album rejected, not a directory: " + node.getPath() );
			return false;
		}

		String albumName = null;
		String artistName = null;

		int childTrackCount = 0;

		for (FileTreeNode child : node.getChildren()) {
			if (child.getAlbum() != null) {
				out.println( "[LibraryLoader] Album rejected, subdirectory is an album: " + node.getPath() );
				return false;
			}

			if (child.getArtist() != null) {
				out.println( "[LibraryLoader] Album rejected, no artist specified: " + node.getPath() );
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
						out.println( "[LibraryLoader] Album rejected, album name in tags too different: " + node.getPath() );
						return false;
					}

					int artistMatchPercent = FuzzySearch.weightedRatio(artistName,
							Utils.prepareArtistForCompare(child.getTrack().getAlbumArtist()));
					if (artistMatchPercent == 0)
						albumMatchPercent = FuzzySearch.ratio(artistName,
								Utils.prepareAlbumForCompare(child.getTrack().getAlbumArtist()));
					if (artistMatchPercent < 90) {

						out.println( "[LibraryLoader] Album rejected, artist name in tags too different: " + node.getPath() );
						return false;
					}
				}
			}
		}

		if (childTrackCount == 0) {
			out.println( "[LibraryLoader] Album rejected, no tracks: " + node.getPath() );
			return false;
		}

		return true;
	}

	private void clearOrphans() {
		
		synchronized (library.musicRoots) {
			if (library.musicRoots.size() == 0) {
				library.merger.clearAll();
				library.albums.clear();
				library.tracks.clear();
				library.artists.clear();
			}
		}
		
		synchronized (library.albums) {
			List<Album> removeMe = new ArrayList<>();
			for (Album album : library.albums) {
				boolean hasRoot = false;
				for (MusicRoot root : library.musicRoots) {
					if (Utils.isChildOf(album.getPath(), root.getPath())) {
						hasRoot = true;
						break;
					}
				}
				if (!hasRoot) {
					removeMe.add(album);
				}
			}

			for (Album album : removeMe) {
				library.merger.removeAlbum(album);
				for (Track track : album.getTracks()) {
					library.merger.removeTrack(track);
				}
			}
		}

		List<Track> libraryTracks = library.getTracksCopy();
		List<Track> removeMeTracks = new ArrayList<>();
		for (Track track : libraryTracks) {
			boolean hasRoot = false;
			for (MusicRoot root : library.musicRoots) {
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
			library.merger.removeTrack(track);
		}
	}

	public void removeMusicRoot(MusicRoot musicRoot) {
		if (Platform.isFxApplicationThread()) {
			library.musicRoots.remove(musicRoot);
		}
		library.merger.removeMusicRoot(musicRoot);
		diskReader.interrupt();
		requestClearOrphans();
	}
}
