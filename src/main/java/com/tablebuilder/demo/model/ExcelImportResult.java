package com.tablebuilder.demo.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Приходящий ")
public class ExcelImportResult {
    private boolean success;
    private int rowsImported;
    private String tableName;
    private String message;

}
