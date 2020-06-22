package slidej;

import IO.PropertyWriter;
import UtilClasses.GenUtils;
import ij.measure.ResultsTable;
import io.scif.ImageMetadata;
import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgSaver;
import net.imagej.ImageJ;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultAxisType;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.apache.commons.io.FileUtils;
import slidej.analysis.Analyser;
import slidej.io.ImageLoader;
import slidej.properties.SlideJParams;
import slidej.segmentation.ImageThresholder;
import slidej.transform.DistanceTransformer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class SlideJ {

    private final String BINARIES = "binaries";
    private final String AUX_INPUTS = "aux_inputs";
    private final SlideJParams props;

    public SlideJ(File propsLocation) {
        props = new SlideJParams();
        try {
            if (propsLocation != null) {
                PropertyWriter.loadProperties(props, null, propsLocation);
            }
        } catch (IOException | InterruptedException | InvocationTargetException e) {
            GenUtils.logError(e, "Failed to load properties file.");
        }
    }

    public <T extends RealType<T> & NativeType<T>> void load(File file, int series, int neighbourhoodSize) {
        System.out.println(String.format("%d processors available.", Runtime.getRuntime().availableProcessors()));

        props.setProperty(SlideJParams.RAW_INPUT, file.getParent());

        System.out.println(String.format("Loading %s", file.getAbsolutePath()));
        System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory()/1e+9));

        ImageLoader<T> il = new ImageLoader<>();
        Img<T> img = il.load(file, series);
        ImageMetadata meta = il.getMeta();
        List<CalibratedAxis> axes = meta.getAxes();

        System.out.println("Calibrating...");

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

        System.out.println("Creating output directories...");

        String output;
        String binaryOutputs = null;
        String mapOutputs = null;

        try {
            output = makeOutputDirectories(new File(file.getParent()), String.format("%s_output", file.getName())).get(0);
            ArrayList<String> children = makeOutputDirectories(new File(output), BINARIES, AUX_INPUTS);
            binaryOutputs = children.get(0);
            mapOutputs = children.get(1);
            mapOutputs = children.get(1);
            props.setProperty(SlideJParams.OUTPUT, output);
            props.setProperty(SlideJParams.AUX_INPUT, mapOutputs);
            props.setProperty(SlideJParams.BIN_INPUT, binaryOutputs);
        } catch (IndexOutOfBoundsException e) {
            System.out.print("Failed to create output directories- aborting.");
        }

        System.out.println("Thresholding and generating distance maps...");

        generateBinariesAndMaps(img,
                binaryOutputs,
                mapOutputs,
                caxis, calibrations);

        System.out.println("Loading distance maps...");

        RandomAccessibleInterval<T> auxs = il.loadAndConcatenate(
                new File(mapOutputs), caxis);
        Analyser<T> a = new Analyser<>(calNeighbourhood, dimLabels, calibrations);

        System.out.println("Analysing intensities in all channels...");

        a.analyse(Views.concatenate(caxis, img, auxs));

        System.out.println("Saving results...");

        try {
            ResultsTable[] rt = a.getRt();
            File outputData = new File(file.getAbsolutePath() + "_results.csv");
            if (outputData.exists() && !outputData.delete()) throw new IOException("Cannot delete existing output file.");
            for (int i = 0; i < rt.length; i++) {
                IO.DataWriter.saveResultsTable(rt[i], new File(file.getAbsolutePath() + "_results.csv"), true, i == 0);
            }
        } catch (IOException e) {
            GenUtils.logError(e, "Could not save results file.");
        }
        saveAnalysisParameters();
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

        ImgFactory<FloatType> factory = new DiskCachedCellImgFactory<>(new FloatType());

        for (int c = 0; c < dims[caxis]; c++) {
            if (!Boolean.parseBoolean(props.getChannelProperty(SlideJParams.THRESHOLD_CHANNEL, c, SlideJParams.DEFAULT_THRESHOLD_CHANNEL)))
                continue;
            System.out.println(String.format("Filtering channel %d.", c));
            Img<FloatType> filtered = factory.create(channelDims);
            RandomAccessible<T> channel = Views.extendValue(Views.hyperSlice(img, caxis, c), img.firstElement().createVariable());
            Gauss3.gauss(getSigma(3, c, calibrations), channel, filtered);
            System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory()/1e+9));
            System.out.println(String.format("Thresholding channel %d.", c));
            Img<BitType> binary = thresholdImg(filtered,
                    props.getChannelProperty(SlideJParams.THRESHOLD, c, SlideJParams.DEFAULT_THRESHOLD_METHOD));
            System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory()/1e+9));
            saveImage(String.format("%S%Sthreshold_%d.tif", binOutDir, File.separator, c),
                    (new ImageJ()).op().convert().uint8(binary));
            long[] binDims = new long[binary.numDimensions()];
            binary.dimensions(binDims);
            System.out.println(String.format("Calculating distance map 1 for channel %d.", c));
            saveImage(String.format("%s%sdistanceMap_%d.tif", mapOutDir, File.separator, c),
                    DistanceTransformer.calcDistanceMap(binary, channelCals, binDims, false));
            System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory()/1e+9));
            System.out.println(String.format("Calculating distance map 2 for channel %d.", c));
            saveImage(String.format("%s%sinvertedDistanceMap_%d.tif", mapOutDir, File.separator, c),
                    DistanceTransformer.calcDistanceMap(binary, channelCals, binDims, true));
            System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory()/1e+9));
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
        SCIFIOConfig config = new SCIFIOConfig();
        config.writerSetCompression("LZW");
        (new ImgSaver()).saveImg(path, img, config);
    }

    private ArrayList<String> makeOutputDirectories(File parent, String... children) {
        ArrayList<String> output = new ArrayList<>();
        for (String path : children) {
            String fullPath = String.format("%s%s%s", parent.getAbsolutePath(), File.separator, path);
            if (makeOutputDirectory(String.format("%s%s%s", parent.getAbsolutePath(), File.separator, path))) {
                output.add(fullPath);
            }
        }
        return output;
    }

    private boolean makeOutputDirectory(String path) {
        File dir = new File(path);
        try {
            if (dir.exists()) {
                FileUtils.cleanDirectory(dir);
            } else {
                dir.mkdirs();
            }
            return true;
        } catch (IOException e) {
            GenUtils.logError(e, "Failed to clean output directories");
            return false;
        }
    }

    private boolean saveAnalysisParameters() {
        try {
            PropertyWriter.saveProperties(props, props.getProperty(SlideJParams.OUTPUT), SlideJParams.TITLE, true);
        } catch (Exception e) {
            GenUtils.logError(e, "Failed to save property file.");
            return false;
        }
        return true;
    }

    double[] getSigma(int nAxis, int c, double[] cal) {
        double[] sigma = new double[nAxis];

        for (int d = 0; d < nAxis; d++) {
            sigma[d] = Double.parseDouble(
                    props.getChannelProperty(
                            SlideJParams.FILTER_RADIUS,
                            c,
                            SlideJParams.DEFAULT_FILTER_RADIUS
                    )
            ) / cal[d];
        }

        return sigma;
    }
}
