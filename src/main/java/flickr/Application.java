package flickr;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.groups.Group;
import flickr.fetcher.Fetcher;
import flickr.model.GroupMatrix;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.*;
import java.util.stream.Collectors;

import static flickr.JsonUtils.readFromJson;
import static flickr.JsonUtils.writeToJson;

public class Application {
    final Properties properties;
    final String baseDir;
    final Fetcher fetcher;

    public Application(Configuration properties, Fetcher fetcher) {
        this.properties = properties;
        baseDir = properties.getProperty("baseDir");
        this.fetcher = fetcher;
    }

    public void selectUsersGroupsSubsets() throws IOException, InstantiationException, IllegalAccessException {
        Set<String> users = new HashSet<>();
        Set<String> groups = new HashSet<>();

        List<String> userIds = (List<String>) readFromJson(baseDir + "/users-2.json", ArrayList.class);
        userIds.stream().
                filter(userId -> new File(baseDir + "/users/" + userId + "_groups.json").exists()).
//                limit(50000).
        forEach(userId -> {
    try {
        users.add(userId);
        String userPathname = baseDir + "/users/" + userId + "_groups.json";
        List<Map<String, String>> userGroups = (List<Map<String, String>>) readFromJson(userPathname, ArrayList.class);
        userGroups.forEach(map -> groups.add(map.get("id")));
    } catch (Throwable e) {
        e.printStackTrace();
    }
});
        writeToJson(baseDir + "/users-medium.json", users);
        writeToJson(baseDir + "/groups-medium.json", groups);
        System.out.println("number of users: " + users.size());
        System.out.println("number of groups: " + groups.size());
    }

    public void createGroupMatrix() throws IOException, InstantiationException, IllegalAccessException {
        List<String> groups = (List<String>) readFromJson(baseDir + "/groups-medium.json", ArrayList.class);
        List<String> users = (List<String>) readFromJson(baseDir + "/users-medium.json", ArrayList.class);

        GroupMatrix groupMatrix = new GroupMatrix(baseDir + "/groups-medium");
        groupMatrix.init((String[]) groups.toArray(new String[0]), users.size());
        for (int i = 0; i < users.size(); i++) {
            final int userPos = i;
            String userPathname = baseDir + "/users/" + users.get(i) + "_groups.json";
            List<Map<String, String>> userGroups = (List<Map<String, String>>) readFromJson(userPathname, ArrayList.class);
            userGroups.stream().map(map -> map.get("id")).forEach(groupId -> groupMatrix.setMember(groupId, userPos));
        }
        groupMatrix.save();
    }

