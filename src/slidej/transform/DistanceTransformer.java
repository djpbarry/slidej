package slidej.transform;

import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class DistanceTransformer {
    public static Img<FloatType> calcDistanceMap(Img<BitType> binary, double[] cals, long[] dims) {
        OpService os = (new ImageJ()).op();

        ImgFactory<FloatType> factory = new CellImgFactory<>(new FloatType());
        ImgFactory<BitType> factory2 = new CellImgFactory<>(new BitType());

        RandomAccessibleInterval<BitType> invertedBinary = factory2.create(dims);
        IterableInterval<BitType> invertedBinaryInterval = Views.iterable(invertedBinary);
        invertedBinaryInterval = os.image().invert(invertedBinaryInterval, Views.iterable(binary));
        return ImgView.wrap(os.image().distancetransform(invertedBinary, cals), factory);
    }
}
