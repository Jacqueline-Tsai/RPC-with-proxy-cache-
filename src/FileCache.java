import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class that implements a file cache.
 * The file cache is used to store the file in the cache.
 */
class FileCache implements Cloneable {
    private int id = 0;
    private File file;
    private String fileName; // file name
    private AtomicInteger nextId = new AtomicInteger(1); // id for the next cloned file
    private int version = 0; // version of the file
    private int fileSize; // file size
    private AtomicInteger pinCount = new AtomicInteger(1); // pin count for readonly
    private boolean modified = false; // if modified for readonly, it's outdated

    // Construct readonly base file cache
    public FileCache(File file, String fileName, int fileSize, int version) {
        this.file = file;
        this.fileName = fileName; 
        this.fileSize = fileSize;
        this.version = version;
        this.pinCount.set(1);
    }

    /**
     * Clone the file cache.
     *
     * @return The cloned file cache.
     * @throws CloneNotSupportedException if the file cache is not cloneable.
     */
    @Override
    public FileCache clone() throws CloneNotSupportedException {
        if (!isReadOnly()) {
            throw new IllegalStateException("Cannot clone a non-read-only FileCache.");
        }
        FileCache cloned = (FileCache) super.clone();
        cloned.id = nextId.getAndIncrement();
        cloned.nextId = new AtomicInteger(0);
        cloned.pinCount = new AtomicInteger(0);
        cloned.setReadOnly(false);
        cloned.setPinCount0();
        return cloned;
    }

    /**
     * Get the file.
     *
     * @return The file.
     */
    public File getFile() {
        return file;
    }
    
    /**
     * Set the file.
     *
     * @param file The file.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Get the file name.
     *
     * @return The file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Set the file name.
     *
     * @param fileName The file name.
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Get the id.
     *
     * @return The id.
     */
    public int getId() {
        return id;
    }

    /**
     * Get the pin count.
     *
     * @return The pin count.
     */
    public int getPinCount() {
        return pinCount.get();
    }

    /**
     * Pin the file cache.
     */
    public void pin() {
        this.pinCount.incrementAndGet();
    }

    /**
     * Unpin the file cache.
     */
    public void unpin() {
        this.pinCount.decrementAndGet();
    }

    /**
     * Set the pin count to 0.
     */
    public void setPinCount0() {
        this.pinCount.set(0);
    }

    /**
     * Get the file size.
     *
     * @return The file size.
     */
    public int getFileSize() {
        return fileSize;
    }

    /**
     * Set the file size.
     *
     * @param fileSize The file size.
     */
    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * Get the version.
     *
     * @return The version.
     */
    public int getVersion() {
        return version;
    }

    /**
     * Set the version.
     *
     * @param version The version.
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Check if the file is read only.
     *
     * @return True if the file is read only, false otherwise.
     */
    public boolean isReadOnly() {
        return (nextId.get() > 0);
    }

    /**
     * Set the read only flag.
     *
     * @param readOnly The read only flag.
     */
    public void setReadOnly(boolean readOnly) {
        if (readOnly == isReadOnly()) {
            return;
        }
        nextId.set(readOnly? 1: 0);
        pinCount.set(readOnly? 1: 0);
    }

    /**
     * Check if the file is modified.
     *
     * @return True if the file is modified, false otherwise.
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Set the modified flag.
     *
     * @param modified The modified flag.
     */
    public void setModified(boolean modified) {
        this.modified = modified;
    }

    /**
     * Set the file cache to a base file.
     *
     * @param version The version.
     */
    public void setToBaseFile(int version) {
        this.id = 0;
        this.version = version;
        setReadOnly(true);
        setPinCount0();
        this.modified = false;
    }

    /**
     * Get the path.
     *
     * @return The path.
     */
    public String getPath() {
        return this.fileName + "." + Integer.toString(this.version) + "." + Integer.toString(this.id);
    }
}