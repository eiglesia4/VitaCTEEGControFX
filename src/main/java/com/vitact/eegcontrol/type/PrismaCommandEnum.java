package com.vitact.eegcontrol.type;

import java.util.Locale;

/**
 * Comandos que acepta el estimulador Prisma ELA (Controller_Board v1.0.X, firmware ELA_SW v2.2).
 * <p>
 * En el protocolo se escriben como {@code VIBRAR g_<comando>}. El prefijo {@code g_} los
 * distingue de los comandos del guante, que se envían tal cual. Por el puerto serie viaja
 * {@link #getCommand()} seguido de {@code \r\n}. El dispositivo no contesta: obedece todo
 * lo que recibe.
 * <p>
 * Qué salidas activa cada comando lo decide el firmware; aquí solo se documenta, según el
 * array {@code INTERACCIONES} de ELA_SW v2.2.
 */
public enum PrismaCommandEnum {
	BRAZOPIERNAMANO("brazopiernamano", "Brazo (J2), pierna (J3), mano (J5) y cuello (J7)"),
	BRAZOMANO("brazomano", "Brazo (J2) y mano (J5)"),
	PIERNA("pierna", "Pierna (J3)"),
	MANO("mano", "Mano (J5)"),
	CUELLO("cuello", "Cuello (J7)"),
	DIAFRAGMACUELLO("diafragmacuello", "Diafragma (J9) y cuello (J7)"),
	OLORDIAFRAGMA("olordiafragma", "Diafragma (J9) y olor (J8)"),
	// Pruebas de cableado: una salida cada vez
	T_J2("t_j2", "Prueba: solo brazo (J2)"),
	T_J3("t_j3", "Prueba: solo pierna (J3)"),
	T_J5("t_j5", "Prueba: solo mano (J5)"),
	T_J7("t_j7", "Prueba: solo cuello (J7)"),
	T_J8("t_j8", "Prueba: solo olor (J8)"),
	T_J9("t_j9", "Prueba: solo diafragma (J9, D9 directo)");

	/** Prefijo que marca, en el argumento de VIBRAR, un comando para el Prisma. */
	public static final String PROTOCOL_PREFIX = "g_";

	/** Terminador que espera el firmware tras cada comando. */
	public static final String TERMINATOR = "\r\n";

	private final String command;
	private final String help;

	PrismaCommandEnum(String command, String help) {
		this.command = command;
		this.help = help;
	}

	/** Cadena exacta que entiende el firmware, sin prefijo ni terminador. */
	public String getCommand() {
		return command;
	}

	/** Salidas que activa el comando, para ayuda/documentación del protocolo. */
	@SuppressWarnings("unused")
	public String getHelp() {
		return help;
	}

	/** Indica si el argumento de un VIBRAR va dirigido al Prisma, ignorando mayúsculas. */
	public static boolean isPrismaArgument(String argument) {
		return argument != null && argument.toLowerCase(Locale.ROOT).startsWith(PROTOCOL_PREFIX);
	}

	/**
	 * Busca el comando de un argumento {@code g_<comando>}, ignorando mayúsculas.
	 *
	 * @return el comando, o null si el argumento no lleva el prefijo o el comando no existe
	 */
	public static PrismaCommandEnum fromProtocolArgument(String argument) {
		if (!isPrismaArgument(argument))
			return null;
		String name = argument.substring(PROTOCOL_PREFIX.length()).toLowerCase(Locale.ROOT);
		for (PrismaCommandEnum c : values()) {
			if (c.command.equals(name))
				return c;
		}
		return null;
	}

	/** Lista de comandos válidos, ya con prefijo, para los mensajes de error. */
	public static String validArguments() {
		StringBuilder sb = new StringBuilder();
		for (PrismaCommandEnum c : values()) {
			if (!sb.isEmpty())
				sb.append(", ");
			sb.append(PROTOCOL_PREFIX).append(c.command);
		}
		return sb.toString();
	}
}
