/*
 * Decompiled with CFR 0.152.
 */
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

public class RPCreceiver
implements Runnable {
    private final FileHandlingMaking fileHandlerFactory;
    private final ServerSocket proxySocket;
    private final int secretPin;

    public RPCreceiver(FileHandlingMaking fileHandlingMaking) throws IOException {
        this.fileHandlerFactory = fileHandlingMaking;
        String string = System.getenv("proxyport15440");
        int n = string != null ? Integer.parseInt(string) : 15440;
        System.out.format("RPCreceiver: Using port %d%n", n);
        String string2 = System.getenv("pin15440");
        int n2 = 0;
        if (string2 != null) {
            n2 = Integer.parseInt(string2);
        } else {
            System.out.format("RPCReceiver: please set pin15440.\n", new Object[0]);
            System.exit(1);
        }
        this.proxySocket = new ServerSocket(n);
        this.secretPin = n2;
    }

    @Override
    public void run() {
        String string = System.getenv("proxyruntrace");
        if (string != null && !string.equals("")) {
            if (string.equals("latencytest")) {
                new LatencyTest().run();
            }
            if (string.equals("moonshot1")) {
                new MoonShot1().run();
            }
            if (string.equals("moonshot2")) {
                new MoonShot2().run();
            }
            return;
        }
        try {
            while (true) {
                Socket socket = this.proxySocket.accept();
                new Thread(new ClientHandler(socket, this.fileHandlerFactory.newclient(), this.secretPin)).start();
            }
        }
        catch (IOException iOException) {
            System.out.println("RPCreceiver: Exception " + iOException);
            return;
        }
    }

    private class LatencyTest
    implements Runnable {
        private LatencyTest() {
        }

        @Override
        public void run() {
            FileHandling fileHandling = RPCreceiver.this.fileHandlerFactory.newclient();
            String[] stringArray = new String[]{"smallfile", "mediumfile", "smallfile", "mediumfile"};
            int n = 1;
            long l = System.nanoTime();
            String string = "";
            for (String string2 : stringArray) {
                System.err.println("LATENCYTEST START " + string2);
                int n2 = fileHandling.open(string2, FileHandling.OpenOption.READ);
                byte[] byArray = new byte[10];
                long l2 = fileHandling.read(n2, byArray);
                int n3 = fileHandling.close(n2);
                long l3 = System.nanoTime();
                long l4 = (l3 - l) / 1000000L;
                if (n2 < 0 || l2 < 0L || l2 < 1L) {
                    n = 0;
                }
                string = string + " " + l4;
                System.err.println("LATENCYTEST END " + string2);
                l = l3;
            }
            byte[] byArray = (n + string).getBytes();
            try {
                FileOutputStream iOException = new FileOutputStream("latencytest");
                iOException.write(byArray);
                iOException.close();
            }
            catch (IOException iOException) {
                System.out.println("Exception " + iOException);
            }
        }
    }

    private class MoonShot1
    implements Runnable {
        private MoonShot1() {
        }

        @Override
        public void run() {
            FileHandling fileHandling = RPCreceiver.this.fileHandlerFactory.newclient();
            int n = 1;
            int n2 = 0;
            long l = System.nanoTime();
            int n3 = fileHandling.open("smallfile", FileHandling.OpenOption.READ);
            byte[] byArray = new byte[8];
            long l2 = fileHandling.read(n3, byArray);
            int n4 = fileHandling.close(n3);
            if (n3 < 0 || l2 < 0L || n4 < 0 || !new String(byArray).equals("original")) {
                n = -1;
            }
            n3 = fileHandling.open("mediumfile", FileHandling.OpenOption.READ);
            l2 = fileHandling.read(n3, byArray);
            n4 = fileHandling.close(n3);
            if (n3 < 0 || l2 < 0L || n4 < 0) {
                n = 0;
            }
            for (n2 = 0; n2 < 30; ++n2) {
                n3 = fileHandling.open("smallfile", FileHandling.OpenOption.READ);
                l2 = fileHandling.read(n3, byArray);
                try {
                    Thread.sleep(67L);
                }
                catch (InterruptedException interruptedException) {
                    System.out.print(interruptedException);
                }
                n4 = fileHandling.close(n3);
                if (n3 >= 0 && l2 >= 0L && n4 >= 0) continue;
                n = -2;
            }
            long l3 = System.nanoTime();
            long l4 = (l3 - l) / 1000000L;
            if (!new String(byArray).equals("version2")) {
                n = -3;
            }
            byte[] byArray2 = (n + " " + l4).getBytes();
            try {
                FileOutputStream fileOutputStream = new FileOutputStream("moonshot");
                fileOutputStream.write(byArray2);
                fileOutputStream.close();
            }
            catch (IOException iOException) {
                System.out.println("Exception " + iOException);
            }
        }
    }

    private class MoonShot2
    implements Runnable {
        private MoonShot2() {
        }

        @Override
        public void run() {
            FileHandling fileHandling = RPCreceiver.this.fileHandlerFactory.newclient();
            try {
                Thread.sleep(1000L);
            }
            catch (InterruptedException interruptedException) {
                System.out.print(interruptedException);
            }
            int n = fileHandling.open("smallfile", FileHandling.OpenOption.WRITE);
            byte[] byArray = new String("version2").getBytes();
            long l = fileHandling.write(n, byArray);
            int n2 = fileHandling.close(n);
        }
    }

    private static class ClientHandler
    implements Runnable {
        private final Socket clientSocket;
        private final FileHandling fileHandler;
        private InputStream clientIn;
        private OutputStream clientOut;
        private static final int MIN_FD = 1000;
        private final int secretPin;

        public ClientHandler(Socket socket, FileHandling fileHandling, int n) {
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
}
