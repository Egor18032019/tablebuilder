package com.tablebuilder.demo.service;


import com.tablebuilder.demo.model.CellData;
import com.tablebuilder.demo.model.ExcelImportResult;
import com.tablebuilder.demo.store.SheetTable;
import com.tablebuilder.demo.store.TemplateCell;
import com.tablebuilder.demo.store.TemplateCellRepository;
import com.tablebuilder.demo.store.UploadedFileTable;
import com.tablebuilder.demo.utils.CellDataType;
import com.tablebuilder.demo.utils.NameUtils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class ExcelImportService {

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private TemplateCellRepository templateCellRepository;

    public ExcelImportResult importExcel(MultipartFile file, String username) {
        try {
            validateFile(file);
            String originalFilename = file.getOriginalFilename();
            String baseFileName = extractBaseName(originalFilename);
            String internalTableName = NameUtils.toValidSqlName(baseFileName);

            try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                return processWorkbook(workbook, originalFilename, internalTableName, username);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ExcelImportResult(false, 0, "", "Error: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("Filename is null");
        }
    }

    private String extractBaseName(String filename) {
        return filename.replaceAll("\\.[^.]*$", "");
    }

    private ExcelImportResult processWorkbook(Workbook workbook, String originalFilename,
                                              String internalTableName, String username) {
        UploadedFileTable savedTable = metadataService.saveUploadedTable(
                originalFilename, internalTableName, username
        );

        int totalRowsImported = 0;
        List<String> processedSheets = new ArrayList<>();

        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            if (isSheetEmpty(sheet)) continue;

            String sheetName = workbook.getSheetName(sheetIndex);
            String tableName = NameUtils.toValidSqlName(internalTableName + "__" + sheetName);
            SheetTable sheetTable = metadataService.saveSheetInTable(savedTable, tableName, sheetName);

            List<TemplateCell> cellsToSave = processSheet(sheet, sheetTable, workbook);
            if (!cellsToSave.isEmpty()) {
                templateCellRepository.saveAll(cellsToSave);
            }

            totalRowsImported += sheet.getLastRowNum() + 1;
            processedSheets.add(tableName);
        }

        if (processedSheets.isEmpty()) {
            return new ExcelImportResult(false, 0, originalFilename, "No valid sheets found");
        }

        return new ExcelImportResult(
                true,
                totalRowsImported,
                String.join(", ", processedSheets),
                "Import successful. Sheets: " + processedSheets
        );
    }

    private boolean isSheetEmpty(Sheet sheet) {
        return sheet.getPhysicalNumberOfRows() < 1;
    }

    private List<TemplateCell> processSheet(Sheet sheet, SheetTable sheetTable, Workbook workbook) {
        List<TemplateCell> cellList = new ArrayList<>();
        int lastRowNum = sheet.getLastRowNum();

        for (int rowIndex = 0; rowIndex <= lastRowNum; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            int lastCellNum = row.getLastCellNum();
            for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
                Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                if (cell == null || cell.getCellType() == CellType.BLANK) continue;

                CellData cellData = getCellForSave(cell, workbook);

                TemplateCell templateCell = buildTemplateCell(cellData, sheetTable, rowIndex, cellIndex);
                cellList.add(templateCell);
            }
        }
        return cellList;
    }

    private TemplateCell buildTemplateCell(CellData cellData, SheetTable sheetTable,
                                           int rowIndex, int cellIndex) {
        TemplateCell cell = new TemplateCell();
        cell.setSheet(sheetTable);
        cell.setValue(cellData.getValue());
        cell.setCellIndex(cellIndex);
        cell.setRowIndex(rowIndex);
        cell.setDataType(cellData.getDataType());
        cell.setFormula(cellData.getFormula());
        cell.setStyle(cellData.getStyles());
        cell.setDescription(cellData.getDescription());
        return cell;
    }

    // --- Обработка стилей ячейки ---

    private CellData getCellForSave(Cell cell, Workbook workbook) {
        CellData cellData = new CellData();
        cellData.setFormula("");

        // Определяем тип данных и значение
        determineCellValueAndType(cell, cellData);

        // Собираем стили
        Map<String, Object> styles = extractCellStyles(cell, workbook);
        cellData.setStyles(styles.toString());

        // Комментарий
        cellData.setDescription(getCellComment(cell));

        return cellData;
    }

    private void determineCellValueAndType(Cell cell, CellData cellData) {
        switch (cell.getCellType()) {
            case BLANK -> {
                // Пропускаем пустые ячейки
            }
            case STRING -> {
                cellData.setDataType(CellDataType.STRING);
                cellData.setValue(cell.getStringCellValue());
            }
//            case DATE -> {
//                cellData.setDataType(CellDataType.STRING);
//                cellData.setValue(cell.getStringCellValue());
//            }
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    cellData.setDataType(CellDataType.DATE);
                    cellData.setValue(cell.getDateCellValue().toString());
                } else {
                    cellData.setDataType(CellDataType.NUMERIC);
                    cellData.setValue(String.valueOf(cell.getNumericCellValue()));
                }
            }
            case BOOLEAN -> {
                cellData.setDataType(CellDataType.BOOLEAN);
                cellData.setValue(String.valueOf(cell.getBooleanCellValue()));
            }
            case FORMULA -> {
                cellData.setDataType(CellDataType.FORMULA);
                // Пытаемся получить вычисленное значение
                try {
                    FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                    CellValue evaluated = evaluator.evaluate(cell);
                    if (evaluated != null) {
                        if (DateUtil.isCellDateFormatted(cell)) {
                            cellData.setValue(cell.getDateCellValue().toString());
                        } else if (evaluated.getCellType() == CellType.NUMERIC) {
                            cellData.setValue(String.valueOf(evaluated.getNumberValue()));
                        } else if (evaluated.getCellType() == CellType.STRING) {
                            cellData.setValue(evaluated.getStringValue());
                        } else if (evaluated.getCellType() == CellType.BOOLEAN) {
                            cellData.setValue(String.valueOf(evaluated.getBooleanValue()));
                        }
                    }
                } catch (Exception e) {
                    // Если не удалось вычислить — сохраняем как есть
                }
                cellData.setFormula(cell.getCellFormula());
            }
            default -> {
                cellData.setDataType(CellDataType.STRING);
                cellData.setValue(cell.toString());
            }
        }
    }

    private Map<String, Object> extractCellStyles(Cell cell, Workbook workbook) {
        Map<String, Object> styles = new HashMap<>();
        CellStyle cellStyle = cell.getCellStyle();
        Font font = workbook.getFontAt(cellStyle.getFontIndex());
        System.out.println(font);
        // Шрифт
        List<String> fontStyles = new ArrayList<>();
        if (font.getBold()) fontStyles.add("bold");
        if (font.getItalic()) fontStyles.add("italic");
        if (font.getStrikeout()) fontStyles.add("strikethrough");
        styles.put("font-styles", fontStyles.isEmpty() ? List.of("normal") : fontStyles);
        styles.put("font-size", font.getFontHeight());
        styles.put("font-color", extractFontColor(font));
        styles.put("font-family", font.getFontName());
        styles.put("font-height", font.getFontHeightInPoints());
        // Фон
        styles.put("background-color", extractBackgroundColor(cellStyle, workbook));
        // Бордеры
        if (cellStyle.getBorderTop() != BorderStyle.NONE) {
            styles.put("border-top", cellStyle.getBorderTop().name().toLowerCase());
            styles.put("border-color", cellStyle.getTopBorderColor());
        }
        if (cellStyle.getBorderLeft() != BorderStyle.NONE) {
            styles.put("border-left", cellStyle.getBorderLeft().name().toLowerCase());
            styles.put("border-color", cellStyle.getLeftBorderColor());
        }
        if (cellStyle.getBorderRight() != BorderStyle.NONE) {
            styles.put("border-right", cellStyle.getBorderRight().name().toLowerCase());
            styles.put("border-color", cellStyle.getRightBorderColor());
        }
        if (cellStyle.getBorderBottom() != BorderStyle.NONE) {
            styles.put("border-bottom", cellStyle.getBorderBottom().name().toLowerCase());
            styles.put("border-color", cellStyle.getBottomBorderColor());
        }
        // Выравнивание
        styles.put("text-align", cellStyle.getAlignment().name().toLowerCase());
        //размер столбца
        Row row = cell.getRow();
        if (row != null) {
            styles.put("rowHeight", row.getHeightInPoints());
            styles.put("line-height",  row.getHeightInPoints());
        } else {
            styles.put("rowHeight", 15.0f); // default
            styles.put("line-height", 15.0f); // default
        }
        if (cellStyle.getWrapText()) {
            styles.put("text-wrap", "wrap");
        }
        return styles;
    }

    private String extractFontColor(Font font) {
        if (font instanceof XSSFFont) {
            XSSFColor fontColor = ((XSSFFont) font).getXSSFColor();
            if (fontColor != null && fontColor.getRGB() != null) {
                byte[] rgb = fontColor.getRGB(); // [R, G, B]
                String hex = String.format("#%02X%02X%02X",
                        rgb[0] & 0xFF,
                        rgb[1] & 0xFF,
                        rgb[2] & 0xFF
                );
               return hex;
            }
        }
        return "#000000";
    }

    private String extractBackgroundColor(CellStyle cellStyle, Workbook workbook) {
        // Попытка получить HEX-цвет для .xlsx
        if (workbook instanceof XSSFWorkbook && cellStyle instanceof XSSFCellStyle) {
            XSSFColor bgColor = ((XSSFCellStyle) cellStyle).getFillBackgroundColorColor();
            if (bgColor != null && bgColor.getRGB() != null) {
                byte[] rgb = bgColor.getRGB();
                return String.format("#%02X%02X%02X",
                        rgb[0] & 0xFF,
                        rgb[1] & 0xFF,
                        rgb[2] & 0xFF
                );
            }
        }

        // Fallback: возвращаем индекс
        return String.valueOf(cellStyle.getFillBackgroundColor());
    }

    private String getCellComment(Cell cell) {
        if (cell.getCellComment() == null) return "";

        return cell.getCellComment().toString();
    }
}