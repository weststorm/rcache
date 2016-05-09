package org.wstorm.rcache.memcached;

import net.spy.memcached.MemcachedClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sunyp
 * @version 1.0
 * @created 2016年05月09日
 */
public class SpyMemcachedClientTest {

    private SpyMemcachedClient spyMemcachedClient;
    private String key = "keyChain";
    private int shutdownTimeout = 10000;
    private int expired = 2;

    @Before
    public void setUp() throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle("memcached");
        if (bundle == null) {
            throw new IllegalArgumentException("[memcached.properties] is not found!");
        }

        shutdownTimeout = (Integer.valueOf(bundle.getString("shutdownTimeout")));

        int updateTimeout = (Integer.valueOf(bundle.getString("updateTimeout")));

        spyMemcachedClient = new SpyMemcachedClient();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(
                bundle.getString("memcached.ip"),
                Integer.parseInt(bundle.getString("memcached.port")));

        MemcachedClient memcachedClient = new MemcachedClient(inetSocketAddress);

        spyMemcachedClient.setMemcachedClient(memcachedClient);

        spyMemcachedClient.setShutdownTimeout(shutdownTimeout);
        spyMemcachedClient.setUpdateTimeout(updateTimeout);
    }

    @After
    public void tearDown() throws Exception {
        spyMemcachedClient.delete(key);
    }


    @Test
    public void destroy() throws Exception {
        spyMemcachedClient.destroy();
        assertThat(spyMemcachedClient
                .getMemcachedClient()
                .shutdown(shutdownTimeout, TimeUnit.MILLISECONDS)).isFalse();
    }

    @Test
    public void getBulk() throws Exception {
        String key1 = "getBulk1", key2 = "getBulk2";
        spyMemcachedClient.set(key1, expired, key1);
        spyMemcachedClient.set(key2, expired, key2);
        Map<String, Object> bulk = spyMemcachedClient.getBulk(Arrays.asList(key1, key2));
        assertThat(bulk).isNotNull();
        assertThat(bulk.get(key1)).isEqualTo(key1);
        assertThat(bulk.get(key2)).isEqualTo(key2);
    }

    @Test
    public void set() throws Exception {
        String value = "set";
        spyMemcachedClient.set(key, expired, value);
        assertThat((String) spyMemcachedClient.get(key)).isEqualTo(value);
    }

    @Test
    public void safeSet() throws Exception {
        String value = "safeSet";
        boolean abc = spyMemcachedClient.safeSet(key, expired, value);
        assertThat(abc).isTrue();
        String actual = spyMemcachedClient.get(key);
        assertThat(actual).isEqualTo(value);
    }

    @Test
    public void delete() throws Exception {
        String value = "delete";
        spyMemcachedClient.set(key, expired, value);
        spyMemcachedClient.delete(key);
        Object o = spyMemcachedClient.get(key);
        assertThat(o).isNull();
    }

    @Test
    public void safeDelete() throws Exception {
        String value = "safeDelete";
        spyMemcachedClient.set(key, expired, value);
        spyMemcachedClient.safeDelete(key);
        Object o = spyMemcachedClient.get(key);
        assertThat(o).isNull();
    }

    @Test
    public void incr() throws Exception {
        long defaultValue = 11;
        long init = spyMemcachedClient.incr(key, 1, defaultValue);
        assertThat(init).isEqualTo(defaultValue);

        long add = spyMemcachedClient.incr(key, 1, defaultValue);
        assertThat(add).isEqualTo(init + 1);

        init = spyMemcachedClient.incr(key, 1, defaultValue, expired);
        assertThat(init).isEqualTo(defaultValue);

        Thread.sleep(expired + 1);
        add = spyMemcachedClient.incr(key, 1, defaultValue, expired);
        assertThat(add).isEqualTo(init);
        Thread.sleep(expired + 1);
        Object o = spyMemcachedClient.get(key);
        assertThat(o).isNull();
    }

    @Test
    public void decr() throws Exception {

        long defaultValue = 11;
        long init = spyMemcachedClient.decr(key, 1, defaultValue);
        assertThat(init).isEqualTo(defaultValue);

        long add = spyMemcachedClient.decr(key, 1, defaultValue);
        assertThat(add).isEqualTo(init - 1);


        init = spyMemcachedClient.decr(key, 1, defaultValue, expired);
        assertThat(init).isEqualTo(defaultValue);
        Thread.sleep(expired + 1);

        add = spyMemcachedClient.decr(key, 1, defaultValue, expired);
        assertThat(add).isEqualTo(init);
        Thread.sleep(expired + 1);

        Object o = spyMemcachedClient.get(key);
        assertThat(o).isNull();
    }

    @Test
    public void asyncIncr() throws Exception {

        Future<Long> longFuture = spyMemcachedClient.asyncIncr(key, 1);
        Long aLong = longFuture.get();
        assertThat(aLong).isEqualTo(1);
        longFuture = spyMemcachedClient.asyncIncr(key, 1);
        aLong = longFuture.get();
        assertThat(aLong).isEqualTo(2);
    }

    @Test
    public void asyncDecr() throws Exception {

        Future<Long> longFuture = spyMemcachedClient.asyncDecr(key, 1);
        Long aLong = longFuture.get();
        assertThat(aLong).isEqualTo(1);
        longFuture = spyMemcachedClient.asyncIncr(key, 1);
        aLong = longFuture.get();
        assertThat(aLong).isEqualTo(2);
    }

}