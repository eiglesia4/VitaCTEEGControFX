package com.vitact.eegcontrol;

import java.io.File;

import com.vitact.eegcontrol.opencv.OpenCVTransform;

import javafx.application.Platform;
import javafx.scene.image.Image;

public class VibrateImageThread extends Thread
{
	Image img = null, whiteImage = null;
	ProtocolThread padre = null;
	
	public VibrateImageThread(ProtocolThread padre, Image img)
	{
		this.padre = padre;
		this.img = img;
		String mediaIniciar = EEGControl.BASE_FILE
		    + EEGControl.MULTIMEDIA_FILE_BASE + "white.png";

		File file = new File(mediaIniciar);
		whiteImage = new Image(file.toURI().toString(),
		                       OpenCVTransform.OLD_STIM_VIDEO_WIDTH,
		                       OpenCVTransform.OLD_STIM_VIDEO_HEIGHT, false, false);

	}

	public void run()
	{
		while(padre.vibrate && !padre.stop)
		{
			Platform.runLater(new Runnable()
			{
				@Override
				public void run()
				{
System.out.println("Showing image");
						EEGControl.addImage(padre.padre.getRootProtocol(), whiteImage);
				}
			});
			try
			{
				sleep(1000);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Platform.runLater(new Runnable()
			{
				@Override
				public void run()
				{
System.out.println("Showing white");
						EEGControl.addImage(padre.padre.getRootProtocol(), whiteImage);
				}
			});
		}
	}

}
