package org.wstorm.rcache;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wstorm.rcache.annotation.CacheConfig;
import org.wstorm.rcache.cache.Cache;
import org.wstorm.rcache.cache.CacheExpiredListener;
import org.wstorm.rcache.cache.CacheProvider;
import org.wstorm.rcache.cache.DataPicker;
import org.wstorm.rcache.cache.ehcache.EhCacheProvider;
import org.wstorm.rcache.cache.nulls.NullCacheProvider;
import org.wstorm.rcache.enums.CacheProviderType;
import org.wstorm.rcache.exception.CacheException;
import org.wstorm.rcache.utils.CollectionsUtils;

import java.util.List;
import java.util.Map;

/**
 * 缓存管理器
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年5月07日
 */
public class CacheManager {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private CacheProvider l1_provider;
    private CacheProvider l2_provider;

    public CacheManager(CacheProvider l2_provider, String l1_provider, String l1_configFile) {

        Preconditions.checkArgument(l1_provider != null);

        this.l2_provider = l2_provider;

        try {

            this.l1_provider = getProviderInstance(l1_provider);

            this.l1_provider.start(l1_configFile);

            log.info("Using L1 CacheProvider : " + l1_provider);

        } catch (Exception e) {
            throw new CacheException("Unable to initialize cache providers", e);
        }
    }

    private CacheProvider getProviderInstance(String value) throws Exception {
        if (CacheProviderType.ehcache.name().equalsIgnoreCase(value))
            return new EhCacheProvider();
        if (CacheProviderType.none.name().equalsIgnoreCase(value))
            return new NullCacheProvider();
        return (CacheProvider) Class.forName(value).newInstance();
    }

    private Cache _GetCache(int level, String cache_name, CacheExpiredListener listener) {
        return ((level == 1) ? l1_provider : l2_provider).buildCache(cache_name, listener);
    }

    public final void shutdown(int level) {
        ((level == 1) ? l1_provider : l2_provider).stop();
    }

    /**
     * 获取缓存中的数据
     *
     * @param level       Cache Level: L1 and L2
     * @param cacheConfig Cache region name
     * @param key         Cache key
     * @param listener    listener
     * @param dataPicker  数据提取器 用于缓存失效后回调
     * @param <T>         缓存对象类型
     * @return Cache object
     */
    public final <T extends RObject<String>> T get(int level, CacheConfig cacheConfig, String key, CacheExpiredListener listener, DataPicker<String, T> dataPicker) {
        // System.out.println("GET1 => " + name+":"+key);
        if (key != null) {
            Cache cache = _GetCache(level, cacheConfig.region(), listener);
            if (cache != null)
                try {
                    return cache.get(cacheConfig, key, dataPicker);
                } catch (Exception e) {
                    log.warn("get {} exception: {}", e.getClass(), e.getLocalizedMessage());
                    if (log.isDebugEnabled()) {
                        log.error("get", e);
                    }
                }
        }
        return null;
    }

    /**
     * 获取缓存中的数据
     *
     * @param level       Cache Level: L1 and L2
     * @param cacheConfig Cache配置
     * @param keys        Cache keys
     * @param listener    缓存过期监听器
     * @param dataPicker  缓存失效后的数据提取回调接口
     * @param <T>         缓存对象类型
     * @return 返回缓存Key, Value Map
     */
    public final <T extends RObject<String>> Map<String, T> getAll(int level, CacheConfig cacheConfig, List<String> keys, CacheExpiredListener listener, DataPicker<String, T> dataPicker) {
        // System.out.println("GET1 => " + name+":"+key);
        if (keys != null) {
            Cache cache = _GetCache(level, cacheConfig.region(), listener);
            if (cache != null)
                try {
                    return cache.getAll(cacheConfig, keys, dataPicker);
                } catch (Exception e) {
                    log.warn("getAll {} exception: {}", e.getClass(), e.getLocalizedMessage());
                    if (log.isDebugEnabled()) {
                        log.error("get", e);
                    }
                }
        }
        return null;
    }

    public final <T extends RObject<String>> void setAll(int level, CacheConfig cacheConfig, Map<String, T> objects, CacheExpiredListener listener) {
        if (CollectionsUtils.isNotEmpty(objects)) {
            Cache cache = _GetCache(level, cacheConfig.region(), listener);
            if (cache != null)
                try {
                    cache.putAll(cacheConfig, objects);
                } catch (Exception e) {
                    log.warn("setAll {} exception: {}", e.getClass(), e.getLocalizedMessage());
                    if (log.isDebugEnabled()) {
                        log.error("get", e);
                    }
                }
        }
    }

    public final <T extends RObject<String>> void set(int level, CacheConfig cacheConfig, String key, T value, CacheExpiredListener listener) {
        if (key != null && value != null) {
            Cache cache = _GetCache(level, cacheConfig.region(), listener);
            if (cache != null)
                try {
                    cache.put(cacheConfig, key, value);
                } catch (Exception e) {
                    log.warn("set {} exception: {}", e.getClass(), e.getLocalizedMessage());
                    if (log.isDebugEnabled()) {
                        log.error("get", e);
                    }
                }
        }
    }

    /**
     * 清除缓存中的某个数据
     *
     * @param level Cache Level: L1 and L2
     * @param name  Cache region name
     * @param key   Cache key
     */
    public final void evict(int level, CacheConfig cacheConfig, String name, String key, CacheExpiredListener listener) {
        // batchEvict(level, name, java.util.Arrays.asList(key));
        if (name == null && cacheConfig != null) {
            name = cacheConfig.region();
        }
        if (name != null && key != null) {
            Cache cache = _GetCache(level, name, listener);
            if (cache != null)
                try {
                    cache.evict(cacheConfig, key);
                } catch (Exception e) {
                    log.warn("evict {} exception: {}", e.getClass(), e.getLocalizedMessage());
                    if (log.isDebugEnabled()) {
                        log.error("get", e);
                    }
                }
        }
    }

    /**
     * 批量删除缓存中的一些数据
     *
     * @param level       Cache Level： L1 and L2
     * @param cacheConfig cache 配置
     * @param name        Cache region name
     * @param keys        Cache keys
     * @param listener    过期监听器
     */
    public final void batchEvict(int level, CacheConfig cacheConfig, String name, List<String> keys, CacheExpiredListener listener) {
        if (cacheConfig != null) {
            name = cacheConfig.region();
        }
        if (name != null && keys != null && keys.size() > 0) {
            Cache cache = _GetCache(level, name, listener);
            if (cache != null)
                try {
                    cache.evict(cacheConfig, keys);
                } catch (Exception e) {
                    log.warn("batchEvict {} exception: {}", e.getClass(), e.getLocalizedMessage());
                    if (log.isDebugEnabled()) {
                        log.error("get", e);
                    }
                }
        }
    }

    /**
     * list cache keys
     *
     * @param level Cache level
     * @param name  cache region name
     * @return Key List
     */
    public final List keys(int level, String name, CacheExpiredListener listener) throws CacheException {
        Cache cache = _GetCache(level, name, listener);
        return (cache != null) ? cache.keys() : null;
    }
}
