package appeng.crafting.v2;

import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FastToIterateArrayBasedMultiMapTest {

    private FastToIterateArrayBasedMultiMap<Integer, Object> map;

    @Before
    public void setUp() {
        map = new FastToIterateArrayBasedMultiMap<>(Integer.class, Object.class);
    }

    @Test
    public void testKeyArrayNotNull() {
        Assert.assertNotNull(map.keyArray());
    }

    @Test
    public void testPutGetInternalArrays() {
        int size = 10;
        Object[][] values = new Object[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                map.put(i, values[i][j] = new Object());
            }
        }

        Assert.assertEquals(size * size, map.size());
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Assert.assertSame(values[i][j], map.valuesArrayAt(i)[j]);
                Assert.assertEquals(i, (int) map.keyArray()[i]);
            }
        }
    }

    @Test
    public void testEntrySet() {
        int size = 10;
        Object[][] values = new Object[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                map.put(i, values[i][j] = new Object());
            }
        }

        Iterator<Entry<Integer, Object>> iterator = map.entrySet().iterator();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Entry<Integer, Object> entry = iterator.next();
                Assert.assertEquals(i, (int) entry.getKey());
                Assert.assertSame(values[i][j], entry.getValue());
            }
        }
    }

    @Test
    public void testRemove() {
        int size = 10;
        Object[][] values = new Object[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                map.put(i, values[i][j] = new Object());
            }
        }

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                map.remove(i, values[i][j]);
            }

            Assert.assertNull(map.valuesArrayAt(i));
        }
        Assert.assertEquals(0, map.size());
    }

    @Test
    public void testIteratorRemove() {
        int size = 10;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                map.put(i, new Object());
            }
        }

        Iterator<Entry<Integer, Object>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }

        Assert.assertEquals(0, map.size());
    }
}
