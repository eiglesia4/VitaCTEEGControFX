package com.vitact.eegcontrol;

import com.vitact.eegcontrol.bean.StudyBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.logging.log4j.*;

public class EEGStudyLoaderMinimalController extends EEGStudyLoader {
	static Logger logger = null;
	@FXML
	TextField tfStudyCode;

	public EEGStudyLoaderMinimalController() {
		logger = LogManager.getLogger(this.getClass().getName());
	}

	@FXML
	public void initialize() {

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

		if (tfStudyCode.getText() != null && tfStudyCode.getText().length() > 0)
			studyBean.setStudyCode(tfStudyCode.getText());
		else {
			launchBlockDialog(event, "código", tfStudyCode);
			return;
		}
		studyBean.setDateInit(sdf.format(new Date()));

		padre.setStudyBean(studyBean);
		cancelDialog(event);
		Logger loggerStudy = LogManager.getLogger("Study");
		loggerStudy.info(studyBean.toString());
		padre.bContinueStudy.setDisable(false);
		padre.executeExperiment(event);
	}

	private void launchBlockDialog(ActionEvent event, String string, Control tfAgeUser2) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Error");
		alert.setContentText("El campo " + string + " es obligatorio.");
		alert.show();
		tfAgeUser2.requestFocus();
	}

}
