package br.com.fiap.bicocerto.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Envelope de paginacao estavel para o app mobile (evita serializar o PageImpl do Spring). */
public record PaginaResponse<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {

    public static <T> PaginaResponse<T> from(Page<T> page) {
        return new PaginaResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
