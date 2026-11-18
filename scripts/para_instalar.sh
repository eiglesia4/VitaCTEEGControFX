#!/usr/bin/env bash
# Prepara para_instalar/ con lo que hay que llevar al PC del laboratorio (EEGFX-39).
#
# Uso:   scripts/para_instalar.sh [--sin-compilar]
#
# para_instalar/ reproduce la estructura de la RAÍZ de la instalación del laboratorio (la
# carpeta de la que cuelgan default\, estudios\ y los logs): su contenido se copia encima de
# esa raíz. Ver para_instalar/LEEME_INSTALACION.txt, que se genera con los pasos.
#
# Qué se lleva: solo el ejecutable y sus librerías.
#   - el fat JAR, con el nombre que ya usa el laboratorio
#   - vlc\  (VLC portable, EEGFX-38), reutilizando ./vlc si ya está descargado
#   - EEGControl.bat, un lanzador opcional que comprueba el Java instalado
# Qué NO se lleva, a propósito: nada de default/ (configuración, protocolos, multimedia) ni de
# estudios/. Ese contenido es del laboratorio y se gestiona aparte.
set -euo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${REPO}/para_instalar"
COMPILAR=true
[[ "${1:-}" == "--sin-compilar" ]] && COMPILAR=false

cd "${REPO}"

# --- 1. JAR -------------------------------------------------------------------------------
if [[ "${COMPILAR}" == true ]]; then
	if ! "${JAVA_HOME:-/nonexistent}/bin/java" -version 2>&1 | grep -qE 'version "2[5-9]|version "[3-9][0-9]'; then
		JAVA_HOME="$(/usr/libexec/java_home -v 25 2>/dev/null || true)"
		[[ -n "${JAVA_HOME}" ]] || { echo "ERROR: hace falta un JDK 25 para compilar" >&2; exit 1; }
		export JAVA_HOME
	fi
	echo "Compilando con ${JAVA_HOME}"
	mvn -q clean package
fi
JAR="$(ls target/eegcontrol-*.jar 2>/dev/null | grep -v '/original-' | head -1 || true)"
[[ -n "${JAR}" ]] || { echo "ERROR: no hay JAR en target/ (¿falta compilar?)" >&2; exit 1; }
JAR_NAME="$(basename "${JAR}")"

# --- 2. VLC portable -----------------------------------------------------------------------
# ./vlc hace de caché: solo se descarga si no está. Para forzar la descarga, borrar ./vlc.
if [[ ! -f vlc/libvlc.dll ]]; then
	echo "No hay VLC de Windows en ./vlc: descargando"
	"${REPO}/scripts/fetch-vlc-win64.sh" vlc
fi

# --- 3. Montaje ---------------------------------------------------------------------------
rm -rf "${OUT}"
mkdir -p "${OUT}"

cp -p "${JAR}" "${OUT}/"
# -p: conservar las fechas mantiene válida la caché de plugins de VLC
cp -Rp vlc "${OUT}/vlc"

# --- 4. Lanzador e instrucciones -------------------------------------------------------------
cat > "${OUT}/EEGControl.bat" <<BAT
@echo off
rem Lanzador de EEGControl. Debe estar en la raiz de la instalacion, junto a default\\,
rem estudios\\ y vlc\\: la aplicacion busca esas carpetas en la carpeta desde la que arranca.
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo No se encuentra Java. EEGControl necesita Java 25 o superior.
  pause
  exit /b 1
)
set JMAJ=0
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set JVER=%%~v
for /f "delims=." %%m in ("%JVER%") do set JMAJ=%%m
if %JMAJ% LSS 25 (
  echo El Java instalado es la version %JVER%. EEGControl necesita Java 25 o superior.
  pause
  exit /b 1
)
start "" javaw -jar "${JAR_NAME}"
BAT

COMMIT="$(git rev-parse --short HEAD)"
DIRTY="$(git diff --quiet HEAD -- src pom.xml || echo ' (con cambios sin commit)')"
cat > "${OUT}/LEEME_INSTALACION.txt" <<TXT
Instalación de EEGControl en el PC del laboratorio
==================================================

Generado el $(date '+%d/%m/%Y %H:%M') desde el commit ${COMMIT}${DIRTY}.
JAR: ${JAR_NAME}   SHA-256: $(shasum -a 256 "${JAR}" | awk '{print $1}')

Esta carpeta tiene la misma estructura que la raíz de la instalación (la carpeta de la que
cuelgan default\\ y estudios\\). Su contenido se copia ENCIMA de esa raíz.

Antes de copiar
  1. Hacer copia de seguridad de estudios\\0000\\. Es la carpeta que usa "Ejecutar
     Protocolo" sin estudio, y se sobrescribe en cada ejecución.
  2. Si EEGControl está abierto, cerrarlo.

Copiar
  3. Copiar TODO el contenido de esta carpeta encima de la raíz de la instalación, aceptando
     reemplazar. Solo se sustituyen el JAR y vlc\\; no se toca nada de default\\ ni de
     estudios\\.
     Usar el Explorador de Windows: conserva las fechas de los ficheros, y VLC las necesita.

Después de copiar (una sola vez)
  4. Doble clic en vlc\\generar-cache.bat. Crea la caché de plugins de VLC; sin ella, VLC
     tarda más en arrancar cada vez.

Arrancar
  5. Con el acceso directo de siempre, que debe apuntar a ${JAR_NAME} y arrancar en la raíz
     de la instalación. Alternativa: EEGControl.bat, que además comprueba que el Java
     instalado es 25 o superior.

Comprobar
  6. En EEGControl.log deben aparecer "VLC encontrado en ...\\vlc" y la versión de VLC.
  7. Ejecutar un protocolo con vídeo y comprobar que se ve Y SE OYE.

Si algo falla
  - Sin vídeo, o vídeo sin sonido: volver a generar vlc\\ con "fetch-vlc-win64.sh --completo".
  - Ver vlc\\LEEME.txt.
TXT

# Los .txt y .bat para Windows, con saltos de línea CRLF (perl: el sed de macOS no entiende \r)
perl -pi -e 's/\r?\n/\r\n/' "${OUT}/EEGControl.bat" "${OUT}/LEEME_INSTALACION.txt"

echo
echo "Listo: ${OUT} ($(du -sh "${OUT}" | awk '{print $1}'))"
(cd "${OUT}" && find . -maxdepth 2 -not -path './vlc/*' | sort | sed 's/^\.\///' | sed '/^\.$/d;s/^/  /')
