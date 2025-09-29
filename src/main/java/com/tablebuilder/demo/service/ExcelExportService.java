package com.tablebuilder.demo.service;

import com.tablebuilder.demo.model.*;
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

    public List<List<String>> getTableData(List<String> internalColumnNames, String listName,
                                           List<FilterRequest> filters,
                                           List<SortRequest> sorts) {
        String selectColumns = String.join(", ", internalColumnNames);

//        String sql = "SELECT " + selectColumns + " FROM " + listName;
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(selectColumns);
        sql.append(" FROM ").append(listName);

        //   WHERE (фильтрация)
        if (filters != null && !filters.isEmpty()) {
            sql.append(" WHERE ");
            for (int i = 0; i < filters.size(); i++) {
                if (i > 0) sql.append(" AND ");
                String col = filters.get(i).getColumn();
                String internalCol = tableColumnRepository.findByDisplayNameAndListName(col, listName).getInternalName();
                buildFilterClause(sql, filters.get(i),internalCol );
            }
        }

        // ORDER BY (сортировка)
        if (sorts != null && !sorts.isEmpty()) {
            sql.append(" ORDER BY ");
            for (int i = 0; i < sorts.size(); i++) {
                if (i > 0) sql.append(", ");
                SortRequest sort = sorts.get(i);
                // Защита от SQL-инъекции ? если enums будут, то не нужно ?
                String internalCol = tableColumnRepository.findByDisplayNameAndListName(sort.getColumn(), listName).getInternalName();

                String dir = "ASC".equalsIgnoreCase(sort.getDirection()) ? "ASC" : "DESC";
                sql.append(internalCol).append(" ").append(dir);
            }
        }
        sql.append(";");
        System.out.println(sql);
        List<List<String>> rows = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            List<String> row = new ArrayList<>();
            for (String col : internalColumnNames) {
                row.add(rs.getString(col));
            }
            return row;
        });
        return rows;
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
           System.out.println(sql);
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

    @Transactional
    public FileDataResponse getFileData(TableRequest request) {
        String fileName = request.getTableName();
        FileDataResponse response = new FileDataResponse();
        response.setFileName(fileName);
        List<SheetData> sheets = new ArrayList<>();
        UploadedTable table = uploadedTableRepository.findByDisplayName(fileName);
// получаем листы по ид
        List<TableList> tableLists = tableListRepository.findByTableId(table.getId());

        for (TableList tableList : tableLists) {
            // Пробегаемся по листам
            List<TableColumn> columns = tableColumnRepository.findByTableIdAndListNameOrderByOriginalIndex(
                    table.getId(), tableList.getListName());
            List<String> displayColumnNames = columns.stream()
                    .map(TableColumn::getDisplayName)
                    .collect(Collectors.toList());
            List<String> internalColumnNames = columns.stream()
                    .map(TableColumn::getInternalName)
                    .collect(Collectors.toList());


            SheetData sheetData = new SheetData();
            String sheetName = tableList.getOriginalListName();
            sheetData.setSheetName(sheetName);
            sheetData.setColumns(displayColumnNames);
            // Запрашиваем данные
            List<List<String>> rows;
            if (request.getListName().equals(sheetName)) {
                List<FilterRequest> filters = request.getFilters();
                List<SortRequest> sorts = request.getSorts();
                rows = getTableData(internalColumnNames, tableList.getListName(), filters, sorts);
            } else {
                String selectColumns = String.join(", ", internalColumnNames);
                String sql = "SELECT " + selectColumns + " FROM " + tableList.getListName();
                System.out.println(sql);
                rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
                    List<String> row = new ArrayList<>();
                    for (String col : internalColumnNames) {
                        row.add(rs.getString(col));
                    }
                    return row;
                });
            }

            sheetData.setRows(rows);
            sheets.add(sheetData);
        }
        response.setSheets(sheets);
        System.out.println(response);
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

    private void buildFilterClause(StringBuilder sql, FilterRequest filter,String col ) {

        String op = filter.getOperator();
        String value = filter.getValue();

        // Защита от SQL-инъекции
        if (!col.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            System.out.println("Защита от SQL-инъекции нужна");
        }

        switch (op) {
            case "contains" -> {
                sql.append(col).append(" ILIKE ").append("%").append(value).append("%");
            }
            case "equals" -> {
                sql.append(col).append(" = ")
                        .append(value);

            }
            case "gt" -> {
                sql.append(col).append(" > ")
                        .append(value);

            }
            case "lt" -> {
                sql.append(col).append(" < ")
                        .append(value);
            }
            case "gte" -> {
                sql.append(col).append(" >= ")
                        .append(value);
            }
            case "lte" -> {
                sql.append(col).append(" <= ")
                        .append(value);
            }
            case "between" -> {
                sql.append(col).append(" BETWEEN")
                        .append(value)
                        .append("AND")
                        .append(filter.getValue2());
            }
            default -> throw new IllegalArgumentException("Unsupported operator: " + op);
        }
    }
}