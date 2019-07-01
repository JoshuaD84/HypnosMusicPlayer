package net.joshuad.hypnos.library;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class CachedList<T> {
	private static final Logger LOGGER = Logger.getLogger(CachedList.class.getName());
	
	ObservableList<T> items = FXCollections.observableArrayList(new ArrayList<T>());
	Lock itemsLock = new ReentrantLock();
	
	ObservableList<T> displayCache = FXCollections.observableArrayList(new ArrayList<T>());
	List<Action<T>> pendingChanges = new ArrayList<>();

	
	boolean runLaterPending = false;
	
	public CachedList() {
		items.addListener( new ListChangeListener<T>() {
			@Override
			public void onChanged(Change<? extends T> change) {
				while (change.next()) {
					if (change.wasPermutated()) {
						for (int i = change.getFrom(); i < change.getTo(); ++i) {
							//I don't care about order, ignoring
						}
					} else if (change.wasUpdated()) {
						// update item
					} else {
						for (T removedItem : change.getRemoved()) {
							pendingChanges.add(new Action<T>(Action.ChangeType.REMOVE, removedItem));
						}
						for (T addedItem : change.getAddedSubList()) {
							pendingChanges.add(new Action<T>(Action.ChangeType.ADD, addedItem));
						}
					}
				}
			}
		});
		
		Thread changeExecutor = new Thread() {
			@Override
			public void run() {
				 while (true) {
		        if (!runLaterPending) {
		          pushChanges();
		        }

		        try {
		          Thread.sleep(100);
		        } catch (InterruptedException e) {
		          LOGGER.log(Level.FINE, "Sleep interupted during wait period.");
		        }
		      }
				
			}
		};
		
		changeExecutor.setDaemon(true);
		changeExecutor.start();
	}
	
	public void remove(T removeMe) {
		if(Platform.isFxApplicationThread()) {
			LOGGER.warning("Modifying the base list while on UI Thread. This is likely a bug, but trying to continue.");
		}
		if(removeMe == null) {
			LOGGER.warning("Asked to remove a null item from list, ignoring.");
			return;
		}
		
		try {
			itemsLock.lock();	
			items.remove(removeMe);
		} finally {
			itemsLock.unlock();
		}
	}
	
	public void addOrReplaceItem(T addMe) {
		if(Platform.isFxApplicationThread()) {
			LOGGER.warning("Modifying the base list while on UI Thread. This is likely a bug, but trying to continue.");
		}
		if(addMe == null) {
			LOGGER.warning("Asked to add a null item to list, ignoring.");
			return;
		}
		
		try {
			itemsLock.lock();	
			T alreadyInList = null;
			
			for (T item : items) {
				if(addMe.equals(item)) {
					alreadyInList = item;
					break;
				}
			}
			
			//TODO: maybe work on an update mechanism rather than this remove/add
			if(alreadyInList!=null) {
				items.remove(alreadyInList);
			} 
				
			items.add(addMe);

		} finally {
			itemsLock.unlock();
		}
	}
	
	public ObservableList<T> getItemsCopy() {
		try {
			itemsLock.lock();
			ObservableList<T> retMe = FXCollections.observableList(items);
			return retMe;
		} finally {
			itemsLock.unlock();
		}
	}
	
	public ObservableList<T> getDisplayItems() {
		if(!Platform.isFxApplicationThread()) {
			LOGGER.warning("Asked for display items while not on FX thread, this is likely a bug, but continuing.");
		}
		return displayCache;
	}
	
	private void pushChanges() {
    if (!pendingChanges.isEmpty()) {
    	runLaterPending = true;
      Platform.runLater(() -> {
        long startTime = System.currentTimeMillis();
        try {
        	if(itemsLock.tryLock(200, TimeUnit.MILLISECONDS)) {
	          while ( pendingChanges.size() > 0 && System.currentTimeMillis() - startTime < 400 ) {
	          	Action<T> action = pendingChanges.remove( 0 );
	            switch (action.getType()) {
							case ADD:
								displayCache.add(action.getItem());
								break;
							case REMOVE:
								displayCache.remove(action.getItem());
								break;
							case UPDATE:
								break;
							default:
								break;
	            }
	          }
        	}
        } catch(Exception e) {
        	LOGGER.log(Level.INFO, "Exception while trying to acquire lock, continuing.", e);
        } finally {
          runLaterPending = false;
          itemsLock.unlock();
        }
      });
    }
	}

	public void addListenerToBase(InvalidationListener listener) {
		items.addListener(listener);
	}
}

class Action<T> {
	public enum ChangeType {
		ADD, REMOVE, UPDATE
	};
	
	T item;
	ChangeType type;
	
	public Action(ChangeType type, T item) {
		this.item = item;
		this.type = type;
	}
	
	public T getItem() {
		return item;
	}
	
	public ChangeType getType() {
		return type;
	}
}
