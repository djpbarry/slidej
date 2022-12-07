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
import net.imagej.ops.stats.StatsNamespace;
import net.imglib2.*;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegionCursor;
import net.imglib2.roi.labeling.LabelRegionRandomAccess;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.moment.SecondMoment;
import org.apache.commons.math3.stat.descriptive.summary.Product;

import java.util.List;

class ObjectAnalyserThread<T extends RealType<T>> extends Thread {

    private final ResultsTable rt;
    private final List<RandomAccessibleInterval<BoolType>> cells;
    private final RandomAccessibleInterval<T> img;
    private final String[] dimLabels;
    private final int[] dimOrder;
    private final double[] calibrations;
    private final StatsNamespace statSpace;
    private final int channel;

    public ObjectAnalyserThread(final List<RandomAccessibleInterval<BoolType>> cells, final RandomAccessibleInterval<T> img,
                                final ResultsTable rt, final String[] dimLabels, final int[] dimOrder,
                                final double[] calibrations, StatsNamespace statSpace, int channel) {
        this.rt = rt;
        this.cells = cells;
        this.img = img;
        this.dimLabels = dimLabels;
        this.dimOrder = dimOrder;
        this.calibrations = calibrations;
        this.statSpace = statSpace;
        this.channel = channel;
    }

    public void run() {
        IterableInterval<T> view;
        Cursor<T> cursor;
        DescriptiveStatistics stats = new DescriptiveStatistics();
        final long[] dims = img.dimensionsAsLongArray();
        int resultsRow = 0;
        long[] regionPos = new long[dims.length - 1];
        for (RandomAccessibleInterval<BoolType> p : cells) {
            if (p instanceof LabelRegion) {
                RealLocalizable com = ((LabelRegion<Integer>) p).getCenterOfMass();
                Integer label = ((LabelRegion<Integer>) p).getLabel();
                for (int c = 0; c < dims[dimOrder[SlideJParams.C_AXIS]]; c++) {
                    LabelRegionCursor regionCursor = ((LabelRegion<?>) p).cursor();
                    LabelRegionRandomAccess<Integer> regionRA = ((LabelRegion<Integer>) p).randomAccess();
                    RandomAccess<T> imageRA = img.randomAccess();
                    stats.clear();
                    int index = 0;
                    for (int d = 0; d < dims.length; d++) {
                        if (!(dimOrder[d] == SlideJParams.C_AXIS)) {
                            rt.setValue(dimLabels[d], resultsRow, (com.getDoublePosition(index++) * calibrations[d]));
                        }
                    }
                    rt.setValue(dimLabels[dimOrder[SlideJParams.C_AXIS]], resultsRow, c);
                    while (regionCursor.hasNext()) {
                        regionCursor.localize(regionPos);
                        imageRA.setPosition(new long[]{regionPos[0], regionPos[1], c, regionPos[2]});
                        stats.addValue(imageRA.get().getRealDouble());
                        regionCursor.fwd();
                    }
                    rt.setValue("Detection Channel", resultsRow, channel);
                    rt.setValue("Object ID", resultsRow, label);
                    rt.setValue("Mean", resultsRow, stats.getMean());
                    rt.setValue("Median", resultsRow, stats.getPercentile(50.0));
                    rt.setValue("Geometric Mean", resultsRow, stats.getGeometricMean());
                    rt.setValue("Kurtosis", resultsRow, stats.getKurtosis());
                    rt.setValue("Max", resultsRow, stats.getMax());
                    rt.setValue("Min", resultsRow, stats.getMin());
                    rt.setValue("Population Variance", resultsRow, stats.getPopulationVariance());
                    rt.setValue("Quadratic Mean", resultsRow, stats.getQuadraticMean());
                    rt.setValue("Skewness", resultsRow, stats.getSkewness());
                    rt.setValue("Standard Deviation", resultsRow, stats.getStandardDeviation());
                    rt.setValue("Sum", resultsRow, stats.getSum());
                    rt.setValue("Sum Squared", resultsRow, stats.getSumsq());
                    rt.setValue("Variance", resultsRow, stats.getVariance());
                    rt.setValue("Product", resultsRow, (new Product()).evaluate(stats.getValues()));
//            rt.setValue("Sum of Logs", resultsRow, (new SumOfLogs()).evaluate(stats.getValues()));
                    rt.setValue("Second Moment", resultsRow, (new SecondMoment()).evaluate(stats.getValues()));
//                    rt.setValue("ImageJ Geometric Mean", resultsRow, statSpace.geometricMean(view).getRealDouble());
//                    rt.setValue("ImageJ Harmonic Mean", resultsRow, statSpace.harmonicMean(view).getRealDouble());
//                    rt.setValue("ImageJ Kurtosis", resultsRow, statSpace.kurtosis(view).getRealDouble());
//                    rt.setValue("ImageJ Moment 1 About Mean", resultsRow, statSpace.moment1AboutMean(view).getRealDouble());
//                    rt.setValue("ImageJ Moment 2 About Mean", resultsRow, statSpace.moment2AboutMean(view).getRealDouble());
//                    rt.setValue("ImageJ Moment 3 About Mean", resultsRow, statSpace.moment3AboutMean(view).getRealDouble());
//                    rt.setValue("ImageJ Moment 4 About Mean", resultsRow, statSpace.moment4AboutMean(view).getRealDouble());
//                    rt.setValue("ImageJ Skewness", resultsRow, statSpace.skewness(view).getRealDouble());
//                    rt.setValue("ImageJ Sum of Inverses", resultsRow, statSpace.sumOfInverses(view).getRealDouble());
//                    rt.setValue("ImageJ Sum of Squares", resultsRow, statSpace.sumOfSquares(view).getRealDouble());

                    resultsRow++;
                }
            }
        }
    }
}
