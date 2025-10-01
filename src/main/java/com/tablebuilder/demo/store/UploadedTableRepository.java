package com.tablebuilder.demo.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedTableRepository extends JpaRepository<UploadedFileTable, Long> {
    Optional<UploadedFileTable> findByInternalName(String internalName);
    boolean existsByInternalName(String internalName);

    List<UploadedFileTable> findByDisplayNameStartingWith(String prefix);

    UploadedFileTable findByDisplayName(String displayName);
}