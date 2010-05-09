package com.bitmechanic.jocko.local;

import com.bitmechanic.jocko.BlobService;
import com.bitmechanic.util.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 29, 2010
 */
@SuppressWarnings({"AppEngineForbiddenCode"})
public class LocalBlobService implements BlobService {

    private File blobDir;

    public LocalBlobService() {
        this(System.getProperty("java.io.tmpdir"));
    }

    public LocalBlobService(String dir) {
        this(new File(dir));
    }

    public LocalBlobService(File blobDir) {
        this.blobDir = blobDir;

        if (!blobDir.exists()) {
            throw new IllegalArgumentException("Directory: " + blobDir + " does not exist");
        }
        else if (!blobDir.isDirectory()) {
            throw new IllegalArgumentException("Path: " + blobDir + " is not a directory");
        }
        else if (!blobDir.canWrite()) {
            throw new IllegalArgumentException("Cannot write to: " + blobDir);
        }
    }

    @Override
    public boolean delete(String key) throws IOException {
        File file = getFile(key);
        return file.delete();
    }

    @Override
    public int deleteByPrefix(String prefix, int maxToDelete) throws IOException {
        int deleted = 0;
        for (File file : blobDir.listFiles()) {
            if (file.getName().startsWith(prefix)) {
                if (file.delete()) {
                    deleted++;
                }
                else {
                    throw new RuntimeException("Can't delete file: " + file);
                }

                if (deleted >= maxToDelete)
                    break;
            }
        }
        return deleted;
    }

    @Override
    public byte[] get(String key) throws IOException {
        File file = getFile(key);
        if (file.exists())
            return IOUtil.getBytesFromFile(file);
        else
            return null;
    }

    @Override
    public byte[] getNoCache(String key) throws IOException {
        return get(key);
    }

    @Override
    public void put(String key, String mimeType, boolean readableByPublic, byte[] data) throws IOException {
        File file = getFile(key);
        FileOutputStream fos = new FileOutputStream(file);
        for (int i = 0; i < data.length; i++) {
            fos.write(data[i]);
        }
        fos.flush();
        fos.close();
    }

    @Override
    public String getUrlForKey(String key) {
        return "file://" + getFile(key).getAbsolutePath();
    }

    private File getFile(String key) {
        return new File(blobDir.getAbsolutePath() + File.separator + key);
    }

}
