package slidej.analysis;

import ij.measure.ResultsTable;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;

public class Analyser<T extends RealType<T> & NativeType<T>> {
    private final int[] neighbourhoodSize;
    private final ResultsTable rt = new ResultsTable();
    private final String[] dimLabels;
    private final double[] calibrations;

    public Analyser(int[] neighbourhoodSize, String[] dimLabels, double[] calibrations) {
        this.neighbourhoodSize = neighbourhoodSize;
        this.dimLabels = dimLabels;
        this.calibrations = calibrations;
    }

    public void analyse(RandomAccessibleInterval<T> img) {

//        RectangleNeighborhoodGPL<T> r = (new RectangleNeighborhoodGPL<>(img));
//
//        r.setSpan(neighbourhoodSize);

        long[] dims = new long[img.numDimensions()];

        img.dimensions(dims);

        List<Pair<Interval, long[]>> cells = Grids.collectAllContainedIntervalsWithGridPositions(dims, neighbourhoodSize);

        DescriptiveStatistics stats = new DescriptiveStatistics();

        int resultsRow = 0;

        Cursor<T> c;

        IterableInterval<T> view;



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
            resultsRow++;
        }


//        RandomAccess<T> curs = img.randomAccess();
//
//        long[] pos;
//
//
//
//        for (long x = neighbourhoodSize[0]; x < dims[0] - neighbourhoodSize[0]; x += stepSize[0]) {
//            for (long y = neighbourhoodSize[1]; y < dims[1] - neighbourhoodSize[1]; y += stepSize[1]) {
//                for (long z = neighbourhoodSize[3]; z < dims[3] - neighbourhoodSize[3]; z += stepSize[3]) {
//                    rt.setValue("X", resultsRow, x);
//                    rt.setValue("Y", resultsRow, y);
//                    rt.setValue("Z", resultsRow, z);
//                    for (long c = neighbourhoodSize[2]; c < dims[2] - neighbourhoodSize[2]; c += stepSize[2]) {
//                        pos = new long[]{x, y, c, z};
//                        curs.setPosition(pos);
//                        r.move(curs);
//                        Cursor<T> rc = r.cursor();
//
//                        stats.clear();
//
//                        while (rc.hasNext()) {
//                            rc.fwd();
//                            T pix = rc.get();
//                            stats.addValue(pix.getRealDouble());
//
//                        }
//                        rt.setValue("Mean_" + c, resultsRow, stats.getMean());
//                        System.out.println(x + " " + y + " " + z + " " + c);
//                    }
//                    resultsRow++;
//                }
//            }
//        }
    }

    private void measure() {

    }

    public ResultsTable getRt() {
        return rt;
    }
}
