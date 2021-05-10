package io;


import androidx.annotation.NonNull;

import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

public class SuFile extends File {

    //private String[] CMDs;
    private final String filePath;
    private final boolean canNoRoot;
    private final File file;
    public SuFile(@NonNull File file) {
        this(file.getAbsolutePath(), true);
    }

    public SuFile(@NonNull File file, boolean canNoRoot) {
        super(file.getAbsolutePath());

        this.filePath = "__F_='" + file.getAbsolutePath() + "'";
        this.file = file;
        if (canNoRoot) {
            this.canNoRoot = file.canRead();
        } else {
            this.canNoRoot = false;
        }


    }

    public SuFile(String pathname) {
        this(new File(pathname), true);
    }

    public SuFile(String pathname, boolean canNoRoot) {
        this(new File(pathname), canNoRoot);
    }

    public SuFile(String parent, String child) {
        this(new File(parent, child), true);
    }

    public SuFile(String parent, String child, boolean canNoRoot) {
        this(new File(parent, child), canNoRoot);
    }

    public SuFile(File parent, String child, boolean canNoRoot) {
        this(parent.getAbsolutePath(), child, canNoRoot);
    }

    public SuFile(File parent, String child) {
        this(parent.getAbsolutePath(), child);
    }

    public SuFile(URI uri) {
        this(new File(uri), true);
    }

    public SuFile(URI uri, boolean canNoRoot) {
        this(new File(uri), canNoRoot);
    }

    private String cmd(String c) {
        String[] commands = new String[]{filePath, c};
        return ShellUtils.fastCmd(commands);
    }

    private boolean cmdBool(String c) {
        String[] commands = new String[]{filePath, c};
        return ShellUtils.fastCmdResult(commands);
    }

    @Override
    public boolean canExecute() {
        return file.canExecute() || cmdBool("[ -x \"$__F_\" ]");
    }

    @Override
    public boolean canRead() {
        return file.canRead() || cmdBool("[ -r \"$__F_\" ]");
    }

    @Override
    public boolean canWrite() {
        return file.canWrite() || cmdBool("[ -w \"$__F_\" ]");
    }

    /**
     * Creates a new, empty file named by this abstract pathname if
     * and only if a file with this name does not yet exist.
     * <p>
     * Requires command {@code touch}.
     *
     * @see File#createNewFile()
     */
    @Override
    public boolean createNewFile() {
        try {
            return file.createNewFile();
        } catch (IOException e) {
            return cmdBool("[ ! -e \"$__F_\" ] && touch \"$__F_\"");
        }
    }

    /**
     * Deletes the file or directory denoted by this abstract pathname. If
     * this pathname denotes a directory, then the directory must be empty in
     * order to be deleted.
     * <p>
     * Requires command {@code rm}, or {@code rmdir} for directories.
     *
     * @see File#delete()
     */
    @Override
    public boolean delete() {

        return file.delete() || cmdBool("rm -f \"$__F_\" || rmdir -f \"$__F_\"");
    }

    /**
     * Deletes the file or directory denoted by this abstract pathname. If
     * this pathname denotes a directory, then the directory will be recursively
     * removed.
     * <p>
     * Requires command {@code rm}.
     *
     * @see File#delete()
     */
    public boolean deleteRecursive() {
        return cmdBool("rm -rf \"$__F_\"");
    }

    /**
     * Clear the content of the file denoted by this abstract pathname.
     * Creates a new file is it does not already exist.
     *
     * @return true if operation succeed
     */
    public boolean clear() {
        return cmdBool("echo -n > \"$__F_\"");
    }

    /**
     * Unsupported
     */
    @Override
    public void deleteOnExit() {
        file.deleteOnExit();
    }

    @Override
    public boolean exists() {

        return canNoRoot ? file.exists() : cmdBool("[ -e \"$__F_\" ]");
    }

    @NonNull
    @Override
    public SuFile getAbsoluteFile() {
        return this;
    }

    /**
     * Returns the canonical pathname string of this abstract pathname.
     * <p>
     * Requires command {@code readlink}.
     *
     * @see File#getCanonicalPath()
     */
    @NonNull
    @Override
    public String getCanonicalPath() {
        try {
            return file.getCanonicalPath();
        } catch (Exception e) {
            String path = cmd("readlink -f \"$__F_\"");
            return (path.isEmpty() ? getAbsolutePath() : path);
        }
    }

    /**
     * Returns the canonical form of this abstract pathname.
     * <p>
     * Requires command {@code readlink}.
     *
     * @see File#getCanonicalFile()
     */
    @NonNull
    @Override
    public SuFile getCanonicalFile() {

        try {
            return new SuFile(file.getCanonicalPath());
        } catch (Exception e) {
            return new SuFile(getCanonicalPath());
        }
    }

