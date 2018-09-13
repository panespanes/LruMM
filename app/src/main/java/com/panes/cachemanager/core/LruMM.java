package com.panes.cachemanager.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Alibaba.com Inc.
 * Copyright (c) 1999-2018 All Rights Reserved.
 *
 * @author panes
 * @contact pt135794@alibaba-inc.com
 */
public class LruMM<K, V> {
    private final LinkedHashMap<K, V> major;
    private final LinkedHashMap<K, V> minor;

    private int size;
    private int majorSize;
    private int maxSize;

    public LruMM(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        major = new LinkedHashMap<>(0, 0.75f, true);
        minor = new LinkedHashMap<>(0, 0.75f, true);

        this.maxSize = maxSize;
    }

    public final V get(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        V value;
        synchronized (this) {
            value = major.get(key);
            if (value != null) {
                return value;
            }
            value = minor.get(key);
            if (value != null) {
                // since we hit the cache which has been requested and used at least twice,
                // it should upgraded from minor to major.
                upgrade(key, value);
                return value;
            }
            return null;
        }
    }

    private void upgrade(K key, V value) {
        // there's no need to find out whether the major contains the specified value,
        // do not forget we have called major.get() before.
        minor.remove(key);
        putMajor(key, value);
    }

    private void putMajor(K key, V value) {
        // major does not contain the key in any case
        majorSize += safeSizeOf(key, value);
        major.put(key, value);
        downgrade();
    }

    /**
     * Logically, when we put a k-v into major which has its size bigger than maxMajorSize,
     * 'downgrade' opposite to 'upgrade' will be triggered.
     * but, in fact, maxMajorSize does not exist -- the size of major or minor is dynamic,
     * the only permanent number is sum of major and minor.
     * so we simply imply maxMajorSize = maxSize / 2. maybe this will be modified later.
     *
     */
    private void downgrade() {
        if (!major.isEmpty() && majorSize > maxSize / 2) {
            Map.Entry<K, V> next = major.entrySet().iterator().next();
            K downgradeKey = next.getKey();
            V downgradeValue = next.getValue();
            majorSize -= safeSizeOf(downgradeKey, downgradeValue);
            major.remove(downgradeKey);
            minor.put(downgradeKey, downgradeValue);
        }
    }

    public final void put(K key, V value) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        V mapValue;
        synchronized (this) {
            mapValue = major.get(key);
            if (mapValue != null) {
                putMajor(key, value);
            } else {
                mapValue = minor.get(key);
                if (mapValue != null) {
                    upgrade(key, value);
                } else {
                    // not in major or minor
                    minor.put(key, value);
                }
            }
            size += safeSizeOf(key, value) - safeSizeOf(key, mapValue);
        }
        trim();
    }

    /**
     * trim major after trimming minor
     */
    private void trim() {
        while (size > maxSize) {
            if (major.isEmpty() && minor.isEmpty()) {
                break;
            }
            K key;
            V value;
            Map.Entry<K, V> next;
            synchronized (this) {
                if (minor.entrySet().iterator().hasNext()) {
                    next = minor.entrySet().iterator().next();
                    key = next.getKey();
                    value = next.getValue();
                    minor.remove(key);
                } else {
                    next = major.entrySet().iterator().next();
                    key = next.getKey();
                    value = next.getValue();
                    major.remove(key);
                    majorSize -= safeSizeOf(key, value);
                }
                size -= safeSizeOf(key, value);
            }
        }
    }

    private int safeSizeOf(K key, V value) {
        int result = sizeOf(key, value);
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + key + "=" + value);
        }
        return result;
    }

    /**
     * The default implementation returns 1 so that size
     * is the number of entries and max size is the maximum number of entries.
     */
    protected int sizeOf(K key, V value) {
        if (value == null){
            return 0;
        }
        return 1;
    }

    /**
     * Returns a copy of the current contents of the cache, ordered from least
     * recently accessed to most recently accessed.
     */
    public synchronized final Map<K, V> snapshotMajor() {
        return new LinkedHashMap<>(major);
    }
    /**
     * Returns a copy of the current contents of the cache, ordered from least
     * recently accessed to most recently accessed.
     */
    public synchronized final Map<K, V> snapshotMinor() {
        return new LinkedHashMap<>(minor);
    }
}
