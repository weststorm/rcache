package org.wstorm.rcache.cache.memcached;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wstorm.rcache.cache.CacheExpiredListener;
import org.wstorm.rcache.cache.CacheProvider;
import org.wstorm.rcache.enums.CacheProviderType;
import org.wstorm.rcache.exception.CacheException;
import org.wstorm.rcache.memcached.SpyMemcachedClient;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Memcached Cache Provider
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月08日
 */
public class MemcachedProvider implements CacheProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 缓存入口
     */
    private SpyMemcachedClient memcached;

    private ConcurrentHashMap<String, MemcachedCache> _cacheManager = new ConcurrentHashMap<>();

    public MemcachedProvider(SpyMemcachedClient memcached) {
        super();
        this.memcached = memcached;
    }

    @Override
    public String name() {
        return CacheProviderType.memcached.name();
    }

    public MemcachedCache buildCache(String name, CacheExpiredListener listener) throws CacheException {
        if (memcached == null) throw new CacheException("memcached client 为空");

        MemcachedCache cache;
        if ((cache = _cacheManager.get(name)) == null) {
            synchronized (this) {
                if ((cache = _cacheManager.get(name)) == null) {
                    cache = new MemcachedCache(name, memcached);
                    _cacheManager.put(name, cache);
                }
            }
        }

        return cache;
    }

    /**
     * Callback to perform any necessary initialization of the underlying cache
     * implementation during SessionFactory construction.
     *
     * @param configFile current configuration settings.
     */
    public void start(String configFile) throws CacheException {
    }

    /**
     * Callback to perform any necessary cleanup of the underlying cache
     * implementation during SessionFactory.close().
     */
    public void stop() {
        log.warn("memcached停止由容器管理,不由缓存提供者管理!");
    }

    /**
     * @param memcached the memcached to set
     */
    public void setMemcached(SpyMemcachedClient memcached) {
        this.memcached = memcached;
    }

}