    @Override
    public SuFile getParentFile() {
        String parent = getParent();
        return parent == null ? null : new SuFile(parent);
    }

    private long statFS(String fmt) {
        String[] res = cmd("stat -fc '%S " + fmt + "' \"$__F_\"").split(" ");
        if (res.length != 2)
            return Long.MAX_VALUE;
        try {
            return Long.parseLong(res[0]) * Long.parseLong(res[1]);
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }


    /**
     * Returns the number of unallocated bytes in the partition.
     * <p>
     * Requires command {@code stat}.
     *
     * @see File#getFreeSpace()
     */
    @Override
    public long getFreeSpace() {
        return canNoRoot ? file.getFreeSpace() : statFS("%f");
    }

    /**
     * Returns the size of the partition.
     * <p>
     * Requires command {@code stat}.
     *
     * @see File#getTotalSpace()
     */
    @Override
    public long getTotalSpace() {
        return canNoRoot ? file.getTotalSpace() : statFS("%b");
    }

    /**
     * Returns the number of bytes available to this process on the partition.
     * <p>
     * Requires command {@code stat}.
     *
     * @see File#getUsableSpace()
     */
    @Override
    public long getUsableSpace() {
        return canNoRoot ? file.getUsableSpace() : statFS("%a");
    }

    @Override
    public boolean isDirectory() {
        return canNoRoot ? file.isDirectory() : cmdBool("[ -d \"$__F_\" ]");
    }

    @Override
    public boolean isFile() {
        return canNoRoot ? file.isFile() : cmdBool("[ -f \"$__F_\" ]");
    }

    /**
     * @return true if the abstract pathname denotes a block device.
     */
    public boolean isBlock() {
        return cmdBool("[ -b \"$__F_\" ]");
    }

    /**
     * @return true if the abstract pathname denotes a character device.
     */
    public boolean isCharacter() {
        return cmdBool("[ -c \"$__F_\" ]");
    }

    /**
     * @return true if the abstract pathname denotes a symbolic link file.
     */
    public boolean isSymlink() {
        return cmdBool("[ -L \"$__F_\" ]");
    }

    /**
     * Returns the time that the file denoted by this abstract pathname was
     * last modified.
     * <p>
     * Requires command {@code stat}.
     *
     * @see File#lastModified()
     */
    @Override
    public long lastModified() {
        if (canNoRoot) {
            return file.lastModified();
        }
        try {
            return Long.parseLong(cmd("stat -c '%Y' \"$__F_\"")) * 1000;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Returns the length of the file denoted by this abstract pathname.
     * <p>
     * Requires either command {@code stat} or {@code wc}.
     *
     * @see File#length()
     */
    @Override
    public long length() {
        if (canNoRoot) {
            return file.length();
        }
        try {
            return Long.parseLong(cmd("stat -c '%s' \"$__F_\""));
        } catch (NumberFormatException e) {
            return 0L;

        }
    }

    /**
     * Creates the directory named by this abstract pathname.
     * <p>
     * Requires command {@code mkdir}.
     *
     * @see File#mkdir()
     */
    @Override
    public boolean mkdir() {
        return file.mkdir() || cmdBool("mkdir \"$__F_\"");
    }

    /**
     * Creates the directory named by this abstract pathname, including any
     * necessary but nonexistent parent directories.
     * <p>
     * Requires command {@code mkdir}.
     *
     * @see File#mkdirs()
     */
    @Override
    public boolean mkdirs() {
        return file.mkdirs() || cmdBool("mkdir -p \"$__F_\"");
    }

    /**
     * Renames the file denoted by this abstract pathname.
     * <p>
     * Requires command {@code mv}.
     *
     * @see File#renameTo(File)
     */
    @Override
    public boolean renameTo(@NonNull File dest) {
        return file.renameTo(dest) || cmdBool("mv -f \"$__F_\" '" + dest.getAbsolutePath() + "'");
    }

    private boolean setPerms(boolean set, boolean ownerOnly, int b) {
        char[] perms = cmd("stat -c '%a' \"$__F_\"").toCharArray();
        for (int i = 0; i < perms.length; ++i) {
            int perm = perms[i] - '0';
            if (set && (!ownerOnly || i == 0))
                perm |= b;
            else
                perm &= ~(b);
            perms[i] = (char) (perm + '0');
        }
        return cmdBool("chmod " + new String(perms) + " \"$__F_\"");
    }

    /**
     * Sets the owner's or everybody's execute permission for this abstract
     * pathname.
     * <p>
     * Requires command {@code stat} and {@code chmod}.
     *
     * @see File#setExecutable(boolean, boolean)
     */
    @Override
    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return file.setExecutable(executable, ownerOnly) || setPerms(executable, ownerOnly, 0x1);
    }


    /**
     * Sets the owner's or everybody's read permission for this abstract
     * pathname.
     * <p>
     * Requires command {@code stat} and {@code chmod}.
     *
     * @see File#setReadable(boolean, boolean)
     */
    @Override
    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return file.setReadable(readable, ownerOnly) || setPerms(readable, ownerOnly, 0x4);
    }

    /**
     * Sets the owner's or everybody's write permission for this abstract
     * pathname.
     * <p>
     * Requires command {@code stat} and {@code chmod}.
     *
     * @see File#setWritable(boolean, boolean)
     */
    @Override
    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return file.setWritable(writable, ownerOnly) || setPerms(writable, ownerOnly, 0x2);
    }

