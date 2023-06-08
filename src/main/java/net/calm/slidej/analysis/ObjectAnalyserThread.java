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

package net.calm.slidej.analysis;

import ij.measure.ResultsTable;
import net.calm.slidej.properties.SlideJParams;
import net.calm.slidej.utils.Utils;
import net.imagej.ops.stats.StatsNamespace;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegionCursor;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.moment.SecondMoment;
import org.apache.commons.math3.stat.descriptive.summary.Product;

import java.util.ArrayList;
import java.util.List;

class ObjectAnalyserThread<T extends RealType<T>> extends Thread {

    private final ResultsTable rt;
    private final List<RandomAccessibleInterval<BoolType>> cells;
    private final RandomAccessibleInterval<T> img;
    private final String[] dimLabels;
    private final int[] dimOrder;
    private final double[] calibrations;
    private final StatsNamespace statSpace;
    private final ArrayList<String> channelNames;

    public ObjectAnalyserThread(final List<RandomAccessibleInterval<BoolType>> cells, final RandomAccessibleInterval<T> img,
                                final ResultsTable rt, final String[] dimLabels, final int[] dimOrder,
                                final double[] calibrations, StatsNamespace statSpace, ArrayList<String> channelNames) {
        this.rt = rt;
        this.cells = cells;
        this.img = img;
        this.dimLabels = dimLabels;
        this.dimOrder = dimOrder;
        this.calibrations = calibrations;
        this.statSpace = statSpace;
        this.channelNames = channelNames;
    }

    public void run() {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        final long[] dims = img.dimensionsAsLongArray();
        int resultsRow = 0;
        long[] regionPos = new long[dims.length - 1];
        for (RandomAccessibleInterval<BoolType> p : cells) {
            if (p instanceof LabelRegion) {
                RealLocalizable com = ((LabelRegion<Integer>) p).getCenterOfMass();
                Integer label = ((LabelRegion<Integer>) p).getLabel();
                rt.setValue("Object ID", resultsRow, label);
                int index = 0;
                for (int d = 0; d < dims.length; d++) {
                    if (!(dimOrder[d] == SlideJParams.C_AXIS)) {
                        rt.setValue(dimLabels[d], resultsRow, (com.getDoublePosition(index++) * calibrations[d]));
                    }
                }
                for (int c = 0; c < dims[dimOrder[SlideJParams.C_AXIS]]; c++) {
                    LabelRegionCursor regionCursor = ((LabelRegion<?>) p).cursor();
                    RandomAccess<T> imageRA = img.randomAccess();
                    stats.clear();
                    while (regionCursor.hasNext()) {
                        regionCursor.localize(regionPos);
                        imageRA.setPosition(new long[]{regionPos[0], regionPos[1], c, regionPos[2]});
                        try {
                            stats.addValue(imageRA.get().getRealDouble());
                        } catch (ArrayIndexOutOfBoundsException e) {
                            Utils.timeStampOutput(String.format("Error accessing location (%d, %d, %d, %d)", regionPos[0], regionPos[1], c, regionPos[2]));
                        }
                        regionCursor.fwd();
                    }
                    rt.setValue(channelNames.get(c) + "_Mean", resultsRow, stats.getMean());
                    rt.setValue(channelNames.get(c) + "_Median", resultsRow, stats.getPercentile(50.0));
                    rt.setValue(channelNames.get(c) + "_Geometric Mean", resultsRow, stats.getGeometricMean());
                    rt.setValue(channelNames.get(c) + "_Kurtosis", resultsRow, stats.getKurtosis());
                    rt.setValue(channelNames.get(c) + "_Max", resultsRow, stats.getMax());
                    rt.setValue(channelNames.get(c) + "_Min", resultsRow, stats.getMin());
                    rt.setValue(channelNames.get(c) + "_Population Variance", resultsRow, stats.getPopulationVariance());
                    rt.setValue(channelNames.get(c) + "_Quadratic Mean", resultsRow, stats.getQuadraticMean());
                    rt.setValue(channelNames.get(c) + "_Skewness", resultsRow, stats.getSkewness());
                    rt.setValue(channelNames.get(c) + "_Standard Deviation", resultsRow, stats.getStandardDeviation());
                    rt.setValue(channelNames.get(c) + "_Sum", resultsRow, stats.getSum());
                    rt.setValue(channelNames.get(c) + "_Sum Squared", resultsRow, stats.getSumsq());
                    rt.setValue(channelNames.get(c) + "_Variance", resultsRow, stats.getVariance());
                    rt.setValue(channelNames.get(c) + "_Product", resultsRow, (new Product()).evaluate(stats.getValues()));
                    rt.setValue(channelNames.get(c) + "_Second Moment", resultsRow, (new SecondMoment()).evaluate(stats.getValues()));
                }
                resultsRow++;
            }
        }
    }
}
