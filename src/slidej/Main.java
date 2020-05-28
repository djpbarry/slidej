package slidej;

import java.io.File;

public class Main {

    public static void main(String[] args) {

        System.setProperty("java.awt.headless", "true");

        File file;

        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "-f":
                    file = new File(args[i + 1]);
                    break;
                default:
                    file = null;
            }

            if (file == null)
                System.exit(0);

            int series = 0;

            SlideJ s = new SlideJ();

            s.load(file, series);

            System.exit(0);

        }
    }
}