package com.wfp.customfields.controller;

import com.wfp.customfields.dto.FieldValueDto;
import com.wfp.customfields.dto.SaveFieldValuesRequest;
import com.wfp.customfields.service.FieldValueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/values")
@RequiredArgsConstructor
public class FieldValueController {

    private final FieldValueService valueService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void save(@Valid @RequestBody SaveFieldValuesRequest request) {
        valueService.saveValues(request);
    }

    @GetMapping
    public List<FieldValueDto> get(@RequestParam String processInstanceId,
                                    @RequestParam(required = false) String taskId) {
        return valueService.getValues(processInstanceId, taskId);
    }
}
