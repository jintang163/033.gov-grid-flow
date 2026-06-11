package com.gov.grid.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long total;
    private List<T> list;
    private Integer pageNum;
    private Integer pageSize;

    private PageResult() {
    }

    public static <T> PageResult<T> of(Long total, List<T> list, Integer pageNum, Integer pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setList(list);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }
}
