package com.vitact.eegcontrol;

import com.fazecast.jSerialComm.SerialPort;
import com.vitact.eegcontrol.bean.*;
import com.vitact.eegcontrol.type.EventEnum;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import javafx.stage.Screen;
import org.apache.logging.log4j.*;

class ProtocolThread extends NotifyingThread {
	ListView<EventBean> list;
	ArrayList<EventBean> events;
	HashMap<String, MediaBean> medias;
	ArrayList<EstimulusBean> estims;
	EstimulusBean estNull;
	List<Integer> marks;
	/** Volatile: lo lee el cronómetro desde el hilo de JavaFX. */
	volatile long initTime;
	long accTime;
	SerialPort comEEG;
	SerialPort comMatrix;
	SerialPort comGlove;
	SerialPort comMulti;
	byte[] zeros = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	Logger loggerProtocol;
	Logger loggerEvent;
	Logger logger;
	boolean useOldProtocol = false;
	boolean multimediaFlag = true;
	volatile boolean videoEndFlag = true;
	volatile EmbeddedMediaPlayer currentVideoPlayer = null;
	volatile boolean audioEndFlag = true;
	volatile MediaPlayer currentAudioPlayer = null;
	EEGControl padre;

	@SuppressWarnings("unused")
	private final Set<ThreadCompleteListener> listeners = new CopyOnWriteArraySet<>();

	/*
	 * byte[] estimNULL = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
	 * 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }; int[][] estimInsubInt = { { 0, 240, 240, 0,
	 * 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240,
	 * 240, 0, 0, 240, 240, 0 }, { 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0,
	 * 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0 }, { 0, 240,
	 * 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0,
	 * 240, 240, 0, 0, 240, 240, 0 }, { 0, 240, 240, 0, 0, 240, 240, 0, 0, 240,
	 * 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0 } };
	 *
	 * byte[][] estimInsub;
	 */

	EstimulusBean defaultStimulus, nullStimulus;
	String stimInsubInt = "0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0 , 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0 , 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0 , 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0, 0, 240, 240, 0";
	String stimNullInt = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
	int eventCounter = 0;
	boolean vibrate = false;
	Image whiteImage;
	AnimationTimer timer = null;

	public ProtocolThread(ListView<EventBean> l, ArrayList<EventBean> ev,
			HashMap<String, MediaBean> medias, List<Integer> ma, ArrayList<EstimulusBean> es,
			EstimulusBean eN, SerialPort cEEG, SerialPort cMatr, SerialPort cGlove,
			SerialPort cMulti, EEGControl padre) {
		logger = LogManager.getLogger(this.getClass().getName());
		loggerProtocol = LogManager.getLogger("ProtocolLog");
		loggerEvent = LogManager.getLogger("EventsLog");
		loggerEvent.info("order;event;event literal;milliseconds");
		this.padre = padre;
		list = l;
		events = ev;
		estims = es;
		marks = ma;
		estNull = eN;
		comEEG = cEEG;
		comMatrix = cMatr;
		comGlove = cGlove;
		comMulti = cMulti;
		this.medias = medias;

		defaultStimulus = new EstimulusBean(1, EEGControl.matrixDimension, stimInsubInt);
		nullStimulus = new EstimulusBean(0, EEGControl.matrixDimension, stimNullInt);
		String mediaIniciar = EEGControl.BASE_FILE + EEGControl.MULTIMEDIA_FILE_BASE + "white.png";

		File file = new File(mediaIniciar);
		whiteImage = new Image(file.toURI().toString(), 48, 32, false, false);

	}

