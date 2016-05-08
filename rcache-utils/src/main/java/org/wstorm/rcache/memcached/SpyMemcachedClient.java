package org.wstorm.rcache.memcached;

import com.google.common.base.Preconditions;
import net.spy.memcached.MemcachedClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.wstorm.rcache.ErrorHandler;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Memcache缓存工具服务类
 *
 * @author sunyp
 * @version 1.0
 * @created 2014年8月14日 下午8:39:00
 */
public class SpyMemcachedClient implements ErrorHandler, DisposableBean {

    private MemcachedClient memcachedClient;
    private long updateTimeout = 2500;
    private long shutdownTimeout = 2500;

    /**
     * Get方法, 转换结果类型并屏蔽异常, 仅返回Null.
     *
     * @param key key
     * @param <T> value type
     * @return cache object value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Preconditions.checkArgument(StringUtils.isNotBlank(key), "key不能为空!");
        try {
            Object obj = memcachedClient.get(key);
            if (obj == null) return null;

            return (T) obj;
        } catch (RuntimeException e) {
            handleException(e, key);
            return null;
        }
    }

    @Override
    public void destroy() throws Exception {
        if (memcachedClient != null) {
            memcachedClient.shutdown(shutdownTimeout, TimeUnit.MILLISECONDS);
        }
    }


    /**
     * GetBulk方法, 转换结果类型并屏蔽异常.
     *
     * @param keys keys
     * @param <T>  value type
     * @return map<key value>
     */
    @SuppressWarnings("unchecked")
    public final <T> Map<String, T> getBulk(Collection<String> keys) {
        try {
            return (Map<String, T>) memcachedClient.getBulk(keys);
        } catch (RuntimeException e) {
            handleException(e, StringUtils.join(keys, ","));
            return null;
        }
    }

    /**
     * 异步Set方法, 不考虑执行结果.
     *
     * @param key         key
     * @param expiredTime expiration time
     * @param value       value
     */
    public final void set(String key, int expiredTime, Object value) {
        Preconditions.checkArgument(StringUtils.isNotBlank(key), "key不能为空");
        Preconditions.checkNotNull(value, "对象不能为空");
        if (expiredTime < 0) {
            expiredTime = 0;
        }
        memcachedClient.set(key, expiredTime, value);
    }

    /**
     * 安全的Set方法, 保证在updateTimeout秒内返回执行结果, 否则返回false并取消操作.
     *
     * @param key        key
     * @param expiration expiration time
     * @param value      value
     * @return true for success
     */
    public boolean safeSet(String key, int expiration, Object value) {
        Future<Boolean> future = memcachedClient.set(key, expiration, value);
        try {
            return future.get(updateTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            future.cancel(false);
        }
        return false;
    }

    /**
     * 异步 Delete方法, 不考虑执行结果.
     *
     * @param key key
     */
    public void delete(String key) {
        if (key == null) {
            return;
        }
        memcachedClient.delete(key);
    }

    /**
     * 安全的Delete方法, 保证在updateTimeout秒内返回执行结果, 否则返回false并取消操作.
     *
     * @param key key
     * @return true for success
     */
    public boolean safeDelete(String key) {
        Future<Boolean> future = memcachedClient.delete(key);
        try {
            return future.get(updateTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            future.cancel(false);
        }
        return false;
    }

    /**
     * Incr方法.
     *
     * @param key          key
     * @param delta        delta
     * @param defaultValue if not exists, the default value
     * @return the key's value
     */
    public long incr(String key, int delta, long defaultValue) {
        return memcachedClient.incr(key, delta, defaultValue);
    }

    /**
     * Incr方法. 有超时方法
     *
     * @param key          key
     * @param delta        delta
     * @param defaultValue if not exists, the default value
     * @param exp          expired time
     * @return the key's value
     */
    public long incr(String key, int delta, long defaultValue, int exp) {
        return memcachedClient.incr(key, delta, defaultValue, exp);
    }

    /**
     * Decr方法.
     *
     * @param key          key
     * @param delta        delta
     * @param defaultValue if not exists, the default value
     * @return the key's value
     */
    public long decr(String key, int delta, long defaultValue) {
        return memcachedClient.decr(key, delta, defaultValue);
    }

    /**
     * Decr方法.. 有超时方法
     *
     * @param key          key
     * @param delta        delta
     * @param defaultValue if not exists, the default value
     * @param exp          expired time
     * @return the key's value
     */
    public long decr(String key, int delta, long defaultValue, int exp) {
        return memcachedClient.decr(key, delta, defaultValue, exp);
    }

    /**
     * 异步Incr方法, 不支持默认值, 若key不存在返回-1.
     *
     * @param key   key
     * @param delta delta
     * @return the key's value
     */
    public Future<Long> asyncIncr(String key, int delta) {
        return memcachedClient.asyncIncr(key, delta);
    }

    /**
     * 异步Decr方法, 不支持默认值, 若key不存在返回-1.
     *
     * @param key   key
     * @param delta delta
     * @return the key's value
     */
    public Future<Long> asyncDecr(String key, int delta) {
        return memcachedClient.asyncDecr(key, delta);
    }

    public MemcachedClient getMemcachedClient() {
        return memcachedClient;
    }

    public void setMemcachedClient(MemcachedClient memcachedClient) {
        this.memcachedClient = memcachedClient;
    }

    public void setShutdownTimeout(long shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    public void setUpdateTimeout(long updateTimeout) {
        this.updateTimeout = updateTimeout;
    }

}
