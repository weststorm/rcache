package org.wstorm.rcache.cache.redis;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wstorm.rcache.TestBase;
import org.wstorm.rcache.TestObj;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sunyp
 * @version 1.0
 * @created 2016年05月14日
 */
public class RedisCacheTest extends TestBase {


    @Before
    public void setUp() throws Exception {
        init();
    }

    @After
    public void tearDown() throws Exception {
        cache.evict(cacheConfig, ids);
        cache.evict(noExpiredCacheConfig, ids);
        super.tearDown();
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