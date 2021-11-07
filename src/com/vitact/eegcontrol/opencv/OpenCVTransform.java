package com.vitact.eegcontrol.opencv;

import java.awt.image.*;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class OpenCVTransform {
	public static final int NORMAL_DISPLAY_WIDTH = 320;
	public static int zoom_factor = 1;
	public static final int WHITE_COLOR = 255;
	public static final int BLACK_COLOR = 0;
	public static int DEFAULT_VIDEO_WIDTH = 24; // was 28, now 24
	public static int DEFAULT_VIDEO_HEIGHT = 24; // was 28 now 24
	public static int OLD_STIM_VIDEO_WIDTH = 48; // was 28, now 24
	public static int OLD_STIM_VIDEO_HEIGHT = 32; // was 28 now 24
	public static final double HEIGHT_SCALAR = 0.80; /*
	 * Use the 80% of the height
	 */
	// TODO: DEFINIR ADECUADAMENTE OLD_STIM_VIDEO_WIDTH Y OLD_STIM_VIDEO_HEIGHT PARA KGS Y ESTIMULADOR ANTIGUO (SOLO POR PRURITO PERSONAL)

	@SuppressWarnings("unused")
	private static final int BYTES_SERIAL = 112; /* 28 * 4 bytes por fila = 112 */
	public static final char START_TO_SEND_CHAR = '!';
	public static final char READY_TO_SEND_CHAR = '?';

	public Image getEdgesOpenCV(Image bitmap) {
		return getEdgesOpenCV(bitmap, DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT);
	}

	public Image getEdgesOpenCV(Image bitmap, VitaCTSize size) {
		return getEdgesOpenCV(bitmap, size.getWidth(), size.getHeight());
	}

	public Image getEdgesOpenCV(Image bitmap, int width, int height) {
		int w = (int) bitmap.getWidth();
		int h = (int) bitmap.getHeight();
		BufferedImage bImage = SwingFXUtils.fromFXImage(bitmap, null);
		Mat raw_frame = img2Mat2(bImage, w, h);

		boolean has_border = false;

		/* Set frame square. */
		Mat frame = get_square_image(raw_frame);

		/* Size to be output */
		Mat display_image = get_normalized_display_image(frame, NORMAL_DISPLAY_WIDTH);

		/* Resize to desired size. */
		Mat li_zoom_frame = get_grey_image(frame, new Size(width, height), false, has_border, 8);

		/* Claculate Edges of big image */
		@SuppressWarnings("unused")
		Mat edge_frame = get_edges(frame, new Size(display_image.width(), display_image.height()));

		/* Claculate Edges of little image */
		Mat li_edge_frame = get_edges(li_zoom_frame, new Size(width, height));
		Mat li_send_frame = li_edge_frame;

		BufferedImage outImage = mat2Img(li_send_frame);

		return SwingFXUtils.toFXImage(outImage, null);

	}

	/*********************************************************************/
	/* Return the edge-detect implementation (Canny) from provided image */
	/* The returned image will be resized to the provided size */

	/*********************************************************************/
	private Mat get_edges(Mat image, Size img_size) {
		Mat img_edg = null, img_frame = null;

		/* Convert to greyscale */
		img_frame = get_grey_image(image, img_size, false, false, 0);

		img_edg = new Mat(new Size(img_frame.width(), img_frame.height()), CvType.CV_8UC1);

		// double t = (double) cvGetTickCount();

		Imgproc.Canny(img_frame, img_edg, 80, 120);

		invert_grayscale_color(img_edg);

		return img_edg;
	} // end get_edges

	/************************************************************************/
	/* invert_grayscale_color: Will invert the color of provided B&W images */
	/* returns 0 if ok */

	/************************************************************************/
	private int invert_grayscale_color(Mat image) {

		byte buff[] = new byte[(int) image.total() * image.channels()];
		image.get(0, 0, buff);
		for (int i = 0; i < buff.length; i++)
			buff[i] = (byte) (WHITE_COLOR - buff[i]);
		image.put(0, 0, buff);
		return 0;
	} // end invert_grayscale

	/******************************************************************/
	/* get_grey_image: Will return a scaled if needed grey image from */
	/* provided one. */
	/* Will resize output according to img_size */
	/* Gray Image will be equalized */
	/* Returned image has to be freed by the app. */
	/*                                                                */

	/******************************************************************/
	private Mat get_grey_image(Mat image, Size img_size, boolean equalize, boolean border,
			int border_size) {
		Mat img = null, img_frame = null, img_eq = null, img_border = null;

		img = new Mat(new Size(image.width(), image.height()), CvType.CV_8UC1);
		if (image.channels() != 1) {
			Imgproc.cvtColor(image, img, Imgproc.COLOR_RGB2GRAY);
		} else {
			image.copyTo(img);
		}

		if (border == true) {
			img_frame = new Mat(
					new Size(img_size.width - border_size, img_size.height - border_size),
					CvType.CV_8UC1);
		} else {
			img_frame = new Mat(new Size(img_size.width, img_size.height), CvType.CV_8UC1);
		}

		/* Resize if required */
		if ((img.width() != img_size.width) || (img.height() != img_size.height)) {
			Imgproc.resize(img, img_frame, img_frame.size());
		} else {
			img.copyTo(img_frame);
		} // end if

		if (border == true) {
			img_border = new Mat(new Size(img_size.width, img_size.height), CvType.CV_8UC1);

			Core.copyMakeBorder(img_frame, img_border, border_size >> 1, border_size >> 1,
					border_size >> 1, border_size >> 1, Core.BORDER_CONSTANT,
					new Scalar(WHITE_COLOR));

			/*
			 * FOR OPENCV 2.x Imgproc.copyMakeBorder(img_frame, img_border,
			 * border_size >> 1, border_size >> 1, border_size >> 1, border_size >> 1,
			 * Imgproc.BORDER_CONSTANT, new Scalar(WHITE_COLOR));
			 */
			img_frame = img_border.clone();
		}

		/* Equalize: */
		if (equalize == true) {
			/* Build resulting equalized image. */
			img_eq = img_frame.clone();

			Imgproc.equalizeHist(img_frame, img_eq);
		} else {
			img_eq = img_frame;
		} // end if

		return img_eq;
	} // end get_grey_image

	/**********************************************************************/
	/* get_square_img: Will return the provided image with the resolution */
	/* in square dimensions. */

	/**********************************************************************/
	private Mat get_square_image(Mat raw_img) {
		Mat img;
		Rect rectSplit;
		int height = (int) (raw_img.height() * HEIGHT_SCALAR);

		/* Image */
		rectSplit = new Rect((raw_img.width() - height) >> 1, (raw_img.height() - height) >> 1,
				height, height);

		img = raw_img.submat(rectSplit);

		return img;
	}

	/**************************************************************************/
	/* get_normalized_display_image: Will return a new empty image with their */
	/* dimensions taken from reference_img */
	/* If reference_img.width is greater than */
	/* reference_width will be scaled to less or */
	/* equal to reference_width. */

	/*************************************************************************/
	private Mat get_normalized_display_image(Mat reference_img, int reference_width) {
		Mat img;

		int factor = reference_img.width() / reference_width;
		int width, height;
		int aspect_ratio = reference_img.width() / reference_img.height();
		if (reference_img.width() > reference_width) {
			width = reference_img.width() / factor;
			height = (reference_img.height() * aspect_ratio) / factor;

			/* Create Normalized Image */
			img = new Mat(new Size(width, height), reference_img.type());
		} else {
			/* Create Normalized Image */
			img = new Mat(new Size(reference_img.width(), reference_img.height()),
					reference_img.type());
		} // end if

		return img;
	} // get_normalized_display_image

	/*****************************************************************/
	/* transform_to_bits_transpose: (nxn) */
	/* image: image to convert (B&W) */
	/* img_data: dest image in bits */
	/* len: length in bytes of img_data */

	/*****************************************************************/
	public byte[] transform_to_bits_transpose(Mat image, int bytesPerRow) {
		byte img_data[] = new byte[image.width() * image.height()];
		byte data_ret[] = new byte[image.width() * image.height()];
		int height = image.height(), width = image.width(); // no step in Java step
		// = 0;
		int index = 0;

		for (int i = 0; i < width; i++) {
			for (int j = (height - 1); j >= 0; j--) {
				if (j == (height - 1))
					index = 0;
				else
					index++;
				byte myByte = img_data[j * width + i];
				if (myByte < ((WHITE_COLOR * 2) / 3)) {

					int byte_num = (i * bytesPerRow) + index / 8;
					int bit_num = index % 8;

					data_ret[byte_num] = (byte) ((byte) data_ret[byte_num] | (1 << (bit_num)));
				} // end if
			} // end for
		} // end for

		return data_ret;
	} // transform_to_bits_transpose

	/*****************************************************************/
	/* is_equal: Compare two images array of size img_size */

	/*****************************************************************/
	boolean is_equal(byte[] img1, byte[] img2, int img_size) {

		int i;

		for (i = 0; i < img_size; i++) {
			if (img1[i] != img2[i])
				return false;
		}

		return true;
	} // end is_equal

	public static void setTo24() {
		DEFAULT_VIDEO_WIDTH = 24;
		DEFAULT_VIDEO_HEIGHT = 24;
	}

	public static void setTo28() {
		DEFAULT_VIDEO_WIDTH = 28;
		DEFAULT_VIDEO_HEIGHT = 28;
	}

	private BufferedImage mat2Img(Mat original) {
		BufferedImage image = null;
		int width = original.width(), height = original.height(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);

		if (original.channels() > 1) {
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		} else {
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

		return image;
	}

	public static Mat img2Mat2(BufferedImage in, int width, int height) {
		Mat out;
		byte[] data;
		int r, g, b;

		if (in.getType() == BufferedImage.TYPE_INT_RGB) {
			out = new Mat(height, width, CvType.CV_8UC3);
			data = new byte[width * height * (int) out.elemSize()];
			int[] dataBuff = in.getRGB(0, 0, width, height, null, 0, width);
			for (int i = 0; i < dataBuff.length; i++) {
				data[i * 3] = (byte) ((dataBuff[i] >> 16) & 0xFF);
				data[i * 3 + 1] = (byte) ((dataBuff[i] >> 8) & 0xFF);
				data[i * 3 + 2] = (byte) ((dataBuff[i] >> 0) & 0xFF);
			}
		} else {
			out = new Mat(height, width, CvType.CV_8UC1);
			data = new byte[width * height * (int) out.elemSize()];
			int[] dataBuff = in.getRGB(0, 0, width, height, null, 0, width);
			for (int i = 0; i < dataBuff.length; i++) {
				r = (byte) ((dataBuff[i] >> 16) & 0xFF);
				g = (byte) ((dataBuff[i] >> 8) & 0xFF);
				b = (byte) ((dataBuff[i] >> 0) & 0xFF);
				//data[i] = (byte)((0.21 * r) + (0.71 * g) + (0.07 * b)); //luminosity
				data[i] = (byte) ((0.31 * r) + (0.21 * g) + (0.37 * b)); //luminosity
			}
		}
		out.put(0, 0, data);
		return out;
	}

	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 *
	 * @param frame
	 * 		the {@link Mat} representing the current frame
	 *
	 * @return the {@link Image} to show
	 */
	public static Image mat2Image(Mat frame) {
		try {
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		} catch (Exception e) {
			System.err.println("Cannot convert the Mat obejct: " + e);
			return null;
		}
	}

	/**
	 * Generic method for putting element running on a non-JavaFX thread on the JavaFX thread, to
	 * properly update the UI
	 *
	 * @param property
	 * 		a {@link ObjectProperty}
	 * @param value
	 * 		the value to set for the given {@link ObjectProperty}
	 */
	public static <T> void onFXThread(final ObjectProperty<T> property, final T value) {
		Platform.runLater(() -> {
			property.set(value);
		});
	}

	/**
	 * Support for the {@link mat2image()} method
	 *
	 * @param original
	 * 		the {@link Mat} object in BGR or grayscale
	 *
	 * @return the corresponding {@link BufferedImage}
	 */
	private static BufferedImage matToBufferedImage(Mat original) {
		// init
		BufferedImage image = null;
		int width = original.width(), height = original.height(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);

		if (original.channels() > 1) {
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		} else {
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

		return image;
	}

}
