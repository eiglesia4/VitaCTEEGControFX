package com.vitact.eegcontrol.bean;

public class EstimulusBean
{
	int mark;
	byte[] estim;
	int dim;

	public EstimulusBean(int m, int d, String s)
	{
		mark = m;
		dim = d;
		// logger.debug("Est "+m);
		String[] array = s.split(",");
		estim = new byte[array.length];
		for (int i = 0; i < array.length; i++)
		{
			estim[i] = (byte) Integer.parseInt(array[i].trim());
		}
	}

	public int getMark()
	{
		return mark;
	}

	public void setMark(int mark)
	{
		this.mark = mark;
	}

	public byte[] getEstim()
	{
		return estim;
	}

	public void setEstim(byte[] estim)
	{
		this.estim = estim;
	}

	public int getDim()
	{
		return dim;
	}

	public void setDim(int dim)
	{
		this.dim = dim;
	}

}
