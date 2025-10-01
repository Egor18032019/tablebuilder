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

@Service
public class ExcelExportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UploadedTableRepository uploadedTableRepository;
    @Autowired
    private SheetTableRepository sheetTableRepository;
    @Autowired
    private TemplateCellRepository templateCellRepository;


    @Transactional
    public FileDataResponse getFileData(String fileName) {
        FileDataResponse response = new FileDataResponse();
        response.setFileName(fileName);
        List<SheetData> sheets = new ArrayList<>();
        UploadedFileTable table = uploadedTableRepository.findByDisplayName(fileName);
// получаем листы по ид
        List<SheetTable> sheetsListFromDB = sheetTableRepository.findByTableId(table.getId());

        for (SheetTable sheet : sheetsListFromDB) {
            String sheetName = sheet.getOriginalListName();
            SheetData sheetData = new SheetData();
            sheetData.setSheetName(sheetName);

            List<TemplateCell> templateCells = templateCellRepository.findBySheetId(sheet.getId());
            List<CellDTO> cellDTOList = templateCells.stream()
                    .map(cell -> new CellDTO(cell.getValue(), cell.getCellIndex(), cell.getRowIndex(),
                            cell.getDataType(), cell.getFormula(), cell.getStyle(), cell.getDescription()))
                    .toList();
            sheetData.setCellDTOList(cellDTOList);
            sheets.add(sheetData);
        }
        response.setSheets(sheets);
        return response;
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

    private void buildFilterClause(StringBuilder sql, FilterRequest filter, String col) {

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