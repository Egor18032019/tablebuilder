package com.tablebuilder.demo.controllers;


import com.tablebuilder.demo.model.FileDataResponse;
import com.tablebuilder.demo.model.RequestString;
import com.tablebuilder.demo.model.TableRequest;
import com.tablebuilder.demo.service.ExcelExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/tables")
public class TableDataController {

    @Autowired
    private ExcelExportService excelExportService;

    /**
     * Возвращает данные таблицы с оригинальными именами столбцов.
     *
     * @param fileName - имя файла
     * @return json
     */
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

    @PostMapping("/file-data")
    public ResponseEntity<FileDataResponse> getFileData(@RequestBody TableRequest request) {
        String fileName = request.getTableName();
        try {
            // URL-декодируем имя файла (на случай %D0%A4%D0%B8%D0%BE...) на всякий случай
            String decodedFileName = java.net.URLDecoder.decode(fileName, StandardCharsets.UTF_8);
            FileDataResponse data = excelExportService.getFileData(decodedFileName);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/data/columns")
    public ResponseEntity<List<String>> getAllValueInColumn(@RequestBody RequestString request) {
        try {
            String fileName = request.getFileName();
            String sheetName = request.getSheetName();
            String columnName = request.getColumnName();
            var columns = excelExportService.getAllValueInColumn(fileName, sheetName, columnName);
            return ResponseEntity.ok(columns);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

    }
}