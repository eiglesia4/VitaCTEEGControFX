package com.vitact.eegcontrol;

import java.util.ArrayList;

import com.vitact.eegcontrol.bean.EventBean;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class EEGProtocolProgressController
{

	@FXML
	Label time;
	@FXML
	ListView<EventBean> list;
	@FXML
	Label timeT;

	ArrayList<EventBean> events;

	@FXML
	public void initialize()
	{
		if(EEGControl.showProtocolEvolWindow)
		{
			list.getSelectionModel().selectedItemProperty().addListener(l -> {
				if (list.getSelectionModel().getSelectedItem().getTipo().equals("TERMINAR"))
					list.getScene().getWindow().hide();
			});
		}
	}

	public ArrayList<EventBean> getEvents()
	{
		return events;
	}

	public void setEvents(ArrayList<EventBean> events)
	{
		this.events = events;
		list.setItems(FXCollections.observableArrayList(getEvents()));
	}
}
