package slidej.segmentation;

import ij.process.AutoThresholder;
import net.imglib2.algorithm.binary.Thresholder;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.algorithm.stats.Histogram;
import net.imglib2.algorithm.stats.RealBinMapper;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

public class ImageThresholder<T extends RealType<T> & NativeType<T>> {
    private final Img<T> input;
    private final String method;
    private Img<BitType> output;

    public ImageThresholder(final Img<T> input, final String method){
        this.input = input;
        this.method = method;
    }

    public void threshold(){
        T min = input.firstElement().createVariable();
        T max = input.firstElement().createVariable();
        ComputeMinMax.computeMinMax(input, min, max);
        Histogram hist = new Histogram(new RealBinMapper(min, max, 256), input.cursor());
        hist.process();
        int threshBin = (new AutoThresholder()).getThreshold(method, hist.getHistogram());
        output = Thresholder.threshold(input, (T)hist.getBinCenter(threshBin), true, Runtime.getRuntime().availableProcessors());
    }

    public Img<BitType> getOutput() {
        return output;
    }
}
