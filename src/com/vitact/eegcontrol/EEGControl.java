package com.vitact.eegcontrol;

import com.fazecast.jSerialComm.SerialPort;
import com.vitact.eegcontrol.bean.*;
import com.vitact.eegcontrol.opencv.OpenCVTransform;
import com.vitact.eegcontrol.type.*;
import com.vitact.eegcontrol.utils.ProtocolUtils;
import java.io.*;
import java.util.*;
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
import javafx.scene.media.Media;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.*;
import javax.swing.*;
import org.apache.logging.log4j.*;
import org.opencv.core.Core;

public class EEGControl extends Application
		implements ThreadCompleteListener, EventHandler<KeyEvent> {
	static Logger logger = null;
	private final int EEG_BAUDRATE = 1200;
	private final int MATRIX_BAUDRATE = 115200; // 9600;
	private final int MULTI_BAUDRATE = 9600; // 9600;
	public static final long STIMULUS_TIME_MILIS = 3000;
	public static final long END_PROTOCOL_WAIT_MILIS = 5000;
	public static final long MULTIMEDIA_TIMEOUT = 15000;
	public static final long OLD_STIM_VIBRATION_MILIS = 33;

	public static final String MULTIMEDIA_FILE_BASE = "default/Multimedia/";
	public static final String PROTOCOL_FILE_BASE = "default/protoc/";
	public static final String STIMULUS_FILE_BASE = "default/stim/";
	public static final String IMAGE_RESOURCES_FILE_BASE = "bin/resources/images/";
	public static final String CONFIG_FILE = "default/properties.ini";
	public static final String STUDY_BASE_DIR = "estudios";
	public static String BASE_FILE = "c://";
	public static final Boolean USE_FULL_STUDY_DATA = false;
	public static final String DEFAULT_VERSION="4.0";

	ArrayList<EventBean> events = new ArrayList<EventBean>();
	HashMap<String, MediaBean> medias = new HashMap<String, MediaBean>();
	ArrayList<EstimulusBean> estims = new ArrayList<EstimulusBean>();
	EstimulusBean estNull;
	boolean off = true;
	JList<String> list;
	long initTime;
	List<Integer> marks = new ArrayList<Integer>();
	int estimMatrix = 0;// 0 significa que no hay conexi�n ocn matrix
	int dimension;
	boolean oldProtocol = false;

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
	Label label = new Label("PULSA EL RATON PARA CONTINUAR");
	int multiStimulationMillisPlaying = 0;

	public static boolean USE_MEDIABEAN = true;

	public static Properties properties = new Properties();

		public EEGControl() {
		String names[] = {"0000", "02"};
		reloadLoggers(names);
		logger = LogManager.getLogger(this.getClass().getName());

		EEGControl.BASE_FILE = System.getProperty("user.dir") + "/";
		logger.info("BAse dir " + EEGControl.BASE_FILE);

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
			BorderPane root = (BorderPane) loader.load();
			mainController = ((EEGViewController) loader.getController());
			mainController.setPadre(this);
			Scene scene = new Scene(root, 500, 500);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setOnCloseRequest(e -> Platform.exit());
			primaryStage.show();
			this.primaryStage = primaryStage;
		} catch (Exception e) {
			logger.error("Error while loading main scene", e);
			Platform.exit();
		}
	}

	public void fileProtocolLoaded(File chosenFile) {
		try {
			protocolName = chosenFile.getName();
		} catch (Exception e) {
			// NO he seleccionado ningún protocolo, me salgo al menú principal
			return;
		}
		logger.debug("Protocolo seleccionado: " + chosenFile.getAbsolutePath());

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

	private void loadPorts() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("EEGPortsControl.fxml"));
			GridPane root = (GridPane) loader.load();
			EEGPortsViewController controller = ((EEGPortsViewController) loader.getController());
			controller.setPadre(this);
			Scene scene = new Scene(root, 500, 265);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			Stage stage = new Stage();
			stage.setTitle("Configuración de Puertos");

			stage.setScene(scene);
			stage.show();
			stage.setOnHidden(e -> {
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
			BorderPane root = (BorderPane) loader.load();
			protocolController = ((EEGProtocolProgressController) loader.getController());
			protocolController.setEvents(events);
			Scene scene = new Scene(root, 400, 400);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			Stage stage = new Stage();
			stage.setTitle("Progreso de la prueba");

			if (showProtocolEvolWindow) {

				stage.setScene(scene);
				stage.setOnShown(e -> {
					doBusinessLogic(protocolController);
				});
				stage.setOnHidden(e -> {
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
	 * @param controller
	 */
	private void doBusinessLogic(EEGProtocolProgressController controller) {
		logger.debug("Starting Protocol Execution");
		// First open execution window
		stageProtocol = new Stage();
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

		// ImageView imageView = new
		// ImageView("file:///Users/eiglesia/Temp/lord-jerome.jpg");
		String imageName = "file://" + BASE_FILE + "bin/resources/images/inicio-experimento.png";
		ImageView imageView = new ImageView(imageName);
		rootProtocol.setStyle("-fx-background-color: black;");
		rootProtocol.setCenter(imageView);

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
		stageProtocol.setOnCloseRequest(e -> {
			/* mediaPlayer.stop(); */
			stageProtocol.hide();
			stageProtocol = null;
		});
		stageProtocol.setOnHidden(e -> {
			System.out.println("Trying to stop");
			if (executer != null) {
				executer.setStop(true);
				executer.checkForTimer();
			}
		});
		stageProtocol.setOnShown(e -> {

			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Confirmation Dialog");
			alert.setContentText("Pulse [OK] para iniciar la ejecución del protocolo.");

			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK) {
				logger.info("OK to Start Protocol");
				alert.close();
				executer.start();
			} else {
				stageProtocol.hide();
				stageProtocol = null;
			}
		});

		try {
			stageProtocol.show();
			//			Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
			//			stageProtocol.setX((screenBounds.getWidth() - stageProtocol.getWidth()) / 2);
			//			stageProtocol.setY((screenBounds.getHeight() - stageProtocol.getHeight()) / 2);
		} catch (Exception e1) {
			logger.error("Error launching the protocol Stage: " + e1.getMessage());
		}

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

		List<String> lines = new ArrayList<String>();
		while (sc.hasNextLine()) {
			lines.add(sc.nextLine());
		}

		String[] arr = lines.toArray(new String[0]);
		for (int i = 0; i < arr.length; i++) {
			arr[i] = arr[i].toUpperCase();
			if (arr[i].length() == 0)
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
							+ arr[i].substring(4, 5) + " no es un entero");
					break;
				}
			}

			if (arr[i].indexOf("EST") == 0) {
				int t = 0;
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
		events = new ArrayList<EventBean>();
		estims = new ArrayList<EstimulusBean>();

		boolean check = true;
		Scanner sc;
		try {
			sc = new Scanner(p);
		} catch (FileNotFoundException e1) {
			logger.error("No Protocol File found after check, something messy happened", e1);
			return false;
		}
		List<String> lines = new ArrayList<String>();
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
			if (arr[i].length() == 0 || arr[i].charAt(0) == ';' || arr[i].charAt(0) == '#')
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
					EEGControl.useMatrixProtocol = ProtocolUtils.trueFalseLine(arr[i]);;
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
					String data[] = arr[i].split("\\s");
					String fileName = null;
					if (data.length > 1)
						fileName = data[1].replace("\"", "");
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
					String data[] = arr[i].split("\\s");
					String fileName = null;
					if (data.length > 1)
						fileName = data[1].replace("\"", "");
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
							@SuppressWarnings("unused")
							OpenCVTransform transform = new OpenCVTransform();
							Image image = new Image(file.toURI().toString(),
									OpenCVTransform.OLD_STIM_VIDEO_WIDTH,
									OpenCVTransform.OLD_STIM_VIDEO_HEIGHT, false, false);
							//							lanzarEvent.setImg(transform.getEdgesOpenCV(image, OpenCVTransform.OLD_STIM_VIDEO_WIDTH,
							//                                      OpenCVTransform.OLD_STIM_VIDEO_HEIGHT));
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
					e.printStackTrace();
					logger.debug(
							"Error en l�nea " + i + " del protocolo: " + arr[i].trim() + ", Error: "
									+ e.getMessage());
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
					check = false;
					logger.error(
							"Error en l�nea " + i + " del protocolo: " + arr[i].substring(7).trim()
									+ " no es un entero");
					check = false;
				}
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
					check = false;
					logger.error(
							"Error en l�nea " + i + " del protocolo: " + arr[i].substring(7).trim()
									+ " no es un entero");
					check = false;
				}
			} else if (arr[i].indexOf("TACTIL") == 0) {
				events.add(new EventBean(EventEnum.TACTIL, 0, i+1));
				continue;
			}
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
			BorderPane rootElement1 = (BorderPane) loader.load();
			// create and style a scene
			Stage primaryStage1 = new Stage();
			Scene scene = new Scene(rootElement1, 600, 600);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			// create the stage with the given title and the previously created
			// scene
			primaryStage1.setTitle("Canny Imager for KGS");
			primaryStage1.setScene(scene);
			// show the GUI
			primaryStage1.show();

			// set the proper behavior on closing the application
			CameraController controller = loader.getController();
			primaryStage1.setOnCloseRequest((new EventHandler<WindowEvent>() {
				public void handle(WindowEvent we) {
					controller.setClosed();
				}
			}));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void notifyOfThreadComplete(Thread thread) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				addImage(rootProtocol, "fin-experimento.png", true);
				try {
					Thread.sleep(EEGControl.END_PROTOCOL_WAIT_MILIS);
				} catch (InterruptedException ex) {
					logger.error("Error finalizando aplicación " + ex.getLocalizedMessage());
				}
				rootProtocol.getScene().getWindow().hide();
				doClean();
			}
		});
	}

	private void doClean() {
		rootProtocol = null;
		stageProtocol = null;
		protocolController = null;
		events = new ArrayList<EventBean>();
		estims = new ArrayList<EstimulusBean>();
		initalImage = null;
		// System.out.println(Runtime.getRuntime().totalMemory() -
		// Runtime.getRuntime().freeMemory());
		System.gc();
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
		String imageStr = null;

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
		logger.debug("Showing image " + imageToShow.hashCode());

		ImageView imageView = null;
		if (imageToShow != null)
			imageView = new ImageView(imageToShow);

		EEGControl.addImage(pane, imageView);
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
			labelBorderPane.getScene().setOnMouseReleased(e -> {
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
		// load the native OpenCV library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
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

	public void setRootProtocol(BorderPane rootProtocol) {
		this.rootProtocol = rootProtocol;
	}

	private boolean checkMediaReference(String mediaReference) {
		return medias.get(mediaReference) == null;
	}

	/**
	 * Creates a MediaBean of type MediaTypeEnum with the appropriate Media element as Primary, if
	 * the MediaBean contains an image, is the imageFile, if contains a sound, is the soundFile (and
	 * also the fileNameImage for the image to show meanwhile the sound is playing)
	 *
	 * @param fileNamePrimary
	 * @param fileNameImage
	 * @param mediaTypeEnum
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
				Media mediaVideo = new Media(file.toURI().toString());
				MediaBean mediaBean = new MediaBean(mediaVideo, mediaTypeEnum);
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
	 * @param sc
	 * @return false if an error is found
	 */
	private boolean createMultimediaEvent(String line, int line_num,  EventEnum eventType,
			MediaTypeEnum mediaTypeEnum, Scanner sc) {
		try {
			String[] data = line.split("\\s");
			String fileName = null;
			String fileNameSecondary = null;
			if (data.length > 1) {
				fileName = data[1].replace("\"", "");
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
				if (checkMediaReference(fileName)) {
					if (data.length > 2) {
						fileNameSecondary = data[2].replace("\"", "");
						if (!fileNameSecondary.contains(".")) {
							fileNameSecondary = fileNameSecondary + ".bmp";
						}
						mediaReference = fileName + "_" + fileNameSecondary;
						mediaTypeEnum = MediaTypeEnum.SOUND_IMAGE;
					}

					if (!createMediaReference(fileName, null, mediaTypeEnum)) {
						sc.close();
						return false;
					}
				}
				multimediaEvent.setMediaReference(mediaReference);
				if(eventType == EventEnum.MULTI){
					int waitSecs = multistimulatorWaitStimImageInMillis;

					if(data.length > 2){
						try{
							waitSecs = Integer.parseInt(data[2].replace("\"", ""));
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
			logger.debug("Error en línea " + (line_num + 1) + " del protocolo: " + line.trim()
					+ ", Error: " + e.getMessage());
			return false;
		}
	}

	private void showErrorDialog(String message) {
		logger.error(message);
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error");
		alert.setContentText(message);
		alert.show();
	}
}
