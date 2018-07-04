package flickr;

public class Recommendation {
    private String groupId;
    private int count;
    private int order;

    public Recommendation(String groupId, int order) {
        this.groupId = groupId;
        this.order = order;
        this.count = 1;
    }

    public void setOrder(int order) {
        if (order < this.order)
            this.order = order;
    }

    public void incCount() {
        count++;
    }

    public String getGroupId() {
        return groupId;
    }

    public int getCount() {
        return count;
    }

    public int getOrder() {
        return order;
    }

    public int compareTo(Recommendation r) {
        if (count == r.count)
            return order - r.order;
        return r.count - count;
    }
}
