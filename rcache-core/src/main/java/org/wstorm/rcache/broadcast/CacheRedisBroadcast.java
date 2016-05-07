package org.wstorm.rcache.broadcast;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wstorm.rcache.CacheManager;
import org.wstorm.rcache.annotation.CacheConfig;
import org.wstorm.rcache.cache.CacheExpiredListener;
import org.wstorm.rcache.cache.DataPicker;
import org.wstorm.rcache.entity.CacheObject;
import org.wstorm.rcache.entity.Command;
import org.wstorm.rcache.enums.CacheRegion;
import org.wstorm.rcache.exception.CacheException;
import org.wstorm.rcache.jedis.Publisher;
import org.wstorm.rcache.jedis.Subscriber;
import org.wstorm.rcache.utils.CacheUtils;
import org.wstorm.rcache.utils.CollectionsUtils;
import org.wstorm.rcache.serializer.KryoPoolSerializer;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.JedisPool;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Redis发布订阅缓存
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年5月07日
 */
public class CacheRedisBroadcast extends BinaryJedisPubSub implements CacheExpiredListener {

    public final static byte LEVEL_1 = 1;
    public final static byte LEVEL_2 = 2;
    private final KryoPoolSerializer serializer = new KryoPoolSerializer();
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String hostId;
    private CacheManager cacheManager;
    private Publisher publisher;
    private Subscriber subscriber;
    private boolean retryWhenSubscribeFail = (true);
    /**
     * 发布/订阅的缓存频道
     */
    private List<CacheRegion> cacheRegions;

