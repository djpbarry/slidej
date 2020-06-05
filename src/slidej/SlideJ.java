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
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.morphology.distance.DistanceTransform;
import net.imglib2.algorithm.morphology.distance.EuclidianDistanceIsotropic;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import slidej.analysis.Analyser;
import slidej.io.ImageLoader;
import slidej.segmentation.ImageThresholder;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SlideJ {

    public static String TITLE = "SlideJ v1.0";

    public SlideJ() {

    }

    public <T extends RealType<T> & NativeType<T>> void load(File file, int series) {
        ImageLoader<T> il = new ImageLoader<>();

        Img<T> img = il.load(file, series);

        ImageMetadata meta = il.getMeta();
        List<CalibratedAxis> axes = meta.getAxes();

        int neighbourhoodSize = 50;

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


}