    public boolean setChmod(int chmod) {
        return cmdBool("chmod " + chmod + " \"$__F_\"");
    }

    /**
     * Marks the file or directory named by this abstract pathname so that
     * only read operations are allowed.
     * <p>
     * Requires command {@code stat} and {@code chmod}.
     *
     * @see File#setReadOnly()
     */
    @Override
    public boolean setReadOnly() {
        return file.setReadOnly() || setWritable(false, false) && setExecutable(false, false);
    }

    /**
     * Sets the last-modified time of the file or directory named by this abstract pathname.
     * <p>
     * Note: On older Android devices, the {@code touch} commands accepts a different timestamp
     * format than GNU {@code touch}. This shell implementation uses the format accepted in GNU
     * coreutils, which is the same in toybox and busybox, so the operation
     * might fail on older Android versions without busybox.
     *
     * @param time The new last-modified time, measured in milliseconds since the epoch.
     * @return {@code true} if and only if the operation succeeded; {@code false} otherwise.
     */
    @Override
    public boolean setLastModified(long time) {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmm", Locale.US);
        String date = df.format(new Date(time));
        return file.setLastModified(time) || cmdBool("[ -e \"$__F_\" ] && touch -t " + date + " \"$__F_\"");
    }

    @Override
    public String[] list() {
        return canNoRoot ? file.list(null) : list(null);
    }

    @Override
    public String[] list(FilenameFilter filter) {
        if (canNoRoot) {

            if (!file.isDirectory()) {
                return null;
            }
            return file.list(filter);

        } else if (!isDirectory()) {
            return null;
        }
        FilenameFilter defFilter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.equals(".") || name.equals("..");
            }
        };
        String[] commands = new String[]{filePath, "ls -a \"$__F_\""};
        //List<String> out = foreverShellUtils.execCommand(commands).getListSuccessMsg();
        List<String> out = Shell.su(commands).to(new LinkedList<>(), null).exec().getOut();
        String name;
        for (ListIterator<String> it = out.listIterator(); it.hasNext(); ) {
            name = it.next();
            if (filter != null && !filter.accept(this, name)) {
                it.remove();
                continue;
            }
            if (defFilter.accept(this, name))
                it.remove();
        }
        return out.toArray(new String[0]);
    }

    @Override
    public SuFile[] listFiles() {
        String[] ss;
        if (canNoRoot) {
            if (!file.isDirectory()) {
                return null;
            }
            ss = file.list();
        } else if (!isDirectory()) {
            return null;
        } else {
            ss = list();
        }
        if (ss == null) return null;
        int n = ss.length;
        SuFile[] fs = new SuFile[n];
        for (int i = 0; i < n; i++) {
            fs[i] = new SuFile(this, ss[i]);
        }
        return fs;
    }

    @Override
    public SuFile[] listFiles(FilenameFilter filter) {
        String[] ss;
        if (canNoRoot) {
            if (!file.isDirectory()) {
                return null;
            }
            ss = file.list(filter);
        } else if (!isDirectory()) {
            return null;
        } else {
            ss = list(filter);
        }
        if (ss == null) return null;
        int n = ss.length;
        SuFile[] fs = new SuFile[n];
        for (int i = 0; i < n; i++) {
            fs[i] = new SuFile(this, ss[i]);
        }

        return fs;
    }

    @Override
    public SuFile[] listFiles(FileFilter filter) {
        String[] ss;
        if (canNoRoot) {
            ss = file.list();
        } else {
            ss = list();
            if (ss == null) return null;
        }
        ArrayList<SuFile> files = new ArrayList<>();
        for (String s : ss) {
            SuFile f = new SuFile(this, s);
            if ((filter == null) || filter.accept(f))
                files.add(f);
        }
        return files.toArray(new SuFile[0]);

    }


}
