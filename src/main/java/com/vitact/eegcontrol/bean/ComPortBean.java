package com.vitact.eegcontrol.bean;

import java.io.Serializable;

import com.fazecast.jSerialComm.SerialPort;

public class ComPortBean implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	SerialPort serialPort;
	
	public ComPortBean()
	{
		this.setSerialPort(null);
	}
	
	public ComPortBean(SerialPort serialPort)
	{
		this.setSerialPort(serialPort);
	}
	
	public String toString()
	{
		return serialPort==null?"":serialPort.getDescriptivePortName();
	}

	public SerialPort getSerialPort()
	{
		return serialPort;
	}

	public void setSerialPort(SerialPort serialPort)
	{
		this.serialPort = serialPort;
	}
	

}
