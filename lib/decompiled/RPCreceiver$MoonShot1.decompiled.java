/*
 * Decompiled with CFR 0.152.
 */
import java.io.FileOutputStream;
import java.io.IOException;

private class RPCreceiver.MoonShot1
implements Runnable {
    private RPCreceiver.MoonShot1() {
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
