/*
 * Copyright (c)  2020, David J. Barry
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.calm.slidej;

import ij.measure.ResultsTable;
import io.scif.ImageMetadata;
import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgSaver;
import net.calm.iaclasslibrary.IO.DataWriter;
import net.calm.iaclasslibrary.IO.PropertyWriter;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import net.calm.slidej.analysis.Analyser;
import net.calm.slidej.io.ImageLoader;
import net.calm.slidej.properties.SlideJParams;
import net.calm.slidej.segmentation.ImageThresholder;
import net.calm.slidej.transform.DistanceTransformer;
import net.imagej.ImageJ;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultAxisType;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import org.apache.commons.io.FileUtils;

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
        System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));

        ImageLoader<T> il = new ImageLoader<>();
        Img<T> img = il.load(file, series);
        ImageMetadata meta = il.getMeta();
        List<CalibratedAxis> axes = meta.getAxes();

        System.out.println(String.format("%s loaded.", file.getAbsolutePath()));
        System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));

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

        System.out.println("Loading aux channels and concatanating datset...");

        RandomAccessibleInterval<T> concat = Views.concatenate(caxis, img, auxs);

        System.out.println("Done.");
        System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));
        System.out.println("Analysing intensities in all channels...");

        a.analyse(concat);

        System.out.println("Saving results...");

        try {
            ResultsTable[] rt = a.getRt();
            File outputData = new File(file.getAbsolutePath() + "_results.csv");
            if (outputData.exists() && !outputData.delete())
                throw new IOException("Cannot delete existing output file.");
            for (int i = 0; i < rt.length; i++) {
                DataWriter.saveResultsTable(rt[i], new File(file.getAbsolutePath() + "_results.csv"), true, i == 0);
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

        for (int c = 0; c < dims[caxis]; c++) {
            if (!Boolean.parseBoolean(props.getChannelProperty(SlideJParams.THRESHOLD_CHANNEL, c, SlideJParams.DEFAULT_THRESHOLD_CHANNEL)))
                continue;
            RandomAccessibleInterval<T> channel = Views.hyperSlice(img, caxis, c);
            System.out.println("Channel Img Type: " + channel.getClass());
            System.out.println("Pixel Type: " + Util.getTypeFromInterval(channel).getClass());
            ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval<T>> extendedChannelView = Views.extendValue(channel, img.firstElement().createVariable());
            System.out.println("Extended Channel Img Type: " + extendedChannelView.getClass());
            System.out.println(String.format("Filtering channel %d.", c));
            System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));
            Gauss3.gauss(getSigma(channel.numDimensions(), c, calibrations), extendedChannelView, channel);
            System.out.println(String.format("Filtering done."));
            System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));
            System.out.println(String.format("Thresholding channel %d.", c));
            Img<BitType> binary = thresholdImg(ImgView.wrap(channel, Util.getSuitableImgFactory(extendedChannelView.getSource(), channel.randomAccess().get())),
                    props.getChannelProperty(SlideJParams.THRESHOLD, c, SlideJParams.DEFAULT_THRESHOLD_METHOD));
            System.out.println("Img Type: " + binary.getClass());
            System.out.println("Pixel Type: " + Util.getTypeFromInterval(binary).getClass());
            System.out.println(String.format("Thresholding done."));
            System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));
            System.out.println(String.format("Converting thresholded image."));
            Img<UnsignedByteType> convBin = (new ImageJ()).op().convert().uint8(binary);
            System.out.println("Img Type: " + convBin.getClass());
            System.out.println("Pixel Type: " + Util.getTypeFromInterval(convBin).getClass());
            System.out.println(String.format("Saving thresholded image for channel %d.", c));
            saveImage(String.format("%S%Sthreshold_%d.ome.tif", binOutDir, File.separator, c), convBin);
            System.out.println(String.format("Saved."));
            System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));
            long[] binDims = new long[binary.numDimensions()];
            binary.dimensions(binDims);
            System.out.println(String.format("Calculating distance map 1 for channel %d.", c));
            Img<FloatType> dm1 = DistanceTransformer.calcDistanceMap(binary, channelCals, binDims, false);
            System.out.println("Img Type: " + dm1.getClass());
            System.out.println("Pixel Type: " + Util.getTypeFromInterval(dm1).getClass());
            System.out.println(String.format("Calculated."));
            System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));
            System.out.println(String.format("Saving distance map 1 for channel %d.", c));
            saveImage(String.format("%s%sdistanceMap_%d.ome.tif", mapOutDir, File.separator, c), dm1);
            System.out.println(String.format("Saved."));
            System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));
            System.out.println(String.format("Calculating distance map 2 for channel %d.", c));
            Img<FloatType> dm2 = DistanceTransformer.calcDistanceMap(binary, channelCals, binDims, true);
            System.out.println("Img Type: " + dm2.getClass());
            System.out.println("Pixel Type: " + Util.getTypeFromInterval(dm2).getClass());
            System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));
            System.out.println(String.format("Saving distance map 2 for channel %d.", c));
            saveImage(String.format("%s%sinvertedDistanceMap_%d.ome.tif", mapOutDir, File.separator, c), dm2);
            System.out.println(String.format("Saved."));
            System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));
        }
    }

    public <T extends RealType<T> & NativeType<T>> Img<BitType> thresholdImg(Img<T> img, String method) {
        ImageThresholder<T> it = new ImageThresholder<>(img, method);
        it.threshold();

        return it.getOutput();
    }

    public <T extends NumericType<T> & NativeType<T>> void showImage(RandomAccessibleInterval<T> img) {
        // ImageJFunctions.show(img);
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
