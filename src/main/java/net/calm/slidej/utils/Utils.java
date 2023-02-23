package net.calm.slidej.utils;

import net.calm.iaclasslibrary.TimeAndDate.TimeAndDate;

public class Utils {
    public static void timeStampOutput(String text) {
        System.out.println(String.format("%s: %s", TimeAndDate.getCurrentTimeAndDate(), text));
    }
}
