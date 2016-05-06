package org.wstorm.rcache.cache;

import org.wstorm.rcache.exception.CacheException;

/**
 * 缓存服务提供者的接口定义,所有提供缓存服务的提供者都要实现接口
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月06日
 */
public interface CacheProvider {

    /**
     * 缓存提供者的标识名称
     *
     * @return 缓存服务提供者名称
     */
    String name();

    /**
     * 配置构建出一个缓存操作器
     *
     * @param regionName the name of the cache region
     * @param listener   listener for expired elements
     * @return return cache instance
     * @throws CacheException 当构建失败
     */
    Cache buildCache(String regionName, CacheExpiredListener listener) throws CacheException;

    /**
     * 启动缓存服务,当一个缓存提供者要第一次启动时需要调用
     *
     * @param cacheConfigFile 缓存提供者所需要的配置文件全路径
     */
    void start(String cacheConfigFile) throws CacheException;

    /**
     * 停止缓存服务,当虚拟机要退出之前请调用该方法
     */
    void stop();

}
