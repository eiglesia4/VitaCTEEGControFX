#!/usr/bin/env bash
# Descarga VLC portable para Windows de 64 bits y prepara la carpeta vlc/ que se despliega
# junto a eegcontrol-x.jar (EEGFX-38). Con ella los vídeos funcionan sin VLC instalado.
#
# Uso:   scripts/fetch-vlc-win64.sh [--completo] [carpeta_destino]   (por defecto: ./vlc)
#
# Por defecto solo se dejan los plugins que la aplicación usa para reproducir MP4
# (H.264 + AAC) desde disco: unos 50 ficheros y 25 MB en lugar de ~370 y 143 MB.
# --completo deja todos los plugins, por si en alguna máquina faltara alguno.
#
# Se ejecuta en la máquina de desarrollo (macOS o Linux: bash, curl, shasum o sha256sum,
# unzip). La carpeta resultante se copia tal cual al ordenador del laboratorio, al lado del
# JAR, de default/ y de estudios/. Después, en Windows, hay que ejecutar UNA VEZ
# vlc\generar-cache.bat (ver vlc/LEEME.txt).
#
# La versión está fijada a propósito: es la que se ha probado. Para cambiarla, actualizar
# VLC_VERSION y VLC_SHA256 con el valor publicado en
# https://download.videolan.org/pub/videolan/vlc/<versión>/win64/vlc-<versión>-win64.zip.sha256
set -euo pipefail

VLC_VERSION="3.0.24"
VLC_SHA256="fcf30850371ad10c9373cc4f0f4501e7dee49e3e9ae9f20c72fb2661a1ca6323"
VLC_ZIP="vlc-${VLC_VERSION}-win64.zip"
VLC_URL="https://get.videolan.org/vlc/${VLC_VERSION}/win64/${VLC_ZIP}"

COMPLETO=false
if [[ "${1:-}" == "--completo" ]]; then
	COMPLETO=true
	shift
fi
DEST="${1:-vlc}"

# Plugins que necesita la aplicación, como <categoría>/<módulo>. Se obtuvieron registrando
# los módulos que carga VLC al reproducir, como lo hace la aplicación (salida vmem hacia el
# ImageView de JavaFX), los vídeos reales del laboratorio (H.264 + AAC en MP4), y se
# comprobó en macOS que con solo estos se reproducen enteros con imagen y sonido.
# Lo propio de Windows (salidas de audio) no se pudo probar allí: si un vídeo no suena o no
# se ve, volver a generar la carpeta con --completo.
KEEP_PLUGINS=(
	access/filesystem
	stream_filter/cache_read stream_filter/record
	demux/mp4
	packetizer/packetizer_h264 packetizer/packetizer_mpeg4audio
	packetizer/packetizer_mpeg4video packetizer/packetizer_copy
	codec/avcodec codec/faad
	audio_output/mmdevice audio_output/wasapi audio_output/directsound
	audio_output/waveout audio_output/adummy
	audio_filter/audio_format audio_filter/samplerate audio_filter/scaletempo
	audio_filter/simple_channel_mixer audio_filter/trivial_channel_mixer
	audio_filter/ugly_resampler audio_filter/speex_resampler audio_filter/remap
	audio_mixer/float_mixer audio_mixer/integer_mixer
	video_chroma/*
	video_output/vmem video_output/vdummy
	text_renderer/tdummy
	logger/console_logger logger/file_logger
	keystore/memory_keystore keystore/file_keystore
)

sha256_of() {
	if command -v shasum >/dev/null 2>&1; then
		shasum -a 256 "$1" | awk '{print $1}'
	else
		sha256sum "$1" | awk '{print $1}'
	fi
}

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "Descargando ${VLC_URL}"
curl -fsSL --retry 3 -o "${WORK}/${VLC_ZIP}" "${VLC_URL}"

ACTUAL="$(sha256_of "${WORK}/${VLC_ZIP}")"
if [[ "${ACTUAL}" != "${VLC_SHA256}" ]]; then
	echo "ERROR: el SHA-256 no coincide." >&2
	echo "  esperado: ${VLC_SHA256}" >&2
	echo "  obtenido: ${ACTUAL}" >&2
	exit 1
fi
echo "SHA-256 verificado"

# Solo lo que necesita libvlc, más las licencias y el generador de la caché de plugins.
# unzip conserva las fechas de los ficheros, que es lo que mantiene válida esa caché.
unzip -q "${WORK}/${VLC_ZIP}" \
	"vlc-${VLC_VERSION}/libvlc.dll" \
	"vlc-${VLC_VERSION}/libvlccore.dll" \
	"vlc-${VLC_VERSION}/plugins/*" \
	"vlc-${VLC_VERSION}/vlc-cache-gen.exe" \
	"vlc-${VLC_VERSION}/COPYING.txt" \
	"vlc-${VLC_VERSION}/AUTHORS.txt" \
	"vlc-${VLC_VERSION}/README.txt" \
	-d "${WORK}/extract"

STAGE="${WORK}/extract/vlc-${VLC_VERSION}"

if [[ "${COMPLETO}" == false ]]; then
	TRIMMED="${WORK}/plugins-trimmed"
	for entry in "${KEEP_PLUGINS[@]}"; do
		category="${entry%%/*}"
		module="${entry#*/}"
		mkdir -p "${TRIMMED}/${category}"
		if [[ "${module}" == "*" ]]; then
			cp -p "${STAGE}/plugins/${category}/"*.dll "${TRIMMED}/${category}/"
		else
			src="${STAGE}/plugins/${category}/lib${module}_plugin.dll"
			if [[ ! -f "${src}" ]]; then
				echo "ERROR: no existe el plugin ${entry} en VLC ${VLC_VERSION}" >&2
				exit 1
			fi
			cp -p "${src}" "${TRIMMED}/${category}/"
		fi
	done
	rm -rf "${STAGE}/plugins"
	mv "${TRIMMED}" "${STAGE}/plugins"
	echo "Plugins reducidos a $(find "${STAGE}/plugins" -name '*.dll' | wc -l | tr -d ' ') (usar --completo para todos)"
