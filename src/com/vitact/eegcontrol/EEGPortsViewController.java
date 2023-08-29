package com.vitact.eegcontrol;

import com.fazecast.jSerialComm.SerialPort;
import com.vitact.eegcontrol.bean.ComPortBean;
import javafx.collections.*;
import javafx.event.ActionEvent;
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
	Label lMatrix;
	@FXML
	Label lGlove;
	@FXML
	Label lMulti;
	@FXML
	CheckBox cbPorts;

	EEGControl padre;
	SerialPort comEEG;
	SerialPort comMatrix;
	SerialPort comGlove;
	SerialPort comMulti;
	SerialPort comPort[];
	boolean configured = false;
	static Logger logger = null;

	public EEGPortsViewController() {
		logger = LogManager.getLogger(this.getClass().getName());
	}

	@FXML
	public void initialize() {
		comPort = SerialPort.getCommPorts();
		String[] coms = new String[comPort.length + 1];
		coms[0] = "";
		for (int i = 0; i < comPort.length; i++)
			coms[i + 1] = comPort[i].getDescriptivePortName();

		String[] coms2 = new String[comPort.length + 1];
		coms2[0] = "";
		for (int i = 0; i < comPort.length; i++)
			coms2[i + 1] = comPort[i].getDescriptivePortName();

		ObservableList<ComPortBean> list = FXCollections.observableArrayList();
		list.add(null);
		for (int i = 0; i < comPort.length; i++)
			list.add(new ComPortBean(comPort[i]));

		cbEEG.setItems(list);
		cbMatrix.setItems(list);
		cbGlove.setItems(list);
		cbMulti.setItems(list);

		if (!EEGControl.useEEGProtocol)
			cbEEG.setDisable(true);
		if (!EEGControl.useMatrixProtocol)
			cbMatrix.setDisable(true);
		if (!EEGControl.useGloveProtocol)
			cbGlove.setDisable(true);
		if (!EEGControl.useMultiStimulator)
			cbMulti.setDisable(true);
	}

	@FXML
	public void conectar(ActionEvent event) {
		if (cbEEG.getValue() == null) {
			logger.debug("Ningún EEG seleccionado");
			comEEG = null;
		} else
			comEEG = cbEEG.getValue().getSerialPort();
		if (cbMatrix.getValue() == null) {
			logger.debug("Ninguna matriz seleccionada");
			comMatrix = null;
		} else
			comMatrix = cbMatrix.getValue().getSerialPort();
		if (cbGlove.getValue() == null) {
			logger.debug("Ningún guante seleccionado");
			comGlove = null;
		} else
			comGlove = cbGlove.getValue().getSerialPort();
		if (cbMulti.getValue() == null) {
			logger.debug("Ningún multiestimulador seleccionado");
			cbMulti = null;
		} else
			comMulti = cbMulti.getValue().getSerialPort();

		if ((comEEG == null || !EEGControl.useEEGProtocol) &&
				(comMatrix == null || !EEGControl.useMatrixProtocol) &&
				(comGlove == null || !EEGControl.useGloveProtocol) &&
				(comMulti == null || !EEGControl.useMultiStimulator)) {
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
			alert.setContentText(message);
			alert.show();
		} else {
			configured = true;
			Stage stage = (Stage) cbEEG.getScene().getWindow();
			// do what you have to do
			stage.close();
		}
	}

	public EEGControl getPadre() {
		return padre;
	}

	public void setPadre(EEGControl padre) {
		this.padre = padre;
	}

	public SerialPort getComEEG() {
		return comEEG;
	}

	public void setComEEG(SerialPort comEEG) {
		this.comEEG = comEEG;
	}

	public SerialPort getComMatrix() {
		return comMatrix;
	}

	public void setComMatrix(SerialPort comMatrix) {
		this.comMatrix = comMatrix;
	}

	public SerialPort getComGlove() {
		return comGlove;
	}

	public void setComGlove(SerialPort comGlove) {
		this.comGlove = comGlove;
	}

	public CheckBox getCbPorts() {
		return cbPorts;
	}

	public void setCbPorts(CheckBox cbPorts) {
		this.cbPorts = cbPorts;
	}

	public boolean isConfigured() {
		return configured;
	}

	public void setConfigured(boolean configured) {
		this.configured = configured;
	}

}
