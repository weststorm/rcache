package org.wstorm.rcache.cache.redis;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wstorm.rcache.TestObj;
import org.wstorm.rcache.annotation.CacheConfig;
import org.wstorm.rcache.cache.DataPicker;
import org.wstorm.rcache.enums.CacheProviderType;
import org.wstorm.rcache.jedis.JedisTestBase;
import org.wstorm.rcache.jedis.JedisWrapper;
import org.wstorm.rcache.utils.CacheUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sunyp
 * @version 1.0
 * @created 2016年05月15日
 */
public class RedisCacheProviderTest extends JedisTestBase {

    protected RedisCacheProvider redisCacheProvider;

    protected List<String> ids;

    protected DataPicker<String, TestObj> dataPicker;

    protected CacheConfig cacheConfig = CacheUtils.getCacheAnnotation(TestObj.class);
    protected CacheConfig noExpiredCacheConfig = CacheUtils.getCacheAnnotation(null);

    @Before
    public void setUp() throws Exception {
        super.setUp();
        redisCacheProvider = new RedisCacheProvider(new JedisWrapper(pool));
        redisCacheProvider.start(null);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        redisCacheProvider.stop();
        super.tearDown();
    }

    @Test
    public void setJedisWrapper() throws Exception {
        redisCacheProvider.setJedisWrapper(new JedisWrapper(pool));
    }

    @Test
    public void name() throws Exception {
        assertThat(redisCacheProvider.name()).isEqualTo(CacheProviderType.redis.name());
    }

    @Test
    public void buildCache() throws Exception {
        RedisCache cache = redisCacheProvider.buildCache("test", null);
        assertThat(cache).isNotNull();
        assertThat(cache.getRegion()).isEqualTo("test");
    }
}