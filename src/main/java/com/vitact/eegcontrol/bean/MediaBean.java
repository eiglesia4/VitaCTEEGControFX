package com.vitact.eegcontrol.bean;

import com.vitact.eegcontrol.type.MediaTypeEnum;
import javafx.scene.image.Image;
import javafx.scene.media.*;

public class MediaBean {
	public static String SOUND_DEFAULT_IMAGE = "soundDefaultImage.png";

	MediaTypeEnum mediaType;
	Media sound;
	Media video;
	Image image;
	MediaPlayer mediaPlayer;
	MediaView mediaView;
	boolean videoInitialized = false;
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

	public MediaBean(Media video, MediaTypeEnum mediaType) {
		this.mediaType = mediaType;
		this.video = video;
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

	public Media getVideo() {
		return video;
	}

	public void setVideo(Media video) {
		this.video = video;
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public MediaPlayer getMediaPlayer() {
		return mediaPlayer;
	}

	public void setMediaPlayer(MediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
	}

	public MediaView getMediaView() {
		return mediaView;
	}

	public void setMediaView(MediaView mediaView) {
		this.mediaView = mediaView;
	}

	public int getLoadRetries() {
		return loadRetries;
	}

	public void incrementLoadRetries() {
		this.loadRetries++;
	}

	public boolean isVideoInitialized() {
		return videoInitialized;
	}

	public void setVideoInitialized(boolean videoInitialized) {
		this.videoInitialized = videoInitialized;
	}

}
