package org.wstorm.rcache.cache;

/**
 * 缓存操作接口
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月06日
 */

import org.wstorm.rcache.annotation.CacheConfig;
import org.wstorm.rcache.exception.CacheException;

import java.util.List;
import java.util.Map;

public interface Cache {

    /**
     * 取多个缓存对象
     *
     * @param cacheConfig 缓存对象的缓存策略配置注解对象
     * @param keys        cache keys
     * @param initData    数据初始化器
     * @return Map with ID Key And T value
     * @throws CacheException 如果缓存无法提供服务异常
     */
    public <ID, T> Map<ID, T> getAll(CacheConfig cacheConfig, List<ID> keys, DataPicker<ID, T> initData) throws CacheException;

    /**
     * 缓存多个对象
     *
     * @param cacheConfig 缓存对象的缓存策略配置注解对象
     * @param objectMap   要缓存的对象Map
     * @throws CacheException 如果缓存数据为空或缓存无法提供服务异常
     */
    public <ID, T> void putAll(CacheConfig cacheConfig, Map<ID, T> objectMap) throws CacheException;

    /**
     * 从缓存中提取单个缓存对象
     *
     * @param cacheConfig 缓存的配置
     * @param key         cache key
     * @return the cached object or null
     */
    public <ID, T> T get(CacheConfig cacheConfig, ID key, DataPicker<ID, T> initData) throws CacheException;

    /**
     * 增加缓存对象
     *
     * @param cacheConfig 缓存对象的缓存策略配置注解对象
     * @param key         cache key
     * @param value       cache value
     */
    public <ID, T> void put(CacheConfig cacheConfig, ID key, T value) throws CacheException;

    /**
     * 获取所有Key
     *
     * @return 返回List
     * @throws CacheException 如果缓存无法提供服务异常
     */
    public List<String> keys() throws CacheException;

    /**
     * 从缓存中移除指定对象
     *
     * @param cacheConfig 缓存对象的缓存策略配置注解对象
     * @param key         the cache keys to be evicted
     */
    public <ID> void evict(CacheConfig cacheConfig, ID key) throws CacheException;

    /**
     * Batch remove cache objects
     *
     * @param cacheConfig 缓存对象的缓存策略配置注解对象
     * @param keys        the cache keys to be evicted
     */
    public <ID> void evict(CacheConfig cacheConfig, List<ID> keys) throws CacheException;

}
