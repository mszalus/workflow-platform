package com.workflowplatform.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    private List<T> data;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private boolean first;
    private boolean last;

    public static <T> PagedResponse<T> of(List<T> data, long totalElements, int page, int size) {
        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) totalElements / size);
        return PagedResponse.<T>builder()
            .data(data)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .page(page)
            .size(size)
            .first(page == 0)
            .last(page >= totalPages - 1)
            .build();
    }
}
