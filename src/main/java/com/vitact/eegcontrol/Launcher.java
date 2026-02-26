package com.vitact.eegcontrol;

/**
 * Launcher class for fat JAR execution.
 * JavaFX requires the main class to NOT extend Application
 * when running from an uber-JAR on the classpath.
 */
public class Launcher {
	public static void main(String[] args) {
		EEGControl.main(args);
	}
}
