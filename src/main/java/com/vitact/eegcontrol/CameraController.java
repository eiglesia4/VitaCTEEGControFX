package com.vitact.eegcontrol;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.sarxos.webcam.Webcam;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
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
	private Webcam webcam;
	private boolean cameraActive = false;
	private static int cameraId = 0;
	Image whiteImage = null;

	private static final int STIM_WIDTH = 48;
	private static final int STIM_HEIGHT = 32;

	@FXML
	public void initialize()
	{
		String mediaIniciar = EEGControl.BASE_FILE
		    + EEGControl.MULTIMEDIA_FILE_BASE + "white.png";

		File file = new File(mediaIniciar);
		whiteImage = new Image(file.toURI().toString(),
		                       STIM_WIDTH, STIM_HEIGHT, false, false);

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
		java.util.List<Webcam> webcams = Webcam.getWebcams();
		for (int i = 0; i < webcams.size(); i++)
		{
			camSelector.getItems().add(i);
		}
		loadCams.setText("Recargar Cámaras");
		loadCams.setDisable(true);
		if (!webcams.isEmpty())
			camSelector.getSelectionModel().selectFirst();
	}

	@FXML
	protected void startCamera(ActionEvent event)
	{
		cameraId = camSelector.getValue();
		if (!this.cameraActive)
		{
			java.util.List<Webcam> webcams = Webcam.getWebcams();
			if (cameraId >= webcams.size())
			{
				System.err.println("Camera ID out of range...");
				return;
			}
			webcam = webcams.get(cameraId);
			webcam.open();

			if (webcam.isOpen())
			{
				this.cameraActive = true;

				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable()
				{
					int counter = 0;
					@Override
					public void run()
					{
						BufferedImage frame = grabFrame();
						if (frame == null) return;
						// Show original image
						Image imageToShow = SwingFXUtils.toFXImage(frame, null);
						updateImageView(imOrig, imageToShow);
						// Resize to stim size, apply Canny edge detection, invert
						BufferedImage mini = resizeImage(frame, STIM_WIDTH, STIM_HEIGHT);
						BufferedImage canny = cannyEdgeDetect(mini, 127, 200);
						invertImage(canny);

						if(counter % 2 == 0)
							imageToShow = SwingFXUtils.toFXImage(canny, null);
						else
							imageToShow = whiteImage;
						updateImageView(imCanny, imageToShow);
						counter++;
					}
				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33,
				                               TimeUnit.MILLISECONDS);

				this.button.setText("Stop Camera");
				loadCams.setDisable(true);
			}
			else
			{
				System.err.println("Impossible to open the camera connection...");
			}
		}
		else
		{
			this.cameraActive = false;
			this.button.setText("Start Camera");
			this.loadCams.setDisable(false);
			this.stopAcquisition();
		}
	}

	/**
	 * Grab a frame from the webcam and convert to grayscale
	 */
	private BufferedImage grabFrame()
	{
		if (webcam != null && webcam.isOpen())
		{
			try
			{
				BufferedImage colorFrame = webcam.getImage();
				if (colorFrame == null) return null;
				// Convert to grayscale
				BufferedImage gray = new BufferedImage(
					colorFrame.getWidth(), colorFrame.getHeight(),
					BufferedImage.TYPE_BYTE_GRAY);
				Graphics2D g = gray.createGraphics();
				g.drawImage(colorFrame, 0, 0, null);
				g.dispose();
				return gray;
			}
			catch (Exception e)
			{
				System.err.println("Exception during the image elaboration: " + e);
			}
		}
		return null;
	}

	/**
	 * Resize a BufferedImage to the given dimensions
	 */
	private static BufferedImage resizeImage(BufferedImage src, int width, int height)
	{
		BufferedImage resized = new BufferedImage(width, height, src.getType());
		Graphics2D g = resized.createGraphics();
		g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
		                   java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.drawImage(src, 0, 0, width, height, null);
		g.dispose();
		return resized;
	}

	/**
	 * Pure Java Canny edge detection for small grayscale images.
	 * Produces a binary (0 or 255) output image.
	 */
	private static BufferedImage cannyEdgeDetect(BufferedImage grayImg, int lowThreshold, int highThreshold)
	{
		int w = grayImg.getWidth();
		int h = grayImg.getHeight();
		byte[] pixels = ((DataBufferByte) grayImg.getRaster().getDataBuffer()).getData();

		// 1. Gaussian blur (3x3 kernel)
		double[] blurred = new double[w * h];
		double[][] kernel = {
			{1/16.0, 2/16.0, 1/16.0},
			{2/16.0, 4/16.0, 2/16.0},
			{1/16.0, 2/16.0, 1/16.0}
		};
		for (int y = 1; y < h - 1; y++) {
			for (int x = 1; x < w - 1; x++) {
				double sum = 0;
				for (int ky = -1; ky <= 1; ky++)
					for (int kx = -1; kx <= 1; kx++)
						sum += (pixels[(y + ky) * w + (x + kx)] & 0xFF) * kernel[ky + 1][kx + 1];
				blurred[y * w + x] = sum;
			}
		}

		// 2. Sobel gradients
		double[] magnitude = new double[w * h];
		double[] direction = new double[w * h];
		for (int y = 1; y < h - 1; y++) {
			for (int x = 1; x < w - 1; x++) {
				double gx = -blurred[(y-1)*w+(x-1)] + blurred[(y-1)*w+(x+1)]
				           -2*blurred[y*w+(x-1)]    + 2*blurred[y*w+(x+1)]
				           -blurred[(y+1)*w+(x-1)]   + blurred[(y+1)*w+(x+1)];
				double gy = -blurred[(y-1)*w+(x-1)] - 2*blurred[(y-1)*w+x] - blurred[(y-1)*w+(x+1)]
				           +blurred[(y+1)*w+(x-1)]  + 2*blurred[(y+1)*w+x] + blurred[(y+1)*w+(x+1)];
				magnitude[y * w + x] = Math.sqrt(gx * gx + gy * gy);
				direction[y * w + x] = Math.atan2(gy, gx);
			}
		}

		// 3. Non-maximum suppression
		double[] suppressed = new double[w * h];
		for (int y = 1; y < h - 1; y++) {
			for (int x = 1; x < w - 1; x++) {
				double angle = direction[y * w + x] * 180.0 / Math.PI;
				if (angle < 0) angle += 180;
				double q, r;
				if ((angle < 22.5) || (angle >= 157.5)) {
					q = magnitude[y * w + (x + 1)];
					r = magnitude[y * w + (x - 1)];
				} else if (angle < 67.5) {
					q = magnitude[(y + 1) * w + (x - 1)];
					r = magnitude[(y - 1) * w + (x + 1)];
				} else if (angle < 112.5) {
					q = magnitude[(y + 1) * w + x];
					r = magnitude[(y - 1) * w + x];
				} else {
					q = magnitude[(y - 1) * w + (x - 1)];
					r = magnitude[(y + 1) * w + (x + 1)];
				}
				double mag = magnitude[y * w + x];
				suppressed[y * w + x] = (mag >= q && mag >= r) ? mag : 0;
			}
		}

		// 4. Double threshold + hysteresis
		BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
		byte[] output = ((DataBufferByte) result.getRaster().getDataBuffer()).getData();
		// Mark strong and weak edges
		byte STRONG = (byte) 255;
		byte WEAK = (byte) 128;
		for (int i = 0; i < w * h; i++) {
			if (suppressed[i] >= highThreshold) output[i] = STRONG;
			else if (suppressed[i] >= lowThreshold) output[i] = WEAK;
			else output[i] = 0;
		}
		// Hysteresis: promote weak edges connected to strong edges
		for (int y = 1; y < h - 1; y++) {
			for (int x = 1; x < w - 1; x++) {
				if (output[y * w + x] == WEAK) {
					boolean hasStrong = false;
					for (int dy = -1; dy <= 1 && !hasStrong; dy++)
						for (int dx = -1; dx <= 1 && !hasStrong; dx++)
							if (output[(y + dy) * w + (x + dx)] == STRONG)
								hasStrong = true;
					output[y * w + x] = hasStrong ? STRONG : 0;
				}
			}
		}
		return result;
	}

	/**
	 * Invert a grayscale image in place (255 - pixel)
	 */
	private static void invertImage(BufferedImage img)
	{
		byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
		for (int i = 0; i < pixels.length; i++)
			pixels[i] = (byte) (255 - (pixels[i] & 0xFF));
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
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				System.err
				  .println("Exception in stopping the frame capture, trying to release the camera now... "
				      + e);
			}
		}

		if (this.webcam != null && this.webcam.isOpen())
		{
			this.webcam.close();
		}
	}

	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 */
	private void updateImageView(ImageView view, Image image)
	{
		Platform.runLater(() -> view.imageProperty().set(image));
	}

	/**
	 * On application close, stop the acquisition from the camera
	 */
	protected void setClosed()
	{
		this.stopAcquisition();
	}
}
