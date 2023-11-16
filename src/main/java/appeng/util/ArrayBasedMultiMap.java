package appeng.util;

import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A simple map implementation that uses an array for the keys and an array of arrays for the values. The only supported
 * operations are {@link #put(Object, Object)} and {@link #size()}.
 *
 * @param <K> The key type
 * @param <V> The value type
 */
public final class ArrayBasedMultiMap<K, V> extends AbstractMap<K, V> {

    private static final int DEFAULT_CAPACITY = 8;
    private static final int DEFAULT_VALUE_LIST_SIZE = 4;

    private final Class<K> keyClass;
    private final Class<V> valueClass;
    private final Class<V[]> valueArrayClass;

    private K[] keys;
    private V[][] values;

    private int size;

    public ArrayBasedMultiMap(Class<?> keyClass, Class<?> valueClass) {
        this.keyClass = (Class<K>) keyClass;
        this.valueClass = (Class<V>) valueClass;
        this.valueArrayClass = (Class<V[]>) Array.newInstance(valueClass, 0).getClass();
    }

    @Override
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
                valueList = createArray(valueClass, DEFAULT_VALUE_LIST_SIZE);
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

    public K[] keyArray() {
        K[] keys = this.keys;
        return keys == null ? createArray(keyClass, 0) : keys;
    }

    public V[] valuesArrayAt(int i) {
        V[][] values = this.values;
        V[] valueList;
        if (values == null || (valueList = values[i]) == null) {
            return createArray(valueClass, 0);
        }

        return valueList;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entrySet = new HashSet<>();
        for (int i = 0; i < keys.length; i++) {
            K key = keys[i];
            if (key == null) {
                break;
            }

            V[] valueList = values[i];
            if (valueList == null) {
                continue;
            }

            for (V value : valueList) {
                if (value == null) {
                    break;
                }

                entrySet.add(new SimpleEntry<>(key, value));
            }
        }

        return entrySet;
    }

    private void resize() {
        K[] keys = this.keys;
        int capacity = keys == null ? 0 : keys.length;
        if ((size + 1) <= capacity) {
            return;
        }

        int newSize = capacity == 0 ? DEFAULT_CAPACITY : capacity * 2;
        K[] newKeys = createArray(keyClass, newSize);
        V[][] newValues = create2DArray(valueArrayClass, newSize);

        if (keys != null) {
            System.arraycopy(keys, 0, newKeys, 0, capacity);
            System.arraycopy(values, 0, newValues, 0, capacity);
        }

        this.keys = newKeys;
        this.values = newValues;
    }

    private static <T> T[] createArray(Class<T> clazz, int defaultValueListSize) {
        return (T[]) Array.newInstance(clazz, defaultValueListSize);
    }

    private static <T> T[][] create2DArray(Class<T[]> arrayClass, int defaultValueListSize) {
        return (T[][]) Array.newInstance(arrayClass, defaultValueListSize);
    }
}
