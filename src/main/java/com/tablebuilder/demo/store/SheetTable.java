package com.tablebuilder.demo.store;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Для хранения информации о листах в файле
 */
@Entity
@Table(name = "file_sheets")
@Data
public class SheetTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "table_id", nullable = false)
    private UploadedFileTable table;
    @Column(nullable = false, length = 63)
    private String internalListName;
    @Column(nullable = false, length = 63)
    private String originalListName;
}
