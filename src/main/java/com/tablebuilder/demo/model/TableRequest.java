package com.tablebuilder.demo.model;

import lombok.Data;

import java.util.List;

@Data
public class TableRequest {
    String tableName;
    String listName;
    private List<FilterRequest> filters;
    private List<SortRequest> sorts;
}
