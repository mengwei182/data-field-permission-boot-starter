package org.example.core;

import lombok.Data;

import java.util.List;

/**
 * 数据及数据列抽象实体
 *
 * @author lihui
 */
@Data
public class DataInformation {
    /**
     * 数据名
     */
    private String dataName;
    /**
     * 数据列字段集合
     */
    private List<String> fieldNames;
}