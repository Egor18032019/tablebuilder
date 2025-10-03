package com.tablebuilder.demo.controllers;


import com.tablebuilder.demo.service.ExcelExportService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/export")
public class ExcelExportController {

    @Autowired
    private ExcelExportService excelExportService;

    @Operation(summary = "Отдаёт файл данных в формате Excel")
    @GetMapping("/sheet/{sheetId}")
    public ResponseEntity<byte[]> exportSheet(@PathVariable Long sheetId) {
        try {
            byte[] excelFile = excelExportService.exportFileToExcel(sheetId);
            String originalFileName = excelExportService.getFileName(sheetId);
            // Получаем имя листа для имени файла
            String fileName = originalFileName + ".xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + encodedFileName);
            headers.add("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelFile);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}