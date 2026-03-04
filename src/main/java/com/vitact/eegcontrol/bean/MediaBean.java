package com.vitact.eegcontrol.bean;

import com.vitact.eegcontrol.type.MediaTypeEnum;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class MediaBean {
	public static String SOUND_DEFAULT_IMAGE = "soundDefaultImage.png";

	MediaTypeEnum mediaType;
	Media sound;
	Image image;

	// VLCJ video fields
	String videoPath;
	EmbeddedMediaPlayer vlcPlayer;
	ImageView videoImageView;
	boolean vlcPlayerReady = false;
	int loadRetries = 0;

	public MediaBean(Media sound, Image image, MediaTypeEnum mediaType) {
		this.mediaType = mediaType;
		this.sound = sound;
		this.image = image;
	}

	public MediaBean(Image image, MediaTypeEnum mediaType) {
		this.mediaType = mediaType;
		this.image = image;
	}

	public MediaBean(String videoPath, MediaTypeEnum mediaType) {
		this.mediaType = mediaType;
		this.videoPath = videoPath;
	}

	public MediaTypeEnum getMediaType() {
		return mediaType;
	}

	public void setMediaType(MediaTypeEnum mediaType) {
		this.mediaType = mediaType;
	}

	public Media getSound() {
		return sound;
	}

	public void setSound(Media sound) {
		this.sound = sound;
	}

	public String getVideoPath() {
		return videoPath;
	}

	public void setVideoPath(String videoPath) {
		this.videoPath = videoPath;
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public EmbeddedMediaPlayer getVlcPlayer() {
		return vlcPlayer;
	}

	public void setVlcPlayer(EmbeddedMediaPlayer vlcPlayer) {
		this.vlcPlayer = vlcPlayer;
	}

	public ImageView getVideoImageView() {
		return videoImageView;
	}

	public void setVideoImageView(ImageView videoImageView) {
		this.videoImageView = videoImageView;
	}

	public int getLoadRetries() {
		return loadRetries;
	}

	public void incrementLoadRetries() {
		this.loadRetries++;
	}

	public boolean isVlcPlayerReady() {
		return vlcPlayerReady;
	}

	public void setVlcPlayerReady(boolean vlcPlayerReady) {
		this.vlcPlayerReady = vlcPlayerReady;
	}

}
