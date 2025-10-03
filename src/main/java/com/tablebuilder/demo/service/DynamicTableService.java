package com.tablebuilder.demo.service;

import com.tablebuilder.demo.exception.InvalidNameException;
import com.tablebuilder.demo.model.ColumnDefinitionDTO;
import com.tablebuilder.demo.model.TableTemplateDTO;
import com.tablebuilder.demo.utils.ColumnType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.tablebuilder.demo.utils.NameUtils.sanitizeName;

@Service
public class DynamicTableService {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Создание таблицы если ее нет
     *
     * @param template
     */
    @Transactional
    public void createTableFromTemplate(TableTemplateDTO template) {

    }


}