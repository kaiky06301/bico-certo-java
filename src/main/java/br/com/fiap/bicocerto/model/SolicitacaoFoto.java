package br.com.fiap.bicocerto.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Foto do problema anexada pelo morador (data URI base64 JPEG/PNG). */
@Entity
@Table(name = "solicitacao_fotos")
public class SolicitacaoFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private Solicitacao solicitacao;

    @Column(nullable = false)
    private int ordem;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String imagem;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected SolicitacaoFoto() {
    }

    public SolicitacaoFoto(Solicitacao solicitacao, int ordem, String imagem) {
        this.solicitacao = solicitacao;
        this.ordem = ordem;
        this.imagem = imagem;
    }

    @PrePersist
    void aoCriar() {
        this.criadaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Solicitacao getSolicitacao() { return solicitacao; }
    public int getOrdem() { return ordem; }
    public String getImagem() { return imagem; }
}
