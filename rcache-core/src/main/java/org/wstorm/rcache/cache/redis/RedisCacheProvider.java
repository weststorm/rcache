package org.wstorm.rcache.cache.redis;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wstorm.rcache.cache.CacheExpiredListener;
import org.wstorm.rcache.cache.CacheProvider;
import org.wstorm.rcache.enums.CacheProviderType;
import org.wstorm.rcache.exception.CacheException;
import org.wstorm.rcache.jedis.JedisWrapper;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis Cache Provider
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月10日
 */
public class RedisCacheProvider implements CacheProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private JedisWrapper jedisWrapper;
    private ConcurrentHashMap<String, RedisCache> _cacheManager = new ConcurrentHashMap<>();

    public RedisCacheProvider(JedisWrapper jedisWrapper) {
        this.jedisWrapper = jedisWrapper;
    }

    public void setJedisWrapper(JedisWrapper jedisWrapper) {
        this.jedisWrapper = jedisWrapper;
    }

    @Override
    public String name() {
        return CacheProviderType.redis.name();
    }

    public RedisCache buildCache(String regionName, CacheExpiredListener listener) throws CacheException {
        if (jedisWrapper == null) throw new CacheException("jedisWrapper 为空");

        RedisCache cache;
        if ((cache = _cacheManager.get(regionName)) == null) {
            synchronized (this) {
                if ((cache = _cacheManager.get(regionName)) == null) {
                    cache = new RedisCache(regionName, jedisWrapper);
                    _cacheManager.put(regionName, cache);
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


}
