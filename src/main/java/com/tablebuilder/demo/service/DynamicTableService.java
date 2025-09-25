package com.tablebuilder.demo.service;

import com.tablebuilder.demo.exception.InvalidNameException;
import com.tablebuilder.demo.model.ColumnDefinitionDTO;
import com.tablebuilder.demo.model.TableTemplateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
                case "DATE"   -> sql.append("DATE");
                case "BOOLEAN"-> sql.append("BOOLEAN");
                case "ENUM"   -> sql.append("TEXT"); // можно расширить
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
     * @param tableName - имя таблицы
     * @param columnNames - имена колонок
     */
    @Transactional
    public void ensureTableExists(String tableName, List<String> columnNames) {
        String safeTableName = sanitizeName(tableName);

        // Проверяем, существует ли таблица
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)",
                Boolean.class,
                safeTableName
        );

        if (Boolean.FALSE.equals(exists)) {
            // Создаём таблицу
            StringBuilder sql = new StringBuilder("CREATE TABLE " + safeTableName + " (id BIGSERIAL PRIMARY KEY");
            //todo: расширить: определять типы (число, дата), добавлять новые столбцы через ALTER TABLE.
            for (String colName : columnNames) {
                String safeColName = sanitizeName(colName);
                sql.append(", ").append(safeColName).append(" TEXT"); // пока все TEXT
            }

            sql.append(");");
            jdbcTemplate.execute(sql.toString());
            System.out.println("[INFO] Table created: " + sql);
        } else {
            System.out.println("[INFO] Table already exists: " + safeTableName);
        }
    }

    /**
     * Вставка через JdbcTemplate
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
     * @param tableName
     * @return
     */
    public List<Map<String, Object>> getAllRows(String tableName) {
        return jdbcTemplate.queryForList("SELECT * FROM " + tableName);
    }
}