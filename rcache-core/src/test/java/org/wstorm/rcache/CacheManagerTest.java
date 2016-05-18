package org.wstorm.rcache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wstorm.rcache.cache.redis.RedisCacheTest;
import org.wstorm.rcache.enums.CacheProviderType;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author sunyp
 * @version 1.0
 * @created 2016年05月17日
 */
public class CacheManagerTest extends RedisCacheTest {

    protected CacheManager cacheManager;
    protected TestExpiredListener listener = new TestExpiredListener();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ids = Arrays.asList("9527", "9528", "9529");
        dataPicker = new TestObjDatePicker(ids);
        cacheManager = new CacheManager(redisCacheProvider, CacheProviderType.ehcache.name(), "ehcache.xml");
    }

    @After
    public void tearDown() throws Exception {
        cacheManager.shutdown(2);
        cacheManager.shutdown(1);
        super.tearDown();
    }

    @Test(expected = NullPointerException.class)
    public void shutdown() throws Exception {
        cacheManager.shutdown(1);
        cacheManager.get(1, cacheConfig, ids.get(0), listener, dataPicker); //throws null
    }

    @Test
    public void get() throws Exception {
        assertThat((Object) cacheManager.get(1, cacheConfig, ids.get(0), listener, null)).isNull();
        assertThat(cacheManager.get(1, cacheConfig, ids.get(0), listener, dataPicker)).isNull();
        TestObj actual = cacheManager.get(2, cacheConfig, ids.get(0), listener, dataPicker);
        assertThat(actual).isEqualTo(dataPicker.pickup(ids.get(0)));//2级缓存空会调用dataPicker加载数据库数据
    }

    @Test
    public void set() throws Exception {
        assertThat(cacheManager.get(1, cacheConfig, ids.get(0), listener, dataPicker)).isNull();
        TestObj pickup = dataPicker.pickup(ids.get(0));
        //只设置了一级缓存,只有1级缓存有数据
        cacheManager.set(1, cacheConfig, ids.get(0), pickup, listener);
        assertThat(cacheManager.get(1, cacheConfig, ids.get(0), listener, dataPicker)).isEqualTo(pickup);
        //二级缓存为空
        assertThat((Object) cacheManager.get(2, cacheConfig, ids.get(0), listener, null)).isNull();
    }

    @Test
    public void evict() throws Exception {

        TestObj pickup = dataPicker.pickup(ids.get(0));
        //只设置了一级缓存,只有1级缓存有数据
        cacheManager.set(1, cacheConfig, ids.get(0), pickup, listener);
        cacheManager.evict(1, cacheConfig, null, ids.get(0), listener);
        assertThat(cacheManager.get(1, cacheConfig, ids.get(0), listener, dataPicker)).isNull();
        Map<String, TestObj> expected =
                ids.stream().collect(Collectors.toMap(
                        String::toString,
                        id -> dataPicker.pickup(id)
                ));
        cacheManager.setAll(1, cacheConfig, expected, listener);
        Map<String, TestObj> actuals = cacheManager.getAll(1, cacheConfig, ids, listener, dataPicker);
        assertThat(actuals).isEqualTo(expected);

        cacheManager.batchEvict(1, cacheConfig, null, ids, listener);
        actuals = cacheManager.getAll(1, cacheConfig, ids, listener, dataPicker);
        assertThat(actuals).isNullOrEmpty();
    }
}