package com.vitact.eegcontrol;

import com.vitact.eegcontrol.bean.StudyBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.apache.logging.log4j.*;

public class EEGStudyLoaderController extends EEGStudyLoader {
	@FXML
	TextField tfNameUser;
	@FXML
	TextField tfSurnameUser;
	@FXML
	TextField tfAgeUser;
	@FXML
	ChoiceBox<String> cbSexUser;
	@FXML
	ChoiceBox<String> cbInvestigator;
	@FXML
	TextField tfStudyName;
	static Logger logger = null;

	public EEGStudyLoaderController() {
		logger = LogManager.getLogger(this.getClass().getName());
	}

	@FXML
	public void initialize() {
		ObservableList<String> sexesList = FXCollections.observableArrayList();
		sexesList.add("Masculino");
		sexesList.add("Femenino");
		//TODO: SUSTITUIR ESTO POR UN FICHERO EXTERNO
		ObservableList<String> investigatorList = FXCollections.observableArrayList();
		investigatorList.add("Elena");
		investigatorList.add("Verónica");
		investigatorList.add("Tomás");
		investigatorList.add("Pablo");
		investigatorList.add("Otro");
		cbSexUser.setItems(sexesList);
		cbInvestigator.setItems(investigatorList);
	}

	@FXML
	public void cancelDialog(ActionEvent event) {
		Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
		stage.close();
	}

	@FXML
	public void okDialog(ActionEvent event) {
		SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
		StudyBean studyBean = new StudyBean();

		if (cbInvestigator.getValue() != null && cbInvestigator.getValue().length() > 0)
			studyBean.setInvestigator(cbInvestigator.getValue());
		else {
			launchBlockDialog(event, "nombre del investigador", cbInvestigator);
			return;
		}
		if (tfAgeUser.getText() != null && tfAgeUser.getText().length() > 0)
			studyBean.setAge(tfAgeUser.getText());
		else {
			launchBlockDialog(event, "edad", tfAgeUser);
			return;
		}
		studyBean.setDateInit(sdf.format(new Date()));
		if (tfNameUser.getText() != null && tfNameUser.getText().length() > 0)
			studyBean.setName(tfNameUser.getText());
		else {
			launchBlockDialog(event, "nombre del sujeto", tfNameUser);
			return;
		}
		if (cbSexUser.getValue() != null && cbSexUser.getValue().length() > 0)
			studyBean.setSex(cbSexUser.getValue());
		else {
			launchBlockDialog(event, "sexo", cbSexUser);
			return;
		}

		if (tfStudyName.getText() != null && tfStudyName.getText().length() > 0)
			studyBean.setStudyName(tfStudyName.getText());
		else {
			launchBlockDialog(event, "nombre del estudio", tfStudyName);
			return;
		}
		if (tfSurnameUser.getText() != null && tfSurnameUser.getText().length() > 0)
			studyBean.setSurname(tfSurnameUser.getText());
		else {
			launchBlockDialog(event, "apellidos del sujeto", tfSurnameUser);
			return;
		}

		padre.setStudyBean(studyBean);
		cancelDialog(event);
		padre.bContinueStudy.setDisable(false);
		padre.executeExperiment(event);

	}

	private void launchBlockDialog(ActionEvent event, String string, Control tfAgeUser2) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error");
		alert.setContentText("El campo " + string + " es obligatorio.");
		alert.show();
		tfAgeUser2.requestFocus();
	}

}
