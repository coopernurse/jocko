package com.bitmechanic.jocko;

import com.bitmechanic.jocko.Infrastructure;
import com.bitmechanic.jocko.PersistableEntity;

import java.util.Collection;
import java.util.Map;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 29, 2010
 */
public class Invoice implements PersistableEntity {

    private String id;
    private long dateCreated;
    private long dateUpdated;
    private double total;
    private double shipping;

    public Invoice() { }

    public Invoice(double shipping, double total) {
        this.dateCreated = System.currentTimeMillis();
        this.dateUpdated = dateCreated;
        this.shipping = shipping;
        this.total = total;
    }

    @Override
    public Collection<EtlJob> getEtlJobs() {
        return null;
    }

    @Override
    public void setInfrastructure(Infrastructure infra) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String uuid) {
        this.id = uuid;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(long dateCreated) {
        this.dateCreated = dateCreated;
    }

    public long getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(long dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public double getShipping() {
        return shipping;
    }

    public void setShipping(double shipping) {
        this.shipping = shipping;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    @Override
    public void fillMetaData(Map<String, Object> map) {
        map.put("dateUpdated", dateUpdated);
        map.put("dateCreated", dateCreated);
    }

    @Override
    public void onAfterGet() {

    }

    @Override
    public void onBeforePut() {
        
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Invoice)) return false;

        Invoice invoice = (Invoice) o;

        if (dateCreated != invoice.dateCreated) return false;
        if (dateUpdated != invoice.dateUpdated) return false;
        if (Double.compare(invoice.shipping, shipping) != 0) return false;
        if (Double.compare(invoice.total, total) != 0) return false;
        if (id != null ? !id.equals(invoice.id) : invoice.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id != null ? id.hashCode() : 0;
        result = 31 * result + (int) (dateCreated ^ (dateCreated >>> 32));
        result = 31 * result + (int) (dateUpdated ^ (dateUpdated >>> 32));
        temp = total != +0.0d ? Double.doubleToLongBits(total) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = shipping != +0.0d ? Double.doubleToLongBits(shipping) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
