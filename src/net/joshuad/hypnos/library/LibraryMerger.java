package net.joshuad.hypnos.library;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import net.joshuad.hypnos.AlphanumComparator;
import net.joshuad.hypnos.fxui.FXUI;
import net.joshuad.hypnos.library.UpdateAction.ActionType;

public class LibraryMerger {
  private static final Logger LOGGER = Logger.getLogger(LibraryMerger.class.getName());

  private Vector<UpdateAction> pendingActions = new Vector<>();

  Library library;
  
  private FXUI ui;

  private Thread mergerThread;

  private boolean runLaterPending = false;

  private final int sleepTimeMS = 400;

  public LibraryMerger(Library library) {
    this.library = library;

    mergerThread = new Thread(() -> {
      while (true) {
        if (!runLaterPending) {
          updateLibrary();
        }

        try {
          Thread.sleep(sleepTimeMS);
        } catch (InterruptedException e) {
          LOGGER.log(Level.FINE, "Sleep interupted during wait period.");
        }
      }
    });

    mergerThread.setName("Library Merger");
    mergerThread.setDaemon(true);
  }
  
	void setUI ( FXUI ui ) {
		this.ui = ui;
	}

  public void start() {
    if (!mergerThread.isAlive()) {
      mergerThread.start();
    } else {
      LOGGER.log(Level.INFO, "Library Merger thread asked to start, but it's already running, request ignored.");
    }
  }
  
  private void updateLibrary() {
    if (!pendingActions.isEmpty()) {
      runLaterPending = true;

      Platform.runLater(() -> {
        long startTime = System.currentTimeMillis();
        try {
          synchronized (pendingActions) {
          	boolean regenerateArtists = false;
	          while ( pendingActions.size() > 0 && System.currentTimeMillis() - startTime < 500 ) {
	          	UpdateAction action = pendingActions.remove( 0 );
              switch (action.getActionType()) {
                case ADD_MUSIC_ROOT:
                	if ( !library.musicRoots.contains( (MusicRoot) action.getItem() ) ) {
                		library.musicRoots.add((MusicRoot) action.getItem());
                	}
    	            library.setDataNeedsToBeSavedToDisk (true);
                  break;
                case REMOVE_MUSIC_ROOT:
                  library.musicRoots.remove((MusicRoot) action.getItem());
    	            library.setDataNeedsToBeSavedToDisk (true);
                  break;
                case ADD_ALBUM:
                  library.albums.add((Album) action.getItem());
                  regenerateArtists = true;
    	            library.setDataNeedsToBeSavedToDisk (true);
                  break;
                case REMOVE_ALBUM:
                  library.albums.remove((Album) action.getItem());
                  regenerateArtists = true;
    	            library.setDataNeedsToBeSavedToDisk (true);
                  break;
                case UPDATE_ALBUM: 
                	Album updateMe = (Album)(((Object[])action.getItem())[0]);
                	Album newData = (Album)(((Object[])action.getItem())[1]);
                	updateMe.setData( newData );
                  regenerateArtists = true;
    	            library.setDataNeedsToBeSavedToDisk (true);
                	break;
                case ADD_TRACK:
                  library.getTracks().add((Track)action.getItem());
                  regenerateArtists = true;
    	            library.setDataNeedsToBeSavedToDisk (true);
                  break;
                case REMOVE_TRACK:
                  library.getTracks().remove((Track)action.getItem());
                  regenerateArtists = true;
    	            library.setDataNeedsToBeSavedToDisk (true);
                  break;
                case SET_ARTISTS:
                	library.getArtists().setAll((List<Artist>)action.getItem());
                	break;
                case CLEAR_ALL:
                	library.getTracks().clear();
                	library.albums.clear();
                	library.artists.clear();
                	ui.libraryCleared();
    	            library.setDataNeedsToBeSavedToDisk(true);
                	break;
                case REFRESH_TRACK_TABLE: 
                	if (ui != null) {
	                	ui.refreshTrackTable();
	                }
                  break;
                case REFRESH_ALBUM_TABLE:
                  if (ui != null) {
                  	ui.refreshAlbumTable();
                  }
                  break;
								case ADD_PLAYLIST:
									//TODO: 
									break;
								case REFRESH_PLAYLIST_TABLE:
									//TODO: 
									break;
								case REMOVE_PLAYLIST:
									//TODO: 
									break;
								default:
									break;
              }
	          }
	          
	          if ( regenerateArtists ) {
	          	library.artists.setAll( library.generateArtists() );
	          }
          }

        } finally {
          runLaterPending = false;
        }
      });
    }
  }

  /*---------------------------------------------------------------------------*/
  /* These methods are run off the JavaFX thread, and help prepare fast merges */
  /*---------------------------------------------------------------------------*/

