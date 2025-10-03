package com.tablebuilder.demo.model;

import java.util.List;

@lombok.Data
public class SheetUpdateRequest {
    private List<CellData> cells;
}
