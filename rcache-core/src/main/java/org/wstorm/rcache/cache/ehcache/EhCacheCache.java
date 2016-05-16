package org.wstorm.rcache.cache.ehcache;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import org.wstorm.rcache.RObject;
import org.wstorm.rcache.annotation.CacheConfig;
import org.wstorm.rcache.cache.Cache;
import org.wstorm.rcache.cache.CacheExpiredListener;
import org.wstorm.rcache.cache.DataPicker;
import org.wstorm.rcache.exception.CacheException;
import org.wstorm.rcache.utils.CacheUtils;
import org.wstorm.rcache.utils.CollectionsUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * EHCache 缓存封装类,实现了缓存和监听器
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月07日
 */
class EhCacheCache implements Cache, CacheEventListener {

    private net.sf.ehcache.Cache cache;
    private CacheExpiredListener listener;

    /**
     * Creates a new Hibernate pluggable cache based on a cache name.
     *
     * @param cache    The underlying EhCache instance to use.
     * @param listener cache listener
     */
    EhCacheCache(net.sf.ehcache.Cache cache, CacheExpiredListener listener) {
        this.cache = cache;
        this.cache.getCacheEventNotificationService().registerListener(this);
        this.listener = listener;
    }

    public List keys() throws CacheException {
        return this.cache.getKeys();
    }

    @SuppressWarnings({"unchecked"})
    public <T extends RObject<String>> Map<String, T> getAll(CacheConfig cacheConfig, List<String> ids, DataPicker<String, T> dataPicker)
            throws CacheException {

        if (CollectionsUtils.isEmpty(ids)) return new HashMap<>();

        try {
            List<String> cacheKeys;
            
            if (cacheConfig != null) cacheKeys = CacheUtils.genCacheKeys(cacheConfig, ids);
            else cacheKeys = ids;

            Map<Object, Element> elements = cache.getAll(cacheKeys);
            Map<String, T> map = Maps.newHashMap();
            for (int i = 0; i < cacheKeys.size(); i++) {
                if (elements.get(cacheKeys.get(i)) != null) {
                    map.put(ids.get(i), (T) elements.get(cacheKeys.get(i)).getObjectValue());
                }
            }

            return map;
        } catch (Exception e) {
            throw new CacheException("getAll", e);
        }
    }

    @SuppressWarnings({"unchecked"})
    public <T extends RObject<String>> T get(CacheConfig cacheConfig, String id, DataPicker<String, T> dataPicker) throws CacheException {
        if (id == null) return null;
        try {
            if (cacheConfig != null) id = CacheUtils.genCacheKey(cacheConfig, id);
            Element element = cache.get(id);

            return (element != null) ? (T) element.getObjectValue() : null;
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException("get", e);
        }
    }


    @Override
    public <T extends RObject<String>> void put(CacheConfig cacheConfig, String id, T value) throws CacheException {
        try {
            if (cacheConfig != null) id = CacheUtils.genCacheKey(cacheConfig, id);
            Element element = new Element(id, value);
            if (cacheConfig != null && cacheConfig.expiredTime() > 0) {
                element.setTimeToLive(cacheConfig.expiredTime());
            }
            cache.put(element);
        } catch (Exception e) {
            throw new CacheException("put", e);
        }

    }

    public <T extends RObject<String>> void putAll(final CacheConfig cacheConfig, Map<String, T> objectMap) throws CacheException {
        List<Element> elements = Lists.newArrayListWithCapacity(objectMap.size());

        if (cacheConfig != null)
            elements.addAll(objectMap.entrySet().stream().map(entry -> {
                Element element = new Element(CacheUtils.genCacheKey(cacheConfig, entry.getKey()), entry.getValue());
                if (cacheConfig.expiredTime() > 0) {
                    element.setTimeToLive(cacheConfig.expiredTime());
                }
                return element;
            }).collect(Collectors.toList()));

        else
            elements.addAll(objectMap.entrySet().stream().map(entry ->
                    new Element(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList()));

        try {
            cache.putAll(elements);
        } catch (Exception e) {
            throw new CacheException("putAll", e);
        }
    }

    @Override
    public void evict(CacheConfig cacheConfig, String id) throws CacheException {
        try {
            if (cacheConfig != null) cache.remove(CacheUtils.genCacheKey(cacheConfig, id));
            else cache.remove(id);
        } catch (IllegalStateException | net.sf.ehcache.CacheException e) {
            throw new CacheException("evict", e);
        }
    }

    @Override
    public void evict(CacheConfig cacheConfig, List<String> ids) throws CacheException {
        try {
            cache.removeAll(CacheUtils.genCacheKeys(cacheConfig, ids));
        } catch (Exception e) {
            throw new CacheException("evict-All", e);
        }

    }

//    public void clear() throws CacheException {
//        try {
//            cache.removeAll();
//        } catch (IllegalStateException | net.sf.ehcache.CacheException e) {
//            throw new CacheException("clear", e);
//        }
//    }
//
//    public void destroy() throws CacheException {
//        try {
//            cache.getCacheManager().removeCache(cache.getName());
//        } catch (IllegalStateException | net.sf.ehcache.CacheException e) {
//            throw new CacheException("destroy", e);
//        }
//    }

    @SuppressWarnings({"all"})
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    @Override
    public void dispose() {
    }

    @Override
    public void notifyElementEvicted(Ehcache cache, Element elem) {
    }

    @Override
    public void notifyElementExpired(Ehcache cache, Element elem) {
        if (listener != null) listener.notifyElementExpired(cache.getName(), elem.getObjectKey());
    }

    @Override
    public void notifyElementPut(Ehcache cache, Element elem) throws CacheException {
    }

    @Override
    public void notifyElementRemoved(Ehcache cache, Element elem) throws CacheException {
    }

    @Override
    public void notifyElementUpdated(Ehcache cache, Element elem) throws CacheException {
    }

    @Override
    public void notifyRemoveAll(Ehcache cache) {
    }
}