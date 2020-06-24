package slidej.binary;

import net.imglib2.img.Img;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.BooleanType;

public class Inverter {

    public static <T extends BooleanType<T>> Img<T> invertImage(Img<T> input) {
        Img<T> inverted = input.factory().create(input);

        LoopBuilder.setImages(input, inverted).forEachPixel((in, out) -> out.set(!in.get()));

        return inverted;
    }
}
