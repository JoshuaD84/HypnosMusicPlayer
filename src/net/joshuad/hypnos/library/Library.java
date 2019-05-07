package net.joshuad.hypnos.library;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import net.joshuad.hypnos.AlphanumComparator;
import net.joshuad.hypnos.fxui.FXUI;

public class Library {

	public enum LoaderSpeed {
		LOW, MED, HIGH
	}
  // private static final Logger LOGGER = Logger.getLogger( Library.class.getName() );

  // These are all three representations of the same data. Add stuff to the
  // Observable List, the other two can't accept add.
  final ObservableList<Track> tracks = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(new ArrayList<Track>()));
  final FilteredList<Track> tracksFiltered = new FilteredList<>(tracks, p -> true);
  final SortedList<Track> tracksSorted = new SortedList<>(tracksFiltered);

  final ObservableList<Album> albums = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(new ArrayList<Album>()));
  final FilteredList<Album> albumsFiltered = new FilteredList<>(albums, p -> true);
  final SortedList<Album> albumsSorted = new SortedList<>(albumsFiltered);

  final ObservableList<Artist> artists = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(new ArrayList<Artist>()));
  final FilteredList<Artist> artistsFiltered = new FilteredList<>(artists, p -> true);
  final SortedList<Artist> artistsSorted = new SortedList<>(artistsFiltered);

  final ObservableList<Playlist> playlists = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(new ArrayList<Playlist>()));
  final FilteredList<Playlist> playlistsFiltered = new FilteredList<>(playlists, p -> true);
  final SortedList<Playlist> playlistsSorted = new SortedList<>(playlistsFiltered);

  final ObservableList<TagError> tagErrors = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(new ArrayList<TagError>()));
  final FilteredList<TagError> tagErrorsFiltered = new FilteredList<>(tagErrors, p -> true);
  final SortedList<TagError> tagErrorsSorted = new SortedList<>(tagErrorsFiltered);

  final ObservableList<MusicRoot> musicRoots = FXCollections.observableArrayList();

  private final LibraryLoader loader;
  private final LibraryDiskWatcher diskWatcher;
  private final LibraryMerger merger;
  
  private boolean dataNeedsToBeSavedToDisk = false;

  public Library() {
    merger = new LibraryMerger(this);
    diskWatcher = new LibraryDiskWatcher(this);
    loader = new LibraryLoader(this);
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

  public List<Track> getTracksCopy() {
  		return new ArrayList<>(tracks); 
  }
  
  public void requestRescan(Path path) {
    loader.queueUpdatePath(path);
  }
  
  public void setMusicRootsOnInitialLoad( ArrayList<MusicRoot> roots ) {
		for ( MusicRoot musicRoot : roots ) {
			musicRoot.setNeedsRescan( true );
		}
  	musicRoots.setAll( roots );
  }

  public void setDataOnInitialLoad(ArrayList<Track> tracks, ArrayList<Album> albums) {
    this.tracks.setAll(tracks);
    this.albums.setAll(albums);
    merger.setArtists( generateArtists() );
  }

  public Collection<MusicRoot> getMusicRoots() {
    return musicRoots;
  }
  
	public void requestRescan(List<Album> albums) {
		for (Album album : albums) {
			requestRescan(album.getPath());
		}
	}

	public void addMusicRoot(Path path) {
		loader.addMusicRoot(path);
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

	public String getUniquePlaylistName() {
		return getUniquePlaylistName ( "New Playlist" );
	}
	
	public void startThreads() {
		loader.start();
		merger.start();
	}
	
	private PrintStream dummy = new PrintStream(OutputStream.nullOutputStream());
	PrintStream getLibraryLog() {
		return dummy;
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
	
	List<Artist> generateArtists() {
  	List<Artist> newArtistList = new ArrayList<>();
		
		List<Album> libraryAlbums = new ArrayList<>(albums);
		Album[] albumArray = libraryAlbums.toArray( new Album[ libraryAlbums.size() ] );

		AlphanumComparator comparator = new AlphanumComparator ( AlphanumComparator.CaseHandling.CASE_INSENSITIVE );
		Arrays.sort( albumArray, Comparator.comparing( Album::getAlbumArtist, comparator ) );
		
		Artist lastArtist = null;
		for ( Album album : albumArray ) {
			if ( album.getAlbumArtist().isBlank() ) continue;
			
			if ( lastArtist != null && lastArtist.getName().equals( album.getAlbumArtist() ) ) {
				lastArtist.addAlbum ( album );
			} else {
				Artist artist = null;
				
				for ( Artist test : newArtistList ) {
					if ( test.getName().equalsIgnoreCase( album.getAlbumArtist() ) ) {
						artist = test;
						break;
					}
				}
				
				if ( artist == null ) {
					artist = new Artist ( album.getAlbumArtist() );
					newArtistList.add( artist );
				}
				artist.addAlbum ( album );
				lastArtist = artist;
			}
		}
		
		List<Track> libraryTracks = getTracksCopy();
		List<Track> looseTracks = new ArrayList<>();
		for ( Track track : libraryTracks ) {
			if ( track.getAlbum() == null ) {
				looseTracks.add( track );
			}
		}
		
		Track[] trackArray = looseTracks.toArray( new Track[ looseTracks.size() ] );
		Arrays.sort( trackArray, Comparator.comparing( Track::getAlbumArtist, comparator ) );
		
		lastArtist = null;
		for ( Track track : trackArray ) {
			if ( track.getAlbumArtist().isBlank() ) continue;
			if ( lastArtist != null && lastArtist.getName().equals( track.getAlbumArtist() ) ) {
				lastArtist.addLooseTrack ( track );
			} else {
				Artist artist = null;
				
				for ( Artist test : newArtistList ) {
					if ( test.getName().equalsIgnoreCase( track.getAlbumArtist()  ) ) {
						artist = test;
						break;
					}
				}
				
				if ( artist == null ) {
					artist = new Artist ( track.getAlbumArtist() );
					newArtistList.add( artist );
				}
				artist.addLooseTrack ( track );
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

	LibraryMerger getMerger() {
		return merger;
	}
}

