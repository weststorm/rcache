package org.wstorm.rcache.cache;

/**
 * 缓存操作接口
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月06日
 */

import org.wstorm.rcache.RObject;
import org.wstorm.rcache.annotation.CacheConfig;
import org.wstorm.rcache.exception.CacheException;

import java.util.List;
import java.util.Map;

public interface Cache {

    /**
     * 取多个缓存对象
     *
     * @param cacheConfig 缓存对象的缓存策略配置注解对象
     * @param ids         object ids, is not the finally key in cached Object identifier
     * @param dataPicker  数据初始化器
     * @return Map with ID Key And T value
     * @throws CacheException 如果缓存无法提供服务异常
     */
    <T extends RObject<String>> Map<String, T> getAll(CacheConfig cacheConfig, List<String> ids, DataPicker<String, T> dataPicker) throws CacheException;

    /**
     * 缓存多个对象
     *
     * @param cacheConfig 缓存对象的缓存策略配置注解对象
     * @param objectMap   要缓存的对象Map
     * @throws CacheException 如果缓存数据为空或缓存无法提供服务异常
     */
    <T extends RObject<String>> void putAll(CacheConfig cacheConfig, Map<String, T> objectMap) throws CacheException;

    /**
     * 从缓存中提取单个缓存对象
     *
     * @param cacheConfig 缓存的配置
     * @param id          object id, is not the finally key in cached Object identifier
     * @return the cached object or null
     */
    <T extends RObject<String>> T get(CacheConfig cacheConfig, String id, DataPicker<String, T> dataPicker) throws CacheException;

    /**
     * 增加缓存对象
     *
     * @param cacheConfig 缓存对象的缓存策略配置注解对象
     * @param id          object id, is not the finally key in cached Object identifier
     * @param value       cache value
     */
    <T extends RObject<String>> void put(CacheConfig cacheConfig, String id, T value) throws CacheException;

    /**
     * 获取所有Key
     *
     * @return 返回List
     * @throws CacheException 如果缓存无法提供服务异常
     */
    List keys() throws CacheException;

    /**
     * 从缓存中移除指定对象
     *
     * @param cacheConfig 缓存对象的缓存策略配置注解对象
     * @param id          object id, is not the finally key in cached Object identifier
     */
    void evict(CacheConfig cacheConfig, String id) throws CacheException;

    /**
     * Batch remove cache objects
     *
     * @param cacheConfig 缓存对象的缓存策略配置注解对象
     * @param ids         object ids, is not the finally key in cached Object identifier
     */
    void evict(CacheConfig cacheConfig, List<String> ids) throws CacheException;

}
