import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * A class that implements a remote interface.
 * The remote interface is used to communicate with the server.
 */
public interface RmiInterface extends Remote {
    byte[] getFile(String path, int offset, int length, boolean finalChunk) throws RemoteException; // Fetch a file
    int putFile(String path, int offset, byte[] data, boolean finalChunk) throws RemoteException; // Update a file
    boolean createFile(String path) throws RemoteException; // Create a file
    boolean unlinkFile(String path) throws RemoteException; // Delete a file
    long[] getFileInfo(String path) throws RemoteException; // Get file info [size, isDir, lastVersion, fileExist]
    boolean fileExist(String path) throws RemoteException; // Check if a file exists
}