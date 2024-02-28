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

package net.calm.slidej;

import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import net.calm.slidej.utils.Utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {

        System.setProperty("java.awt.headless", "true");
        File file = new File("E:/Dropbox (The Francis Crick)/Debugging/BrainSaw/inputs/C1-section_20_Probabilities_crop.ome.tiff");
        File props = new File("E:/Dropbox (The Francis Crick)/Debugging/BrainSaw/props/step_properties_ilastik.xml");
        Path tmpDir = Paths.get("C:/cache");
        int[] neighbourhoodSize = new int[]{512, 512, 10};
        int series = 0;

        try {
            for (int i = 0; i < args.length - 1; i++) {
                switch (args[i]) {
                    case "-f":
                        file = new File(args[i + 1]);
                        break;
                    case "-n":
                        String[] neighbourhood = args[i + 1].split(",");
                        neighbourhoodSize = new int[neighbourhood.length];
                        for (int j = 0; j < neighbourhood.length; j++) {
                            neighbourhoodSize[j] = Integer.parseInt(neighbourhood[j]);
                        }
                        break;
                    case "-s":
                        series = Integer.parseInt(args[i + 1]);
                        break;
                    case "-p":
                        props = new File(args[i + 1]);
                        break;
                    case "-t":
                        tmpDir = Paths.get(args[i + 1]);
                        break;
                    default:

                }
            }
        } catch (NumberFormatException e) {
            GenUtils.logError(e, "Invalid numeric argument - using default value.");
        } catch (NullPointerException e) {
            GenUtils.logError(e, "Invalid filename - aborting.");
        }

        if (file == null)
            System.exit(0);

        Utils.timeStampOutput(String.format("Input: %s", file.getAbsolutePath()));

        SlideJ s = new SlideJ(props, tmpDir);

        if (neighbourhoodSize.length < 3) {
            neighbourhoodSize = new int[]{neighbourhoodSize[0], neighbourhoodSize[0], neighbourhoodSize[0]};
        }

        s.load(file, series, neighbourhoodSize);

        Utils.timeStampOutput("Done");

        System.exit(0);

    }
}