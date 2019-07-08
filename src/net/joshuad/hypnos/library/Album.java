package net.joshuad.hypnos.library;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

public class Album implements Serializable, AlbumInfoSource {
	private static transient final Logger LOGGER = Logger.getLogger(Album.class.getName());

	private static final long serialVersionUID = 2L;
	private File directory;
	long creationTimeMS = 0;

	private transient ObservableList<Track> tracks;
	private transient StringProperty albumArtistProperty;
	private transient StringProperty yearProperty;
	private transient StringProperty albumTitleProperty;
	private transient StringProperty fullAlbumTitleProperty;
	private transient StringProperty dateAddedStringProperty;
	private transient IntegerProperty discNumberProperty;
	private transient IntegerProperty discCountProperty;
	private transient StringProperty releaseTypeProperty;
	private transient StringProperty discSubtitleProperty;

	private void initializeTransientFields() {
		tracks = FXCollections.observableArrayList();
		this.tracks.addListener(new InvalidationListener() {
			@Override
			public void invalidated(Observable arg0) {
				updateData();
			}
		});
		albumArtistProperty = new SimpleStringProperty("");
		yearProperty = new SimpleStringProperty("");
		albumTitleProperty = new SimpleStringProperty("");
		fullAlbumTitleProperty = new SimpleStringProperty("");
		dateAddedStringProperty = new SimpleStringProperty("");
		discNumberProperty = new SimpleIntegerProperty();
		discCountProperty = new SimpleIntegerProperty();
		releaseTypeProperty = new SimpleStringProperty("");
		discSubtitleProperty = new SimpleStringProperty("");
	}

	public Album(Path albumDirectory, List<Track> tracks) {
		initializeTransientFields();
		this.directory = albumDirectory.toFile();
		try {
			creationTimeMS = Files.readAttributes(directory.toPath(), BasicFileAttributes.class).creationTime().toMillis();
		} catch (IOException e) {
			LOGGER.info("Unable to determine file creation time for album, assuming it is very old." + directory.toString());
		}
		setTracks(tracks);
	}

	public void setTracks(List<Track> tracks) {
		this.tracks.setAll(tracks);
		for (Track track : tracks) {
			track.setAlbum(this);
		}
	}

	private void updateData() {
		albumArtistProperty.set(tracks.get(0).getAlbumArtist());
		if (tracks.get(0).getYear().length() > 4) {
			yearProperty.set(tracks.get(0).getYear().substring(0, 4));
		} else {
			yearProperty.set(tracks.get(0).getYear());
		}
		albumTitleProperty.set(tracks.get(0).getAlbumTitle());
		fullAlbumTitleProperty.set(tracks.get(0).getFullAlbumTitle());
		discNumberProperty.set(tracks.get(0).getDiscNumber());
		discCountProperty.set(tracks.get(0).getDiscCount());
		releaseTypeProperty.set(tracks.get(0).getReleaseType());
		discSubtitleProperty.set(tracks.get(0).getDiscSubtitle());
	}

	public StringProperty getAlbumArtistProperty() {
		return albumArtistProperty;
	}

	public StringProperty getYearProperty() {
		return yearProperty;
	}

	public StringProperty getAlbumTitleProperty() {
		return albumTitleProperty;
	}

	public StringProperty getFullAlbumTitleProperty() {
		return fullAlbumTitleProperty;
	}

	public StringProperty getDateAddedStringProperty() {
		return dateAddedStringProperty;
	}

	public IntegerProperty getDiscNumberProperty() {
		return discNumberProperty;
	}

	public IntegerProperty getDiscCountProperty() {
		return discCountProperty;
	}

	public StringProperty getReleaseTypeProperty() {
		return releaseTypeProperty;
	}

	public StringProperty getDiscSubtitleProperty() {
		return discSubtitleProperty;
	}

	public String getAlbumArtist() {
		return albumArtistProperty.get();
	}

	public String getYear() {
		return yearProperty.get();
	}

	public String getAlbumTitle() {
		return albumTitleProperty.get();
	}

	public String getFullAlbumTitle() {
		return fullAlbumTitleProperty.get();
	}

	public Integer getDiscNumber() {
		return discNumberProperty.get();
	}

	public Integer getDiscCount() {
		return discCountProperty.get();
	}

	public String getReleaseType() {
		return releaseTypeProperty.get();
	}

	public String getDiscSubtitle() {
		return discSubtitleProperty.get();
	}

	public Path getPath() {
		return directory.toPath();
	}

	public ObservableList<Track> getTracks() {
		return tracks;
	}

	@Override
	public boolean equals(Object e) {
		if (!(e instanceof Album)) {
			return false;
		}
		Album compareTo = (Album) e;
		return compareTo.getPath().toAbsolutePath().equals(this.getPath().toAbsolutePath());
	}

	public Image getAlbumCoverImage() {
		for (Track track : tracks) {
			if (track.getAlbumCoverImage() != null) {
				return track.getAlbumCoverImage();
			}
		}
		return null;
	}

	public Image getAlbumArtistImage() {
		for (Track track : tracks) {
			if (track.getAlbumCoverImage() != null) {
				return track.getArtistImage();
			}
		}
		return null;
	}
	
	private void writeObject ( ObjectOutputStream out ) throws IOException {
		out.defaultWriteObject();
		out.writeObject(new ArrayList<Track>(tracks));
	}
	
	private void readObject ( ObjectInputStream in ) throws IOException, ClassNotFoundException {
		initializeTransientFields();
		in.defaultReadObject();
		tracks.addAll((ArrayList<Track>)in.readObject());
		updateData();
	}
}
