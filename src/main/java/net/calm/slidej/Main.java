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

import net.calm.iaclasslibrary.TimeAndDate.TimeAndDate;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import net.calm.slidej.properties.SlideJParams;

import java.io.File;

public class Main {

    public static void main(String[] args) {

        System.setProperty("java.awt.headless", "true");

        System.out.println(SlideJParams.TITLE);
        System.out.println(TimeAndDate.getCurrentTimeAndDate());

        File file = null;
        File props = null;
        int neighbourhoodSize = 50;
        int series = 0;

        try {
            for (int i = 0; i < args.length - 1; i++) {
                switch (args[i]) {
                    case "-f":
                        file = new File(args[i + 1]);
                        break;
                    case "-n":
                        neighbourhoodSize = Integer.parseInt(args[i + 1]);
                        break;
                    case "-s":
                        series = Integer.parseInt(args[i + 1]);
                        break;
                    case "-p":
                        props = new File(args[i + 1]);
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

        System.out.println(String.format("Input: %s", file.getAbsolutePath()));

        SlideJ s = new SlideJ(props);

        s.load(file, series, neighbourhoodSize);

        System.out.println(String.format("Done: %s", TimeAndDate.getCurrentTimeAndDate()));

        System.exit(0);

    }
}