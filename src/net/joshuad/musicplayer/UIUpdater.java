package net.joshuad.musicplayer;

import javafx.application.Platform;

public class UIUpdater {

	private static final int MAX_CHANGES_PER_REQUEST = 250;

	private static boolean runLaterPending = false;
	
	
	public static void init() {
		
		Thread uiUpdaterThread = new Thread( () -> {
			while ( true ) {
				
				if( !runLaterPending ) {
					UIUpdater.updateUI();
				}
				
				try {
					Thread.sleep ( 50 );
				} catch ( InterruptedException e ) {
					e.printStackTrace();
				}
			}
		});
		
		uiUpdaterThread.setDaemon( true );
		uiUpdaterThread.start();
	}
	
	private static void updateUI () { 
		if ( !Library.albumsToAdd.isEmpty() || !Library.albumsToRemove.isEmpty() || !Library.albumsToUpdate.isEmpty() 
		  || !Library.tracksToAdd.isEmpty() || !Library.tracksToRemove.isEmpty() || !Library.tracksToUpdate.isEmpty() 	
		  || !Library.playlistsToAdd.isEmpty() || !Library.playlistsToRemove.isEmpty() || !Library.playlistsToUpdate.isEmpty() 
		  ){
			
			runLaterPending = true;
			
			Platform.runLater( () -> {
				try {
				
					int changeCount = 0;
					
					synchronized ( Library.albumsToRemove ) {
						if ( !Library.albumsToRemove.isEmpty() ) {
							while ( changeCount < MAX_CHANGES_PER_REQUEST && !Library.albumsToRemove.isEmpty() ) {
								Library.albums.remove( Library.albumsToRemove.remove( 0 ) );
								changeCount++;
							}
							
							MusicPlayerUI.updateAlbumListPlaceholder();

							if ( changeCount >= MAX_CHANGES_PER_REQUEST ) {
								MusicPlayerUI.albumTable.refresh(); //TODO: this may not be necessary. 
								return;
							}
						}
					}
						
					synchronized ( Library.albumsToAdd ) {
						if ( !Library.albumsToAdd.isEmpty() ) {
							while ( changeCount < MAX_CHANGES_PER_REQUEST && !Library.albumsToAdd.isEmpty() ) {
								Library.albums.add( Library.albumsToAdd.remove( 0 ) );
								changeCount++;
							}
							
							MusicPlayerUI.updateAlbumListPlaceholder();

							if ( changeCount >= MAX_CHANGES_PER_REQUEST ) {
								MusicPlayerUI.albumTable.refresh(); //TODO: this may not be necessary. 
								return;
							}
						}
					}
					
					synchronized ( Library.albumsToUpdate ) {
						if ( !Library.albumsToUpdate.isEmpty() ) {
							while ( changeCount < MAX_CHANGES_PER_REQUEST && !Library.albumsToUpdate.isEmpty() ) {
								Album updateSource = Library.albumsToUpdate.remove( 0 );
								if ( Library.albums.contains( updateSource ) ) {
									Album updateMe = Library.albums.get( Library.albums.indexOf( updateSource ) );
									updateMe.refreshTagData();
									
								} else {
									Library.albums.add( updateSource );
								}
								changeCount += 2; //We charge two here because this is a costly transaction
 							}
							
							if ( changeCount >= MAX_CHANGES_PER_REQUEST ) {
								MusicPlayerUI.albumTable.refresh(); //TODO: this may not be necessary. 
								return;
							}
						}
					}
					
					synchronized ( Library.tracksToRemove ) {
						if ( !Library.tracksToRemove.isEmpty() ) {
							while ( changeCount < MAX_CHANGES_PER_REQUEST && !Library.tracksToRemove.isEmpty() ) {
								boolean removed = Library.tracks.remove( Library.tracksToRemove.remove( 0 ) );
								
								if ( removed ) {
									changeCount++;
								}
							}


							MusicPlayerUI.updateTrackListPlaceholder();
							
							if ( changeCount >= MAX_CHANGES_PER_REQUEST ) {
								MusicPlayerUI.trackTable.refresh(); //TODO: this may not be necessary. 
								return;
							}
						}
					}
						
					synchronized ( Library.tracksToAdd ) {
						if ( !Library.tracksToAdd.isEmpty() ) {
							while ( changeCount < MAX_CHANGES_PER_REQUEST && !Library.tracksToAdd.isEmpty() ) {
								Library.tracks.add( Library.tracksToAdd.remove( 0 ) );
								changeCount++;
							}


							MusicPlayerUI.updateTrackListPlaceholder();
							
							if ( changeCount >= MAX_CHANGES_PER_REQUEST ) {
								MusicPlayerUI.trackTable.refresh(); //TODO: this may not be necessary. 
								return;
							}
						}
					}
	
					Library.tracksToUpdate.clear();
					//TODO: Update tracks
					
					//TODO: make sure these don't violate MAX_CHANGES like above, but whatever do it later not gonna happen. 
					synchronized ( Library.playlistsToRemove ) {
						Library.playlists.removeAll( Library.playlistsToRemove );
						Library.playlistsToRemove.clear();

						MusicPlayerUI.updatePlaylistPlaceholder();
					}
				
					synchronized ( Library.playlistsToAdd ) {
						Library.playlists.addAll( Library.playlistsToAdd );
						Library.playlistsToAdd.clear();
					}
					
					Library.playlistsToUpdate.clear();

					MusicPlayerUI.updatePlaylistPlaceholder();
					
				} finally {
					runLaterPending = false;
				}
			});
		}
	}
}
