package com.gov.grid.service;

import com.gov.grid.entity.SysDept;

import java.util.List;
import java.util.Map;

public interface DeptService {

    SysDept create(SysDept dept);

    SysDept update(SysDept dept);

    void delete(Long id);

    SysDept getById(Long id);

    List<SysDept> listAll();

    List<Map<String, Object>> tree();
}
