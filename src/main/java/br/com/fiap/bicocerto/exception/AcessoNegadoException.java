package br.com.fiap.bicocerto.exception;

/** O usuario esta autenticado, mas o recurso pertence a outra pessoa. */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
