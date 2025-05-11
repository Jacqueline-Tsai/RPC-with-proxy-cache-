/*
 * Decompiled with CFR 0.152.
 */
public interface FileHandling {
    public int open(String var1, OpenOption var2);

    public int close(int var1);

    public long write(int var1, byte[] var2);

    public long read(int var1, byte[] var2);

    public long lseek(int var1, long var2, LseekOption var4);

    public int unlink(String var1);

    public void clientdone();

    public static enum LseekOption {
        FROM_START,
        FROM_END,
        FROM_CURRENT;

    }

    public static enum OpenOption {
        READ,
        WRITE,
        CREATE,
        CREATE_NEW;
    }

    public static class Errors {
        public static final int EPERM = -1;
        public static final int ENOENT = -2;
        public static final int EBADF = -9;
        public static final int ENOMEM = -12;
        public static final int EBUSY = -16;
        public static final int EEXIST = -17;
        public static final int ENOTDIR = -20;
        public static final int EISDIR = -21;
        public static final int EINVAL = -22;
        public static final int EMFILE = -24;
        public static final int ENOSYS = -38;
        public static final int ENOTEMPTY = -39;
    }
}
