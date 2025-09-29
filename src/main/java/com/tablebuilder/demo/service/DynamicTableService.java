package com.tablebuilder.demo.service;

import com.tablebuilder.demo.exception.InvalidNameException;
import com.tablebuilder.demo.model.ColumnDefinitionDTO;
import com.tablebuilder.demo.model.TableTemplateDTO;
import com.tablebuilder.demo.utils.ColumnType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.tablebuilder.demo.utils.NameUtils.sanitizeName;

@Service
public class DynamicTableService {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Создание таблицы если ее нет
     *
     * @param template
     */
    @Transactional
    public void createTableFromTemplate(TableTemplateDTO template) {
        String tableName = sanitizeName(template.getName());

        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sql.append(tableName).append(" (id BIGSERIAL PRIMARY KEY");

        for (ColumnDefinitionDTO col : template.getColumns()) {
            String colName = sanitizeName(col.getName());
            sql.append(", ").append(colName).append(" ");

            switch (col.getType().toUpperCase()) {
                case "STRING" -> sql.append("TEXT");
                case "NUMBER" -> sql.append("NUMERIC");
                case "DATE" -> sql.append("DATE");
                case "BOOLEAN" -> sql.append("BOOLEAN");
                case "ENUM" -> sql.append("TEXT"); // можно расширить
                default -> sql.append("TEXT");
            }

            if (col.isRequired()) {
                sql.append(" NOT NULL");
            }

            if (col.isUnique()) {
                sql.append(" UNIQUE");
            }
        }

        sql.append(");");

        System.out.println("[SQL] " + sql); // для отладки
        jdbcTemplate.execute(sql.toString());
    }

    /**
     * Создание таблицы если ее нет
     *
     * @param tableName   - имя таблицы
     * @param columnNames - имена колонок
     */
    @Transactional
    public Map<Object, ColumnType> ensureTableExists(String tableName, List<String> originalColumnNames, List<String> columnNames,
                                                     List<Map<String, Object>> sampleData) {
        String safeTableName = sanitizeName(tableName);

        // Проверяем, существует ли таблица
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)",
                Boolean.class,
                safeTableName
        );
        Map<Object, ColumnType> columnTypes = new HashMap<>();
        if (Boolean.FALSE.equals(exists)) {

            // Анализ типов
            columnTypes = new java.util.HashMap<>();
            for (Object colName : columnNames) {
                List<Object> values = new ArrayList<>();
                for (Map<String, Object> row : sampleData) {
                    values.add(row.get(colName));
                }
                columnTypes.put(colName, detectColumnType(values));
            }
            System.out.println(columnTypes);

            // Создаём таблицу
            StringBuilder sql = new StringBuilder("CREATE TABLE " + safeTableName + " (id BIGSERIAL PRIMARY KEY");

            for (String colName : columnNames) {
                String safeColName = sanitizeName(colName);
                sql.append(", ").append(safeColName).append(" ");

                ColumnType type = columnTypes.get(colName);
                switch (type) {
                    case NUMBER -> sql.append("NUMERIC");
                    case DATE -> sql.append("DATE");
                    case BOOLEAN -> sql.append("BOOLEAN");
                    default -> sql.append("TEXT");
                }
            }


            sql.append(");");
            jdbcTemplate.execute(sql.toString());
            System.out.println("[INFO] Table created: " + sql);
        } else {
            System.out.println("[INFO] Table already exists: " + safeTableName);
        }

        return columnTypes;
    }

    /**
     * Вставка через JdbcTemplate
     *
     * @param tableName
     * @param rowData
     */
    public void insertRow(String tableName, Map<String, Object> rowData) {
        StringBuilder columns = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder placeholders = new StringBuilder(" VALUES (");

        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, Object> entry : rowData.entrySet()) {
            columns.append(entry.getKey()).append(", ");
            placeholders.append("?, ");
            values.add(entry.getValue());
        }

        // Убираем последние ", "
        String cols = columns.substring(0, columns.length() - 2) + ")";
        String ph = placeholders.substring(0, placeholders.length() - 2) + ")";

        String sql = cols + ph;

        jdbcTemplate.update(sql, values.toArray());
    }

    /**
     * Получение всех строк таблицы
     *
     * @param tableName
     * @return
     */
    public List<Map<String, Object>> getAllRows(String tableName) {
        return jdbcTemplate.queryForList("SELECT * FROM " + tableName);
    }


    private ColumnType detectColumnType(List<Object> values) {
        if (values == null || values.isEmpty()) {
            return ColumnType.TEXT;
        }

        boolean allNumbers = true;
        boolean allBooleans = true;
        boolean allDates = true;

        for (Object value : values) {
            if (value == null || value.toString().trim().isEmpty()) {
                continue; // пропускаем пустые
            }

            // Проверка на boolean
            if (!isBoolean(value)) {
                allBooleans = false;
            }

            // Проверка на число
            if (!isNumeric(value)) {
                allNumbers = false;
            }

            // Проверка на дату
            if (!isDate(value)) {
                allDates = false;
            }

            // Если уже всё false — выходим
            if (!allNumbers && !allBooleans && !allDates) {
                return ColumnType.TEXT;
            }
        }

        if (allBooleans && !allNumbers) return ColumnType.BOOLEAN;
        if (allDates && !allNumbers) return ColumnType.DATE;
        if (allNumbers) return ColumnType.NUMBER;

        return ColumnType.TEXT;
    }


    private boolean isBoolean(Object value) {
        String v = value.toString().trim().toLowerCase();
        return "true".equals(v) || "false".equals(v) || "1".equals(v) || "0".equals(v);
    }

    private boolean isNumeric(Object value) {
        if (value == null || value.toString().trim().isEmpty()) return false;
        try {
            Double.parseDouble(value.toString().trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isDate(Object value) {
        if (value == null || value.toString().trim().isEmpty()) return false;
        String v = value.toString().trim();

        // Поддерживаем форматы: yyyy-MM-dd, dd.MM.yyyy, dd/MM/yyyy
        String[] formats = {"yyyy-MM-dd", "dd.MM.yyyy", "dd/MM/yyyy", "MM/dd/yyyy"};

        for (String format : formats) {
            try {
                new java.text.SimpleDateFormat(format).parse(v);
                return true;
            } catch (Exception e) {
                // try next
            }
        }
        return false;
    }
}