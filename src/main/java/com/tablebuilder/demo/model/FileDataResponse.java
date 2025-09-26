package com.tablebuilder.demo.model;

import lombok.Data;

import java.util.List;

@Data
public class FileDataResponse {
    private String fileName;
    private List<SheetData> sheets;
}