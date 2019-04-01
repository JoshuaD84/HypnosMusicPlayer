package net.joshuad.library;

public class UpdateAction {

  public enum ActionType {
    ADD_TRACK, REMOVE_TRACK, REPLACE_TRACK,
    ADD_ALBUM, REMOVE_ALBUM, UPDATE_ALBUM,
    ADD_PLAYLIST, REMOVE_PLAYLIST,
    ADD_MUSIC_ROOT, REMOVE_MUSIC_ROOT,
    CLEAR_ALL,
    REFRESH_TRACK_TABLE,
    REFRESH_ALBUM_TABLE,
    REFRESH_PLAYLIST_TABLE
  }

  private Object item1, item2;
  private ActionType actionType;

  public UpdateAction(Object item, ActionType actionType) {
    this.item1 = item;
    this.actionType = actionType;
    //TODO: error checking. is item instanceof not right? 
  }
  
  public UpdateAction(Object item1, Object item2, ActionType actionType) {
    this.item1 = item1;
    this.item2 = item2;
    this.actionType = actionType;
    //TODO: error checking. is item instanceof not right? 
  }

  public Object getItem() {
    return item1;
  }
  
  public Object getItem2() {
  	return item2;
  }
  
  public ActionType getActionType() {
    return actionType;
  }

	public void setItem(Object item) {
		this.item1 = item;
	}
}
