package org.wstorm.rcache;

import org.wstorm.rcache.annotation.CacheConfig;
import org.wstorm.rcache.cache.DataPicker;
import org.wstorm.rcache.cache.redis.RedisCache;
import org.wstorm.rcache.cache.redis.RedisCacheProvider;
import org.wstorm.rcache.enums.CacheProviderType;
import org.wstorm.rcache.jedis.JedisTestBase;
import org.wstorm.rcache.jedis.JedisWrapper;
import org.wstorm.rcache.utils.CacheUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author sunyp
 * @version 1.0
 * @created 2016年05月18日
 */
public abstract  class TestBase extends JedisTestBase {

    protected List<String> ids;

    protected DataPicker<String, TestObj> dataPicker;

    protected CacheConfig cacheConfig = CacheUtils.getCacheAnnotation(TestObj.class);
    protected CacheConfig noExpiredCacheConfig = CacheUtils.getCacheAnnotation(null);

    protected TestExpiredListener listener = new TestExpiredListener();


    protected RedisCacheProvider redisCacheProvider;

    protected RedisCache cache;

    protected void init() throws Exception {
        super.setUp();
        ids = Arrays.asList("9527", "9528", "9529");
        dataPicker = new TestObjDatePicker(ids);
        redisCacheProvider = new RedisCacheProvider(new JedisWrapper(pool));
        redisCacheProvider.start(null);
        cache  = redisCacheProvider.buildCache(cacheConfig.region(), listener);
    }


}
