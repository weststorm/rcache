/**
 * Copyright 2003-2006 Greg Luck, Jboss Inc
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wstorm.rcache.cache.ehcache;

import net.sf.ehcache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wstorm.rcache.cache.CacheExpiredListener;
import org.wstorm.rcache.cache.CacheProvider;
import org.wstorm.rcache.enums.CacheProviderType;
import org.wstorm.rcache.exception.CacheException;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache Provider plugin
 * <p>
 * Taken from EhCache 1.3 distribution
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月07日
 */
public class EhCacheProvider implements CacheProvider {

    private final static Logger log = LoggerFactory.getLogger(EhCacheProvider.class);

    private CacheManager manager;
    private ConcurrentHashMap<String, EhCacheCache> _CacheManager;

    @Override
    public String name() {
        return CacheProviderType.ehcache.name();
    }

    /**
     * Builds a Cache. Even though this method provides properties, they are not
     * used. Properties for EHCache are specified in the ehcache.xml file.
     * Configuration will be read from ehcache.xml for a cache declaration where
     * the regionName attribute matches the regionName parameter in this builder.
     *
     * @param regionName     the regionName of the cache. Must match a cache configured in
     *                 ehcache.xml
     * @param listener cache listener
     * @return a newly built cache will be built and initialised
     * @throws CacheException inter alia, if a cache of the same regionName already exists
     */
    public EhCacheCache buildCache(String regionName, CacheExpiredListener listener)
            throws CacheException {
        EhCacheCache ehcache;
        if ((ehcache = _CacheManager.get(regionName)) == null) {
            try {
                synchronized (this) {
                    if ((ehcache = _CacheManager.get(regionName)) == null) {
                        net.sf.ehcache.Cache cache = manager.getCache(regionName);
                        if (cache == null) {
                            log.warn("Could not find configuration [" + regionName + "]; using defaults.");
                            manager.addCache(regionName);
                            cache = manager.getCache(regionName);
                            log.debug("started EHCache region: " + regionName);
                        }
                        ehcache = new EhCacheCache(cache, listener);
                        _CacheManager.put(regionName, ehcache);
                    }
                }
            } catch (net.sf.ehcache.CacheException e) {
                throw new CacheException(e);
            }
        }
        return ehcache;
    }

    /**
     * Callback to perform any necessary initialization of the underlying cache
     * implementation during SessionFactory construction.
     *
     * @param cacheConfigFile current configuration settings.
     */
    public void start(String cacheConfigFile) throws CacheException {
        if (manager != null) {
            log.warn("Attempt to restart an already started EhCacheProvider. Use sessionFactory.close() "
                    + " between repeated calls to buildSessionFactory. Using previously created EhCacheProvider."
                    + " If this behaviour is required, consider using net.sf.ehcache.hibernate.SingletonEhCacheProvider.");
            return;
        }
        URL xml = getClass().getClassLoader().getParent().getResource(cacheConfigFile);

        if (xml == null) xml = getClass().getResource(cacheConfigFile);
        if (xml == null) throw new CacheException("cannot find ehcache.xml !!!");

        manager = new CacheManager(xml);
        _CacheManager = new ConcurrentHashMap<>();
    }

    /**
     * Callback to perform any necessary cleanup of the underlying cache
     * implementation during SessionFactory.close().
     */
    public void stop() {
        if (manager != null) {
            manager.shutdown();
            manager = null;
        }
    }

}
