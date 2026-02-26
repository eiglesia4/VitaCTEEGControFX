package com.vitact.eegcontrol.utils;

import com.vitact.eegcontrol.EEGControl;

public class ProtocolUtils {
	public static boolean trueFalseLine(String line) {
		String data[] = line.split("\\s");
		String fileName = null;
		if (data.length > 1)
			fileName = data[1].replace("\"", "");

		if (fileName != null) {
			// OVERRIDES CONFIGURATION DEFINITION OF FULLSCREEN
			if (fileName.equalsIgnoreCase("SI") || fileName.equalsIgnoreCase("YES")
					|| fileName.equalsIgnoreCase("TRUE"))
				return true;
			else
				return false;
		} else {
			// IGNORE MALFORMED FULLSCREEN
			return false;
		}
	}
}
