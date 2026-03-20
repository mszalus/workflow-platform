package com.wfp.customfields.controller;

import com.wfp.customfields.dto.CreateFieldSchemaRequest;
import com.wfp.customfields.dto.FieldSchemaDto;
import com.wfp.customfields.service.FieldSchemaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schemas")
@RequiredArgsConstructor
public class FieldSchemaController {

    private final FieldSchemaService schemaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FieldSchemaDto create(@Valid @RequestBody CreateFieldSchemaRequest request) {
        return schemaService.createSchema(request);
    }

    @GetMapping("/{id}")
    public FieldSchemaDto get(@PathVariable UUID id) {
        return schemaService.getSchema(id);
    }

    @GetMapping
    public List<FieldSchemaDto> list(@RequestParam String processDefinitionKey) {
        return schemaService.listByProcessDefinition(processDefinitionKey);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        schemaService.deleteSchema(id);
    }
}
