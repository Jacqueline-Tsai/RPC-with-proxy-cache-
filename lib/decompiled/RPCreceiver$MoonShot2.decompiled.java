/*
 * Decompiled with CFR 0.152.
 */
private class RPCreceiver.MoonShot2
implements Runnable {
    private RPCreceiver.MoonShot2() {
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
