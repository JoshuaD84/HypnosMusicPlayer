package net.joshuad.hypnos;

import javafx.application.Platform;
import net.joshuad.hypnos.fxui.FXUI;

//TODO: This has an unclear mission. Does a few things. 
public class LibraryUpdater {

	private static final int MAX_CHANGES_PER_REQUEST = 250;

	private boolean runLaterPending = false;
	
	private FXUI ui;
	
	public LibraryUpdater( FXUI ui ) {
		this.ui = ui;
		
		Thread uiUpdaterThread = new Thread( () -> {
			while ( true ) {
				if( !runLaterPending ) {
					updateLibrary();
				}
				
				try {
					Thread.sleep ( 100 );
				} catch ( InterruptedException e ) {
					e.printStackTrace();
				}
			}
		});
		
		uiUpdaterThread.setDaemon( true );
		uiUpdaterThread.start();
	}
	
	private void updateLibrary () { 
		if ( !Hypnos.library().albumsToAdd.isEmpty() || !Hypnos.library().albumsToRemove.isEmpty() || !Hypnos.library().albumsToUpdate.isEmpty() 
		  || !Hypnos.library().tracksToAdd.isEmpty() || !Hypnos.library().tracksToRemove.isEmpty() || !Hypnos.library().tracksToUpdate.isEmpty() 	
		  || !Hypnos.library().playlistsToAdd.isEmpty() || !Hypnos.library().playlistsToRemove.isEmpty() || !Hypnos.library().playlistsToUpdate.isEmpty() 
		  ){
			
			runLaterPending = true;
			
			Platform.runLater( () -> {
				try {
				
					int changeCount = 0;
					
					synchronized ( Hypnos.library().albumsToRemove ) {
						if ( !Hypnos.library().albumsToRemove.isEmpty() ) {
							while ( changeCount < MAX_CHANGES_PER_REQUEST && !Hypnos.library().albumsToRemove.isEmpty() ) {
								Hypnos.library().albums.remove( Hypnos.library().albumsToRemove.remove( 0 ) );
								changeCount++;
							}
							
							ui.updateAlbumListPlaceholder();

							if ( changeCount >= MAX_CHANGES_PER_REQUEST ) {
								ui.refreshAlbumTable(); //TODO: this may not be necessary. 
								return;
							}
						}
					}
						
					synchronized ( Hypnos.library().albumsToAdd ) {
						if ( !Hypnos.library().albumsToAdd.isEmpty() ) {
							while ( changeCount < MAX_CHANGES_PER_REQUEST && !Hypnos.library().albumsToAdd.isEmpty() ) {
								Hypnos.library().albums.add( Hypnos.library().albumsToAdd.remove( 0 ) );
								changeCount++;
							}
							
							ui.updateAlbumListPlaceholder();

							if ( changeCount >= MAX_CHANGES_PER_REQUEST ) {
								ui.refreshAlbumTable();  //TODO: this may not be necessary. 
								return;
							}
						}
					}
					
					synchronized ( Hypnos.library().albumsToUpdate ) {
						if ( !Hypnos.library().albumsToUpdate.isEmpty() ) {
							while ( changeCount < MAX_CHANGES_PER_REQUEST && !Hypnos.library().albumsToUpdate.isEmpty() ) {
								Album updateSource = Hypnos.library().albumsToUpdate.remove( 0 );
								if ( Hypnos.library().albums.contains( updateSource ) ) {
									Album updateMe = Hypnos.library().albums.get( Hypnos.library().albums.indexOf( updateSource ) );
									updateMe.refreshTagData();
									
								} else {
									Hypnos.library().albums.add( updateSource );
								}
								changeCount += 2; //We charge two here because this is a costly transaction
 							}
							
							if ( changeCount >= MAX_CHANGES_PER_REQUEST ) {
								ui.refreshAlbumTable();  //TODO: this may not be necessary. 
								return;
							}
						}
					}
					
					synchronized ( Hypnos.library().tracksToRemove ) {
						if ( !Hypnos.library().tracksToRemove.isEmpty() ) {
							while ( changeCount < MAX_CHANGES_PER_REQUEST && !Hypnos.library().tracksToRemove.isEmpty() ) {
								boolean removed = Hypnos.library().tracks.remove( Hypnos.library().tracksToRemove.remove( 0 ) );
								
								if ( removed ) {
									changeCount++;
								}
							}


							ui.updateTrackListPlaceholder();
							
							if ( changeCount >= MAX_CHANGES_PER_REQUEST ) {
								ui.refreshTrackTable(); //TODO: this may not be necessary. 
								return;
							}
						}
					}
						
					synchronized ( Hypnos.library().tracksToAdd ) {
						if ( !Hypnos.library().tracksToAdd.isEmpty() ) {
							while ( changeCount < MAX_CHANGES_PER_REQUEST && !Hypnos.library().tracksToAdd.isEmpty() ) {
								Hypnos.library().tracks.add( Hypnos.library().tracksToAdd.remove( 0 ) );
								changeCount++;
							}


							ui.updateTrackListPlaceholder();
							
							if ( changeCount >= MAX_CHANGES_PER_REQUEST ) {
								ui.refreshTrackTable(); //TODO: this may not be necessary. 
								return;
							}
						}
					}
	
					Hypnos.library().tracksToUpdate.clear();
					//TODO: Update tracks
					
					//TODO: make sure these don't violate MAX_CHANGES like above, but whatever do it later not gonna happen. 
					synchronized ( Hypnos.library().playlistsToRemove ) {
						Hypnos.library().playlists.removeAll( Hypnos.library().playlistsToRemove );
						Hypnos.library().playlistsToRemove.clear();

						ui.updatePlaylistPlaceholder();
					}
				
					synchronized ( Hypnos.library().playlistsToAdd ) {
						Hypnos.library().playlists.addAll( Hypnos.library().playlistsToAdd );
						Hypnos.library().playlistsToAdd.clear();
					}
					
					Hypnos.library().playlistsToUpdate.clear();

					ui.updatePlaylistPlaceholder();
					
				} finally {
					runLaterPending = false;
				}
			});
		}
	}
}
