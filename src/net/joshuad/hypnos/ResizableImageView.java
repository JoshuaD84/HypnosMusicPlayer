package net.joshuad.hypnos;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

class ResizableImageView extends ImageView {
	ResizableImageView ( Image image ) {
		super ( image );
		setPreserveRatio( true );
	}
	
	@Override
	public double minWidth(double height) {
		return 100;
	}

	@Override
	public double prefWidth(double height) {
		Image I = getImage();
		if (I == null) {
			return minWidth(height);
		}
		return I.getWidth();
	}

	@Override
	public double maxWidth(double height) {
		return 10000;
	}

	@Override
	public double minHeight(double width) {
		return 100;
	}

	@Override
	public double prefHeight(double width) {
		Image I = getImage();
		if (I == null) {
			return minHeight(width);
		}
		return I.getHeight();
	}

	@Override
	public double maxHeight(double width) {
		return 10000;
	}

	@Override
	public boolean isResizable() {
		return true;
	}

	@Override
	public void resize(double width, double height) {
		setFitWidth(width);
		setFitHeight(height);
	}
}