package org.wstorm.rcache.cache;

/**
 * 数据提取接口,用于缓存Miss后回调抓取
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月06日
 */
public interface DataPicker<ID, T> {
    /**
     * 到持久数据层提取数据
     *
     * @param id 提取的Id
     * @return 提取到的数据对象
     */
    T initData(ID id);
}
