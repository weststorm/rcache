package org.wstorm.rcache.jedis;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class Subscriber extends JedisWrapper {

    public Subscriber(JedisPool jedisPool) {
        super(jedisPool);
    }

    /**
     * 订阅频道后并阻塞住当前线程
     *
     * @param binaryJedisPubSub 订阅器
     * @param channels          频道数组
     */
    public void subscribeAndBlock(final BinaryJedisPubSub binaryJedisPubSub, final String... channels) {
        if (channels.length == 0) {
            return;
        }
        if (binaryJedisPubSub == null) {
            return;
        }

        new JedisAction<Void>() {

            @Override
            public Void execute(final Jedis jedis) {
                try {
                    byte[][] channelBytes = new byte[channels.length][];

                    for (int i = 0; i < channels.length; i++) {
                        channelBytes[i] = serialKey(channels[i]);
                        log.info("订阅缓存频道{}", channels[i]);
                    }

                    jedis.subscribe(binaryJedisPubSub, channelBytes);

                } catch (Exception e) {
                    log.warn("订阅缓存频道异常{}", e);
                    throw e;
                } finally {
                    jedis.close();
                }
                return null;
            }
        };
    }
}