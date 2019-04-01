package net.joshuad.library;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

class FileTreeNode implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private final FileTreeNode parent;
	private final File file;
	private final Set<FileTreeNode> children = new LinkedHashSet<> ();
	
	private Track track;
	private Album album;
	private Artist artist;
	
	FileTreeNode ( Path path, FileTreeNode parent ) {
		this.file = path.toFile();
		this.parent = parent;
	}
	
	FileTreeNode(Path filePath, FileTreeNode parent, Track track) {
		this ( filePath, parent );
		this.track = track;
	}

	FileTreeNode getParent() {
		return parent;
	}
	
	Path getPath() {
		return file.toPath();
	}
	
	void addChild ( FileTreeNode child ) {
		children.add( child );
	}
	
	Set<FileTreeNode> getChildren() {
		return children;
	}
	
	Track getTrack() {
		return track;
	}

	void setAlbum( Album album ) {
		this.album = album;
	}
	
	Album getAlbum() {
		return album;
	}

	void setArtist( Artist artist ) {
		this.artist = artist;
	}
	
	Artist getArtist() {
		return artist;
	}
}
