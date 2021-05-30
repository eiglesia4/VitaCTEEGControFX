package com.vitact.eegcontrol.bean;

import java.io.*;
import java.util.Properties;

public class StudyBean {
	String name, surname, age, sex, studyName, investigator, dateInit;
	String studyCode;

	public static StudyBean loadStudyBeanFromProps(String filePath) {
		try (FileInputStream fis = new FileInputStream(filePath)) {
			Properties prop = new Properties();
			prop.load(fis);
			StudyBean studyBean = new StudyBean();
			studyBean.setName(prop.getProperty("nombre"));
			studyBean.setSurname(prop.getProperty("apellidos"));
			studyBean.setAge(prop.getProperty("edad"));
			studyBean.setSex(prop.getProperty("sexo"));
			studyBean.setStudyName(prop.getProperty("estudio"));
			studyBean.setInvestigator(prop.getProperty("investigador"));
			studyBean.setDateInit(prop.getProperty("fecha"));
			studyBean.setStudyCode(prop.getProperty("studyCode"));

			return studyBean;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[sujeto]");
		sb.append("\nstudyCode=");
		sb.append(studyCode);
		sb.append("\nnombre=");
		sb.append(name);
		sb.append("\napellidos=");
		sb.append(surname);
		sb.append("\nedad=");
		sb.append(age);
		sb.append("\nsexo=");
		sb.append(sex);
		sb.append("\n[estudio]");
		sb.append("\nestudio=");
		sb.append(studyName);
		sb.append("\nfecha=");
		sb.append(dateInit);
		sb.append("\n[investigador]");
		sb.append("\ninvestigador=");
		sb.append(investigator);
		sb.append("\n");
		return sb.toString();

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getAge() {
		return age;
	}

	public void setAge(String age) {
		this.age = age;
	}

	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

	public String getStudyName() {
		return studyName;
	}

	public void setStudyName(String studyName) {
		this.studyName = studyName;
	}

	public String getInvestigator() {
		return investigator;
	}

	public void setInvestigator(String investigator) {
		this.investigator = investigator;
	}

	public String getDateInit() {
		return dateInit;
	}

	public void setDateInit(String dateInit) {
		this.dateInit = dateInit;
	}

	public String getStudyCode() {
		return studyCode;
	}

	public void setStudyCode(String studyCode) {
		this.studyCode = studyCode;
	}
}
