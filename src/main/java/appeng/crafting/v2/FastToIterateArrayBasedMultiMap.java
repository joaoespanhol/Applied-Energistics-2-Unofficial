package appeng.crafting.v2;

import java.lang.reflect.Array;
import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A simple map implementation that uses an array for the keys and an array of arrays for the values. This map is only
 * intended for a very small number of keys and values, in a scenario where the map is initialized once and afterward
 * only queried. All operations are slow, except for manually iterating over {@link #keyArray()} and
 * {@link #valuesArrayAt(int)}.
 *
 * @param <K> The key type
 * @param <V> The value type
 */
public final class FastToIterateArrayBasedMultiMap<K, V> {

    private static final int DEFAULT_CAPACITY = 8;
    private static final int DEFAULT_VALUE_LIST_SIZE = 4;

    private final Class<K> keyClass;
    private final Class<V> valueClass;
    private final Class<V[]> valueArrayClass;

    /**
     * A plain array of keys
     */
    private K[] keys;
    /**
     * A 2D array of values, where values[i] is a plain array containing the values for the key at keys[i]
     */
    private V[][] values;

    private int size;

    public FastToIterateArrayBasedMultiMap(Class<?> keyClass, Class<?> valueClass) {
        this.keyClass = (Class<K>) keyClass;
        this.valueClass = (Class<V>) valueClass;
        this.valueArrayClass = (Class<V[]>) Array.newInstance(valueClass, 0).getClass();

        this.keys = createArray(this.keyClass, DEFAULT_CAPACITY);
        this.values = createArray(this.valueArrayClass, DEFAULT_CAPACITY);
    }

    public V put(K newKey, V newValue) {
        resize();

        K[] keys = this.keys;
        int size = keys.length;
        for (int i = 0; i < size; i++) {
            K key = keys[i];
            if (key != null && !key.equals(newKey)) {
                continue;
            }

            if (key == null) {
                keys[i] = newKey;
            }

            V[][] values = this.values;
            V[] valueList = values[i];
            if (valueList == null) {
                valueList = createArray(this.valueClass, DEFAULT_VALUE_LIST_SIZE);
                values[i] = valueList;
            }

            int valueLength = valueList.length;
            int j = 0;
            for (; j < valueLength; j++) {
                V value = valueList[j];
                if (value == null) {
                    break;
                }

                if (value.equals(newValue)) {
                    valueList[j] = newValue;
                    return value;
                }
            }

            if (j == valueLength) {
                valueList = Arrays.copyOf(valueList, valueLength * 2);
                values[i] = valueList;
            }

            this.size++;
            valueList[j] = newValue;
            return null;
        }

        throw new IllegalStateException("Unreachable");
    }

    public void remove(Object key, Object value) {
        K[] keys = this.keys;
        V[][] values = this.values;
        for (int i = 0; i < keys.length; i++) {
            K k = keys[i];
            if (k == null || !k.equals(key)) {
                continue;
            }

            V[] valueList = values[i];
            int valueCount = 0;
            for (int j = 0; j < valueList.length; j++) {
                V v = valueList[j];
                if (v == null) {
                    continue;
                }

                if (v.equals(value)) {
                    valueList[j] = null;
                    this.size--;
                } else {
                    valueCount++;
                }
            }

            if (valueCount == 0) {
                keys[i] = null;
                values[i] = null;
            }
            return;
        }
    }

    /**
     * Returns the internal keys array of this map. Do not modify. Can contain null entries.
     *
     * @return the internal keys array
     */
    public K[] keyArray() {
        // Sadly we have to expose this array to allow fast iteration over all keys.
        return this.keys;
    }

    /**
     * Returns the internal values array of the key at index {@code i}. Do not modify. Can contain null entries.
     *
     * @param i the index of the key
     * @return the internal values array of the key at index {@code i}
     */
    public V[] valuesArrayAt(int i) {
        // Sadly we have to expose these arrays to allow fast iteration over all values.
        return this.values[i];
    }

    public int size() {
        return size;
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return new EntrySet();
    }

    private void resize() {
        K[] keys = this.keys;
        int capacity = keys == null ? 0 : keys.length;
        if ((size + 1) <= capacity) {
            return;
        }

        int newSize = capacity == 0 ? DEFAULT_CAPACITY : capacity * 2;
        K[] newKeys = createArray(this.keyClass, newSize);
        V[][] newValues = createArray(this.valueArrayClass, newSize);

        if (keys != null) {
            System.arraycopy(keys, 0, newKeys, 0, capacity);
            System.arraycopy(values, 0, newValues, 0, capacity);
        }

        this.keys = newKeys;
        this.values = newValues;
    }

    private static <T> T[] createArray(Class<T> clazz, int length) {
        return (T[]) Array.newInstance(clazz, length);
    }

    class EntrySet extends AbstractSet<Map.Entry<K, V>> {

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return FastToIterateArrayBasedMultiMap.this.size();
        }
    }

    class EntryIterator implements Iterator<Map.Entry<K, V>> {

        private int keyIndex;
        private int valueIndex;
        private Map.Entry<K, V> next;

        @Override
        public boolean hasNext() {
            return findNext(true);
        }

        @Override
        public Map.Entry<K, V> next() {
            findNext(false);
            return this.next;
        }

        @Override
        public void remove() {
            if (next == null) throw new IllegalStateException();

            FastToIterateArrayBasedMultiMap.this.remove(next.getKey(), next.getValue());
            if (FastToIterateArrayBasedMultiMap.this.valuesArrayAt(keyIndex) == null) {
                keyIndex++;
                valueIndex = 0;
            }

            next = null;
        }

        private boolean findNext(boolean simulate) {
            K[] keys = FastToIterateArrayBasedMultiMap.this.keys;
            V[][] values = FastToIterateArrayBasedMultiMap.this.values;
            for (int i = this.keyIndex; i < keys.length; i++) {
                K key = keys[i];
                if (key == null) {
                    continue;
                }

                V[] valueList = values[i];
                for (int j = valueIndex; j < valueList.length; j++) {
                    V value = valueList[j];
                    if (value == null) {
                        continue;
                    }

                    if (!simulate) {
                        this.keyIndex = i;
                        this.valueIndex = j + 1;
                        this.next = new SimpleEntry<>(key, value);
                    }
                    return true;
                }

                this.valueIndex = 0;
            }

            this.next = null;
            return false;
        }
    }
}
