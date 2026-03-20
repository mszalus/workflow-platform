package com.workflowplatform.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> implements Serializable {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
