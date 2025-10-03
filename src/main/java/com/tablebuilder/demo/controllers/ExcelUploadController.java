package com.tablebuilder.demo.controllers;

import com.tablebuilder.demo.model.ExcelImportResult;
import com.tablebuilder.demo.service.ExcelImportService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/excel")
public class ExcelUploadController {

    @Autowired
    private ExcelImportService excelImportService;

    @Operation(summary = "Загрузка файла (в параметре можно указать имя пользователя)")
    @PostMapping("/upload")
    public ResponseEntity<ExcelImportResult> uploadExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "username", defaultValue = "anonymous") String username) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ExcelImportResult(false, 0, "", "File is empty")
            );
        }

        ExcelImportResult result = excelImportService.importExcel(file, username);
        return ResponseEntity.ok(result);
    }
}