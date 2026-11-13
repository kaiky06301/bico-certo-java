package br.com.fiap.bicocerto.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "avaliacoes")
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false, unique = true)
    private Solicitacao solicitacao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "prestador_id", nullable = false)
    private Prestador prestador;

    @Column(nullable = false)
    private int nota;

    @Column(length = 500)
    private String comentario;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected Avaliacao() {
    }

    public Avaliacao(Solicitacao solicitacao, Prestador prestador, int nota, String comentario) {
        this.solicitacao = solicitacao;
        this.prestador = prestador;
        this.nota = nota;
        this.comentario = comentario;
    }

    @PrePersist
    void aoCriar() {
        this.criadaEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Solicitacao getSolicitacao() { return solicitacao; }
    public Prestador getPrestador() { return prestador; }
    public int getNota() { return nota; }
    public String getComentario() { return comentario; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}
