package br.com.fiap.bicocerto.dto;

import br.com.fiap.bicocerto.model.Endereco;

public record EnderecoResponse(Long id, String apelido, String logradouro, String complemento, String bairro,
                               String cidade, String uf, boolean principal) {

    public static EnderecoResponse from(Endereco e) {
        return new EnderecoResponse(e.getId(), e.getApelido(), e.getLogradouro(), e.getComplemento(), e.getBairro(),
                e.getCidade(), e.getUf(), e.isPrincipal());
    }
}
