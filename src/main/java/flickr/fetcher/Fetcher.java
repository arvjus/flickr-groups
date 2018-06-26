package flickr.fetcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.flickr4java.flickr.*;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.groups.Group;
import com.flickr4java.flickr.groups.members.Member;
import com.flickr4java.flickr.groups.members.MembersList;
import com.flickr4java.flickr.util.IOUtilities;
import flickr.Configuration;
import flickr.model.GroupMatrix;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static flickr.JsonUtils.fileExists;
import static flickr.JsonUtils.readFromJson;
import static flickr.JsonUtils.writeToJson;

public class Fetcher {
    final Flickr flickr;
    final Properties properties;
    final String baseDir;

    public Fetcher(Properties properties) throws IOException {
        this.properties = properties;
        baseDir = properties.getProperty("baseDir");
        flickr = new Flickr(properties.getProperty("apiKey"), properties.getProperty("secret"), new REST());

        Auth auth = new Auth();
        auth.setPermission(Permission.READ);
        auth.setToken(properties.getProperty("token"));
        auth.setTokenSecret(properties.getProperty("tokensecret"));
        RequestContext.getRequestContext().setAuth(auth);
        Flickr.debugRequest = false;
        Flickr.debugStream = false;
    }

    public void fetchGroupsL1AndUsersL2() throws FlickrException, IOException {
        String userIdL1 = properties.getProperty("userIdL1");
        Set<String> groupIds = new HashSet<>();

        Set<String> userIds = new HashSet<>();
        List<String> blackList = Arrays.asList("61859776@N00", "2784352@N25", "2896642@N25", "497397@N20", "66351550@N00");

        List<Group> userL1Groups = getGroups(userIdL1);
        userL1Groups.stream().
                filter(group -> !blackList.contains(group.getId())).
                forEach(group -> {
                    try {
                        String groupId = group.getId();
                        groupIds.add(groupId);
                        String groupPathname = baseDir + "/groups/" + groupId + "_members.json";
                        if (fileExists(groupPathname)) {
                            ArrayList<LinkedHashMap<String, String>> members = (ArrayList) readFromJson(groupPathname, ArrayList.class);
                            members.forEach(member -> userIds.add(member.get("id")));
                        } else {
                            List<Member> members = getMembers(groupId);
                            members.forEach(member -> userIds.add(member.getId()));
                            writeGroupMembersToJson(members, groupPathname);
                        }
                    } catch (FlickrException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    }
                });
        writeUserGroupsToJson(userL1Groups, baseDir + "/users/" + userIdL1 + "_groups.json");
        writeToJson(baseDir + "/groups-1.json", groupIds);
        writeToJson(baseDir + "/users-2.json", userIds);
        System.out.println("number of users-2 objects: " + userIds.size());
    }

    public void listMissingGroupsL2() throws IllegalAccessException, IOException, InstantiationException {
        List<String> userIds = (List<String>) readFromJson(baseDir + "/users-2.json", ArrayList.class);
        System.out.println("number of users: " + userIds.size());
        final int[] count = new int[1];
        final int[] index = new int[1];
        final FileOutputStream[] out = {null};
        count[0] = Integer.MAX_VALUE;
        index[0] = 0;
        out[0] = null;
        userIds.forEach(userId -> {
            try {
                if (count[0] >= 1000) {
                    count[0] = 0;
                    if (out[0] != null)
                        out[0].close();
                    out[0] = new FileOutputStream(new File(baseDir + "/lst/lst#" + ++index[0]));
                }

                String userPathname = baseDir + "/users/" + userId + "_groups.json";
                if (!fileExists(userPathname)) {
                    out[0].write((userId + "\n").getBytes());
                    count[0]++;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
        if (out[0] != null)
            out[0].close();
    }

    public void fetchMissingGroupsL2(String filename) throws IllegalAccessException, IOException, InstantiationException {
        try (Stream<String> lines = Files.lines(Paths.get(filename), Charset.defaultCharset())) {
            lines.forEach(userId -> {
                try {
                    String userPathname = baseDir + "/users/" + userId + "_groups.json";
                    if (!fileExists(userPathname)) {
                        System.out.println("current: " + userId);
                        List<Group> userGroups = getGroups(userId);
                        writeUserGroupsToJson(userGroups, userPathname);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void retrieveGroupsL2() throws IOException, InstantiationException, IllegalAccessException {
        Map<String, String> groups = new HashMap<>();

        List<String> userIds = (List<String>) readFromJson(baseDir + "/users-2.json", ArrayList.class);
        userIds.forEach(userId -> {
            try {
                String userPathname = baseDir + "/users/" + userId + "_groups.json";
                List<Map<String, String>> userGroups = (List<Map<String, String>>) readFromJson(userPathname, ArrayList.class);
                userGroups.forEach(map -> groups.put(map.get("id"), map.get("name")));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
        writeToJson(baseDir + "/groups-2.json", groups);
        System.out.println("number of groups: " + groups.size());
    }


    public List<Group> getGroups(String userId) throws FlickrException {
        return flickr.getPeopleInterface().getGroups(userId);
    }

    public List<Member> getMembers(String groupId) throws FlickrException {
        System.out.println("fetching members for " + groupId);
        List<Member> membersTotal = new ArrayList<>();
        for (int page = 1; page < 1000000; page++) {
            System.out.println("getting page #" + page);
            MembersList<Member> members = null;
            try {
                members = flickr.getMembersInterface().getList(groupId, null, 100, page);
                membersTotal.addAll(members);
                if (members.size() == 0)
                    break;
            } catch (FlickrRuntimeException e) {
                e.printStackTrace();
            } catch (FlickrException e) {
                e.printStackTrace();
            }
        }
        return membersTotal;
    }

    private void writeGroupMembersToJson(List<Member> members, String pathname) throws IOException {
        List<Map<String, String>> memberList = members.stream().
                map(Fetcher::userToMap).
                collect(Collectors.toList());

        writeToJson(pathname, memberList);
    }

    private static Map<String, String> userToMap(Member member) {
        Map<String, String> map = new HashMap<>();
        map.put("id", member.getId());
        map.put("userName", member.getUserName());
        return map;
    }

    private void writeUserGroupsToJson(List<Group> groups, String pathname) throws IOException {
        List<Map<String, String>> groupList = groups.stream().
                map(Fetcher::groupToMap).
                collect(Collectors.toList());

        writeToJson(pathname, groupList);
    }

    private static Map<String, String> groupToMap(Group group) {
        Map<String, String> map = new HashMap<>();
        map.put("id", group.getId());
        map.put("name", group.getName());
        return map;
    }

    public Map<String,String> fetchGroupInfo(String group) throws FlickrException {
        Map<String,String> map = new HashMap<>();
        map.put("name", flickr.getGroupsInterface().getInfo(group).getName());
        map.put("members", "" + flickr.getGroupsInterface().getInfo(group).getMembers());
        map.put("url", flickr.getUrlsInterface().getGroup(group));
        return map;
    }
}