	/* (non-Javadoc)
	 * @see com.vitact.eegcontrol.NotifyingThread#doRun()
	 */
	/* (non-Javadoc)
	 * @see com.vitact.eegcontrol.NotifyingThread#doRun()
	 */
	@Override
	public void doRun() {
		int i = 0;

		if (EEGControl.useMatrixProtocol) {
			logger.info("PREPARANDO MATRIZ");

			if (comMatrix != null) {
				if (comMatrix.isOpen()) {
					if (!sendNULL()) {
						if (comMatrix != null) {
							if (comMatrix.isOpen()) {
								comMatrix.closePort();
								comMatrix = null;
							}
						}
						return;
					}
					comMatrix.writeBytes("AST".getBytes(), "AST".getBytes().length);
					synchronized (this) {
						try {
							wait(1000);
						} catch (Exception e) {
							notifyError("Error initilizing matrix", e);
						}
					}
				} else {
					throw new RuntimeException("The comunications with the matrix is closed.");
				}
			}
		}

		if (EEGControl.useGloveProtocol) {
			logger.info("PREPARANDO GUANTE");

			if (comGlove != null) {
				if (comGlove.isOpen()) {
					//					if (true)
					//					{
					//						if (comGlove != null)
					//						{
					//							if (comGlove.isOpen())
					//							{
					//								comGlove.closePort();
					//								comGlove = null;
					//							}
					//						}
					//						return;
					//					}
					comGlove.writeBytes("INI".getBytes(), "INI".getBytes().length);
					synchronized (this) {
						try {
							wait(1000);
						} catch (Exception e) {
							notifyError("Error initilizing glove", e);
						}
					}
				} else {
					throw new RuntimeException("The comunications with the glove is closed.");
				}
			}
		}

		initTime = System.currentTimeMillis();
		accTime = initTime;

		// If is fullScreen we must wait until the screen has resized
		if (padre.showFullScreen) {
			synchronized (this) {
				accTime += 3000;
				waitFor(accTime);
			}
		}

		// If needed position the mouse in the center of the screen
		if (EEGControl.centerMouse) {
			Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
			int screenX = (int) screenBounds.getWidth() / 2;
			int screenY = (int) screenBounds.getHeight() / 2;
			Platform.runLater(() -> {
				try {
					Robot robot = new Robot();
					robot.mouseMove(screenX, screenY);
				} catch (AWTException e) {
					logger.error("No se ha podido centrar el ratón", e);
				}
			});
		}

		loggerProtocol.info("INICIO PROTOCOLO");

		for (EventBean e : events) {
			if (isStop()) {
				System.out.println("Stopping");
				break;
			}
			if (list != null) {
				list.scrollTo(i);
				list.getFocusModel().focus(i);
				list.getSelectionModel().select(i);
			}
			i++;
			switch (e.getTipo()) {
				case INICIAR: {
					loggerProtocol.info(e.getTipo().getCode() + " " + e.getFile());
					checkForTimer();
					if (e.getFile() != null) {
						multimediaFlag = false;
						Platform.runLater(() -> {
							EEGControl.addImage(padre.getRootProtocol(), e.getFile(), true);
							multimediaFlag = true;
						});
						try {
							waitForMultimediaFlagImage();
						} catch (TimeoutException e1) {
							notifyError("No se ha podido cargar la imagen " + e.getFile(), null);
						}
					}
					break;
				}
				case SPACESTOP:
				case CLICKSTOP: {
					loggerProtocol.info(e.getTipo().getCode() + " " + e.getFile());
					multimediaFlag = false;
					Platform.runLater(() -> padre.showClickLabel(e.getTipo() == EventEnum.CLICKSTOP));
					try {
						waitForClickFlag();
						// If needed possition the mouse in the center of the screen
						if (EEGControl.centerMouse) {
							Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
							int screenX = (int) screenBounds.getWidth() / 2;
							int screenY = (int) screenBounds.getHeight() / 2;
							Platform.runLater(() -> {
								try {
									Robot robot = new Robot();
									robot.mouseMove(screenX, screenY);
								} catch (AWTException e1) {
									logger.error("No se ha podido centrar el ratón", e1);
								}
							});
						}

					} catch (TimeoutException e1) {
						notifyError("No se ha recibido el click del raton" + e.getFile(), null);
					}
					break;
				}

				case KGS:
				case ESTIM_OLD: {
					loggerProtocol.info(e.getTipo().getCode() + " " + e.getFile());
					if (e.getFile() != null) {
						//						Runnable frameGrabber = new Runnable()
						//						{
						//							int counter = 0;
						//							@Override
						//							public void run()
						//							{
						//								System.out.println("Counter: " + counter);
						//								if (counter % 2 == 0)
						//									EEGControl.addImage(padre.getRootProtocol(), e.getImg());
						//								else
						//									EEGControl.addImage(padre.getRootProtocol(), whiteImage);
						//
						//								counter++;
						//							}
						//						};
						//
						//						timerXcutor = Executors.newSingleThreadScheduledExecutor();
						//						timerXcutor.scheduleAtFixedRate(frameGrabber, 0, 33,
						//						                               TimeUnit.MILLISECONDS);

						if (EEGControl.kgsVibrate) {
							checkForTimer();
							timer = new AnimationTimer() {
								int counter = 0;
								private long lastUpdate = 0;

								@Override
								public void handle(long now) {
									if (now - lastUpdate >= EEGControl.OLD_STIM_VIBRATION_MILIS
											* 1000000) // THIS TIME IS NANOS
									{
										if (counter % 2 == 0)
											EEGControl.addImage(padre.getRootProtocol(),
													e.getImg());
										else
											EEGControl.addImage(padre.getRootProtocol(),
													whiteImage);
										counter++;
										lastUpdate = now;
									}

								}
							};
							timer.start();
						} else {

							multimediaFlag = false;
							Platform.runLater(() -> {
								EEGControl.addImage(padre.getRootProtocol(), e.getImg());
								multimediaFlag = true;
							});
							try {
								waitForMultimediaFlagImage();
							} catch (TimeoutException e1) {
								notifyError("No se ha podido cargar la imagen " + e.getFile(),
										null);
							}

						}

						/*
						 * flasher = new Timeline( new KeyFrame(Duration.seconds(0.5), ev ->
						 * { EEGControl.addImage(padre.getRootProtocol(), e.getImg()); }),
						 *
						 * new KeyFrame(Duration.seconds(0.5), ev -> {
						 * EEGControl.addImage(padre.getRootProtocol(), whiteImage); }) );
						 * flasher.setCycleCount(Timeline.INDEFINITE);
						 * flasher.setAutoReverse(true); flasher.play();
						 */
					}
					break;
				}
				case MULTI: {
					loggerProtocol.info(e.getTipo().getCode() + " " + e.getMediaReference() + " " + e.getLength());
					// First send command
					Thread t1 = new Thread(this::sendMultistimulator);
					t1.start();
					// Wait for time between stim and image
					accTime += e.getLength();
					waitFor(accTime);
					// Then Show image
					executeShowImage(e);
				}
				case MOSTRAR: {
					executeShowImage(e);
					break;
				}
				case SONAR: {
					loggerProtocol.info(e.getTipo().getCode() + " " + e.getFile());
					checkForTimer();
					MediaBean mediaBean = medias.get(e.getMediaReference());
					if (mediaBean != null) {
						multimediaFlag = false;
						// Stop previous audio (if any) so a new SONAR replaces it instead of overlapping
						MediaPlayer previous = currentAudioPlayer;
						if (previous != null) {
							logger.debug("SONAR: stopping previous audio before playing new one");
							previous.stop();
							previous.dispose();
							currentAudioPlayer = null;
						}
						audioEndFlag = false;
						Platform.runLater(() -> {
							// ShowImage
							if (mediaBean.getImage() != null)
								EEGControl.addImage(padre.getRootProtocol(), mediaBean.getImage());
							else
								EEGControl.addImage(padre.getRootProtocol(),
										MediaBean.SOUND_DEFAULT_IMAGE, false);
							// Play Sound
							final MediaPlayer mediaPlayer = new MediaPlayer(mediaBean.getSound());
							// End-of-media / error callbacks: only react if we are still the
							// active player (a later SONAR may have replaced us already)
							mediaPlayer.setOnEndOfMedia(() -> {
								if (currentAudioPlayer == mediaPlayer) {
									currentAudioPlayer = null;
									audioEndFlag = true;
								}
								mediaPlayer.dispose();
							});
							mediaPlayer.setOnError(() -> {
								logger.error("Audio MediaPlayer error: " + mediaPlayer.getError());
								if (currentAudioPlayer == mediaPlayer) {
									currentAudioPlayer = null;
									audioEndFlag = true;
								}
								multimediaFlag = true;
							});
							currentAudioPlayer = mediaPlayer;
							mediaPlayer.play();
							multimediaFlag = true;
						});
						try {
							waitForMultimediaFlagImage();
						} catch (TimeoutException e1) {
							notifyError("No se ha podido cargar el sonido " + e.getMediaReference(),
									null);
						}
					}
					break;
				}
				case LANZAR: {
					long multimediaInit = System.currentTimeMillis();
					loggerProtocol.info(e.getTipo().getCode() + " " + e.getFile());
					checkForTimer();
					MediaBean mediaBean = medias.get(e.getMediaReference());
					if (mediaBean != null) {
						multimediaFlag = false;
						addVideo(this.padre.getRootProtocol(), mediaBean);
						try {
							waitForMultimediaFlagVideo();
						} catch (TimeoutException e1) {
							notifyError("No se ha podido cargar el video " + e.getFile(), null);
							setStop(true);
							break;
						}
						long multimediaStart = System.currentTimeMillis();
						accTime = accTime + (multimediaStart - multimediaInit);
						loggerProtocol.info("LANZADO " + e.getFile());
					}

					break;
				}
				case MARCAR: {
					loggerProtocol.info(e.getTipo().getCode() + " " + e.getFile());

					sendMark(e.getLength());
					/*
					 * Thread t1 = new Thread(new Runnable() { public void run() {
					 * sendEstim(e.getLength()); } }); t1.start();
					 */
					break;
				}
				case TACTIL: {
					loggerProtocol.info(e.getTipo().getCode() + " " + e.getFile());

					Thread t1 = new Thread(this::sendEstim);
					t1.start();
					break;
				}
				case VIBRAR: {
					loggerProtocol.info(e.getTipo().getCode() + " " + e.getFile());

					Thread t1 = new Thread(() -> sendGlove(e.getFile()));
					t1.start();
					break;
				}
				case ESPERAR: {
					loggerProtocol.info(e.getTipo().getCode() + " " + e.getFile());

					accTime += e.getLength();
					waitFor(accTime);
					break;
				}
				case ESPERAR_VIDEO: {
					loggerProtocol.info(e.getTipo().getCode());
					if (!videoEndFlag) {
						logger.debug("ESPERAR_VIDEO: video still playing, waiting for it to end");
						waitForVideoEnd();
					} else {
						logger.debug("ESPERAR_VIDEO: video already ended, continuing");
					}
					accTime = System.currentTimeMillis();
					break;
				}
				case PARAR_VIDEO: {
					loggerProtocol.info(e.getTipo().getCode());
					EmbeddedMediaPlayer player = currentVideoPlayer;
					if (player != null) {
						logger.debug("PARAR_VIDEO: stopping current video");
						player.controls().stop();
						currentVideoPlayer = null;
						videoEndFlag = true;
					} else {
						logger.debug("PARAR_VIDEO: no video playing, continuing");
					}
					accTime = System.currentTimeMillis();
					break;
				}
				case ESPERAR_AUDIO: {
					loggerProtocol.info(e.getTipo().getCode());
					if (!audioEndFlag) {
						logger.debug("ESPERAR_AUDIO: audio still playing, waiting for it to end");
						waitForAudioEnd();
					} else {
						logger.debug("ESPERAR_AUDIO: audio already ended, continuing");
					}
					accTime = System.currentTimeMillis();
					break;
				}
				case PARAR_AUDIO: {
					loggerProtocol.info(e.getTipo().getCode());
					MediaPlayer audioPlayer = currentAudioPlayer;
					if (audioPlayer != null) {
						logger.debug("PARAR_AUDIO: stopping current audio");
						audioPlayer.stop();
						audioPlayer.dispose();
						currentAudioPlayer = null;
						audioEndFlag = true;
					} else {
						logger.debug("PARAR_AUDIO: no audio playing, continuing");
					}
					accTime = System.currentTimeMillis();
					break;
				}
				case TERMINAR: {
					loggerProtocol.info(e.getTipo().getCode() + " " + e.getFile());
					checkForTimer();
					closePorts();
					setStop(true);
					break;
				}
				default: {
					checkForTimer();
					logger.error("Unknown Protocol Event " + e.getTipo());
					break;
				}
			}
		}
		loggerProtocol.info("FINALIZADO");

		if (comMatrix != null) {
			if (comMatrix.isOpen()) {
				comMatrix.closePort();
				comMatrix = null;
			}
		}
		// padrePane.getScene().getWindow().hide();
	}

