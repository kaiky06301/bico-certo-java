package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.SolicitacaoFoto;

public record FotoResponse(Long id, int ordem, String imagem) {

    public static FotoResponse from(SolicitacaoFoto f) {
        return new FotoResponse(f.getId(), f.getOrdem(), f.getImagem());
    }
}
