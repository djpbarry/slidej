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

import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgOpener;
import io.scif.img.ImgSaver;
import net.calm.slidej.io.DiskCacheOptions;
import net.imagej.ImageJ;
import net.imagej.ops.threshold.ThresholdNamespace;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class ImgSaverTest {
    public void run(long[] dimensions) {
        String ioPath = "D:/Debugging/ImgIOTest.ome.tif";
        String ioPath2 = "D:/Debugging/ImgIOTest2.ome.tif";
        String ioPath3 = "D:/Debugging/ImgIOTest3.ome.tif";

        ImgSaver saver = new ImgSaver();
        SCIFIOConfig config = new SCIFIOConfig();
        config.writerSetCompression("LZW");

        ImgOpener opener = new ImgOpener();

        ThresholdNamespace threshold = (new ImageJ()).op().threshold();

        System.out.println("Creating img...");
        Img<UnsignedShortType> img = (new DiskCachedCellImgFactory<>(new UnsignedShortType())).create(dimensions);

        System.out.println("Thresholding img...");
        Img<BitType> binary = (new DiskCachedCellImgFactory<>(new BitType())).create(img);
        binary = (Img<BitType>) threshold.huang(binary, img);

        System.out.println("Converting binary img...");
        Img<UnsignedByteType> convertedBinary = (new DiskCachedCellImgFactory<>(new UnsignedByteType())).create(binary);
        convertedBinary = (new ImageJ()).op().convert().uint8(convertedBinary, binary);

        System.out.println("Saving binary img...");
        saver.saveImg(ioPath2, convertedBinary, config);

        System.out.println("Saving img...");
        saver.saveImg(ioPath, img, config);

        System.out.println("Reopening img...");
        Img<UnsignedShortType> img2 = (Img<UnsignedShortType>) opener.openImgs(ioPath).get(0);

        System.out.println("Rethresholding img...");
        Img<BitType> binary2 = (new DiskCachedCellImgFactory<>(new BitType())).create(img2);
        binary2 = (Img<BitType>) threshold.huang(binary2, img2);

        System.out.println("Reconverting binary img...");
        Img<UnsignedByteType> convertedBinary2 = (new DiskCachedCellImgFactory<>(new UnsignedByteType())).create(binary2);
        convertedBinary2 = (new ImageJ()).op().convert().uint8(convertedBinary2, binary2);

        System.out.println("Resaving binary img...");
        saver.saveImg(ioPath3, convertedBinary2, config);
        System.out.println("Done.");
    }

    public static void main(String args[]) {
        System.out.println("Test 1.");
        (new ImgSaverTest()).run(new long[]{500, 3000, 5});

        System.out.println("Test 2.");
        (new ImgSaverTest()).run(new long[]{5000, 30000, 45});
        System.exit(0);
    }
}
