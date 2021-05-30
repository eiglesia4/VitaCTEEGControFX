package com.vitact.eegcontrol;

import com.vitact.eegcontrol.bean.StudyBean;
import java.io.*;
import java.util.Arrays;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.*;
import org.apache.logging.log4j.*;

public class EEGViewController {
	File chosenFile;
	private EEGControl padre;
	static Logger logger = null;
	public StudyBean studyBean = null;
	@FXML
	Label lLastProtocol;
	@FXML
	Label lLastProtocolName;
	@FXML
	Button bContinueStudy;
	String loggerNames[];
	@FXML
	Label lStudyCode;
	@FXML
	Label lProtocol;
	@FXML
	Label lDate;
	@FXML
	Label lStudy;

	public EEGViewController() {
		logger = LogManager.getLogger(this.getClass().getName());
		loggerNames = getLastStudyAndProtocol();
	}

	@FXML
	public void initialize() {
		if (loggerNames[0].equals("0000")) {
			bContinueStudy.setDisable(true);
		} else {
			if (studyBean == null) {
				loadLastStudy(loggerNames);
			}
		}
	}

	@FXML
	public void startGlasses(ActionEvent event) {
		padre.loadCameras();
	}

	@FXML
	public void newStudy(ActionEvent event) {
		int numStrudio = Integer.parseInt(loggerNames[0]) + 1;
		loggerNames[0] = String.format("%04d", numStrudio);
		loggerNames[1] = String.format("%02d", 2);
		padre.reloadLoggers(loggerNames);

		launchNewStudyDialog();
	}

	@FXML
	public void continueStudy(ActionEvent event) {
		loggerNames = getLastStudyAndProtocol();
		padre.reloadLoggers(loggerNames);

		if (studyBean != null && padre.studyBean == null)
			padre.studyBean = studyBean;

		executeExperiment(event);
	}

	@FXML
	public void startExperiment(ActionEvent event) {
		String[] studyAndProtocol = {"0000", "02"};
		padre.reloadLoggers(studyAndProtocol);
		executeExperiment(event);
	}

	private void launchNewStudyDialog() {
		try {
			FXMLLoader loader = null;
			if (EEGControl.USE_FULL_STUDY_DATA) {
				loader = new FXMLLoader(getClass().getResource("EEGStudyLoader.fxml"));
			} else {
				loader = new FXMLLoader(getClass().getResource("EEGStudyLoaderMinimal.fxml"));
			}
			BorderPane root = (BorderPane) loader.load();
			EEGStudyLoader eegStudyLoader = loader.getController();
			eegStudyLoader.setPadre(this);
			Scene scene = new Scene(root);
			Stage stage = new Stage();
			stage.setTitle("Configuración de Nuevo Estudio");

			stage.setScene(scene);
			stage.show();
		} catch (IOException e) {
			logger.error("Error while loading Nuevo Estudio scene", e);
			Platform.exit();
		}
	}

	public void executeExperiment(ActionEvent event) {
		Scene scene = ((Button) event.getSource()).getScene();
		Stage stage = (Stage) scene.getWindow();
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Seleccionar Protocolo");
		fileChooser.setInitialDirectory(
				new File(EEGControl.BASE_FILE + EEGControl.PROTOCOL_FILE_BASE));
		chosenFile = fileChooser.showOpenDialog(stage);
		if (chosenFile == null) {
			logger.warn("No protocol opened, returning to main page");
			return;
		}
		try {
			lLastProtocolName.setText(chosenFile.getName());
		} catch (Exception e) {
			// He cancelado el dialogo
			lLastProtocolName.setText("NINGUNO");
		}
		studyBean.setStudyName(lLastProtocolName.getText());
		Logger loggerStudy = LogManager.getLogger("Study");
		loggerStudy.info(studyBean.toString());
		lProtocol.setText(lLastProtocolName.getText());
		padre.fileProtocolLoaded(chosenFile);
	}

	@SuppressWarnings("finally")
	private String[] getLastStudyAndProtocol() {
		File f = null;
		File[] paths;
		File[] paths1;
		String[] studyAndProtocol = new String[2];
		try {
			f = new File(EEGControl.STUDY_BASE_DIR);
			paths = f.listFiles();
			Arrays.sort(paths);

			studyAndProtocol[0] = paths[paths.length - 1].getName();
			if (Integer.parseInt(studyAndProtocol[0]) == 0) {
				studyAndProtocol[0] = "0000";
				studyAndProtocol[1] = "02";
			} else {
				paths1 = paths[paths.length - 1].listFiles();
				Arrays.sort(paths1);
				String fileName = paths1[paths1.length - 1].getName();
				if (fileName != null && fileName.contains("-")) {
					studyAndProtocol[1] = fileName.substring(fileName.lastIndexOf("-") + 1,
							fileName.indexOf("."));
					int protocolNumber = Integer.parseInt(studyAndProtocol[1]);
					studyAndProtocol[1] = String.format("%02d", protocolNumber + 2);
				} else {
					studyAndProtocol[1] = String.format("%02d", 2);
				}
			}
		} catch (Exception e) {
			logger.warn("Error " + e.getMessage() + " loading last study file. Using first");
			studyAndProtocol[0] = "0001";
			studyAndProtocol[1] = "02";
		} finally {
			return studyAndProtocol;
		}
	}

	private void loadLastStudy(String[] routePath) {
		String filePath =
				EEGControl.STUDY_BASE_DIR + "/" + routePath[0] + "/estudio-" + routePath[0]
						+ ".dat";
		logger.info("Loading Study from " + filePath);
		StudyBean myBean = StudyBean.loadStudyBeanFromProps(filePath);
		if (myBean == null) {
			logger.warn("No Study found on " + filePath + ". Using dummy with studyName=UNKNOWN");
			myBean = new StudyBean();
			myBean.setStudyName("UNKNOWN");
		} else {
			setStudyBean(myBean);
			logger.info("Loaded Study name: " + getStudyBean().getStudyName() + ", subject: "
					+ getStudyBean().getName() + " " + getStudyBean().getSurname());
		}
	}

	public EEGControl getPadre() {
		return padre;
	}

	public void setPadre(EEGControl padre) {
		this.padre = padre;
		if (studyBean != null)
			this.padre.setStudyBean(studyBean);
	}

	public StudyBean getStudyBean() {
		return studyBean;
	}

	public void setStudyBean(StudyBean studyBean) {
		this.studyBean = studyBean;
		lStudy.setText(loggerNames[0]);
		lDate.setText(studyBean.getDateInit());
		lProtocol.setText(studyBean.getStudyName());
		lStudyCode.setText(studyBean.getStudyCode());
		if (padre != null)
			padre.setStudyBean(studyBean);
	}

}
