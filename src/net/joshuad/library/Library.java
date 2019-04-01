package net.joshuad.library;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import net.joshuad.hypnos.fxui.FXUI;

public class Library {

	public enum LoaderSpeed {
		LOW, MED, HIGH
	}
  // private static final Logger LOGGER = Logger.getLogger( Library.class.getName() );

  // These are all three representations of the same data. Add stuff to the
  // Observable List, the other two can't accept add.
  final ObservableList<Track> tracks = FXCollections.observableArrayList(new ArrayList<Track>());
  final FilteredList<Track> tracksFiltered = new FilteredList<>(tracks, p -> true);
  final SortedList<Track> tracksSorted = new SortedList<>(tracksFiltered);

  final ObservableList<Album> albums = FXCollections.observableArrayList(new ArrayList<Album>());
  final FilteredList<Album> albumsFiltered = new FilteredList<>(albums, p -> true);
  final SortedList<Album> albumsSorted = new SortedList<>(albumsFiltered);

  final ObservableList<Artist> artists = FXCollections.observableArrayList(new ArrayList<Artist>());
  final FilteredList<Artist> artistsFiltered = new FilteredList<>(artists, p -> true);
  final SortedList<Artist> artistsSorted = new SortedList<>(artistsFiltered);

  final ObservableList<Playlist> playlists = FXCollections.observableArrayList(new ArrayList<Playlist>());
  final FilteredList<Playlist> playlistsFiltered = new FilteredList<>(playlists, p -> true);
  final SortedList<Playlist> playlistsSorted = new SortedList<>(playlistsFiltered);

  final ObservableList<TagError> tagErrors = FXCollections.observableArrayList(new ArrayList<TagError>());
  final FilteredList<TagError> tagErrorsFiltered = new FilteredList<>(tagErrors, p -> true);
  final SortedList<TagError> tagErrorsSorted = new SortedList<>(tagErrorsFiltered);

  final ObservableList<MusicRoot> musicRoots = FXCollections.observableArrayList();

  final LibraryLoader loader;
  final LibraryDiskWatcher diskWatcher;
  final LibraryMerger merger;
  
  private boolean dataNeedsToBeSavedToDisk = false;

  public Library() {
    loader = new LibraryLoader(this);
    diskWatcher = new LibraryDiskWatcher(this);
    merger = new LibraryMerger(this);
  }

  public void setUI(FXUI ui) {
    loader.setUI(ui);
    diskWatcher.setUI(ui);
    merger.setUI(ui);
  }

  public boolean dataNeedsToBeSavedToDisk() {
	return dataNeedsToBeSavedToDisk;
  }
  
  public void setDataNeedsToBeSavedToDisk(boolean needsSaving) {
    this.dataNeedsToBeSavedToDisk = needsSaving;
  }

  public SortedList <Playlist> getPlaylistsSorted () {
    return playlistsSorted;
  }
  
  public ObservableList <Playlist> getPlaylists () {
      return playlists;
  }
  
  public ObservableList <MusicRoot> getMusicSourcePaths () { //TODO: RENAME getMusicRoots
      return musicRoots;
  }
  
  public FilteredList <Playlist> getPlaylistsFiltered () {
      return playlistsFiltered;
  }
  
  public ObservableList <Album> getAlbums () {
      return albums;
  }
  
  public SortedList <Album> getAlbumsSorted() {
      return albumsSorted;
  }
  
  public FilteredList <Album> getAlbumsFiltered() {
      return albumsFiltered;
  }
  
  public ObservableList <Track> getTracks () {
      return tracks;
  }
  
  public SortedList <Track> getTracksSorted () {
      return tracksSorted;
  }
  
  public FilteredList <Track> getTracksFiltered () {
      return tracksFiltered;
  }
  
  public ObservableList <Artist> getArtists () {
      return artists;
  }
  
  public SortedList <Artist> getArtistsSorted() {
      return artistsSorted;
  }
  
  public FilteredList <Artist> getArtistsFiltered() {
      return artistsFiltered;
  } 
  
  public SortedList <TagError> getTagErrorsSorted () {
      return tagErrorsSorted;
  }

  public void requestRescan(Path path) {
    loader.queueUpdatePath(path);
  }

  public void setDataOnInitialLoad(ArrayList<Track> tracks, ArrayList<Album> albums) {
    this.tracks.addAll(tracks);
    this.albums.addAll(albums);
  }

  public Collection<MusicRoot> getMusicRoots() {
    return musicRoots;
  }

  public void requestRescan(List<Album> albums) {
    for (Album album : albums) {
      requestRescan( album.getPath() );
    }
  }

	public void addMusicRoot(Path path) {
		loader.queueScanMusicRoot(path);
	}

	public void removeMusicRoot(MusicRoot musicRoot) {
		loader.removeMusicRoot(musicRoot);
	}

	public void removePlaylist(Playlist playlist) {
		merger.removePlaylist(playlist);
	}

	public void addPlaylist(Playlist playlist) {
		merger.addOrUpdatePlaylist(playlist);
	}

	public boolean sourcesHasUnsavedData() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setRootsHasUnsavedData(boolean b) {
		// TODO Auto-generated method stub
	}
	
	public String getUniquePlaylistName() {
		return getUniquePlaylistName ( "New Playlist" );
	}
	
	public String getUniquePlaylistName( String base ) {
		String name = base;
		
		int number = 0;
		
		while ( true ) {
			
			boolean foundMatch = false;
			for ( Playlist playlist : playlists ) {
				if ( playlist.getName().toLowerCase().equals( name.toLowerCase() ) ) {
					foundMatch = true;
				}
			}
			
			if ( foundMatch ) {
				number++;
				name = base + " " + number;
			} else {
				break;
			}
		}
		
		return name;
	}

	public void startThreads() {
		loader.start();
		merger.start();
	}
}

