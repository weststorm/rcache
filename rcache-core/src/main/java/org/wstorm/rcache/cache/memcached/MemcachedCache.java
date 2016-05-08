package org.wstorm.rcache.cache.memcached;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.wstorm.rcache.RObject;
import org.wstorm.rcache.annotation.CacheConfig;
import org.wstorm.rcache.cache.Cache;
import org.wstorm.rcache.cache.CacheProvider;
import org.wstorm.rcache.cache.DataPicker;
import org.wstorm.rcache.exception.CacheException;
import org.wstorm.rcache.memcached.SpyMemcachedClient;
import org.wstorm.rcache.utils.CacheUtils;
import org.wstorm.rcache.utils.CollectionsUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MemcachedCache
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月08日
 */
class MemcachedCache implements Cache {

    private static final List<String> EMPTY_KEYS_LIST = new ArrayList<>();

    private SpyMemcachedClient memcached;
    private String region;

    /**
     * @param region    The underlying EhCache instance to use.
     * @param memcached cache listener
     */
    MemcachedCache(String region, SpyMemcachedClient memcached) {
        this.region = region;
        this.memcached = memcached;
    }

    public List<String> keys() throws CacheException {
        return EMPTY_KEYS_LIST;
    }

    @Override
    public <T extends RObject<String>> Map<String, T> getAll(CacheConfig cacheConfig, final List<String> keys, final DataPicker<String, T> dataPicker) throws CacheException {

        if (CollectionsUtils.isEmpty(keys)) return Maps.newHashMap();

        try {

            List<String> cacheKeys = CacheUtils.genCacheKeys(cacheConfig, keys);

            Map<String, T> bulk = memcached.getBulk(cacheKeys);
            if (dataPicker == null || CollectionsUtils.isEmpty(bulk) || bulk.size() == keys.size()) {
                return bulk;
            }

            Map<String, T> map;
            if (cacheKeys.get(0).equals(keys.get(0))) map = bulk;
            else map = Maps.newHashMapWithExpectedSize(keys.size());

            for (int i = 0; i < keys.size(); i++) {

                T t = bulk.get(cacheKeys.get(i));
                if (t == null) {

                    t = dataPicker.pickup(keys.get(i));

                    if (t == null) t = dataPicker.makeEmptyData();

                    memcached.set(cacheKeys.get(i), cacheConfig.expiredTime(), t);
                }

                if (t != null && !t.isBlank()) map.put(keys.get(i), t);
            }

            return map;
        } catch (CacheException e) {
            throw new CacheException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends RObject<String>> T get(CacheConfig cacheConfig, String key, DataPicker<String, T> dataPicker) throws CacheException {
        if (key == null) return null;
        try {
            String cacheKey = CacheUtils.genCacheKey(cacheConfig, key);
            Object o = memcached.get(cacheKey);
            T t;
            if (o == null) {
                if ((t = dataPicker.pickup(key)) == null) t = dataPicker.makeEmptyData();
                memcached.set(cacheKey, cacheConfig.expiredTime(), t);
                return t.isBlank() ? null : t;
            } else t = (T) o;

            return t;
        } catch (CacheException e) {
            throw new CacheException(e);
        }
    }

    /**
     * Puts an object into the cache.
     *
     * @param key   a key
     * @param value a value
     * @throws CacheException if the {@link CacheProvider} is stop or another
     *                        {@link Exception} occurs.
     */
    @Override
    public <T extends RObject<String>> void put(CacheConfig cacheConfig, String key, T value) throws CacheException {
        try {
            if (cacheConfig.expiredTime() < 0) memcached.set(CacheUtils.genCacheKey(cacheConfig, key), 0, value);
            else memcached.set(CacheUtils.genCacheKey(cacheConfig, key), cacheConfig.expiredTime(), value);
        } catch (Exception e) {
            throw new CacheException(e);
        }

    }

    /**
     * Removes the element which matches the key If no element matches, nothing
     * is removed and no Exception is thrown.
     *
     * @param key the key of the element to remove
     * @throws CacheException cache exception
     */
    @Override
    public void evict(CacheConfig cacheConfig, String key) throws CacheException {
        try {
            memcached.delete(CacheUtils.genCacheKey(cacheConfig, key));
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    @Override
    public void evict(CacheConfig cacheConfig, List<String> keys) throws CacheException {
        if (keys == null) return;
        List<String> cks = CacheUtils.genCacheKeys(cacheConfig, keys);
        if (cks != null) cks.forEach(key -> memcached.delete(key));
    }

    @Override
    public <T extends RObject<String>> void putAll(CacheConfig cacheConfig, Map<String, T> objectMap) throws CacheException {
        Preconditions.checkNotNull(objectMap);
        objectMap.entrySet().forEach(entry -> put(cacheConfig, entry.getKey(), entry.getValue()));
    }

    public String getRegion() {
        return region;
    }
}