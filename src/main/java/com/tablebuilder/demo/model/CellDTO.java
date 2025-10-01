package com.tablebuilder.demo.model;

import com.tablebuilder.demo.utils.CellDataType;

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class CellDTO {
    String value;
    Integer cellIndex;
    Integer rowIndex;
    CellDataType dataType;
    String formula;
    String style;
    String description;
}
