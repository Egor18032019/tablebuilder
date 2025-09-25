package com.tablebuilder.demo.model;

import lombok.Data;

import java.util.List;

@Data
public class TableTemplateDTO {
    private String name;
    private List<ColumnDefinitionDTO> columns;
}