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

package net.calm.slidej.segmentation;

import ij.process.AutoThresholder;
import net.calm.slidej.properties.SlideJParams;
import net.calm.slidej.util.Utils;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.algorithm.stats.Histogram;
import net.imglib2.algorithm.stats.RealBinMapper;
import net.imglib2.img.Img;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import java.nio.file.Path;

public class ImageThresholder {
    private final Img<UnsignedShortType> input;
    private final String method;
    private Img<BitType> output;

    public ImageThresholder(final Img<UnsignedShortType> input, Path tmpDir, final String method) {
        this.input = input;
        this.method = method;
        this.output = (new CellImgFactory<>(new BitType(), SlideJParams.CELL_IMG_DIM)).create(input);
    }

    public ImageThresholder(final Img<UnsignedShortType> input) {
        this.input = input;
        this.method = "Manual";
        this.output = (new CellImgFactory<>(new BitType(), SlideJParams.CELL_IMG_DIM)).create(input);
    }

    public void threshold() {
        UnsignedShortType min = new UnsignedShortType();
        UnsignedShortType max = new UnsignedShortType();
        ComputeMinMax.computeMinMax(input, min, max);
        Histogram<UnsignedShortType> hist = new Histogram<>(new RealBinMapper<>(min, max, 256), input);
        hist.process();
        int threshBin = (new AutoThresholder()).getThreshold(method, hist.getHistogram());

//        Method[] methods = ThresholdNamespace.class.getDeclaredMethods();
//        Method threshMethod = null;
//        for (Method m : methods) {
//            if (m.getName().equalsIgnoreCase(method) && m.getParameterCount() == 2
//                    && m.getParameterTypes()[0].getName().equalsIgnoreCase(IterableInterval.class.getName())
//                    && m.getParameterTypes()[1].getName().equalsIgnoreCase(IterableInterval.class.getName())) {
//                threshMethod = m;
//            }
//        }

//        if (threshMethod == null) {
//            System.out.println(String.format("%s is not a valid threshold method.", method));
//            return;
//        }

//        try {
//            output = (Img<BitType>) threshMethod.invoke((new ImageJ().op().threshold()), output, input);
//        } catch (IllegalAccessException | InvocationTargetException e) {
//            System.out.println(String.format("Could not threshold image: %s", e.toString()));
//        }

        thresholdImage(hist.getBinCenter(threshBin));

//        output = Thresholder.threshold(input, hist.getBinCenter(threshBin), true, Runtime.getRuntime().availableProcessors());
    }

    public void thresholdImage(UnsignedShortType threshold) {
        Utils.timeStampOutput(String.format("Binarising with threshold value of %d", threshold.getInteger()));
        BitType fg = new BitType();
        BitType bg = new BitType();
        fg.set(true);
        bg.set(false);

        LoopBuilder.setImages(input, output).multiThreaded().forEachPixel((in, out) -> out.set(in.get() > threshold.getInteger() ? fg : bg));
    }

    public Img<BitType> getOutput() {
        return output;
    }
}
