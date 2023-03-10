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

package net.calm.slidej.properties;

import java.io.IOException;
import java.util.Properties;

public class SlideJParams extends Properties {
    public static final String THRESHOLD = "Threshold";
    public static final String TOP_HAT = "Top hat";
    public static final String SKELETONISE = "Skeletonise";
    public static final String RAW_INPUT = "Raw input data";
    public static final String AUX_INPUT = "Aux input data";
    public static final String BIN_INPUT = "Binary input data";
    public static final String OUTPUT = "Output directory";
    public static final String TITLE = "SlideJ";
    private String version;
    public static final String FILTER_RADIUS = "Filter radius";
    public static final String TH_FILTER_RADIUS = "Top hat filter radius";
    public static final String DEFAULT_FILTER_RADIUS = "2.0";
    public static final String DEFAULT_TH_FILTER_RADIUS = "5.0";
    public static final String DEFAULT_THRESHOLD_METHOD = "Default";
    public static final String THRESHOLD_CHANNEL = "Segment";
    public static final String DEFAULT_THRESHOLD_CHANNEL = "true";
    public static final String DEFAULT_TH_CHANNEL = "false";
    public static final String DEFAULT_SKEL_CHANNEL = "false";
    public static final String COLOC = "Colocalise";
    public static final String DO_3D = "3D Analysis";
    public static final String NEIGHBOURHOOD = "Neighbourhood size";
    public static final int CELL_SIZE = 1000;
    public static final String OUTPUT_FILE_EXT = ".ome.btf";
    public static final String N_STEPS = "Number of steps";
    public static final String CHANNEL_FOR_STEP = "Channel for";
    public static final int CELL_IMG_DIM = 1000;
    public static final int X_AXIS = 0;
    public static final int Y_AXIS = 1;
    public static final int Z_AXIS = 2;
    public static final int C_AXIS = 3;
    public static final int N_AXIS = 4;

    public SlideJParams() {
        try {
            final Properties properties = new Properties();
            String cwd = System.getProperty("user.dir");
            properties.load(this.getClass().getResourceAsStream("../../../../project.properties"));
            this.version = String.format("v%s", properties.getProperty("version"));
        } catch (IOException e) {
            this.version = "v1.0.0";
        }
    }

    public synchronized Object setChannelProperty(String key, String value, int channel) {
        return super.setProperty(getFormattedKey(key, channel), value);
    }

    public String getStepProperty(String key, int channel, String defaultVal) {
        String formattedKey = getFormattedKey(key, channel);
        String prop = super.getProperty(formattedKey);
        if (prop == null) {
            prop = defaultVal;
            super.setProperty(formattedKey, prop);
        }
        return prop;
    }

    private String getFormattedKey(String key, int channel) {
        return String.format("%s Step %d", key, channel);
    }

    public String getVersion() {
        return version;
    }
}
