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

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RObject<String>> Map<String, T> getAll(CacheConfig cacheConfig, final List<String> ids, final DataPicker<String, T> dataPicker) throws CacheException {

        if (CollectionsUtils.isEmpty(ids)) return Maps.newHashMap();

        Map<String, Response<String>> responseMap = Maps.newHashMap();

        jedisWrapper.execute(jedis -> {
            Pipeline pipelined = jedis.pipelined();
            ids.forEach(key -> responseMap.put(key, pipelined.get(CacheUtils.genCacheKey(cacheConfig, key))));
            pipelined.sync();
            return null;
        });

        return responseMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                responseEntry -> {
                    String serialData = responseEntry.getValue().get();
                    try {
                        return (T) serializer.deserialize(serialData.getBytes());
                    } catch (Exception e) {
                        throw new CacheException("getAll", e);
                    }
                }));
    }

    @Override
    public <T extends RObject<String>> void putAll(CacheConfig cacheConfig, Map<String, T> objectMap) throws CacheException {

        jedisWrapper.execute(jedis -> {

            Pipeline pipelined = jedis.pipelined();

            objectMap.entrySet().parallelStream().forEach(
                    tEntry -> {
                        try {
                            if (cacheConfig.expiredTime() > 0)
                                pipelined.setex(
                                        CacheUtils.genCacheKey(cacheConfig, tEntry.getKey()),
                                        cacheConfig.expiredTime(),
                                        new String(serializer.serialize(tEntry.getValue())));
                            else
                                pipelined.set(
                                        CacheUtils.genCacheKey(cacheConfig, tEntry.getKey()),
                                        new String(serializer.serialize(tEntry.getValue())));
                        } catch (Exception e) {
                            throw new CacheException("putAll", e);
                        }
                    });

            pipelined.sync();
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RObject<String>> T get(CacheConfig cacheConfig, String id, DataPicker<String, T> dataPicker) throws CacheException {

        String data = jedisWrapper.execute(jedis -> jedis.get(CacheUtils.genCacheKey(cacheConfig, id)));

        try {
            return (T) serializer.deserialize(data.getBytes());
        } catch (Exception e) {
            throw new CacheException("get", e);
        }
    }

    @Override
    public <T extends RObject<String>> void put(CacheConfig cacheConfig, String id, T value) throws CacheException {
        jedisWrapper.execute(jedis -> {
            String data = null;
            try {
                data = new String(serializer.serialize(value));
            } catch (Exception e) {
                throw new CacheException("put", e);
            }
            return jedis.set(CacheUtils.genCacheKey(cacheConfig, id), data);
        });
    }

    @Override
    public List keys() throws CacheException {
        return EMPTY_KEYS_LIST;
    }

    @Override
    public void evict(CacheConfig cacheConfig, String id) throws CacheException {
        jedisWrapper.execute(jedis -> jedis.del(CacheUtils.genCacheKey(cacheConfig, id)));
    }

    @Override
    public void evict(CacheConfig cacheConfig, List<String> ids) throws CacheException {
        jedisWrapper.execute(jedis -> jedis.del(CacheUtils.genCacheKeys(cacheConfig, ids).toArray(new String[0])));
    }

    public String getRegion() {
        return region;
    }
}
