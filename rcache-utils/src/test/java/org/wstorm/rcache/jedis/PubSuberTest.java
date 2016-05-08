package org.wstorm.rcache.jedis;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.util.SafeEncoder;

import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sunyp
 * @version 1.0
 * @created 2016年05月08日
 */
public class PubSuberTest extends JedisTestBase {

    private String msg;
    private CountDownLatch countDownLatch;
    private PubSuber pubSuber;
    private String channel = "publisher_SafeByte";

    @After
    public void tearDown() throws Exception {
        System.out.println("del channel:" + pubSuber.execute(jedis -> jedis.del(channel)));
        super.tearDown();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        msg = null;
        countDownLatch = new CountDownLatch(1);
        pubSuber = new PubSuber(pool);
    }

    @Test
    public void publish() throws Exception {

        JedisSub sub = new JedisSub();

        (new Thread(() -> {
            pubSuber.subscribeAndBlock(sub, channel);
        })).start();

        Thread.sleep(100);
        String expect = "hello 中国!";

        pubSuber.publish(channel, SafeEncoder.encode(expect));

        countDownLatch.await();

        assertThat(msg).isEqualTo(expect);
    }

    private class JedisSub extends BinaryJedisPubSub {

        @Override
        public void onMessage(byte[] channel, byte[] message) {
            System.out.print("receive message: " + message);
            try {
                msg = SafeEncoder.encode(message);
                System.out.println("| msg: " + msg);
            } finally {
                countDownLatch.countDown();
            }
        }
    }

}