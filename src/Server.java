import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class that implements a server.
 * The server is used to retrieve store the file in the server.
 */
public class Server extends UnicastRemoteObject implements RmiInterface {
    private String rootDir; // Root directory to retrieve/store the files. Client cannot access outside this directory.
    private static ConcurrentHashMap<String, AtomicInteger> fileVersions = new ConcurrentHashMap<>(); // Map file name to current file version.

    /**
     * Constructor for the server.
     * 
     * @param rootDir The root directory.
     * @throws RemoteException If the server cannot be created.
     */
    public Server(String rootDir) throws RemoteException {
        this.rootDir = rootDir;
        File rootDirFile = new File(rootDir);
        if (!rootDirFile.exists()) {
            rootDirFile.mkdirs();
        }
    }

    /**
     * Get a file from the server.
     * 
     * @param path The path of the file.
     * @param offset The offset of the file.
     * @param length The length of the file.
     * @param finalChunk Whether the file is the final chunk.
     * @return The file.
     */
    @Override 
    public byte[] getFile(String path, int offset, int length, boolean finalChunk) throws RemoteException {
        synchronized (path.intern()) {
            Path filePath = Paths.get(rootDir, path);
            byte[] buffer = new byte[length];
            try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
                raf.seek(offset);
                raf.readFully(buffer);
                return buffer;
            } catch (IOException e) {
                throw new RemoteException("Failed to read file: " + path);
            }
        }
    }

    /**
     * Put a file to the server.
     * 
     * @param path The path of the file.
     * @param offset The offset of the file.
     * @param data The data of the file.
     * @param finalChunk Whether the file is the final chunk.
     * @return The version of the file.
     */
    @Override 
    public int putFile(String path, int offset, byte[] data, boolean finalChunk) throws RemoteException {
        synchronized (path.intern()) {
            try {
                Path filePath = Paths.get(rootDir, path);
                Files.createDirectories(filePath.getParent());
                try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw")) {
                    raf.seek(offset);
                    raf.write(data);
                    long newFileSize = Math.max(offset + data.length, raf.length());
                    raf.setLength(newFileSize);
                }
                if (!fileVersions.containsKey(path)) {
                    fileVersions.put(path, new AtomicInteger(0));
                }
                int version = finalChunk? fileVersions.get(path).incrementAndGet() : fileVersions.get(path).get();
                return version;
            } catch (IOException e) {
                throw new RemoteException("Failed to write file: " + path);
            }
        }
    }

    /**
     * Create a file on the server.
     * 
     * @param path The path of the file.
     * @return True if the file was created, false otherwise.
     */
    @Override
    public boolean createFile(String path) throws RemoteException {
        Path filePath = Paths.get(rootDir, path);
        File file = filePath.toFile();
        if (file.exists()) {
            return false;
        }
        try {
            Files.createDirectories(filePath.getParent());
            Files.createFile(filePath);
            return true;
        } catch (IOException e) {
            throw new RemoteException("Failed to create file: " + path);
        }
    }

    /**
     * Unlink a file on the server.
     * 
     * @param path The path of the file.
     * @return True if the file was unlinked, false otherwise.
     */
    @Override
    public boolean unlinkFile(String path) throws RemoteException {
        synchronized (path.intern()) {
            try {
                Path filePath = Paths.get(rootDir, path);
                return Files.deleteIfExists(filePath);
            } catch (IOException e) {
                throw new RemoteException("Failed to delete file: " + path);
            }
        }
    }

    /**
     * Get the file info of a file on the server.
     * 
     * @param path The path of the file.
     * @return The file info.
     */
    @Override
    public long[] getFileInfo(String path) throws RemoteException {
        try {
            Path filePath = Paths.get(rootDir, path);
            File file = filePath.toFile();
            return new long[]{file.length(), file.isDirectory() ? 1 : 0, fileVersions.getOrDefault(path, new AtomicInteger(0)).get(), file.exists() ? 1 : 0};
        } catch (Exception e) {
            throw new RemoteException("Failed to get file info: " + path);
        }
    }

    /**
     * Check if a file exists on the server.
     * 
     * @param path The path of the file.
     * @return True if the file exists, false otherwise.
     */
    @Override
    public boolean fileExist(String path) throws RemoteException {
        path = rootDir + path;
        File file = new File(path);
        return file.exists();
    }

    /**
     * Main method for the server.
     * 
     * @param args Arguments: port, rootdir
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java Server <port> <rootdir>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String rootDir = args[1] + "/";

        try {
            Server server = new Server(rootDir);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("ServerOperations", server);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}