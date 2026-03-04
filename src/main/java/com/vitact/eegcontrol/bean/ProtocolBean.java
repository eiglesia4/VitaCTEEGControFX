package com.vitact.eegcontrol.bean;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ProtocolBean
{
	String description, fileName, dateExecution;
	StudyBean studyBean;
	
	public static String getDateString(Date date)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
		return sdf.format(date);
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		String nameStudy = "0000";
		if(studyBean!=null)
			nameStudy = studyBean.getStudyName();
		sb.append("[protocolo]");
		sb.append("\ndescripcion=");
		sb.append(description);
		sb.append("\nfichero=");
		sb.append(fileName);
		sb.append("\nfecha creación=");
		sb.append(dateExecution);
		sb.append("\n[estudio]");
		sb.append("\nnombre=");
		sb.append(nameStudy);
		sb.append("\n");
		return sb.toString();
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getFileName()
	{
		return fileName;
	}

	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	public String getDateExecution()
	{
		return dateExecution;
	}

	public void setDateExecution(String dateExecution)
	{
		this.dateExecution = dateExecution;
	}



	public StudyBean getStudyBean()
	{
		return studyBean;
	}



	public void setStudyBean(StudyBean studyBean)
	{
		this.studyBean = studyBean;
	}
}