    public CacheRedisBroadcast(JedisPool cachePublishJedisPool, SubscribeCacheRegionConfig subCacheRegionConfig, CacheManager cacheManager) {
        Preconditions.checkNotNull(cacheManager, "cacheManager can not be null!");
        Preconditions.checkNotNull(cachePublishJedisPool, "cachePublishJedisPool can not be null!");
        Preconditions.checkArgument(subCacheRegionConfig.getSubscribeCacheRegions() != null && !subCacheRegionConfig.getSubscribeCacheRegions().isEmpty(), "subCacheRegionConfig can not be null!");
        cacheRegions = subCacheRegionConfig.getSubscribeCacheRegions();

        this.cacheManager = cacheManager;


        byte b2[] = new byte[12];
        ByteBuffer.wrap(b2).putInt((int) (System.currentTimeMillis())).putInt(ThreadLocalRandom.current().nextInt());
        final StringBuilder buf = new StringBuilder(24);
        for (final byte b : b2) buf.append(String.format("%02x", b & 0xff));

        this.hostId = buf.toString();

        publisher = new Publisher(cachePublishJedisPool);
        subscriber = new Subscriber(cachePublishJedisPool);
        // 开始订阅
        (new Thread(() -> {
            while (retryWhenSubscribeFail) {
                String[] channels = new String[cacheRegions.size()];
                for (int i = 0; i < cacheRegions.size(); i++) {
                    channels[i] = cacheRegions.get(i).region;
                }
                subscriber.subscribeAndBlock(CacheRedisBroadcast.this, channels);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        })).start();

    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void notifyElementExpired(String region, Object key) {

        if (log.isDebugEnabled()) log.debug("Cache data expired| region={}| key={}", region, key);

        // 删除二级缓存
        if (key instanceof List) cacheManager.batchEvict(LEVEL_2, null, region, (List<String>) key, this);
        else cacheManager.evict(LEVEL_2, null, region, String.valueOf(key), this);

        // 发送广播
        _publishEvictCmd(region, key);
    }

    /**
     * 发布清除缓存的命令
     *
     * @param region Cache region name
     * @param key    cache key
     */
    private void _publishEvictCmd(String region, Object key) {
        // 发送广播
        Command cmd = new Command(hostId, Command.OPT_DELETE_KEY, region, key);
        try {
            publisher.publish(region, serializer.serialize(cmd));
        } catch (Exception e) {
            log.error("Unable to delete cache| region={}| key={}", region, key, e);
        }
    }

    /**
     * 获取缓存中的数据
     *
     * @param cacheConfig Cache Region name
     * @param key         Cache key
     * @param dataPicker  缓存过期时间。
     * @return cacheObject include cacheObject T
     */
    public <T> CacheObject<T> get(CacheConfig cacheConfig, String key, DataPicker<String, T> dataPicker) {
        CacheObject<T> obj = new CacheObject<>();
        obj.setRegion(cacheConfig.region().region);
        obj.setKey(key);
        if (cacheConfig.region().region != null && key != null) {
            obj.setValue(cacheManager.get(LEVEL_1, cacheConfig, key, this, dataPicker));
            if (obj.getValue() == null) {
                obj.setValue(cacheManager.get(LEVEL_2, cacheConfig, key, this, dataPicker));
                if (obj.getValue() != null) {
                    obj.setLevel(LEVEL_2);
                    cacheManager.set(LEVEL_1, cacheConfig, key, obj.getValue(), this);
                }
            } else obj.setLevel(LEVEL_1);
        }
        return obj;
    }

    /**
     * 写入缓存
     *
     * @param cacheConfig cache config
     * @param key         cache key
     * @param value       cache object
     */
    public <T> void set(CacheConfig cacheConfig, String key, T value) {
        if (cacheConfig.region().region != null && key != null) {
            if (value == null) evict(cacheConfig, cacheConfig.region().region, key);
            else {
                // 分几种情况
                // 1. L1 和 L2 都没有
                // 2. L1 有 L2 没有（这种情况不存在，除非是写 L2 的时候失败
                // 3. L1 没有，L2 有
                // 4. L1 和 L2 都有
                _publishEvictCmd(cacheConfig.region().region, CacheUtils.genCacheKey(cacheConfig, key));// 清除原有的一级缓存的内容
                cacheManager.set(LEVEL_1, cacheConfig, key, value, this);
                cacheManager.set(LEVEL_2, cacheConfig, key, value, this);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("write data to cache region={}| key={}| value={}", cacheConfig.region().region, key, value);
        }
    }

    public final <T> void setAll(CacheConfig cacheConfig, Map<String, T> objects) {
        if (cacheConfig.region().region != null && objects != null) {
            _publishEvictCmd(cacheConfig.region().region, CacheUtils.genCacheKeys(cacheConfig, Lists.newArrayList(objects.keySet().iterator())));// 清除原有的一级缓存的内容
            cacheManager.setAll(LEVEL_2, cacheConfig, objects, this);
            cacheManager.setAll(LEVEL_1, cacheConfig, objects, this);
        }
    }

    /**
     * 删除缓存
     *
     * @param cacheConfig 配置项
     * @param key         主Key
     */
    public void evict(CacheConfig cacheConfig, String key) {
        evict(cacheConfig, cacheConfig.region().region, key);
    }

    /**
     * 删除缓存
     *
     * @param region Cache Region name
     * @param key    Cache key
     */
    public void evict(CacheConfig cacheConfig, String region, String key) {
        cacheManager.evict(LEVEL_1, cacheConfig, region, key, this); // 删除一级缓存
        cacheManager.evict(LEVEL_2, cacheConfig, region, key, this); // 删除二级缓存
        _publishEvictCmd(region, CacheUtils.genCacheKey(cacheConfig, key)); // 发送广播
    }

    /**
     * 批量删除缓存
     *
     * @param cacheConfig cache config
     * @param keys        cache keys
     */
    public void batchEvict(CacheConfig cacheConfig, List<String> keys) {
        batchEvict(cacheConfig, cacheConfig.region().region, keys);
    }

    /**
     * 批量删除缓存
     *
     * @param cacheConfig cache config
     * @param region      Cache region name
     * @param keys        Cache key
     */
    public void batchEvict(CacheConfig cacheConfig, String region, List<String> keys) {
        cacheManager.batchEvict(LEVEL_1, cacheConfig, region, keys, this);
        cacheManager.batchEvict(LEVEL_2, cacheConfig, region, keys, this);
        _publishEvictCmd(region, keys);
    }

    /**
     * Get cache region keys
     *
     * @param region Cache region name
     * @return key list
     */
    public List keys(String region) throws CacheException {
        return cacheManager.keys(LEVEL_1, region, this);
    }

    /**
     * 删除一级缓存的键对应内容
     *
     * @param region Cache region name
     * @param key    cache key
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void onDeleteCacheKey(String region, Object key) {
        if (key instanceof List) {
            cacheManager.batchEvict(LEVEL_1, null, region, (List) key, this);
        } else if (key instanceof Set) {
            List<String> objects = ((Stream<String>) ((Set) key).stream().map(Object::toString))
                    .collect(Collectors.toList());
            cacheManager.batchEvict(LEVEL_1, null, region, objects, this);
        } else {
            cacheManager.evict(LEVEL_1, null, region, String.valueOf(key), this);
        }
        if (log.isDebugEnabled()) {
            log.debug("Received cache evict message| region={}| key={}", region, key);
        }
    }

    /**
     * 订阅消息
     *
     * @param channel 来源
     * @param message 消息
     */
    @Override
    public void onMessage(byte[] channel, byte[] message) {

        try {
            Command cmd = (Command) serializer.deserialize(message);
            if (cmd == null)
                return;
            // 同一进程的消息忽略掉
            if (hostId.equalsIgnoreCase(cmd.getHostId())) {
                if (log.isDebugEnabled())
                    log.debug("ignore host hostId={} | current host hostId={}", cmd.getHostId(), hostId);
                return;
            }
            switch (cmd.getOperator()) {
                case Command.OPT_DELETE_KEY:
                    if (log.isDebugEnabled())
                        log.debug("收到[{}]消息，清除region[{}]key[{}] ", new String(channel), cmd.getRegion(), cmd.getKey());
                    onDeleteCacheKey(cmd.getRegion(), cmd.getKey());
                    break;
                default:
                    log.warn("Unknown message type = {}", cmd.getOperator());
            }
        } catch (Exception e) {
            log.error("Unable to handle received msg", e);
        }
    }

    /**
     * 关闭到通道的连接
     */
    public void close() {
        cacheManager.shutdown(LEVEL_1);
        cacheManager.shutdown(LEVEL_2);
        CacheRedisBroadcast.this.punsubscribe();
    }


    /**
     * 批量获取
     *
     * @param cacheConfig cache config annotation
     * @param keys        key list
     * @param dataPicker  cache data pick from db layer
     * @param <T>         cache object type
     * @return cacheObject include map<String,T>
     */
    public <T> CacheObject<Map<String, T>> getList(CacheConfig cacheConfig, List<String> keys, DataPicker<String, T> dataPicker) {
        CacheObject<Map<String, T>> obj = new CacheObject<>();
        obj.setKey(StringUtils.join(keys, ","));
        obj.setRegion(cacheConfig.region().region);
        if (cacheConfig.region().region != null && keys != null && !keys.isEmpty()) {
            Map<String, T> bulk = cacheManager.getAll(LEVEL_1, cacheConfig, keys, this, dataPicker);
            if (CollectionsUtils.isEmpty(bulk) || bulk.size() < keys.size()) {
                bulk = cacheManager.getAll(LEVEL_2, cacheConfig, keys, this, dataPicker);

                if (CollectionsUtils.isNotEmpty(bulk) && bulk.size() == keys.size()) {
                    obj.setLevel(LEVEL_2);
                    cacheManager.setAll(LEVEL_1, cacheConfig, bulk, this);
                }
            } else {
                obj.setLevel(LEVEL_1);
            }
            obj.setValue(bulk);
        }
        return obj;
    }


}
