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
import java.util.Map;
import java.util.stream.Collectors;

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
    }


    @Test
    public void notifyElementExpired() throws Exception {

        CacheManager cacheManager2 = new CacheManager(redisCacheProvider, CacheProviderType.ehcache.name()
                , "ehcache-2.xml");
        CacheRedisBroadcast otherCacheRedisBroadcast = new CacheRedisBroadcast(pool, config, cacheManager2);
        Thread.sleep(300); //等着订阅线程准备完毕启动

        //没传 dataPicker,从二级缓存获取
        // 此次直接删除让其马上通知otherCache
        {
            Map<String, TestObj> expected = ids.stream().collect(
                    Collectors.toMap(String::toString, id -> dataPicker.pickup(id)));
            cacheRedisBroadcast.setAll(cacheConfig, expected);

            CacheObject<Map<String, TestObj>>
                    otherCacheObj = otherCacheRedisBroadcast.getList(cacheConfig, ids, null);
            assertThat(otherCacheObj.getLevel()).isEqualTo(CacheRedisBroadcast.LEVEL_2);
            assertThat(otherCacheObj.getValue()).isEqualTo(expected);
//            Thread.sleep(100);
            //从本地一级缓存获取
            otherCacheObj = otherCacheRedisBroadcast.getList(cacheConfig, ids, null);
            assertThat(otherCacheObj.getLevel()).isEqualTo(CacheRedisBroadcast.LEVEL_1);
            assertThat(otherCacheObj.getValue()).isEqualTo(expected);

            //清除当前缓存会触发向otherCache发送清除其本身数据
            cacheRedisBroadcast.batchEvict(cacheConfig, ids);
            Thread.sleep(100);
            //被广播到消除1级缓存后,拿不到任何数据了
            otherCacheObj = otherCacheRedisBroadcast.getList(cacheConfig, ids, null);
            assertThat(otherCacheObj.getValue()).isEmpty();
            assertThat(otherCacheObj.getLevel()).isZero();
        }

        //没传 dataPicker,从二级缓存获取
        //  ehcache.xml 配置了指定的region 2秒过期了,
        // 10秒ehcache清理一次 解发消除命令,并通知到otherCacheRedisBroadcast去清除1级缓存
        {
            TestObj expected = dataPicker.pickup(ids.get(0));
            cacheRedisBroadcast.set(cacheConfig, ids.get(0), expected);

            CacheObject<TestObj> otherCacheObj = otherCacheRedisBroadcast.get(cacheConfig, ids.get(0), null);
            assertThat(otherCacheObj.getLevel()).isEqualTo(CacheRedisBroadcast.LEVEL_2);
            assertThat(otherCacheObj.getValue()).isEqualTo(expected);

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


    }
}