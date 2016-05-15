package org.wstorm.rcache.cache.redis;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wstorm.rcache.TestObj;
import org.wstorm.rcache.annotation.CacheConfig;
import org.wstorm.rcache.cache.DataPicker;
import org.wstorm.rcache.jedis.JedisTestBase;
import org.wstorm.rcache.jedis.JedisWrapper;
import org.wstorm.rcache.utils.CacheUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sunyp
 * @version 1.0
 * @created 2016年05月14日
 */
public class RedisCacheTest extends JedisTestBase {

    private List<String> ids;

    private final DataPicker<String, TestObj> dataPicker = new DataPicker<String, TestObj>() {

        @Override
        public TestObj pickup(String key) {

            if (ids.contains(key)) {
                return new TestObj(key, 100);
            }
            return null;
        }

        @Override
        public TestObj makeEmptyData() {
            return new TestObj();
        }
    };

    private CacheConfig cacheConfig = CacheUtils.getCacheAnnotation(TestObj.class);
    private CacheConfig noExpiredCacheConfig = CacheUtils.getCacheAnnotation(null);

    private RedisCache cache;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ids = Arrays.asList("9527", "9528", "9529");
        cache = new RedisCache(cacheConfig.region(), new JedisWrapper(pool));
    }

    @After
    public void tearDown() throws Exception {
        cache.evict(cacheConfig, ids);
        cache.evict(noExpiredCacheConfig, ids);
    }

    @Test
    public void getAll() throws Exception {
        Map<String, TestObj> getAll = cache.getAll(cacheConfig, ids, null);
        getAll.forEach((key, obj) -> assertThat(obj).isNull());

        getAll = cache.getAll(cacheConfig, ids, dataPicker);
        getAll.forEach((key, obj) -> {
            assertThat(obj.isBlank()).isFalse();
            assertThat(obj.getSum()).isEqualTo(100);
        });
    }

    @Test
    public void putAll() throws Exception {
        Map<String, TestObj> getAll = cache.getAll(noExpiredCacheConfig, ids, null);
        getAll.forEach((key, obj) -> assertThat(obj).isNull());

        Map<String, TestObj> objectMap = ids.stream().collect(
                Collectors.toMap(String::toString, dataPicker::pickup));

        cache.putAll(noExpiredCacheConfig, objectMap);

        getAll = cache.getAll(noExpiredCacheConfig, ids, null);
        getAll.forEach((key, obj) -> {
            assertThat(obj.isBlank()).isFalse();
            assertThat(obj.id()).isEqualTo(key);
        });
    }

    @Test
    public void get() throws Exception {
        TestObj obj = cache.get(cacheConfig, ids.get(0), null);
        assertThat(obj).isNull();
        obj = cache.get(cacheConfig, ids.get(0), dataPicker);
        assertThat(obj).isNotNull();
        assertThat(obj.id()).isEqualTo(ids.get(0));

        obj = cache.get(cacheConfig, "null-noExists", dataPicker);
        assertThat(obj.isBlank()).isTrue();
    }

    @Test
    public void put() throws Exception {
        TestObj obj = cache.get(noExpiredCacheConfig, ids.get(0), null);
        assertThat(obj).isNull();
        obj = dataPicker.pickup(ids.get(0));
        cache.put(noExpiredCacheConfig, obj.id(), obj);
        TestObj get = cache.get(noExpiredCacheConfig, ids.get(0), null);
        assertThat(get).isNotNull();
        assertThat(get).isEqualTo(obj);
    }

    @Test
    public void keys() throws Exception {
        assertThat(cache.keys()).isEmpty();
    }

    @Test
    public void evict() throws Exception {
        Map<String, TestObj> objectMap = ids.stream().collect(
                Collectors.toMap(String::toString, dataPicker::pickup));
        cache.putAll(cacheConfig, objectMap);

        TestObj obj = cache.get(cacheConfig, ids.get(0), null);
        assertThat(obj).isNotNull();
        assertThat(obj.id()).isEqualTo(ids.get(0));

        cache.evict(cacheConfig, obj.id());
        obj = cache.get(cacheConfig, ids.get(0), null);
        assertThat(obj).isNull();

        obj = cache.get(cacheConfig, ids.get(1), null);
        assertThat(obj).isNotNull();
        assertThat(obj.id()).isEqualTo(ids.get(1));

        cache.evict(cacheConfig, ids);

        Map<String, TestObj> all = cache.getAll(cacheConfig, ids, null);
        all.forEach((id, nullObj) -> assertThat(nullObj).isNull());
    }

    @Test
    public void getRegion() throws Exception {
        assertThat(cache.getRegion()).isEqualTo(cacheConfig.region());
    }

}