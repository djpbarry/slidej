package slidej.transform;

import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.real.FloatType;
import slidej.binary.Inverter;

public class DistanceTransformer {

    public static Img<FloatType> calcDistanceMap(Img<BitType> binary, double[] cals, long[] dims, boolean inverted) {
        OpService os = (new ImageJ()).op();
        ImgFactory<FloatType> factory = new CellImgFactory<>(new FloatType());

        if (!inverted) {
            return ImgView.wrap(os.image().distancetransform(binary, cals), factory);
        } else {
            //RandomAccessibleInterval<BitType> invertedBinary = new CellImgFactory<>(new BitType()).create(dims);
            //IterableInterval<BitType> invertedBinaryInterval = Views.iterable(invertedBinary);
            //invertedBinaryInterval = os.image().invert(invertedBinaryInterval, Views.iterable(binary));

            Img<BitType> invertedBinary = Inverter.invertImage(binary);

            return ImgView.wrap(os.image().distancetransform(invertedBinary, cals), factory);
        }
    }

}
