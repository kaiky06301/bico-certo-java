package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.PrestadorFoto;

import java.time.LocalDateTime;

public record TrabalhoFotoResponse(Long id, String imagem, String legenda, LocalDateTime criadaEm) {

    public static TrabalhoFotoResponse from(PrestadorFoto f) {
        return new TrabalhoFotoResponse(f.getId(), f.getImagem(), f.getLegenda(), f.getCriadaEm());
    }
}
