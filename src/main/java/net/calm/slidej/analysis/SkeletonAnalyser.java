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
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;

import java.util.List;

public class SkeletonAnalyser<T extends RealType<T>> {
    private final int[] neighbourhoodSize;
    private ResultsTable[] rt;
    private final String regionsName;
    private final String tmpDir;

    public SkeletonAnalyser(int[] neighbourhoodSize, final String regionsName, final String tmpDir) {
        this.neighbourhoodSize = neighbourhoodSize;
        this.regionsName = regionsName;
        this.tmpDir = tmpDir;
    }

    public void analyse(RandomAccessibleInterval<T> img) {
        long[] dims = new long[img.numDimensions()];
        img.dimensions(dims);
        List<Pair<Interval, long[]>> cells = Grids.collectAllContainedIntervalsWithGridPositions(dims, neighbourhoodSize);
        int nbCPUs = Runtime.getRuntime().availableProcessors();
        int nThreads = Math.min(nbCPUs, cells.size());
        float nCellsPerThread = ((float) cells.size()) / nThreads;
        rt = new ResultsTable[nThreads];
        Thread[] ats = new Thread[nThreads];
        int startIndex;
        int endIndex = 0;
        for (int thread = 0; thread < nThreads; thread++) {
            rt[thread] = new ResultsTable();
            startIndex = endIndex;
            if (startIndex >= cells.size()) break;
            endIndex = Math.min(Math.round((thread + 1) * nCellsPerThread), cells.size());
            ats[thread] = new SkeletonAnalyserThread<>(cells.subList(startIndex, endIndex),
                    img, rt[thread], regionsName, tmpDir);
            ats[thread].start();
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
