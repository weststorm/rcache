package org.wstorm.rcache.jedis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.SafeEncoder;

/**
 * jedis wrapper for jedis Action
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月07日
 */
public class JedisWrapper {

    protected final static Logger log = LoggerFactory.getLogger(JedisWrapper.class);

    private JedisPool jedisPool;

    public JedisWrapper(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * 执行Jedis任务并返回
     *
     * @param callback 任务回调器
     * @param <T>      返回类型
     * @return 返回对象
     */
    public <T> T execute(JedisAction<T> callback) {
        try (Jedis jedis = jedisPool.getResource()) {
            return callback.execute(jedis);
        }
    }

    /**
     * 对key进行编码成byte[]
     *
     * @param key redis 存储的key
     * @return 返回编码后key字节数组
     */
    public byte[] serializeKey(String key) {
        return SafeEncoder.encode(key);
    }

    /**
     * 触码Key成String
     *
     * @param bytes redis key byte[]
     * @return String
     */
    public String deserializeKey(byte[] bytes) {
        return SafeEncoder.encode(bytes);
    }

    /**
     * jedis 执行动作回调接口
     *
     * @param <T> 返回类型
     */
    public interface JedisAction<T> {
        T execute(Jedis jedis);
    }


}
