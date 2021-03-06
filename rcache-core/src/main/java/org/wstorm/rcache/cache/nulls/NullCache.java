package org.wstorm.rcache.cache.nulls;

import org.wstorm.rcache.RObject;
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
    public <T extends RObject<String>> Map<String, T> getAll(CacheConfig cacheConfig, List<String> ids, DataPicker<String, T> dataPicker) throws CacheException {
        return null;
    }

    @Override
    public <T extends RObject<String>> void putAll(CacheConfig cacheConfig, Map<String, T> objectMap) throws CacheException {
    }

    @Override
    public <T extends RObject<String>> T get(CacheConfig cacheConfig, String id, DataPicker<String, T> dataPicker) throws CacheException {
        return null;
    }

    @Override
    public <T extends RObject<String>> void put(CacheConfig cacheConfig, String id, T value) throws CacheException {
    }

    @Override
    public List<String> keys() throws CacheException {
        return null;
    }

    @Override
    public void evict(CacheConfig cacheConfig, String id) throws CacheException {
    }

    @Override
    public void evict(CacheConfig cacheConfig, List<String> ids) throws CacheException {
    }

}
