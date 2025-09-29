package com.tablebuilder.demo.store;

import aj.org.objectweb.asm.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TableColumnRepository extends JpaRepository<TableColumn, Long> {
    List<TableColumn> findByTableIdAndListNameOrderByOriginalIndex(Long tableId, String listName);

    List<TableColumn> findByTableIdOrderByOriginalIndex(Long tableId);

    boolean existsByTableIdAndInternalName(Long tableId, String internalColumnName);

    TableColumn findByTableIdAndInternalName(Long tableId, String internalColumnName);

    TableColumn findByDisplayNameAndListName(String col, String listName);
}