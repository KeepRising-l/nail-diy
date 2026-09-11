package com.diy.nail.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    /** 总记录数 */
    private Long total;
    /** 当前页数据 */
    private List<T> records;

    public static <T> PageResult<T> of(Long total, List<T> records) {
        return new PageResult<>(total, records);
    }
}
