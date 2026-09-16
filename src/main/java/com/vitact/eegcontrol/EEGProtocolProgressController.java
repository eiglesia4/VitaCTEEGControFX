package com.vitact.eegcontrol;

import java.util.ArrayList;

import com.vitact.eegcontrol.bean.EventBean;
import com.vitact.eegcontrol.type.EventEnum;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class EEGProtocolProgressController
{

	@FXML
	ListView<EventBean> list;

	ArrayList<EventBean> events;

	@FXML
	public void initialize()
	{
		if(EEGControl.showProtocolEvolWindow)
		{
			// getTipo() devuelve EventEnum; se comparaba con la cadena "TERMINAR", de modo
			// que la condición era siempre falsa y la ventana nunca se cerraba sola.
			// El listener también salta con null al limpiarse la selección.
			// De los tres argumentos del ChangeListener solo interesa el seleccionado.
			list.getSelectionModel().selectedItemProperty().addListener(
					(ignoredProperty, ignoredOldValue, selected) -> {
						if (selected != null && selected.getTipo() == EventEnum.TERMINAR)
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
