package com.vitact.eegcontrol;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import com.vitact.eegcontrol.opencv.OpenCVTransform;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class CameraController
{
	@FXML
	private Button button;
	@FXML
	private Button loadCams;
	@FXML
	private ChoiceBox<Integer> camSelector;
	@FXML
	private ImageView imOrig;
	@FXML
	private ImageView imCanny;
	private ScheduledExecutorService timer;
	// the OpenCV object that realizes the video capture
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive = false;
	// the id of the camera to be used
	private static int cameraId = 0;
	Image whiteImage = null;

	@FXML
	public void initialize()
	{
		String mediaIniciar = EEGControl.BASE_FILE
		    + EEGControl.MULTIMEDIA_FILE_BASE + "white.png";

		File file = new File(mediaIniciar);
		whiteImage = new Image(file.toURI().toString(),
		                       OpenCVTransform.OLD_STIM_VIDEO_WIDTH,
		                       OpenCVTransform.OLD_STIM_VIDEO_HEIGHT, false, false);

		loadCamsAct(null);
		Platform.runLater(new Runnable()
		{
			@Override
			public void run()
			{
				startCamera(null);
			}
		});
		
	}
	
	@FXML
	protected void loadCamsAct(ActionEvent event)
	{
		camSelector.getItems().clear();
		boolean hasCamera = false;
		int number = 0;
		while (true)
		{
			try
			{
				capture.open(number);
			}
			catch (Exception e)
			{
				// No more cams
				break;
			}
			if (!capture.isOpened())
			{
				break;
			}
			else
			{
				camSelector.getItems().add(number);
				number++;
				capture.release();
				hasCamera = true;
			}
		}
		loadCams.setText("Recargar Cámaras");
		loadCams.setDisable(true);
		if(hasCamera)
			camSelector.getSelectionModel().selectFirst();

	}

	@FXML
	protected void startCamera(ActionEvent event)
	{
		cameraId = camSelector.getValue();
		if (!this.cameraActive)
		{
			// start the video capture
			this.capture.open(cameraId);

			// is the video stream available?
			if (this.capture.isOpened())
			{
				this.cameraActive = true;

				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable()
				{
					int counter = 0;
					@Override
					public void run()
					{
						// effectively grab and process a single frame
						Mat frame = grabFrame();
						// Show bigImage
						Image imageToShow = OpenCVTransform.mat2Image(frame);
						updateImageView(imOrig, imageToShow);
						// convert and show the frame
						Mat mini = new Mat(32, 48, frame.type());
						int interpolation = Imgproc.INTER_CUBIC;
						Imgproc.resize(frame, mini, mini.size(), 0, 0, interpolation);
						Mat canny = new Mat();
						Imgproc.Canny(mini, canny, 127, 200);
						Core.bitwise_not(canny, canny);

						if(counter % 2 == 0)
							imageToShow = OpenCVTransform.mat2Image(canny);
						else
							imageToShow = whiteImage;
						updateImageView(imCanny, imageToShow);
						counter++;
					}
				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33,
				                               TimeUnit.MILLISECONDS);

				// update the button content
				this.button.setText("Stop Camera");
				loadCams.setDisable(true);
			}
			else
			{
				// log the error
				System.err.println("Impossible to open the camera connection...");
			}
		}
		else
		{
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.button.setText("Start Camera");
			this.loadCams.setDisable(false);

			// stop the timer
			this.stopAcquisition();
		}
	}

	/**
	 * Get a frame from the opened video stream (if any)
	 *
	 * @return the {@link Mat} to show
	 */
	private Mat grabFrame()
	{
		// init everything
		Mat frame = new Mat();

		// check if the capture is open
		if (this.capture.isOpened())
		{
			try
			{
				// read the current frame
				this.capture.read(frame);

				// if the frame is not empty, process it
				if (!frame.empty())
				{
					Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
				}

			}
			catch (Exception e)
			{
				// log the error
				System.err.println("Exception during the image elaboration: " + e);
			}
		}

		return frame;
	}

	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition()
	{
		if (this.timer != null && !this.timer.isShutdown())
		{
			try
			{
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				// log any exception
				System.err
				  .println("Exception in stopping the frame capture, trying to release the camera now... "
				      + e);
			}
		}

		if (this.capture.isOpened())
		{
			// release the camera
			this.capture.release();
		}
	}

	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 * 
	 * @param view
	 *          the {@link ImageView} to update
	 * @param image
	 *          the {@link Image} to show
	 */
	private void updateImageView(ImageView view, Image image)
	{
		OpenCVTransform.onFXThread(view.imageProperty(), image);
	}

	/**
	 * On application close, stop the acquisition from the camera
	 */
	protected void setClosed()
	{
		this.stopAcquisition();
	}
}
