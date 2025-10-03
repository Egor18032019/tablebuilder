package com.tablebuilder.demo.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SheetTableRepository extends JpaRepository<SheetTable, Long> {
    List<SheetTable> findByTableId(Long id);

}
