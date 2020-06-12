package slidej;

import UtilClasses.GenUtils;
import ij.measure.ResultsTable;
import io.scif.ImageMetadata;
import io.scif.img.ImgSaver;
import net.imagej.ImageJ;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultAxisType;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import slidej.analysis.Analyser;
import slidej.io.ImageLoader;
import slidej.segmentation.ImageThresholder;
import slidej.transform.DistanceTransformer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SlideJ {

    public static String TITLE = "SlideJ v1.0";

    public SlideJ() {

    }

    public <T extends RealType<T> & NativeType<T>> void load(File file, int series, int neighbourhoodSize) {
        ImageLoader<T> il = new ImageLoader<>();

        Img<T> img = il.load(file, series);

        ImageMetadata meta = il.getMeta();
        List<CalibratedAxis> axes = meta.getAxes();

        int[] calNeighbourhood = new int[img.numDimensions()];

        double[] calibrations = new double[img.numDimensions()];

        int caxis = -1;

        String[] dimLabels = new String[img.numDimensions()];

        for (int i = 0; i < calNeighbourhood.length; i++) {
            CalibratedAxis axis = axes.get(i);
            AxisType type = axis.type();
            if (axis instanceof DefaultLinearAxis) {
                calNeighbourhood[i] = (int) Math.round(axis.rawValue(neighbourhoodSize));
                calibrations[i] = ((DefaultLinearAxis) axis).scale();
            }
            if (type instanceof DefaultAxisType) {
                dimLabels[i] = type.getLabel();
            }
            if (dimLabels[i].equalsIgnoreCase("Channel")) {
                caxis = i;
            }
        }

        calNeighbourhood[caxis] = 1;
        calibrations[caxis] = 2;

        ImageThresholder<T> it = new ImageThresholder<>(ImgView.wrap(Views.hyperSlice(img, caxis, 0), img.factory()), "Default");
        it.threshold();

        Img<BitType> binary = it.getOutput();

        saveImage("E:/Dropbox (The Francis Crick)/Antoniana's Data/TestOut/threshold.tif",
                (new ImageJ()).op().convert().uint8(binary));

        long[] dims = new long[binary.numDimensions()];
        binary.dimensions(dims);

        double[] channelCals = new double[calibrations.length - 1];
        System.arraycopy(calibrations, 0, channelCals, 0, caxis);
        System.arraycopy(calibrations, caxis + 1, channelCals, caxis, channelCals.length - caxis);

        saveImage("E:/Dropbox (The Francis Crick)/Antoniana's Data/TestOut/distanceMap.tif",
                DistanceTransformer.calcDistanceMap(binary, channelCals, dims, false));

        saveImage("E:/Dropbox (The Francis Crick)/Antoniana's Data/TestOut/invertedDistanceMap.tif",
                DistanceTransformer.calcDistanceMap(binary, channelCals, dims, true));

        Analyser<T> a = new Analyser<>(calNeighbourhood, dimLabels, calibrations);

        a.analyse(img);

        try {
            ResultsTable[] rt = a.getRt();
            File output = new File(file.getAbsolutePath() + "_results.csv");
            if (output.exists() && !output.delete()) throw new IOException("Cannot delete existing output file.");
            for (int i = 0; i < rt.length; i++) {
                IO.DataWriter.saveResultsTable(rt[i], new File(file.getAbsolutePath() + "_results.csv"), true, i == 0);
            }
        } catch (IOException e) {
            GenUtils.logError(e, "Could not save results file.");
        }
    }

    public <T extends NumericType<T> & NativeType<T>> void showImage(RandomAccessibleInterval<T> img) {
        ImageJFunctions.show(img);
    }

    private <T extends RealType<T> & NativeType<T>> void saveImage(String path, Img<T> img) {
        (new ImgSaver()).saveImg(path, img);
    }

}
