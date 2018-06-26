package flickr;

import java.io.Serializable;

public class GroupDist implements Serializable {
    GroupDist(String group, Double dist) {
        this.group = group;
        this.dist = dist;
    }

    String group;
    Double dist;

    public String getGroup() {
        return group;
    }

    public Double getDist() {
        return dist;
    }
}
