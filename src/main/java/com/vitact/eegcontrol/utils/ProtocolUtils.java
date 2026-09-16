package com.vitact.eegcontrol.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProtocolUtils {

	/**
	 * Un tramo entrecomillado, o una secuencia sin espacios. El grupo 1 captura el
	 * contenido de las comillas sin ellas.
	 */
	private static final Pattern TOKEN = Pattern.compile("\"([^\"]*)\"|(\\S+)");

	/**
	 * Trocea una línea de protocolo en comando y argumentos, tratando un tramo entre
	 * comillas dobles como un único token aunque contenga espacios.
	 * <p>
	 * Sustituye al {@code split("\\s")} que se usaba antes, que troceaba la línea
	 * <em>antes</em> de quitar las comillas y por tanto partía en dos cualquier nombre de
	 * fichero con espacios: {@code MOSTRAR "MI IMAGEN.png"} daba
	 * {@code [MOSTRAR, "MI, IMAGEN.png"]} y el nombre quedaba en {@code MI}.
	 *
	 * @param line línea completa del fichero de protocolo
	 * @return los tokens, ya sin comillas; lista vacía si la línea no tiene contenido
	 */
	public static String[] tokenize(String line) {
		List<String> tokens = new ArrayList<>();
		if (line == null)
			return new String[0];
		Matcher m = TOKEN.matcher(line);
		while (m.find())
			tokens.add(m.group(1) != null ? m.group(1) : m.group(2));
		return tokens.toArray(new String[0]);
	}

	public static boolean trueFalseLine(String line) {
		String[] data = tokenize(line);
		String fileName = null;
		if (data.length > 1)
			fileName = data[1];

		if (fileName != null) {
			// OVERRIDES CONFIGURATION DEFINITION OF FULLSCREEN
			if (fileName.equalsIgnoreCase("SI") || fileName.equalsIgnoreCase("YES")
					|| fileName.equalsIgnoreCase("TRUE"))
				return true;
			else
				return false;
		} else {
			// IGNORE MALFORMED FULLSCREEN
			return false;
		}
	}
}
