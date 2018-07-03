package flickr;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class Stats {
    Double members;
    Double min;
    Double q1;
    Double median;
    Double q3;
    Double max;
    Double mean;
    Double sd;

    public void calculate(String[] fields) {
        int n = fields.length - 3;

        // members - min-max normalization
        members = (Double.valueOf(fields[2])-350)/52000;

        min = Double.valueOf(fields[3]);
        q1 = Double.valueOf(fields[n / 4 + 3]);
        median = Double.valueOf(fields[n / 2 + 3]);
        q3 = Double.valueOf(fields[n / 4 * 3 + 3]);
        max = Double.valueOf(fields[fields.length - 1]);

        // mean
        double sum = 0.0;
        for (int i = 3; i < fields.length; i ++)
            sum += Double.valueOf(fields[i]);
        mean = sum / n;

        // sd
        sum = 0.0;
        for (int i = 3; i < fields.length; i ++)
            sum += pow(Double.valueOf(fields[i]) - mean, 2);
        sd = sqrt(sum / n);
    }
}
