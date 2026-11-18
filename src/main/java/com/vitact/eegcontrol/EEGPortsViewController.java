package com.vitact.eegcontrol;

import com.fazecast.jSerialComm.SerialPort;
import com.vitact.eegcontrol.bean.ComPortBean;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.apache.logging.log4j.*;

public class EEGPortsViewController {

	@FXML
	Label lEEG;
	@FXML
	ComboBox<ComPortBean> cbEEG;
	@FXML
	ComboBox<ComPortBean> cbMatrix;
	@FXML
	ComboBox<ComPortBean> cbGlove;
	@FXML
	ComboBox<ComPortBean> cbMulti;
	@FXML
	ComboBox<ComPortBean> cbPrisma;
	@FXML
	Label lMatrix;
	@FXML
	Label lGlove;
	@FXML
	Label lMulti;
	@FXML
	Label lPrisma;
	@FXML
	CheckBox cbPorts;

	EEGControl padre;
	SerialPort comEEG;
	SerialPort comMatrix;
	SerialPort comGlove;
	SerialPort comMulti;
	SerialPort comPrisma;
	boolean configured = false;
	static Logger logger = null;

	public EEGPortsViewController() {
		logger = LogManager.getLogger(this.getClass().getName());
	}

	@FXML
	public void initialize() {
		ObservableList<ComPortBean> list = FXCollections.observableArrayList();
		list.add(null);
		for (SerialPort port : SerialPort.getCommPorts())
			list.add(new ComPortBean(port));

		cbEEG.setItems(list);
		cbMatrix.setItems(list);
		cbGlove.setItems(list);
		cbMulti.setItems(list);
		cbPrisma.setItems(list);

		if (!EEGControl.useEEGProtocol)
			cbEEG.setDisable(true);
		if (!EEGControl.useMatrixProtocol)
			cbMatrix.setDisable(true);
		if (!EEGControl.useGloveProtocol)
			cbGlove.setDisable(true);
		if (!EEGControl.useMultiStimulator)
			cbMulti.setDisable(true);
		if (!EEGControl.usePrisma)
			cbPrisma.setDisable(true);
	}

	/** Puerto elegido en el combo, o null si no se eligió ninguno. */
	private static SerialPort selectedPort(ComboBox<ComPortBean> combo, String noneMessage) {
		if (combo.getValue() == null) {
			logger.debug(noneMessage);
			return null;
		}
		return combo.getValue().getSerialPort();
	}

	@FXML
	public void conectar() {
		comEEG = selectedPort(cbEEG, "Ningún EEG seleccionado");
		comMatrix = selectedPort(cbMatrix, "Ninguna matriz seleccionada");
		comGlove = selectedPort(cbGlove, "Ningún guante seleccionado");
		comMulti = selectedPort(cbMulti, "Ningún multiestimulador seleccionado");
		comPrisma = selectedPort(cbPrisma, "Ningún estimulador Prisma seleccionado");

		if ((comEEG == null || !EEGControl.useEEGProtocol) &&
				(comMatrix == null || !EEGControl.useMatrixProtocol) &&
				(comGlove == null || !EEGControl.useGloveProtocol) &&
				(comMulti == null || !EEGControl.useMultiStimulator) &&
				(comPrisma == null || !EEGControl.usePrisma)) {
			logger.info("Programa terminado. Ningún puerto abierto");
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			String message = "";
			if (EEGControl.useEEGProtocol && comEEG == null)
				message = message + "Debe elegir un puerto para la comunicación con el EEG\n";
			else if (EEGControl.useMatrixProtocol && comMatrix == null)
				message = message + "Debe elegir un puerto para la comunicación con la Matriz\n";
			else if (EEGControl.useGloveProtocol && comGlove == null)
				message = message + "Debe elegir un puerto para la comunicación con el Guante\n";
			else if (EEGControl.useMultiStimulator && comMulti == null)
				message = message + "Debe elegir un puerto para la comunicación con el Multiestimulador\n";
			else if (EEGControl.usePrisma && comPrisma == null)
				message = message + "Debe elegir un puerto para la comunicación con el estimulador Prisma\n";
			alert.setContentText(message);
			alert.show();
		} else {
			configured = true;
			Stage stage = (Stage) cbEEG.getScene().getWindow();
			stage.close();
		}
	}

	public void setPadre(EEGControl padre) {
		this.padre = padre;
	}

	public SerialPort getComEEG() {
		return comEEG;
	}

	public SerialPort getComMatrix() {
		return comMatrix;
	}

	public SerialPort getComGlove() {
		return comGlove;
	}

	public SerialPort getComMulti() {
		return comMulti;
	}

	public SerialPort getComPrisma() {
		return comPrisma;
	}

	public CheckBox getCbPorts() {
		return cbPorts;
	}

	public boolean isConfigured() {
		return configured;
	}
}
