/**
 * 
 */
package slidej.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.scif.ImageMetadata;
import io.scif.Metadata;
import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgOpener;
import io.scif.img.SCIFIOImgPlus;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;

/**
 * 
 * @author david.barry@crick.ac.uk
 *
 */
public class ImageLoader <T extends NumericType<T> & NativeType<T>> {

	private ImageMetadata meta;

	public ImageLoader() {

	}

	public Img<T> load(File file, int series) {
		SCIFIOConfig config = new SCIFIOConfig();
		config.imgOpenerSetIndex(series);

		SCIFIOImgPlus<?> sciImg = new ImgOpener().openImgs(file.getAbsolutePath(), config).get(0);
		this.meta = sciImg.getImageMetadata();

		return (Img<T>)sciImg;
	}

	public ImageMetadata getMeta() {
		return meta;
	}
}