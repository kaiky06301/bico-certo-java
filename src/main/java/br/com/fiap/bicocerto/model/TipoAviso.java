package br.com.fiap.bicocerto.model;

/** Categoria do aviso: define icone e titulo no app e para onde o toque leva. */
public enum TipoAviso {
    PEDIDO("Novo pedido na sua área"),
    ORCAMENTO("Orçamento"),
    NEGOCIACAO("Negociação de valor"),
    CONTRATACAO("Contratação"),
    AGENDAMENTO("Agendamento"),
    SERVICO("Serviço em andamento"),
    CONCLUSAO("Serviço concluído"),
    CANCELAMENTO("Cancelamento");

    private final String titulo;

    TipoAviso(String titulo) {
        this.titulo = titulo;
    }

    public String getTitulo() {
        return titulo;
    }
}
