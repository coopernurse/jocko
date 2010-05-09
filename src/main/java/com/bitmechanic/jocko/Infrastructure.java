package com.bitmechanic.jocko;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Nov 8, 2009
 */
public interface Infrastructure {

    public PersistenceService getPersistenceService();
    public BlobService getBlobService();
    public MailService getMailService();
    public QueueService getQueueService();
    public CacheService getCacheService();
    public AsyncService getAsyncService();
    public ImageService getImageService();
    public BillingService getBillingService();
    public LockService getLockService();
}
