package org.wstorm.rcache.cache;

import org.wstorm.rcache.RObject;

/**
 * 数据提取接口,用于缓存Miss后回调抓取
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月06日
 */
public interface DataPicker<ID, T extends RObject<ID>> {
    /**
     * 到持久数据层提取数据
     *
     * @param id 提取的Id
     * @return 提取到的数据对象
     */
    T pickup(ID id);

    /**
     * 生成一个空的对象, 主要用于外层设置到缓存中,以避免缓存被一些无用的id攻击给击穿
     *
     * @return 空的T对象
     */
    T makeEmptyData();

}
