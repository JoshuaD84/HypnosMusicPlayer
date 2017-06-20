package net.joshuad.hypnos;

import java.io.Serializable;

public class SocketCommand implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	enum CommandType {
		CONTROL,
		LOAD_TRACKS
	}
	
	private CommandType type;
	private Object object;
	
	public SocketCommand ( CommandType type, Object object ) {
		this.type = type;
		this.object = object;
	}
	
	public CommandType getType () { return type; }
	public Object getObject() { return object; }
}
