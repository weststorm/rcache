package org.wstorm.rcache.cache.nulls;

import org.wstorm.rcache.annotation.CacheConfig;
import org.wstorm.rcache.cache.Cache;
import org.wstorm.rcache.cache.DataPicker;
import org.wstorm.rcache.exception.CacheException;

import java.util.List;
import java.util.Map;

/**
 * 空的缓存服务,当不需要某一级缓存时的实现
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月07日
 */
final class NullCache implements Cache {

    @Override
    public <T> Map<String, T> getAll(CacheConfig cacheConfig, List<String> keys, DataPicker<String, T> dataPicker) throws CacheException {
        return null;
    }

    @Override
    public <T> void putAll(CacheConfig cacheConfig, Map<String, T> objectMap) throws CacheException {
    }

    @Override
    public <T> T get(CacheConfig cacheConfig, String key, DataPicker<String, T> dataPicker) throws CacheException {
        return null;
    }

    @Override
    public <T> void put(CacheConfig cacheConfig, String key, T value) throws CacheException {
    }

    @Override
    public List<String> keys() throws CacheException {
        return null;
    }

    @Override
    public void evict(CacheConfig cacheConfig, String key) throws CacheException {
    }

    @Override
    public void evict(CacheConfig cacheConfig, List<String> keys) throws CacheException {
    }

}
