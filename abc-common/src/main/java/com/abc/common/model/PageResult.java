package com.abc.common.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {
    private long total;
    private long current;
    private long size;
    private List<T> records;

    public static <T> PageResult<T> empty(long current, long size) {
        PageResult<T> r = new PageResult<>();
        r.setTotal(0);
        r.setCurrent(current);
        r.setSize(size);
        r.setRecords(Collections.emptyList());
        return r;
    }
}
