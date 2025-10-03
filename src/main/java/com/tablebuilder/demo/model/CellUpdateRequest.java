package com.tablebuilder.demo.model;

import com.tablebuilder.demo.utils.CellDataType;
import lombok.Data;

@Data
public class CellUpdateRequest {

    private Long sheetId;
    private String value;
    private Integer cellIndex;
    private Integer rowIndex;
    private CellDataType dataType;
    private String formula;
    private String style;
    private String description;
}