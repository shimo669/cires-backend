package com.cires.ciresbackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class PagedResponseDTO<T> {
    private List<T> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}

