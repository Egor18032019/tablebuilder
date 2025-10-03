package com.tablebuilder.demo.controllers;

import com.tablebuilder.demo.exception.InvalidNameException;
import com.tablebuilder.demo.model.TableTemplateDTO;
import com.tablebuilder.demo.service.DynamicTableService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/tables", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "DynamicTable Controller", description = "Контроллер для создания динамических таблиц")
public class DynamicTableController {


    private DynamicTableService dynamicTableService;

    @PostMapping("/create")
    public ResponseEntity<?> createTable(@RequestBody TableTemplateDTO template) {
        try {
            dynamicTableService.createTableFromTemplate(template);
            return ResponseEntity.ok().body("File '" + template.getName() + "' saved successfully!");
        } catch (InvalidNameException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to saved file: " + e.getMessage());
        }
    }
}