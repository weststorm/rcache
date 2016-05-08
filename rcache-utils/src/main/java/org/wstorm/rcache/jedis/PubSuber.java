package org.wstorm.rcache.jedis;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.JedisPool;

/**
 * 消息发布器
 *
 * @author syp
 * @version 1.0
 * @created 2016年5月07日
 */
public class PubSuber extends JedisWrapper {

    public PubSuber(JedisPool jedisPool) {
        super(jedisPool);
    }

    public void publish(final String channel, final byte[] message) {
        execute(jedis -> {
            jedis.publish(serialKey(channel), message);
            return null;
        });
    }

    /**
     * 订阅频道后并阻塞住当前线程
     *
     * @param binaryJedisPubSub 订阅器
     * @param channels          频道数组
     */
    public void subscribeAndBlock(final BinaryJedisPubSub binaryJedisPubSub, final String... channels) {

        if (channels.length == 0 || binaryJedisPubSub == null) return;

        execute(jedis -> {
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
            }
            return null;
        });
    }
}