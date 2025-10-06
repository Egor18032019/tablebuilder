package com.tablebuilder.demo.controllers;


import com.tablebuilder.demo.model.*;
import com.tablebuilder.demo.service.ExcelExportService;
import com.tablebuilder.demo.store.SheetTable;
import com.tablebuilder.demo.store.TemplateCell;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/table")
public class TableDataController {

    @Autowired
    private ExcelExportService excelExportService;

    @Operation(summary = "Возвращает данные таблицы с оригинальными именами столбцов по имени файла - " +
            "First.xlsx")
    @GetMapping("/{fileName}/data")
    public ResponseEntity<FileDataResponse> getFileDataOnPath(@PathVariable String fileName) {
        try {
            // URL-декодируем имя файла (на случай %D0%A4%D0%B8%D0%BE...) на всякий случай
            String decodedFileName = java.net.URLDecoder.decode(fileName, StandardCharsets.UTF_8);
            FileDataResponse data = excelExportService.getFileData(decodedFileName);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createTable(@RequestBody TableTemplateDTO template) {
        if (template != null) {
            return ResponseEntity.ok(new FileDataResponse());
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Обновление ОДНОЙ ячейки")
    @PutMapping("/cell")
    public ResponseEntity<?> updateCell(@RequestBody CellUpdateRequest request) {
        try {
            TemplateCell cell = excelExportService.updateCell(request);
            return ResponseEntity.ok().body(cell.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Обновление ВСЕГО листа (массовое редактирование)
     */
    @Operation(summary = "Обновление ВСЕГО листа (массовое редактирование). \n" +
            " ! ВСЕ ! ячейки листа обновляются. ")
    @PutMapping("/sheet/{sheetId}")
    public ResponseEntity<?> updateSheet(@PathVariable Long sheetId, @RequestBody SheetUpdateRequest request) {
        try {
            SheetTable sheet = excelExportService.updateSheet(sheetId, request);
            return ResponseEntity.ok().body(sheet.getId());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Фильтрация строк листа по значению в определённом столбце
     *
     * @param sheetId      ID листа
     * @param columnIndex  Номер столбца (0-based)
     * @param filterValue  Значение для фильтрации
     * @param operator     Оператор: equals, contains
     * @param filterValue2 Второе значение для between (опционально)
     */
    @Operation(summary = "Фильтрация строк листа по значению в определённом столбце. \n" +
            "Оператор: equals, contains, gt, lt, gte, lte, between (опционально)")
    @GetMapping("/sheet/{sheetId}/filter")
    public ResponseEntity<List< CellData>> filterSheet(
            @PathVariable Long sheetId,
            @RequestParam Integer columnIndex,
            @RequestParam String filterValue,
            @RequestParam(defaultValue = "equals") String operator,
            @RequestParam(required = false) String filterValue2) {

        try {
            List<CellData> filteredRows = excelExportService.filterByColumn(
                    sheetId, columnIndex, filterValue, operator, filterValue2
            );
            return ResponseEntity.ok(filteredRows);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}