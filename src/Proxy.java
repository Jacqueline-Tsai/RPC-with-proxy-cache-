import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.OutputStream;

/**
 * A class that implements a proxy.
 * The proxy is used to proxy the requests to the server.
 */
class Proxy {
    private static final int MAX_CHUNK_SIZE = 500000; // Maximum chunk size
    private static String host = "0.0.0.0", cacheDir = "cache/"; // Host and cache directory
    private static int port = -1, cacheSize = 0; // Port and cache size
    private static RmiInterface server = null; // Server
    private static ConcurrentHashMap<Integer, Long> fdOffsets = new ConcurrentHashMap<>(); // File descriptor offsets
    private static ConcurrentHashMap<Integer, FileCache> fdFileCache = new ConcurrentHashMap<>(); // File descriptor file cache
    private static LRUCache fileNameFileCache = null; // Map file name to file cache, with LRU eviction
    private static AtomicInteger nextFd = new AtomicInteger(0); // Next file descriptor

    /**
     * A class that implements a file handler.
     * The file handler is used to handle the file.
     */
    private static class FileHandler implements FileHandling {

        /**
         * Write a file from server to local. Chunk size is MAX_CHUNK_SIZE.
         * 
         * @param fileName The file name on server.
         * @param basePath The base path for local file.
         * @param fileSize The total file size.
         * @return The local file.
         */
        private static File writeFromServerToLocal(String fileName, String basePath, int fileSize) throws IOException {
            Path localPath = Paths.get(cacheDir, basePath);
            Files.createDirectories(localPath.getParent());
            try (OutputStream outputStream = Files.newOutputStream(localPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                int offset = 0;
                while (offset < fileSize - MAX_CHUNK_SIZE) {
                    byte[] fileContent = server.getFile(fileName, offset, MAX_CHUNK_SIZE, false);
                    outputStream.write(fileContent);
                    offset += MAX_CHUNK_SIZE;
                }
                int chunkSize = fileSize - offset;
                byte[] fileContent = server.getFile(fileName, offset, chunkSize, true);
                outputStream.write(fileContent);
            }
            return localPath.toFile();
        }

        /**
         * Write from local to server.
         * 
         * @param fileName The file name on server.
         * @param basePath The base path for local file.
         * @param fileSize The total file size.
         * @return The version of the file.
         */
        private static int writeFromLocalToServer(String fileName, String basePath, int fileSize) throws IOException {
            Path localPath = Paths.get(cacheDir, basePath);
            if (!Files.exists(localPath)) {
                throw new IOException("Local file not found: " + localPath);
            }
            int version;
            try (InputStream inputStream = Files.newInputStream(localPath, StandardOpenOption.READ)) {
                int offset = 0;
                while (offset < fileSize - MAX_CHUNK_SIZE) {
                    byte[] fileContent = new byte[MAX_CHUNK_SIZE];
                    int bytesRead = inputStream.read(fileContent);
                    if (bytesRead == -1) {
                        break;
                    }
                    version = server.putFile(fileName, offset, fileContent, false);
                    offset += bytesRead;
                }
                int chunkSize = fileSize - offset;
                byte[] fileContent = new byte[chunkSize];
                int bytesRead = inputStream.read(fileContent);
                if (bytesRead == -1) {
                    throw new IOException("Failed to read final chunk from local file: " + fileName);
                }
                version = server.putFile(fileName, offset, fileContent, true);
            }
            return version;
        }

        /**
         * Open a file.
         * 
         * @param path The path.
         * @param o The open option.
         */
        public int open(String path, OpenOption o) {
            Path normalizedPath = Paths.get(path).normalize();
            if (!normalizedPath.isAbsolute() && normalizedPath.getNameCount() > 0 
                && normalizedPath.getName(0).toString().equals("..")) {
                return Errors.EINVAL;
            }
            path = normalizedPath.toString();
            synchronized (path.intern()) {
                boolean fileInCache;
                boolean fileExist;
                long[] fileInfo;
                try {
                    fileInCache = fileNameFileCache.containsKey(path);
                    fileInfo = server.getFileInfo(path);
                    fileExist = fileInfo[3] != 0;
                } catch (RemoteException e) {
                    return Errors.EINVAL; // I/O error
                }
                if (o == OpenOption.CREATE_NEW && fileExist) {
                    return Errors.EEXIST;
                }
                if ((o == OpenOption.CREATE && !fileExist) || o == OpenOption.CREATE_NEW) {
                    try {
                        if (!server.createFile(path)) {
                            return Errors.EEXIST; // File already exists
                        }
                        fileExist = true;
                    } catch (IOException e) {
                        return Errors.EINVAL; // I/O error
                    }
                }
                if (!fileExist) {
                    return Errors.ENOENT;
                }
                int fd = nextFd.getAndIncrement();
                if (fileInfo[1] == 1) {
                    return fd;
                }
                try {
                    if (fileInCache && (fileInfo[2] == fileNameFileCache.get(path).getVersion())) {
                        FileCache fileCache = fileNameFileCache.get(path);
                        String basePath = fileCache.getPath();
                        if (o == OpenOption.READ) {
                            fileCache.pin();
                        }
                        else {
                            Path source = Paths.get(cacheDir, fileCache.getPath());
                            fileCache = fileCache.clone();
                            Path target = Paths.get(cacheDir, fileCache.getPath());
                            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                            File newFile = target.toFile();
                            newFile.createNewFile();
                            fileCache.setFile(newFile);
                            fileNameFileCache.put(fileCache.getPath(), fileCache);
                        }
                        fdFileCache.put(fd, fileCache);
                        fdOffsets.put(fd, 0L);
                        return fd;
                    }
                    String basePath = path + "." + fileInfo[2] + ".0";
                    File file = writeFromServerToLocal(path, basePath, (int)fileInfo[0]);
                    FileCache fileCache = new FileCache(file, path, (int)fileInfo[0], (int)fileInfo[2]);
                    fileNameFileCache.put(path, fileCache);
                    if (o != OpenOption.READ) {
                        Path source = Paths.get(cacheDir, fileCache.getPath());
                        fileCache = fileCache.clone();
                        Path target = Paths.get(cacheDir, fileCache.getPath());
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                        File newFile = target.toFile();
                        newFile.createNewFile();
                        fileCache.setFile(newFile);
                        fileNameFileCache.put(fileCache.getPath(), fileCache);
                    }
                    fdFileCache.put(fd, fileCache);
                    fdOffsets.put(fd, 0L);
                    return fd;
                } catch (IOException e) {
                    return Errors.EINVAL; // I/O error
                } catch (CloneNotSupportedException e) {
                    return Errors.EINVAL; // I/O error
                }
            }
        }

        /**
         * Close a file.
         * 
         * @param fd The file descriptor.
         * @return 0 if success, otherwise an error code.
         */
        public int close(int fd) {
            if (!fdFileCache.containsKey(fd)) {
                return Errors.EBADF; // Invalid file descriptor
            }
            FileCache fileCache = fdFileCache.get(fd);
            // get for LRU order
            if (fileNameFileCache.containsKey(fileCache.getPath())) {
                fileNameFileCache.get(fileCache.getPath());
            }
            else {
                fileNameFileCache.get(fileCache.getFileName());
            }

            fdFileCache.remove(fd);
            if (fileCache.getId() == 0) {
                fileCache.unpin();
                return 0;
            }
            try {
                if (!fileCache.isModified()) {
                    fileNameFileCache.remove(fileCache.getPath());
                }
                else {
                    String filePath = fileCache.getPath();
                    int version = writeFromLocalToServer(fileCache.getFileName(), filePath, (int)fileCache.getFileSize());
                    fileNameFileCache.setToBaseFile(cacheDir, filePath, version);
                }
            } catch (IOException e) {
                return Errors.EINVAL; // I/O error
            }
            return 0; // Success
        }

        /**
         * Write to a file.
         * 
         * @param fd The file descriptor.
         * @param buf The buffer.
         * @return The number of bytes written.
         */
        public long write(int fd, byte[] buf) {
            if (!fdFileCache.containsKey(fd)) {
                return Errors.EBADF;
            }

            FileCache fileCache = fdFileCache.get(fd);

            if (fileCache.isReadOnly()) {
                return Errors.EBADF;
            }
            
            File file = fileCache.getFile();

            long pointer = fdOffsets.get(fd);
            fileCache.setModified(true);

            // Synchronize file operations to prevent concurrent writes
            synchronized (file) {
                try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                    raf.seek(pointer);
                    int modifiedFileSize = (int)Math.max(pointer + buf.length, fileCache.getFileSize());
                    fileNameFileCache.increaseFileCacheSize(fileCache.getPath(), modifiedFileSize);
                    raf.write(buf);
                    fdOffsets.put(fd, pointer + buf.length);
                    return buf.length;
                } catch (IOException e) {
                    return Errors.EINVAL; // I/O error
                }
            }
        }

