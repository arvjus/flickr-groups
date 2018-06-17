package flickr.model;

import java.io.*;
import java.util.*;

public class GroupMatrix {
    private String filepath;
    private int nGroups;
    private int nMembers;
    private int nMembersBytes;
    Map<String, Long> groupPositions;
    Map<String, List<Integer>> changes;

    public GroupMatrix(String filepath) {
        this.filepath = filepath;
        this.changes = new HashMap<>();
    }

    public void init(String[] groupNames, int nMembers) throws IOException {
        this.nGroups = groupNames.length;
        this.nMembers = nMembers;
        this.nMembersBytes = calculateMemberBytes(this.nMembers);

        groupPositions = new LinkedHashMap<>();
        for (int i = 0; i < groupNames.length; i++)
            groupPositions.put(groupNames[i], new Long((long)i * (long)nMembersBytes));

        try (FileWriter fileWriter = new FileWriter(new File(filepath + ".idx"))) {
            fileWriter.write(String.format("%d:%d\n", nGroups, nMembers));
            for (Map.Entry<String, Long> entry : groupPositions.entrySet())
                fileWriter.write(String.format("%s:%d\n", entry.getKey(), entry.getValue()));
            fileWriter.close();
        }

        try (RandomAccessFile raf = new RandomAccessFile(new File(filepath + ".dat"), "rw")) {
            byte[] zeros = new byte[nMembersBytes];
            for (int g = 0; g < nGroups; g++)
                raf.write(zeros);
            raf.close();
        }
    }

    public void load() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(filepath + ".idx")))) {
            String line = reader.readLine();
            String[] strings = line.split(":");
            this.nGroups = Integer.valueOf(strings[0]);
            this.nMembers = Integer.valueOf(strings[1]);
            this.nMembersBytes = calculateMemberBytes(this.nMembers);

            groupPositions = new LinkedHashMap<>();
            while ((line = reader.readLine()) != null) {
                strings = line.split(":");
                groupPositions.put(strings[0], new Long(strings[1]));
            }
            reader.close();
        }
    }

    public void save() throws IOException {
        if (!isDirty())
            return;

        byte[] buffer = new byte[nMembersBytes];
        try (RandomAccessFile raf = new RandomAccessFile(new File(filepath + ".dat"), "rw")) {
            for (Map.Entry<String, List<Integer>> entry : changes.entrySet()) {
                Arrays.fill(buffer, (byte) 0);
                for (Integer position : entry.getValue()) {
                    int offs = position / 8;
                    buffer[offs] |= (1 << (position % 8));
                }

                raf.seek(getGroupPos(entry.getKey()));
                raf.write(buffer);
            }
            raf.close();
        }
        changes.clear();
    }

    public void setMember(String groupName, int memberPos) {
        List<Integer> positions = changes.get(groupName);
        if (positions == null)
            positions = new ArrayList<>();
        positions.add(memberPos);
        changes.put(groupName, positions);
    }

    public boolean isDirty() {
        return changes.size() > 0;
    }

    public Map<String, Double> dist(String groupName) throws IOException {
        Map<String, Double> dist = new LinkedHashMap<>();
        long groupPos = getGroupPos(groupName);

        try (RandomAccessFile raf = new RandomAccessFile(new File(filepath + ".dat"), "r")) {
            byte[] groupBuffer = new byte[nMembersBytes];
            raf.seek(groupPos);
            raf.read(groupBuffer);

            byte[] buffer = new byte[nMembersBytes];
            for (Map.Entry<String, Long> entry : groupPositions.entrySet()) {
                raf.seek(entry.getValue());
                raf.read(buffer);
                dist.put(entry.getKey(), dist(groupBuffer, buffer));
            }
            raf.close();
        }
        return dist;
    }

    static double dist(byte[] v1, byte[] v2) {
        double dist = 0.0;
        for (int i = 0; i < v1.length; i++) {
            byte b1 = v1[i];
            byte b2 = v2[i];
            for (short mask = 0b10000000; mask > 0; mask >>= 1)
                dist += ((b1 & mask) == (b2 & mask) ? 0 : 1);
        }
        return dist / v1.length / 8;
    }

    private static int calculateMemberBytes(int nMembers) {
        int nMembersBytes = nMembers / 8;
        if (nMembers % 8 > 0)
            nMembersBytes++;
        return nMembersBytes;
    }

    long getGroupPos(String groupName) {
        Long groupPos = groupPositions.get(groupName);
        if (groupPos == null)
            throw new RuntimeException("invalid group name " + groupName);
        return groupPos.longValue();
    }
}
