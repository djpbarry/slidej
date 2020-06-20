/**
 * 
 */
package slidej.io;

import io.scif.ImageMetadata;
import io.scif.SCIFIO;
import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgOpener;
import io.scif.img.SCIFIOImgPlus;
import io.scif.services.DatasetIOService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

import java.io.File;
import java.util.ArrayList;

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
		config.imgOpenerSetImgModes(SCIFIOConfig.ImgMode.CELL);

		SCIFIOImgPlus<?> sciImg = new ImgOpener().openImgs(file.getAbsolutePath(), config).get(0);
		this.meta = sciImg.getImageMetadata();

		return (Img<T>)sciImg;
	}

	public RandomAccessibleInterval<T> loadAndConcatenate(File dir, int concatAxis){
		File[] inputs = dir.listFiles();
		ArrayList<RandomAccessibleInterval<T>> imgs = new ArrayList<>();
		DatasetIOService io = (new SCIFIO()).datasetIO();
		for(File f:inputs){
			if(io.canOpen(f.getAbsolutePath())){
				imgs.add(Views.addDimension(load(f, 0), 0, 0));
			}
		}
		RandomAccessibleInterval<T> concatImgs = Views.concatenate(imgs.get(0).numDimensions() - 1, imgs);
		return Views.moveAxis(concatImgs, concatImgs.numDimensions() - 1, concatAxis);
	}

	public ImageMetadata getMeta() {
		return meta;
	}
}