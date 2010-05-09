package com.bitmechanic.jocko.amazon;

import com.amazon.s3shell.S3Store;
import com.bitmechanic.jocko.BlobService;
import com.bitmechanic.jocko.CacheService;
import com.bitmechanic.jocko.NoOpCacheService;
import com.bitmechanic.util.Contract;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 29, 2010
 */
public class S3BlobService implements BlobService {

    public static final String S3_HOSTNAME = "s3.amazonaws.com";

    private String awsAccessId;
    private String awsSecretKey;
    private String bucket;

    private CacheService cacheService;

    public S3BlobService(String awsAccessId, String awsSecretKey, String bucket) {
        this(awsAccessId, awsSecretKey, bucket, new NoOpCacheService());
    }

    public S3BlobService(String awsAccessId, String awsSecretKey, String bucket, CacheService cacheService) {
        this.awsAccessId  = awsAccessId;
        this.awsSecretKey = awsSecretKey;
        this.bucket       = bucket;
        this.cacheService = cacheService;

        try {
            initS3().createBucket();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean delete(String key) throws IOException {
        if (cacheService != null)
            cacheService.delete(key);
        return initS3().deleteItem(key);
    }

    public int deleteByPrefix(String prefix, int maxToDelete) throws IOException {
        Contract.ensure(maxToDelete > 0, "maxToDelete must be > 0");

        S3Store s3 = initS3();
        int deleted = 0;
        for (String id : s3.listItems(prefix)) {
            s3.deleteItem(id);
            deleted++;
            if (deleted >= maxToDelete)
                break;
        }
        return deleted;
    }

    public byte[] get(String key) throws IOException {
        return get(key, false);
    }

    public byte[] getNoCache(String key) throws IOException {
        return get(key, true);
    }

    public byte[] get(String key, boolean noCache) throws IOException {
        byte[] arr = null;
        if (!noCache)
            arr = cacheService.get(key);

        if (arr == null) {
            arr = initS3().getItem(key);
            if (arr != null)
                cacheService.put(key, arr);
        }

        return arr;
    }

    public String getUrlForKey(String key) {
        return "http://" + bucket + "." + S3_HOSTNAME + "/" + key;
    }

    public void put(String key, String mimeType, boolean readableByPublic, byte[] data) throws IOException {
        cacheService.delete(key);

        Map<String, List<String>> headers = null;
        if (mimeType != null) {
            headers = new HashMap<String, List<String>>();
            headers.put("Content-Type", Collections.singletonList(mimeType));
        }

        String acl = readableByPublic ? "public-read" : "private";

        if (!initS3().storeItem(key, data, acl, headers)) {
            throw new IOException("Unable to store item with key: " + key);
        }
        cacheService.put(key, data);
    }

    private S3Store initS3() {
        S3Store s3 = new S3Store(S3_HOSTNAME, this.awsAccessId, this.awsSecretKey);
        s3.setBucket(bucket);
        return s3;
    }
    
}
