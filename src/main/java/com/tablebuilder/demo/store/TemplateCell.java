package com.tablebuilder.demo.store;

import com.tablebuilder.demo.utils.CellDataType;
import jakarta.persistence.*;
import lombok.Data;

/**
 * Для хранения строк таблицы
 */
@Entity
@Data
@Table(name = "cell_data", indexes = {
        // Составной индекс для быстрой выборки всех ячеек листа по строкам и столбцам
        @Index(name = "idx_cell_sheet_row_cell", columnList = "sheet_id, row_index, cell_index"),

        // Индекс для выборки всех ячеек листа (например, при экспорте)
        @Index(name = "idx_cell_sheet_id", columnList = "sheet_id"),

        // Опционально:  фильтр  по типу данных
        // @Index(name = "idx_cell_data_type", columnList = "data_type")
})
public class TemplateCell {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sheet_id", nullable = false)
    private SheetTable sheet; // ссылка на TableListTemplate.id
    @Column(nullable = false)

    private String value;
    @Column(nullable = false, length = 10000)
    private Integer cellIndex;
    @Column(nullable = false)
    private Integer rowIndex;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CellDataType dataType;

    @Column(nullable = false, length = 10000)
    private String formula;
    @Column(nullable = false, length = 10000)
    private String style;
    @Column(nullable = false, length = 10000)
    private String description;
}
