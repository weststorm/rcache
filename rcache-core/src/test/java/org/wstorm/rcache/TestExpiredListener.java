package org.wstorm.rcache;

import org.wstorm.rcache.cache.CacheExpiredListener;

public class TestExpiredListener implements CacheExpiredListener {

    @Override
    public <ID> void notifyElementExpired(String region, ID key) {
        System.out.println("region:" + region + ", ID=" + key);
    }
}
