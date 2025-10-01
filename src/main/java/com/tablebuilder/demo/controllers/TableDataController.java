package com.tablebuilder.demo.controllers;


import com.tablebuilder.demo.model.FileDataResponse;
import com.tablebuilder.demo.service.ExcelExportService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/tables")
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

//    /**
//     * Возвращает данные таблицы
//     * и фильтрация и сортировка.
//     *
//     * @param request - имя файла и параметры фильтрации и сортировки.
//     * @return - данные таблицы посредством фильтрации и сортировки.
//     */
//    @Operation(summary = "Фильтрация и сортировка данных таблицы. \n"
//            +"  Поддерживаемые операторы:\n" +
//            " contains — текст содержит подстроку (регистронезависимо)\n" +
//            " equals — точное совпадение\n" +
//            "  gt / lt / gte / lte — для чисел и дат\n" +
//            " between — диапазон (value → value2)"
//            )
//    @PostMapping("/file-data")
//    public ResponseEntity<FileDataResponse> getFileData(@RequestBody TableRequest request) {
//
//        try {
//
//            FileDataResponse data = excelExportService.getFileData(request);
//            return ResponseEntity.ok(data);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//
//    @Operation(summary = "Возвращает все значения в столбце")
//    @PostMapping("/data/columns")
//    public ResponseEntity<List<String>> getAllValueInColumn(@RequestBody RequestString request) {
//        try {
//            String fileName = request.getFileName();
//            String sheetName = request.getSheetName();
//            String columnName = request.getColumnName();
//            var columns = excelExportService.getAllValueInColumn(fileName, sheetName, columnName);
//            return ResponseEntity.ok(columns);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().build();
//        }
//
//    }
}