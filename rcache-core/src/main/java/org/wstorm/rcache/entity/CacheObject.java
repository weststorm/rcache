package org.wstorm.rcache.entity;


import java.io.Serializable;


/**
 * 所获取的缓存对象
 *
 * @param <T> 缓存对象类型
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月06日
 *
 */
public class CacheObject<T> implements Serializable {


    private String region;
    private String key;
    private T value;
    private byte level;

    /**
     * @return the region
     */
    public String getRegion() {
        return region;
    }

    /**
     * @param region the region to set
     */
    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @return the value
     */
    public T getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(T value) {
        this.value = value;
    }

    /**
     * @return the level
     */
    public byte getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(byte level) {
        this.level = level;
    }

}
