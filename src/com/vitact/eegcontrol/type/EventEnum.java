package com.vitact.eegcontrol.type;

public enum EventEnum {
	LANZAR("LANZAR", "Reproducir un vídeo"),
	MOSTRAR("MOSTRAR", "Mostrar Imagen"),
	SONAR("SONAR", "Reproducir un sonido");

	private String code;
	private String help;

	String getCode() {
		return this.code;
	}

	String getHelp() {
		return this.help;
	}

	EventEnum(String code, String help) {
		this.code = code;
		this.help = help;
	}

}
