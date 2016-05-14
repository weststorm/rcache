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
    public <T extends RObject<String>> Map<String, T> getAll(CacheConfig cacheConfig, final List<String> ids, final DataPicker<String, T> dataPicker) throws CacheException {

        if (CollectionsUtils.isEmpty(ids)) return Maps.newHashMap();

        try {

            List<String> cacheKeys = CacheUtils.genCacheKeys(cacheConfig, ids);

            Map<String, T> bulk = memcached.getBulk(cacheKeys);
            if (dataPicker == null || CollectionsUtils.isEmpty(bulk) || bulk.size() == ids.size()) {
                return bulk;
            }

            Map<String, T> map;
            if (cacheKeys.get(0).equals(ids.get(0))) map = bulk;
            else map = Maps.newHashMapWithExpectedSize(ids.size());

            for (int i = 0; i < ids.size(); i++) {

                T t = bulk.get(cacheKeys.get(i));

                if (t == null) {
                    t = getBackOff(cacheConfig, ids.get(i), dataPicker, cacheKeys.get(i), null);
                }

                if (t != null && !t.isBlank()) map.put(ids.get(i), t);
            }

            return map;
        } catch (CacheException e) {
            throw new CacheException("getAll", e);
        }
    }

    @Override
    public <T extends RObject<String>> T get(CacheConfig cacheConfig, String id, DataPicker<String, T> dataPicker) throws CacheException {
        if (id == null) return null;
        try {
            String cacheKey = CacheUtils.genCacheKey(cacheConfig, id);
            return getBackOff(cacheConfig, id, dataPicker, cacheKey, memcached.get(cacheKey));
        } catch (CacheException e) {
            throw new CacheException("get", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends RObject<String>> T getBackOff(CacheConfig cacheConfig, String id, DataPicker<String, T> dataPicker, String cacheKey, Object o) {

        if (o != null) return (T) o;

        T t;
        if ((t = dataPicker.pickup(id)) != null || (t = dataPicker.makeEmptyData()) != null)
            memcached.set(cacheKey, cacheConfig.expiredTime(), t);

        return t != null ? (t.isBlank() ? null : t) : null;
    }

    /**
     * Puts an object into the cache.
     *
     * @param id    a key
     * @param value a value
     * @throws CacheException if the {@link CacheProvider} is stop or another
     *                        {@link Exception} occurs.
     */
    @Override
    public <T extends RObject<String>> void put(CacheConfig cacheConfig, String id, T value) throws CacheException {
        try {
            memcached.set(CacheUtils.genCacheKey(cacheConfig, id), cacheConfig.expiredTime(), value);
        } catch (Exception e) {
            throw new CacheException("put", e);
        }
    }

    /**
     * Removes the element which matches the key If no element matches, nothing
     * is removed and no Exception is thrown.
     *
     * @param id the key of the element to remove
     * @throws CacheException cache exception
     */
    @Override
    public void evict(CacheConfig cacheConfig, String id) throws CacheException {
        try {
            memcached.delete(CacheUtils.genCacheKey(cacheConfig, id));
        } catch (Exception e) {
            throw new CacheException("evict", e);
        }
    }

    @Override
    public void evict(CacheConfig cacheConfig, List<String> ids) throws CacheException {
        if (ids == null) return;
        try {
            CacheUtils.genCacheKeys(cacheConfig, ids).forEach(key -> memcached.delete(key));
        } catch (Exception e) {
            throw new CacheException("evict-All", e);
        }
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