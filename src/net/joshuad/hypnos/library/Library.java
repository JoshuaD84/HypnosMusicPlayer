package net.joshuad.hypnos.library;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import net.joshuad.hypnos.AlphanumComparator;
import net.joshuad.hypnos.fxui.FXUI;

public class Library {
	private static final Logger LOGGER = Logger.getLogger(Library.class.getName());

	public enum LoaderSpeed {
		LOW, MED, HIGH
	}

	// These are all three representations of the same data. Add stuff to the
	// Observable List, the other two can't accept add.
	private final CachedList<Track> tracks = new CachedList<>();
	private final FilteredList<Track> tracksFiltered = new FilteredList<>(tracks.getDisplayItems(), p -> true);
	private final SortedList<Track> tracksSorted = new SortedList<>(tracksFiltered);

	private final CachedList<Album> albums = new CachedList<>();
	private final FilteredList<Album> albumsFiltered = new FilteredList<>(albums.getDisplayItems(), p -> true);
	private final SortedList<Album> albumsSorted = new SortedList<>(albumsFiltered);

	private final CachedList<Artist> artists = new CachedList<>();
	private final FilteredList<Artist> artistsFiltered = new FilteredList<>(artists.getDisplayItems(), p -> true);
	private final SortedList<Artist> artistsSorted = new SortedList<>(artistsFiltered);

	private final CachedList<Playlist> playlists = new CachedList<>();
	private final FilteredList<Playlist> playlistsFiltered = new FilteredList<>(playlists.getDisplayItems(), p -> true);
	private final SortedList<Playlist> playlistsSorted = new SortedList<>(playlistsFiltered);

	private final CachedList<TagError> tagErrors = new CachedList<>();
	private final FilteredList<TagError> tagErrorsFiltered = new FilteredList<>(tagErrors.getDisplayItems(), p -> true);
	private final SortedList<TagError> tagErrorsSorted = new SortedList<>(tagErrorsFiltered);

	private final CachedList<MusicRoot> musicRoots = new CachedList<>();

	private final LibraryLoader loader;
	private final LibraryDiskWatcher diskWatcher;
	private final LibraryScanLogger scanLogger = new LibraryScanLogger();

	private boolean dataNeedsToBeSavedToDisk = false;

	private boolean artistsNeedToBeRegenerated = false;
	private final Object artistSychronizeFlagLock = new Object();

	public Library() {
		diskWatcher = new LibraryDiskWatcher(this, scanLogger);
		loader = new LibraryLoader(this, scanLogger);

		InvalidationListener invalidationListener = new InvalidationListener() {

			@Override
			public void invalidated(Observable arg0) {
				dataNeedsToBeSavedToDisk = true;
				synchronized (artistSychronizeFlagLock) {
					artistsNeedToBeRegenerated = true;
				}
			}
		};

		tracks.addListenerToBase(invalidationListener);
		albums.addListenerToBase(invalidationListener);

		Thread artistGeneratorThread = new Thread() {
			@Override
			public void run() {
				while (true) {
					boolean doRegenerate = false;

					synchronized (artistSychronizeFlagLock) {
						doRegenerate = artistsNeedToBeRegenerated;
						artistsNeedToBeRegenerated = false;
					}

					if (doRegenerate) {
						artistsNeedToBeRegenerated = false;
						artists.getItemsCopy().setAll(generateArtists());
					}

					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						LOGGER.log(Level.FINE, "Sleep interupted during wait period.");
					}
				}
			}
		};

