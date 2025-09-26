package com.tablebuilder.demo.service;

import com.tablebuilder.demo.model.FileDataResponse;
import com.tablebuilder.demo.model.SheetData;
import com.tablebuilder.demo.model.TableDataResponse;
import com.tablebuilder.demo.store.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExcelExportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UploadedTableRepository uploadedTableRepository;
    @Autowired
    private TableListRepository tableListRepository;
    @Autowired
    private TableColumnRepository tableColumnRepository;

    public TableDataResponse getTableData(String internalTableName) {
        // 1. Получаем метаданные таблицы
        var uploadedTable = uploadedTableRepository.findByInternalName(internalTableName)
                .orElseThrow(() -> new RuntimeException("Table not found: " + internalTableName));

        // 2. Получаем столбцы в правильном порядке
        List<TableColumn> columns = tableColumnRepository.findByTableIdOrderByOriginalIndex(uploadedTable.getId());

        List<String> displayColumnNames = columns.stream()
                .map(TableColumn::getDisplayName)
                .collect(Collectors.toList());

        List<String> internalColumnNames = columns.stream()
                .map(TableColumn::getInternalName)
                .collect(Collectors.toList());

        // 3. Формируем SELECT
        String selectColumns = String.join(", ", internalColumnNames);
        String sql = "SELECT " + selectColumns + " FROM " + internalTableName;

        // 4. Выполняем запрос
        List<List<String>> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
            List<String> row = new java.util.ArrayList<>();
            for (String col : internalColumnNames) {
                // Все данные храним как TEXT → безопасно читаем как String
                row.add(rs.getString(col));
            }
            return row;
        });

        // 5. Формируем ответ
        TableDataResponse response = new TableDataResponse();
        response.setTableName(uploadedTable.getDisplayName());
        response.setColumns(displayColumnNames);
        response.setRows(rows);

        return response;
    }

    @Transactional
    public FileDataResponse getFileData(String fileName) {
        FileDataResponse response = new FileDataResponse();
        response.setFileName(fileName);
        List<SheetData> sheets = new ArrayList<>();
        UploadedTable table = uploadedTableRepository.findByDisplayName(fileName);
// получаем листы по ид
        List<TableList> tableLists = tableListRepository.findByTableId(table.getId());

        for (TableList tableList : tableLists) {
            // Получаем столбцы
            List<TableColumn> columns = tableColumnRepository.findByTableIdAndListNameOrderByOriginalIndex(
                    table.getId(), tableList.getListName());
            List<String> displayColumnNames = columns.stream()
                    .map(TableColumn::getDisplayName)
                    .collect(Collectors.toList());
            System.out.println(displayColumnNames);
            List<String> internalColumnNames = columns.stream()
                    .map(TableColumn::getInternalName)
                    .collect(Collectors.toList());
            System.out.println(internalColumnNames);

            SheetData sheetData = new SheetData();
            String sheetName = tableList.getOriginalListName();
            sheetData.setSheetName(sheetName);
            sheetData.setColumns(displayColumnNames);

            // Запрашиваем данные
            String selectColumns = String.join(", ", internalColumnNames);
            String sql = "SELECT " + selectColumns + " FROM " + tableList.getListName();
            List<List<String>> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
                List<String> row = new ArrayList<>();
                for (String col : internalColumnNames) {
                    row.add(rs.getString(col));
                }
                return row;
            });
            sheetData.setRows(rows);
            sheets.add(sheetData);
        }
        response.setSheets(sheets);
        return response;
    }


    public List<String> getAllValueInColumn(String decodedFileName, String sheetName, String columnName) {
        SheetData sheetData = getSheetByOriginalName(decodedFileName, sheetName);
        List<String> columns = sheetData.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).equals(columnName)) {
                int finalI = i;
                return sheetData.getRows().stream().map(row -> row.get(finalI)).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }


    public SheetData getSheetByOriginalName(String decodedFileName, String sheetName) {
        FileDataResponse fileDataResponse = getFileData(decodedFileName);
        List<SheetData> sheets = fileDataResponse.getSheets();
        Optional<SheetData> sheetData = sheets.stream()
                .filter(sheet -> sheet.getSheetName().equals(sheetName))
                .findFirst();

        SheetData answer = new SheetData();
        if (sheetData.isPresent()) {
            answer = sheetData.get();
        }
        return answer;
    }
}