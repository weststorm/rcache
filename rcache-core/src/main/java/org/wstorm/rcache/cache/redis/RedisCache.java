package org.wstorm.rcache.cache.redis;

import com.google.common.collect.Maps;
import org.wstorm.rcache.RObject;
import org.wstorm.rcache.annotation.CacheConfig;
import org.wstorm.rcache.cache.Cache;
import org.wstorm.rcache.cache.DataPicker;
import org.wstorm.rcache.exception.CacheException;
import org.wstorm.rcache.jedis.JedisWrapper;
import org.wstorm.rcache.serializer.KryoPoolSerializer;
import org.wstorm.rcache.utils.CacheUtils;
import org.wstorm.rcache.utils.CollectionsUtils;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Redis cache implement
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月10日
 */
public class RedisCache implements Cache {

    private static final List<String> EMPTY_KEYS_LIST = new ArrayList<>();
    private final KryoPoolSerializer serializer = new KryoPoolSerializer();
    private final String region;
    private JedisWrapper jedisWrapper;


    public RedisCache(String regionName, JedisWrapper jedisWrapper) {
        region = regionName;
        this.jedisWrapper = jedisWrapper;
    }

    @Override
    public <T extends RObject<String>> Map<String, T> getAll(CacheConfig cacheConfig,
                                                             final List<String> ids,
                                                             final DataPicker<String, T> dataPicker)
            throws CacheException {

        if (CollectionsUtils.isEmpty(ids)) return Maps.newHashMap();

        Map<String, Response<String>> responseMap = Maps.newHashMap();

        jedisWrapper.execute(jedis -> {
            Pipeline pipelined = jedis.pipelined();
            try {
                ids.forEach(id -> responseMap.put(id, pipelined.get(CacheUtils.genCacheKey(cacheConfig, id))));
            } finally {
                pipelined.sync();
            }
            return null;
        });

        Map<String, T> backOff = Maps.newHashMap();

        try {
            return responseMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                    responseEntry -> getBackOff(cacheConfig,
                            responseEntry.getKey(),
                            dataPicker,
                            responseEntry.getValue().get())));
        } finally {
            if (!backOff.isEmpty()) {
                try {
                    putAll(cacheConfig, backOff);
                } catch (Exception ignored) {
                }
            }
        }
    }


    @Override
    public <T extends RObject<String>> T get(CacheConfig cacheConfig, String id, DataPicker<String, T> dataPicker)
            throws CacheException {
        try {
            return jedisWrapper.execute(jedis -> {
                String cacheKey = CacheUtils.genCacheKey(cacheConfig, id);
                String serialData = jedis.get(cacheKey);
                return getBackOff(cacheConfig, id, dataPicker, serialData);
            });
        } catch (Exception e) {
            throw new CacheException("get", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends RObject<String>> T getBackOff(CacheConfig cacheConfig, String id, DataPicker<String, T> dataPicker, String serialData) {
        if (serialData != null) {
            try {
                return (T) serializer.deserialize(serialData.getBytes());
            } catch (Exception e) {
                throw new CacheException("getBackOff", e);
            }
        }

        if (dataPicker == null) return null;

        T t;
        if ((t = dataPicker.pickup(id)) != null || (t = dataPicker.makeEmptyData()) != null) put(cacheConfig, id, t);

        return t != null ? (t.isBlank() ? null : t) : null;

    }

    @Override
    public <T extends RObject<String>> void putAll(CacheConfig cacheConfig, Map<String, T> objectMap) throws CacheException {

        jedisWrapper.execute(jedis -> {

            Pipeline pipelined = jedis.pipelined();
            try {

                objectMap.entrySet().parallelStream().forEach(
                        tEntry -> {
                            try {
                                if (cacheConfig.expiredTime() > 0)
                                    pipelined.setex(
                                            jedisWrapper.serializeKey(CacheUtils.genCacheKey(cacheConfig, tEntry.getKey())),
                                            cacheConfig.expiredTime(),
                                            serializer.serialize(tEntry.getValue())
                                    );
                                else
                                    pipelined.set(
                                            jedisWrapper.serializeKey(CacheUtils.genCacheKey(cacheConfig, tEntry.getKey())),
                                            serializer.serialize(tEntry.getValue())
                                    );
                            } catch (Exception e) {
                                throw new CacheException("putAll", e);
                            }
                        });
            } finally {
                pipelined.sync();
            }

            return null;
        });
    }

    @Override
    public <T extends RObject<String>> void put(CacheConfig cacheConfig, String id, T value) throws CacheException {
        jedisWrapper.execute(jedis -> {
            try {
                if (cacheConfig.expiredTime() > 0)
                    return jedis.setex(
                            jedisWrapper.serializeKey(CacheUtils.genCacheKey(cacheConfig, id)),
                            cacheConfig.expiredTime(),
                            serializer.serialize(value)
                    );
                else
                    return jedis.set(
                            jedisWrapper.serializeKey(CacheUtils.genCacheKey(cacheConfig, id)),
                            serializer.serialize(value)
                    );

            } catch (Exception e) {
                throw new CacheException("put", e);
            }
        });
    }

    @Override
    public List keys() throws CacheException {
        return EMPTY_KEYS_LIST;
    }

    @Override
    public void evict(CacheConfig cacheConfig, String id) throws CacheException {
        try {
            jedisWrapper.execute(jedis -> jedis.del(CacheUtils.genCacheKey(cacheConfig, id)));
        } catch (Exception e) {
            throw new CacheException("evict", e);
        }
    }

    @Override
    public void evict(CacheConfig cacheConfig, List<String> ids) throws CacheException {
        try {
            jedisWrapper.execute(jedis -> jedis.del(CacheUtils.genCacheKeys(cacheConfig, ids).toArray(new String[0])));
        } catch (Exception e) {
            throw new CacheException("evict-All", e);
        }
    }

    public String getRegion() {
        return region;
    }
}
