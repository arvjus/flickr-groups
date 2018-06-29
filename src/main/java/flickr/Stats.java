package flickr;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class Stats {
    Double min;
    Double q1;
    Double median;
    Double q3;
    Double max;
    Double mean;
    Double sd;

    public void calculate(String[] fields) {
        int n = fields.length - 2;

        min = Double.valueOf(fields[2]);
        q1 = Double.valueOf(fields[n / 4 + 2]);
        median = Double.valueOf(fields[n / 2 + 2]);
        q3 = Double.valueOf(fields[n / 4 * 3 + 2]);
        max = Double.valueOf(fields[fields.length - 1]);

        // mean
        double sum = 0.0;
        for (int i = 2; i < fields.length; i ++)
            sum += Double.valueOf(fields[i]);
        mean = sum / n;

        // sd
        sum = 0.0;
        for (int i = 2; i < fields.length; i ++)
            sum += pow(Double.valueOf(fields[i]) - mean, 2);
        sd = sqrt(sum / n);
    }
}