    public void getRecommendationsForUser(String userId, int topLimit) throws FlickrException, IllegalAccessException, IOException, InstantiationException {
        String indexPathname = baseDir + "/html/index.json";
        HashSet<String> groupIds = (HashSet<String>) readFromJson(indexPathname, HashSet.class);

        List<String> blackList = Arrays.asList("61859776@N00", "2784352@N25", "2896642@N25", "497397@N20", "66351550@N00");

        List<Group> userGroups = fetcher.getGroups(userId);
        userGroups.stream().
                filter(group -> !groupIds.contains(group.getId()) && !blackList.contains(group.getId())).
                forEach(group -> {
                    try {
                        System.out.println("get recommendations for " + group.getName());
                        getRecommendationsForGroup(group.getId(), topLimit);
                        groupIds.add(group.getId());
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                });
        writeToJson(indexPathname, groupIds);
    }

    public void getRecommendationsForGroup(String groupId, int topLimit) throws IOException, FlickrException {
        GroupMatrix groupMatrix = new GroupMatrix(baseDir + "/groups-medium");
        groupMatrix.load();
        Map<String, Double> dist = groupMatrix.dist(groupId);

        List<GroupDist> groupDist = dist.entrySet().stream().
                map(entry -> new GroupDist(entry.getKey(), entry.getValue())).
                sorted((o1, o2) -> o1.getDist().compareTo(o2.getDist())).
//                map(o -> o.getDist()).
        collect(Collectors.toList());
//        writeToJson(baseDir + "/group-dist.json", groupDist);
        generateReport(groupId, groupDist, topLimit);
    }

    public void generateReport(String groupId, List<GroupDist> groupDist, int topLimit) throws IOException, FlickrException {
        Map<String, String> mainGroupInfo = fetcher.fetchGroupInfo(groupId);
        String filename = mainGroupInfo.get("name").
                replace(" ", "_").
                replace("/", "_").
                replace("?", "_").
                replace("!", "_") + ".html";

        try (FileOutputStream out = new FileOutputStream(new File(baseDir + "/html/" + filename))) {
            out.write("<html><body><br>".getBytes());
            out.write(("<h2><a href=\"" + mainGroupInfo.get("url") + "\">" + mainGroupInfo.get("name") + "</a> (members: " + mainGroupInfo.get("members") + ")</h2>").getBytes());

            groupDist.stream().filter(gd -> !groupId.equals(gd.group)).limit(topLimit).forEach(gd -> {
                try {
                    Map<String, String> groupInfo = fetcher.fetchGroupInfo(gd.group);
                    System.out.println("fetching info for " + groupInfo.get("name"));
                    out.write(("<a href=\"" + groupInfo.get("url") + "\">" + groupInfo.get("name") + "</a> (members: " + groupInfo.get("members") + ", dist: " + gd.dist + ")<br>").getBytes());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });

            out.write("</body></html>".getBytes());
            out.close();
        }
    }

    public void getDistancesForUser(String userId) throws FlickrException, IllegalAccessException, IOException, InstantiationException {
        GroupMatrix groupMatrix = new GroupMatrix(baseDir + "/groups-medium");
        groupMatrix.load();

        List<String> blackList = Arrays.asList("61859776@N00", "2784352@N25", "2896642@N25", "497397@N20", "66351550@N00");

        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(baseDir + "/distances_tmp.csv"));

        List<Group> userGroups = fetcher.getGroups(userId);
        userGroups.stream().
                filter(group -> !blackList.contains(group.getId())).
                forEach(group -> {
                    try {
                        System.out.println("get recommendations for " + group.getName());
                        out.write(group.getId());
                        Map<String, Double> dist = groupMatrix.dist(group.getId());
                        dist.entrySet().stream().
                                map(entry -> new GroupDist(entry.getKey(), entry.getValue())).
                                sorted((o1, o2) -> o1.getDist().compareTo(o2.getDist())).
                                limit(500).
                                forEach(groupDist -> {
                                    try {
                                        out.write("," + groupDist.getDist());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                        out.write("\n");
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                });
        out.close();
    }

    static class Stats {
        Double min;
        Double q1;
        Double median;
        Double q3;
        Double max;
        Double mean;

        public void calculate(String[] fields) {
            int n = fields.length - 2;
            min = Double.valueOf(fields[2]);
            q1 = Double.valueOf(fields[n / 4 + 2]);
            median = Double.valueOf(fields[n / 2 + 2]);
            q3 = Double.valueOf(fields[n / 4 * 3 + 2]);
            max = Double.valueOf(fields[fields.length - 1]);
            double sum = 0.0;
            for (int i = 2; i < fields.length; i ++)
                sum += Double.valueOf(fields[i]);
            mean = sum / n;
        }
    }

    public void transformDistances() throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(baseDir + "/distances_tmp.csv"));
             BufferedWriter out = new BufferedWriter(new FileWriter(baseDir + "/distances.csv"))) {

            out.write(String.format("min,q1,m,x4,x5,x6,x7,y%n"));

            String line;
            while ((line = in.readLine()) != null) {
                String[] fields = line.split(",", -1);
                int nfirst = Integer.valueOf(fields[1]);
                if (nfirst == 0)
                    continue;

                Stats stats = new Stats();
                stats.calculate(fields);
                for (int i = 2; i < fields.length; i++)
                    out.write(String.format("%f,%f,%f,%f,%f,%f,%s,%d%n",
                            stats.min, stats.q1, stats.median, stats.q3, stats.max, stats.mean, fields[i], i - 1 <= nfirst ? 1 : 0));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration properties = new Configuration();
        Fetcher fetcher = new Fetcher(properties);
//        fetcher.fetchGroupsL1AndUsersL2();
//        fetcher.listMissingGroupsL2();
//        if (args.length > 0)
//            fetcher.fetchMissingGroupsL2(args[0]);
//        fetcher.retrieveGroupsL2();

        Application application = new Application(properties, fetcher);
//        application.selectUsersGroupsSubsets();
//        application.createGroupMatrix();
//        application.getRecommendationsForGroup("22768280@N00", 10);
//        application.getRecommendationsForUser("31964888@N08", 100);
//        application.getDistancesForUser("31964888@N08");
        application.transformDistances();
    }
}
