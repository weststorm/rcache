package org.wstorm.rcache.broadcast;


import java.util.List;

/**
 * 订阅缓存区域配置接口，用于返回订阅的缓存区域配置信息
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年5月06日
 */
public interface SubscribeCacheRegionConfig {

    /**
     * @return 获取缓存区域配置
     */
    List<String> getSubscribeCacheRegions();

}
