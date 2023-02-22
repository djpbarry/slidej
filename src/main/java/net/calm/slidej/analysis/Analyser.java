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
import net.imagej.ImageJ;
import net.imagej.ops.stats.StatsNamespace;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;

import java.util.List;

public class Analyser<T extends RealType<T>> {
    private final int[] neighbourhoodSize;
    private final String[] dimLabels;
    private final double[] calibrations;
    private ResultsTable[] rt;
    private final int[] dimOrder;
    private Img<FloatType>[][] outputs;
    private final boolean coloc;

    public Analyser(int[] neighbourhoodSize, String[] dimLabels, double[] calibrations, int[] dimOrder, boolean coloc) {
        this.neighbourhoodSize = neighbourhoodSize;
        this.dimLabels = dimLabels;
        this.calibrations = calibrations;
        this.dimOrder = dimOrder;
        this.coloc = coloc;
    }

    public void analyse(RandomAccessibleInterval<T> img) {

        long[] dims = new long[img.numDimensions()];

        img.dimensions(dims);

        if (coloc) {

            final ImgFactory<FloatType> imgFactory = new CellImgFactory<>(new FloatType(), SlideJParams.CELL_IMG_DIM);

            outputs = new Img[2][(((int) dims[dimOrder[SlideJParams.C_AXIS]] - 1) * (int) dims[dimOrder[SlideJParams.C_AXIS]]) / 2];

            for (int i = 0; i < outputs.length; i++) {
                for (int j = 0; j < outputs[i].length; j++) {
                    outputs[i][j] = imgFactory.create((int) Math.ceil((float) dims[dimOrder[SlideJParams.X_AXIS]] / neighbourhoodSize[dimOrder[SlideJParams.X_AXIS]]),
                            (int) Math.ceil((float) dims[dimOrder[SlideJParams.Y_AXIS]] / neighbourhoodSize[dimOrder[SlideJParams.Y_AXIS]]),
                            (int) Math.ceil((float) dims[dimOrder[SlideJParams.Z_AXIS]] / neighbourhoodSize[dimOrder[SlideJParams.Z_AXIS]]));
                }
            }
        }

        List<Pair<Interval, long[]>> cells = Grids.collectAllContainedIntervalsWithGridPositions(dims, neighbourhoodSize);

        int nbCPUs = Runtime.getRuntime().availableProcessors();

        rt = new ResultsTable[nbCPUs];

        Thread[] ats = new Thread[nbCPUs];

        int nCellsPerThread = (int) Math.ceil((float) cells.size() / nbCPUs);
        StatsNamespace stats = new ImageJ().op().stats();
        for (int thread = 0; thread < nbCPUs; thread++) {
            rt[thread] = new ResultsTable();
            int startIndex = thread * nCellsPerThread;
            int endIndex = Math.min(startIndex + nCellsPerThread, cells.size());
            if (!coloc) {
                ats[thread] = new AnalyserThread<T>(cells.subList(startIndex, endIndex), img, neighbourhoodSize,
                        rt[thread], dimLabels, calibrations, stats);
            } else {
                ats[thread] = new CorrelationThread<T>(cells.subList(startIndex, endIndex), img, neighbourhoodSize,
                        rt[thread], dimLabels, calibrations, dimOrder, outputs);
            }
            ats[thread].start();
        }
        try {
            for (int thread = 0; thread < nbCPUs; thread++) {
                ats[thread].join();
            }
        } catch (InterruptedException ie) {
            System.out.println(String.format("Thread %d was interrupted:\n %s", Thread.currentThread().getId(), ie));
        }

    }

    private void measure() {

    }

    public ResultsTable[] getRt() {
        return rt;
    }

    public Img<FloatType>[][] getOutputs() {
        return outputs;
    }
}
