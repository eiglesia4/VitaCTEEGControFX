# PROTOCOLO

NOTA: Los protocolos son una extensión de los protocolos de ATI Cognistin

Un fichero de protocolo es un fichero de texto, salvado con extensión lst por compatibilidad con ATI (aunque puede tener extensión txt). Este fichero tiene dos secciones:

1. Sección de configuración en la que se establecen las opciones del protocolo. Por convenio se coloca al principio del fichero

2. Sección de eventos del protocolo. Se inicia con la palabra clave de inicio de protocolo INICIAR y finaliza con el fichero o con la palabra clave de finalización de protocolo TERMINAR

Se pueden introducir comentarios en el fichero de protocolos iniciando la línea con ; o con #. No se admiten comentarios a mitad de línea.

## Configuración

La sección de configuración suele ir colocada al inicio del protocolo e indica las características a habilitar en el protocolo. Todas las configuraciones tienen a FALSE como valor por defecto. Para activar cada propiedad de configuración es necesario poner como TRUE su valor.

| Propiedad | Objetivo |
|-----------|----------|
| USE_EEG | Indica que se usará ATI Vertex para recoger como EEG el resultado del experimento. Provocará la aparición de un diálogo para seleccionar el puerto al que está conectado el amplificador antes de iniciar el protocolo. |
| USE_MATRIX | Indica que se usará la Matriz de VitaCT para estimular táctilmente al sujeto. Provocará la aparición de un diálogo para seleccionar el puerto al que está conectado la matriz antes de iniciar el protocolo. |
| KGS_VIBRATE | Indica que se usará la Matriz de KGS para estimular táctilmente al sujeto. Su uso es meramente documental. |
| USE_GLOVE | Indica que se usará el guante de Álvaro para estimular táctilmente al sujeto. Provocará la aparición de un diálogo para seleccionar el puerto al que está conectado el guante antes de iniciar el protocolo. |
| USE_TARGET | Reservado para uso futuro |
| USE_SPACEKEY | Indica que no se iniciará el protocolo hasta que no se toque la tecla espaciadora |
| FULLSCREEN | Hace que la ventana en la que se muestra la evolución del protocolo se muestre a pantalla completa. |
| CENTER_MOUSE | Centra el ratón en la pantalla. WARNING: Al usarse junto a FULLSCREEN, en Windows no se consigue el efecto esperado de centrar exactamente el ratón. Se arreglará en futuras versiones. |

## Eventos

Los eventos se ejecutan en el orden en el que se escriben en el fichero de protocolo.

