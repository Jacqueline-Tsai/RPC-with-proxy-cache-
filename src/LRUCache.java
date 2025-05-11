import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;

/**
 * A class that implements a Least Recently Used (LRU) cache.
 * The cache uses a LinkedHashMap to store the items, and evicts the least recently used item when the cache is full.
 */
public class LRUCache extends LinkedHashMap<String, FileCache> {
    private final int capacity; // Maximum capacity of the cache
    private final int maxSize; // Maximum size of the cache
    private static int currentSize; // Current size of the cache

    /**
     * Constructor to initialize the LRUCache with a given capacity.
     *
     * @param capacity The maximum number of items the cache can hold.
     * @param maxSize The maximum size of the cache.
     */
    public LRUCache(int capacity, int maxSize) {
        super(capacity, 0.75f, true);
        this.capacity = capacity;
        this.maxSize = maxSize;
        this.currentSize = 0;
    }

    /**
     * Override this method to control when the oldest entry should be removed.
     *
     * @param eldest The least recently accessed entry.
     * @return true if the eldest entry should be removed, false otherwise.
     */
    @Override
    protected synchronized boolean removeEldestEntry(Map.Entry<String, FileCache> eldest) {
        return false;
    }

    /**
     * Evict the least recently used item from the cache if the size exceeds the capacity.
     */
    public synchronized void evictIfNeeded() {
        String evictStr = "evictIfNeeded";
        for (Map.Entry<String, FileCache> entry : new ArrayList<>(entrySet())) {
            FileCache fileCache = entry.getValue();
            if (fileCache.getId() == 0 && fileCache.isModified() && fileCache.getPinCount() == 0) {
                remove(entry.getKey());
                evictStr += " | " + entry.getKey();
            }
        }
        while (currentSize > maxSize) {
            for (Map.Entry<String, FileCache> entry : new ArrayList<>(entrySet())) {
                FileCache fileCache = entry.getValue();
                if (fileCache.getId() == 0 && fileCache.getPinCount() == 0) {
                    remove(entry.getKey());
                    evictStr += " | " + entry.getKey();
                    break;
                }
            }
        }
        if (!evictStr.equals("evictIfNeeded")) {
            System.err.println(evictStr + " || " + currentSize);
        }
    }

    /**
     * Increase the size of the file cache.
     * 
     * @param key The key of the file cache.
     * @param size The new size of the file cache.
     */
    public synchronized void increaseFileCacheSize(String key, int size) {
        int sizeToAdd = size - get(key).getFileSize();
        if (sizeToAdd <= 0) {
            return;
        }
        currentSize += sizeToAdd;
        evictIfNeeded();
        get(key).setFileSize(size);
    }

    /**
     * Check if the cache contains a key.
     *
     * @param key The key to check for.
     * @return true if the key is in the cache, false otherwise.
     */
    @Override
    public synchronized boolean containsKey(Object key) {
        return super.containsKey(key);
    }

    /**
     * Get the value associated with the key.
     *
     * @param key The key to get the value for.
     * @return The value associated with the key, or null if the key was not found.
     */
    @Override
    public synchronized FileCache get(Object key) {
        return super.get(key);
    }

    /**
     * Add a key-value pair to the cache.
     * If the key already exists, its value is updated.
     *
     * @param key   The key to insert.
     * @param value The value to associate with the key.
     */
    public synchronized FileCache put(String key, FileCache value) {
        if (containsKey(key) && get(key).getId() == 0) {
            FileCache fileCache = get(key);
            fileCache.setModified(true);
            String newKey = fileCache.getPath();
            super.remove(key);
            super.put(newKey, fileCache);
        }
        currentSize += value.getFileSize();
        evictIfNeeded();
        return super.put(key, value);
    }

    /**
     * Remove the least recently used item from the cache.
     *
     * @return The removed key-value pair, or null if the cache is empty.
     */
    public synchronized FileCache remove(String key) {
        if (!containsKey(key)) {
            return null;
        }
        currentSize -= get(key).getFileSize();
        get(key).getFile().delete();
        return super.remove(key);
    }

    /**
     * Set the file cache to a base file.
     *
     * @param cacheDir The cache directory.
     * @param key The key of the file cache.
     * @param version The version of the file cache.
     */
    public synchronized FileCache setToBaseFile(String cacheDir, String key, int version) {
        if (!containsKey(key)) {
            return null;
        }
        FileCache fileCache = get(key);
        fileCache.setToBaseFile(version);
        String newKey = fileCache.getFileName();
        currentSize -= fileCache.getFileSize();
        super.remove(key);
        put(newKey, fileCache);
        fileCache.getFile().renameTo(new File(cacheDir + fileCache.getPath()));
        fileCache.setFile(new File(cacheDir + fileCache.getPath()));
        return fileCache;
    }
}