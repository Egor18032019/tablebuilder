package com.tablebuilder.demo.model;

import com.tablebuilder.demo.store.TemplateCell;
import com.tablebuilder.demo.utils.CellDataType;

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class CellData {
    String value;
    Integer cellIndex;
    Integer rowIndex;
    CellDataType dataType;
    String formula;
    String styles;
    String description;

    public CellData( TemplateCell cell) {
        this.value = cell.getValue();
        this.cellIndex = cell.getCellIndex();
        this.rowIndex = cell.getRowIndex();
        this.dataType = cell.getDataType();
        this.formula = cell.getFormula();
        this.styles = cell.getStyle();
        this.description = cell.getDescription() == null ? "" : cell.getDescription();
    }
}
