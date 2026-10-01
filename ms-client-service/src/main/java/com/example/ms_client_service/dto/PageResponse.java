package com.example.ms_client_service.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

/*
 * No devolvemos el Page<T> de Spring Data tal cual porque su serializacion JSON cambia entre
 * versiones (y trae campos internos como "pageable"/"sort" que no necesitamos exponer en la
 * API). Este wrapper fija el contrato de respuesta: content + los metadatos minimos para que
 * el cliente pueda paginar (pagina actual, tamanio, total de elementos/paginas y si es la ultima).
 */
@Getter
@Builder
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
