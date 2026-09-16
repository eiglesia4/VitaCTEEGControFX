package com.vitact.eegcontrol;

import com.fazecast.jSerialComm.SerialPort;
import com.vitact.eegcontrol.bean.*;
import com.vitact.eegcontrol.type.*;
import com.vitact.eegcontrol.utils.ProtocolUtils;
import java.io.*;
import java.net.URL;
import java.util.*;
import javafx.animation.PauseTransition;
import javafx.application.*;
import javafx.event.EventHandler;
import javafx.fxml.*;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.*;
import javafx.util.Duration;
import org.apache.logging.log4j.*;
public class EEGControl extends Application
		implements ThreadCompleteListener, EventHandler<KeyEvent> {
	static Logger logger = null;
	private static final int EEG_BAUDRATE = 1200;
	private static final int MATRIX_BAUDRATE = 115200; // 9600;
	private static final int MULTI_BAUDRATE = 9600; // 9600;
	public static final long STIMULUS_TIME_MILIS = 3000;
	public static final long END_PROTOCOL_WAIT_MILIS = 5000;
	/** Plazo máximo de espera a que muera el hilo de un protocolo que se está abortando. */
	public static final long PROTOCOL_STOP_TIMEOUT_MILIS = 5000;
	public static final long MULTIMEDIA_TIMEOUT = 15000;
	public static final long OLD_STIM_VIBRATION_MILIS = 33;

	public static final String MULTIMEDIA_FILE_BASE = "default/Multimedia/";
	public static final String PROTOCOL_FILE_BASE = "default/protoc/";
	public static final String STIMULUS_FILE_BASE = "default/stim/";
	public static final String IMAGE_RESOURCES_FILE_BASE = "default/images/";
	public static final String CONFIG_FILE = "default/properties.ini";
	public static final String STUDY_BASE_DIR = "estudios";
	public static String BASE_FILE = "c://";
	public static final Boolean USE_FULL_STUDY_DATA = false;
	public static final String DEFAULT_VERSION="4.0";

	/** Prefijo de los iconos de ventana en el classpath; se completa con el tamaño. */
	private static final String ICON_BASE = "/images/eeg_icon_";
	/** Tamaños de icono a cargar. JavaFX escoge el mejor para cada contexto. */
	private static final int[] ICON_SIZES = {16, 32, 48, 256};

	ArrayList<EventBean> events = new ArrayList<>();
	HashMap<String, MediaBean> medias = new HashMap<>();
	ArrayList<EstimulusBean> estims = new ArrayList<>();
	EstimulusBean estNull;
	List<Integer> marks = new ArrayList<>();
	int dimension;

	SerialPort comMatrix;
	SerialPort comEEG;
	SerialPort comGlove;
	SerialPort comMulti;
	Stage primaryStage;
	String protocolName = null;
	Stage stageProtocol;
	boolean showFullScreen = false;
	static boolean useEEGProtocol = false;
	static boolean useGloveProtocol = false;
	static boolean useMatrixProtocol = false;
	static boolean showProtocolEvolWindow = false;
	static boolean showVideoController = false;
	static boolean centerMouse = false;
	static boolean kgsVibrate = false;
	static boolean useSpaceAsKey = false;
	static boolean useTarget = false;
	static boolean useMultiStimulator = false;
	boolean correctStimulus = false;
	static int matrixDimension = 28;
	static boolean waitingForSpace = false;
	static String multistimulatorCommand = "s";
	static int multistimulatorWaitStimImageInMillis = 100;
	static String version = DEFAULT_VERSION;
	EEGProtocolProgressController protocolController = null;
	BorderPane rootProtocol = null;
	BorderPane labelBorderPane = null;

	String initalImage = null;
	EEGViewController mainController = null;
	boolean doReusePorts = false;
	StudyBean studyBean;
	ProtocolBean protocolBean;
	ProtocolThread executer = null;
	static MediaPlayerFactory mediaPlayerFactory;
	/** Caché del juego de iconos; se carga una sola vez en el primer uso. */
	private static List<Image> appIcons = null;
	/**
	 * Se incrementa al cargar cada protocolo. La limpieza de fin de protocolo compara su
	 * generación con esta: si no coinciden es que se ha cargado otro protocolo mientras
	 * tanto, y limpiar borraría el estado del nuevo.
	 */
	private int protocolGeneration = 0;
	Label label = new Label("PULSA EL RATON PARA CONTINUAR");
	int multiStimulationMillisPlaying = 0;

	public static boolean USE_MEDIABEAN = true;

	public static Properties properties = new Properties();

		public EEGControl() {
		String[] names = {"0000", "02"};
		reloadLoggers(names);
		logger = LogManager.getLogger(this.getClass().getName());

		EEGControl.BASE_FILE = System.getProperty("user.dir") + "/";
		logger.info("BAse dir " + EEGControl.BASE_FILE);

		// Initialize VLCJ MediaPlayerFactory
		try {
			mediaPlayerFactory = new MediaPlayerFactory();
			logger.info("VLCJ MediaPlayerFactory initialized successfully");
		} catch (Exception e) {
			logger.error("Failed to initialize VLCJ. Is VLC installed?", e);
		}

		File file = new File(CONFIG_FILE);
		if (!file.exists()) {
			logger.error("Config file " + CONFIG_FILE + " does not exists");
		} else {
			try {
				properties.load(new FileInputStream(file));
				showFullScreen = properties.getProperty("fullScreen", "false").equalsIgnoreCase(
						"true");
				showProtocolEvolWindow = properties.getProperty("showEvolution", "false")
						.equalsIgnoreCase("true");
				showVideoController = properties.getProperty("showVideoController", "false")
						.equalsIgnoreCase("true");
				multistimulatorCommand = properties.getProperty("multistimulatorCommand", "s");
				version = properties.getProperty("version", DEFAULT_VERSION);
				try {
					multistimulatorWaitStimImageInMillis = Integer.parseInt(
							properties.getProperty("multistimulatorWaitStimImageInMillis", "100"));
				} catch (NumberFormatException e) {
					logger.error(
							"Error en fichero de configuración: multistiulatorWaitStimImageInSecs debe ser numérico");
				}
				try {
					matrixDimension = Integer.parseInt(
							properties.getProperty("matrixDimension", "28"));
				} catch (NumberFormatException e) {
					logger.error(
							"Error en fichero de configuración: matrixDimension debe ser numérico");
				}
			} catch (FileNotFoundException e) {
				logger.error("Config file " + CONFIG_FILE + " does not exists");
			} catch (IOException e) {
				logger.error("Config file " + CONFIG_FILE + " cannot be read");
			}
		}
	}

	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("EEGControl.fxml"));
			BorderPane root = loader.load();
			mainController = loader.getController();
			mainController.setPadre(this);
			Scene scene = new Scene(root, 500, 500);
			applyStylesheet(scene);
			applyIcon(primaryStage);
			primaryStage.setScene(scene);
			primaryStage.setOnCloseRequest(ignored -> Platform.exit());
			primaryStage.show();
			this.primaryStage = primaryStage;
		} catch (Exception e) {
			logger.error("Error while loading main scene", e);
			Platform.exit();
		}
	}

	public void fileProtocolLoaded(File chosenFile) {
		String previousProtocol = protocolName;
		try {
			protocolName = chosenFile.getName();
		} catch (Exception e) {
			// NO he seleccionado ningún protocolo, me salgo al menú principal
			return;
		}
		logger.debug("Protocolo seleccionado: " + chosenFile.getAbsolutePath());

		// Solo puede ejecutarse un protocolo a la vez. Si quedara uno vivo hay que pararlo
		// y esperar a que muera ANTES de parsear el nuevo: checkProtocolFile() reasigna la
		// lista de eventos que el hilo está recorriendo y libera reproductores VLCJ nativos.
		if (!stopRunningProtocol(previousProtocol)) {
			showErrorDialog("No se ha podido detener el protocolo en ejecución. Cierre la "
					+ "ventana de ejecución y vuelva a intentarlo.");
			return;
		}

		if (!checkProtocolFile(chosenFile)) {
			logger.debug("Programa terminado. Protocolo err�neo");
		} else {
			if (doReusePorts)
				portsLoaded();
			else {
				if (useEEGProtocol || useMatrixProtocol || useGloveProtocol || useMultiStimulator)
					loadPorts();
				else
					loadBusinessLogic();
			}
		}

	}

	/**
	 * Detiene el protocolo en ejecución, si lo hubiera, y espera a que su hilo termine.
	 * <p>
	 * Cargar un protocolo mientras otro corre no debería ocurrir nunca, pero si ocurre hay
	 * que ordenarlo antes de seguir: {@code checkProtocolFile()} reasigna la lista de eventos
	 * que el hilo está recorriendo y {@code resetMedias()} libera los reproductores VLCJ.
	 * Liberar un reproductor nativo mientras renderiza no lanza excepción, tumba la JVM, y
	 * por eso aquí se espera de verdad a que el hilo muera en lugar de solo pedirle que pare.
	 *
	 * @param previousProtocol nombre del protocolo que se estaba ejecutando, solo para el log
	 * @return false si el hilo sigue vivo pasado el plazo, en cuyo caso no se debe continuar
	 */
	private boolean stopRunningProtocol(String previousProtocol) {
		ProtocolThread running = executer;
		if (running == null || !running.isAlive())
			return true;

		logger.warn("Se ha cargado el protocolo " + protocolName + " mientras " + previousProtocol
				+ " seguía ejecutándose; se detiene el anterior");

		running.setStop(true);
		running.checkForTimer();
		running.stopMedia();

		if (stageProtocol != null) {
			stageProtocol.hide();
			stageProtocol = null;
		}

		try {
			running.join(PROTOCOL_STOP_TIMEOUT_MILIS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.warn("Interrumpida la espera a que terminase el protocolo anterior", e);
			return false;
		}

		if (running.isAlive()) {
			logger.error("El protocolo anterior no ha terminado en "
					+ PROTOCOL_STOP_TIMEOUT_MILIS + " ms; no se carga el nuevo para no liberar "
					+ "reproductores que siguen en uso");
			return false;
		}
		executer = null;
		return true;
	}

	private void loadPorts() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("EEGPortsControl.fxml"));
			GridPane root = loader.load();
			EEGPortsViewController controller = loader.getController();
			controller.setPadre(this);
			Scene scene = new Scene(root, 500, 265);
			applyStylesheet(scene);
			Stage stage = new Stage();
			applyIcon(stage);
			stage.setTitle("Configuración de Puertos");

			stage.setScene(scene);
			stage.show();
			stage.setOnHidden(ignored -> {
				if (controller.isConfigured()) {
					comMatrix = controller.getComMatrix();
					comEEG = controller.getComEEG();
					comGlove = controller.getComGlove();
					comMulti = controller.getComMulti();
					doReusePorts = controller.getCbPorts().isSelected();
					portsLoaded();
				}
			});
		} catch (Exception e) {
			logger.error("Error while loading ports scene", e);
			Platform.exit();
		}

	}

	private void portsLoaded() {
		if (comMatrix != null)
			logger.debug("Puerto matriz: " + comMatrix.getDescriptivePortName());
		if (comEEG != null)
			logger.debug("Puerto EEG: " + comEEG.getDescriptivePortName());
		if (comGlove != null)
			logger.debug("Puerto Guante: " + comGlove.getDescriptivePortName());
		if (comMulti != null)
			logger.debug("Puerto Multi-Estimulador: " + comMulti.getDescriptivePortName());

		/*
		 * if (comMatrix != null)
		 *
		 * { File estimulos = fileStimulusLoad(); if (!checkStimulusFile(estimulos))
		 * { logger.error("Programa terminado. Fichero de est�mulos err�neo");
		 * Platform.exit(); } }
		 */

		// Prepare Ports
		if (comEEG != null) {
			comEEG.setComPortParameters(EEG_BAUDRATE, 8, SerialPort.ONE_STOP_BIT,
					SerialPort.NO_PARITY);
			comEEG.openPort();
			logger.debug("EEG port ready");
		}
		if (comMatrix != null) {
			comMatrix.setComPortParameters(MATRIX_BAUDRATE, 8, SerialPort.ONE_STOP_BIT,
					SerialPort.NO_PARITY);
			boolean portOpen = comMatrix.openPort();
			if (portOpen)
				logger.debug("Matrix port ready");
			else {
				logger.debug("Matrix port IS NOT ready");
				showErrorDialog("No se puede abrir la comunicación con la matriz, reinicie el programa.");
				return;
			}
		}
		if (comGlove != null) {
			comGlove.setComPortParameters(MATRIX_BAUDRATE, 8, SerialPort.ONE_STOP_BIT,
					SerialPort.NO_PARITY);
			boolean portOpen = comGlove.openPort();
			if (portOpen)
				logger.debug("Glove port ready");
			else {
				logger.debug("glove port IS NOT ready");
				showErrorDialog("No se puede abrir la comunicación con el guante, reinicie el programa.");
				return;
			}
		}
		if (comMulti != null) {
			comMulti.setComPortParameters(MULTI_BAUDRATE, 8, SerialPort.ONE_STOP_BIT,
					SerialPort.NO_PARITY);
			boolean portOpen = comMulti.openPort();
			if (portOpen)
				logger.debug("Multistimulator port ready");
			else {
				logger.debug("Multistimulator port IS NOT ready");
				showErrorDialog(
						"No se puede abrir la comunicación con el multiestimulador, reinicie el programa.");
				return;
			}
		}


		logger.debug("Sistema preparado...");

		loadBusinessLogic();
	}

	private void loadBusinessLogic() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("EEGProtocolProgress.fxml"));
			BorderPane root = loader.load();
			protocolController = loader.getController();
			protocolController.setEvents(events);
			Scene scene = new Scene(root, 400, 400);
			applyStylesheet(scene);
			Stage stage = new Stage();
			applyIcon(stage);
			stage.setTitle("Progreso de la prueba");

			if (showProtocolEvolWindow) {

				stage.setScene(scene);
				stage.setOnShown(ignored -> doBusinessLogic(protocolController));
				stage.setOnHidden(ignored -> {
					if (stageProtocol != null)
						stageProtocol.close();
				});
				try {
					stage.show();
				} catch (NullPointerException e1) {
					Stage stage1 = (Stage) scene.getWindow();
					stage1.close();
					if (stageProtocol != null)
						stageProtocol.close();
				}
			} else {
				doBusinessLogic(protocolController);
			}
		} catch (IOException e) {
			logger.error("Error on FXML file for ProtocolProgress.", e);
			Platform.exit();
		}

	}

	/**
	 * Abre la ventana de ejecución del protocolo y prepara el hilo que lo ejecuta.
	 *
	 * @param controller controlador de la ventana de progreso, del que se toman la lista de
	 *                   eventos y la etiqueta de tiempo que el hilo va actualizando
	 */
	private void doBusinessLogic(EEGProtocolProgressController controller) {
		logger.debug("Starting Protocol Execution");
		// First open execution window
		stageProtocol = new Stage();
		applyIcon(stageProtocol);
		stageProtocol.setTitle("Ejecución del protocolo " + protocolName);
		if (showFullScreen) {
			stageProtocol.setFullScreen(true);
			stageProtocol.setFullScreenExitHint("");
		} else {
			stageProtocol.setFullScreen(false);
		}
		// stageProtocol.getScene().setCursor(Cursor.NONE);

		StackPane grandParentRoot = new StackPane();
		rootProtocol = new BorderPane();

		executer = new ProtocolThread(controller.list, events, medias, marks, estims, estNull,
				comEEG, comMatrix, comGlove, comMulti, controller.timeT, this);
		executer.addListener(this);

		if (comMatrix != null) {
			if (!comMatrix.isOpen()) {
				showErrorDialog(
						"El puerto de comunicación con la matriz está cerrado, apague y encienda la matriz.");
				stageProtocol.close();
				return;
			}
		}
		if (comGlove != null) {
			if (!comGlove.isOpen()) {
				showErrorDialog(
						"El puerto de comunicación con el guante está cerrado, apague y encienda la matriz.");
				stageProtocol.close();
				return;
			}
		}
		if (comMulti != null) {
			if (!comMulti.isOpen()) {
				showErrorDialog(
						"El puerto de comunicación con el multistimulador está cerrado, apague y encienda el dispositivo.");
				return;
			}
		}

		// Fondo negro del panel: evita que se vea el fondo por defecto de la escena
		// en las zonas que la imagen centrada no cubre.
		rootProtocol.setStyle("-fx-background-color: black;");

		if (initalImage != null)
			addImage(rootProtocol, initalImage, false);
		else
			addImage(rootProtocol, "inicio-experimento.png", true);

		// rootProtocol.setFocusTraversable(true);
		// rootProtocol.requestFocus();
		// rootProtocol.setOnKeyPressed(this);

		// Create the label for CLICKSTOP
		labelBorderPane = new BorderPane();
		label.setTextFill(Color.WHITE);
		label.setFont(new Font(30));
		label.setAlignment(Pos.CENTER);
		HBox hBox = new HBox(label);
		hBox.setAlignment(Pos.CENTER);
		labelBorderPane.setBottom(hBox);
		labelBorderPane.setVisible(false);

		grandParentRoot.getChildren().add(rootProtocol);
		grandParentRoot.getChildren().add(labelBorderPane);
		Scene scene = new Scene(grandParentRoot, 1024, 768);
		scene.setOnKeyPressed(e -> {
			logger.info("Evento : " + e.getCode());
			if (waitingForSpace && e.getCode() == KeyCode.SPACE) {
				waitingForSpace = false;
				labelBorderPane.setVisible(false);
				if (executer != null)
					executer.multimediaFlag = true;
			} else if (useSpaceAsKey && e.getCode() == KeyCode.SPACE) {
				if (useTarget) {
					if (correctStimulus)
						executer.sendMark(6);
					else
						executer.sendMark(7);
				} else
					executer.sendMark(8);
			} else if (e.getCode() == KeyCode.DIGIT1 || e.getCode() == KeyCode.Z)
				// RED BUTTON
				executer.sendMark(8);
			else if (e.getCode() == KeyCode.DIGIT2 || e.getCode() == KeyCode.M)
				// GREEN BUTTON
				executer.sendMark(9);
		});

		stageProtocol.setScene(scene);
		stageProtocol.setOnCloseRequest(ignored -> {
			/* mediaPlayer.stop(); */
			stageProtocol.hide();
			stageProtocol = null;
		});
		stageProtocol.setOnHidden(ignored -> {
			System.out.println("Trying to stop");
			if (executer != null) {
				executer.setStop(true);
				executer.checkForTimer();
			}
		});
		stageProtocol.setOnShown(ignored -> validateAndStartProtocol());

		try {
			stageProtocol.show();
			//			Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
			//			stageProtocol.setX((screenBounds.getWidth() - stageProtocol.getWidth()) / 2);
			//			stageProtocol.setY((screenBounds.getHeight() - stageProtocol.getHeight()) / 2);
		} catch (Exception e1) {
			logger.error("Error launching the protocol Stage: " + e1.getMessage());
		}

	}

	/**
	 * Validates that all pre-created VLCJ video players were initialized correctly.
	 * VLC is more resilient than JavaFX MediaPlayer — file existence was already
	 * validated in createMediaReference, so we just verify player creation succeeded.
	 */
	private void validateAndStartProtocol() {
		for (Map.Entry<String, MediaBean> entry : medias.entrySet()) {
			MediaBean mb = entry.getValue();
			if (mb.getMediaType() == MediaTypeEnum.VIDEO) {
				if (mb.getVlcPlayer() == null) {
					String errorMsg = "Error: no se pudo crear el reproductor VLCJ para '" + entry.getKey() + "'";
					logger.error(errorMsg);
					Platform.runLater(() -> {
						showErrorDialog(errorMsg);
						if (stageProtocol != null) {
							stageProtocol.hide();
							stageProtocol = null;
						}
					});
					return;
				}
			}
		}

		logger.info("All " + medias.values().stream().filter(m -> m.getMediaType() == MediaTypeEnum.VIDEO).count()
				+ " VLCJ video players validated and ready");

		Platform.runLater(this::showConfirmationAndStart);
	}

	private void showConfirmationAndStart() {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Confirmation Dialog");
		alert.setContentText("Pulse [OK] para iniciar la ejecución del protocolo.");
		applyIcon(alert);
		alert.setOnHidden(ignored -> {
			if (alert.getResult() == ButtonType.OK) {
				logger.info("OK to Start Protocol");
				executer.start();
			} else {
				if (stageProtocol != null) {
					stageProtocol.hide();
					stageProtocol = null;
				}
			}
		});
		alert.show();
	}

	@SuppressWarnings("unused")
	private boolean checkStimulusFile(File chosenFile) {
		boolean check = true;
		Scanner sc;
		try {
			sc = new Scanner(chosenFile);
		} catch (FileNotFoundException e1) {
			logger.error("No Stimulus File found after check, something messy happened", e1);
			return false;
		}

		List<String> lines = new ArrayList<>();
		while (sc.hasNextLine()) {
			lines.add(sc.nextLine());
		}

		String[] arr = lines.toArray(new String[0]);
		for (int i = 0; i < arr.length; i++) {
			arr[i] = arr[i].toUpperCase();
			if (arr[i].isEmpty())
				continue;
			if (arr[i].charAt(0) == ';')
				continue;

			if (arr[i].indexOf("DIM:") == 0) {
				try {
					dimension = Integer.parseInt(arr[i].substring(4).trim());
					logger.debug(
							"Dimensi�n del est�mulo: " + dimension + "x" + dimension + " puntos");
					continue;
				} catch (NumberFormatException e) {
					check = false;
					logger.error("Dimensi�n: Error en l�nea " + i + " del fichero de est�mulos: "
							+ arr[i].charAt(4) + " no es un entero");
					break;
				}
			}

			if (arr[i].indexOf("EST") == 0) {
				int t;
				try {
					t = Integer.parseInt(
							arr[i].substring(arr[i].indexOf('_') + 1, arr[i].indexOf(':')).trim());
				} catch (NumberFormatException e) {
					check = false;
					logger.error("Error en l�nea " + i + " del fichero de est�mulos:_" + arr[i]
							.substring(arr[i].indexOf('_') + 1, arr[i].indexOf(':')).trim()
							+ "_no es un entero");
					break;
				}
				if (t != 0)
					estims.add(new EstimulusBean(t, dimension,
							arr[i].substring(arr[i].indexOf(':') + 1).trim()));
				else
					estNull = new EstimulusBean(t, dimension,
							arr[i].substring(arr[i].indexOf(':') + 1).trim());
			}
		}
		sc.close();
		return check;

	}

	@SuppressWarnings("unused")
	private File fileStimulusLoad() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		fileChooser.setInitialDirectory(
				new File(EEGControl.BASE_FILE + EEGControl.STIMULUS_FILE_BASE));
		File chosenFile = fileChooser.showOpenDialog(primaryStage);
		if (chosenFile == null) {
			logger.warn("Programa terminado. Ning�n fichero de est�mulos abierto");
			Platform.exit();
		}
		return chosenFile;
	}

	private boolean checkProtocolFile(File p) {
		events = new ArrayList<>();
		estims = new ArrayList<>();
		// Cargar un protocolo tiene que bastarse a sí mismo: si no se limpia aquí, el
		// contenido multimedia del protocolo anterior sobrevive y validateAndStartProtocol()
		// falla sobre ficheros que este protocolo ni siquiera referencia. No basta con
		// limpiar en doClean(), porque un protocolo interrumpido puede no llegar a él.
		resetMedias();
		protocolGeneration++;

		boolean check = true;
		Scanner sc;
		try {
			sc = new Scanner(p);
		} catch (FileNotFoundException e1) {
			logger.error("No Protocol File found after check, something messy happened", e1);
			return false;
		}
		List<String> lines = new ArrayList<>();
		ProtocolBean myBean = new ProtocolBean();
		myBean.setDateExecution(ProtocolBean.getDateString(new Date()));
		myBean.setDescription(sc.nextLine());
		myBean.setFileName(p.getName());
		myBean.setStudyBean(getStudyBean());

		while (sc.hasNextLine()) {
			lines.add(sc.nextLine().trim());
		}

		String[] arr = lines.toArray(new String[0]);
		for (int i = 0; i < arr.length; i++) {
			String anal = arr[i].toUpperCase();
			// COMMENTS WITH ; OR #, also ignore empty lines
			if (arr[i].isEmpty() || arr[i].charAt(0) == ';' || arr[i].charAt(0) == '#')
				continue;
			// CONFIGURATION OF THE PROTOCOL
			else if (anal.indexOf("FULLSCREEN") == 0) {
				try {
					this.showFullScreen = ProtocolUtils.trueFalseLine(arr[i]);
				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
					check = false;
				}
			} else if (anal.indexOf("CENTER_MOUSE") == 0) {
				try {
					EEGControl.centerMouse = ProtocolUtils.trueFalseLine(arr[i]);
				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
					check = false;
				}
			} else if (anal.indexOf("USE_SPACEKEY") == 0) {
				try {
					EEGControl.useSpaceAsKey = ProtocolUtils.trueFalseLine(arr[i]);
				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
					check = false;
				}
			} else if (anal.indexOf("USE_TARGET") == 0) {
				try {
					EEGControl.useTarget = ProtocolUtils.trueFalseLine(arr[i]);
				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
					check = false;
				}
			} else if (anal.indexOf("KGS_VIBRATE") == 0) {
				try {
					EEGControl.kgsVibrate = ProtocolUtils.trueFalseLine(arr[i]);
				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
					check = false;
				}
			} else if (anal.indexOf("USE_MATRIX") == 0) {
				try {
					EEGControl.useMatrixProtocol = ProtocolUtils.trueFalseLine(arr[i]);
				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
					check = false;
				}
			} else if (anal.indexOf("USE_EEG") == 0) {
				try {
					EEGControl.useEEGProtocol = ProtocolUtils.trueFalseLine(arr[i]);
				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
					check = false;
				}
			} else if (anal.indexOf("USE_GLOVE") == 0) {
				try {
					EEGControl.useGloveProtocol = ProtocolUtils.trueFalseLine(arr[i]);
				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
					check = false;
				}
			} else if (anal.indexOf("USE_MULTI") == 0) {
				try {
					EEGControl.useMultiStimulator = ProtocolUtils.trueFalseLine(arr[i]);
				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
					check = false;
				}
			}

			// TEST COMMANDS
			else if (anal.indexOf("LANZAR") == 0) {
				check = createMultimediaEvent(arr[i], i, EventEnum.LANZAR, MediaTypeEnum.VIDEO, sc);
			} else if (anal.indexOf("INICIAR") == 0) {
				try {
					String[] data = ProtocolUtils.tokenize(arr[i]);
					String fileName = null;
					if (data.length > 1)
						fileName = data[1];
					EventBean lanzarEvent = new EventBean(EventEnum.INICIAR, fileName, i+1);

					if (fileName != null) {
						String mediaIniciar =
								EEGControl.BASE_FILE + EEGControl.IMAGE_RESOURCES_FILE_BASE
										+ lanzarEvent.getFile();

						File file = new File(mediaIniciar);
						if (file.exists()) {
							initalImage = lanzarEvent.getFile();
							events.add(lanzarEvent);
						} else {
							showErrorDialog("No se encuentra la imagen " + lanzarEvent.getFile());
							sc.close();
							return false;
						}
					} else {
						events.add(lanzarEvent);
					}
				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
					check = false;
				}
			} else if (anal.indexOf("TARGET") == 0) {
				try {
					EventBean lanzarEvent = new EventBean(EventEnum.TARGET, 0, i+1);
					events.add(lanzarEvent);
				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
					check = false;
				}
			} else if (anal.indexOf("FAIL") == 0) {
				try {
					EventBean lanzarEvent = new EventBean(EventEnum.FAIL, 0, i+1);
					events.add(lanzarEvent);
				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
					check = false;
				}
			} else if (anal.indexOf("CLICKSTOP") == 0) {
				try {
					EventBean lanzarEvent = new EventBean(EventEnum.CLICKSTOP, 0, i+1);
					events.add(lanzarEvent);
				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
					check = false;
				}
			} else if (anal.indexOf("SPACESTOP") == 0) {
				try {
					EventBean lanzarEvent = new EventBean(EventEnum.SPACESTOP, 0, i+1);
					events.add(lanzarEvent);
				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
					check = false;
				}
			} else if (anal.indexOf("ESTIM_OLD") == 0 || anal.indexOf("KGS") == 0) {
				try {
					String[] data = ProtocolUtils.tokenize(arr[i]);
					String fileName = null;
					if (data.length > 1)
						fileName = data[1];
					EventBean lanzarEvent = new EventBean(EventEnum.ESTIM_OLD, fileName, i+1);

					if (fileName != null) {
						if (!fileName.contains(".")) {
							fileName = fileName + ".bmp";
							lanzarEvent.setFile(fileName);
						}

						String mediaIniciar = EEGControl.BASE_FILE + EEGControl.MULTIMEDIA_FILE_BASE
								+ lanzarEvent.getFile();

						File file = new File(mediaIniciar);
						if (file.exists()) {
							Image image = new Image(file.toURI().toString(),
									48, 32, false, false);
							lanzarEvent.setImg(image);
							events.add(lanzarEvent);
							continue;
						} else {
							showErrorDialog("No se encuentra la imagen " + lanzarEvent.getFile());
							sc.close();
							return false;
						}
					} else {
						// TODO: There is nothing to show. Include black image???

						events.add(lanzarEvent);
						continue;
					}
				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage(), e);
					check = false;
				}
			} else if (anal.indexOf("MOSTRAR") == 0) {
				check = createMultimediaEvent(arr[i], i, EventEnum.MOSTRAR, MediaTypeEnum.IMAGE, sc);
			} else if (anal.indexOf("SONAR") == 0) {
				check = createMultimediaEvent(arr[i], i, EventEnum.SONAR, MediaTypeEnum.SOUND, sc);
			} else if (anal.indexOf(EventEnum.MULTI.getCode()) == 0){
				check = createMultimediaEvent(arr[i], i, EventEnum.MULTI, MediaTypeEnum.IMAGE, sc);

			} else if (arr[i].indexOf("MARCAR") == 0) {
				try {
					int t = Integer.parseInt(arr[i].substring(7).trim());
					events.add(new EventBean(EventEnum.MARCAR, t, i+1));
					storeMark(t);
					continue;
				} catch (NumberFormatException e) {
					logger.error(
							"Error en l�nea " + i + " del protocolo: " + arr[i].substring(7).trim()
									+ " no es un entero");
					check = false;
				}
			} else if (anal.indexOf("ESPERAR_VIDEO") == 0) {
				events.add(new EventBean(EventEnum.ESPERAR_VIDEO, 0, i+1));
			} else if (anal.indexOf("PARAR_VIDEO") == 0) {
				events.add(new EventBean(EventEnum.PARAR_VIDEO, 0, i+1));
			} else if (anal.indexOf("ESPERAR_AUDIO") == 0) {
				events.add(new EventBean(EventEnum.ESPERAR_AUDIO, 0, i+1));
			} else if (anal.indexOf("PARAR_AUDIO") == 0) {
				events.add(new EventBean(EventEnum.PARAR_AUDIO, 0, i+1));
			} else if (arr[i].indexOf("ESPERAR") == 0) {
				try {
					int t = Integer.parseInt(arr[i].substring(8).trim());

					if(multiStimulationMillisPlaying > 0 && t < multiStimulationMillisPlaying)
					{
						String error = "Error en línea " + (i + 1) + " del protocolo: " + arr[i] +
								"El tiempo de espera definido "+arr[i].substring(7).trim()+
								" es menor que tiempo de ejecución del multiestimulador ("+
								multiStimulationMillisPlaying+").";
						showErrorDialog(error);
						sc.close();
						check = false;
					}
					// Really if multiStimulationMillisPlaying is 0, it means that the multiestimulator is not used
					// but if in use, we have to take into account that its waiting time will be used
					// so, the real time to wait is the time defined in the protocol minus the time of the multiestimulator
					t = t - multiStimulationMillisPlaying;
					// Now reset the time of the multiestimulator
					multiStimulationMillisPlaying =0;

					events.add(new EventBean(EventEnum.ESPERAR, t, i+1));
				} catch (NumberFormatException e) {
					showErrorDialog("Error en l�nea " + i + " del protocolo: " + arr[i].substring(7).trim()
									+ " no es un entero");
					sc.close();
					check = false;
				}
			} else if (anal.indexOf("VIBRAR") == 0) {
				try {
					String params = arr[i].substring(7).trim();
					EventBean lanzarEvent = new EventBean(EventEnum.VIBRAR, params, i+1);
					events.add(lanzarEvent);

				} catch (Exception e) {
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
					check = false;
				}
			} else if (arr[i].indexOf("TERMINAR") == 0) {
				try {
					events.add(new EventBean(EventEnum.TERMINAR, 0, i+1));
					continue;
				} catch (NumberFormatException e) {
					logger.error(
							"Error en l�nea " + i + " del protocolo: " + arr[i].substring(7).trim()
									+ " no es un entero");
					check = false;
				}
			} else if (arr[i].indexOf("TACTIL") == 0) {
				events.add(new EventBean(EventEnum.TACTIL, 0, i+1));
				continue;
			}
			if (!check) break;
		}
		sc.close();
		setProtocolBean(myBean);
		Logger loggerProtocol = LogManager.getLogger("Protocol");
		loggerProtocol.info(getProtocolBean().toString());
		Collections.sort(marks);
		return check;
	}

	private void storeMark(int t) {
		if (!marks.contains(t))
			marks.add(t);
	}

	public void loadCameras() {
		try {
			// load the FXML resource
			FXMLLoader loader = new FXMLLoader(getClass().getResource("CameraControl.fxml"));
			// store the root element so that the controllers can use it
			BorderPane rootElement1 = loader.load();
			// create and style a scene
			Stage primaryStage1 = new Stage();
			applyIcon(primaryStage1);
			Scene scene = new Scene(rootElement1, 600, 600);
			applyStylesheet(scene);
			// create the stage with the given title and the previously created
			// scene
			primaryStage1.setTitle("Canny Imager for KGS");
			primaryStage1.setScene(scene);
			// show the GUI
			primaryStage1.show();

			// set the proper behavior on closing the application
			CameraController controller = loader.getController();
			primaryStage1.setOnCloseRequest(ignored -> controller.setClosed());
		} catch (Exception e) {
			logger.error("Error al abrir la ventana de cámara", e);
		}
	}

	@Override
	public void notifyOfThreadComplete(Thread thread) {
		final int generation = protocolGeneration;
		Platform.runLater(() -> {
			// Si mientras tanto se ha cargado otro protocolo, este cierre ya no le
			// corresponde: pintar aquí la imagen de fin la mostraría sobre la ventana del
			// protocolo nuevo.
			if (generation != protocolGeneration) {
				logger.debug("El protocolo ha sido reemplazado; se omite su cierre");
				return;
			}
			addImage(rootProtocol, "fin-experimento.png", true);
			// La espera se hacía con Thread.sleep sobre el hilo de JavaFX, que lo bloqueaba:
			// la imagen de fin no llegaba a pintarse hasta que terminaba la espera, porque el
			// hilo no volvía al bucle de render. PauseTransition espera sin bloquearlo.
			PauseTransition endWait = new PauseTransition(
					Duration.millis(EEGControl.END_PROTOCOL_WAIT_MILIS));
			endWait.setOnFinished(ignored -> closeProtocolWindow(generation));
			endWait.play();
		});
	}

	/**
	 * Cierra la ventana de ejecución y limpia el estado. La limpieza va en un finally para
	 * que se ejecute aunque el cierre falle: si no, el siguiente protocolo arrancaría con el
	 * contenido multimedia del anterior todavía en memoria.
	 */
	private void closeProtocolWindow(int generation) {
		try {
			if (rootProtocol != null && rootProtocol.getScene() != null
					&& rootProtocol.getScene().getWindow() != null)
				rootProtocol.getScene().getWindow().hide();
		} catch (Exception e) {
			logger.warn("Error cerrando la ventana de ejecución del protocolo", e);
		} finally {
			if (generation == protocolGeneration)
				doClean();
			else
				logger.debug("Se ha cargado otro protocolo durante la espera de fin; "
						+ "se omite doClean() para no borrar su estado");
		}
	}

	private void doClean() {
		rootProtocol = null;
		stageProtocol = null;
		protocolController = null;
		events = new ArrayList<>();
		resetMedias();
		estims = new ArrayList<>();
		initalImage = null;
		System.gc();
	}

	/**
	 * Libera los reproductores de vídeo y vacía la caché de contenido multimedia. Se llama
	 * tanto al cargar un protocolo como al terminarlo, para que ninguna de las dos vías
	 * dependa de que la otra se haya ejecutado.
	 */
	private void resetMedias() {
		releaseVideoPlayers();
		medias = new HashMap<>();
	}

	private void releaseVideoPlayers() {
		for (MediaBean mb : medias.values()) {
			if (mb.getMediaType() == MediaTypeEnum.VIDEO && mb.getVlcPlayer() != null) {
				mb.getVlcPlayer().release();
				mb.setVlcPlayer(null);
			}
		}
	}

	@Override
	public void stop() {
		releaseVideoPlayers();
		if (mediaPlayerFactory != null) {
			mediaPlayerFactory.release();
			mediaPlayerFactory = null;
		}
	}

	@Override
	public void notifyEvent(Thread thread, EventBean event) {
		logger.debug("Evento de Thread " + event.getTipo());

	}

	@Override
	public void handle(KeyEvent event) {
		System.out.println("Tecla pulsada: #" + event.getCode() + "#");
	}

	public static void addImage(BorderPane pane, String fileName, boolean isResource) {
		String imageStr;

		if (isResource)
			imageStr = EEGControl.BASE_FILE + EEGControl.IMAGE_RESOURCES_FILE_BASE + fileName;
		else
			imageStr = EEGControl.BASE_FILE + EEGControl.MULTIMEDIA_FILE_BASE + fileName;

		// System.out.println(imageStr);
		File file = new File(imageStr);
		if (!file.exists()) {
			logger.error("No se encuentra la imagen " + imageStr);
			return;
		}

		ImageView imageView = new ImageView(file.toURI().toString());

		EEGControl.addImage(pane, imageView);

	}

	public static void addImage(BorderPane pane, Image imageToShow) {
		if (imageToShow == null) {
			logger.error("No hay imagen que mostrar");
			return;
		}
		logger.debug("Showing image " + imageToShow.hashCode());

		EEGControl.addImage(pane, new ImageView(imageToShow));
	}

	private static void addImage(BorderPane pane, ImageView imageView) {
		if (pane != null) {
			pane.getChildren().removeAll();

			if (imageView != null) {
				VBox mvPane = new VBox();
				mvPane.getChildren().add(imageView);
				mvPane.setStyle("-fx-background-color: black;");
				mvPane.setAlignment(Pos.CENTER);
				pane.setCenter(mvPane);
			}
		}

	}

	public void reloadLoggers(String[] names) {
		System.setProperty("studyBaseDir", EEGControl.STUDY_BASE_DIR);
		System.setProperty("studyNumber", names[0]);
		System.setProperty("protocolNumber", names[1]);
		org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(
				false);
		ctx.reconfigure();
	}

	public void showClickLabel(boolean isClick) {
		if (isClick) {
			label.setText("PULSA EL RATON PARA CONTINUAR");
			labelBorderPane.getScene().setOnMouseReleased(ignored -> {
				labelBorderPane.setVisible(false);
				if (executer != null)
					executer.multimediaFlag = true;
			});
		} else {
			waitingForSpace = true;
			label.setText("PULSA LA TECLA ESPACIO PARA CONTINUAR");
		}
		labelBorderPane.setVisible(true);
	}

	public static void main(String[] args) {
		launch(args);
	}

	public StudyBean getStudyBean() {
		return studyBean;
	}

	public void setStudyBean(StudyBean studyBean) {
		this.studyBean = studyBean;
	}

	public ProtocolBean getProtocolBean() {
		return protocolBean;
	}

	public void setProtocolBean(ProtocolBean protocolBean) {
		this.protocolBean = protocolBean;
	}

	public BorderPane getRootProtocol() {
		return rootProtocol;
	}

	private boolean checkMediaReference(String mediaReference) {
		return medias.get(mediaReference) == null;
	}

	/**
	 * Creates a MediaBean of type MediaTypeEnum with the appropriate Media element as Primary, if
	 * the MediaBean contains an image, is the imageFile, if contains a sound, is the soundFile (and
	 * also the fileNameImage for the image to show meanwhile the sound is playing)
	 *
	 * @param fileNamePrimary nombre del fichero principal (imagen, sonido o vídeo); si no lleva
	 *                        extensión se le añade la propia del tipo de media
	 * @param fileNameImage   nombre del fichero de imagen a mostrar mientras suena el sonido,
	 *                        sólo se usa con SOUND_IMAGE, null en el resto de casos
	 * @param mediaTypeEnum   tipo de media a crear: IMAGE, SOUND, SOUND_IMAGE o VIDEO
	 * @return false si el fichero no existe o no se puede crear el reproductor
	 */
	private boolean createMediaReference(String fileNamePrimary, String fileNameImage,
			MediaTypeEnum mediaTypeEnum) {
		// First check file extension
		switch(mediaTypeEnum) {
			case IMAGE: {
				if (!fileNamePrimary.contains("."))
					fileNamePrimary = fileNamePrimary + ".bmp";
				break;
			}
			case SOUND: {
				if (!fileNamePrimary.contains("."))
					fileNamePrimary = fileNamePrimary + ".wav";
				break;
			}
			case SOUND_IMAGE: {
				if (fileNameImage == null) {
					showErrorDialog("Error interno: se ha pedido sonido con imagen de fondo "
							+ "para " + fileNamePrimary + " sin indicar la imagen.");
					return false;
				}
				if (!fileNamePrimary.contains("."))
					fileNamePrimary = fileNamePrimary + ".wav";
				if (!fileNameImage.contains("."))
					fileNameImage = fileNameImage + ".bmp";
				break;
			}
			case VIDEO: {
				if (!fileNamePrimary.contains("."))
					fileNamePrimary = fileNamePrimary + ".mp4";
				break;
			}
		}

		String fileNamePrimaryAbs =
				EEGControl.BASE_FILE + EEGControl.MULTIMEDIA_FILE_BASE + fileNamePrimary;
		File file = new File(fileNamePrimaryAbs);
		if (!file.exists()) {
			showErrorDialog("No se encuentra el fichero " + fileNamePrimary + " de tipo "
					+ mediaTypeEnum.getDescription());
			return false;
		}
		switch (mediaTypeEnum) {
			case SOUND: {
				Media mediaSound = new Media(file.toURI().toString());
				MediaBean mediaBean = new MediaBean(mediaSound, null, mediaTypeEnum);
				medias.put(fileNamePrimary, mediaBean);
				break;
			}
			case SOUND_IMAGE: {
				Media mediaSound = new Media(file.toURI().toString());
				String fileNameImageAbs =
						EEGControl.BASE_FILE + EEGControl.MULTIMEDIA_FILE_BASE + fileNameImage;
				File file1 = new File(fileNameImageAbs);
				if (!file1.exists()) {
					showErrorDialog("No se encuentra el fichero " + fileNameImage
							+ " de tipo imagen para el sonido" + fileNamePrimary);
					return false;
				}
				Image mediaImage = new Image(file1.toURI().toString());
				MediaBean mediaBean = new MediaBean(mediaSound, mediaImage, mediaTypeEnum);
				medias.put(fileNamePrimary+"_"+fileNameImage, mediaBean);
				break;
			}
			case IMAGE: {
				Image mediaImage = new Image(file.toURI().toString());
				MediaBean mediaBean = new MediaBean(mediaImage, mediaTypeEnum);
				medias.put(fileNamePrimary, mediaBean);
				break;
			}
			case VIDEO: {
				if (mediaPlayerFactory == null) {
					showErrorDialog("VLCJ no inicializado. ¿Está VLC instalado?");
					return false;
				}
				String videoPath = file.getAbsolutePath();
				MediaBean mediaBean = new MediaBean(videoPath, mediaTypeEnum);
				EmbeddedMediaPlayer vlcPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
				ImageView imageView = new ImageView();
				imageView.setPreserveRatio(true);
				vlcPlayer.videoSurface().set(new ImageViewVideoSurface(imageView));
				mediaBean.setVlcPlayer(vlcPlayer);
				mediaBean.setVideoImageView(imageView);
				logger.info("Pre-created VLCJ player for video: " + fileNamePrimary);
				medias.put(fileNamePrimary, mediaBean);
				break;
			}
		}
		return true;
	}

	/**
	 * Creates a MultimediaEventBean of type eventType with the appropriate MediaBean as Primary,
	 * if the MultimediaEventBean contains an image, is the imageFile, if contains a sound, is the
	 * soundFile (and also the fileNameImage for the image to show meanwhile the sound is playing)
	 *
	 * @param line, The whole line of the protocol file
	 * @param line_num, The line number of the protocol file
	 * @param eventType, The type of event to create
	 * @param mediaTypeEnum, The type of media depending on the event, can be IMAGE, SOUND, SOUND_IMAGE or VIDEO. In case of SOUND, the fileNameImage is evaluated and the mediaTypeEnum can be chaged to SOUND_IMAGE
	 * @param sc, The protocol file scanner, closed here when an error aborts the parsing
	 * @return false if an error is found
	 */
	private boolean createMultimediaEvent(String line, int line_num,  EventEnum eventType,
			MediaTypeEnum mediaTypeEnum, Scanner sc) {
		try {
			// tokenize() respeta las comillas: un nombre con espacios llega entero.
			String[] data = ProtocolUtils.tokenize(line);
			if (data.length > 1) {
				String fileName = data[1];
				switch (mediaTypeEnum) {
					case VIDEO: {
						fileName = fileName.contains(".") ? fileName : fileName + ".mp4";
						break;
					}
					case IMAGE: {
						fileName = fileName.contains(".") ? fileName : fileName + ".bmp";
						break;
					}
					case SOUND:
					case SOUND_IMAGE: {
						fileName = fileName.contains(".") ? fileName : fileName + ".wav";
						break;
					}

				}

				EventBean multimediaEvent = new EventBean(eventType, fileName, line_num+1);
				String mediaReference = fileName;
				String fileNameSecondary = null;

				// Solo SONAR admite una imagen de fondo como segundo argumento. Antes esto se
				// deducía de cuántos trozos daba el split, así que un nombre con espacios, o el
				// tiempo de espera de MULTI, activaban SOUND_IMAGE por error.
				if (eventType == EventEnum.SONAR && data.length > 2) {
					fileNameSecondary = data[2];
					if (!fileNameSecondary.contains(".")) {
						fileNameSecondary = fileNameSecondary + ".bmp";
					}
					mediaReference = fileName + "_" + fileNameSecondary;
					mediaTypeEnum = MediaTypeEnum.SOUND_IMAGE;
				}

				// Se comprueba la referencia real bajo la que se indexa el media, no el nombre
				// del fichero: con SOUND_IMAGE la clave es "sonido_imagen".
				if (checkMediaReference(mediaReference)) {
					if (!createMediaReference(fileName, fileNameSecondary, mediaTypeEnum)) {
						sc.close();
						return false;
					}
				}
				multimediaEvent.setMediaReference(mediaReference);
				if(eventType == EventEnum.MULTI){
					int waitSecs = multistimulatorWaitStimImageInMillis;

					if(data.length > 2){
						try{
							waitSecs = Integer.parseInt(data[2]);
						}catch(NumberFormatException e){
							showErrorDialog("Error en línea " + (line_num + 1) + " del protocolo: " + line.trim()
									+ ", No se puede parsear el tiempo de espera indicado en segundos, debe ser un entero. ");
							sc.close();
							return false;
						}
					}
					multiStimulationMillisPlaying = waitSecs;
					multimediaEvent.setLength(waitSecs);
				}
				events.add(multimediaEvent);
				return true;
			} else {
				String errorMsg =
						"Error en línea " + (line_num + 1) + " del protocolo: " + line.trim()
								+ ", No hay fichero de "+mediaTypeEnum.getDescription()+" indicado. ";
				showErrorDialog(errorMsg);
				sc.close();
				return false;
			}
		} catch (Exception e) {
			// A nivel debug esto era invisible: el protocolo se rechazaba y la aplicación
			// volvía al menú sin decir nada. Se registra como error y se avisa al operador.
			String errorMsg = "Error en línea " + (line_num + 1) + " del protocolo: "
					+ line.trim() + ", Error: " + e.getMessage();
			logger.error(errorMsg, e);
			showErrorDialog(errorMsg);
			sc.close();
			return false;
		}
	}

	/**
	 * Aplica la hoja de estilos de la aplicación a una escena, avisando si el recurso
	 * no está disponible en el classpath en lugar de fallar con NullPointerException.
	 *
	 * @param scene la escena a la que aplicar los estilos
	 */
	private void applyStylesheet(Scene scene) {
		URL css = getClass().getResource("application.css");
		if (css != null)
			scene.getStylesheets().add(css.toExternalForm());
		else
			logger.warn("No se encuentra application.css en el classpath");
	}

	/**
	 * Carga una sola vez el juego de iconos de la aplicación desde el classpath. Se cargan
	 * varios tamaños porque JavaFX elige el más adecuado según el contexto: la barra de
	 * título, la barra de tareas y el conmutador Alt+Tab usan resoluciones distintas.
	 * Los tamaños que falten se omiten con un aviso, sin impedir el arranque.
	 *
	 * @return los iconos disponibles, lista vacía si no se ha podido cargar ninguno
	 */
	private static List<Image> getAppIcons() {
		if (appIcons == null) {
			appIcons = new ArrayList<>();
			for (int size : ICON_SIZES) {
				String path = ICON_BASE + size + ".png";
				try (InputStream is = EEGControl.class.getResourceAsStream(path)) {
					if (is != null)
						appIcons.add(new Image(is));
					else
						logger.warn("No se encuentra el icono " + path + " en el classpath");
				} catch (IOException e) {
					logger.warn("No se ha podido leer el icono " + path, e);
				}
			}
			if (appIcons.isEmpty())
				logger.error("No se ha cargado ningún icono de la aplicación; las ventanas "
						+ "mostrarán el icono por defecto de Java");
		}
		return appIcons;
	}

	/**
	 * Aplica el icono de la aplicación a una ventana.
	 *
	 * @param stage la ventana a la que aplicar el icono
	 */
	public static void applyIcon(Stage stage) {
		if (stage != null)
			stage.getIcons().addAll(getAppIcons());
	}

	/**
	 * Aplica el icono de la aplicación a un diálogo. Los Alert tienen su propia ventana,
	 * que de otro modo seguiría mostrando el icono por defecto de Java.
	 *
	 * @param alert el diálogo al que aplicar el icono
	 */
	public static void applyIcon(Alert alert) {
		Scene scene = alert.getDialogPane().getScene();
		if (scene != null && scene.getWindow() instanceof Stage stage)
			applyIcon(stage);
	}

	private void showErrorDialog(String message) {
		logger.error(message);
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error");
		alert.setContentText(message);
		applyIcon(alert);
		try {
			alert.showAndWait();
		} catch (IllegalStateException e) {
			logger.warn("showAndWait not allowed in current context, using non-blocking show()");
			alert.show();
		}
	}
}
