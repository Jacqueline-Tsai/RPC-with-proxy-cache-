/*
 * Decompiled with CFR 0.152.
 */
import java.io.FileOutputStream;
import java.io.IOException;

private class RPCreceiver.LatencyTest
implements Runnable {
    private RPCreceiver.LatencyTest() {
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
