package org.wstorm.rcache;

import org.wstorm.rcache.cache.DataPicker;

import java.util.List;

/**
 * @author sunyp
 * @version 1.0
 * @created 2016年05月16日
 */
public class TestObjDatePicker implements DataPicker<String, TestObj> {

    private final List<String> ids;

    public TestObjDatePicker(List<String> ids) {
        this.ids = ids;
    }

    @Override
    public TestObj pickup(String key) {

        if (ids.contains(key)) {
            return new TestObj(key, 100);
        }
        return null;
    }

    @Override
    public TestObj makeEmptyData() {
        return new TestObj();
    }

}
