package com.vitact.eegcontrol.bean;

import com.vitact.eegcontrol.type.EventEnum;
import javafx.scene.image.Image;

public class EventBean {
	EventEnum tipo;
	int length;
	String file;
	Image img;
	String mediaReference;

	public EventBean(EventEnum t, int l) {
		tipo = t;
		length = l;
		file = "";
	}

	public EventBean(EventEnum t, String f) {
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

	public EventEnum getTipo() {
		return tipo;
	}

	public void setTipo(EventEnum tipo) {
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
