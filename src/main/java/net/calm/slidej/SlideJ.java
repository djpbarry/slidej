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
import io.scif.ome.OMEMetadata;
import net.calm.iaclasslibrary.IO.DataWriter;
import net.calm.iaclasslibrary.IO.PropertyWriter;
import net.calm.iaclasslibrary.TimeAndDate.TimeAndDate;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import net.calm.slidej.analysis.Analyser;
import net.calm.slidej.analysis.ObjectAnalyser;
import net.calm.slidej.convert.ConvertBinary;
import net.calm.slidej.io.ImageLoader;
import net.calm.slidej.properties.SlideJParams;
import net.calm.slidej.segmentation.ImageThresholder;
import net.calm.slidej.transform.DistanceTransformer;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultAxisType;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.morphology.StructuringElements;
import net.imglib2.algorithm.morphology.TopHat;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;

public class SlideJ {

    private final String BINARIES = "binaries";
    private final String AUX_INPUTS = "aux_inputs";
    private final Path tmpDir;
    private final SlideJParams props;
    private final ArrayList<RandomAccessibleInterval<UnsignedShortType>> maps = new ArrayList<>();
    private final ArrayList<String> channelNames = new ArrayList<>();
    private final LinkedHashMap<String, ArrayList<RandomAccessibleInterval<BoolType>>> regions = new LinkedHashMap<>();

    public SlideJ(File propsLocation, Path tmpDir) {
        props = new SlideJParams();
        try {
            if (propsLocation != null) {
                PropertyWriter.loadProperties(props, null, propsLocation);
            }
        } catch (IOException | InterruptedException | InvocationTargetException e) {
            GenUtils.logError(e, "Failed to load properties file.");
        }
        this.tmpDir = tmpDir;
    }

    public void load(File file, int series, int neighbourhoodSize) {
        System.out.println(String.format("%d processors available.", Runtime.getRuntime().availableProcessors()));

        props.setProperty(SlideJParams.RAW_INPUT, file.getParent());
        props.setProperty(SlideJParams.NEIGHBOURHOOD, String.valueOf(neighbourhoodSize));

        System.out.println(String.format("Loading %s", file.getAbsolutePath()));
        System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));

        ImageLoader<UnsignedShortType> il = new ImageLoader<>();
        Img<UnsignedShortType> img = il.load(file, series);
        ImageMetadata meta = il.getMeta();
        OMEMetadata omeMeta = il.getOmeMeta();

        for (int c = 0; c < omeMeta.getRoot().getChannelCount(0); c++) {
            channelNames.add(omeMeta.getRoot().getChannelFluor(0, c));
        }

        List<CalibratedAxis> axes = meta.getAxes();

        System.out.println(String.format("%s loaded.", file.getAbsolutePath()));
        System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));

        System.out.println("Img Type: " + img.getClass());
        System.out.println("Pixel Type: " + Util.getTypeFromInterval(img).getClass());

        System.out.println("Calibrating...");

        int[] calNeighbourhood = new int[img.numDimensions()];
        double[] calibrations = new double[img.numDimensions()];
        int[] axisOrder = new int[SlideJParams.N_AXIS];
        Arrays.fill(axisOrder, -1);
        String[] dimLabels = new String[img.numDimensions()];

        for (int i = 0; i < calNeighbourhood.length; i++) {
            CalibratedAxis axis = axes.get(i);
            AxisType type = axis.type();
            if (axis instanceof DefaultLinearAxis) {
                ((DefaultLinearAxis) axis).setOrigin(0.0);
                calNeighbourhood[i] = (int) Math.round(axis.rawValue(neighbourhoodSize));
                calibrations[i] = ((DefaultLinearAxis) axis).scale();
            }
            if (type instanceof DefaultAxisType) {
                dimLabels[i] = type.getLabel();
            }
            if (dimLabels[i].equalsIgnoreCase("Channel")) {
                axisOrder[SlideJParams.C_AXIS] = i;
            } else if (dimLabels[i].equalsIgnoreCase("Z")) {
                axisOrder[SlideJParams.Z_AXIS] = i;
            } else if (dimLabels[i].equalsIgnoreCase("X")) {
                axisOrder[SlideJParams.X_AXIS] = i;
            } else if (dimLabels[i].equalsIgnoreCase("Y")) {
                axisOrder[SlideJParams.Y_AXIS] = i;
            }
        }

        calNeighbourhood[axisOrder[SlideJParams.C_AXIS]] = 1;
        if (!Boolean.parseBoolean(props.getProperty(SlideJParams.DO_3D)))
            calNeighbourhood[axisOrder[SlideJParams.Z_AXIS]] = 1;
        calibrations[axisOrder[SlideJParams.C_AXIS]] = 2;

        System.out.println("Creating output directories...");

        String output;
        String binaryOutputs = null;
        String mapOutputs = null;

        try {
            output = makeOutputDirectories(new File(file.getParent()), String.format("%s_SlideJ_%s", file.getName(), TimeAndDate.getCurrentTimeAndDate().replace('/', '-').replace(':', '-'))).get(0);
            ArrayList<String> children = makeOutputDirectories(new File(output), BINARIES, AUX_INPUTS);
            binaryOutputs = children.get(0);
            mapOutputs = children.get(1);
            props.setProperty(SlideJParams.OUTPUT, output);
            props.setProperty(SlideJParams.AUX_INPUT, mapOutputs);
            props.setProperty(SlideJParams.BIN_INPUT, binaryOutputs);

//            FileUtils.copyFile(file, new File(String.format("%S%Sthreshold_3.ome.btf", binaryOutputs, File.separator)));
        } catch (IndexOutOfBoundsException e) {
            System.out.print("Failed to create output directories- aborting.");
        }

        System.out.println("Thresholding and generating distance maps...");

