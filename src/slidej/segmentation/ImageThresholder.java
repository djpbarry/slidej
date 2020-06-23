package slidej.segmentation;

import ij.process.AutoThresholder;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.algorithm.stats.Histogram;
import net.imglib2.algorithm.stats.RealBinMapper;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.img.Img;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

public class ImageThresholder<T extends RealType<T> & NativeType<T>> {
    private final Img<T> input;
    private final String method;
    private final Img<BitType> output;

    public ImageThresholder(final Img<T> input, final String method) {
        this.input = input;
        this.method = method;
        this.output = new DiskCachedCellImgFactory<>(new BitType()).create(input);
    }

    public void threshold() {
        T min = input.firstElement().createVariable();
        T max = input.firstElement().createVariable();
        ComputeMinMax.computeMinMax(input, min, max);
        Histogram<T> hist = new Histogram<>(new RealBinMapper<>(min, max, 256), input.cursor());
        hist.process();
        int threshBin = (new AutoThresholder()).getThreshold(method, hist.getHistogram());

        //(new ImageJ()).op().threshold().apply(output, input, hist.getBinCenter(threshBin));

        thresholdImage(hist.getBinCenter(threshBin));

        //output = Thresholder.threshold(input, hist.getBinCenter(threshBin), true, Runtime.getRuntime().availableProcessors());
        System.out.println("Image thresholded.");
        System.out.println(String.format("%.1f GB of RAM free.", Runtime.getRuntime().freeMemory() / 1e+9));
    }

    private void thresholdImage(T threshold) {
        BitType fg = new BitType();
        BitType bg = new BitType();
        fg.set(true);
        bg.set(false);

        LoopBuilder.setImages(input, output).forEachPixel((in, out) -> out.set(in.getRealFloat() > threshold.getRealFloat() ? fg : bg));
    }

    public Img<BitType> getOutput() {
        return output;
    }
}
