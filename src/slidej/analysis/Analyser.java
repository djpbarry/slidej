package slidej.analysis;

import ij.measure.ResultsTable;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;

import java.util.List;

public class Analyser<T extends RealType<T> & NativeType<T>> {
    private final int[] neighbourhoodSize;
    private ResultsTable rt;
    private final String[] dimLabels;
    private final double[] calibrations;

    public Analyser(int[] neighbourhoodSize, String[] dimLabels, double[] calibrations) {
        this.neighbourhoodSize = neighbourhoodSize;
        this.dimLabels = dimLabels;
        this.calibrations = calibrations;
    }

    public void analyse(RandomAccessibleInterval<T> img) {

        long[] dims = new long[img.numDimensions()];

        img.dimensions(dims);

        List<Pair<Interval, long[]>> cells = Grids.collectAllContainedIntervalsWithGridPositions(dims, neighbourhoodSize);

        rt = new ResultsTable(cells.size());

        int nbCPUs = Runtime.getRuntime().availableProcessors();

        AnalyserThread<T>[] ats = new AnalyserThread[nbCPUs];

        int nCellsPerThread = (int) Math.ceil((float)cells.size() / nbCPUs);
        for (int thread = 0; thread < nbCPUs; thread++) {
            int startIndex = thread * nCellsPerThread;
            int endIndex = Math.min(startIndex + nCellsPerThread, cells.size());
            ats[thread] = new AnalyserThread<>(cells.subList(startIndex, endIndex), img, neighbourhoodSize,
                    rt, dimLabels, calibrations, startIndex);
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

    public ResultsTable getRt() {
        return rt;
    }
}
