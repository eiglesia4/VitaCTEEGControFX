package com.vitact.eegcontrol;

import com.fazecast.jSerialComm.SerialPort;
import com.vitact.eegcontrol.bean.*;
import com.vitact.eegcontrol.opencv.OpenCVTransform;
import com.vitact.eegcontrol.type.EventEnum;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.*;
import javafx.geometry.Insets;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.util.Duration;
import org.apache.logging.log4j.*;

class ProtocolThread extends NotifyingThread {
	ListView<EventBean> list;
	Label timeT;
	ArrayList<EventBean> events;
	HashMap<String, MediaBean> medias;
	ArrayList<EstimulusBean> estims;
	EstimulusBean estNull;
	List<Integer> marks;
	long initTime;
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
	EEGControl padre;

	Duration duration;
	Slider timeSlider;
	Label playTime;

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
			EstimulusBean eN, SerialPort cEEG, SerialPort cMatr, SerialPort cGlove, SerialPort cMulti, Label ti,
			EEGControl padre) {
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
		timeT = ti;
		this.medias = medias;

		defaultStimulus = new EstimulusBean(1, EEGControl.matrixDimension, stimInsubInt);
		nullStimulus = new EstimulusBean(0, EEGControl.matrixDimension, stimNullInt);
		String mediaIniciar = EEGControl.BASE_FILE + EEGControl.MULTIMEDIA_FILE_BASE + "white.png";

		File file = new File(mediaIniciar);
		whiteImage = new Image(file.toURI().toString(), OpenCVTransform.OLD_STIM_VIDEO_WIDTH,
				OpenCVTransform.OLD_STIM_VIDEO_HEIGHT, false, false);

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
					e.printStackTrace();
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
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								EEGControl.addImage(padre.getRootProtocol(), e.getFile(), true);
								multimediaFlag = true;
							}
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
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							padre.showClickLabel(e.getTipo()== EventEnum.CLICKSTOP);
						}
					});
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
									e1.printStackTrace();
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
							Platform.runLater(new Runnable() {

								@Override
								public void run() {
									EEGControl.addImage(padre.getRootProtocol(), e.getImg());
									multimediaFlag = true;
								}
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
					Thread t1 = new Thread(new Runnable() {
						public void run() {
							sendMultistimulator();
						}
					});
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
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								// ShowImage
								if (mediaBean.getImage() != null)
									EEGControl.addImage(padre.getRootProtocol(),
											mediaBean.getImage());
								else
									EEGControl.addImage(padre.getRootProtocol(),
											MediaBean.SOUND_DEFAULT_IMAGE, false);
								// Play Sound
								MediaPlayer mediaPlayer = new MediaPlayer(mediaBean.getSound());
								mediaPlayer.play();
								//mediaBean.getMediaPlayer().play();
								multimediaFlag = true;
							}
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
						addVideo(this.padre.getRootProtocol(), mediaBean.getVideo());
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

					Thread t1 = new Thread(new Runnable() {
						public void run() {
							sendEstim();
						}
					});
					t1.start();
					break;
				}
				case VIBRAR: {
					loggerProtocol.info(e.getTipo().getCode() + " " + e.getFile());

					Thread t1 = new Thread(new Runnable() {
						public void run() {
							sendGlove(e.getFile());
						}
					});
					t1.start();
					break;
				}
				case ESPERAR: {
					loggerProtocol.info(e.getTipo().getCode() + " " + e.getFile());

					accTime += e.getLength();
					waitFor(accTime);
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

	private void waitFor(long t) {
		while (System.currentTimeMillis() < t) {
			toMin2(System.currentTimeMillis() - initTime);
		}
	}

	private void waitForMultimediaFlagVideo() throws TimeoutException {
		long initFlag = System.currentTimeMillis();
		while (!multimediaFlag) {
			toMin2(System.currentTimeMillis() - initTime);
			if (System.currentTimeMillis() - initFlag > EEGControl.MULTIMEDIA_TIMEOUT)
				throw new TimeoutException("No se ha podido cargar el contenido multimedia.");
			// timeT.setText(toMin2(System.currentTimeMillis() - initTime) + "");
		}
	}

	private void waitForMultimediaFlagImage() throws TimeoutException {
		long multimediaInit = System.currentTimeMillis();
		while (!multimediaFlag) {
			toMin2(System.currentTimeMillis() - initTime);
			if (System.currentTimeMillis() - multimediaInit > EEGControl.MULTIMEDIA_TIMEOUT) {
				long multimediaStart = System.currentTimeMillis();
				accTime = accTime + (multimediaStart - multimediaInit);
				throw new TimeoutException("No se ha podido cargar el contenido multimedia.");
			}
		}
		long multimediaStart = System.currentTimeMillis();
		accTime = accTime + (multimediaStart - multimediaInit);
	}

	private void waitForClickFlag() throws TimeoutException {
		long multimediaInit = System.currentTimeMillis();
		while (!multimediaFlag) {
			toMin2(System.currentTimeMillis() - initTime);
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
			b.append(String.valueOf(hours));
		}
		b.append(":");
		if (minutes == 0)
			b.append("00");
		else {
			if (minutes < 10)
				b.append("0");
			b.append(String.valueOf(minutes));
		}
		b.append(":");
		if (seconds == 0)
			b.append("00");
		else {
			if (seconds < 10)
				b.append("0");
			b.append(String.valueOf(seconds));
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
					sendMSG(estims.get(m));
					// waitFor(3000);
					synchronized (this) {
						try {
							wait(EEGControl.STIMULUS_TIME_MILIS);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					sendNULL();
				} else {
					logger.error("The comunications with the matrix is closed.");
					return;
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
					return;
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
					return;
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

					sendMSG(defaultStimulus);
					// waitFor(3000);
					synchronized (this) {
						try {
							wait(EEGControl.STIMULUS_TIME_MILIS);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					sendNULL();
				} else {
					logger.error("The comunications with the matrix is closed.");
					return;
				}
			}
		} else {
			loggerProtocol.info(
					"ESTIMULO TACTICL POR DEFECTO NO ENVIADO DEBIDO A CONFIGURACIÓN DEL PROTOCOLO");
		}
	}

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
							e.printStackTrace();
						}
						counter++;
						if (counter * readTimeout > timeoutReadMillis) {
							logger.warn("Cansado de esperar " + c);
							notifyError("No comunico con la matriz. ¿Está encendida?", null);
							this.setStop(false);
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

	void sendMSG(EstimulusBean est) {
		// comMatrix.writeBytes(charInt, 1);
		byte[] bytesInt = "?".getBytes();
		comMatrix.writeBytes(bytesInt, bytesInt.length);
		int dim = est.getDim();
		waitFor2('!');
		for (int i = 0; i < 4; i++) {
			comMatrix.writeBytes(Arrays.copyOfRange(est.getEstim(), dim * i, dim * (i + 1)), dim);
			if (i != 3 && !useOldProtocol)
				waitFor2('*');
		}
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
			return;
		}
	}

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
			if (i != 3 && !useOldProtocol)
				waitFor2('*');
		}
		return true;
	}

	private void addVideo(BorderPane pane, String fileName) {
		String mediaSample = EEGControl.BASE_FILE + EEGControl.MULTIMEDIA_FILE_BASE + fileName;

		File file = new File(mediaSample);
		Media media = null;
		try {
			media = new Media(file.toURI().toString());
		} catch (Exception e1) {
			notifyError("Error al cargar el fichero de video " + fileName, e1);
			return;
		}
		addVideo(pane, media);
	}

	private void addVideo(BorderPane pane, Media media) {
		logger.debug("Trying to load video " + media.getSource());
		MediaPlayer mediaPlayer = new MediaPlayer(media);
		mediaPlayer.setAutoPlay(false);
		MediaView mediaView = new MediaView(mediaPlayer);

		pane.getScene().getWindow().setOnHidden(e -> {
			mediaPlayer.stop();
			setStop(true);
		});
		// create mediaView and add media player to the viewer
		mediaPlayer.setOnReady(new Runnable() {
			@Override
			public void run() {
				pane.getChildren().removeAll();

				VBox mvPane = new VBox();
				mvPane.getChildren().add(mediaView);
				mvPane.setStyle("-fx-background-color: black;");
				mvPane.setAlignment(Pos.CENTER);
				pane.setCenter(mvPane);
				logger.debug("Media Ready. Trying to run video ");

				if (EEGControl.showVideoController) {
					HBox mediaBar = new HBox();
					mediaBar.setAlignment(Pos.CENTER);
					mediaBar.setPadding(new Insets(5, 10, 5, 10));
					BorderPane.setAlignment(mediaBar, Pos.CENTER);
					// Add spacer
					Label spacer = new Label("   ");
					mediaBar.getChildren().add(spacer);

					// Add Time label
					Label timeLabel = new Label("Time: ");
					timeLabel.setTextFill(Color.WHITE);
					mediaBar.getChildren().add(timeLabel);

					// Add time slider
					timeSlider = new Slider();
					HBox.setHgrow(timeSlider, Priority.ALWAYS);
					timeSlider.setMinWidth(50);
					timeSlider.setMaxWidth(Double.MAX_VALUE);
					mediaBar.getChildren().add(timeSlider);

					// Add Play label
					playTime = new Label();
					playTime.setTextFill(Color.WHITE);
					playTime.setPrefWidth(130);
					playTime.setMinWidth(50);
					mediaBar.getChildren().add(playTime);

					duration = mediaPlayer.getMedia().getDuration();
					updateValues(mediaPlayer);

					padre.getRootProtocol().setBottom(mediaBar);
				}

				mediaPlayer.play();

				multimediaFlag = true;

				mediaPlayer.setOnEndOfMedia(() -> {
					mediaPlayer.dispose();
				});
			}
		});

		mediaPlayer.currentTimeProperty().addListener(new InvalidationListener() {
			public void invalidated(Observable ov) {
				updateValues(mediaPlayer);
			}
		});

	}

	private void executeShowImage(EventBean e) {
		loggerProtocol.info(e.getTipo().getCode() + " " + e.getMediaReference());
		checkForTimer();
		MediaBean mediaBean = medias.get(e.getMediaReference());
		if (mediaBean != null) {
			multimediaFlag = false;
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					if (mediaBean.getImage() != null && EEGControl.USE_MEDIABEAN)
						EEGControl.addImage(padre.getRootProtocol(),
								mediaBean.getImage());
					else
						EEGControl.addImage(padre.getRootProtocol(), e.getFile(),
								false);
					multimediaFlag = true;
				}
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

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Error Ejecutando Protocolo");
				alert.setContentText(message);

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
			}
		});
	}

	protected void updateValues(MediaPlayer mp) {
		if (playTime != null && timeSlider != null) {
			Platform.runLater(new Runnable() {
				@SuppressWarnings("deprecation")
				public void run() {
					Duration currentTime = mp.getCurrentTime();
					playTime.setText(formatTime(currentTime, duration));
					timeSlider.setDisable(duration.isUnknown());
					if (!timeSlider.isDisabled() && duration.greaterThan(Duration.ZERO)
							&& !timeSlider.isValueChanging()) {
						timeSlider.setValue(currentTime.divide(duration).toMillis() * 100.0);
					}
				}
			});
		}
	}

	private static String formatTime(Duration elapsed, Duration duration) {
		int intElapsed = (int) Math.floor(elapsed.toSeconds());
		int elapsedHours = intElapsed / (60 * 60);
		if (elapsedHours > 0) {
			intElapsed -= elapsedHours * 60 * 60;
		}
		int elapsedMinutes = intElapsed / 60;
		int elapsedSeconds = intElapsed - elapsedHours * 60 * 60 - elapsedMinutes * 60;

		if (duration.greaterThan(Duration.ZERO)) {
			int intDuration = (int) Math.floor(duration.toSeconds());
			int durationHours = intDuration / (60 * 60);
			if (durationHours > 0) {
				intDuration -= durationHours * 60 * 60;
			}
			int durationMinutes = intDuration / 60;
			int durationSeconds = intDuration - durationHours * 60 * 60 - durationMinutes * 60;
			if (durationHours > 0) {
				return String.format("%d:%02d:%02d/%d:%02d:%02d", elapsedHours, elapsedMinutes,
						elapsedSeconds, durationHours, durationMinutes, durationSeconds);
			} else {
				return String.format("%02d:%02d/%02d:%02d", elapsedMinutes, elapsedSeconds,
						durationMinutes, durationSeconds);
			}
		} else {
			if (elapsedHours > 0) {
				return String.format("%d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds);
			} else {
				return String.format("%02d:%02d", elapsedMinutes, elapsedSeconds);
			}
		}
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
