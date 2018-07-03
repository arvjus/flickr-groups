package flickr;

import java.io.Serializable;

public class GroupDist implements Serializable {
    GroupDist(String groupId, Double dist) {
        this.groupId = groupId;
        this.dist = dist;
    }

    String groupId;
    Double dist;

    public String getGroupId() {
        return groupId;
    }

    public Double getDist() {
        return dist;
    }
}
