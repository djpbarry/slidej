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
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
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
    private final String BINARIES = "binaries";
    private final String AUX_INPUTS = "aux_inputs";

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

        if (!makeOutputDirectories(new File(file.getParent()), BINARIES, AUX_INPUTS)) {
            System.out.print("Failed to create output directories- aborting.");
            return;
        }

        generateBinariesAndMaps(img,
                String.format("%s%s%s", file.getParent(), File.separator, BINARIES),
                String.format("%s%s%s", file.getParent(), File.separator, AUX_INPUTS),
                caxis, calibrations);

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

    public <T extends RealType<T> & NativeType<T>> void generateBinariesAndMaps(Img<T> img, String binOutDir, String mapOutDir, int caxis, double[] calibrations) {
        long[] dims = new long[img.numDimensions()];
        img.dimensions(dims);

        double[] channelCals = new double[calibrations.length - 1];
        System.arraycopy(calibrations, 0, channelCals, 0, caxis);
        System.arraycopy(calibrations, caxis + 1, channelCals, caxis, channelCals.length - caxis);

        long[] channelDims = new long[img.numDimensions() - 1];
        System.arraycopy(dims, 0, channelDims, 0, caxis);
        System.arraycopy(dims, caxis + 1, channelDims, caxis, channelDims.length - caxis);

        ImgFactory<FloatType> factory = new CellImgFactory<>(new FloatType());

        for (int c = 0; c < dims[caxis]; c++) {
            Img<FloatType> filtered = factory.create(channelDims);
            RandomAccessible<T> channel = Views.extendValue(Views.hyperSlice(img, caxis, c), img.firstElement().createVariable());
            Gauss3.gauss(2.0, channel, filtered);
            Img<BitType> binary = thresholdImg(filtered, "Default");
            saveImage(String.format("%S%Sthreshold_%d.tif", binOutDir, File.separator, c),
                    (new ImageJ()).op().convert().uint8(binary));
            long[] binDims = new long[binary.numDimensions()];
            binary.dimensions(binDims);
            saveImage(String.format("%s%sdistanceMap_%d.tif", mapOutDir, File.separator, c),
                    DistanceTransformer.calcDistanceMap(binary, channelCals, binDims, false));
            saveImage(String.format("%s%sinvertedDistanceMap_%d.tif", mapOutDir, File.separator, c),
                    DistanceTransformer.calcDistanceMap(binary, channelCals, binDims, true));
        }
    }

    public <T extends RealType<T> & NativeType<T>> Img<BitType> thresholdImg(Img<T> img, String method) {
        ImageThresholder<T> it = new ImageThresholder<>(img, method);
        it.threshold();

        return it.getOutput();
    }

    public <T extends NumericType<T> & NativeType<T>> void showImage(RandomAccessibleInterval<T> img) {
        ImageJFunctions.show(img);
    }

    private <T extends RealType<T> & NativeType<T>> void saveImage(String path, Img<T> img) {
        (new ImgSaver()).saveImg(path, img);
    }

    private boolean makeOutputDirectories(File parent, String... children) {
        boolean success = true;
        for (String path : children) {
            success = success && makeOutputDirectory(String.format("%s%s%s", parent.getAbsolutePath(), File.separator, path));
        }
        return success;
    }

    private boolean makeOutputDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return true;
    }
}
