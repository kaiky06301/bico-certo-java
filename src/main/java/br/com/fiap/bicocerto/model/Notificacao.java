package br.com.fiap.bicocerto.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacoes")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private Solicitacao solicitacao;

    @Column(nullable = false, length = 300)
    private String mensagem;

    @Column(nullable = false)
    private boolean lida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAviso tipo = TipoAviso.PEDIDO;

    /** Valor citado no aviso (orcamento, contraproposta, contratacao). */
    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal valor;

    /** Orcamento do modulo .NET ligado ao aviso (o app abre direto a proposta). */
    @Column(name = "orcamento_id")
    private Long orcamentoId;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected Notificacao() {
    }

    public Notificacao(Usuario usuario, Solicitacao solicitacao, String mensagem) {
        this(usuario, solicitacao, TipoAviso.PEDIDO, mensagem, null, null);
    }

    public Notificacao(Usuario usuario, Solicitacao solicitacao, TipoAviso tipo, String mensagem,
                       java.math.BigDecimal valor, Long orcamentoId) {
        this.usuario = usuario;
        this.solicitacao = solicitacao;
        this.tipo = tipo;
        this.mensagem = mensagem;
        this.valor = valor;
        this.orcamentoId = orcamentoId;
    }

    @PrePersist
    void aoCriar() {
        this.criadaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Solicitacao getSolicitacao() { return solicitacao; }
    public String getMensagem() { return mensagem; }
    public boolean isLida() { return lida; }
    public void marcarComoLida() { this.lida = true; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public TipoAviso getTipo() { return tipo; }
    public java.math.BigDecimal getValor() { return valor; }
    public Long getOrcamentoId() { return orcamentoId; }
}
