package com.tablebuilder.demo.service;

import com.tablebuilder.demo.store.TableColumn;
import com.tablebuilder.demo.store.TableColumnRepository;
import com.tablebuilder.demo.store.UploadedTable;
import com.tablebuilder.demo.store.UploadedTableRepository;
import com.tablebuilder.demo.utils.NameUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MetadataService {

    @Autowired
    private UploadedTableRepository uploadedTableRepository;

    @Autowired
    private TableColumnRepository tableColumnRepository;

    /**
     * Сохраняем метаданные таблицы
     *
     * @param originalColumnNames - имена столбцов в файле
     * @param internalColumnNames - имена столбцов в БД
     */
    @Transactional
    public void saveTableMetadata(
            UploadedTable savedTable,
            List<String> originalColumnNames,
            List<String> internalColumnNames) {

        for (int i = 0; i < originalColumnNames.size(); i++) {
            TableColumn col = new TableColumn();
            col.setTable(savedTable);
            col.setInternalName(internalColumnNames.get(i));
            col.setDisplayName(originalColumnNames.get(i));
            col.setOriginalIndex(i);
            tableColumnRepository.save(col);
        }
    }

    public void saveTableColumns(UploadedTable savedTable, List<String> originalColumnNames, List<String> internalColumnNames) {
        // Сохраняем столбцы
        for (int i = 0; i < originalColumnNames.size(); i++) {
            TableColumn col = new TableColumn();
            col.setTable(savedTable);
            col.setInternalName(internalColumnNames.get(i));
            col.setDisplayName(originalColumnNames.get(i));
            col.setOriginalIndex(i);
            tableColumnRepository.save(col);
        }
    }

    public UploadedTable saveUploadedTable(String originalTableName, String internalTableName, String username) {
        UploadedTable table = new UploadedTable();
        table.setInternalName(internalTableName);
        table.setDisplayName(originalTableName);
        table.setUsername(username);
        return uploadedTableRepository.save(table);
    }

    //todo написать 2 метода - по имени таблицы и по id таблицы  и второй по колонкам

    public UploadedTable getOriginalTableName(String internalTableName) {
        return uploadedTableRepository.findByInternalName(internalTableName)
                .orElseThrow(() -> new RuntimeException("Table not found: " + internalTableName));
    }

    public String getOriginalColumnName(Long tableId, String internalColumnName) {
        if (tableColumnRepository.existsByTableIdAndInternalName(tableId, internalColumnName)) {
            return tableColumnRepository.findByTableIdAndInternalName(tableId, internalColumnName)
                    .getDisplayName();
        } else {
            throw new RuntimeException("Column not found: " + internalColumnName);
        }

    }

}