	/**
	 * Detiene el vídeo y el audio que estuvieran sonando. Pensado para abortar el protocolo
	 * desde fuera: hay que dejar de renderizar antes de que se liberen los reproductores,
	 * porque liberar un reproductor VLCJ en uso no lanza excepción, tumba la JVM.
	 */
	void stopMedia() {
		EmbeddedMediaPlayer video = currentVideoPlayer;
		if (video != null) {
			logger.debug("stopMedia: deteniendo vídeo en curso");
			video.controls().stop();
			currentVideoPlayer = null;
			videoEndFlag = true;
		}
		MediaPlayer audio = currentAudioPlayer;
		if (audio != null) {
			logger.debug("stopMedia: deteniendo audio en curso");
			audio.stop();
			audio.dispose();
			currentAudioPlayer = null;
			audioEndFlag = true;
		}
	}

	public void checkForTimer() {
		if (timer != null) {
			timer.stop();
			timer = null;
		}

		//		if (timerXcutor != null && !timerXcutor.isShutdown())
		//		{
		//			try
		//			{
		//				// stop the timer
		//				timerXcutor.shutdown();
		//				timerXcutor.awaitTermination(33, TimeUnit.MILLISECONDS);
		//			}
		//			catch (InterruptedException e)
		//			{
		//				// log any exception
		//				System.err
		//				  .println("Exception in stopping the frame capture, trying to release the camera now... "
		//				      + e);
		//			}
		//		}
	}

