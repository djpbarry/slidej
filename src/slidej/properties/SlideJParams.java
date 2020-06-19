package slidej.properties;

import java.util.Properties;

public class SlideJParams extends Properties {
    public static final String THRESHOLD = "Threshold";
    public static final String RAW_INPUT = "Raw input data";
    public static final String AUX_INPUT = "Aux input data";
    public static final String TITLE = "SlideJ v1.0";
    public static final String FILTER_RADIUS = "Filter radius";
    public static final String DEFAULT_FILTER_RADIUS = "2.0";
    public static final String DEFAULT_THRESHOLD_METHOD = "Default";
    public static final String THRESHOLD_CHANNEL = "Segment";
    public static final String DEFAULT_THRESHOLD_CHANNEL = "true";

    public SlideJParams() {

    }

    public synchronized Object setChannelProperty(String key, String value, int channel) {
        return super.setProperty(getFormattedKey(key, channel), value);
    }

    public String getChannelProperty(String key, int channel, String defaultVal) {
        String formattedKey = getFormattedKey(key, channel);
        String prop = super.getProperty(formattedKey);
        if (prop == null) {
            prop = defaultVal;
            super.setProperty(formattedKey, prop);
        }
        return prop;
    }

    private String getFormattedKey(String key, int channel) {
        return String.format("%s Channel %d", key, channel);
    }
}
