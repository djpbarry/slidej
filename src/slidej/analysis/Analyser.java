package slidej.analysis;

import java.util.Arrays;

import ij.measure.ResultsTable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import net.imglib2.Cursor;
import net.imglib2.algorithm.region.localneighborhood.RectangleNeighborhoodGPL;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;

public class Analyser<T extends RealType<T> & NativeType<T>> {
    private final long[] neighbourhoodSize;
    private final ResultsTable rt = new ResultsTable();

    public Analyser(long[] neighbourhoodSize) {
        this.neighbourhoodSize = neighbourhoodSize;
    }

    public void analyse(RandomAccessibleInterval<T> img) {

        RectangleNeighborhoodGPL<T> r = (new RectangleNeighborhoodGPL<>(img));

        r.setSpan(neighbourhoodSize);

        long[] dims = new long[img.numDimensions()];

        img.dimensions(dims);

        RandomAccess<T> curs = img.randomAccess();

        long[] pos;

        DescriptiveStatistics stats = new DescriptiveStatistics();

        int resultsRow = 0;

        for (long x = neighbourhoodSize[0]; x < dims[0] - neighbourhoodSize[0]; x += neighbourhoodSize[0]) {
            for (long y = neighbourhoodSize[1]; y < dims[1] - neighbourhoodSize[1]; y += neighbourhoodSize[1]) {
                for (long c = neighbourhoodSize[2]; c < dims[2] - neighbourhoodSize[2]; c += neighbourhoodSize[2]) {
                    for (long z = neighbourhoodSize[3]; z < dims[3] - neighbourhoodSize[3]; z += neighbourhoodSize[3]) {
                        pos = new long[]{x, y, c, z};
                        curs.setPosition(pos);
                        r.move(curs);
                        Cursor<T> rc = r.cursor();

                        rt.setValue("X", resultsRow, x);
                        rt.setValue("Y", resultsRow, y);
                        rt.setValue("Z", resultsRow, z);

                        stats.clear();

                        while (rc.hasNext()) {
                            rc.fwd();
                            T pix = rc.get();
                            stats.addValue(pix.getRealDouble());

                        }
                        rt.setValue("Mean_"+c, resultsRow, stats.getMean());
                        resultsRow++;
                    }
                }
            }
        }
    }

    private void measure() {

    }

    public ResultsTable getRt() {
        return rt;
    }
}