  void addOrUpdateTrack(Track track) {
    if (track == null) {
    	library.getLog().println("[Merger] Asked to add a null track to library, ignoring");
      return;
    }
    
    synchronized (pendingActions) {
      boolean didUpdate = false;
      for (int k= 0; k < pendingActions.size(); k++) {
        UpdateAction action = pendingActions.get(k);
        if (action.getActionType() == ActionType.ADD_TRACK && track.equals(action.getItem())) {
          //We can do updates to data off the javafx thread because the values aren't observable. 
          ((Track)action.getItem()).setData(track);
          library.setDataNeedsToBeSavedToDisk (true);
          didUpdate = true;
        }
      }
      
      //TODO: This is potentially buggy, need to fix.
      // existingLibraryIndex can change from when we call indexOf to when we use the info
      /*
      int existingLibraryIndex = library.tracks.indexOf(track);
      if (existingLibraryIndex != -1) {
        //We can do updates to data off the javafx thread because the values aren't observable. 
        library.tracks.get(existingLibraryIndex).setData(track);
        library.setDataNeedsToBeSavedToDisk (true);
        didUpdate = true;
      }
      */

      if (!didUpdate) {
        pendingActions.add(new UpdateAction(track, ActionType.ADD_TRACK));
      } else {
        pendingActions.add(new UpdateAction(null, ActionType.REFRESH_TRACK_TABLE));
      }
    }
  }
  
  public void addOrUpdatePlaylist(Playlist playlist) {
    if (playlist == null) {
    	library.getLog().println("[Merger] Asked to add a null playlist to library, ignoring");
      return;
    }
    
    synchronized (pendingActions) {
      boolean didUpdate = false;
      for (int k= 0; k < pendingActions.size(); k++) {
        UpdateAction action = pendingActions.get(k);
        if (action.getActionType() == ActionType.ADD_PLAYLIST && playlist.equals(action.getItem())) {
          //We can do updates to data off the javafx thread because the values aren't observable. 
          ((Playlist)action.getItem()).setData(playlist);
          didUpdate = true;
        }
      }
      
      int existingLibraryIndex = library.playlists.indexOf(playlist);
      if (existingLibraryIndex != -1) {
        //We can do updates to data off the javafx thread because the values aren't observable. 
        library.playlists.get(existingLibraryIndex).setData(playlist);
        didUpdate = true;
      }
      
      if (!didUpdate) {
        pendingActions.add(new UpdateAction(playlist, ActionType.ADD_PLAYLIST));
      } else {
        pendingActions.add(new UpdateAction(null, ActionType.REFRESH_PLAYLIST_TABLE));
      }
    }
  }

  void addOrUpdateAlbum(Album album) {
    if (album == null) {
    	library.getLog().println("[Merger] Asked to add/update a null album to library, ignoring");
      return;
    }
    
    boolean didUpdate = false;
    synchronized (pendingActions) {
	    for (int k= 0; k < pendingActions.size(); k++) {
	      UpdateAction action = pendingActions.get(k);
	      if (action.getActionType() == ActionType.ADD_ALBUM && album.equals(action.getItem())) {
	        //We can do updates to data off the javafx thread because the values aren't observable. 
	        ((Album)action.getItem()).setData(album);
	        library.setDataNeedsToBeSavedToDisk(true);
	        didUpdate = true;
	      }
	    }
    }

    int existingLibraryIndex = library.albums.indexOf(album);
    if (existingLibraryIndex != -1) {
    	pendingActions.add(new UpdateAction(new Object[]{ library.albums.get(existingLibraryIndex), album }, ActionType.UPDATE_ALBUM));
      //We can do updates to data off the javafx thread because the values aren't observable. 
      didUpdate = true;
    }

    if (!didUpdate) {
      pendingActions.add(new UpdateAction(album, ActionType.ADD_ALBUM));
    }
  }

	void notAnAlbum(Path path) {
		Album foundAlbum = null;
		for (UpdateAction action : pendingActions) {
			if (action.getActionType() == ActionType.ADD_ALBUM) {
				if (((Album)action.getItem()).getPath().equals(path)) {
					foundAlbum = (Album)action.getItem();
					//Not breaking intentionally; imagine a queue that has add, remove, add at same path. 
				}
			}
		}
		
		if (foundAlbum == null) {
			List<Album> libraryAlbums = new ArrayList<>(library.albums);
			for (Album album : libraryAlbums) {
				if(album.getPath().equals(path)) {
					foundAlbum = album;
					break;
				}
			}
		}
		
		if ( foundAlbum != null) {
			for (Track track : foundAlbum.getTracks()) {
				track.setAlbum( null );
			}
			removeAlbum(foundAlbum);
		}
	}
  
	void addMusicRoot(MusicRoot musicRoot) {    
    pendingActions.add(new UpdateAction(musicRoot, ActionType.ADD_MUSIC_ROOT));
  }
  
	void removeMusicRoot(MusicRoot musicRoot) {    
    pendingActions.add(new UpdateAction(musicRoot, ActionType.REMOVE_MUSIC_ROOT));
  }
  
  void removeTrack(Track track) {
    pendingActions.add(new UpdateAction(track, ActionType.REMOVE_TRACK));
  }

  void removeAlbum(Album album) {
    pendingActions.add(new UpdateAction(album, ActionType.REMOVE_ALBUM));
  }
  
  public void removePlaylist(Playlist playlist) {
    pendingActions.add(new UpdateAction(playlist, ActionType.REMOVE_PLAYLIST));
  }

	void clearAll() {
    pendingActions.add(new UpdateAction(null, ActionType.CLEAR_ALL));
	}

	void setArtists(List<Artist> newArtistList) {
    pendingActions.add(new UpdateAction(newArtistList, ActionType.SET_ARTISTS));
	}
}