		artistGeneratorThread.setDaemon(true);
		artistGeneratorThread.start();
	}

	public void setUI(FXUI ui) {
		loader.setUI(ui);
		diskWatcher.setUI(ui);
	}

	public boolean dataNeedsToBeSavedToDisk() {
		return dataNeedsToBeSavedToDisk;
	}

	public SortedList<Playlist> getPlaylistsSorted() {
		return playlistsSorted;
	}

	public ObservableList<Playlist> getPlaylistData() {
		return playlists.getItemsCopy();
	}

	public ObservableList<Playlist> getPlaylistsDisplayCache() {
		return playlists.getDisplayItems();
	}

	public ObservableList<MusicRoot> getMusicRootData() {
		return musicRoots.getItemsCopy();
	}

	public ObservableList<MusicRoot> getMusicRootDisplayCache() {
		return musicRoots.getDisplayItems();
	}

	public FilteredList<Playlist> getPlaylistsFiltered() {
		return playlistsFiltered;
	}

	public SortedList<Album> getAlbumsSorted() {
		return albumsSorted;
	}

	public FilteredList<Album> getAlbumsFiltered() {
		return albumsFiltered;
	}

	public ObservableList<Track> getTrackDataCopy() {
		return tracks.getItemsCopy();
	}

	public ObservableList<Track> getTrackDisplayCache() {
		return tracks.getDisplayItems();
	}

	public ObservableList<Album> getAlbumData() {
		return albums.getItemsCopy();
	}

	public ObservableList<Album> getAlbumDisplayCache() {
		return albums.getDisplayItems();
	}

	public ObservableList<Artist> getArtistData() {
		return artists.getItemsCopy();
	}

	public ObservableList<Artist> getArtistDisplayCache() {
		return artists.getDisplayItems();
	}

	public SortedList<Track> getTracksSorted() {
		return tracksSorted;
	}

	public FilteredList<Track> getTracksFiltered() {
		return tracksFiltered;
	}

	public SortedList<Artist> getArtistsSorted() {
		return artistsSorted;
	}

	public FilteredList<Artist> getArtistsFiltered() {
		return artistsFiltered;
	}

	public SortedList<TagError> getTagErrorsSorted() {
		return tagErrorsSorted;
	}

	public void requestRescan(Path path) {
		loader.queueUpdatePath(path);
	}

	public void setMusicRootsOnInitialLoad(ArrayList<MusicRoot> roots) {
		for (MusicRoot musicRoot : roots) {
			musicRoot.setNeedsRescan(true);
		}
		musicRoots.getItemsCopy().setAll(roots);
	}

	public void setDataOnInitialLoad(ArrayList<Track> tracks, ArrayList<Album> albums) {
		this.tracks.getItemsCopy().setAll(tracks);
		this.albums.getItemsCopy().setAll(albums);
		this.artists.getItemsCopy().setAll(generateArtists());
	}

	public void requestRescan(List<Album> albums) {
		for (Album album : albums) {
			requestRescan(album.getPath());
		}
	}

	public void addMusicRoot(Path path) {
		musicRoots.addOrReplaceItem(new MusicRoot(path), true);
	}

	public void removeMusicRoot(MusicRoot musicRoot) {
		loader.interruptDiskReader();
		musicRoots.remove(musicRoot, true);
	}

	public void removePlaylist(Playlist playlist) {
		playlists.remove(playlist, true);
	}

	public void addPlaylist(Playlist playlist) {
		playlists.addOrReplaceItem(playlist, true);
	}

	public String getUniquePlaylistName() {
		return getUniquePlaylistName("New Playlist");
	}

	public void startThreads() {
		loader.start();
	}

	public LibraryScanLogger getScanLogger() {
		return scanLogger;
	}

	public String getUniquePlaylistName(String base) {
		String name = base;

		int number = 0;

		while (true) {

			boolean foundMatch = false;
			for (Playlist playlist : playlists.getItemsCopy()) {
				if (playlist.getName().toLowerCase().equals(name.toLowerCase())) {
					foundMatch = true;
				}
			}

			if (foundMatch) {
				number++;
				name = base + " " + number;
			} else {
				break;
			}
		}

		return name;
	}

	private List<Artist> generateArtists() {
		List<Artist> newArtistList = new ArrayList<>();

		List<Album> libraryAlbums = new ArrayList<>(albums.getItemsCopy());
		Album[] albumArray = libraryAlbums.toArray(new Album[libraryAlbums.size()]);

		AlphanumComparator comparator = new AlphanumComparator(AlphanumComparator.CaseHandling.CASE_INSENSITIVE);
		Arrays.sort(albumArray, Comparator.comparing(Album::getAlbumArtist, comparator));

		Artist lastArtist = null;
		for (Album album : albumArray) {
			if (album.getAlbumArtist().isBlank()) {
				continue;
			}
			
			if (lastArtist != null && lastArtist.getName().equals(album.getAlbumArtist())) {
				lastArtist.addAlbum(album);
			} else {
				Artist artist = null;

				for (Artist test : newArtistList) {
					if (test.getName().equalsIgnoreCase(album.getAlbumArtist())) {
						artist = test;
						break;
					}
				}

				if (artist == null) {
					artist = new Artist(album.getAlbumArtist());
					newArtistList.add(artist);
				}
				artist.addAlbum(album);
				lastArtist = artist;
			}
		}

		List<Track> looseTracks = new ArrayList<>();
		for (Track track : tracks.getItemsCopy()) {
			if (track.getAlbum() == null) {
				looseTracks.add(track);
			}
		}

		Track[] trackArray = looseTracks.toArray(new Track[looseTracks.size()]);
		Arrays.sort(trackArray, Comparator.comparing(Track::getAlbumArtist, comparator));

		lastArtist = null;
		for (Track track : trackArray) {
			if (track.getAlbumArtist().isBlank())
				continue;
			if (lastArtist != null && lastArtist.getName().equals(track.getAlbumArtist())) {
				lastArtist.addLooseTrack(track);
			} else {
				Artist artist = null;

				for (Artist test : newArtistList) {
					if (test.getName().equalsIgnoreCase(track.getAlbumArtist())) {
						artist = test;
						break;
					}
				}

				if (artist == null) {
					artist = new Artist(track.getAlbumArtist());
					newArtistList.add(artist);
				}
				artist.addLooseTrack(track);
				lastArtist = artist;
			}
		}
		return newArtistList;
	}

	LibraryLoader getLoader() {
		return loader;
	}

	LibraryDiskWatcher getDiskWatcher() {
		return diskWatcher;
	}

	public void setDataNeedsToBeSavedToDisk(boolean b) {
		this.dataNeedsToBeSavedToDisk = b;
	}

	void addTrackData(Track track) {
		tracks.addOrReplaceItem(track);
	}

	void addTrackData(List<Track> addMe) {
		for (Track track : addMe) {
			addTrackData(track);
		}
	}

	void removeTrack(Track track) {
		tracks.remove(track);
	}

	void addAlbumData(Album album) {
		albums.addOrReplaceItem(album);
	}

	void notAnAlbum(Path path) {
		for (Album album : albums.getItemsCopy()) {
			if (album.getPath().equals(path)) {
				albums.remove(album);
				break;
			}
		}
	}

	void removeAlbum(Album album) {
		albums.remove(album);
	}
}
