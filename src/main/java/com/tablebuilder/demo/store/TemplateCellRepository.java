package com.tablebuilder.demo.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateCellRepository extends JpaRepository<TemplateCell, Long> {
    List<TemplateCell> findBySheetId(Long id);
    // Для поиска по строке:
    List<TemplateCell> findBySheetIdAndRowIndex(Long sheetId, Integer rowIndex);
}
