package flickr;

import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.groups.Group;
import flickr.fetcher.Fetcher;
import flickr.model.GroupMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
        writeToJson(baseDir + "/users.json", users);
        writeToJson(baseDir + "/groups.json", groups);
        System.out.println("number of users: " + users.size());
        System.out.println("number of groups: " + groups.size());
    }

    public void createGroupMatrix() throws IOException, InstantiationException, IllegalAccessException {
        List<String> groups = (List<String>) readFromJson(baseDir + "/groups.json", ArrayList.class);
        List<String> users = (List<String>) readFromJson(baseDir + "/users.json", ArrayList.class);

        GroupMatrix groupMatrix = new GroupMatrix(baseDir + "/groups");
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

        List<Group> userGroups = fetcher.getGroups(userId);
        userGroups.stream().
                filter(group -> !groupIds.contains(group.getId())).
                forEach(group -> {
                    try {
                        System.out.println("get recommendations for " + group.getName());
                        getRecommendationsForGroup(group.getId(), topLimit);
                        groupIds.add(group.getId());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (FlickrException e) {
                        e.printStackTrace();
                    }
                });
        writeToJson(indexPathname, groupIds);
    }

    public void getRecommendationsForGroup(String groupId, int topLimit) throws IOException, FlickrException {
        GroupMatrix groupMatrix = new GroupMatrix(baseDir + "/groups");
        groupMatrix.load();
        Map<String, Double> dist = groupMatrix.dist(groupId);

        List<GroupDist> groupDist = new ArrayList<>();
        for (Map.Entry<String, Double> entry : dist.entrySet())
            groupDist.add(new GroupDist(entry.getKey(), entry.getValue()));
        groupDist.sort(new Comparator<GroupDist>() {
            @Override
            public int compare(GroupDist o1, GroupDist o2) {
                return o1.dist.compareTo(o2.dist);
            }
        });
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
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (FlickrException e) {
                    e.printStackTrace();
                }
            });

            out.write("</body></html>".getBytes());
            out.close();
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
//        application.getRecommendationsForGroup("22768280@N00", 100);
        application.getRecommendationsForUser("31964888@N08", 100);
    }
}
