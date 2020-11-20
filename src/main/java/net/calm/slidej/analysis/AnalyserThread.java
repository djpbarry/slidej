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
import net.imagej.ops.stats.StatsNamespace;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.moment.SecondMoment;
import org.apache.commons.math3.stat.descriptive.summary.Product;
import org.apache.commons.math3.stat.descriptive.summary.SumOfLogs;

import java.util.List;

class AnalyserThread<T extends RealType<T>> extends Thread {

    private final ResultsTable rt;
    private final List<Pair<Interval, long[]>> cells;
    private final RandomAccessibleInterval<T> img;
    private final int[] neighbourhoodSize;
    private final String[] dimLabels;
    private final double[] calibrations;
    private final StatsNamespace statSpace;

    public AnalyserThread(final List<Pair<Interval, long[]>> cells, final RandomAccessibleInterval<T> img,
                          final int[] neighbourhoodSize, final ResultsTable rt, final String[] dimLabels,
                          final double[] calibrations, StatsNamespace statSpace) {
        this.rt = rt;
        this.cells = cells;
        this.img = img;
        this.neighbourhoodSize = neighbourhoodSize;
        this.dimLabels = dimLabels;
        this.calibrations = calibrations;
        this.statSpace = statSpace;
    }

    public void run() {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        Cursor<T> c;
        IterableInterval<T> view;
        int resultsRow = 0;
        for (Pair<Interval, long[]> p : cells) {
            view = Views.interval(img, p.getA());
            c = view.cursor();
            stats.clear();
            for (int d = 0; d < p.getB().length; d++) {
                rt.setValue(dimLabels[d], resultsRow, (p.getB()[d] + 1) * calibrations[d] * neighbourhoodSize[d] / 2.0);
            }
            while (c.hasNext()) {
                c.fwd();
                stats.addValue(c.get().getRealDouble());
            }
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
            rt.setValue("ImageJ Geometric Mean", resultsRow, statSpace.geometricMean(view).getRealDouble());
            rt.setValue("ImageJ Harmonic Mean", resultsRow, statSpace.harmonicMean(view).getRealDouble());
            rt.setValue("ImageJ Kurtosis", resultsRow, statSpace.kurtosis(view).getRealDouble());
            rt.setValue("ImageJ Moment 1 About Mean", resultsRow, statSpace.moment1AboutMean(view).getRealDouble());
            rt.setValue("ImageJ Moment 2 About Mean", resultsRow, statSpace.moment2AboutMean(view).getRealDouble());
            rt.setValue("ImageJ Moment 3 About Mean", resultsRow, statSpace.moment3AboutMean(view).getRealDouble());
            rt.setValue("ImageJ Moment 4 About Mean", resultsRow, statSpace.moment4AboutMean(view).getRealDouble());
            rt.setValue("ImageJ Skewness", resultsRow, statSpace.skewness(view).getRealDouble());
//            rt.setValue("ImageJ Sum of Inverses", resultsRow, statSpace.sumOfInverses(view).getRealDouble());
            rt.setValue("ImageJ Sum of Squares", resultsRow, statSpace.sumOfSquares(view).getRealDouble());

            resultsRow++;
        }
    }
}
