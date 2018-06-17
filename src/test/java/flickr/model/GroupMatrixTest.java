package flickr.model;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static java.lang.Math.round;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class GroupMatrixTest {
    GroupMatrix groupMatrix;

    @Before
    public void setup() {
        groupMatrix = new GroupMatrix("/data/appdata/flickrgroups/test1");
    }

    @Test
    public void testInit() throws IOException {
        File datfile = new File("/data/appdata/flickrgroups/test1.dat");
        File idxfile = new File("/data/appdata/flickrgroups/test1.idx");
        if (datfile.exists())
            datfile.delete();
        if (idxfile.exists())
            idxfile.delete();
        groupMatrix.init(new String[] {"g1", "g2", "g3", "g4"}, 3);
        assertTrue(datfile.exists());
        assertEquals(4 * 1, datfile.length());
        assertTrue(idxfile.exists());
    }

    @Test
    public void testLoad() throws IOException {
        File datfile = new File("/data/appdata/flickrgroups/test1.dat");
        File idxfile = new File("/data/appdata/flickrgroups/test1.idx");
        if (datfile.exists() && idxfile.exists()) {
            groupMatrix = new GroupMatrix("/data/appdata/flickrgroups/test1");
            groupMatrix.load();
            assertEquals(2 * 1, groupMatrix.getGroupPos("g3"));
        }
    }

    @Test
    public void testSave() throws IOException {
        groupMatrix.load();
        assertFalse(groupMatrix.isDirty());
        groupMatrix.setMember("g1", 0);
        groupMatrix.setMember("g1", 2);
        groupMatrix.setMember("g2", 0);
        groupMatrix.setMember("g2", 2);
        groupMatrix.setMember("g3", 0);
        groupMatrix.setMember("g3", 1);
        groupMatrix.setMember("g3", 2);
        groupMatrix.setMember("g4", 1);
        assertTrue(groupMatrix.isDirty());
        groupMatrix.save();
        assertFalse(groupMatrix.isDirty());
    }

    @Test
    @Ignore
    public void testSave_part() throws IOException {
        groupMatrix.load();
        assertFalse(groupMatrix.isDirty());
        groupMatrix.setMember("g3", 0);
        assertTrue(groupMatrix.isDirty());
        groupMatrix.save();
        assertFalse(groupMatrix.isDirty());
    }

    @Test(expected = RuntimeException.class)
    public void testDist_failure() throws IOException {
        groupMatrix.dist("no-such-group");
    }

    @Test
    public void testDist_success() throws IOException {
        groupMatrix.load();
        Map<String, Double> dist = groupMatrix.dist("g1");
        assertEquals(4, dist.size());
        assertEquals(0.0, dist.get("g1"));
        assertEquals(13, round(dist.get("g3") * 100));
        assertEquals(0.375, dist.get("g4"));
    }

    @Test
    public void testDist() {
        assertEquals(0.0, GroupMatrix.dist(new byte[] {0}, new byte[] {0}));
        assertEquals(0.0, GroupMatrix.dist(new byte[] {1}, new byte[] {1}));
        assertEquals(1.0, GroupMatrix.dist(new byte[] {(byte)255}, new byte[] {0}));
        assertEquals(0.125, GroupMatrix.dist(new byte[] {1}, new byte[] {0}));
        assertEquals(0.0625, GroupMatrix.dist(new byte[] {1, 1}, new byte[] {1, 0}));
    }
}