fi

cat > "${STAGE}/generar-cache.bat" <<'BAT'
@echo off
rem Genera plugins\plugins.dat, la cache de plugins de VLC. Sin ella VLC revisa los ~400
rem plugins cada vez que arranca la aplicacion. Ejecutar UNA VEZ tras copiar esta carpeta.
cd /d "%~dp0"
vlc-cache-gen.exe plugins
if exist plugins\plugins.dat (echo Cache generada: plugins\plugins.dat) else (echo ERROR: no se ha generado la cache)
pause
BAT
# cmd.exe espera saltos de línea CRLF (perl, porque el sed de macOS no entiende \r)
perl -pi -e 's/\n/\r\n/' "${STAGE}/generar-cache.bat"

cat > "${STAGE}/LEEME.txt" <<TXT
VLC ${VLC_VERSION} portable para Windows de 64 bits (EEGFX-38)
==========================================================

Esta carpeta permite reproducir vídeos sin tener VLC instalado. La aplicación la busca
en vlc\\ junto a eegcontrol-x.jar antes que el VLC instalado en el sistema.

Instalación en el ordenador del laboratorio:
  1. Copiar esta carpeta "vlc" junto al JAR, al lado de default\\ y estudios\\.
  2. Ejecutar UNA VEZ generar-cache.bat (doble clic). Crea plugins\\plugins.dat; sin ese
     fichero VLC revisa todos los plugins en cada arranque.
  3. Arrancar la aplicación. En EEGControl.log debe aparecer
     "VLC encontrado en ...\\vlc" y "VLC ${VLC_VERSION}".

Solo lleva los plugins necesarios para reproducir MP4 (H.264 + AAC). Si algún vídeo no
se viera o no sonara, generar la carpeta de nuevo con "fetch-vlc-win64.sh --completo".

Si se borra esta carpeta, la aplicación usa el VLC instalado. Si tampoco lo hay, arranca
igualmente, pero los protocolos con vídeo no se podrán ejecutar.

Origen: ${VLC_URL}
SHA-256 del ZIP: ${VLC_SHA256}

VLC es software libre de VideoLAN. libvlc y libvlccore se distribuyen bajo la licencia
LGPL 2.1 o posterior; algunos plugins, bajo GPL 2 o posterior. Ver COPYING.txt y
AUTHORS.txt. Código fuente: https://www.videolan.org/vlc/download-sources.html
TXT

if [[ -e "${DEST}" ]]; then
	echo "Sustituyendo ${DEST} existente"
	rm -rf "${DEST}"
fi
mkdir -p "$(dirname "${DEST}")"
mv "${STAGE}" "${DEST}"

echo "Listo: ${DEST} ($(du -sh "${DEST}" | awk '{print $1}'))"
echo "Copiar la carpeta junto al JAR y, en Windows, ejecutar una vez ${DEST}\\generar-cache.bat"
