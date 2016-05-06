package org.wstorm.rcache.cache;

/**
 * 缓存过期事件监听处理接口
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月06日
 */
public interface CacheExpiredListener {

    /**
     * 当缓存中的某个对象超时被清除的时候触发
     *
     * @param region Cache region name
     * @param key    cache key
     * @param <ID>   the cache key type
     */
    <ID> void notifyElementExpired(String region, ID key);

}