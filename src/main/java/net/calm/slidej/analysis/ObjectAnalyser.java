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
import net.imagej.ImageJ;
import net.imagej.ops.stats.StatsNamespace;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import java.util.ArrayList;

public class ObjectAnalyser<T extends RealType<T>> {
    private final String[] dimLabels;
    private final double[] calibrations;
    private ResultsTable[] rt;
    private final int[] dimOrder;
    private final ArrayList<String> channelNames;

    public ObjectAnalyser(String[] dimLabels, double[] calibrations, int[] dimOrder, ArrayList<String> channelNames) {
        this.dimLabels = dimLabels;
        this.calibrations = calibrations;
        this.dimOrder = dimOrder;
        this.channelNames = channelNames;
    }

    public void analyse(RandomAccessibleInterval<T> img, ArrayList<RandomAccessibleInterval<BoolType>> regions) {

        long[] dims = new long[img.numDimensions()];

        img.dimensions(dims);

        int nbCPUs = Runtime.getRuntime().availableProcessors();

        int nThreads = Math.min(nbCPUs, regions.size());

        rt = new ResultsTable[nThreads];

        Thread[] ats = new Thread[nThreads];

        int nCellsPerThread = (int) Math.ceil((float) regions.size() / nThreads);
        StatsNamespace stats = new ImageJ().op().stats();
        int startIndex = 0;
        for (int thread = 0; thread < nThreads && startIndex < regions.size(); thread++) {
            rt[thread] = new ResultsTable();
            int endIndex = Math.min(startIndex + nCellsPerThread, regions.size() - 1);
            ats[thread] = new ObjectAnalyserThread<T>(regions.subList(startIndex, endIndex), img,
                    rt[thread], dimLabels, dimOrder, calibrations, stats, channelNames);
            ats[thread].start();
            startIndex += nCellsPerThread;
        }
        try {
            for (int thread = 0; thread < nThreads; thread++) {
                if (ats[thread] != null) ats[thread].join();
            }
        } catch (InterruptedException ie) {
            System.out.println(String.format("Thread %d was interrupted:\n %s", Thread.currentThread().getId(), ie));
        }

    }

    public ResultsTable[] getRt() {
        return rt;
    }
}
