/*
 * Copyright (c)  2020, David J. Barry
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
