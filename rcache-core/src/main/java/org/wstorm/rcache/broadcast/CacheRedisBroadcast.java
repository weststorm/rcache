package org.wstorm.rcache.broadcast;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wstorm.rcache.CacheManager;
import org.wstorm.rcache.RObject;
import org.wstorm.rcache.annotation.CacheConfig;
import org.wstorm.rcache.cache.CacheExpiredListener;
import org.wstorm.rcache.cache.DataPicker;
import org.wstorm.rcache.entity.CacheObject;
import org.wstorm.rcache.entity.Command;
import org.wstorm.rcache.exception.CacheException;
import org.wstorm.rcache.jedis.PubSuber;
import org.wstorm.rcache.serializer.KryoPoolSerializer;
import org.wstorm.rcache.utils.CacheUtils;
import org.wstorm.rcache.utils.CollectionsUtils;
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
    private PubSuber pubSuber;
    private boolean retryWhenSubscribeFail = (true);
    /**
     * 发布/订阅的缓存频道
     */
    private List<String> cacheRegions;

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

        pubSuber = new PubSuber(cachePublishJedisPool);
        // 开始订阅
        (new Thread(() -> {
            while (retryWhenSubscribeFail) {
                String[] channels = new String[cacheRegions.size()];
                for (int i = 0; i < cacheRegions.size(); i++) {
                    channels[i] = cacheRegions.get(i);
                }
                pubSuber.subscribeAndBlock(CacheRedisBroadcast.this, channels);
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

        if (log.isDebugEnabled()) log.debug("Cache data expired| hostId={}| region={}| key={}", hostId, region, key);

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
        if (log.isDebugEnabled()) log.debug("_publishEvictCmd| hostId={}| region=={}| key={}", hostId, region, key);
        Command cmd = new Command(hostId, Command.OPT_DELETE_KEY, region, key);
        try {
            pubSuber.publish(region, serializer.serialize(cmd));
        } catch (Exception e) {
            log.error("Unable to delete cache| region={}| key={}", region, key, e);
        }
    }

    /**
     * 获取缓存中的数据
     *
     * @param cacheConfig Cache Region name
     * @param id          object id, is not the finally key in cached Object identifier
     * @param dataPicker  缓存过期时间。
     * @return cacheObject include cacheObject T
     */
    public <T extends RObject<String>> CacheObject<T> get(CacheConfig cacheConfig, String id, DataPicker<String, T> dataPicker) {
        CacheObject<T> obj = new CacheObject<>();
        obj.setRegion(cacheConfig.region());
        obj.setKey(id);
        if (id != null) {
            obj.setValue(cacheManager.get(LEVEL_1, cacheConfig, id, this, dataPicker));
            if (obj.getValue() == null) {
                obj.setValue(cacheManager.get(LEVEL_2, cacheConfig, id, this, dataPicker));
                if (obj.getValue() != null) {
                    obj.setLevel(LEVEL_2);
                    cacheManager.set(LEVEL_1, cacheConfig, id, obj.getValue(), this);
                }
            } else obj.setLevel(LEVEL_1);
        }
        return obj;
    }

    /**
     * 写入缓存
     *
     * @param cacheConfig cache config
     * @param id          object id, is not the finally key in cached Object identifier
     * @param value       cache object
     */
    public <T extends RObject<String>> void set(CacheConfig cacheConfig, String id, T value) {
        if (id != null) {
            if (value == null) evict(cacheConfig, cacheConfig.region(), id);
            else {
                // 分几种情况
                // 1. L1 和 L2 都没有
                // 2. L1 有 L2 没有（这种情况不存在，除非是写 L2 的时候失败
                // 3. L1 没有，L2 有
                // 4. L1 和 L2 都有
                _publishEvictCmd(cacheConfig.region(), CacheUtils.genCacheKey(cacheConfig, id));// 清除原有的一级缓存的内容
                cacheManager.set(LEVEL_1, cacheConfig, id, value, this);
                cacheManager.set(LEVEL_2, cacheConfig, id, value, this);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("write data to cache region={}| id={}| value={}", cacheConfig.region(), id, value);
        }
    }

    public <T extends RObject<String>> void setAll(CacheConfig cacheConfig, Map<String, T> objects) {
        if (objects != null) {
            _publishEvictCmd(cacheConfig.region(), CacheUtils.genCacheKeys(cacheConfig, Lists.newArrayList(objects.keySet().iterator())));// 清除原有的一级缓存的内容
            cacheManager.setAll(LEVEL_2, cacheConfig, objects, this);
            cacheManager.setAll(LEVEL_1, cacheConfig, objects, this);
        }
    }

    /**
     * 删除缓存
     *
     * @param cacheConfig 配置项
     * @param id          object id, is not the finally key in cached Object identifier
     */
    public void evict(CacheConfig cacheConfig, String id) {
        evict(cacheConfig, cacheConfig.region(), id);
    }

    /**
     * 删除缓存
     *
     * @param region Cache Region name
     * @param id     object id, is not the finally key in cached Object identifier
     */
    public void evict(CacheConfig cacheConfig, String region, String id) {
        cacheManager.evict(LEVEL_1, cacheConfig, region, id, this); // 删除一级缓存
        cacheManager.evict(LEVEL_2, cacheConfig, region, id, this); // 删除二级缓存
        _publishEvictCmd(region, CacheUtils.genCacheKey(cacheConfig, id)); // 发送广播
    }

    /**
     * 批量删除缓存
     *
     * @param cacheConfig cache config
     * @param ids         object id, is not the finally key in cached Object identifier
     */
    public void batchEvict(CacheConfig cacheConfig, List<String> ids) {
        batchEvict(cacheConfig, cacheConfig.region(), ids);
    }

    /**
     * 批量删除缓存
     *
     * @param cacheConfig cache config
     * @param region      Cache region name
     * @param ids         object id, is not the finally key in cached Object identifier
     */
    public void batchEvict(CacheConfig cacheConfig, String region, List<String> ids) {
        cacheManager.batchEvict(LEVEL_1, cacheConfig, region, ids, this);
        cacheManager.batchEvict(LEVEL_2, cacheConfig, region, ids, this);
        if (cacheConfig != null) _publishEvictCmd(region, CacheUtils.genCacheKeys(cacheConfig, ids));
        else _publishEvictCmd(region, ids);
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
            log.debug("Received cache evict message| hostId={}| region={}| key={}", hostId, region, key);
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
            Command cmd = serializer.deserialize(message);
            if (cmd == null)
                return;
            // 同一进程的消息忽略掉
            if (hostId.equalsIgnoreCase(cmd.getHostId())) {
                if (log.isDebugEnabled())
                    log.debug("onMessage| 忽略同缓存管理器[{}]的消息", hostId);
                return;
            }

            if (log.isDebugEnabled())
                log.debug("onMessage| 当前缓存管理器[{}]收到缓存管理器[{}]发来[{}]消息，清除region[{}]key[{}] ",
                        hostId, cmd.getHostId(), new String(channel), cmd.getRegion(), cmd.getKey());

            switch (cmd.getOperator()) {
                case Command.OPT_DELETE_KEY:
                    onDeleteCacheKey(cmd.getRegion(), cmd.getKey());
                    break;
                default:
                    log.warn("onMessage| hostId={}| Unknown message type={}", hostId, cmd.getOperator());
            }
        } catch (Exception e) {
            log.error("onMessage| hostId={}| channel={}", hostId, new String(channel), e);
        }
    }

    /**
     * 关闭到通道的连接
     */
    public void close() {
        CacheRedisBroadcast.this.punsubscribe();
        cacheManager.shutdown(LEVEL_1);
        cacheManager.shutdown(LEVEL_2);
    }


    /**
     * 批量获取
     *
     * @param cacheConfig cache config annotation
     * @param ids         object ids, is not the finally key in cached Object identifier
     * @param dataPicker  cache data pick from db layer
     * @param <T>         cache object type
     * @return cacheObject include map<String,T>
     */
    public <T extends RObject<String>> CacheObject<Map<String, T>> getList(CacheConfig cacheConfig, List<String> ids, DataPicker<String, T> dataPicker) {
        CacheObject<Map<String, T>> obj = new CacheObject<>();
        obj.setKey(StringUtils.join(ids, ","));
        obj.setRegion(cacheConfig.region());
        if (ids != null && !ids.isEmpty()) {
            Map<String, T> bulk = cacheManager.getAll(LEVEL_1, cacheConfig, ids, this, dataPicker);
            if (CollectionsUtils.isEmpty(bulk) || bulk.size() < ids.size()) {
                bulk = cacheManager.getAll(LEVEL_2, cacheConfig, ids, this, dataPicker);

                if (CollectionsUtils.isNotEmpty(bulk) && bulk.size() == ids.size()) {
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
