package com.vitact.eegcontrol.type;

public enum EventEnum {
	LANZAR("LANZAR", "Reproducir un vídeo"),
	MOSTRAR("MOSTRAR", "Mostrar imagen"),
	SONAR("SONAR", "Reproducir un sonido"),
	MULTI("MULTI", "Multi-estimulación (vibración, olor, sabor e imágen"),
	MARCAR("MARCAR", "Envia una marca al EEG"),
	ESPERAR("ESPERAR", "Esperar un tiempo determinado"),
	VIBRAR("VIBRAR", "Vibrar, uso del guante vibrador"),
	TERMINAR("TERMINAR", "Termina el protocolo"),
	TACTIL("TACTIL", "Estimulación táctil"),
	INICIAR("INICIAR", "Inicia el protocolo"),
	TARGET("TARGET", "Estimulación táctil??????"),
	FAIL("FAIL", "??????"),
	CLICKSTOP("CLICKSTOP", "Click on mouse to stop protocol"),
	SPACESTOP("SPACESTOP", "Press space to stop protocol"),
	ESTIM_OLD("ESTIM_OLD", "Uso del estimulador de KGS"),
	KGS("ESTIM_OLD", "Uso del estimulador de KGS. Alias de ESTIM_OLD");

	private String code;
	private String help;

	public String getCode() {
		return this.code;
	}

	public String getHelp() {
		return this.help;
	}

	EventEnum(String code, String help) {
		this.code = code;
		this.help = help;
	}

}
