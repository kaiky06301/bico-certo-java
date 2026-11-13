package br.com.fiap.bicocerto.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitacoes")
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "prestador_id")
    private Prestador prestador;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(nullable = false, length = 80)
    private String bairro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Urgencia urgencia;

    @Column(name = "preco_minimo", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoMinimo;

    @Column(name = "preco_maximo", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoMaximo;

    @Column(name = "resumo_triagem", length = 500)
    private String resumoTriagem;

    @Column(name = "dica_seguranca", length = 500)
    private String dicaSeguranca;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_triagem", nullable = false, length = 10)
    private OrigemTriagem origemTriagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSolicitacao status = StatusSolicitacao.ABERTA;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    protected Solicitacao() {
    }

    public Solicitacao(Usuario cliente, String descricao, String bairro) {
        this.cliente = cliente;
        this.descricao = descricao;
        this.bairro = bairro;
    }

    public void aplicarTriagem(Categoria categoria, Urgencia urgencia, BigDecimal precoMinimo, BigDecimal precoMaximo,
                               String resumo, String dicaSeguranca, OrigemTriagem origem) {
        this.categoria = categoria;
        this.urgencia = urgencia;
        this.precoMinimo = precoMinimo;
        this.precoMaximo = precoMaximo;
        this.resumoTriagem = resumo;
        this.dicaSeguranca = dicaSeguranca;
        this.origemTriagem = origem;
    }

    @PrePersist
    void aoCriar() {
        this.criadaEm = LocalDateTime.now();
        this.atualizadaEm = this.criadaEm;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Usuario getCliente() { return cliente; }
    public Categoria getCategoria() { return categoria; }
    public Prestador getPrestador() { return prestador; }
    public void setPrestador(Prestador prestador) { this.prestador = prestador; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public Urgencia getUrgencia() { return urgencia; }
    public BigDecimal getPrecoMinimo() { return precoMinimo; }
    public BigDecimal getPrecoMaximo() { return precoMaximo; }
    public String getResumoTriagem() { return resumoTriagem; }
    public String getDicaSeguranca() { return dicaSeguranca; }
    public OrigemTriagem getOrigemTriagem() { return origemTriagem; }
    public StatusSolicitacao getStatus() { return status; }
    public void setStatus(StatusSolicitacao status) { this.status = status; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
}
