package com.vitact.eegcontrol;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryDirectoryProvider;
import uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryProviderPriority;

/**
 * Hace que VLCJ busque primero un VLC portable en la carpeta {@code vlc/} junto a la
 * aplicación, antes que el VLC instalado en el sistema. Así los vídeos funcionan en una
 * máquina sin VLC instalado, y siempre con la versión probada (ver EEGFX-38).
 * <p>
 * Se registra como servicio en
 * {@code META-INF/services/uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryDirectoryProvider}.
 * VLCJ consulta los proveedores de mayor a menor prioridad, y solo acepta un directorio si
 * contiene las librerías del sistema operativo en curso ({@code libvlc.dll} y
 * {@code libvlccore.dll} en Windows), así que una carpeta {@code vlc/} incompleta o de otro
 * sistema se ignora y la búsqueda sigue con el VLC instalado.
 * <p>
 * Distribuciones que se aceptan:
 * <ul>
 *   <li>Windows: {@code vlc/libvlc.dll}, {@code vlc/libvlccore.dll} y {@code vlc/plugins/}.</li>
 *   <li>macOS: {@code vlc/lib/libvlc.dylib} y {@code vlc/plugins/}, la misma forma que dentro
 *   de {@code VLC.app/Contents/MacOS}. VLCJ busca los plugins en {@code <lib>/../plugins}.</li>
 * </ul>
 */
public class VlcDirectoryProvider implements DiscoveryDirectoryProvider {

	/** Carpeta, relativa a la de la aplicación, donde va el VLC portable. */
	public static final String PORTABLE_VLC_DIR = "vlc";

	/**
	 * Por encima de la carpeta del JAR ({@link DiscoveryProviderPriority#APP_DIR}) y del VLC
	 * instalado ({@link DiscoveryProviderPriority#INSTALL_DIR}). Por debajo del fichero de
	 * configuración de VLCJ, que es una elección explícita de quien lo escribe.
	 */
	private static final int PRIORITY = 0;

	@Override
	public int priority() {
		return PRIORITY;
	}

	@Override
	public String[] directories() {
		Set<String> dirs = new LinkedHashSet<>();
		// La aplicación se ejecuta desde la carpeta que contiene default/ y estudios/, que es
		// donde se despliega también vlc/. La carpeta del JAR suele ser la misma, pero se
		// mira también por si se lanza el JAR desde otro sitio.
		addCandidates(dirs, new File(System.getProperty("user.dir")));
		addCandidates(dirs, jarDirectory());
		return dirs.toArray(new String[0]);
	}

	@Override
	public boolean supported() {
		return true;
	}

	private static void addCandidates(Set<String> dirs, File base) {
		if (base == null)
			return;
		File vlc = new File(base, PORTABLE_VLC_DIR);
		if (!vlc.isDirectory())
			return;
		dirs.add(vlc.getAbsolutePath());
		File lib = new File(vlc, "lib");
		if (lib.isDirectory())
			dirs.add(lib.getAbsolutePath());
	}

	private static File jarDirectory() {
		try {
			var source = VlcDirectoryProvider.class.getProtectionDomain().getCodeSource();
			if (source == null || source.getLocation() == null)
				return null;
			File location = new File(source.getLocation().toURI());
			return location.isDirectory() ? location : location.getParentFile();
		} catch (Exception e) {
			return null;
		}
	}
}
