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
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.Arrays;
import java.util.List;

class CorrelationThread<T extends RealType<T>> extends Thread {

    private final ResultsTable rt;
    private final List<Pair<Interval, long[]>> cells;
    private final RandomAccessibleInterval<T> img;
    private final int[] neighbourhoodSize;
    private final String[] dimLabels;
    private final double[] calibrations;
    private final int[] dimOrder;
    private final Img<FloatType>[][] outputs;

    public CorrelationThread(final List<Pair<Interval, long[]>> cells, final RandomAccessibleInterval<T> img,
                             final int[] neighbourhoodSize, final ResultsTable rt, final String[] dimLabels,
                             final double[] calibrations, final int[] dimOrder, final Img<FloatType>[][] outputs) {
        this.rt = rt;
        this.cells = cells;
        this.img = img;
        this.neighbourhoodSize = neighbourhoodSize;
        this.dimLabels = dimLabels;
        this.calibrations = calibrations;
        this.dimOrder = dimOrder;
        this.outputs = outputs;
    }

    public void run() {
        Cursor<T> c;
        int resultsRow = 0;
        for (Pair<Interval, long[]> p : cells) {
            c = Views.interval(img, p.getA()).localizingCursor();
            if (c.getIntPosition(dimOrder[SlideJParams.C_AXIS]) == 0) {
                for (int d = 0; d < p.getB().length; d++) {
                    rt.setValue(dimLabels[d], resultsRow, (p.getB()[d] + 1) * calibrations[d] * neighbourhoodSize[d] / 2.0);
                }
                float[][] coeffs = calcCorrelations(c, img.randomAccess(), img.numDimensions(), (int) img.dimension(dimOrder[SlideJParams.C_AXIS]));
                int index = 0;
                for (int chan = c.getIntPosition(dimOrder[SlideJParams.C_AXIS]); chan < img.dimension(dimOrder[SlideJParams.C_AXIS]) - 1; chan++) {
                    for (int chan2 = chan + 1; chan2 < img.dimension(dimOrder[SlideJParams.C_AXIS]); chan2++) {
                        RandomAccess<FloatType> r = outputs[0][index].randomAccess();
                        r.setPosition(new long[]{p.getB()[dimOrder[SlideJParams.X_AXIS]], p.getB()[dimOrder[SlideJParams.Y_AXIS]], p.getB()[dimOrder[SlideJParams.Z_AXIS]]});
                        r.get().set(coeffs[0][index]);

                        RandomAccess<FloatType> r2 = outputs[1][index].randomAccess();
                        r2.setPosition(new long[]{p.getB()[dimOrder[SlideJParams.X_AXIS]], p.getB()[dimOrder[SlideJParams.Y_AXIS]], p.getB()[dimOrder[SlideJParams.Z_AXIS]]});
                        r2.get().set(coeffs[1][index]);

                        rt.setValue(String.format("PC_%d_%d", chan, chan2), resultsRow, coeffs[0][index]);
                        rt.setValue(String.format("SC_%d_%d", chan, chan2), resultsRow, coeffs[1][index]);
                        index++;
                    }
                }
                resultsRow++;
            }
        }
    }

    float[][] calcCorrelations(Cursor<T> c, RandomAccess<T> r, int nDims, int nC) {
        int channel = c.getIntPosition(dimOrder[SlideJParams.C_AXIS]);
        float[][] coeffs = new float[2][((nC - 1) * nC) / 2];
        for (float[] coeff : coeffs) {
            Arrays.fill(coeff, Float.NaN);
        }
        DescriptiveStatistics[] stats = new DescriptiveStatistics[nC];
        PearsonsCorrelation pc = new PearsonsCorrelation();
        SpearmansCorrelation sc = new SpearmansCorrelation();
        for (int s = 0; s < stats.length; s++) {
            stats[s] = new DescriptiveStatistics();
        }
        while (c.hasNext()) {
            c.fwd();
            stats[channel].addValue(c.get().getRealDouble());
            for (int chan = channel + 1; chan < nC; chan++) {
                r.setPosition(c);
                r.setPosition(chan, dimOrder[SlideJParams.C_AXIS]);
                stats[chan].addValue(r.get().getRealDouble());
            }
        }
        int index = 0;
        for (int chan = channel; chan < nC - 1; chan++) {
            for (int chan2 = chan + 1; chan2 < nC; chan2++) {
                coeffs[0][index] = (float) pc.correlation(stats[chan].getValues(), stats[chan2].getValues());
                coeffs[1][index] = (float) sc.correlation(stats[chan].getValues(), stats[chan2].getValues());
                index++;
            }
        }
        return coeffs;
    }
}
