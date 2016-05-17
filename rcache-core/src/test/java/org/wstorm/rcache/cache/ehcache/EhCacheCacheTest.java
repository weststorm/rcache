package org.wstorm.rcache.cache.ehcache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wstorm.rcache.RObject;
import org.wstorm.rcache.TestExpiredListener;
import org.wstorm.rcache.TestObj;
import org.wstorm.rcache.TestObjDatePicker;
import org.wstorm.rcache.annotation.CacheConfig;
import org.wstorm.rcache.exception.CacheException;
import org.wstorm.rcache.utils.CacheUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sunyp
 * @version 1.0
 * @created 2016年05月15日
 */
public class EhCacheCacheTest {

    private EhCacheProvider provider = new EhCacheProvider();
    private EhCacheCache cache;

    private TestExpiredListener listener = new TestExpiredListener();
    private CacheConfig cacheConfig;
    private List<String> ids;
    private TestObjDatePicker datePicker;

    @Before
    public void setUp() throws Exception {
        provider.start("ehcache.xml");
        cache = provider.buildCache("test", listener);
        ids = Arrays.asList("9527", "9528", "9529");
        cacheConfig = CacheUtils.getCacheAnnotation(TestObj.class);
        datePicker = new TestObjDatePicker(ids);
    }

    @After
    public void tearDown() throws Exception {
        cache.dispose();
        provider.stop();
    }

    @Test
    public void keys() throws Exception {
        TestObj obj = new TestObj(ids.get(0), 200);
        cache.put(cacheConfig, ids.get(0), obj);
        List keys = cache.keys();
        assertThat(keys.size()).isEqualTo(1);
        assertThat(keys.get(0)).isEqualTo(CacheUtils.genCacheKey(cacheConfig, ids.get(0)));
    }


    @Test(expected = CacheException.class)
    public void getPutAll() throws Exception {
        Map<String, TestObj> all = cache.getAll(cacheConfig, ids, null);
        assertThat(all).isEmpty();
        all = cache.getAll(cacheConfig, ids, datePicker); //ehcache有无数据返回与dataPicker无关
        assertThat(all).isEmpty();

        Map<String, TestObj> rs = ids.stream().collect(Collectors.toMap(
                String::toString,
                id -> datePicker.pickup(id)
        ));
        cache.putAll(cacheConfig, rs);


        all = cache.getAll(cacheConfig, ids, null);
        assertThat(all).isEqualTo(rs);

        List<String> cacheKeys = CacheUtils.genCacheKeys(cacheConfig, ids);
        rs = ids.stream().collect(Collectors.toMap(
                id -> CacheUtils.genCacheKey(cacheConfig, id),
                id -> datePicker.pickup(id)
        ));
        cache.putAll(null, rs);

        all = cache.getAll(null, cacheKeys, null);
        assertThat(all).isEqualTo(rs);


        provider.stop(); //模拟异常
        cache.putAll(cacheConfig, rs);
    }

    @Test(expected = CacheException.class)
    public void mockGetAllException() throws Exception {
        provider.stop(); //模拟异常
        cache.getAll(cacheConfig, ids, null);
    }

    @Test(expected = CacheException.class)
    public void getPut() throws Exception {
        TestObj expect = cache.get(cacheConfig, ids.get(0), null);
        assertThat(expect).isNull();
        expect = cache.get(cacheConfig, ids.get(0), datePicker);
        assertThat(expect).isNull();//ehcache有无数据返回与dataPicker无关
        expect = new TestObj(ids.get(0), 200);
        cache.put(cacheConfig, ids.get(0), expect);
        TestObj actual = cache.get(cacheConfig, ids.get(0), null);
        assertThat(actual).isEqualTo(expect);

        provider.stop(); //模拟异常
        cache.put(cacheConfig, ids.get(0), expect);
    }

    @Test(expected = CacheException.class)
    public void mockGetException() throws Exception {
        provider.stop(); //模拟异常
        cache.get(cacheConfig, ids.get(0), datePicker);
    }

    @Test(expected = CacheException.class)
    @SuppressWarnings({"unchecked"})
    public void evictAll() throws Exception {
        TestObj expect = new TestObj(ids.get(0), 200);
        cache.put(cacheConfig, ids.get(0), expect);

        {
            TestObj actual = cache.get(cacheConfig, ids.get(0), null);
            assertThat(actual).isEqualTo(expect);
        }
        
        cache.evict(cacheConfig, ids.get(0));
        cache.evict(null, CacheUtils.genCacheKey(cacheConfig, ids.get(0)));

        {
            TestObj actual = cache.get(cacheConfig, ids.get(0), null);
            assertThat(actual).isNull();
        }

        Map<String, TestObj> rs = ids.stream().collect(Collectors.toMap(
                String::toString,
                id -> datePicker.pickup(id)
        ));
        cache.putAll(cacheConfig, rs);
        cache.evict(cacheConfig, ids);
        Map<String, RObject<String>> all = cache.getAll(cacheConfig, ids, null);
        assertThat(all).isNullOrEmpty();


        provider.stop(); //模拟异常
        cache.evict(cacheConfig, ids);
    }


    @Test(expected = CloneNotSupportedException.class)
    public void cloneObject() throws Exception {
        cache.clone();
    }


}