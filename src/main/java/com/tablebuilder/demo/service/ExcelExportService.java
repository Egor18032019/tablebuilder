package com.tablebuilder.demo.service;

import com.tablebuilder.demo.model.*;
import com.tablebuilder.demo.store.*;
import com.tablebuilder.demo.utils.CellDataType;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExcelExportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UploadedTableRepository fileRepository;
    @Autowired
    private SheetTableRepository sheetRepository;
    @Autowired
    private TemplateCellRepository cellRepository;


    @Transactional
    public FileDataResponse getFileData(String fileName) {
        FileDataResponse response = new FileDataResponse();
        response.setFileName(fileName);
        List<SheetData> sheets = new ArrayList<>();
        UploadedFileTable table = fileRepository.findByDisplayName(fileName);
// получаем листы по ид
        List<SheetTable> sheetsListFromDB = sheetRepository.findByTableId(table.getId());

        for (SheetTable sheet : sheetsListFromDB) {
            String sheetName = sheet.getOriginalListName();
            SheetData sheetData = new SheetData();
            sheetData.setSheetName(sheetName);

            List<TemplateCell> templateCells = cellRepository.findBySheetId(sheet.getId());
            List<CellData> cellDataList = templateCells.stream()
                    .map(cell -> new CellData(cell.getValue(), cell.getColumnIndex(), cell.getRowIndex(),
                            cell.getDataType(), cell.getFormula(), cell.getStyle(), cell.getDescription()))
                    .toList();
            sheetData.setCellDataList(cellDataList);
            sheets.add(sheetData);
        }
        response.setSheets(sheets);
        return response;
    }

    public String getFileName(Long fileId) {
        if (fileId != null) {
            UploadedFileTable file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));
            return file.getDisplayName();
        } else {
            return "";
        }
    }

    /**
     * Экспорт ВСЕГО файла (все листы) в один Excel-файл
     */
    public byte[] exportFileToExcel(Long fileId) throws IOException {
        UploadedFileTable file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        // Получаем все листы файла
        List<SheetTable> sheets = sheetRepository.findByTableId(file.getId());
        if (sheets.isEmpty()) {
            throw new IllegalArgumentException("No sheets found for file: " + fileId);
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            for (SheetTable sheet : sheets) {
                exportSheetToWorkbook(workbook, sheet);
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        }
    }

    /**
     * Экспорт одного листа в workbook
     */
    private void exportSheetToWorkbook(XSSFWorkbook workbook, SheetTable sheet) {
        List<TemplateCell> cells = cellRepository.findBySheetId(sheet.getId());

        // Группируем ячейки
        Map<Integer, Map<Integer, TemplateCell>> cellMatrix = cells.stream()
                .collect(Collectors.groupingBy(
                        TemplateCell::getRowIndex,
                        Collectors.toMap(TemplateCell::getColumnIndex, c -> c)
                ));

        int maxRow = cellMatrix.keySet().stream().max(Integer::compareTo).orElse(0);
        int maxCol = cells.stream().mapToInt(TemplateCell::getColumnIndex).max().orElse(0);

        // Ограничиваем имя листа до 31 символа (лимит Excel)
        String sheetName = sheet.getOriginalListName();
        if (sheetName.length() > 31) {
            sheetName = sheetName.substring(0, 31);
        }

        XSSFSheet sheetX = workbook.createSheet(sheetName);
        Map<String, CellStyle> styleCache = new HashMap<>();

        for (int r = 0; r <= maxRow; r++) {
            XSSFRow row = sheetX.createRow(r);
            for (int c = 0; c <= maxCol; c++) {
                XSSFCell cell = row.createCell(c);
                TemplateCell dbCell = cellMatrix.getOrDefault(r, Collections.emptyMap()).get(c);

                if (dbCell != null) {
                    applyCellValue(cell, dbCell);
                    applyCellStyle(cell, dbCell, workbook, styleCache);
                }
            }
        }

        // Автоподбор ширины
        for (int c = 0; c <= maxCol; c++) {
            sheetX.autoSizeColumn(c);
        }
    }

    private void applyCellValue(XSSFCell cell, TemplateCell dbCell) {
        String value = dbCell.getValue();
        CellDataType type = dbCell.getDataType();

        switch (type) {
            case BLANK:
                // Пропускаем пустые ячейки
                break;
            case STRING:
                cell.setCellValue(value);
                break;
            case NUMERIC:
                try {
                    cell.setCellValue(Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    cell.setCellValue(value); // fallback
                }
                break;
            case DATE:
                try {
                    java.time.LocalDate date = java.time.LocalDate.parse(value);
                    cell.setCellValue(java.util.Date.from(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
                } catch (Exception e) {
                    cell.setCellValue(value); // fallback
                }
                break;
            case BOOLEAN:
                cell.setCellValue(Boolean.parseBoolean(value));
                break;
            case FORMULA:
//                cell.setCellFormula(dbCell.getFormula());
                cell.setCellValue(dbCell.getFormula());
                break;
            default:
                cell.setCellValue(value);
        }
    }

    private void applyCellStyle(XSSFCell cell, TemplateCell dbCell, XSSFWorkbook workbook,
                                Map<String, CellStyle> styleCache) {
        try {
            // Парсим стиль из JSON (или строки)
            Map<String, Object> styles = parseStyleMap(dbCell.getStyle());

            // Создаём или получаем стиль из кэша
            String styleKey = styles.toString();
            CellStyle cellStyle = styleCache.computeIfAbsent(styleKey, k -> createCellStyle(styles, workbook));
            System.out.println(cellStyle.toString());
            cell.setCellStyle(cellStyle);
        } catch (Exception e) {
            // Игнорируем ошибки стилей
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseStyleMap(String styleStr) {
        // todo ObjectMapper ?
        Map<String, Object> map = new HashMap<>();
        if (styleStr != null && styleStr.contains("font-styles")) {
            map.put("font-styles", Arrays.asList("normal"));
            map.put("font-size", (short) 11);
        }
        return map;
    }

    private CellStyle createCellStyle(Map<String, Object> styles, XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();

        // Шрифт
        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 11); // default

        List<String> fontStyles = (List<String>) styles.getOrDefault("font-styles", List.of("normal"));
        font.setBold(fontStyles.contains("bold"));
        font.setItalic(fontStyles.contains("italic"));
        font.setStrikeout(fontStyles.contains("strikethrough"));
        String fontSize = (String) styles.get("font-size");
        String fontName = (String) styles.get("font-family");
        font.setFontName(fontName);
        font.setFontHeightInPoints(fontSize != null ? Short.parseShort(fontSize) : (short) 11);
        String fontHeight = (String) styles.get("font-height");
        font.setFontHeightInPoints(fontHeight != null ? Short.parseShort(fontHeight) : (short) 11);
        // Цвет фона
        String bgColor = (String) styles.get("background-color");
        if (bgColor != null) { // && bgColor.startsWith("#")
            try {
                Color color = Color.decode(bgColor);
                style.setFillForegroundColor(new XSSFColor(color, null));
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            } catch (Exception e) {
                // ignore
            }
        }

        style.setFont(font);
        return style;
    }

    public TemplateCell updateCell(CellUpdateRequest request) {
        // Находим или создаём ячейку
        TemplateCell cell = cellRepository.findBySheetIdAndRowIndexAndColumnIndex(
                request.getFileId(),
                request.getRowIndex(),
                request.getCellIndex()
        ).orElseGet(() -> {
            TemplateCell newCell = new TemplateCell();
            SheetTable sheet = sheetRepository.findById(request.getFileId())
                    .orElseThrow(() -> new RuntimeException("Sheet not found"));
            newCell.setSheet(sheet);
            newCell.setRowIndex(request.getRowIndex());
            newCell.setColumnIndex(request.getCellIndex());
            return newCell;
        });

        // Обновляем данные
        cell.setValue(request.getValue());
        cell.setDataType(request.getDataType());
        cell.setFormula(request.getFormula() != null ? request.getFormula() : "");
        cell.setStyle(request.getStyle() != null ? request.getStyle() : "{}");
        cell.setDescription(request.getDescription() != null ? request.getDescription() : "");

        cellRepository.save(cell);
        return cell;
    }

    @Transactional
    public SheetTable updateSheet(Long sheetId, SheetUpdateRequest request) {
        SheetTable sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new RuntimeException("Sheet not found"));

        // Удаляем старые ячейки листа
        cellRepository.deleteBySheetId(sheetId);

        // Сохраняем новые ячейки
        List<TemplateCell> cellsToSave = request.getCells().stream()
                .map(cellData -> {
                    TemplateCell cell = new TemplateCell();
                    cell.setSheet(sheet);
                    cell.setRowIndex(cellData.getRowIndex());
                    cell.setColumnIndex(cellData.getColumnIndex());
                    cell.setValue(cellData.getValue());
                    cell.setDataType(cellData.getDataType());
                    cell.setFormula(cellData.getFormula() != null ? cellData.getFormula() : "");
                    cell.setStyle(cellData.getStyles() != null ? cellData.getStyles() : "{}");
                    cell.setDescription(cellData.getDescription() != null ? cellData.getDescription() : "");
                    return cell;
                })
                .collect(Collectors.toList());

        cellRepository.saveAll(cellsToSave);
        return sheet;
    }

    public List<CellData> filterByColumn(Long sheetId, Integer columnIndex, String filterValue,
                                         String operator, String filterValue2) {
        // Получаем все ячейки листа
        List<TemplateCell> allCells = cellRepository.findBySheetId(sheetId);

        // Группируем по строкам
        Map<Integer, Map<Integer, TemplateCell>> rows = allCells.stream()
                .collect(Collectors.groupingBy(
                        TemplateCell::getRowIndex,
                        Collectors.toMap(TemplateCell::getColumnIndex, c -> c)
                ));

        // Определяем максимальный индекс столбца
        int maxCol = allCells.stream().mapToInt(TemplateCell::getColumnIndex).max().orElse(0);

        // Фильтруем строки, где значение в нужном столбце удовлетворяет условию
        List<CellData> result = new ArrayList<>();

        for (Map.Entry<Integer, Map<Integer, TemplateCell>> rowEntry : rows.entrySet()) {
            Map<Integer, TemplateCell> row = rowEntry.getValue();
            TemplateCell cellInColumn = row.get(columnIndex);

            if (cellInColumn != null && matchesFilter(cellInColumn, filterValue, operator, filterValue2)) {
                CellData cellData = new CellData(cellInColumn);
                result.add(cellData);

            }
        }

        return result;
    }

    private boolean matchesFilter(TemplateCell cell, String filterValue, String operator, String filterValue2) {
        String cellValue = cell.getValue();
        String dataType = cell.getDataType().name();

        switch (operator.toLowerCase()) {
            case "equals":
                return cellValue.equalsIgnoreCase(filterValue);

            case "contains":
                return cellValue.toLowerCase().contains(filterValue.toLowerCase());

            case "gt":
                return compareNumbersOrDates(cellValue, filterValue, dataType) > 0;

            case "lt":
                return compareNumbersOrDates(cellValue, filterValue, dataType) < 0;

            case "gte":
                return compareNumbersOrDates(cellValue, filterValue, dataType) >= 0;

            case "lte":
                return compareNumbersOrDates(cellValue, filterValue, dataType) <= 0;

            case "between":
                if (filterValue2 == null) return false;
                int cmp1 = compareNumbersOrDates(cellValue, filterValue, dataType);
                int cmp2 = compareNumbersOrDates(cellValue, filterValue2, dataType);
                return cmp1 >= 0 && cmp2 <= 0;

            default:
                return false;
        }
    }

    private int compareNumbersOrDates(String value1, String value2, String dataType) {
        try {
            if (CellDataType.NUMERIC.name().equals(dataType)) {
                BigDecimal num1 = new BigDecimal(value1);
                BigDecimal num2 = new BigDecimal(value2);
                return num1.compareTo(num2);
            } else if (CellDataType.DATE.name().equals(dataType)) {
                LocalDate date1 = LocalDate.parse(value1);
                LocalDate date2 = LocalDate.parse(value2);
                return date1.compareTo(date2);
            } else {
                // Для строк — лексикографическое сравнение
                return value1.compareTo(value2);
            }
        } catch (Exception e) {
            // Если не удалось сравнить — считаем, что не совпадает
            return -1;
        }
    }
}