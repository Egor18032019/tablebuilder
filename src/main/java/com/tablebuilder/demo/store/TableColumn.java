package com.tablebuilder.demo.store;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Для хранения информации о столбцах таблицы,
 * то есть в Excel столбец = "№ мест" а в БД столбец = no_mest
 */
@Entity
@Table(name = "table_columns",
        uniqueConstraints = @UniqueConstraint(columnNames = {"table_id", "internal_name"}))
@Data
public class TableColumn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "table_id", nullable = false)
    private UploadedTable table;

    @Column(nullable = false, length = 63)
    private String internalName; // fio

    @Column(nullable = false)
    private String displayName; // ФИО

    private int originalIndex; // порядок в Excel (0, 1, 2...)
}