| Acción | Objetivo | Variables |
|--------|----------|-----------|
| INICIAR | Marca el inicio del protocolo. | 1.- Imagen a mostrar (opcional) |
| TERMINAR | Finaliza el protocolo (opcional) | No tiene |
| CLICKSTOP | Suspende la ejecución del protocolo hasta hacer click de ratón. No detiene el temporizador. | No tiene |
| SPACESTOP | Suspende la ejecución del protocolo hasta pulsar la tecla espacio. No detiene el temporizador. | No tiene |
| ESPERAR | Espera X milisegundos para ejecutar el siguiente comando del protocolo. | 1.- Milisegs. espera (obligatorio) |
| ESPERAR_VIDEO | Espera a que termine el vídeo actualmente en reproducción. Si el vídeo ya ha terminado cuando se alcanza este comando, continúa inmediatamente. Útil cuando la duración del vídeo es mayor que la suma de los ESPERAR entre el LANZAR y este comando. | No tiene |
| PARAR_VIDEO | Detiene inmediatamente el vídeo en reproducción y libera sus recursos. Si no hay ningún vídeo en reproducción, no hace nada. Útil cuando se quiere cortar un vídeo en un momento determinado del protocolo. | No tiene |
| ESPERAR_AUDIO | Espera a que termine el audio actualmente en reproducción. Si el audio ya ha terminado cuando se alcanza este comando, continúa inmediatamente. Útil cuando la duración del audio es mayor que la suma de los ESPERAR entre el SONAR y este comando. | No tiene |
| PARAR_AUDIO | Detiene inmediatamente el audio en reproducción. Si no hay ningún audio en reproducción, no hace nada. Útil cuando se quiere cortar un audio en un momento determinado del protocolo. | No tiene |
| ESTIM_OLD | Muestra en pantalla la imagen definida tras una transformación de tamaño 28x28. La imagen debe ser en blanco y negro. No se detectan bordes, se muestra entera. | 1.- Imagen a mostrar (obligatorio) |
| KGS | Muestra en pantalla la imagen definida tras una transformación de tamaño 48x32. La imagen debe ser en blanco y negro. No se detectan bordes, se muestra entera. | 1.- Imagen a mostrar (obligatorio) |
| LANZAR | Muestra un vídeo | 1.- Video a mostrar (obligatorio) |
| MOSTRAR | Muestra una imagen | 1.- Imagen a mostrar (obligatorio) |
| SONAR | Reproduce un sonido con una imagen de fondo. Si no se especifica imagen se reproduce el fichero soundDefaultImage.png. La reproducción es no bloqueante: el protocolo continúa inmediatamente. Si hay un audio sonando, se detiene antes de reproducir el nuevo. Para esperar a que termine, usar ESPERAR_AUDIO; para detenerlo, usar PARAR_AUDIO. | 1.- Sonido reproducir (obligatorio), 2.- Imagen mostrar (opcional) |
| MARCAR | Envía una marca al EEG. Se reservan las marcas: 6 => Tecla espacio para estímulo correcto, 7 => Tecla espacio para estímulo incorrecto, 8 => Botón Izqdo / Tecla 1 / Tecla Z, 9 => Botón Dcho / Tecla 2 / Tecla M | 1.- Número de marca (obligatorio 1 a 9) |
| VIBRAR | Envía un estímulo táctil al estimulador de VitaCT durante 3 segundos. | 1.- Imagen a mostrar (obligatorio) |
| TACTIL | Envía un estímulo táctil al guante de Álvaro. El estímulo es una "imagen" que se envía como bytes. | 1.- Imagen a enviar (obligatorio) |
| TARGET | RESERVADO. Para marcar en un protocolo que el multimedia a mostrar es el correcto | No tiene |
| FAIL | RESERVADO. Para marcar en un protocolo que el multimedia a mostrar es el incorrecto | No tiene |

## Nombres de fichero con espacios

Los nombres de fichero deben ir **entre comillas dobles** siempre que contengan espacios:

```
MOSTRAR "MI IMAGEN.png"
SONAR "mi sonido.wav" "imagen de fondo.bmp"
LANZAR "video de prueba.mp4"
```

Sin comillas, el nombre se corta en el primer espacio y el fichero no se encuentra. Para nombres sin espacios las comillas son opcionales, aunque se recomiendan por coherencia.

Afecta a `INICIAR`, `MOSTRAR`, `SONAR`, `LANZAR`, `MULTI`, `ESTIM_OLD` y `KGS`.

Cuidado con `MULTI`: su segundo argumento son milisegundos de espera, **no** una imagen de fondo. La imagen de fondo como segundo argumento solo la admite `SONAR`.

```
MULTI "imagen.bmp" 300      ; 300 ms entre el estímulo y la imagen
SONAR "sonido.wav" "fondo.bmp"
```

## Formatos de audio soportados

La reproducción de audio (comando `SONAR`) utiliza el motor de medios de JavaFX (`javafx.scene.media.MediaPlayer`). Esto determina qué formatos de fichero pueden usarse en los protocolos.

### Formatos soportados

| Formato | Extensión | Notas |
|---------|-----------|-------|
| WAV | `.wav` | **Solo PCM sin comprimir** (8 / 16 / 24 / 32 bits lineal). No se admiten variantes comprimidas como µ-law, A-law o ADPCM. |
| MP3 | `.mp3` | MPEG-1 Layer 3. |
| AIFF | `.aif`, `.aiff` | Solo PCM sin comprimir. |

