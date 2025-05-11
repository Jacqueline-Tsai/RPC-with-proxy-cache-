/*
 * Decompiled with CFR 0.152.
 */
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

private static class RPCreceiver.ClientHandler
implements Runnable {
    private final Socket clientSocket;
    private final FileHandling fileHandler;
    private InputStream clientIn;
    private OutputStream clientOut;
    private static final int MIN_FD = 1000;
    private final int secretPin;

    public RPCreceiver.ClientHandler(Socket socket, FileHandling fileHandling, int n) {
        this.clientSocket = socket;
        this.fileHandler = fileHandling;
        this.secretPin = n;
    }

    private int readInt() throws EOFException, IOException {
        int n = 0;
        for (int i = 0; i < 4; ++i) {
            int n2 = this.clientIn.read();
            if (n2 < 0 || n2 > 255) {
                throw new EOFException();
            }
            n |= n2 << i * 8;
        }
        return n;
    }

    private long readLong() throws EOFException, IOException {
        long l = 0L;
        for (int i = 0; i < 8; ++i) {
            long l2 = this.clientIn.read();
            if (l2 < 0L || l2 > 255L) {
                throw new EOFException();
            }
            l |= l2 << i * 8;
        }
        return l;
    }

    private void readBuf(byte[] byArray) throws EOFException, IOException {
        int n;
        int n2 = 0;
        for (int i = byArray.length; i > 0; i -= n) {
            n = this.clientIn.read(byArray, n2, i);
            if (n < 0) {
                throw new EOFException();
            }
            n2 += n;
        }
    }

    private void writeInt(byte[] byArray, int n, int n2) {
        for (int i = 0; i < 4; ++i) {
            byArray[n + i] = (byte)(n2 >> i * 8 & 0xFF);
        }
    }

    private void writeLong(byte[] byArray, int n, long l) {
        for (int i = 0; i < 8; ++i) {
            byArray[n + i] = (byte)(l >> i * 8 & 0xFFL);
        }
    }

    private void writeBuf(byte[] byArray, int n, byte[] byArray2) {
        for (int i = 0; i < byArray2.length; ++i) {
            byArray[n + i] = byArray2[i];
        }
    }

    private boolean challenge() throws IOException {
        byte[] byArray = new byte[4];
        Random random = new Random();
        int n = random.nextInt() & 0x3FFFFFFF;
        int n2 = this.readInt();
        n2 = (n2 ^ this.secretPin) & 0x3FFFFFFF;
        int n3 = n + n2 ^ this.secretPin;
        this.writeInt(byArray, 0, n3);
        this.clientOut.write(byArray);
        int n4 = this.readInt();
        n4 = (n4 ^ this.secretPin) & 0x3FFFFFFF;
        if (n4 == n) {
            this.writeInt(byArray, 0, 87104);
            this.clientOut.write(byArray);
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        block28: {
            try {
                this.clientIn = this.clientSocket.getInputStream();
                this.clientOut = this.clientSocket.getOutputStream();
                if (!this.challenge()) break block28;
                block19: while (true) {
                    int n;
                    try {
                        n = this.readInt();
                    }
                    catch (EOFException eOFException) {
                        break block28;
                    }
                    switch (n) {
                        case 1: {
                            FileHandling.OpenOption openOption;
                            int n2 = this.readInt();
                            switch (n2) {
                                case 1: {
                                    openOption = FileHandling.OpenOption.WRITE;
                                    break;
                                }
                                case 2: {
                                    openOption = FileHandling.OpenOption.CREATE;
                                    break;
                                }
                                case 3: {
                                    openOption = FileHandling.OpenOption.CREATE_NEW;
                                    break;
                                }
                                default: {
                                    openOption = FileHandling.OpenOption.READ;
                                }
                            }
                            int n3 = this.readInt();
                            byte[] byArray = new byte[n3];
                            this.readBuf(byArray);
                            Object object = new String(byArray);
                            int n4 = this.fileHandler.open((String)object, openOption);
                            if (n4 >= 0) {
                                n4 += 1000;
                            }
                            byte[] byArray2 = new byte[4];
                            this.writeInt(byArray2, 0, n4);
                            this.clientOut.write(byArray2);
                            continue block19;
                        }
                        case 2: {
                            int n2 = this.readInt() - 1000;
                            int n5 = this.fileHandler.close(n2);
                            byte[] byArray = new byte[4];
                            this.writeInt(byArray, 0, n5);
                            this.clientOut.write(byArray);
                            continue block19;
                        }
                        case 3: {
                            long l;
                            int n2 = this.readInt() - 1000;
                            long l2 = this.readLong();
                            byte[] byArray2 = new byte[8];
                            if (l2 > 65536L) {
                                l2 = 65536L;
                            }
                            if (l2 < 0L) {
                                l = -22L;
                            } else {
                                byte[] byArray = new byte[(int)l2];
                                l = this.fileHandler.read(n2, byArray);
                                if (l > 0L) {
                                    byArray2 = new byte[8 + (int)l];
                                    this.writeBuf(byArray2, 8, Arrays.copyOfRange(byArray, 0, (int)l));
                                }
                            }
                            this.writeLong(byArray2, 0, l);
                            this.clientOut.write(byArray2);
                            continue block19;
                        }
                        case 4: {
                            int n2 = this.readInt() - 1000;
                            long l = this.readLong();
                            byte[] byArray = new byte[(int)l];
                            byte[] byArray2 = new byte[8];
                            this.readBuf(byArray);
                            long l3 = this.fileHandler.write(n2, byArray);
                            this.writeLong(byArray2, 0, l3);
                            this.clientOut.write(byArray2);
                            continue block19;
                        }
                        case 5: {
                            int n2 = this.readInt() - 1000;
                            long l = this.readLong();
                            int n6 = this.readInt();
                            Object object = FileHandling.LseekOption.FROM_START;
                            if (n6 == 2) {
                                object = FileHandling.LseekOption.FROM_END;
                            } else if (n6 == 1) {
                                object = FileHandling.LseekOption.FROM_CURRENT;
                            }
                            long l4 = this.fileHandler.lseek(n2, l, (FileHandling.LseekOption)((Object)object));
                            byte[] byArray = new byte[8];
                            this.writeLong(byArray, 0, l4);
                            this.clientOut.write(byArray);
                            continue block19;
                        }
                        case 7: {
                            int n2 = this.readInt();
                            byte[] byArray = new byte[n2];
                            this.readBuf(byArray);
                            String string = new String(byArray);
                            int n7 = this.fileHandler.unlink(string);
                            Object object = new byte[4];
                            this.writeInt((byte[])object, 0, n7);
                            this.clientOut.write((byte[])object);
                            continue block19;
                        }
                    }
                    break;
                }
                throw new IOException();
            }
            catch (IOException iOException) {
                System.out.println("ClientHandler: Exception " + iOException);
            }
        }
        this.fileHandler.clientdone();
        try {
            this.clientSocket.close();
        }
        catch (IOException iOException) {
            System.out.println("ClientHandler: Exception " + iOException);
        }
    }
}