	// Todos los bucles de espera comprueban isStop(): sin eso, interrumpir el protocolo
	// mientras espera deja el hilo girando para siempre, doRun() nunca retorna y la
	// limpieza de fin de protocolo no llega a ejecutarse.
	private void waitFor(long t) {
		// Espera activa a propósito: la precisión exigida es de milisegundos, así que no
		// se puede dormir. onSpinWait() le dice a la CPU que esto es un bucle de espera.
		while (System.currentTimeMillis() < t && !isStop()) {
			Thread.onSpinWait();
		}
	}

	private void waitForVideoEnd() {
		while (!videoEndFlag && !isStop()) {
			Thread.onSpinWait();
		}
	}

	private void waitForAudioEnd() {
		while (!audioEndFlag && !isStop()) {
			Thread.onSpinWait();
		}
	}

	private void waitForMultimediaFlagVideo() throws TimeoutException {
		long initFlag = System.currentTimeMillis();
		while (!multimediaFlag && !isStop()) {
			if (System.currentTimeMillis() - initFlag > EEGControl.MULTIMEDIA_TIMEOUT)
				throw new TimeoutException("No se ha podido cargar el contenido multimedia.");
		}
	}

	private void waitForMultimediaFlagImage() throws TimeoutException {
		long multimediaInit = System.currentTimeMillis();
		while (!multimediaFlag && !isStop()) {
			if (System.currentTimeMillis() - multimediaInit > EEGControl.MULTIMEDIA_TIMEOUT) {
				long multimediaStart = System.currentTimeMillis();
				accTime = accTime + (multimediaStart - multimediaInit);
				throw new TimeoutException("No se ha podido cargar el contenido multimedia.");
			}
		}
		long multimediaStart = System.currentTimeMillis();
		accTime = accTime + (multimediaStart - multimediaInit);
	}

