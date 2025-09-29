package com.tablebuilder.demo.service;


import com.tablebuilder.demo.model.ExcelImportResult;

import com.tablebuilder.demo.store.UploadedTable;
import com.tablebuilder.demo.utils.ColumnType;
import com.tablebuilder.demo.utils.NameUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                    int firsRowSaveValue = 1;
                    boolean firstRowFound = false;
                    int maxLengthRow = 2;
                    for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row != null) {
                            if (!firstRowFound) {
                                firstRow = row;
                                firstRowFound = true;
                            }
                            int lastCell = row.getLastCellNum();
                            if (lastCell > maxColumns) {
                                if (i > 2) {
                                    maxLengthRow = i;
                                }
                                maxColumns = lastCell;
                                firsRowSaveValue = maxColumns;

                            }
                        }
                    }
                    if (maxColumns == 0) {
                        continue;
                    }

                    System.out.println("firsRowSaveValue - " + firsRowSaveValue);
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


                    // Собираем первые 10 строк как пример для анализа типов
                    List<Map<String, Object>> sampleData = new ArrayList<>();
                    int sampleSize = Math.min(10, sheet.getLastRowNum());
                    System.out.println(sampleSize + " - sampleSize");
                    for (int r = maxLengthRow; r <= maxLengthRow + sampleSize; r++) {
                        Row row = sheet.getRow(r);
                        if (row == null) continue;

                        Map<String, Object> rowData = new java.util.HashMap<>();
                        for (int c = 0; c < columnNames.size(); c++) {
                            Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                            rowData.put(columnNames.get(c), getCellValue(cell));
                        }
                        sampleData.add(rowData);
                    }
                    // Создаём таблицу
                    Map<Object, ColumnType> columnTypes = dynamicTableService.ensureTableExists(tableName, originalColumnNames, columnNames, sampleData);

                    // Вставляем данные
                    int rowsImported = 0;
                    for (int i = firsRowSaveValue; i <= sheet.getLastRowNum(); i++) {
                        Row row = sheet.getRow(i);
                        if (row == null) continue;

                        List<Object> values = new ArrayList<>();
                        for (int j = 0; j < columnNames.size(); j++) {
                            Cell cell = row.getCell(j);
                            Object cellValue = getCellValue(cell);
                            values.add(cellValue);
                        }

                        insertRow(tableName, columnNames, columnTypes, values);
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

    private Object getCellValue(Cell cell) {
        if (cell == null) return "";
//todo добавить стили
        switch (cell.getCellType()) {
            case STRING -> {
                return cell.getStringCellValue();
            }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    return cell.getNumericCellValue();

                }
            }
            case BOOLEAN -> {
                return cell.getBooleanCellValue();
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

    private void insertRow(String tableName, List<String> columnNames, Map<Object, ColumnType> columnTypes, List<Object> rawValues) {
        StringBuilder columns = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder placeholders = new StringBuilder(" VALUES (");

        List<Object> convertedValues = new ArrayList<>();

        for (int i = 0; i < columnNames.size(); i++) {
            String colName = columnNames.get(i);
            ColumnType colType = columnTypes.get(colName);
            Object rawValue = rawValues.get(i);

            columns.append(colName).append(", ");
            placeholders.append("?, ");

            // Конвертируем значение в правильный тип
            Object convertedValue = convertValue(rawValue, colType);
            convertedValues.add(convertedValue);
        }

        String cols = columns.substring(0, columns.length() - 2) + ")";
        String ph = placeholders.substring(0, placeholders.length() - 2) + ")";
        String sql = cols + ph;

        jdbcTemplate.update(sql, convertedValues.toArray());
    }

    private Object convertValue(Object rawValue, ColumnType columnType) {
        // Пустые значения → null
        if (rawValue == null || rawValue.toString().trim().isEmpty()) {
            return null;
        }

        try {
            switch (columnType) {
                case ColumnType.NUMBER:
                    // Убираем пробелы, заменяем запятые на точки (для русских Excel)
                    String numericStr = rawValue.toString().trim().replace(",", ".");
                    return new java.math.BigDecimal(numericStr);

                case ColumnType.DATE:
                    // Поддерживаем несколько форматов
                    String dateStr = rawValue.toString().trim();
                    if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        return java.sql.Date.valueOf(dateStr);
                    } else if (dateStr.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                        // dd.MM.yyyy → yyyy-MM-dd
                        String[] parts = dateStr.split("\\.");
                        String isoDate = parts[2] + "-" + parts[1] + "-" + parts[0];
                        return java.sql.Date.valueOf(isoDate);
                    }
                    // Если не распознали — сохраняем как строку (или null)
                    return null;

                case ColumnType.BOOLEAN:
                    String boolStr = rawValue.toString().trim().toLowerCase();
                    return "true".equals(boolStr) || "1".equals(boolStr) || "да".equals(boolStr);

                default:
                    return rawValue; // STRING
            }
        } catch (Exception e) {
            // Если не удалось распарсить — сохраняем как null
            System.err.println("Conversion error for value '" + rawValue + "' to type " + columnType + ": " + e.getMessage());
            return null;
        }
    }
}