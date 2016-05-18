package org.wstorm.rcache.broadcast;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wstorm.rcache.CacheManager;
import org.wstorm.rcache.TestBase;
import org.wstorm.rcache.TestObj;
import org.wstorm.rcache.entity.CacheObject;
import org.wstorm.rcache.enums.CacheProviderType;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sunyp
 * @version 1.0
 * @created 2016年05月17日
 */
public class CacheRedisBroadcastTest extends TestBase {

    private CacheRedisBroadcast cacheRedisBroadcast;
    private SubscribeCacheRegionConfig config = () -> Arrays.asList("udserv:app", "udserv:usr");
    private CacheManager cacheManager;

    @Before
    public void setUp() throws Exception {
        init();
        cacheManager = new CacheManager(redisCacheProvider, CacheProviderType.ehcache.name(), "ehcache.xml");
        cacheRedisBroadcast = new CacheRedisBroadcast(pool, config, cacheManager);
        Thread.sleep(100);
    }

    @After
    public void tearDown() throws Exception {
        cacheRedisBroadcast.setCacheManager(cacheManager);
        cacheRedisBroadcast.close();
//        super.tearDown();
    }

    @Test(expected = NullPointerException.class)
    public void setCacheManager() throws Exception {
        cacheRedisBroadcast.setCacheManager(null);
        cacheRedisBroadcast.close();
    }

    @Test
    public void notifyElementExpired() throws Exception {

        CacheManager cacheManager2 = new CacheManager(redisCacheProvider, CacheProviderType.ehcache.name()
                , "ehcache-2.xml");
        CacheRedisBroadcast otherCacheRedisBroadcast = new CacheRedisBroadcast(pool, config, cacheManager2);
        Thread.sleep(100);
        TestObj expected = dataPicker.pickup(ids.get(0));
        cacheRedisBroadcast.set(cacheConfig, ids.get(0), expected);

        //没传 dataPicker,从二级缓存获取
        //  ehcache.xml 配置了指定的region 2秒过期了,
        // 10秒ehcache清理一次 解发消除命令,并通知到otherCacheRedisBroadcast去清除1级缓存
        {
            CacheObject<TestObj> otherCacheObj = otherCacheRedisBroadcast.get(cacheConfig, ids.get(0), null);
            assertThat(otherCacheObj.getLevel()).isEqualTo(CacheRedisBroadcast.LEVEL_2);
            assertThat(otherCacheObj.getValue()).isEqualTo(expected);

//            Thread.sleep(100);
            //从本地一级缓存获取
            otherCacheObj = otherCacheRedisBroadcast.get(cacheConfig, ids.get(0), null);
            assertThat(otherCacheObj.getLevel()).isEqualTo(CacheRedisBroadcast.LEVEL_1);
            assertThat(otherCacheObj.getValue()).isEqualTo(expected);

            Thread.sleep(10100L); //10秒ehcache清理一次

            //现在自己也拿不到任何数据了
            otherCacheObj = otherCacheRedisBroadcast.get(cacheConfig, ids.get(0), null);
            assertThat(otherCacheObj.getValue()).isNull();
            assertThat(otherCacheObj.getLevel()).isZero();
        }

        // 再次设置
        cacheRedisBroadcast.set(cacheConfig, ids.get(0), expected);
        //没传 dataPicker,从二级缓存获取
        // 此次直接删除让其马上通知othercache
        {
            CacheObject<TestObj> otherCacheObj = otherCacheRedisBroadcast.get(cacheConfig, ids.get(0), null);
            assertThat(otherCacheObj.getLevel()).isEqualTo(CacheRedisBroadcast.LEVEL_2);
            assertThat(otherCacheObj.getValue()).isEqualTo(expected);
//            Thread.sleep(100);
            //从本地一级缓存获取
            otherCacheObj = otherCacheRedisBroadcast.get(cacheConfig, ids.get(0), null);
            assertThat(otherCacheObj.getLevel()).isEqualTo(CacheRedisBroadcast.LEVEL_1);
            assertThat(otherCacheObj.getValue()).isEqualTo(expected);

            //清除当前缓存会触发向othercache发送清除其本身数据
            cacheRedisBroadcast.evict(cacheConfig, ids.get(0));
            Thread.sleep(100);
            //现在自己也拿不到任何数据了
            otherCacheObj = otherCacheRedisBroadcast.get(cacheConfig, ids.get(0), null);
            assertThat(otherCacheObj.getValue()).isNull();
            assertThat(otherCacheObj.getLevel()).isZero();
        }
    }

    @Test
    public void get() throws Exception {

    }

    @Test
    public void set() throws Exception {

    }

    @Test
    public void setAll() throws Exception {

    }

    @Test
    public void evict() throws Exception {

    }

    @Test
    public void evict1() throws Exception {

    }

    @Test
    public void batchEvict() throws Exception {

    }

    @Test
    public void batchEvict1() throws Exception {

    }

    @Test
    public void keys() throws Exception {

    }

    @Test
    public void onMessage() throws Exception {

    }

    @Test
    public void close() throws Exception {

    }

    @Test
    public void getList() throws Exception {

    }

}