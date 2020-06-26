package slidej;

import TimeAndDate.TimeAndDate;
import UtilClasses.GenUtils;
import slidej.properties.SlideJParams;

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