        /**
         * Read from a file.
         * 
         * @param fd The file descriptor.
         * @param buf The buffer.
         * @return The number of bytes read.
         */
        public long read(int fd, byte[] buf) {
            if (!fdFileCache.containsKey(fd)) {
                return Errors.EBADF; // Invalid file descriptor
            }

            FileCache fileCache = fdFileCache.get(fd);
            File file = fileCache.getFile();

            synchronized (file) {
                long pointer = fdOffsets.get(fd);
                try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                    raf.seek(pointer);
                    int bytesRead = raf.read(buf);
                    if (bytesRead > 0) {
                        fdOffsets.put(fd, pointer + bytesRead);
                    }
                    return bytesRead == -1 ? 0 : bytesRead;
                } catch (IOException e) {
                    return Errors.EINVAL; // I/O error
                }
            }
        }

        /**
         * Lseek a file.
         * 
         * @param fd The file descriptor.
         * @param pos The position.
         * @param o The lseek option.
         * @return The new position.
         */
        public long lseek(int fd, long pos, LseekOption o) {
            if (!fdFileCache.containsKey(fd)) {
                return Errors.EBADF; // Invalid file descriptor
            }

            FileCache fileCache = fdFileCache.get(fd);
            long pointer = fdOffsets.get(fd);

            synchronized (fileCache) {
                switch (o) {
                    case FROM_START:
                        pointer = pos;
                        break;
                    case FROM_CURRENT:
                        pointer += pos;
                        break;
                    case FROM_END:
                        pointer = fileCache.getFileSize() + pos;
                        break;
                }
                if (pointer < 0) {
                    return Errors.EINVAL;
                }
                fdOffsets.put(fd, pointer);
                return pointer;
            }
        }

