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

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected Notificacao() {
    }

    public Notificacao(Usuario usuario, Solicitacao solicitacao, String mensagem) {
        this.usuario = usuario;
        this.solicitacao = solicitacao;
        this.mensagem = mensagem;
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
}
