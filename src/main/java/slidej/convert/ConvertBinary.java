package slidej.convert;

import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.img.Img;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import slidej.io.DiskCacheOptions;

public class ConvertBinary {
    public static <T extends BooleanType<T>, R extends RealType<R> & NativeType<R>> Img<R> convertBinary(Img<T> input, R type) {
        Img<R> converted = (new DiskCachedCellImgFactory<>(type, new DiskCacheOptions().getOptions())).create(input);

        R max = type.createVariable();
        R min = type.createVariable();

        max.setReal(type.getMaxValue());
        min.setReal(type.getMinValue());

        LoopBuilder.setImages(input, converted).forEachPixel((in, out) -> out.set(in.get() ? max : min));

        return converted;
    }
}
