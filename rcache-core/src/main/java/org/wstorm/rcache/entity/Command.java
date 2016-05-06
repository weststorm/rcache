package org.wstorm.rcache.entity;

import java.io.Serializable;


/**
 * 缓存命令
 * <p>
 * @author sunyp
 * @version 1.0
 * @created 2016年05月06日
 */
public class Command implements Serializable {

    public final static transient byte OPT_DELETE_KEY = 0x01;

    private byte operator;
    private String region;
    private Object key;
    private String hostId;

    public Command(String hostId, byte operator, String region, Object key) {
        super();
        this.operator = operator;
        this.region = region;
        this.key = key;
        this.hostId = hostId;
    }

    public Command() {

    }

    public byte getOperator() {
        return operator;
    }

    public void setOperator(byte operator) {
        this.operator = operator;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

}