	// Sin timeout a propósito: espera una acción del sujeto y no se puede acotar por
	// tiempo sin abortar experimentos legítimos. isStop() basta para poder interrumpirla.
	private void waitForClickFlag() throws TimeoutException {
		long multimediaInit = System.currentTimeMillis();
		while (!multimediaFlag && !isStop()) {
			Thread.onSpinWait();
		}
		long multimediaStart = System.currentTimeMillis();
		accTime = accTime + (multimediaStart - multimediaInit);
	}

	public String toMin2(final long millis) {
		long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
				TimeUnit.MILLISECONDS.toMinutes(millis));
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
				TimeUnit.MILLISECONDS.toHours(millis));
		long hours = TimeUnit.MILLISECONDS.toHours(millis);

		StringBuilder b = new StringBuilder();
		if (hours == 0)
			b.append("00");
		else {
			if (hours < 10)
				b.append("0");
			b.append(hours);
		}
		b.append(":");
		if (minutes == 0)
			b.append("00");
		else {
			if (minutes < 10)
				b.append("0");
			b.append(minutes);
		}
		b.append(":");
		if (seconds == 0)
			b.append("00");
		else {
			if (seconds < 10)
				b.append("0");
			b.append(seconds);
		}
		return b.toString();
	}

	public void sendMark(int m) {
		// loggerEvent.info("order;event;event literal;milliseconds");
		if (EEGControl.useEEGProtocol) {
			loggerEvent.info((eventCounter++) + ";" + m + ";Marca Externa " + m + ";"
					+ System.currentTimeMillis());
			if (m > 9)
				m = 9;
			if (comEEG != null)
				comEEG.writeBytes(zeros, m);
		} else {
			loggerEvent.info((eventCounter++) + ";" + m + ";Marca Externa " + m + ";"
					+ System.currentTimeMillis() + "; NOT SENT TO EEG DUE TO CONFIG");
		}
	}

	/** Envía un estímulo táctil concreto de la lista de estímulos. Sin uso actualmente
	 *  (el protocolo sólo usa {@link #sendEstim()}), se mantiene para el comando MARCAR con matriz. */
	@SuppressWarnings("unused")
	public void sendEstim(int m) {
		if (EEGControl.useMatrixProtocol) {
			if (comMatrix != null) {
				if (comMatrix.isOpen()) {
					try {
						if (estims.get(m) == null)
							return;
					} catch (IndexOutOfBoundsException e1) {
						logger.error("Error leyendo estímulos: " + e1.getMessage());
						return;
					}
					loggerProtocol.debug("ENVIADO EST�MULO T�CTIL " + m);
					// Si la matriz no confirma no tiene sentido esperar el tiempo del
					// estímulo ni mandar el NULL: waitFor2() ya ha marcado la parada.
					if (!sendMSG(estims.get(m)))
						return;
					// waitFor(3000);
					synchronized (this) {
						try {
							wait(EEGControl.STIMULUS_TIME_MILIS);
						} catch (Exception e) {
							logger.error("Error esperando al estímulo táctil", e);
						}
					}

					// El NULL apaga el estímulo. Si falla, la matriz puede quedarse
					// energizada; waitFor2() ya habrá abortado el protocolo, pero conviene
					// que quede constancia de por qué.
					if (!sendNULL())
						logger.error("No se ha podido apagar el estímulo táctil: la matriz "
								+ "puede haber quedado activa");
				} else {
					logger.error("The comunications with the matrix is closed.");
				}
			}
		} else {
			loggerProtocol.info(
					"ESTIMULO TACTICL " + m + " NO ENVIADO DEBIDO A CONFIGURACIÓN DEL PROTOCOLO");
		}
	}

	public void sendGlove(String t) {
		if (EEGControl.useGloveProtocol) {
			if (comGlove != null) {
				if (comGlove.isOpen()) {
					loggerProtocol.info("ENVIADO EST�MULO T�CTIL " + t + " A GUANTE ");

					sendStrGlove(t);

				} else {
					logger.error("The communications with the glove are closed.");
				}
			}
		} else {
			loggerProtocol.info(
					"ESTIMULO TACTIL A GUANTE NO ENVIADO DEBIDO A CONFIGURACI�N DEL PROTOCOLO");
		}
	}

	public void sendMultistimulator() {
		if (EEGControl.useMultiStimulator) {
			if (comMulti != null) {
				if (comMulti.isOpen()) {
					String t = EEGControl.multistimulatorCommand;
					loggerProtocol.info("ENVIADO COMANDO " + t + " AL MULTISTIMULADOR ");

					sendStrMulti(t);

				} else {
					logger.error("The communications with the multistimulator are closed.");
				}
			}
		} else {
			loggerProtocol.info(
					"COMANDO A MULTIESTIMULADOR NO ENVIADO DEBIDO A CONFIGURACIÓN DEL PROTOCOLO");
		}
	}

	public void sendEstim() {
		if (EEGControl.useMatrixProtocol) {
			if (comMatrix != null) {
				if (comMatrix.isOpen()) {
					loggerProtocol.info("ENVIADO EST�MULO T�CTIL POR DEFECTO");

					if (!sendMSG(defaultStimulus))
						return;
					// waitFor(3000);
					synchronized (this) {
						try {
							wait(EEGControl.STIMULUS_TIME_MILIS);
						} catch (Exception e) {
							logger.error("Error esperando al estímulo táctil por defecto", e);
						}
					}

					// El NULL apaga el estímulo. Si falla, la matriz puede quedarse
					// energizada; waitFor2() ya habrá abortado el protocolo, pero conviene
					// que quede constancia de por qué.
					if (!sendNULL())
						logger.error("No se ha podido apagar el estímulo táctil: la matriz "
								+ "puede haber quedado activa");
				} else {
					logger.error("The comunications with the matrix is closed.");
				}
			}
		} else {
			loggerProtocol.info(
					"ESTIMULO TACTICL POR DEFECTO NO ENVIADO DEBIDO A CONFIGURACIÓN DEL PROTOCOLO");
		}
	}

	/**
	 * Espera a que la matriz conteste con el carácter indicado. Si no llega dentro del
	 * plazo, aborta el protocolo y avisa al operador.
	 * <p>
	 * El contrato es positivo, {@code true} = la matriz ha confirmado, igual que
	 * {@link #sendMSG(EstimulusBean)} y {@link #sendNULL()}. IntelliJ avisa de que todas
	 * las llamadas lo niegan, porque el único caso interesante es el fallo; invertirlo a
	 * "ha fallado" rompería la coherencia con los otros dos y deja lógica negativa en las
	 * condiciones compuestas del bucle de envío.
	 *
	 * @param c carácter de confirmación esperado
	 * @return true si la matriz ha confirmado dentro del plazo
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	boolean waitFor2(char c) {
		InputStream in = comMatrix.getInputStream();
		int counter = 0;
		long readTimeout = 20;
		long timeoutReadMillis = 1000;
		try {
			while (true) {
				if (comMatrix.bytesAvailable() == 0) {
					synchronized (this) {
						try {
							wait(readTimeout);
						} catch (Exception e) {
							logger.error("Error esperando datos de la matriz", e);
						}
						counter++;
						if (counter * readTimeout > timeoutReadMillis) {
							logger.error("Timeout esperando '" + c + "' de la matriz tras "
									+ timeoutReadMillis + " ms; se aborta el protocolo");
							// Aquí había setStop(false), que limpiaba el flag en vez de
							// ponerlo. Si la matriz no responde no habrá más estimulación
							// táctil, así que continuar solo produce un registro que parece
							// completo y no lo es: las marcas afirmarían estímulos que el
							// sujeto nunca recibió.
							setStop(true);
							notifyError("No hay comunicación con la matriz. ¿Está encendida? "
									+ "Se detiene el protocolo: sin matriz no se entregarían "
									+ "los estímulos táctiles y el registro no sería válido.",
									null);
							in.close();
							return false;
						}
					}
				} else {
					char ca = (char) in.read();
					// System.out.print(ca);
					if (ca == c) {
						in.close();
						return true;
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error found waiting for char " + c, e);
			return false;
		}
	}

	/**
	 * Envía un estímulo a la matriz. Los retornos de waitFor2() se ignoraban, de modo que un
	 * timeout a mitad de protocolo seguía escribiendo el resto de la rejilla a un dispositivo
	 * que no contestaba y la ejecución continuaba como si nada.
	 *
	 * @return false si la matriz no ha confirmado; el protocolo ya se ha marcado para parar
	 */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	boolean sendMSG(EstimulusBean est) {
		// comMatrix.writeBytes(charInt, 1);
		byte[] bytesInt = "?".getBytes();
		comMatrix.writeBytes(bytesInt, bytesInt.length);
		int dim = est.getDim();
		if (!waitFor2('!'))
			return false;
		for (int i = 0; i < 4; i++) {
			comMatrix.writeBytes(Arrays.copyOfRange(est.getEstim(), dim * i, dim * (i + 1)), dim);
			if (i != 3 && !useOldProtocol && !waitFor2('*'))
				return false;
		}
		return true;
	}

	void sendStrGlove(String t) {
		comGlove.writeBytes(t.getBytes(), t.getBytes().length);
	}

	void sendStrMulti(String t) {
		if(comMulti.isOpen()) {
			comMulti.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
			logger.warn("Sending to multistimulator: #" + t + "#");
			comMulti.writeBytes(t.getBytes(), t.length());
			//portInUse.closePort();
		} else {
			logger.error("The communications with the multistimulator are closed.");
		}
	}

	/** @return true si la matriz ha confirmado el estímulo nulo. Contrato positivo, ver
	 *  {@link #waitFor2(char)}. */
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	boolean sendNULL() {
		// comMatrix.writeBytes(charInt, 1);
		byte[] bytesInt = "?".getBytes();
		comMatrix.writeBytes(bytesInt, bytesInt.length);
		int dim = nullStimulus.getDim();
		if (!waitFor2('!')) {
			return false;
		}
		for (int i = 0; i < 4; i++) {
			comMatrix.writeBytes(
					Arrays.copyOfRange(nullStimulus.getEstim(), dim * i, dim * (i + 1)), dim);
			if (i != 3 && !useOldProtocol && !waitFor2('*'))
				return false;
		}
		return true;
	}

	private void addVideo(BorderPane pane, MediaBean mediaBean) {
		logger.debug("Trying to load video " + mediaBean.getVideoPath());
		videoEndFlag = false;

		EmbeddedMediaPlayer vlcPlayer = mediaBean.getVlcPlayer();
		ImageView imageView = mediaBean.getVideoImageView();

		// Set up event listeners once per player
		if (!mediaBean.isVlcPlayerReady()) {
			mediaBean.setVlcPlayerReady(true);
			vlcPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
				@Override
				public void playing(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer) {
					logger.debug("VLCJ playing: " + mediaBean.getVideoPath());
					multimediaFlag = true;
				}

				@Override
				public void finished(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer) {
					logger.debug("VLCJ finished: " + mediaBean.getVideoPath());
					currentVideoPlayer = null;
					videoEndFlag = true;
				}

				@Override
				public void error(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer) {
					logger.error("VLCJ player error for: " + mediaBean.getVideoPath());
					currentVideoPlayer = null;
					videoEndFlag = true;
					multimediaFlag = true;
				}
			});
		}

		currentVideoPlayer = vlcPlayer;

		Platform.runLater(() -> {
			pane.getChildren().clear();

			// Remove from previous parent if replaying
			if (imageView.getParent() != null) {
				((Pane) imageView.getParent()).getChildren().remove(imageView);
			}

			VBox mvPane = new VBox();
			mvPane.getChildren().add(imageView);
			mvPane.setStyle("-fx-background-color: black;");
			mvPane.setAlignment(Pos.CENTER);
			pane.setCenter(mvPane);

			// Fit video to available space
			imageView.fitWidthProperty().bind(pane.widthProperty());
			imageView.fitHeightProperty().bind(pane.heightProperty());
			imageView.setPreserveRatio(true);

			pane.getScene().getWindow().setOnHidden(ignored -> {
				vlcPlayer.controls().stop();
				setStop(true);
			});

			logger.debug("Starting VLCJ playback: " + mediaBean.getVideoPath());
			vlcPlayer.media().play(mediaBean.getVideoPath());
		});
	}


	private void executeShowImage(EventBean e) {
		loggerProtocol.info(e.getTipo().getCode() + " " + e.getMediaReference());
		checkForTimer();
		MediaBean mediaBean = medias.get(e.getMediaReference());
		if (mediaBean != null) {
			multimediaFlag = false;
			Platform.runLater(() -> {
				if (mediaBean.getImage() != null && EEGControl.USE_MEDIABEAN)
					EEGControl.addImage(padre.getRootProtocol(), mediaBean.getImage());
				else
					EEGControl.addImage(padre.getRootProtocol(), e.getFile(), false);
				multimediaFlag = true;
			});
			try {
				waitForMultimediaFlagImage();
			} catch (TimeoutException e1) {
				notifyError("No se ha podido cargar la imagen " + e.getMediaReference(),
						null);
			}
		}
	}

	private void notifyError(String message, Throwable th) {
		if (th == null)
			logger.error(message);
		else
			logger.error(message, th);

		Platform.runLater(() -> {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error Ejecutando Protocolo");
			alert.setContentText(message);
			EEGControl.applyIcon(alert);

			if (th != null) {
				// Create expandable Exception.
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				th.printStackTrace(pw);
				String exceptionText = sw.toString();

				Label label = new Label("The exception stacktrace was:");

				TextArea textArea = new TextArea(exceptionText);
				textArea.setEditable(false);
				textArea.setWrapText(true);

				textArea.setMaxWidth(Double.MAX_VALUE);
				textArea.setMaxHeight(Double.MAX_VALUE);
				GridPane.setVgrow(textArea, Priority.ALWAYS);
				GridPane.setHgrow(textArea, Priority.ALWAYS);

				GridPane expContent = new GridPane();
				expContent.setMaxWidth(Double.MAX_VALUE);
				expContent.add(label, 0, 0);
				expContent.add(textArea, 0, 1);

				// Set expandable Exception into the dialog pane.
				alert.getDialogPane().setExpandableContent(expContent);
			}

			alert.showAndWait();
		});
	}


	private void closePorts() {
		if (comMatrix != null) {
			if (comMatrix.isOpen()) {
				comMatrix.closePort();
				comMatrix = null;
			}
		}
		if (comEEG != null) {
			if (comEEG.isOpen()) {
				comEEG.closePort();
				comEEG = null;
			}
		}
		if (comGlove != null) {
			if (comGlove.isOpen()) {
				comGlove.closePort();
				comGlove = null;
			}
		}
		if (comMulti != null) {
			if (comMulti.isOpen()) {
				comMulti.closePort();
				comMulti = null;
			}
		}
	}

}
