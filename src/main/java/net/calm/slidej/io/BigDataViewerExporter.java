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

package net.calm.slidej.io;

import bdv.export.ExportMipmapInfo;
import bdv.export.WriteSequenceToHdf5;
import bdv.img.hdf5.Partition;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.TimePoint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import java.io.File;
import java.util.ArrayList;

public class BigDataViewerExporter {

    public static void export(RandomAccessibleInterval<UnsignedShortType> img, String outputDir, String filename) {
        long[] auxDims = new long[img.numDimensions()];
        img.dimensions(auxDims);

        double[] auxDoubleDims = new double[auxDims.length];

        for (int i = 0; i < auxDims.length; i++) {
            auxDoubleDims[i] = auxDims[i];
        }

        ArrayList<TimePoint> tp = new ArrayList<>();
        tp.add(new TimePoint(1));

        ArrayList<BasicViewSetup> bvs = new ArrayList<>();
        bvs.add(new BasicViewSetup(0, filename, img, new FinalVoxelDimensions("Microns", auxDoubleDims)));

        ArrayList<Partition> partitions = Partition.split(tp, bvs, 0, 0, String.format("%s%s%s", outputDir, File.separator, filename));

        WriteSequenceToHdf5.writeViewToHdf5PartitionFile(img,
                partitions.get(0),
                0,
                0,
                new ExportMipmapInfo(new int[][]{{1, 1, 1}, {2, 2, 2}, {4, 4, 4}, {8, 8, 8}}, new int[][]{{16, 16, 16}, {16, 16, 16}, {16, 16, 16}, {8, 8, 8}}),
                true,
                true,
                null,
                null,
                Runtime.getRuntime().availableProcessors() - 1,
                null);
    }

}