### Formatos NO soportados

- AAC suelto (`.aac`, `.m4a`)
- OGG / Vorbis / Opus
- FLAC
- WMA
- WAV comprimido (µ-law, A-law, ADPCM, etc.)

Si necesita usar uno de estos formatos, conviértalo previamente a WAV PCM o MP3 con una herramienta externa (por ejemplo, Audacity).

### Extensión por defecto

Si en el protocolo se escribe `SONAR misonido` sin extensión, la aplicación añade automáticamente `.wav`. Para usar otro formato, especifique la extensión completa: `SONAR misonido.mp3`.

### Recomendación para potenciales evocados

Para experimentos donde la precisión del instante de inicio del estímulo es crítica (por ejemplo, paradigmas P300 oddball), se recomienda **WAV PCM**: no introduce latencia de decodificación variable y es el formato ya usado en los protocolos existentes. MP3 introduce un pequeño retardo variable de decodificación que puede afectar al sincronismo si se está midiendo en decenas de milisegundos.

## Ejemplos de protocolo

### Ejemplo 1

Protocolo que muestra imágenes y envía marcas al amplificador de EEG

```
;Protocolo TEST Horizontal(T)-Vertical

FULLSCREEN false
USE_MATRIX false
USE_EEG true
CENTER_MOUSE true
KGS_VIBRATE false
USE_SPACEKEY true

; Horizontal => Marca 1
; Vertical => Marca 2

INICIAR inicio-experimento.png
ESPERAR 3000

;Lanza la imagen y espera la tecla espacio
KGS stim_pancita_abajo.jpg
SPACESTOP


KGS stim_linea_horizontal.jpg
MARCAR 1
ESPERAR 300
KGS white.png
ESPERAR 700


KGS stim_linea_vertical.jpg
MARCAR 2
ESPERAR 300
KGS white.png
ESPERAR 700


KGS stim_linea_vertical.jpg
MARCAR 2
ESPERAR 300
KGS white.png
ESPERAR 700

(REPETIDO N VECES)

KGS stim_linea_vertical.jpg
MARCAR 2
ESPERAR 300
KGS white.png
ESPERAR 700


KGS stim_linea_horizontal.jpg
MARCAR 1
ESPERAR 300
KGS white.png
ESPERAR 700

TERMINAR
```

### Ejemplo 2

Ejemplo de uso de sonidos e imágenes intercaladas, sin registro ni vibraciones. Nótese que no se pone el comando de finalizar, ya que no es obligatorio.

```
;Protocolo TESTING Sounds

FULLSCREEN false
USE_MATRIX false
USE_EEG false
CENTER_MOUSE false
KGS_VIBRATE false
USE_SPACEKEY false


INICIAR inicio-experimento.png
ESPERAR 3000

;mostrar una imagen
MOSTRAR Lenna.png
ESPERAR 1000

;Lanza el sonido
SONAR MOUSE.wav
;SONAR file_example_WAV_1MG.wav
ESPERAR 1000

;mostrar una imagen
MOSTRAR Lenna.png
ESPERAR 1000

;Lanza el sonido
SONAR MOUSE.wav
;SONAR file_example_WAV_1MG.wav
ESPERAR 1000
```

### Ejemplo 3

Ejemplo de uso de vídeo y de envío de señales de vibración en determinados instantes calculados en función de la duración del vídeo y su registro vía EEG.

