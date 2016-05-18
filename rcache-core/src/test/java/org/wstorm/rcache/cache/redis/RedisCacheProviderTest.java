package org.wstorm.rcache.cache.redis;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wstorm.rcache.TestBase;
import org.wstorm.rcache.enums.CacheProviderType;
import org.wstorm.rcache.jedis.JedisWrapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sunyp
 * @version 1.0
 * @created 2016年05月15日
 */
public class RedisCacheProviderTest extends TestBase {


    @Before
    public void setUp() throws Exception {
        init();
    }

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