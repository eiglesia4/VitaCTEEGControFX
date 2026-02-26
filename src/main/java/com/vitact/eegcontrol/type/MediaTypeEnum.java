package com.vitact.eegcontrol.type;

public enum MediaTypeEnum {
	SOUND("Sonido"),
	SOUND_IMAGE("Sonido con imagen"),
	IMAGE("Imagen"),
	VIDEO("Vídeo");

	String description;

	MediaTypeEnum(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
