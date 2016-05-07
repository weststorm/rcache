/**
 * Copyright
 */
package org.wstorm.rcache.utils;

import java.util.Collection;
import java.util.Map;


/**
 * 集合工具类
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年5月07日
 */
public final class CollectionsUtils {

    private CollectionsUtils() {
    }
    /**
     * 判断是否为空.
     *
     * @param collection list or collection
     * @return boolean
     */
    public static boolean isEmpty(Collection collection) {
        return !isNotEmpty(collection);
    }

    /**
     * 判断是否为空.
     *
     * @param map map
     * @return true/false
     */
    public static boolean isEmpty(Map map) {
        return (map == null) || map.isEmpty();
    }

    /**
     * 判断是否不为空.
     *
     * @param map map
     * @return true/false
     */
    public static boolean isNotEmpty(Map map) {
        return !isEmpty(map);
    }

    /**
     * 判断是否不为空.
     *
     * @param collection list or collection
     * @return boolean
     */
    public static boolean isNotEmpty(Collection collection) {
        return (collection != null) && !(collection.isEmpty());
    }

}
