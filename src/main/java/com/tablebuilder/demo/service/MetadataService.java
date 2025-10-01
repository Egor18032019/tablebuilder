package com.tablebuilder.demo.service;

import com.tablebuilder.demo.store.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetadataService {

    @Autowired
    private UploadedTableRepository uploadedTableRepository;

    @Autowired
    private SheetTableRepository sheetTableRepository;

//    /**
//     * Сохраняем метаданные таблицы
//     *
//     * @param originalColumnNames - имена столбцов в файле
//     * @param internalColumnNames - имена столбцов в БД
//     */
//    @Transactional
//    public void saveTableMetadata(
//            UploadedFileTable savedTable,
//            List<String> originalColumnNames,
//            List<String> internalColumnNames,
//            String listName) {
//
//        for (int i = 0; i < originalColumnNames.size(); i++) {
//            TableColumn col = new TableColumn();
//            col.setTable(savedTable);
//            col.setInternalName(internalColumnNames.get(i));
//            col.setDisplayName(originalColumnNames.get(i));
//            col.setOriginalIndex(i);
//            col.setListName(listName);
//            tableColumnRepository.save(col);
//        }
//    }


    public UploadedFileTable saveUploadedTable(String originalTableName, String internalTableName, String username) {
        UploadedFileTable table = new UploadedFileTable();
        table.setInternalName(internalTableName);
        table.setDisplayName(originalTableName);
        table.setUsername(username);
        return uploadedTableRepository.save(table);
    }

    //todo написать 2 метода - по имени таблицы и по id таблицы  и второй по колонкам

    public UploadedFileTable getOriginalTableName(String internalTableName) {
        return uploadedTableRepository.findByInternalName(internalTableName)
                .orElseThrow(() -> new RuntimeException("Table not found: " + internalTableName));
    }



    public SheetTable saveSheetInTable(UploadedFileTable savedTable, String tableName, String originalListName) {
        SheetTable sheetTable = new SheetTable();
        sheetTable.setInternalListName(tableName);
        sheetTable.setTable(savedTable);
        sheetTable.setOriginalListName(originalListName);
        sheetTableRepository.save(sheetTable);
        return sheetTable;
    }
}