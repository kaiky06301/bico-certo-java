package br.com.fiap.bicocerto.exception;

/** Violacao de regra de negocio: a requisicao e valida, mas nao pode ser executada no estado atual. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