```
;Protocolo InSub4 CONTADORES-TACTIL-ESTIMULOS MUDOS

FULLSCREEN true
USE_MATRIX true
USE_EEG true

; Videos
; MARCA 1 => INICIO DE VIDEO
; MARCA 2 => ESTIMULO SUBTITULO
; MARCA 3 => ESTIMULO AUDIO
; MARCA 4 => ESTIMULO TACIL
; MARCA 5 => ESTIMULO MUDO

INICIAR inicio-experimento-insub.png
ESPERAR 3000

; Lanzar el video y esperar la introduccion (poner en pantalla completa)
lanzar "F2_H264.mp4"
ESPERAR 5000

;Video SUBTITULO
MARCAR 1
ESPERAR 5567
MARCAR 2
ESPERAR 4433


;Video AUDIO
MARCAR 1
ESPERAR 2300
MARCAR 3
ESPERAR 7700


;Video TACTIL
MARCAR 1
ESPERAR 4290
MARCAR 4
TACTIL
ESPERAR 5710


;Video MUDO
MARCAR 1
ESPERAR 2200
MARCAR 5
ESPERAR 7800


;Video MUDO
MARCAR 1
ESPERAR 5633
MARCAR 5
ESPERAR 4367

...
(repetido n veces)
...

;Video MUDO
MARCAR 1
ESPERAR 4667
MARCAR 5
ESPERAR 5333


TERMINAR
```

### Ejemplo 4

Este ejemplo muestra imágenes con dos opciones de solución (correcta e incorrecta) intercaladas con estímulos de descanso. El usuario debe decidir cuál cree que es la correcta mediante el uso de dos botones. Los botones en mano derecha e izquierda. El protocolo no lo muestra, pero cada vez que el usuario pulsa el botón izquierdo se envía una marca 8 al amplificador y cuando pulsa el derecho la marca 9.

```
; ALCA EJEMPLO

FULLSCREEN true
USE_EEG true

; Solucion Izquierda
MOSTRAR "ALCA21"
MARCAR 2
ESPERAR 1000
MOSTRAR "blanco"
ESPERAR 500

; Solucion Izquierda
MOSTRAR "ALCA32"
MARCAR 2
ESPERAR 1000
MOSTRAR "blanco"
ESPERAR 500

; Solucion Izquierda
MOSTRAR "ALCA2"
MARCAR 2
ESPERAR 1000
MOSTRAR "blanco"
ESPERAR 500

; Solucion Izquierda
MOSTRAR "ALCA40"
MARCAR 2
ESPERAR 1000
MOSTRAR "blanco"
ESPERAR 500

; Solucion Derecha
MOSTRAR "ALCA26"
MARCAR 1
ESPERAR 1000
MOSTRAR "blanco"
ESPERAR 500

MOSTRAR "ALCA7"
MARCAR 2
ESPERAR 1000
MOSTRAR "blanco"
ESPERAR 500

MOSTRAR "ALCA17"
MARCAR 2
ESPERAR 1000
MOSTRAR "blanco"
ESPERAR 500

MOSTRAR "ALCA36"
MARCAR 1
ESPERAR 1000
MOSTRAR "blanco"
ESPERAR 500

MOSTRAR "ALCA39"
MARCAR 2
ESPERAR 1000
MOSTRAR "blanco"
ESPERAR 500

MOSTRAR "ALCA3"
MARCAR 2
ESPERAR 1000
MOSTRAR "blanco"
ESPERAR 500
```

### Ejemplo 5

Ejemplo de uso de ESPERAR_VIDEO para sincronizar la ejecución del protocolo con la duración del vídeo. Si el vídeo dura más que la suma de ESPERARs entre LANZAR y ESPERAR_VIDEO, el protocolo espera a que el vídeo termine antes de continuar.

```
;Protocolo ELA con sincronización de vídeo

FULLSCREEN true
USE_EEG true

INICIAR inicio-experimento.png
ESPERAR 3000

; El vídeo dura 3000ms. Las ESPERAs suman 2500ms.
; ESPERAR_VIDEO esperará los 500ms restantes.
lanzar "ELA_1.mp4"
ESPERAR 1000
MARCAR 4
ESPERAR 1500
ESPERAR_VIDEO

MOSTRAR Negro.bmp

; Si el vídeo es más corto que las ESPERAs,
; ESPERAR_VIDEO no añade espera adicional.
lanzar "ELA_2.mp4"
ESPERAR 1120
MARCAR 4
ESPERAR 1040
MARCAR 4
ESPERAR_VIDEO

MOSTRAR Negro.bmp

TERMINAR
```
