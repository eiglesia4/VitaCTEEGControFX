package com.vitact.eegcontrol.bean;

import javafx.scene.image.Image;

public class EventBean {
	String tipo;
	int length;
	String file;
	Image img;
	String mediaReference;

	public EventBean(String t, int l) {
		tipo = t;
		length = l;
		file = "";
	}

	public EventBean(String t, String f) {
		tipo = t;
		length = -1;
		file = f;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(getTipo());
		sb.append(" ");
		sb.append(getLength());
		sb.append(" ");
		sb.append(getFile());
		return sb.toString();
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public Image getImg() {
		return img;
	}

	public void setImg(Image img) {
		this.img = img;
	}

	public String getMediaReference() {
		return mediaReference;
	}

	public void setMediaReference(String mediaReference) {
		this.mediaReference = mediaReference;
	}
}
