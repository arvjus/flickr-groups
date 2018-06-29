package flickr;

import flickr.model.GroupMatrix;
import flickr.utils.Ranking;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static flickr.JsonUtils.readFromJson;
import static flickr.JsonUtils.writeToJson;
import static java.lang.Math.round;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class RankingTest {
    @Test
    public void test() throws IllegalAccessException, IOException, InstantiationException {
        List<Double> distances = (List<Double>) readFromJson("/data/appdata/flickr-groups/group-dist.json", ArrayList.class);

        Double topDistance = 0.0;
        for (Double distance : distances)
            if (distance != 0) {
                topDistance = distance;
                break;
            }

        for (Double distance : distances)
            System.out.println(distance / topDistance);
    }
}
