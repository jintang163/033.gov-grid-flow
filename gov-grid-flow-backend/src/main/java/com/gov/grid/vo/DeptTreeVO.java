package com.gov.grid.vo;

import lombok.Data;

@Data
public class DeptTreeVO {

    private Long id;

    private String name;

    private String code;

    private Long parentId;

    private String deptType;

    private String deptTypeName;

    private String leader;

    private String phone;

    private Integer sort;

    private Integer status;

    private String streetName;

    private Boolean isCrossStreetEnabled;

    private String cooperationScope;

    private Boolean hasChildren;

    private java.util.List<DeptTreeVO> children;
}
