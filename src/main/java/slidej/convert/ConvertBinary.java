package slidej.convert;

import net.imglib2.img.Img;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ConvertBinary {
    public static <T extends BooleanType<T>, R extends RealType<R> & NativeType<R>> Img<R> convertBinary(Img<T> input, R type) {
        Img<R> converted = (new CellImgFactory<>(type)).create(input);

        R max = type.createVariable();
        R min = type.createVariable();

        max.setReal(type.getMaxValue());
        min.setReal(type.getMinValue());

        LoopBuilder.setImages(input, converted).multiThreaded().forEachPixel((in, out) -> out.set(in.get() ? max : min));

        return converted;
    }
}
