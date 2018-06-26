package flickr.utils;

import flickr.GroupDist;

import java.util.Comparator;
import java.util.List;

public class Ranking {
    public static List<GroupDist> getTopRanking(List<GroupDist> groupDist, int treshold) {
        groupDist.sort(new Comparator<GroupDist>() {
            @Override
            public int compare(GroupDist o1, GroupDist o2) {
                return o1.getDist().compareTo(o2.getDist());
            }
        });
        return groupDist;
    }
}
