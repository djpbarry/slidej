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
        ImageLoader il = new ImageLoader<T>();

        Img<T> img = il.load(file, series);

        ImageMetadata meta = il.getMeta();
        List<CalibratedAxis> axes = meta.getAxes();

        int neighbourhoodSize = 8;

        long[] calNeighbourhood = new long[img.numDimensions()];

        int caxis = -1;

        for (int i = 0; i < calNeighbourhood.length; i++) {
            CalibratedAxis axis = axes.get(i);
            AxisType type = axis.type();
            if (axis instanceof DefaultLinearAxis) {
                calNeighbourhood[i] = (long) Math.round(axis.rawValue(neighbourhoodSize));
            }
            if (type instanceof DefaultAxisType && type.getLabel().equalsIgnoreCase("Channel")) {
                caxis = i;
            }
        }

        calNeighbourhood[caxis] = 1;

        Analyser<T> a = new Analyser<>(calNeighbourhood);


        // IntervalView<T> channel = Views.hyperSlice(img, 2, 0);

        //long[] minDims = new long[channel.numDimensions()];
        //long[] maxDims = new long[channel.numDimensions()];

        //  channel.min(minDims);
        //  channel.max(maxDims);

        // for (int i = 0; i < channel.numDimensions(); i++) {
        //     minDims[i] += neighbourhoodSize;
        //      maxDims[i] -= neighbourhoodSize;
        //  }

        // FinalInterval internalView = new FinalInterval(minDims, maxDims);

        //  a.analyse(Views.interval(channel, internalView));

        a.analyse(img);

        try {
            IO.DataWriter.saveResultsTable(a.getRt(), new File(file.getAbsolutePath() + "_results.csv"));
        } catch (IOException e) {
            GenUtils.logError(e, "Could not save results file.");
        }
//		showImage(img);
    }

    public <T extends NumericType<T> & NativeType<T>> void showImage(RandomAccessibleInterval<T> img) {
        ImageJFunctions.show(img);
    }
}