        /**
         * Unlink a file.
         * 
         * @param path The path.
         * @return 0 if success, otherwise an error code.
         */
        public int unlink(String path) {
            boolean fileExist;
            long[] fileInfo;
            try {
                fileInfo = server.getFileInfo(path);
                fileExist = fileInfo[3] != 0;
            } catch (RemoteException e) {
                return Errors.EINVAL; // I/O error
            }
            if (!fileExist) {
                return Errors.ENOENT; // No such file or directory
            }
            if (fileInfo[1] == 1) {
                return Errors.EISDIR; // Not a directory
            }
            try {
                server.unlinkFile(path);
                return 0;
            } catch (RemoteException e) {
                return Errors.EINVAL; // I/O error
            }
        }

        public void clientdone() {

        }
    }

    /**
     * A class that implements a file handling factory.
     * The file handling factory is used to create a file handler.
     */
    private static class FileHandlingFactory implements FileHandlingMaking {
        /**
         * New client.
         */
        public FileHandling newclient() {
            return new FileHandler();
        }
    }

    /**
     * Main method.
     * 
     * @param args Arguments: serverip, port, cachedir, cachesize
     */
    public static void main(String[] args) throws IOException {
        try {
            host = args[0];
            port = Integer.parseInt(args[1]);
            cacheDir = args[2] + "/";
            cacheSize = Integer.parseInt(args[3]);
            fileNameFileCache = new LRUCache(2000, cacheSize);
            File cacheDirFile = new File(cacheDir);
            cacheDirFile.mkdirs();
        } catch (Exception e) {
            System.err.println("Usage: java Proxy <serverip> <port> <cachedir> <cachesize>");
            System.exit(1);
        }

        // bind a RMI service
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            server = (RmiInterface) registry.lookup("ServerOperations");
        } catch (Exception e) {
            e.printStackTrace();
        }

        (new RPCreceiver(new FileHandlingFactory())).run();
    }
}