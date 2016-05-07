package org.wstorm.rcache.utils;


import org.assertj.core.util.Lists;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sunyp
 * @version 1.0
 * @created 2016年05月07日
 */
public class CollectionsUtilsTest {

    @Test
    public void isEmpty() throws Exception {
        List<String> list = Lists.newArrayList();
        assertThat(CollectionsUtils.isEmpty(list)).isTrue();
        list.add("hello");
        assertThat(CollectionsUtils.isEmpty(list)).isFalse();
        list.clear();
        assertThat(CollectionsUtils.isEmpty(list)).isTrue();
        if (list.isEmpty()) {
            list = null;
        }
        assertThat(CollectionsUtils.isEmpty(list)).isTrue();
    }

    @Test
    public void isEmptyMap() throws Exception {
        Map<String, String> map = new HashMap<>();
        assertThat(CollectionsUtils.isEmpty(map)).isTrue();
        map.put("1", "hello");
        assertThat(CollectionsUtils.isEmpty(map)).isFalse();
        map.clear();
        assertThat(CollectionsUtils.isEmpty(map)).isTrue();
        if (map.isEmpty()) {
            map = null;
        }
        System.out.println(map);
        assertThat(CollectionsUtils.isEmpty(map)).isTrue();

    }

    @Test
    public void isNotEmpty() throws Exception {

        List<String> list = Lists.newArrayList();
        assertThat(CollectionsUtils.isNotEmpty(list)).isFalse();

        list.add("hello");
        assertThat(CollectionsUtils.isNotEmpty(list)).isTrue();
        list.clear();
        assertThat(CollectionsUtils.isNotEmpty(list)).isFalse();
        if (list.isEmpty()) {
            list = null;
        }
        assertThat(CollectionsUtils.isNotEmpty(list)).isFalse();
    }

    @Test
    public void isNotEmptyMap() throws Exception {

        Map<String, String> map = new HashMap<>();
        assertThat(CollectionsUtils.isNotEmpty(map)).isFalse();

        map.put("1", "hello");
        assertThat(CollectionsUtils.isNotEmpty(map)).isTrue();
        map.clear();
        assertThat(CollectionsUtils.isNotEmpty(map)).isFalse();
        if (map.isEmpty()) {
            map = null;
        }
        System.out.println(map);
        assertThat(CollectionsUtils.isNotEmpty(map)).isFalse();

    }

}