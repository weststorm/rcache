package org.wstorm.rcache.jedis;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wstorm.rcache.serializer.KryoPoolSerializer;
import org.wstorm.rcache.serializer.SObject;
import org.wstorm.rcache.serializer.Serializer;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sunyp
 * @version 1.0
 * @created 2016年05月08日
 */
public class JedisWrapperTest extends JedisTestBase {

    private Serializer serializer = new KryoPoolSerializer();
    private JedisWrapper wrapper;
    private String key = "key_jedisWrapperTest";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        wrapper = new JedisWrapper(pool);
    }

    @After
    public void tearDown() throws Exception {
        wrapper.execute(jedis -> jedis.del(key));
        super.tearDown();
    }

    @Test
    public void execute() throws Exception {
        assertThat((Long) wrapper.execute(jedis -> jedis.rpush(key, "test"))).isEqualTo(1);
        assertThat((String) wrapper.execute(jedis -> jedis.lpop(key))).isEqualTo("test");
    }

    @Test
    public void testSerialize() throws Exception {
        SObject obj = new SObject("888", 1888);

        SObject actual = wrapper.execute(jedis -> {
            try {
                byte[] serialize = serializer.serialize(obj);
                byte[] base64 = Base64.getEncoder().encode(serialize);
                System.out.println("serialize.length=" + serialize.length + ", base64.length=" + base64.length);
                jedis.set(wrapper.serializeKey(obj.id()), base64);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String serialData = jedis.get(obj.id());
            try {
                return (SObject) serializer.deserialize(Base64.getDecoder().decode(serialData));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
        assertThat(actual).isNotNull();
        assertThat(actual.id()).isEqualTo(obj.id());
    }

    @Test
    public void 测试管道是否独占导致其他Client的操作被阻塞() throws Exception {
        CountDownLatch 等等我 = new CountDownLatch(2);
        CyclicBarrier _123走起 = new CyclicBarrier(2);
        (new Thread(() -> {
            try {
                _123走起.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
            wrapper.execute(jedis -> {
                Pipeline pipelined = jedis.pipelined();
                List<Response<Long>> incrList = Lists.newArrayList();
                incrList.add(pipelined.incr(key));
                incrList.add(pipelined.incr(key));
                try {
                    System.out.println("thread1-我睡觉2秒");
                    Thread.sleep(2000L);
                    System.out.println("thread1-我睡觉2秒醒了");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                incrList.add(pipelined.incr(key));
                pipelined.sync();
                incrList.forEach(resp -> System.out.println("thread1-incr" + resp.get()));
                return null;
            });
            等等我.countDown();
        })).start();

        (new Thread(() -> {
            try {
                _123走起.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
            wrapper.execute(jedis -> {
                try {
                    Thread.sleep(1000L);
                    System.out.println("thread2-我睡觉1秒醒了");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("thread2-incr:" + jedis.incr(key));

                return null;
            });
            等等我.countDown();
        })).start();

        等等我.await();
        Thread.sleep(100L);
    }

    @Test
    public void serialKey() throws Exception {
        byte[] bytes = wrapper.serializeKey(key);
        assertThat(wrapper.deserializeKey(bytes)).isEqualTo(key);
    }

}