//        generateBinariesAndMaps(img,
//                binaryOutputs,
//                mapOutputs,
//                caxis, calibrations);
        generateDistanceMaps(img, mapOutputs, binaryOutputs, axisOrder[SlideJParams.C_AXIS], calibrations);

        System.out.println("Concatenating distance maps...");
//
////        RandomAccessibleInterval<T> auxs = il.loadAndConcatenate(
////                new File(mapOutputs), caxis);

        RandomAccessibleInterval<UnsignedShortType> auxs = il.concatenate(
                maps, axisOrder[SlideJParams.C_AXIS]);

        Analyser<UnsignedShortType> a = new Analyser<>(calNeighbourhood, dimLabels, calibrations, axisOrder, Boolean.parseBoolean(props.getProperty(SlideJParams.COLOC)));

//        System.out.println("Loading aux channels and concatanating datset...");

        RandomAccessibleInterval<UnsignedShortType> concat = Views.concatenate(axisOrder[SlideJParams.C_AXIS], img, auxs);

        System.out.println("Done.");
        System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));
        System.out.println("Analysing intensities in all channels...");

        a.analyse(concat);

        System.out.println("Saving results...");

        try {
            ResultsTable[] rt = a.getRt();
            File outputData = new File(String.format("%s%s%s_results.csv", props.getProperty(SlideJParams.OUTPUT), File.separator, file.getName()));
            if (outputData.exists() && !outputData.delete())
                throw new IOException("Cannot delete existing output file.");
            for (int i = 0; i < rt.length; i++) {
                DataWriter.saveResultsTable(rt[i], outputData, true, i == 0);
            }
        } catch (IOException e) {
            GenUtils.logError(e, "Could not save results file.");
        }

        for (Map.Entry<String, ArrayList<RandomAccessibleInterval<BoolType>>> entry : regions.entrySet()) {
            analyseObjects(entry.getValue(), concat, calibrations, axisOrder, dimLabels, file, entry.getKey());
        }

        if (Boolean.parseBoolean(props.getProperty(SlideJParams.COLOC))) {

            Img<FloatType>[][] outputs = a.getOutputs();

            int index = 0;
            for (int chan = 0; chan < img.dimension(axisOrder[SlideJParams.C_AXIS]) - 1; chan++) {
                for (int chan2 = chan + 1; chan2 < img.dimension(axisOrder[SlideJParams.C_AXIS]); chan2++) {
                    saveImage(String.format("%s%sPC_%d_%d.ome.tiff", props.getProperty(SlideJParams.OUTPUT), File.separator, chan, chan2), outputs[0][index]);
                    saveImage(String.format("%s%sSC_%d_%d.ome.tiff", props.getProperty(SlideJParams.OUTPUT), File.separator, chan, chan2), outputs[1][index]);
                    index++;
                }
            }
        }

        saveAnalysisParameters();
    }

    private void generateDistanceMaps(Img<UnsignedShortType> img,
                                      String mapOutDir, String binOutDir,
                                      int caxis, double[] calibrations) {
        long[] dims = new long[img.numDimensions()];
        img.dimensions(dims);

        ImgSaver saver = new ImgSaver();
        SCIFIOConfig config = new SCIFIOConfig();
        config.writerSetCompression("LZW");

        double[] channelCals = new double[calibrations.length - 1];
        System.arraycopy(calibrations, 0, channelCals, 0, caxis);
        System.arraycopy(calibrations, caxis + 1, channelCals, caxis, channelCals.length - caxis);

        for (int c = 0; c < dims[caxis]; c++) {
            if (!Boolean.parseBoolean(props.getChannelProperty(SlideJParams.THRESHOLD_CHANNEL, c, SlideJParams.DEFAULT_THRESHOLD_CHANNEL)))
                continue;
            System.out.println(String.format("Processing channel %d...", c));
            RandomAccessibleInterval<UnsignedShortType> channel = Views.hyperSlice(img, caxis, c);

            System.out.println("Filtering...");
            Img<UnsignedShortType> filtered = (new DiskCachedCellImgFactory<>(new UnsignedShortType())).create(channel);
            Gauss3.gauss(getSigma(channel.numDimensions(), c, channelCals), Views.extendValue(channel, img.firstElement().createVariable()), filtered);

            if (Boolean.parseBoolean(props.getChannelProperty(SlideJParams.TOP_HAT, c, SlideJParams.DEFAULT_TH_CHANNEL))) {
                System.out.println("Top-hat filtering...");
                Img<UnsignedShortType> thFiltered = TopHat.topHat(filtered, StructuringElements.rectangle(getSpan(channel.numDimensions(), c, channelCals, SlideJParams.TOP_HAT, SlideJParams.DEFAULT_TH_FILTER_RADIUS)), Runtime.getRuntime().availableProcessors());
//                try {
//                    saver.saveImg(String.format("%s%stop_hat_filtered_%d.ome.btf", binOutDir, File.separator, c), thFiltered, config);
//                } catch (Exception e) {
//                    System.out.println("Saving failed.");
//                    System.out.println(e.toString());
//                    System.out.println(e.getMessage());
//                }
                filtered = thFiltered;
            }
//            String[] methods = AutoThresholder.getMethods();
//            for (String method : methods) {

            System.out.println("Thresholding...");
            Img<BitType> binary = thresholdImg(filtered, props.getChannelProperty(SlideJParams.THRESHOLD, c, SlideJParams.DEFAULT_THRESHOLD_METHOD));

            System.out.println("Labelling connected components...");
            Img<UnsignedShortType> labelled = (new DiskCachedCellImgFactory<>(new UnsignedShortType())).create(binary);
            ConnectedComponents.labelAllConnectedComponents(binary, labelled, ConnectedComponents.StructuringElement.EIGHT_CONNECTED);
            regions.put(channelNames.get(c), getRegionsList(labelled));

//                Img<BitType> binary = thresholdImg(filtered, method);
            System.out.println("Converting binary image...");
            Img<UnsignedByteType> convertedBinary = ConvertBinary.convertBinary(binary, tmpDir);

            System.out.println("Saving...");
            try {
                saver.saveImg(String.format("%S%S%sthreshold_%d.ome.btf", binOutDir, File.separator,
                        props.getChannelProperty(SlideJParams.THRESHOLD, c, SlideJParams.DEFAULT_THRESHOLD_METHOD), c), convertedBinary, config);
                saver.saveImg(String.format("%S%Slabeling_%d.ome.btf", binOutDir, File.separator, c), labelled, config);
            } catch (Exception e) {
                System.out.println("Saving failed.");
                System.out.println(e.toString());
                System.out.println(e.getMessage());
            }

            System.out.println("Calculating distance map 1...");
            Img<UnsignedShortType> dm1 = DistanceTransformer.calcDistanceMap(binary, channelCals, tmpDir, false);

            maps.add(dm1);
            channelNames.add(channelNames.get(c) + "_" + "DistanceMap");

            System.out.println("Calculating distance map 2...");
            Img<UnsignedShortType> dm2 = DistanceTransformer.calcDistanceMap(binary, channelCals, tmpDir, true);

            System.out.println("Saving...");
            saver.saveImg(String.format("%s%sdistanceMap_%d%s", mapOutDir, File.separator, c, SlideJParams.OUTPUT_FILE_EXT), dm2, config);
//            }
            maps.add(dm2);
            channelNames.add(channelNames.get(c) + "_" + "InvertedDistanceMap");
        }
        return;
    }

    public Img<BitType> thresholdImg(Img<UnsignedShortType> img, String method) {
        ImageThresholder it = new ImageThresholder(img, tmpDir, method);
        it.threshold();

        return it.getOutput();
    }

    public <T extends NumericType<T> & NativeType<T>> void showImage(RandomAccessibleInterval<T> img) {
        // ImageJFunctions.show(img);
    }

    private <T extends RealType> void saveImage(String path, Img<T> img) {
        SCIFIOConfig config = new SCIFIOConfig();
        config.writerSetCompression("LZW");
        //config.parserSetSaveOriginalMetadata(true);

        (new ImgSaver()).saveImg(path, img, config);
    }

    private ArrayList<String> makeOutputDirectories(File parent, String... children) {
        ArrayList<String> output = new ArrayList<>();
        for (String path : children) {
            String fullPath = String.format("%s%s%s", parent.getAbsolutePath(), File.separator, path);
            fullPath = GenUtils.openResultsDirectory(fullPath);
            if (fullPath != null) {
                output.add(fullPath);
            }
        }
        return output;
    }

    private boolean makeOutputDirectory(String path) {
        File dir = new File(path);
        GenUtils.openResultsDirectory(path);
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

    int[] getSpan(int nAxis, int c, double[] cal, String propName, String defaultPropValue) {
        int[] span = new int[nAxis];

        for (int d = 0; d < nAxis; d++) {
            span[d] = (int) Math.round(
                    Double.parseDouble(
                            props.getChannelProperty(
                                    SlideJParams.TH_FILTER_RADIUS,
                                    c,
                                    SlideJParams.DEFAULT_TH_FILTER_RADIUS
                            )
                    ) / cal[d]);
        }

        return span;
    }

    private ArrayList<RandomAccessibleInterval<BoolType>> getRegionsList(RandomAccessibleInterval<UnsignedShortType> img) {
        final Dimensions dims = img;
        final UnsignedShortType t = new UnsignedShortType();
        final RandomAccessibleInterval<UnsignedShortType> labelImg = Util.getArrayOrCellImgFactory(dims, t).create(dims, t);
        ImgLabeling<Integer, UnsignedShortType> labelingImg = new ImgLabeling<Integer, UnsignedShortType>(labelImg);

        final Cursor<LabelingType<Integer>> labelCursor = Views.flatIterable(labelingImg).cursor();

        for (final UnsignedShortType input : Views.flatIterable(img)) {
            final LabelingType<Integer> element = labelCursor.next();
            if (input.getRealFloat() != 0) {
                element.add((int) input.getRealFloat());
            }
        }

        // create list of regions
        LabelRegions<Integer> labelRegions = new LabelRegions<Integer>(labelingImg);
        ArrayList<RandomAccessibleInterval<BoolType>> regionsList = new ArrayList<RandomAccessibleInterval<BoolType>>();

        Object[] regionsArr = labelRegions.getExistingLabels().toArray();
        for (int i = 0; i < labelRegions.getExistingLabels().size(); i++) {
            LabelRegion<Integer> lr = labelRegions.getLabelRegion((Integer) regionsArr[i]);

            regionsList.add(lr);
        }
        return regionsList;
    }

    void analyseObjects(ArrayList<RandomAccessibleInterval<BoolType>> regions, RandomAccessibleInterval<UnsignedShortType> img,
                        double[] calibrations, int[] axisOrder, String[] dimLabels, File file, String channel) {
        ObjectAnalyser<UnsignedShortType> a = new ObjectAnalyser<>(dimLabels, calibrations, axisOrder, channelNames);

//        System.out.println("Loading aux channels and concatanating datset...");

        System.out.println("Done.");
        System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));
        System.out.println("Analysing intensities in all channels...");

        a.analyse(img, regions);

        System.out.println("Saving results...");

        try {
            ResultsTable[] rt = a.getRt();
            File outputData = new File(String.format("%s%s%s_%s_object_results.csv", props.getProperty(SlideJParams.OUTPUT),
                    File.separator, file.getName(), channel));
            if (outputData.exists() && !outputData.delete())
                throw new IOException("Cannot delete existing output file.");
            for (int i = 0; i < rt.length; i++) {
                if (rt[i] != null) DataWriter.saveResultsTable(rt[i], outputData, true, i == 0);
            }
        } catch (IOException e) {
            GenUtils.logError(e, "Could not save results file.");
        }
    }
}
