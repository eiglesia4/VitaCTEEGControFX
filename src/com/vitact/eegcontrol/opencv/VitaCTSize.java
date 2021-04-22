package com.vitact.eegcontrol.opencv;

public class VitaCTSize
{
	private int width, height;
	
	public double ratio()
	{
    return this.getWidth()/ this.getHeight();
	}

	public VitaCTSize() {}
	
	public VitaCTSize(int width, int height)
	{
		this.width = width;
		this.height = height;
	}
	
	public int getHeight()
	{
		return height;
	}

	public void setHeight(int height)
	{
		this.height = height;
	}


	public int getWidth()
	{
		return width;
	}


	public void setWidth(int width)
	{
		this.width = width;
	}
	
}
