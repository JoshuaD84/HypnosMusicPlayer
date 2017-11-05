package net.joshuad.hypnos;

import java.util.List;

public class CurrentListTrackState {
	
	private boolean isCurrentTrack = false;
	private List <Integer> queueIndices;
	
	public CurrentListTrackState ( boolean isCurrentTrack, List <Integer> queueIndices ) {
		this.isCurrentTrack = isCurrentTrack;
		this.queueIndices = queueIndices;
	}
	
	public List <Integer> getQueueIndices () {
		return queueIndices;
	}
	
	public boolean getIsCurrentTrack () {
		return isCurrentTrack;
	}
}
