package com.vitact.eegcontrol.bean;

public class MultimediaBean
{
	public static int TYPE_VIDEO = 0;
	public static int TYPE_IMAGE = 1;
	public static int TYPE_SOUND = 2;
	
	String file;
	int type;
	public String getFile()
	{
		return file;
	}
	public void setFile(String file)
	{
		this.file = file;
	}
	public int getType()
	{
		return type;
	}
	public void setType(int type)
	{
		this.type = type;
	}
}
