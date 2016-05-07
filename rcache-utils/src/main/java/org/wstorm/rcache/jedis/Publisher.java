package org.wstorm.rcache.jedis;

import redis.clients.jedis.JedisPool;

/**
 * 消息发布器
 *
 * @author syp
 * @version 1.0
 * @created 2016年5月07日
 */
public class Publisher extends JedisWrapper {

    public Publisher(JedisPool jedisPool) {
        super(jedisPool);
    }

    public void publish(final String channel, final byte[] message) {
        execute(jedis -> {
            jedis.publish(serialKey(channel), message);
            return null;
        });
    }
}