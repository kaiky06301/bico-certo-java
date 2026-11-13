package br.com.fiap.bicocerto.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/** Formato padrao de todas as respostas de erro da API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoInvalido> campos) {

    public record CampoInvalido(String campo, String mensagem) {
    }

    public static ErroResponse de(int status, String erro, String mensagem, String caminho) {
        return new ErroResponse(LocalDateTime.now(), status, erro, mensagem, caminho, null);
    }
}
