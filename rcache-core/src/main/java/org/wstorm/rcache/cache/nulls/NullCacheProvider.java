package org.wstorm.rcache.cache.nulls;


import org.wstorm.rcache.cache.Cache;
import org.wstorm.rcache.cache.CacheExpiredListener;
import org.wstorm.rcache.cache.CacheProvider;
import org.wstorm.rcache.enums.CacheProviderType;
import org.wstorm.rcache.exception.CacheException;

/**
 * 空的缓存实现,当不需要时用它
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月07日
 */
public class NullCacheProvider implements CacheProvider {

    private final static NullCache cache = new NullCache();

    @Override
    public String name() {
        return CacheProviderType.none.name();
    }

    @Override
    public Cache buildCache(String regionName, CacheExpiredListener listener)
            throws CacheException {
        return cache;
    }

    @Override
    public void start(String configFile) throws CacheException {
    }

    @Override
    public void stop() {
    }

}
