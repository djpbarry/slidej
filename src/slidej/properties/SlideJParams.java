package slidej.properties;

import java.util.Properties;

public class SlideJParams extends Properties {
    public static final String THRESHOLD = "Threshold";
    public static final String RAW_INPUT = "Raw input data";
    public static final String AUX_INPUT = "Aux input data";
    public static String TITLE = "SlideJ v1.0";
    public static String FILTER_RADIUS = "Filter radius";
    public static String DEFAULT_FILTER_RADIUS = "2.0";
    public static String DEFAULT_THRESHOLD_METHOD = "Default";

    public SlideJParams() {

    }

    public synchronized Object setChannelProperty(String key, String value, int channel) {
        return super.setProperty(getFormattedKey(key, channel), value);
    }

    public String getChannelAxisProperty(String key, int channel, int axis, String defaultVal) {
        String formattedKey = getFormattedKey(key, channel, axis);
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

    private String getFormattedKey(String key, int channel, int axis) {
        String cKey = getFormattedKey(key, channel);
        if (axis < 0) return cKey;
        return String.format("%s Axis %d", cKey, axis);
    }
}
