package slidej.analysis;

import ij.measure.ResultsTable;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.moment.SecondMoment;
import org.apache.commons.math3.stat.descriptive.summary.Product;
import org.apache.commons.math3.stat.descriptive.summary.SumOfLogs;

import java.util.List;

class AnalyserThread<T extends RealType<T> & NativeType<T>> extends Thread {

    private final ResultsTable rt;
    private final List<Pair<Interval, long[]>> cells;
    private final RandomAccessibleInterval<T> img;
    private final int[] neighbourhoodSize;
    private final String[] dimLabels;
    private final double[] calibrations;

    public AnalyserThread(final List<Pair<Interval, long[]>> cells, final RandomAccessibleInterval<T> img,
                          final int[] neighbourhoodSize, final ResultsTable rt, final String[] dimLabels,
                          final double[] calibrations) {
        this.rt = rt;
        this.cells = cells;
        this.img = img;
        this.neighbourhoodSize = neighbourhoodSize;
        this.dimLabels = dimLabels;
        this.calibrations = calibrations;
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
            rt.setValue("Sum of Logs", resultsRow, (new SumOfLogs()).evaluate(stats.getValues()));
            rt.setValue("Second Moment", resultsRow, (new SecondMoment()).evaluate(stats.getValues()));
            resultsRow++;
        }
    }
}