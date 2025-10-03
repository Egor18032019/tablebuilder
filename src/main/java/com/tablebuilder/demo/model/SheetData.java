package com.tablebuilder.demo.model;

import lombok.Data;

import java.util.List;

@Data
public class SheetData {
    private String sheetName;        // "Продажи"
    private List<CellData> cellDataList;    // ["№", "ФИО", "Сумма"]

}