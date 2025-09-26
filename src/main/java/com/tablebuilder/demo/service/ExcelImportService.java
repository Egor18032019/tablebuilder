package com.tablebuilder.demo.service;


import com.tablebuilder.demo.model.ExcelImportResult;

import com.tablebuilder.demo.store.UploadedTable;
import com.tablebuilder.demo.utils.NameUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelImportService {

    @Autowired
    private DynamicTableService dynamicTableService;
    @Autowired
    private MetadataService metadataService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ExcelImportResult importExcel(MultipartFile file, String username) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return new ExcelImportResult(false, 0, "", "Filename is null");
            }

            // Убираем расширение один раз
            String baseFileName = originalFilename.replaceAll("\\.[^.]*$", "");
            String internalTableName = NameUtils.toValidSqlName(baseFileName);
            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                int totalRowsImported = 0;
                List<String> processedTables = new ArrayList<>();
                UploadedTable savedTable = metadataService.saveUploadedTable(originalFilename, internalTableName, username);
                // Проходим по всем листам
                for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                    Sheet sheet = workbook.getSheetAt(sheetIndex);
                    String sheetName = workbook.getSheetName(sheetIndex);

                    // Пропускаем пустые листы
                    if (sheet.getPhysicalNumberOfRows() < 1) {
                        continue;
                    }

                    Row firstRow = sheet.getRow(0);

                    // === Имя таблицы: файл__лист ===
                    String tableName = NameUtils.toValidSqlName(internalTableName + "__" + sheetName);
                    metadataService.saveTableList(savedTable, tableName, sheetName);

                    // Определяем количество столбцов
                    int maxColumns = 0;
                    boolean firstRowFound = false;
                    for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row != null) {
                            if (!firstRowFound) {
                                firstRow = row;
                                firstRowFound = true;
                            }
                            int lastCell = row.getLastCellNum();
                            if (lastCell > maxColumns) {
                                maxColumns = lastCell;
                            }
                        }
                    }
                    if (maxColumns == 0) {
                        continue;
                    }
                    System.out.println(maxColumns);
// Теперь maxColumns — реальная ширина таблицы
                    // Парсим заголовки
                    List<String> originalColumnNames = new ArrayList<>();
                    List<String> columnNames = new ArrayList<>();

                    for (int i = 0; i < maxColumns; i++) {
                        Cell cell = firstRow.getCell(i);
                        String colNameBefore = (cell == null) ? "col_" + i : cell.getStringCellValue();
                        String colName = NameUtils.toValidSqlName(colNameBefore);

                        originalColumnNames.add(colNameBefore);
                        columnNames.add(colName);
                    }

                    // Создаём таблицу
                    dynamicTableService.ensureTableExists(tableName, columnNames);

                    // Вставляем данные
                    int rowsImported = 0;
                    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) continue;

                        List<Object> values = new ArrayList<>();
                        for (int j = 0; j < columnNames.size(); j++) {
                            Cell cell = row.getCell(j);
                            String cellValue = getCellValueAsString(cell);

                            values.add(cellValue);
                        }

                        insertRow(tableName, columnNames, values);
                        rowsImported++;
                    }

                    totalRowsImported += rowsImported;
                    processedTables.add(tableName);

                    // Сохраняем метаданные для этого листа
                    metadataService.saveTableMetadata(
                            savedTable,
                            originalColumnNames,
                            columnNames,
                            tableName
                    );
                }

                if (processedTables.isEmpty()) {
                    return new ExcelImportResult(false, 0, originalFilename, "No valid sheets found");
                }

                return new ExcelImportResult(
                        true,
                        totalRowsImported,
                        String.join(", ", processedTables),
                        "Import successful. Tables: " + processedTables
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ExcelImportResult(false, 0, "", "Error: " + e.getMessage());
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
//todo добавить стили
        switch (cell.getCellType()) {
            case STRING -> {
                return cell.getStringCellValue();
            }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        return String.valueOf((long) value);
                    } else {
                        return String.valueOf(value);
                    }
                }
            }
            case BOOLEAN -> {
                return String.valueOf(cell.getBooleanCellValue());
            }
            case FORMULA -> {
                return "=" + cell.getCellFormula();
            }
            default -> {
                return "";
            }
        }
    }

    private void insertRow(String tableName, List<String> columnNames, List<Object> values) {
        StringBuilder columns = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder placeholders = new StringBuilder(" VALUES (");

        for (String col : columnNames) {
            columns.append(col).append(", ");
            placeholders.append("?, ");
        }

        // Убираем последние ", "
        String cols = columns.substring(0, columns.length() - 2) + ")";
        String ph = placeholders.substring(0, placeholders.length() - 2) + ")";

        String sql = cols + ph;

        jdbcTemplate.update(sql, values.toArray());
    }
}