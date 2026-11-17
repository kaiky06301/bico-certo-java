package br.com.fiap.bicocerto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** O cliente descreve o problema com as proprias palavras; a triagem define categoria, urgencia e preco. */
public record SolicitacaoRequest(
        @NotBlank(message = "Descreva o problema")
        @Size(min = 10, max = 1000, message = "A descricao deve ter entre 10 e 1000 caracteres") String descricao,
        @Size(max = 80) String bairro,
        /** Endereco salvo em "Meus enderecos"; quando informado, define o bairro e a rua do servico. */
        Long enderecoId,
        /** Informacao adicional opcional ("o disjuntor desarma quando ligo o chuveiro"). */
        @Size(max = 500, message = "A observacao deve ter ate 500 caracteres") String observacao,
        /** Ate 3 fotos (data URI JPEG/PNG em base64). Na edicao: null mantem as fotos atuais, lista vazia remove. */
        @Size(max = 3, message = "Envie no maximo 3 fotos")
        List<@Size(max = 3_000_000, message = "Cada foto deve ter ate 2 MB")
             @Pattern(regexp = "^data:image/(jpeg|jpg|png);base64,[A-Za-z0-9+/=\\r\\n]+$",
                     message = "Envie a foto como imagem JPEG ou PNG") String> fotos) {
}
