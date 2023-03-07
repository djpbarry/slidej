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

package net.calm.slidej.analysis;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import net.calm.slidej.io.ImageSaver;
import net.calm.slidej.util.Utils;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;
import org.apache.commons.io.FileUtils;
import sc.fiji.analyzeSkeleton.AnalyzeSkeleton_;
import sc.fiji.analyzeSkeleton.Point;
import sc.fiji.analyzeSkeleton.SkeletonResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class SkeletonAnalyserThread<T extends RealType<T>> extends Thread {

    private final ResultsTable rt;
    private final List<Pair<Interval, long[]>> cells;
    private final RandomAccessibleInterval<T> img;
    private final String regionsName;
    private final String tmpDir;

    public SkeletonAnalyserThread(final List<Pair<Interval, long[]>> cells, final RandomAccessibleInterval<T> img,
                                  final ResultsTable rt, final String regionsName, String tmpDir) {
        this.rt = rt;
        this.cells = cells;
        this.img = img;
        this.regionsName = regionsName;
        this.tmpDir = tmpDir;
    }

    public void run() {
        int row = 0;
        RandomAccessibleInterval<T> view;
        int index = 0;
        for (Pair<Interval, long[]> p : cells) {
            long[] offsets = p.getA().minAsLongArray();
            view = Views.interval(img, p.getA());
//            ImageJFunctions.show(view);
//            ImagePlus skelImp = ImageJFunctions.wrapBit(view, String.format("Binary_%s", regionsName));
            String tmpFileName = String.format("%s%stemp_skel_imp_file_%d_%d.ome.tiff", tmpDir, File.separator,
                    Thread.currentThread().getId(), index);
            IJ.save(ImageJFunctions.wrapBit(view, String.format("Binary_%s", regionsName)), tmpFileName);
            //ImageSaver.saveImage(tmpFileName, ImgView.wrap(view));
            ImagePlus skelImp = IJ.openImage(tmpFileName);
            AnalyzeSkeleton_ analyser = new AnalyzeSkeleton_();
            analyser.setup("", skelImp);
            SkeletonResult skelResult = analyser.run(AnalyzeSkeleton_.NONE, false, false, null,
                    true, true);
            ArrayList<Point> junctions = skelResult.getListOfJunctionVoxels();
            for (Point pt : junctions) {
                rt.setValue("X", row, pt.x + offsets[0]);
                rt.setValue("Y", row, pt.y + offsets[1]);
                rt.setValue("Z", row, pt.z + offsets[2]);
                rt.setLabel("Junction", row);
                row++;
            }

            ArrayList<Point> ends = skelResult.getListOfEndPoints();
            for (Point pt : ends) {
                rt.setValue("X", row, pt.x + offsets[0]);
                rt.setValue("Y", row, pt.y + offsets[1]);
                rt.setValue("Z", row, pt.z + offsets[2]);
                rt.setLabel("End", row);
                row++;
            }
            System.out.printf("Thread %d: %.1f%% done.\n", Thread.currentThread().getId(),
                    (100.0 * ++index / cells.size()));
            skelImp.close();
            try {
                FileUtils.delete(new File(tmpFileName));
            } catch (IOException e) {
                Utils.timeStampOutput("Could not delete temporary file.");
            }
        }
    }
}
