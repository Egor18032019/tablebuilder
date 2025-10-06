package com.tablebuilder.demo.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateCellRepository extends JpaRepository<TemplateCell, Long> {
    List<TemplateCell> findBySheetId(Long id);
    // Для поиска по строке:
    List<TemplateCell> findBySheetIdAndRowIndex(Long sheetId, Integer rowIndex);
    // Для обновления одной ячейки
    Optional<TemplateCell> findBySheetIdAndRowIndexAndColumnIndex(
            Long sheetId, Integer rowIndex, Integer columnIndex
    );

    // Для удаления всех ячеек листа
    void deleteBySheetId(Long sheetId);
}
