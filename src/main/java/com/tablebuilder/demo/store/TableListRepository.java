package com.tablebuilder.demo.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TableListRepository extends JpaRepository<TableList, Long> {
    List<TableList> findByTableId(Long id);
}
