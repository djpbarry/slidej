package slidej.segmentation;

import net.imagej.ops.threshold.ThresholdNamespace;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.img.Img;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ImageThresholder<T extends RealType<T> & NativeType<T>> {
    private final Img<T> input;
    private final String method;
    private Img<BitType> output;

    public ImageThresholder(final Img<T> input, final String method) {
        this.input = input;
        this.method = method;
        this.output = (new CellImgFactory<>(new BitType())).create(input);
    }

    public void threshold() {
        T min = input.randomAccess().get();
        T max = input.randomAccess().get();
        ComputeMinMax.computeMinMax(input, min, max);
        //Histogram<T> hist = new Histogram<>(new RealBinMapper<>(min, max, 256), input.randomAccess());
        //hist.process();
        //int threshBin = (new AutoThresholder()).getThreshold(method, hist.getHistogram());

        try {
            Method threshMethod = ThresholdNamespace.class.getDeclaredMethod(method.toLowerCase());
            output = (Img<BitType>) threshMethod.invoke(input);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            System.out.println(String.format("Could not threshold image: %s", e.toString()));
        }

        //thresholdImage(hist.getBinCenter(threshBin));

        //output = Thresholder.threshold(input, hist.getBinCenter(threshBin), true, Runtime.getRuntime().availableProcessors());
    }

    private void thresholdImage(T threshold) {
        BitType fg = new BitType();
        BitType bg = new BitType();
        fg.set(true);
        bg.set(false);

        //LoopBuilder.setImages(input, output).multiThreaded().forEachPixel((in, out) -> out.set(in.getRealFloat() > threshold.getRealFloat() ? fg : bg));
    }

    public Img<BitType> getOutput() {
        return output;
    }
}
