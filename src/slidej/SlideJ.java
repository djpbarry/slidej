package slidej;

import UtilClasses.GenUtils;
import io.scif.ImageMetadata;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultAxisType;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import slidej.analysis.Analyser;
import slidej.io.ImageLoader;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SlideJ {

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
            IO.DataWriter.saveResultsTable(a.getRt(), new File(file.getAbsolutePath() + "_results.csv"));
        } catch (IOException e) {
            GenUtils.logError(e, "Could not save results file.");
        }
    }

    public <T extends NumericType<T> & NativeType<T>> void showImage(RandomAccessibleInterval<T> img) {
        ImageJFunctions.show(img);
    }


}
